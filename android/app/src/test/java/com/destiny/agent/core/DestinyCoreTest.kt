package com.destiny.agent.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Destiny 择优引擎单测 —— 原生 Kotlin 版
 *
 * 逐条对应 Web 版 _coretest.js 的 62 项断言。
 * 引擎是纯逻辑层，不依赖 Android，所以能在 JVM 上直接跑。
 */
class DestinyCoreTest {

    private val now = 1_700_000_000_000L

    private fun req() = DestinyCore.ConfirmationGate.GateRequest(
        action = "send",
        target = "项目群",
        content = "日报",
        amount = null,
        app = "飞书"
    )

    private fun screen() = DestinyCore.ConfirmationGate.ScreenFact("飞书", "项目群  日报")

    /* ── 1. 审批策略 ─────────────────────────────────────────────────── */

    @Test
    fun 普通动作直接放行() {
        assertEquals(
            DestinyCore.ALLOW,
            DestinyCore.approvalDecide(false, null, "Attended", "Ui", false)
        )
    }

    @Test
    fun 付款询问() {
        assertEquals(
            DestinyCore.ASK,
            DestinyCore.approvalDecide(true, null, "Attended", "Ui", false)
        )
    }

    @Test
    fun 无人值守或定时来源直接拒绝() {
        assertEquals(
            DestinyCore.DENY,
            DestinyCore.approvalDecide(true, null, "Unattended", "Ui", false)
        )
        assertEquals(
            DestinyCore.DENY,
            DestinyCore.approvalDecide(true, null, "Attended", "Scheduled", false)
        )
    }

    @Test
    fun 自主模式连付款也不先问() {
        assertEquals(
            DestinyCore.ALLOW,
            DestinyCore.approvalDecide(true, null, "Ui", "Ui", true)
        )
    }

    /* ── 2. 一次性 Grant ─────────────────────────────────────────────── */

    @Test
    fun 前台不符不交给用户() {
        val g = DestinyCore.ConfirmationGate()
        assertEquals("Mismatch", g.declare(req(), "微信", now))
    }

    @Test
    fun 申报成功进入Pending() {
        val g = DestinyCore.ConfirmationGate()
        assertEquals("Pending", g.declare(req(), "飞书", now))
    }

    @Test
    fun 同意记一次性Grant() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        assertEquals("Granted", g.decide("Once", 2, now))
    }

    @Test
    fun 绑定屏幕事实才放行() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        val p = g.permits(
            DestinyCore.ConfirmationGate.Hit("send"),
            screen(),
            3,
            "日报"
        )
        assertEquals("Allowed", p.permit)
    }

    @Test
    fun 超过三步同意作废() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        val p = g.permits(
            DestinyCore.ConfirmationGate.Hit("send"),
            screen(),
            9,
            "日报"
        )
        assertEquals("Mismatch", p.permit)
    }

    @Test
    fun 付款永不在这里放行() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        val p = g.permits(
            DestinyCore.ConfirmationGate.Hit("pay"),
            screen(),
            3,
            ""
        )
        assertTrue((p.reason ?: "").contains("付款"))
    }

    @Test
    fun 打字以外的写操作作废同意() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        g.noteWrite("Back")
        val p = g.permits(
            DestinyCore.ConfirmationGate.Hit("send"),
            screen(),
            3,
            "日报"
        )
        assertEquals("NoGrant", p.permit)
    }

    @Test
    fun 打字不清空同意() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        g.noteWrite("TypeText")
        val p = g.permits(
            DestinyCore.ConfirmationGate.Hit("send"),
            screen(),
            3,
            "日报"
        )
        assertEquals("Allowed", p.permit)
    }

    @Test
    fun 超时默认拒绝() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        assertEquals("TimedOutDeny", g.decide("Once", 2, now + 120_001))
    }

    @Test
    fun 屏上工具声明需一字不差() {
        val g = DestinyCore.ConfirmationGate()
        g.declare(req(), "飞书", now)
        g.decide("Once", 2, now)
        assertEquals("Allowed", g.permitsExactly(req(), 3).permit)
    }

    @Test
    fun 付款申报走接管而非确认() {
        val g = DestinyCore.ConfirmationGate()
        val pay = DestinyCore.ConfirmationGate.GateRequest("pay", "", "", null, "飞书")
        assertEquals("Unsupported", g.declare(pay, "飞书", now))
    }

    @Test
    fun 脱敏卡号与验证码被遮() {
        val out = DestinyCore.scrub("验证码 882913 卡号 6222021234567890123")
        assertFalse(out.contains("882913"))
        assertFalse(out.contains("6222021234567890123"))
    }

    /* ── 3. 红线 ─────────────────────────────────────────────────────── */

    @Test
    fun 红线人机验证() {
        assertTrue(DestinyCore.redLineCheck(DestinyCore.RedLineAction(text = "请完成滑块验证")).blocked)
    }

    @Test
    fun 红线人脸实名() {
        assertTrue(DestinyCore.redLineCheck(DestinyCore.RedLineAction(text = "需要人脸核验")).blocked)
    }

    @Test
    fun 红线单笔超五百() {
        assertTrue(
            DestinyCore.redLineCheck(DestinyCore.RedLineAction(text = "付款", amount = 501.0)).blocked
        )
    }

    @Test
    fun 红线系统安全设置() {
        assertTrue(
            DestinyCore.redLineCheck(
                DestinyCore.RedLineAction(text = "打开无障碍", target = "system")
            ).blocked
        )
    }

    @Test
    fun 普通发消息不触红线() {
        assertFalse(
            DestinyCore.redLineCheck(
                DestinyCore.RedLineAction(text = "发日报到项目群", amount = 0.0)
            ).blocked
        )
    }

    @Test
    fun 改账户密码也触红线() {
        assertTrue(
            DestinyCore.redLineCheck(
                DestinyCore.RedLineAction(text = "帮我修改支付宝账户密码")
            ).blocked
        )
    }

    /* ── 4. 事件账本 + 安全断点 ──────────────────────────────────────── */

    private fun ledger(): DestinyCore.EventLedger {
        val l = DestinyCore.EventLedger()
        l.append("r1", "run_start", emptyMap(), false)
        l.append("r1", "step", mapOf("stepIndex" to "0", "ms" to "100", "tokens" to "90"), false)
        l.append("r1", "step", mapOf("stepIndex" to "1", "ms" to "200", "tokens" to "120"), true)
        l.append("r1", "step", mapOf("stepIndex" to "2", "ms" to "150", "tokens" to "110"), false)
        return l
    }

    @Test
    fun 概要由事件流纯函数累计() {
        val s = ledger().summary("r1")
        assertEquals(3, s.steps)
        assertEquals(1, s.writes)
        assertEquals(320, s.tokens)
    }

    @Test
    fun 断点只落在无副作用读步骤() {
        val safe = ledger().replayableFrom("r1")
        assertEquals(2, safe.stepIndex)
        assertTrue(safe.replayable)
        assertEquals(1, safe.writesSkipped)
    }

    @Test
    fun 重启把未完成run标中断() {
        val l = ledger()
        assertTrue(l.interruptUnfinished(emptyList()).contains("r1"))
        assertFalse(l.interruptUnfinished(emptyList()).contains("r1"))
    }

    /* ── 5. 条件触发 ─────────────────────────────────────────────────── */

    private fun conditionDef() = DestinyCore.ConditionDef(
        conditionGoal = "降价超过 50",
        notifyWhen = "becomes_met",
        cooldownMs = 86_400_000L,
        times = listOf("08:00"),
        kind = "daily"
    )

    @Test
    fun 首次不满足不触发但值成为基线() {
        val r = DestinyCore.afterCheck(
            conditionDef(),
            DestinyCore.ConditionOutcome(false, "¥399"),
            now,
            now
        )
        assertFalse(r.fire)
        assertEquals("¥399", r.next.baselineValue)
    }

    @Test
    fun 不满足变满足才触发() {
        val first = DestinyCore.afterCheck(
            conditionDef(),
            DestinyCore.ConditionOutcome(false, "¥399"),
            now,
            now
        )
        val r = DestinyCore.afterCheck(
            first.next,
            DestinyCore.ConditionOutcome(true, "¥329"),
            now + 3_600_000,
            now + 3_600_000
        )
        assertTrue(r.fire)
        assertNull(r.reason)
    }

    @Test
    fun 条件仍满足不重复触发() {
        val first = DestinyCore.afterCheck(
            conditionDef(),
            DestinyCore.ConditionOutcome(false, "¥399"),
            now,
            now
        )
        val second = DestinyCore.afterCheck(
            first.next,
            DestinyCore.ConditionOutcome(true, "¥329"),
            now + 3_600_000,
            now + 3_600_000
        )
        val r = DestinyCore.afterCheck(
            second.next,
            DestinyCore.ConditionOutcome(true, "¥319"),
            now + 7_200_000,
            now + 7_200_000
        )
        assertFalse(r.fire)
        assertEquals(DestinyCore.COND.UNCHANGED, r.reason)
    }

    @Test
    fun 冷却期内跳过且不补跑() {
        val def = DestinyCore.ConditionDef(
            conditionGoal = "x",
            notifyWhen = "each_time_met",
            cooldownMs = 86_400_000L,
            lastFiredAtMs = now
        )
        val r = DestinyCore.afterCheck(
            def,
            DestinyCore.ConditionOutcome(true, "2"),
            now + 3_600_000,
            now + 3_600_000
        )
        assertFalse(r.fire)
        assertEquals(DestinyCore.COND.COOLDOWN, r.reason)
    }

    @Test
    fun 动作没启动成回滚基线() {
        val after = DestinyCore.ConditionDef(
            conditionGoal = "降价超过 50",
            lastConditionMet = true,
            baselineValue = "¥329"
        )
        val before = DestinyCore.ConditionDef(
            conditionGoal = "降价超过 50",
            lastFiredAtMs = null,
            baselineValue = "¥399",
            baselineAtMs = now
        )
        val reverted = DestinyCore.revertFire(after, before)
        assertFalse(reverted.lastConditionMet == true)
        assertEquals("¥399", reverted.baselineValue)
    }

    @Test
    fun 条件规则一改历史基线失效() {
        val edited = DestinyCore.withConditionHistoryFrom(
            DestinyCore.ConditionDef(conditionGoal = "改了规则", baselineValue = "X"),
            DestinyCore.ConditionDef(conditionGoal = "x", baselineValue = "¥399")
        )
        assertNull(edited.baselineValue)
    }

    @Test
    fun 预览三次与保存同源() {
        val def = DestinyCore.ConditionDef(
            conditionGoal = "x",
            times = listOf("08:00", "12:30", "20:00")
        )
        assertEquals(3, DestinyCore.upcoming(def, now, 3).size)
    }

    /* ── 6. 虚拟屏 ───────────────────────────────────────────────────── */

    @Test
    fun 分配四个槽位() {
        val p = DestinyCore.ScreenPool(4)
        assertEquals(0, (p.acquire(DestinyCore.PoolTask("a", "微信")) as DestinyCore.Acquire.Ok).slot)
        assertEquals(1, (p.acquire(DestinyCore.PoolTask("b", "飞书")) as DestinyCore.Acquire.Ok).slot)
    }

    @Test
    fun 同App锁排队并写明持有者() {
        val p = DestinyCore.ScreenPool(4)
        p.acquire(DestinyCore.PoolTask("a", "微信"))
        val r = p.acquire(DestinyCore.PoolTask("c", "微信")) as DestinyCore.Acquire.Wait
        assertEquals("app", r.wait)
        assertEquals("a", r.holder)
    }

    @Test
    fun 互等当场认出死锁() {
        val p = DestinyCore.ScreenPool(4)
        p.acquire(DestinyCore.PoolTask("a", "微信"))
        p.acquire(DestinyCore.PoolTask("c", "微信"))
        p.waitFor["a"] = "c"
        val dl = p.acquire(DestinyCore.PoolTask("c", "微信")) as DestinyCore.Acquire.Deadlock
        assertTrue(dl.cycle.size >= 3)
    }

    @Test
    fun 释放反序且幂等() {
        val p = DestinyCore.ScreenPool(4)
        p.acquire(DestinyCore.PoolTask("a", "微信"))
        p.release("a")
        p.release("a")
        assertNull(p.slots[0])
        assertFalse(p.locks.containsKey("微信"))
    }

    @Test
    fun 优先级插队到同优先组前() {
        val p = DestinyCore.ScreenPool(4)
        val q = mutableListOf<DestinyCore.PoolTask>()
        p.enqueue(q, DestinyCore.PoolTask("x", "其他", 0))
        p.enqueue(q, DestinyCore.PoolTask("y", "其他", 0))
        p.enqueue(q, DestinyCore.PoolTask("z", "其他", 5))
        assertEquals("z", q[0].id)
    }

    /* ── 7. 自检依赖 DAG ─────────────────────────────────────────────── */

    @Test
    fun 十二项全有结论() {
        assertEquals(12, DestinyCore.runSelfCheck(emptyMap(), 90_000).status.size)
    }

    @Test
    fun 依赖没过写明缺哪项() {
        val sc = DestinyCore.runSelfCheck(emptyMap(), 90_000)
        assertTrue((sc.status["app_launch"] ?: "").contains("缺"))
    }

    @Test
    fun 设备能力如实标需真机不谎报通过() {
        val sc = DestinyCore.runSelfCheck(emptyMap(), 90_000)
        assertEquals("Skipped: 需真机", sc.status["device"])
    }

    @Test
    fun 总预算可压缩触发() {
        val allPass = DestinyCore.CHECKS.associate { it.key to "Pass" }
        val tight = DestinyCore.runSelfCheck(allPass, 6_000)
        assertTrue(tight.status.any { it.value.contains("总时限") })
    }

    @Test
    fun release幂等标记() {
        assertTrue(DestinyCore.runSelfCheck(emptyMap(), 90_000).released)
    }

    /* ── 8. 网关分级熔断 ─────────────────────────────────────────────── */

    @Test
    fun rate冷却一百二十秒() {
        val h = DestinyCore.GatewayHealth()
        h.recordFailure("p1", "RATE", "rate limit", now)
        assertFalse(h.isUsable("p1", now + 1_000))
        assertTrue(h.isUsable("p1", now + 120_001))
    }

    @Test
    fun auth直接死且不复活() {
        val h = DestinyCore.GatewayHealth()
        h.recordFailure("p2", "AUTH", "invalid key sk-abc", now)
        assertTrue(h.state("p2").dead)
        assertFalse(h.isUsable("p2", now + 9_999_999))
    }

    @Test
    fun region只熔断该区域() {
        val h = DestinyCore.GatewayHealth()
        h.recordFailure("p3", "REGION", "没有可用的国内版账号 cn:", now, "cn")
        assertFalse(h.isUsable("p3", now + 1_000, "cn"))
        assertTrue(h.isUsable("p3", now + 1_800_001, "cn"))
    }

    @Test
    fun 按真实探针延迟择优() {
        val h = DestinyCore.GatewayHealth()
        h.recordProbe("p4", 210, now)
        h.recordProbe("p5", 880, now)
        assertEquals("p4", h.pickBest(listOf("p4", "p5"), now))
    }

    @Test
    fun 自愈阶梯走到成功那一步() {
        val h = DestinyCore.GatewayHealth()
        val res = h.repairLadder("p6", listOf({ false }, { true }))
        assertEquals("FIXED", res.second)
    }

    /* ── 9. 能力装配 + 确定性路由 ────────────────────────────────────── */

    @Test
    fun 能力即插件单失败只摘自己() {
        val asm = DestinyCore.assemble(
            listOf("phone", "web"),
            listOf(
                DestinyCore.McpServer("sqlite", on = true),
                DestinyCore.McpServer("github", on = false)
            )
        )
        assertEquals(4, asm.capabilities.size)
        assertEquals("degraded", asm.capabilities[3].state)
        assertEquals("ready", asm.capabilities[2].state)
    }

    @Test
    fun 工具组懒展开前缀最多失效一次() {
        val asm = DestinyCore.assemble(
            listOf("phone", "web"),
            listOf(DestinyCore.McpServer("sqlite", on = true))
        )
        assertTrue(DestinyCore.expandGroup(asm, "phone").first)
        assertFalse(DestinyCore.expandGroup(asm, "web").first)
    }

    @Test
    fun 算数确定性选中sqlite() {
        val r = DestinyCore.route("算一下今天用了多少 Token")
        assertTrue(r.capabilities.contains("mcp:sqlite"))
    }

    @Test
    fun 路由不靠模型自觉() {
        assertTrue(DestinyCore.route("搜一下最新新闻").deterministic)
    }

    /* ── 10. 督查 / 身份 / MCP / 记忆 ─────────────────────────────────── */

    @Test
    fun 督查检阅一次过率() {
        val s = DestinyCore.Supervisor()
        s.submit("r1", "日报", true)
        s.review("r1", "accept")
        s.submit("r2", "下单", false)
        s.review("r2", "reject")
        assertEquals(50, s.rate())
    }

    @Test
    fun 一个号风控同身份另一号停二十四小时() {
        val i = DestinyCore.Identity()
        i.bind("me", "g1", "s1")
        i.bind("me", "g1", "s2")
        i.bind("team", "g1", "s3")
        i.flagRisk("s1", now)
        assertTrue(i.holdFor("me", now).first)
        assertFalse(i.holdFor("team", now).first)
    }

    @Test
    fun 只读服务拒绝写工具() {
        val d = DestinyCore.mcpCheck(
            DestinyCore.McpServer("ro", readOnly = true),
            "write_file",
            "/tmp/a"
        )
        assertEquals("Deny", d.decision)
    }

    @Test
    fun 写工具默认每次批准() {
        val d = DestinyCore.mcpCheck(
            DestinyCore.McpServer("fs", scope = "/tmp"),
            "write_file",
            "/tmp/a"
        )
        assertEquals("Ask", d.decision)
    }

    @Test
    fun 只读且路径越界拒绝() {
        val d = DestinyCore.mcpCheck(
            DestinyCore.McpServer("fs", scope = "/tmp"),
            "read_file",
            "/etc/passwd"
        )
        assertEquals("Deny", d.decision)
    }

    @Test
    fun 做法落成待批准() {
        val m = DestinyCore.Memory()
        val mem = m.add("做多入场门槛按 4 分", "procedure")
        assertEquals("pending", mem.status)
    }

    @Test
    fun 非手写改动已批准做法打回待批准() {
        val m = DestinyCore.Memory()
        val mem = m.add("做多入场门槛按 4 分", "procedure")
        m.approve(mem.id)
        m.update(mem.id, "门槛改 5 分", "model")
        assertEquals("pending", m.get(mem.id)?.status)
    }

    @Test
    fun 之后被改过则拒绝撤销() {
        val m = DestinyCore.Memory()
        val mem = m.add("做多入场门槛按 4 分", "procedure")
        m.approve(mem.id)
        val tok = m.takeUndo(mem.id)
        m.update(mem.id, "又改了", "manual")
        assertFalse(m.undo(tok ?: "").first)
    }

    @Test
    fun 敏感信息不进记忆() {
        val m = DestinyCore.Memory()
        val item = m.add("密码 abc123456", "fact")
        assertFalse(item.text.contains("abc123456"))
    }

    @Test
    fun 取舍矩阵八组可核对() {
        assertEquals(8, DestinyCore.DECISIONS.size)
        assertTrue(DestinyCore.DECISIONS.all { it.take.isNotEmpty() && it.drop.isNotEmpty() && it.from.isNotEmpty() && it.why.isNotEmpty() })
    }
}
