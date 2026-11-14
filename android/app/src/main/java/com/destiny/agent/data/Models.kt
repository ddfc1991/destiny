package com.destiny.agent.data

import kotlinx.serialization.Serializable

/* ════════════════════════════════════════════════════════════
   Destiny 原生版状态模型
   与 Web 版 destiny-app.js 的 state 一一对应，并新增三大核心能力：
     · 主动执行 (Proactive)  → ProactivePlan / Suggestion
     · 持久记忆 (Memory)      → MemoryItem（带来源与置信度）
     · Computer Use           → RemoteSession / ComputerStep
   ════════════════════════════════════════════════════════════ */

@Serializable
data class Account(
    val method: String = "google",      // google | email | phone
    val id: String = "",
    val name: String = "",
    val loginAt: Long = 0L
)

@Serializable
data class ChatMessage(
    val role: String,                    // user | bot
    val text: String,
    val t: Long,
    val link: String? = null,            // 关联任务 id
    val page: String? = null,            // 可跳转页面
    val kind: String? = null             // reply | typing | blocked | proactive
)

@Serializable
data class TaskStep(
    val t: Long,
    val title: String,
    val kind: String,                    // route | screen
    val ms: Int = 0,
    val detail: String = "",
    val gate: String? = null,
    val auto: Boolean = false
)

@Serializable
data class TaskMetrics(
    val steps: Int = 0,
    val tokens: Int = 0,
    val ms: Int = 0,
    val calls: Int = 0
)

@Serializable
data class Task(
    val id: String,
    val text: String,
    val app: String,
    val risk: String? = null,
    val flowKey: String = "default",
    val status: String = "running",      // running|awaiting|queued|paused|done|stopped|failed
    val vd: Int? = null,                 // 虚拟屏槽位
    val waitingFor: String? = null,
    val createdAt: Long = 0L,
    val idx: Int = 0,
    val steps: List<TaskStep> = emptyList(),
    val inserts: List<String> = emptyList(),
    val gate: String? = null,
    val summary: String? = null,
    val metrics: TaskMetrics = TaskMetrics(),
    val reviewed: Boolean = false,
    val priority: Int = 0,
    val source: String = "chat"          // chat | proactive | schedule | computer
)

/* ── 能力二：持久记忆 ─────────────────────────────────────────
   每条记忆带来源（怎么知道的）与状态（是否生效），
   「做法」类必须人工点头才生效。 */
@Serializable
data class MemoryItem(
    val id: String,
    val text: String,
    val kind: String = "fact",           // fact 关于你 | preference 偏好 | procedure 做法
    val status: String = "active",       // active | pending
    val source: String = "manual",       // manual | observed | inferred
    val v: Int = 1,
    val at: Long = 0L
)

/* ── 能力一：主动执行 ─────────────────────────────────────────
   Agent 主动提出的建议与已排期的计划。 */
@Serializable
data class Suggestion(
    val id: String,
    val title: String,
    val reason: String,                   // 为什么现在提这件事
    val action: String = "",              // 采纳后直接派给谁
    val app: String = "其他",
    val risk: String? = null,
    val tone: String = "info",            // info | warn | urgent
    val at: Long = 0L,
    val dismissed: Boolean = false
)

@Serializable
data class ScheduleItem(
    val id: String,
    val title: String,
    val at: Long,                        // 触发时间
    val repeat: String = "once",         // once | daily | weekly
    val app: String = "其他",
    val done: Boolean = false,
    val taskId: String? = null
)

/* ── 能力三：Computer Use（云端电脑） ──────────────────────────
   云端桌面会话：连接状态 + 步骤流水 + 产出 */
@Serializable
data class ComputerStep(
    val t: Long,
    val title: String,
    val detail: String = "",
    val state: String = "ok",             // ok | running | blocked | needs-human
    val shot: String? = null              // 截图占位（演示版为空）
)

@Serializable
data class RemoteSession(
    val id: String,
    val goal: String,
    val status: String = "idle",          // idle | connecting | running | done | blocked
    val steps: List<ComputerStep> = emptyList(),
    val createdAt: Long = 0L,
    val result: String? = null,
    val needsHuman: String? = null,
    val handedOver: Boolean = false        // 你已亲手处理过需要人的那一步
)

@Serializable
data class ApprovalRecord(
    val ts: Long,
    val taskId: String,
    val type: String,                     // payment | captcha
    val decision: String,                 // allow | deny | always
    val policy: String
)

@Serializable
data class Usage(
    val tokens: Int = 0,
    val calls: Int = 0,
    val tasks: Int = 0,
    val sessions: Int = 0
)

@Serializable
data class AppState(
    val v: Int = 1,
    val theme: String = "dark",            // dark | light
    val mode: String = "ask",             // ask | auto
    val runtime: String = "local",        // local | cloud
    val cloudEndpoint: String = "",
    val cloudKey: String = "",
    val cloudStatus: String = "unset",      // unset | testing | ok | error
    val cloudHost: String = "",
    val cloudError: String? = null,         // 探活失败的真实原因

    val account: Account? = null,
    val chatMsgs: List<ChatMessage> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val schedule: List<ScheduleItem> = emptyList(),
    val remote: List<RemoteSession> = emptyList(),
    val approvals: List<ApprovalRecord> = emptyList(),

    val selectedTask: String? = null,
    val appAccess: Map<String, Boolean> = emptyMap(),
    val allowAlways: List<String> = emptyList(),
    val skills: Map<String, Boolean> = emptyMap(),
    val mcp: Map<String, Boolean> = emptyMap(),
    val usage: Usage = Usage(),
    val linkedServices: List<String> = emptyList(),
    val firstRun: Long = 0L
) {
    val loggedIn: Boolean get() = account != null

    fun pendingCount(): Int = tasks.count { it.status == "awaiting" }

    fun activeTasks(): List<Task> =
        tasks.filter { it.status in listOf("running", "queued", "awaiting", "paused") }
}
