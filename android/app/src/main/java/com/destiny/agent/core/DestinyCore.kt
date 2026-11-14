package com.destiny.agent.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Destiny 择优引擎 · 原生 Kotlin 移植版
 *
 * 逐节对应 Web 版 destiny-core.js（纯逻辑层，无 UI 依赖，可在 JVM 单测）。
 * 取舍依据见 [DECISIONS]：
 *   - 审批策略取 Mika core，�� mibot 的「超时默认拒绝」与「脱敏」
 *   - 条件触发全取 Mika core（baselineValue / lastFiredAtMs 状态机）
 *   - 虚拟屏取 core 的死锁检测 + mibot 的优先级插队
 *   - 网关全取 mibot（按真实探针分级熔断）
 *   - 红线 / 督查 / 身份 / MCP 最小权限为新写（两边原本只有静态文案）
 */
object DestinyCore {

    // ── 1. 审批策略 ────────────────────────────────────────────────────────
    const val RISK_READ = "Read"
    const val RISK_CHANGE = "Change"
    const val RISK_EXTERNAL = "External"
    const val RISK_IRREVERSIBLE = "Irreversible"
    const val RISK_UNKNOWN = "Unknown"

    const val MAX_GRANT_STEPS = 3
    const val CONFIRM_TIMEOUT_MS = 120_000L

    const val ALLOW = "Allow"
    const val DENY = "Deny"
    const val ASK = "Ask"

    /**
     * 纯函数审批策略。
     * @param payment 是否花钱操作
     * @param ask     需要人工接管的原因（captcha 等），null 表示无
     * @param attendance 在场状态：Attended / Unattended
     * @param origin 来源：Ui / Scheduled
     */
    fun approvalDecide(
        payment: Boolean,
        ask: String?,
        attendance: String,
        origin: String,
        autonomous: Boolean
    ): String {
        if (autonomous) return ALLOW
        if (!payment && ask == null) return ALLOW
        if (attendance == "Unattended" || origin != "Ui") return DENY
        return ASK
    }

    /** 借 VaultBroker.scrub：密码 / 卡号 / 邮箱一律脱敏 */
    fun scrub(text: String?): String {
        var s = text ?: ""
        s = Regex("(?:密码|口令|验证码|卡号)\\s*[:：]?\\s*\\S{4,}").replace(s, "[已脱敏]")
        s = Regex("\\b(\\d{16,19})\\b").replace(s, "[卡号已脱敏]")
        s = Regex("\\b([\\w.+-]+@[\\w-]+\\.[\\w.]+)\\b").replace(s, "[邮箱已脱敏]")
        return s
    }

    /** 一次性 Grant：绑定屏幕事实的一次性放行凭据 */
    class ConfirmationGate {
        private var pending: GateRequest? = null
        private var grant: Grant? = null
        private val declined = mutableSetOf<String>()
        private var pendingAt = 0L

        fun reset() {
            pending = null
            grant = null
            declined.clear()
            pendingAt = 0L
        }

        fun declare(req: GateRequest, foregroundApp: String?, now: Long): String {
            if (req.action == "pay") return "Unsupported"
            if (declined.contains(key(req))) return "Declined"
            if (foregroundApp.isNullOrEmpty() || foregroundApp != req.app) return "Mismatch"
            pending = req
            pendingAt = now
            return "Pending"
        }

        fun decide(choice: String, step: Int, now: Long): String? {
            val req = pending ?: return null
            pending = null
            if (now - pendingAt > CONFIRM_TIMEOUT_MS) {
                declined.add(key(req))
                return "TimedOutDeny"
            }
            if (choice != "Once") {
                declined.add(key(req))
                return "Deny"
            }
            grant = Grant(req, step)
            return "Granted"
        }

        /** 只放行紧接着那一个匹配操作；有同意不论结果都用掉它。 */
        fun permits(hit: Hit, screen: ScreenFact?, step: Int, typing: String?, seen: List<String> = emptyList()): Permit {
            val g = grant ?: return Permit("NoGrant", null)
            grant = null
            val r = g.req
            val s = screen
            fun shown(v: String?): Boolean = v != null && s?.text?.contains(v) == true
            fun seenShows(v: String?): Boolean =
                v != null && v.length >= 2 && seen.any { it.contains(v) }
            val reason: String? = when {
                hit.action == "pay" -> "付款永不在这里放行，必须人工接管"
                step - g.step > MAX_GRANT_STEPS -> "同意已过期（> $MAX_GRANT_STEPS 步）"
                hit.action != r.action -> "同意的是 ${r.action}，这一步是 ${hit.action}"
                s == null || s.app != r.app -> "前台 App 与申报不一致"
                !shown(r.target) && !seenShows(r.target) -> "屏上找不到申报对象「${r.target}」"
                r.action == "send" && r.content != (typing ?: "") -> "要发的字与同意时不完全一致"
                r.amount != null && !shown(r.amount) -> "屏上找不到申报金额 ${r.amount}"
                else -> null
            }
            return if (reason != null) Permit("Mismatch", reason) else Permit("Allowed", null)
        }

        /** 不在屏上的一步（工具自己声明要问），申报一字不差才放行。 */
        fun permitsExactly(ask: GateRequest, step: Int): Permit {
            val g = grant ?: return Permit("NoGrant", null)
            grant = null
            if (step - g.step > MAX_GRANT_STEPS) return Permit("Mismatch", "同意已过期")
            if (g.req != ask) return Permit("Mismatch", "同意的是另一条操作")
            return Permit("Allowed", null)
        }

        /** 打字 / 清空不作废同意，其余写操作（返回、点别处、开 App）作废。 */
        fun noteWrite(verb: String) {
            if (verb != "TypeText" && verb != "Clear") grant = null
        }

        private fun key(r: GateRequest) = "${r.action}|${r.app}"

        data class GateRequest(
            val action: String,
            val target: String,
            val content: String,
            val amount: String?,
            val app: String
        )

        data class Grant(val req: GateRequest, val step: Int)
        data class Hit(val action: String)
        data class ScreenFact(val app: String, val text: String)
        data class Permit(val permit: String, val reason: String?)
    }

    // ── 2. 红线：问也不许做 ───────────────────────────────────────────────
    data class RedLineAction(
        val text: String? = null,
        val target: String? = null,
        val amount: Double? = null
    )

    data class RedLineResult(
        val blocked: Boolean,
        val hits: List<String>,
        val reason: String?
    )

    private val RE_CREDENTIAL_BEFORE =
        Regex("(改|修改|重置|设置)[^。？]{0,6}(密码|口令)")
    private val RE_CREDENTIAL_AFTER =
        Regex("(密码|口令)[^。？]{0,6}(改|修改|重置|设置)")
    private val RE_CREDENTIAL_SCOPE = Regex("(银行|支付|账户|帐户|银行卡)")

    private val redLineRules: List<Pair<String, (RedLineAction) -> Boolean>> = listOf(
        "人机验证（滑块 / 点选）" to { a ->
            Regex("(滑块|人机验证|captcha|点选验证)", RegexOption.IGNORE_CASE).containsMatchIn(a.text ?: "")
        },
        "人脸或实名认证" to { a ->
            Regex("(人脸|活体|实名认证|身份证核验)").containsMatchIn(a.text ?: "")
        },
        "单笔超过 ¥500 的付款" to { a -> a.amount != null && a.amount > 500.0 },
        "修改系统安全设置" to { a ->
            a.target == "system" && Regex("(无障碍|无线调试|锁屏|安全设置)").containsMatchIn(a.text ?: "")
        },
        "改账户 / 银行 / 支付密码" to { a ->
            val t = a.text ?: ""
            (RE_CREDENTIAL_BEFORE.containsMatchIn(t) || RE_CREDENTIAL_AFTER.containsMatchIn(t)) &&
                RE_CREDENTIAL_SCOPE.containsMatchIn(t)
        }
    )

    fun redLineCheck(action: RedLineAction): RedLineResult {
        val hits = redLineRules.filter { it.second.invoke(action) }.map { it.first }
        return RedLineResult(
            blocked = hits.isNotEmpty(),
            hits = hits,
            reason = if (hits.isNotEmpty()) "红线：" + hits.joinToString(" / ") else null
        )
    }

    // ── 3. 事件账本 + 断点恢复 ───────────────────────────────────────────
    data class LedgerEvent(
        val seq: Int,
        val ts: Long,
        val runId: String,
        val kind: String,
        val payload: Map<String, String> = emptyMap(),
        val sideEffect: Boolean = false
    )

    data class Replayable(val stepIndex: Int, val replayable: Boolean, val writesSkipped: Int)

    data class RunSummary(
        val steps: Int,
        val writes: Int,
        val ms: Long,
        val tokens: Int,
        val calls: Int,
        val firstFeedbackMs: Long
    )

    class EventLedger {
        val events = mutableListOf<LedgerEvent>()
        var seq = 0
            private set

        fun append(runId: String, kind: String, payload: Map<String, String> = emptyMap(), sideEffect: Boolean = false): LedgerEvent {
            val e = LedgerEvent(++seq, System.currentTimeMillis(), runId, kind, payload, sideEffect)
            events.add(e)
            return e
        }

        fun forRun(runId: String): List<LedgerEvent> = events.filter { it.runId == runId }

        /** 重启把未结束的 run 标 Interrupted；不据此重放工具调用。 */
        fun interruptUnfinished(liveIds: List<String>): List<String> {
            val live = liveIds.toSet()
            val seen = mutableSetOf<String>()
            events.forEach { if (it.kind == "run_end" || it.kind == "run_interrupted") seen.add(it.runId) }
            val all = events.map { it.runId }.toSet()
            val out = all.filter { it !in seen && it !in live }.toMutableList()
            out.forEach { append(it, "run_interrupted", mapOf("reason" to "restart"), false) }
            return out
        }

        /** 可安全续跑的步号：最后一个「无副作用读步骤」；写步骤不重放。 */
        fun replayableFrom(runId: String): Replayable {
            val list = forRun(runId)
            var idx = -1
            for (e in list) {
                if (e.kind == "step" && !e.sideEffect) {
                    idx = e.payload["stepIndex"]?.toIntOrNull() ?: idx
                }
            }
            return Replayable(idx, idx >= 0, list.count { it.sideEffect })
        }

        /** 概要由纯函数从事件流累计 */
        fun summary(runId: String): RunSummary {
            val list = forRun(runId)
            var steps = 0; var writes = 0; var ms = 0L; var tokens = 0; var calls = 0
            var firstTs: Long? = null
            list.forEach { e ->
                if (firstTs == null) firstTs = e.ts
                if (e.kind == "step") {
                    steps++
                    ms += e.payload["ms"]?.toLongOrNull() ?: 0L
                    tokens += e.payload["tokens"]?.toIntOrNull() ?: 0
                    calls++
                }
                if (e.sideEffect) writes++
            }
            val first = firstTs ?: 0L
            val last = list.lastOrNull()?.ts ?: first
            return RunSummary(steps, writes, ms, tokens, calls, last - first)
        }
    }

    // ── 4. 条件触发 ──────────────────────────────────────────────────────
    object COND {
        const val NOT_MET = "condition_not_met"
        const val UNCHANGED = "condition_unchanged"
        const val COOLDOWN = "cooldown"
        const val PLAN_EDITED = "plan_edited"
    }

    data class ConditionDef(
        val conditionGoal: String,
        val notifyWhen: String = "becomes_met",
        val cooldownMs: Long? = null,
        val reminderText: String = "",
        val times: List<String> = emptyList(),
        val kind: String = "daily",
        val lastConditionMet: Boolean? = null,
        val lastValue: String? = null,
        val baselineValue: String? = null,
        val baselineAtMs: Long? = null,
        val lastFiredAtMs: Long? = null
    )

    data class ConditionOutcome(val met: Boolean, val value: String? = null)

    data class ConditionResult(val fire: Boolean, val reason: String?, val next: ConditionDef)

    fun afterCheck(def: ConditionDef, outcome: ConditionOutcome, nowMs: Long, slotMs: Long = nowMs): ConditionResult {
        val want = outcome.met && (def.notifyWhen == "each_time_met" || def.lastConditionMet != true)
        val seeded = if (def.baselineValue == null && outcome.value != null) {
            def.copy(baselineValue = outcome.value, baselineAtMs = nowMs)
        } else def
        val observed = seeded.copy(lastValue = outcome.value)
        val last = def.lastFiredAtMs
        val cooling = def.cooldownMs != null && last != null && nowMs >= last && (slotMs - last) < def.cooldownMs
        if (want && cooling) return ConditionResult(false, COND.COOLDOWN, observed)
        if (want) {
            return ConditionResult(
                true, null,
                observed.copy(
                    lastConditionMet = true,
                    lastFiredAtMs = if (last != null && slotMs < last && nowMs >= last) last else slotMs,
                    baselineValue = outcome.value ?: observed.baselineValue,
                    baselineAtMs = if (outcome.value != null) nowMs else observed.baselineAtMs
                )
            )
        }
        return ConditionResult(
            false,
            if (outcome.met) COND.UNCHANGED else COND.NOT_MET,
            observed.copy(lastConditionMet = outcome.met)
        )
    }

    fun revertFire(nowDef: ConditionDef, before: ConditionDef): ConditionDef = nowDef.copy(
        lastConditionMet = false,
        lastFiredAtMs = before.lastFiredAtMs,
        baselineValue = before.baselineValue ?: nowDef.baselineValue,
        baselineAtMs = if (before.baselineValue != null) before.baselineAtMs else nowDef.baselineAtMs
    )

    /** 条件规则一改，历史基线失效。 */
    fun withConditionHistoryFrom(next: ConditionDef, prev: ConditionDef): ConditionDef {
        val same = next.conditionGoal == prev.conditionGoal && next.notifyWhen == prev.notifyWhen &&
            next.cooldownMs == prev.cooldownMs && next.reminderText == prev.reminderText
        return if (!same) {
            next.copy(
                lastConditionMet = null, lastValue = null, baselineValue = null,
                baselineAtMs = null, lastFiredAtMs = null
            )
        } else {
            next.copy(
                lastConditionMet = prev.lastConditionMet, lastValue = prev.lastValue,
                baselineValue = prev.baselineValue, baselineAtMs = prev.baselineAtMs,
                lastFiredAtMs = prev.lastFiredAtMs
            )
        }
    }

    data class Upcoming(val at: Long, val label: String)

    /** 预览与保存共用同一转换，避免「建完等一天才发现时区错了」。 */
    fun upcoming(def: ConditionDef, nowMs: Long, count: Int = 3): List<Upcoming> {
        val times = (if (def.times.isNotEmpty()) def.times else listOf("09:00")).sorted()
        val out = mutableListOf<Upcoming>()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
        var d = 0
        while (d < 10 && out.size < count) {
            val base = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMs + d * 86_400_000L
            }
            if (def.kind == "weekly" && base.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
                d++; continue
            }
            for (t in times) {
                if (out.size >= count) break
                val parts = t.split(":")
                val dt = java.util.Calendar.getInstance().apply {
                    timeInMillis = base.timeInMillis
                    set(java.util.Calendar.HOUR_OF_DAY, (parts.getOrNull(0) ?: "9").toIntOrNull() ?: 9)
                    set(java.util.Calendar.MINUTE, (parts.getOrNull(1) ?: "0").toIntOrNull() ?: 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                if (dt.timeInMillis <= nowMs) continue
                out.add(
                    Upcoming(
                        dt.timeInMillis,
                        "${dt.get(java.util.Calendar.MONTH) + 1}/${dt.get(java.util.Calendar.DAY_OF_MONTH)} " +
                            "%02d:%02d".format(
                                dt.get(java.util.Calendar.HOUR_OF_DAY),
                                dt.get(java.util.Calendar.MINUTE)
                            )
                    )
                )
            }
            d++
        }
        return out
    }

    // ── 5. 虚拟屏 / App 锁 ───────────────────────────────────────────────
    sealed interface Acquire {
        data class Ok(val slot: Int) : Acquire
        data class Wait(val wait: String, val holder: String?) : Acquire
        data class Deadlock(val cycle: List<String>) : Acquire
    }

    data class PoolTask(val id: String, val app: String, val priority: Int = 0)

    class ScreenPool(val size: Int = 4) {
        val slots = arrayOfNulls<String>(size)
        val locks = mutableMapOf<String, String>()
        val waitFor = mutableMapOf<String, String>()
        /** 占用记录：id → 槽位 → app（对齐 JS 原版 order.push({id, slot, app})） */
        val order = mutableListOf<Triple<String, Int, String>>()

        fun acquire(task: PoolTask): Acquire {
            val app = task.app
            val id = task.id
            val holder = locks[app]
            if (holder != null && holder != id) {
                waitFor[id] = holder
                val cycle = findCycle(id)
                if (cycle != null) {
                    waitFor.remove(id)
                    return Acquire.Deadlock(cycle)
                }
                return Acquire.Wait("app", holder)
            }
            var slot = -1
            for (i in slots.indices) if (slots[i] == null) { slot = i; break }
            if (slot < 0) return Acquire.Wait("slot", null)
            slots[slot] = id
            locks[app] = id
            order.add(Triple(id, slot, app))
            waitFor.remove(id)
            return Acquire.Ok(slot)
        }

        /** 互等：A 等 B、B 等 A */
        fun findCycle(start: String): List<String>? {
            val seen = mutableSetOf<String>()
            val chain = mutableListOf<String>()
            var cur: String? = start
            while (cur != null && !seen.contains(cur)) {
                seen.add(cur)
                chain.add(cur)
                cur = waitFor[cur]
            }
            if (cur != null && chain.contains(cur)) {
                val out = chain.subList(chain.indexOf(cur), chain.size).toMutableList()
                out.add(cur)
                return out
            }
            return null
        }

        /** 反序释放；重复调用不报错。 */
        fun release(id: String) {
            for (i in order.indices.reversed()) {
                if (order[i].first != id) continue
                val e = order[i]
                if (slots[e.second] == id) slots[e.second] = null
                if (locks[e.third] == id) locks.remove(e.third)
                order.removeAt(i)
            }
            waitFor.remove(id)
        }

        /** 优先级插队：同优先组内 FIFO */
        fun enqueue(list: MutableList<PoolTask>, task: PoolTask): MutableList<PoolTask> {
            val at = if (task.priority > 0) {
                val idx = list.indexOfFirst { it.priority <= task.priority }
                if (idx < 0) list.size else idx
            } else list.size
            list.add(at, task)
            return list
        }
    }

    // ── 6. 自检：依赖 DAG + 跳过写明原因 ────────────────────────────────
    data class Check(val key: String, val name: String, val needs: List<String>, val ms: Int, val device: Boolean)

    val CHECKS: List<Check> = listOf(
        Check("device", "机型 / 系统 / 架构", emptyList(), 5000, true),
        Check("permissions", "权限与电池", emptyList(), 5000, true),
        Check("helper", "内置助手", listOf("device"), 15000, true),
        Check("virtual_display", "虚拟屏 / 焦点", listOf("helper"), 10000, true),
        Check("app_launch", "在虚拟屏打开 App", listOf("permissions", "virtual_display"), 20000, true),
        Check("screen_reading", "读屏 / 截图非黑屏", listOf("app_launch"), 10000, true),
        Check("actions", "点按 / 中英输入 / 滚动", listOf("screen_reading"), 20000, true),
        Check("stays_alive", "持续运行十几秒", listOf("screen_reading"), 25000, true),
        Check("main_screen", "主屏任务没被抢", listOf("virtual_display"), 5000, true),
        Check("browser", "内置浏览器读写", emptyList(), 15000, true),
        Check("sandbox", "沙箱 echo + Python", emptyList(), 20000, true),
        Check("app_copies", "分身 / 工作资料数量", listOf("helper"), 10000, true)
    )

    data class SelfCheckResult(
        val status: Map<String, String>,
        val budgetMs: Int,
        val usedMs: Int,
        val released: Boolean
    )

    /** 未给结果的设备项一律 Partial(需真机)，不谎报通过。 */
    fun runSelfCheck(results: Map<String, String>, budgetMs: Int = 90_000): SelfCheckResult {
        val status = linkedMapOf<String, String>()
        var used = 0
        CHECKS.forEach { c ->
            val missing = c.needs.filter { status[it] != "Pass" && status[it] != "Partial" }
            if (missing.isNotEmpty()) {
                status[c.key] = "Skipped: 缺 ${missing.joinToString(", ")}"
                return@forEach
            }
            if (used + c.ms > budgetMs) {
                status[c.key] = "Skipped: 总时限用尽"
                return@forEach
            }
            val r = results[c.key]
            if (r == null && c.device) {
                status[c.key] = "Skipped: 需真机"
                return@forEach
            }
            used += Math.round(c.ms * 0.15f)
            status[c.key] = if (r == "Pass" || r == "Fail" || r == "Partial") r else "Fail"
        }
        return SelfCheckResult(status, budgetMs, used, true)
    }

    // ── 7. 网关：分级熔断 ───────────────────────────────────────────────
    object COOLDOWN {
        const val RATE = 120_000L
        const val QUOTA = 600_000L
        const val NETWORK = 30_000L
        const val REGION = 1_800_000L
        const val OTHER = 600_000L
    }

    data class GatewayState(
        var failures: Int = 0,
        var consecutive: Int = 0,
        var cooldownUntil: Long = 0,
        var dead: Boolean = false,
        var okCount: Int = 0,
        var latencyMs: Long = 0,
        var kind: String = "OK",
        var lastError: String? = null,
        var lastCheckAt: Long = 0,
        var regionsDown: MutableMap<String, Long> = mutableMapOf()
    )

    class GatewayHealth {
        val map = mutableMapOf<String, GatewayState>()

        fun state(id: String): GatewayState = map.getOrPut(id) { GatewayState() }

        fun recordFailure(id: String, kind: String, reason: String?, now: Long, region: String? = null): GatewayState {
            val s = state(id)
            s.failures++; s.consecutive++; s.kind = kind
            s.lastError = scrub(reason ?: "").take(200)
            if (kind == "AUTH") s.dead = true
            if (kind == "REGION" && region != null) s.regionsDown[region] = now + COOLDOWN.REGION
            val cool = if (kind == "OTHER") {
                min(COOLDOWN.OTHER, 30_000L * 2.0.pow(min(s.consecutive, 5)).toLong())
            } else {
                when (kind) {
                    "RATE" -> COOLDOWN.RATE
                    "QUOTA" -> COOLDOWN.QUOTA
                    "NETWORK" -> COOLDOWN.NETWORK
                    "REGION" -> COOLDOWN.REGION
                    else -> COOLDOWN.OTHER
                }
            }
            s.cooldownUntil = now + cool
            return s
        }

        fun recordProbe(id: String, latencyMs: Long, now: Long): GatewayState {
            val s = state(id)
            s.consecutive = 0; s.cooldownUntil = 0; s.dead = false; s.okCount++
            s.kind = "OK"; s.latencyMs = latencyMs; s.lastCheckAt = now
            return s
        }

        fun isUsable(id: String, now: Long, region: String? = null): Boolean {
            val s = state(id)
            if (s.dead || s.kind == "AUTH") return false
            if (now < s.cooldownUntil) return false
            if (region != null && (s.regionsDown[region] ?: 0L) > now) return false
            return true
        }

        /** 按真实探针延迟择优 */
        fun pickBest(ids: List<String>, now: Long, region: String? = null): String? =
            ids.filter { isUsable(it, now, region) }
                .sortedBy { state(it).latencyMs }
                .firstOrNull()

        /** 自愈阶梯：重试退避 → DoH → 镜像，任一步成功即停 */
        fun repairLadder(id: String, probes: List<() -> Boolean>): Pair<String?, String> {
            val steps = listOf("retry-backoff", "dns-over-https", "mirror-base-url")
            for (i in steps.indices) {
                val ok = probes.getOrNull(i)?.invoke() ?: false
                if (ok) {
                    recordProbe(id, 0, System.currentTimeMillis())
                    return steps[i] to "FIXED"
                }
            }
            return null to "FAILED"
        }
    }

    // ── 8. 能力装配 + 确定性路由 ─────────────────────────────────────────
    data class Capability(val id: String, val kind: String, val state: String, val note: String = "")

    data class Assembly(
        val capabilities: List<Capability>,
        var groupsCollapsed: Boolean,
        var expandsUsed: Int
    )

    fun assemble(capabilities: List<String>, servers: List<McpServer>): Assembly {
        val list = mutableListOf<Capability>()
        (if (capabilities.isEmpty()) listOf("phone", "web", "sandbox") else capabilities)
            .forEach { list.add(Capability(it, "builtin", "ready")) }
        servers.forEach { s ->
            list.add(
                Capability(
                    "mcp:" + s.id, "mcp",
                    if (s.on == false) "degraded" else "ready",
                    if (s.on == false) "单服务失败，只摘自己，不影响其余能力" else s.perm
                )
            )
        }
        return Assembly(list, true, 0)
    }

    fun expandGroup(asm: Assembly, groupId: String): Pair<Boolean, String> {
        if (asm.expandsUsed >= 1) return false to "请求前缀最多失效一次"
        asm.expandsUsed++
        asm.groupsCollapsed = false
        return true to "首次用到才展开全组，省 token"
    }

    data class McpServer(val id: String, val on: Boolean? = true, val perm: String = "", val readOnly: Boolean = false, val scope: String? = null)

    data class RouteChain(val kind: String, val step: String)

    data class RouteResult(
        val chain: List<RouteChain>,
        val why: String,
        val capabilities: List<String>,
        val deterministic: Boolean = true
    )

    private val ROUTE_RULES: List<Pair<Regex, Triple<List<RouteChain>, String, List<String>>>> = listOf(
        Regex("算|统计|多少|几|加|减|汇总") to Triple(
            listOf(RouteChain("技能", "意图门控"), RouteChain("MCP", "sqlite"), RouteChain("—", "写回会话")),
            "算数一律走 sqlite，不让模型心算；因此不打开浏览器，也不调用外部搜索。",
            listOf("sandbox", "mcp:sqlite")
        ),
        Regex("搜|查一下|最新|新闻|天气") to Triple(
            listOf(RouteChain("技能", "意图门控"), RouteChain("技能", "网页搜索"), RouteChain("APP", "内置浏览器"), RouteChain("—", "写回会话")),
            "需要外部最新信息才开浏览器；免费源 100 次/天没用完，不用 Jina / Exa。搜索内容按不可信处理。",
            listOf("web")
        ),
        Regex("文件|存|整理|导出") to Triple(
            listOf(RouteChain("技能", "意图门控"), RouteChain("MCP", "filesystem"), RouteChain("技能", "读屏 OCR"), RouteChain("—", "写回会话")),
            "filesystem 只放行 Downloads/Destiny 与工作区；不需要网页，所以不开浏览器。",
            listOf("sandbox", "mcp:filesystem")
        ),
        Regex("付款|下单|买|支付|转账") to Triple(
            listOf(RouteChain("技能", "意图门控"), RouteChain("闸门", "授权 · hand_to_user"), RouteChain("APP", "目标 App"), RouteChain("—", "写回会话")),
            "花钱操作不进自动放行，走人工接管；自主模式也只留授权记录。",
            listOf("phone")
        )
    )

    fun route(goal: String): RouteResult {
        val hit = ROUTE_RULES.firstOrNull { it.first.containsMatchIn(goal) }
        val t = hit?.second ?: Triple(
            listOf(RouteChain("技能", "意图门控"), RouteChain("技能", "操作路线"), RouteChain("APP", "目标 App"), RouteChain("—", "写回会话")),
            "这件事以前办成过，按沉淀的路线走更快；路线失效会自己停用，不硬跑。",
            listOf("phone")
        )
        return RouteResult(t.first, t.second, t.third)
    }

    // ── 9. 督查与检阅 ────────────────────────────────────────────────────
    data class ReviewItem(
        val runId: String,
        val title: String,
        val selfChecked: Boolean,
        var state: String,
        val at: Long
    )

    class Supervisor {
        val items = mutableListOf<ReviewItem>()

        fun submit(runId: String, title: String, selfChecked: Boolean): ReviewItem {
            val it = ReviewItem(runId, title, selfChecked, "pending", System.currentTimeMillis())
            items.add(0, it)
            return it
        }

        fun review(runId: String, decision: String): ReviewItem? {
            val it = items.firstOrNull { it.runId == runId && it.state == "pending" } ?: return null
            it.state = if (decision == "accept") "accepted" else "rejected"
            return it
        }

        fun rate(): Int {
            val fin = items.filter { it.state != "pending" }
            if (fin.isEmpty()) return 0
            return Math.round(fin.count { it.state == "accepted" } * 100.0 / fin.size).toInt()
        }
    }

    // ── 10. 身份隔离：身份 → 组 → 会话 ──────────────────────────────────
    class Identity {
        val groups = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        val risk = mutableMapOf<String, Long>()

        fun bind(identity: String, group: String, session: String) {
            val g = groups.getOrPut(identity) { mutableMapOf() }
            g.getOrPut(group) { mutableListOf() }.add(session)
        }

        fun sessionsOf(identity: String): List<String> =
            groups[identity]?.values?.flatten() ?: emptyList()

        fun flagRisk(session: String, now: Long) {
            risk[session] = now + 86_400_000L
        }

        /** 一个号被风控 → 同身份下别的号停 24h，但不波及其他身份。 */
        fun holdFor(identity: String, now: Long): Triple<Boolean, Long?, List<String>> {
            val sessions = sessionsOf(identity)
            val hit = sessions.filter { (risk[it] ?: 0L) > now }
            val until = if (hit.isEmpty()) null else hit.maxOf { risk[it] ?: 0L }
            return Triple(hit.isNotEmpty(), until, sessions)
        }
    }

    // ── 11. MCP 最小权限 ─────────────────────────────────────────────────
    private val WRITE_TOOLS = Regex("^(write|delete|create|update|push|send|exec|run|set_|remove)", RegexOption.IGNORE_CASE)

    data class McpDecision(val decision: String, val reason: String)

    fun mcpCheck(server: McpServer?, tool: String?, argsPath: String?, always: Boolean = false): McpDecision {
        val write = tool != null && WRITE_TOOLS.containsMatchIn(tool)
        if (server?.readOnly == true && write) {
            return McpDecision("Deny", "该服务登记为只读，写工具一律拒绝")
        }
        if (write && always) return McpDecision("Allow", "已登记 ALWAYS")
        if (write) return McpDecision("Ask", "写操作每次批准")
        val scope = server?.scope
        if (scope != null && argsPath != null && !argsPath.startsWith(scope)) {
            return McpDecision("Deny", "路径超出登记范围 $scope")
        }
        return McpDecision("Allow", "只读且在放行范围内")
    }

    // ── 12. 记忆：做法待批准 + 撤销令牌 ──────────────────────────────────
    data class MemoryItem(
        val id: String,
        var text: String,
        var kind: String,
        var status: String,
        var source: String,
        var v: Int,
        val at: Long
    )

    data class UndoToken(val id: String, val v: Int)

    class Memory {
        val items = mutableListOf<MemoryItem>()
        val undos = mutableMapOf<String, UndoToken>()
        val audit = mutableListOf<Map<String, String>>()

        fun add(text: String, kind: String, source: String = "manual"): MemoryItem {
            val item = MemoryItem(
                id = "m" + System.currentTimeMillis() + Random.nextInt(100, 999),
                text = scrub(text),
                kind = kind,
                status = if (kind == "procedure") "pending" else "active",
                source = source,
                v = 1,
                at = System.currentTimeMillis()
            )
            items.add(0, item)
            audit.add(mapOf("at" to item.at.toString(), "act" to "add", "id" to item.id, "text" to item.text))
            return item
        }

        fun approve(id: String): MemoryItem? {
            val m = get(id) ?: return null
            m.status = "active"; m.v++
            audit.add(mapOf("at" to System.currentTimeMillis().toString(), "act" to "approve", "id" to id))
            return m
        }

        fun update(id: String, text: String, source: String): MemoryItem? {
            val m = get(id) ?: return null
            m.text = scrub(text); m.v++
            // 非手写改动已批准的「做法」要打回 pending（reapprove）
            if (m.kind == "procedure" && source != "manual" && m.status == "active") m.status = "pending"
            audit.add(mapOf("at" to System.currentTimeMillis().toString(), "act" to "update", "id" to id, "source" to source))
            return m
        }

        fun takeUndo(id: String): String? {
            val m = get(id) ?: return null
            val token = "u" + undos.size
            undos[token] = UndoToken(id, m.v)
            if (undos.size > 50) {
                val first = undos.keys.first()
                undos.remove(first)
            }
            return token
        }

        fun undo(token: String): Pair<Boolean, String> {
            val t = undos[token] ?: return false to "撤销令牌不存在"
            val m = get(t.id)
            undos.remove(token)
            if (m == null || m.v != t.v) return false to "之后被改过，拒绝撤销"
            items.removeAll { it.id == t.id }
            return true to "已撤销"
        }

        fun get(id: String): MemoryItem? = items.firstOrNull { it.id == id }

        fun context(mode: String, limit: Int = 20): List<MemoryItem> {
            val active = items.filter { it.status == "active" }
            val proc = active.filter { it.kind == "procedure" }.take(3)
            val rest = active.filter { it.kind != "procedure" }
            val head = if (mode == "Auto" && active.size <= limit) rest else rest.take(limit)
            return head + proc
        }
    }

    // ── 取舍矩阵（界面直接读，逐项可核对） ──────────────────────────────
    data class Decision(
        val id: String,
        val topic: String,
        val winner: String,
        val take: String,
        val drop: String,
        val from: String,
        val why: String
    )

    val DECISIONS: List<Decision> = listOf(
        Decision(
            "approval", "审批 / 授权闸门", "Mika core + 借 mibot 两点",
            "ApprovalPolicy 纯函数分级 + ConfirmationGate 一次性 Grant（绑定屏幕事实、3 步过期、写操作作废）",
            "VaultBroker.isHighRisk 关键词表、第三套 ConfigConfirmationGate",
            "CORE/action/Approval.kt · CORE/gate/ConfirmationGate.kt · APP/mibot/vault/VaultBroker.kt",
            "关键词判定易绕过；纯函数可测，Grant 绑定前台 App/对象/金额/发送内容更严。借 mibot 的 120s 超时默认拒绝与脱敏。"
        ),
        Decision(
            "ledger", "运行记录 / 断点恢复（两边冲突）", "Mika core 为主，折中",
            "events 事件即账本 + 重启标 Interrupted；恢复只从「无副作用读步骤」接，写步骤一律不重放",
            "TaskRunQueue 无条件从 breakpoint 续跑（会重放工具调用）",
            "CORE/record/RunEvent.kt · CORE/run/RunStore.kt · APP/mibot/queue/TaskRunQueue.kt",
            "core 明确拒绝中途续跑是怕重放副作用；折中保住 mibot 的断点价值又守住安全底线。"
        ),
        Decision(
            "trigger", "定时 / 条件任务", "Mika core 全胜",
            "afterCheck 与上次基线比较、按计划时段冷却不补跑、revertFire 回滚、预览与保存同源",
            "APP/scheduled/ScheduledTask.kt 只有日历/interval，没有条件语义",
            "CORE/trigger/ConditionCheck.kt · CORE/trigger/Triggers.kt · CORE/trigger/PlanEditor.kt",
            "「降价超过 50 块告诉我」比的是上次触发时的值，core 才有 baselineValue/lastFiredAtMs/slotMs 完整状态机。"
        ),
        Decision(
            "screen", "虚拟屏 / App 锁", "core 死锁检测 + mibot 优先级",
            "awaitAppFree 互等检测 WouldDeadlock、ResourceScope 建好即登记 / 反序释放 / 幂等",
            "APP/mibot/phone/Vd* 裸锁无互等检测",
            "CORE/uistate/ScreenPort.kt · CORE/resource/ResourceScope.kt · APP/mibot/queue/TaskRunQueue.kt",
            "core 能当场认出两个任务互相等；补 mibot 的 priority>0 插队，解决 core 缺公平队列。"
        ),
        Decision(
            "selfcheck", "兼容自检", "Mika core 全胜",
            "12 项 + needs 依赖 DAG，依赖没过就 Skipped 并写明缺哪项，逐项超时 + 90s 总预算 + 幂等 release",
            "APP/diagnostics/* 聚合页与 VdSelfCheck 单项（无依赖，失败会硬跑后续）",
            "CORE/selfcheck/SelfCheck.kt · CORE/selfcheck/SelfCheckRunner.kt",
            "虚拟屏建不了时继续测「点按/打字」只会得到一串假失败；依赖跳过才诚实。"
        ),
        Decision(
            "gateway", "网关 / 供应商切换", "mibot 全胜",
            "GatewayHealth 分级冷却 RATE 120s / QUOTA 600s / NETWORK 30s / AUTH dead / REGION 区域 30min + DoH→镜像自愈",
            "CORE/agent/ModelRouter.kt 只按任务类型分主/轻模型，不看健康",
            "APP/mibot/gateway/Gateway.kt · APP/mibot/gateway/GatewayNet.kt",
            "状态来自最近一次真实探针而非「跑过检查就算 OK」，区域粒度熔断、重启不丢。"
        ),
        Decision(
            "assembly", "能力装配 / 路由", "Mika core + 补确定性",
            "能力白名单、每 MCP server 一个能力、单服务失败只摘自己、工具组懒展开",
            "DshToolRegistry（注释自认 Not wired）、CapabilityBus 无类型 Map",
            "CORE/assembly/RunAssembler.kt · CORE/tool/ToolGroups.kt · APP/mibot/plugins/DshToolRegistry.kt",
            "DSH 是半成品；core 错误隔离好，但选择全靠提示词自觉 → 补 ROUTE_RULES 确定性路由。"
        ),
        Decision(
            "gap", "两边都缺的 4 项（新写）", "Destiny 新写",
            "红线可执行校验 / 督查检阅状态机 / 身份三层隔离与风控联动 / MCP 最小权限判定",
            "V7Elem 静态文案（v7:1574 红线、v7:1286 督查、v7:1107 身份指纹、spec:1462 MCP）",
            "APP/mibot/v7/DestinyV7Screens.kt · APP/mibot/spec/DestinySpecScreens.kt",
            "这些只存在于 56 屏规格稿里，没有数据表也没有校验函数，等于没实现。"
        )
    )
}
