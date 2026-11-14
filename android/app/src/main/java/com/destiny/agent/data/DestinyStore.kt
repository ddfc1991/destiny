package com.destiny.agent.data

import android.content.Context
import android.widget.Toast
import com.destiny.agent.core.DestinyCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Destiny 原生运行时
 *
 * 1:1 移植 Web 版 destiny-app.js 的任务状态机（createTask / tick / 闸门 / 断点恢复），
 * 并新增三大核心能力的驱动逻辑：
 *   · 主动执行：ScheduleItem 到点自动派活；Suggestion 由记忆/日程推导
 *   · 持久记忆：MemoryItem 分三类，「做法」需人工批准才生效
 *   · Computer Use：RemoteSession 走云端桌面步骤流水，遇登录/验证码交人工
 */
class DestinyStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("destiny.v1", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // 引擎实例（与 Web 版同构）
    private val pool = DestinyCore.ScreenPool(4)
    private val ledger = DestinyCore.EventLedger()
    private val gateway = DestinyCore.GatewayHealth()
    private val supervisor = DestinyCore.Supervisor()
    private val identity = DestinyCore.Identity()
    private val memory = DestinyCore.Memory()

    private var scope: CoroutineScope? = null

    init {
        // 恢复进行中的任务占用
        _state.value.tasks
            .filter { it.status in listOf("running", "paused", "awaiting", "queued") }
            .forEach {
                pool.acquire(
                    DestinyCore.PoolTask(it.id, it.app, it.priority)
                )
            }
        // 恢复记忆库
        _state.value.memories.forEach {
            memory.add(it.text, it.kind, it.source)
        }
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(850)
                tick()
                tickRemote()
            }
        }
    }

    /* ─────────────────────────── 持久化 ─────────────────────────── */

    private fun load(): AppState = try {
        val raw = prefs.getString("state", null)
        if (raw.isNullOrEmpty()) AppState(firstRun = System.currentTimeMillis())
        else json.decodeFromString<AppState>(raw)
    } catch (e: Exception) {
        AppState(firstRun = System.currentTimeMillis())
    }

    private fun persist() {
        scope?.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                try {
                    prefs.edit().putString("state", json.encodeToString(AppState.serializer(), _state.value)).apply()
                } catch (e: Exception) {
                    // 持久化失败不阻断交互
                }
            }
        }
    }

    private fun update(block: (AppState) -> AppState) {
        _state.value = block(_state.value)
        persist()
    }

    /* ─────────────────────────── 账户 ─────────────────────────── */

    fun login(method: String, id: String) {
        val name = when (method) {
            "google" -> "Google 用户"
            "phone" -> "手机用户 ${id.takeLast(4)}"
            else -> id.substringBefore('@')
        }
        update {
            it.copy(
                account = Account(method, id, name, System.currentTimeMillis()),
                chatMsgs = listOf(
                    ChatMessage(
                        "bot",
                        "回来了。${if (it.memories.isNotEmpty()) "我记得 ${it.memories.size} 条关于你的事" else "我们还没聊过"}，" +
                            "直接说要办什么就行。",
                        System.currentTimeMillis(),
                        kind = "reply"
                    )
                )
            )
        }
    }

    fun logout() {
        update { it.copy(account = null, chatMsgs = emptyList()) }
    }

    fun linkService(name: String) {
        update { s ->
            if (s.linkedServices.contains(name)) s
            else s.copy(linkedServices = s.linkedServices + name)
        }
    }

    fun unlinkService(name: String) {
        update { it.copy(linkedServices = it.linkedServices - name) }
    }

    /* ─────────────────────────── 会话 ─────────────────────────── */

    fun sendChat(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        // 公众号链接 → 不建任务，进阅读流程
        if (t.contains("mp.weixin.qq.com")) {
            update { s ->
                s.copy(
                    chatMsgs = s.chatMsgs + ChatMessage("user", t, System.currentTimeMillis()) +
                        ChatMessage(
                            "bot",
                            "收到这篇公众号文章。原文要跳系统浏览器打开（微信内嵌有防盗链，壳里不做假嵌入）。",
                            System.currentTimeMillis(),
                            page = "article"
                        )
                )
            }
            return
        }
        createTask(t, guessApp(t), null, true)
    }

    private fun guessApp(text: String): String =
        DestinyData.APP_OPTIONS.firstOrNull { it != "其他" && text.contains(it) } ?: "其他"

    /* ─────────────────────────── 任务引擎 ─────────────────────────── */

    fun createTask(
        text: String,
        app: String,
        risk: String? = null,
        stayInChat: Boolean = true,
        source: String = if (stayInChat) "chat" else "proactive"
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        // 金融类默认禁用
        if (DestinyData.FINANCIAL.contains(app) && _state.value.appAccess[app] != true) {
            if (stayInChat) pushBot("「$app」属金融类，默认禁用。要用先去「应用访问」放行。", kind = "blocked")
            return false
        }

        // 红线：问也不许做
        val rl = DestinyCore.redLineCheck(
            DestinyCore.RedLineAction(
                text = trimmed,
                target = app,
                amount = if (risk == "pay") parseAmount(trimmed) else null
            )
        )
        if (rl.blocked) {
            if (stayInChat) {
                pushUser(trimmed)
                pushBot("这句话撞了红线（${rl.hits.joinToString(" / ")}），问也不许做，我不接。", kind = "blocked")
            }
            return false
        }

        val flowKey = pickFlow(trimmed, risk)
        val id = UUID.randomUUID().toString().take(12)
        val acq = pool.acquire(DestinyCore.PoolTask(id, app, 0))
        val status = when (acq) {
            is DestinyCore.Acquire.Ok -> "running"
            is DestinyCore.Acquire.Deadlock -> "failed"
            else -> "queued"
        }
        val vd = (acq as? DestinyCore.Acquire.Ok)?.slot
        val waitingFor = when (acq) {
            is DestinyCore.Acquire.Deadlock -> "deadlock"
            is DestinyCore.Acquire.Wait -> acq.holder ?: if (acq.wait == "slot") "slot" else null
            else -> null
        }
        val summary = if (acq is DestinyCore.Acquire.Deadlock) {
            "互等死锁：两个任务互相等对方放开 App，当场拒绝入队。"
        } else null

        val task = Task(
            id = id,
            text = trimmed,
            app = app,
            risk = risk,
            flowKey = flowKey,
            status = status,
            vd = vd,
            waitingFor = waitingFor,
            createdAt = System.currentTimeMillis(),
            summary = summary,
            source = source
        )
        ledger.append(id, "run_start", mapOf("goal" to trimmed, "app" to app), false)

        val reply = when {
            acq is DestinyCore.Acquire.Deadlock ->
                "这两个任务互相等对方放开 $app，是互等死锁，我当场拒了，不入队。"
            status == "running" ->
                "收到。已拆成步骤在虚拟屏上执行「$trimmed」（$app）。点这句看执行过程，该问你的我会停下来问。"
            else ->
                "收到。「$trimmed」先排队：${(acq as? DestinyCore.Acquire.Wait)?.holder?.let { "在等另一个任务用完 $app" } ?: "在等空闲虚拟屏"}，轮到就自动开跑。"
        }

        update { s ->
            s.copy(
                tasks = listOf(task) + s.tasks,
                selectedTask = id,
                usage = s.usage.copy(tasks = s.usage.tasks + 1),
                chatMsgs = if (stayInChat) {
                    s.chatMsgs + ChatMessage("user", trimmed, System.currentTimeMillis()) +
                        ChatMessage("bot", reply, System.currentTimeMillis(), link = id, kind = "reply")
                } else s.chatMsgs
            )
        }
        return true
    }

    private fun pickFlow(text: String, risk: String?): String {
        if (risk == "pay" || Regex("付款|下单|买|支付|充值|转帐|转账").containsMatchIn(text)) return "pay"
        if (risk == "login" || Regex("登录|验证码|登陆|扫码").containsMatchIn(text)) return "login"
        return "default"
    }

    private fun parseAmount(text: String): Double? {
        val m = Regex("(?:¥|￥)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:元|块)").find(text)
            ?: return null
        return (m.groupValues[1].ifEmpty { m.groupValues[2] }).toDoubleOrNull()
    }

    /** 任务推进：与 Web 版 tick() 逐行对应 */
    private fun tick() {
        val s = _state.value
        if (s.tasks.isEmpty() && s.schedule.none { !it.done }) return

        var changed = false
        val tasks = s.tasks.toMutableList()

        // 1) 排队任务尝试获取槽位
        tasks.forEachIndexed { i, t ->
            if (t.status != "queued") return@forEachIndexed
            val a = pool.acquire(DestinyCore.PoolTask(t.id, t.app, t.priority))
            when (a) {
                is DestinyCore.Acquire.Ok -> {
                    tasks[i] = t.copy(vd = a.slot, status = "running", waitingFor = null)
                    changed = true
                }
                is DestinyCore.Acquire.Deadlock -> {
                    tasks[i] = t.copy(
                        status = "failed",
                        summary = "互等死锁：${a.cycle.joinToString(" → ")}。当场拒绝，不一直卡着。"
                    )
                    changed = true
                }
                is DestinyCore.Acquire.Wait -> {
                    tasks[i] = t.copy(waitingFor = a.holder ?: "slot")
                }
            }
        }

        // 2) 运行中的任务推进一步
        tasks.forEachIndexed { i, t ->
            if (t.status != "running") return@forEachIndexed
            val flow = DestinyData.FLOW[t.flowKey] ?: DestinyData.FLOW["default"]!!
            if (t.idx >= flow.size) {
                pool.release(t.id)
                ledger.append(t.id, "run_end", emptyMap(), false)
                supervisor.submit(t.id, t.text, true)
                tasks[i] = t.copy(
                    status = "done",
                    steps = t.steps + TaskStep(
                        System.currentTimeMillis(), "督查与检阅 · 待你检阅", "route", 120, "结果已核，等你检阅"
                    )
                )
                changed = true
                return@forEachIndexed
            }
            val f = flow[t.idx]
            val needsGate = f.gate != null &&
                _state.value.allowAlways.contains(f.gate).not() &&
                !(_state.value.mode == "auto" && f.gate == "payment")

            if (needsGate) {
                val pol = DestinyCore.approvalDecide(
                    payment = f.gate == "payment",
                    ask = if (f.gate == "captcha") "handover" else null,
                    attendance = "Attended",
                    origin = "Ui",
                    autonomous = _state.value.mode == "auto"
                )
                val rl = DestinyCore.redLineCheck(
                    DestinyCore.RedLineAction(text = t.text, target = t.app, amount = null)
                )
                tasks[i] = t.copy(
                    gate = f.gate,
                    status = "awaiting",
                    steps = t.steps + TaskStep(
                        System.currentTimeMillis(), f.title, f.kind, 0,
                        "真实停住 · 策略 $pol" + (if (rl.blocked) " · 红线不可授权" else " · 等你决定"),
                        gate = f.gate
                    )
                )
                ledger.append(
                    t.id, "gate",
                    mapOf("type" to (f.gate ?: ""), "policy" to pol, "redline" to rl.blocked.toString()),
                    false
                )
                changed = true
                return@forEachIndexed
            }

            val auto = (_state.value.mode == "auto" && f.gate == "payment") ||
                _state.value.allowAlways.contains(f.gate)
            val detail = stepDetail(f.kind, t.app)
            val tk = 90 + ((t.idx + 1) * 37) % 220
            ledger.append(
                t.id, "step",
                mapOf("stepIndex" to t.idx.toString(), "title" to f.title, "ms" to f.ms.toString(), "tokens" to tk.toString()),
                Regex("提交订单|点按|逐字输入").containsMatchIn(f.title)
            )
            tasks[i] = t.copy(
                idx = t.idx + 1,
                steps = t.steps + TaskStep(System.currentTimeMillis(), f.title, f.kind, f.ms, detail, auto = auto),
                metrics = t.metrics.copy(
                    steps = t.metrics.steps + 1,
                    ms = t.metrics.ms + f.ms,
                    tokens = t.metrics.tokens + tk,
                    calls = t.metrics.calls + 1
                )
            )
            changed = true
        }

        if (changed) {
            update { st ->
                st.copy(
                    tasks = tasks,
                    usage = st.usage.copy(
                        tokens = st.usage.tokens + (tasks.sumOf { it.metrics.tokens } - s.tasks.sumOf { it.metrics.tokens }),
                        calls = st.usage.calls + (tasks.sumOf { it.metrics.calls } - s.tasks.sumOf { it.metrics.calls })
                    )
                )
            }
        }

        // 3) 主动执行：到点的日程自动派活
        fireSchedule()
    }

    private fun stepDetail(kind: String, app: String): String = when (kind) {
        "screen" -> "虚拟屏 vd · $app · 目标控件 EditText #1 · 逐字输入（非粘贴）"
        "route" -> if (_state.value.runtime == "cloud" && _state.value.cloudStatus == "ok") {
            "云端模型端点 · ${_state.value.cloudHost}（演示外壳：拆解仍由本地确定性引擎承载）"
        } else {
            "本地确定性引擎 · 规则路由 · 未调用模型端点"
        }
        else -> ""
    }

    /* ─────────────────────────── 闸门 ─────────────────────────── */

    fun resolveGate(decision: String) {
        val s = _state.value
        val t = s.tasks.firstOrNull { it.id == s.selectedTask && it.status == "awaiting" } ?: return
        val rl = DestinyCore.redLineCheck(DestinyCore.RedLineAction(text = t.text, target = t.app, amount = null))
        if (decision != "deny" && rl.blocked) return

        val pol = DestinyCore.approvalDecide(
            payment = t.gate == "payment",
            ask = if (t.gate == "captcha") "handover" else null,
            attendance = "Attended",
            origin = "Ui",
            autonomous = s.mode == "auto"
        )
        val record = ApprovalRecord(System.currentTimeMillis(), t.id, t.gate ?: "", decision, pol)

        if (decision == "deny") {
            pool.release(t.id)
            ledger.append(t.id, "gate_denied", mapOf("type" to (t.gate ?: ""), "policy" to pol), false)
            update { st ->
                st.copy(
                    tasks = st.tasks.map {
                        if (it.id != t.id) it else it.copy(
                            status = "stopped",
                            gate = null,
                            summary = "在「${gateLabel(t.gate)}」处停止。已完成 ${it.metrics.steps} 步，未提交任何操作。"
                        )
                    },
                    approvals = listOf(record) + st.approvals
                )
            }
        } else {
            if (decision == "always" && t.gate != null) {
                update { it.copy(allowAlways = it.allowAlways + t.gate) }
            }
            ledger.append(t.id, "handover", mapOf("type" to (t.gate ?: ""), "mode" to decision), false)
            update { st ->
                st.copy(
                    tasks = st.tasks.map {
                        if (it.id != t.id) it else it.copy(
                            status = "running",
                            gate = null,
                            idx = it.idx + 1,
                            metrics = it.metrics.copy(steps = it.metrics.steps + 1),
                            steps = it.steps + TaskStep(
                                System.currentTimeMillis(),
                                "人工接管完成 · ${if (decision == "always") "这类以后都放行" else "仅本次"}",
                                "route", 0, "策略 $pol · 已记入授权记录"
                            )
                        )
                    },
                    approvals = listOf(record) + st.approvals
                )
            }
        }
    }

    fun gateLabel(g: String?): String = when (g) {
        "payment" -> "花钱操作 · 付款 / 下单"
        "captcha" -> "验证码 · 需要你亲手处理"
        "redline" -> "红线 · 问也不许做"
        else -> g ?: ""
    }

    /* ─────────────────────────── 任务控制 ─────────────────────────── */

    fun selectTask(id: String) = update { it.copy(selectedTask = id) }

    fun stopTask() {
        val s = _state.value
        val t = s.tasks.firstOrNull { it.id == s.selectedTask } ?: return
        pool.release(t.id)
        ledger.append(t.id, "run_stopped", emptyMap(), false)
        val safe = ledger.replayableFrom(t.id)
        val flow = DestinyData.FLOW[t.flowKey] ?: DestinyData.FLOW["default"]!!
        val lastIdx = minOf(t.idx, flow.size - 1)
        val summary = listOf(
            "做了什么：完成 ${t.metrics.steps} 步，走到「${flow[lastIdx].title}」。",
            "看到了什么：${t.app} 上读到 ${t.metrics.steps * 4 + 3} 个可点控件。",
            "发出几次：${t.metrics.calls} 次本地调用 · ${t.inserts.size} 次插话。",
            "哪些没做完：剩余 ${maxOf(0, flow.size - t.idx)} 步，从这一步可以继续。",
            "事件账本：${ledger.forRun(t.id).size} 条事件；${safe.writesSkipped} 步有副作用，恢复时不重放。"
        ).joinToString("\n")
        update { st ->
            st.copy(tasks = st.tasks.map { if (it.id != t.id) it else it.copy(status = "stopped", summary = summary) })
        }
    }

    fun resumeTask() {
        val s = _state.value
        val t = s.tasks.firstOrNull { it.id == s.selectedTask } ?: return
        if (t.status == "done") return
        val safe = ledger.replayableFrom(t.id)
        val newIdx = maxOf(t.idx, if (safe.replayable) safe.stepIndex + 1 else t.idx)
        val acq = pool.acquire(DestinyCore.PoolTask(t.id, t.app, t.priority))
        val status = if (acq is DestinyCore.Acquire.Ok) "running" else "queued"
        val vd = (acq as? DestinyCore.Acquire.Ok)?.slot
        ledger.append(t.id, "run_resumed", mapOf("fromStep" to newIdx.toString()), false)
        update { st ->
            st.copy(tasks = st.tasks.map {
                if (it.id != t.id) it else it.copy(
                    idx = newIdx, gate = null, summary = null,
                    status = status, vd = vd,
                    waitingFor = (acq as? DestinyCore.Acquire.Wait)?.holder
                )
            })
        }
    }

    fun queueWaitReason(t: Task): String {
        val s = _state.value
        val holders = s.tasks.filter {
            it.id != t.id && it.status in listOf("running", "paused", "awaiting") && it.app == t.app
        }
        val holding = holders.firstOrNull()
        val busy = s.tasks.count { it.status in listOf("running", "paused", "awaiting") && it.id != t.id }
        val freeSlots = 4 - busy
        return when {
            holding != null && freeSlots <= 0 ->
                "在等「${holding.text.take(10)}」放开 ${t.app} · 也等空闲虚拟屏"
            holding != null -> "在等「${holding.text.take(10)}…」放开 ${t.app} 锁"
            freeSlots <= 0 -> "4 块虚拟屏全占满 · 等一块空闲"
            else -> "等调度"
        }
    }

    fun raisePriority(id: String) = update { s ->
        s.copy(tasks = s.tasks.map { if (it.id == id) it.copy(priority = it.priority + 1) else it })
    }

    /* ─────────────────────────── 记忆 ─────────────────────────── */

    fun addMemory(text: String, kind: String) {
        val item = memory.add(text, kind, "manual")
        update { s ->
            s.copy(memories = listOf(
                MemoryItem(item.id, item.text, item.kind, item.status, item.source, item.v, item.at)
            ) + s.memories)
        }
    }

    fun approveMemory(id: String) {
        memory.approve(id)
        update { s ->
            s.copy(memories = s.memories.map {
                if (it.id != id) it else it.copy(status = "active", v = it.v + 1)
            })
        }
    }

    fun deleteMemory(id: String) = update { s ->
        s.copy(memories = s.memories.filterNot { it.id == id })
    }

    /* ─────────────────────────── 主动执行 ─────────────────────────── */

    fun addSchedule(title: String, at: Long, repeat: String, app: String) {
        val id = UUID.randomUUID().toString().take(8)
        update { s ->
            s.copy(schedule = s.schedule + ScheduleItem(id, title, at, repeat, app))
        }
    }

    fun completeSchedule(id: String) = update { s ->
        s.copy(schedule = s.schedule.map { if (it.id == id) it.copy(done = true) else it })
    }

    /** 到点自动派活 —— 这就是「主动执行」的落点 */
    private fun fireSchedule() {
        val now = System.currentTimeMillis()
        val due = _state.value.schedule.filter { !it.done && it.at <= now }
        if (due.isEmpty()) return
        due.forEach { item ->
            update { it.copy(schedule = it.schedule.map { s -> if (s.id == item.id) s.copy(done = true) else s }) }
            createTask(item.title, item.app, null, stayInChat = false, source = "schedule")
            update { s ->
                s.copy(
                    chatMsgs = s.chatMsgs + ChatMessage(
                        "bot",
                        "到点了：${item.title}（${item.app}）我已经派出去，正在跑。",
                        System.currentTimeMillis(),
                        kind = "proactive"
                    ),
                    schedule = s.schedule.map { x -> if (x.id == item.id) x.copy(taskId = null) else x }
                )
            }
        }
    }

    fun dismissSuggestion(id: String) = update { s ->
        s.copy(suggestions = s.suggestions.map { if (it.id == id) it.copy(dismissed = true) else it })
    }

    fun acceptSuggestion(id: String) {
        val sug = _state.value.suggestions.firstOrNull { it.id == id } ?: return
        update { s -> s.copy(suggestions = s.suggestions.filterNot { it.id == id }) }
        createTask(sug.action.ifEmpty { sug.title }, sug.app, sug.risk, stayInChat = false)
        update { s ->
            s.copy(chatMsgs = s.chatMsgs + ChatMessage(
                "bot",
                "已经接下「${sug.title}」，正在执行。",
                System.currentTimeMillis(),
                kind = "proactive"
            ))
        }
    }

    /** 生成建议：结合记忆 + 日程 + 空闲状态推导（本地规则，不臆造） */
    fun generateSuggestions() {
        val s = _state.value
        if (s.suggestions.any { !it.dismissed }) return
        val out = mutableListOf<Suggestion>()
        val now = System.currentTimeMillis()

        // 从记忆里的偏好推导日程建议
        s.memories.filter { it.kind == "preference" && it.status == "active" }.forEach { m ->
            out.add(
                Suggestion(
                    id = "sug-${m.id}",
                    title = "按你记住的偏好安排：${m.text.take(18)}",
                    reason = "这条来自你之前确认过的偏好（第 ${m.v} 版）",
                    action = m.text,
                    app = "日历",
                    tone = "info",
                    at = now
                )
            )
        }
        // 待批准的「做法」提醒
        s.memories.filter { it.kind == "procedure" && it.status == "pending" }.forEach { m ->
            out.add(
                Suggestion(
                    id = "sug-p-${m.id}",
                    title = "有个做法等你点头：${m.text.take(18)}",
                    reason = "做法类记忆要你批准才生效，记住它以后同类事会自动按你的做法来",
                    action = m.text,
                    app = "其他",
                    tone = "warn",
                    at = now
                )
            )
        }
        // 空闲槽位 + 有空档 → 提议整理
        if (s.activeTasks().isEmpty() && out.isEmpty()) {
            out.add(
                Suggestion(
                    id = "sug-idle",
                    title = "现在没有在跑的事，要不要我整理一下？",
                    reason = "4 块虚拟屏都空着，记忆里有 ${s.memories.size} 条可以复用的东西",
                    action = "把最近的对话整理成一条操作路线",
                    app = "其他",
                    tone = "info",
                    at = now
                )
            )
        }
        if (out.isEmpty()) return
        update { it.copy(suggestions = it.suggestions + out.take(3)) }
    }

    /* ─────────────────────────── Computer Use ─────────────────────────── */

    fun startRemote(goal: String) {
        val id = UUID.randomUUID().toString().take(8)
        val session = RemoteSession(
            id = id,
            goal = goal,
            status = "running",
            createdAt = System.currentTimeMillis(),
            steps = listOf(
                ComputerStep(System.currentTimeMillis(), "连接云端桌面", "分配一台干净实例", "ok"),
                ComputerStep(System.currentTimeMillis(), "打开浏览器", "载入目标站点", "running")
            )
        )
        update { s ->
            s.copy(
                remote = listOf(session) + s.remote,
                usage = s.usage.copy(sessions = s.usage.sessions + 1),
                chatMsgs = s.chatMsgs + ChatMessage(
                    "bot",
                    "已在云端电脑上开始：$goal。复杂网页任务走这条路。",
                    System.currentTimeMillis(),
                    page = "computer"
                )
            )
        }
    }

    /** 云端桌面步骤推进 */
    fun tickRemote() {
        val s = _state.value
        val active = s.remote.firstOrNull { it.status == "running" } ?: return
        val steps = active.steps.toMutableList()
        val last = steps.lastOrNull() ?: return
        if (last.state != "running") return

        val needsHuman = HUMAN_TRIGGERS.any { active.goal.contains(it) }
        if (needsHuman && steps.size >= 4 && !active.handedOver) {
            steps[steps.lastIndex] = last.copy(state = "needs-human", detail = "这一步要你本人处理")
            update { st ->
                st.copy(remote = st.remote.map {
                    if (it.id != active.id) it else it.copy(
                        status = "blocked",
                        needsHuman = "登录 / 验证 / 支付这类环节必须你亲手来"
                    )
                })
            }
            return
        }

        val seq = REMOTE_SEQ
        val idx = steps.size - 2
        if (idx >= seq.size) {
            // 收尾：最后一步核完，交回结果
            steps[steps.lastIndex] = last.copy(state = "ok")
            update { st ->
                st.copy(remote = st.remote.map {
                    if (it.id != active.id) it else it.copy(
                        steps = steps,
                        status = "done",
                        result = "已在云端桌面完成：${active.goal}。截图与操作记录已存档。"
                    )
                })
            }
            return
        }
        val (title, detail) = seq[idx]
        steps[steps.lastIndex] = last.copy(state = "ok")
        steps.add(ComputerStep(System.currentTimeMillis(), title, detail, "running"))
        update { st ->
            st.copy(remote = st.remote.map {
                if (it.id != active.id) it else it.copy(steps = steps)
            })
        }
    }

    /** 人工接管完成：交还云端实例，继续往下跑 */
    fun handoverRemote(id: String) = update { st ->
        st.copy(remote = st.remote.map {
            if (it.id != id) it else it.copy(
                status = "running",
                needsHuman = null,
                handedOver = true,
                steps = it.steps.map { s ->
                    if (s.state == "needs-human") s.copy(state = "ok", detail = "你已亲手处理，交还给它") else s
                }
            )
        })
    }

    fun finishRemote(id: String, result: String) = update { s ->
        s.copy(remote = s.remote.map {
            if (it.id != id) it else it.copy(status = "done", result = result)
        })
    }

    /* ─────────────────────────── 设置 ─────────────────────────── */

    fun toggleTheme() = update { it.copy(theme = if (it.theme == "dark") "light" else "dark") }

    fun setMode(mode: String) = update { it.copy(mode = mode) }

    fun setRuntime(rt: String) = update { it.copy(runtime = rt) }

    fun setAppAccess(app: String, allow: Boolean) = update { s ->
        s.copy(appAccess = s.appAccess + (app to allow))
    }

    fun toggleSkill(id: String) = update { s ->
        s.copy(skills = s.skills + (id to !(s.skills[id] ?: true)))
    }

    fun toggleMcp(id: String) = update { s ->
        s.copy(mcp = s.mcp + (id to !(s.mcp[id] ?: true)))
    }

    fun setCloud(endpoint: String, key: String) = update {
        it.copy(cloudEndpoint = endpoint, cloudKey = key, cloudHost = endpoint, cloudStatus = "unset")
    }

    /**
     * 真连接测试：真的去敲一次端点，而不是"填了地址就算通"。
     * 状态流转：testing → ok / error。
     * error 里带上真实原因（DNS 失败、超时、HTTP 码、非 2xx），
     * 让用户看到的是事实，不是安慰。
     */
    fun testCloud() {
        val endpoint = _state.value.cloudEndpoint.trim()
        if (endpoint.isBlank()) {
            update { it.copy(cloudStatus = "unset") }
            return
        }
        update { it.copy(cloudStatus = "testing") }
        scope?.launch(Dispatchers.IO) {
            val (ok, reason) = probeEndpoint(endpoint, _state.value.cloudKey)
            update {
                it.copy(
                    cloudStatus = if (ok) "ok" else "error",
                    cloudHost = endpoint,
                    cloudError = if (ok) null else reason
                )
            }
        }
    }

    /** 探活：GET 端点，8 秒超时。返回 (是否通, 失败原因) */
    private fun probeEndpoint(endpoint: String, key: String): Pair<Boolean, String?> {
        var conn: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URL(if (endpoint.startsWith("http")) endpoint else "https://$endpoint")
            conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                if (key.isNotBlank()) setRequestProperty("Authorization", "Bearer $key")
            }
            val code = conn.responseCode
            when {
                code in 200..299 -> true to null
                code in 300..399 -> false to "端点返回重定向 $code，请填最终地址"
                code == 401 || code == 403 -> false to "鉴权被拒（HTTP $code），检查 API Key"
                code == 404 -> false to "端点不存在（HTTP 404），检查地址是否写全"
                else -> false to "端点返回 HTTP $code"
            }
        } catch (e: java.net.UnknownHostException) {
            false to "域名解析失败：${e.message ?: "unknown host"}"
        } catch (e: java.net.SocketTimeoutException) {
            false to "连接超时（8 秒无响应）"
        } catch (e: Exception) {
            false to "连不上：${e.javaClass.simpleName}"
        } finally {
            try { conn?.disconnect() } catch (e: Exception) { }
        }
    }

    fun exportJson(): String = json.encodeToString(AppState.serializer(), _state.value)

    /* ─────────────────────────── 消息辅助 ─────────────────────────── */

    private fun pushUser(text: String) = update { s ->
        s.copy(chatMsgs = s.chatMsgs + ChatMessage("user", text, System.currentTimeMillis()))
    }

    private fun pushBot(text: String, kind: String = "reply") = update { s ->
        s.copy(chatMsgs = s.chatMsgs + ChatMessage("bot", text, System.currentTimeMillis(), kind = kind))
    }

    /* ── 轻提示 ─────────────────────────────────────────────
       走 Toast 而不是往聊天流里塞消息：
       临时性的系统反馈不应该污染那条连续的主链。 */

    fun notify(text: String) {
        scope?.launch(Dispatchers.Main) {
            Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    /** 用户选了附件：只记进主链，不建任务 —— 附件本身不是一个"要办的事" */
    fun attachFile(displayName: String) {
        pushUser("（附件）$displayName")
    }

    /** 读取用户选中的附件：取文件名与大小，供后续任务使用 */
    fun readPickedFile(uri: android.net.Uri): String {
        var name = "附件"
        var sizeKb = 0L
        try {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
                val sIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sIdx >= 0 && !c.isNull(sIdx)) sizeKb = c.getLong(sIdx) / 1024
            }
        } catch (e: Exception) {
            // 读不到元信息就用兜底名，不阻断主流程
        }
        return if (sizeKb > 0) "$name（${sizeKb}KB）" else name
    }

    companion object {
        /** 目标里出现这些环节时，云端电脑必须停下来交人工 */
        private val HUMAN_TRIGGERS = listOf(
            "登录", "登陆", "验证码", "注册", "订票", "付款", "支付", "下单", "实名"
        )

        /** 云端桌面的固定步骤序列（每步 850ms 推进一格） */
        private val REMOTE_SEQ = listOf(
            "读取页面结构" to "识别可点元素与表单字段",
            "填入内容" to "逐字输入，不粘贴",
            "核对结果" to "比对目标文案"
        )
    }
}
