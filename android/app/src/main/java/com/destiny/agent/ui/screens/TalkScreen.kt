package com.destiny.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.destiny.agent.data.ChatMessage
import com.destiny.agent.data.DestinyData
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.data.Suggestion
import com.destiny.agent.data.Task
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.theme.LocalAccents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话页 —— Hark 主链版
 *
 * 刻意**不做聊天气泡**：没有左右分栏的对话卡片，没有会话列表。
 * 一条长期连续的主链：你的话是右对齐的纯文字，它的话是左对齐的正文，
 * 只有"它正在替你动手"的动作才升级成带边框的卡片，并在卡内暴露
 * 步骤日志与进度 —— 让 AI 的每一步都看得见，不黑箱。
 */
@Composable
fun TalkScreen(
    store: DestinyStore,
    onOpenTask: (String) -> Unit,
    onOpenProactive: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val state by store.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var pendingFile by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 附件选择器：用系统 OpenDocument，拿到真实文件名而不是编一句话
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = store.readPickedFile(uri)
            store.attachFile(name)
        }
    }

    val msgs = state.chatMsgs
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部：当下最需要的行动项（不是功能介绍按钮）
        val urgent = state.suggestions.filter { !it.dismissed }.take(2)
        if (urgent.isNotEmpty()) {
            IntentBar(urgent, store, onOpenProactive)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (msgs.isEmpty()) {
                item {
                    TalkEmptyState(
                        onPick = { store.sendChat(it) },
                        onCloud = onOpenCloud,
                        onMemory = onOpenMemory
                    )
                }
            }
            items(msgs, key = { "${it.t}-${it.text.hashCode()}" }) { msg ->
                ChainBlock(msg, state.tasks, onOpenTask)
            }
        }

        Composer(
            value = input,
            onValueChange = { input = it },
            onSend = {
                if (input.isNotBlank()) {
                    store.sendChat(input)
                    input = ""
                }
            },
            onVoice = { store.notify("语音输入还没接上，先用键盘说") },
            onAttach = { pendingFile = true }
        )
    }

    // 附件：用系统文件选择器真选，而不是往聊天流里塞一句假消息
    LaunchedEffect(pendingFile) {
        if (pendingFile) {
            pendingFile = false
            try {
                launcher.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                store.notify("这台设备打不开文件选择器")
            }
        }
    }
}

/* ── 顶部意图条：现在最该处理的事 ─────────────────────── */

@Composable
private fun IntentBar(
    items: List<Suggestion>,
    store: DestinyStore,
    onOpenProactive: () -> Unit
) {
    val a = LocalAccents.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "它替你盯着",
            style = MaterialTheme.typography.labelSmall,
            color = a.inkFaint
        )
        items.forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(a.elevated)
                    .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(a.champagne)
                        .clickable { store.acceptSuggestion(s.id) }
                        .padding(horizontal = 16.dp)
                        // 无障碍：读出这条建议的具体内容，而不是只念「就这么办」
                        .semantics {
                            contentDescription = "就这么办：${s.title}。${s.reason}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "就这么办",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/* ── 空态 ─────────────────────────────────────────────── */

@Composable
private fun TalkEmptyState(
    onPick: (String) -> Unit,
    onCloud: () -> Unit,
    onMemory: () -> Unit
) {
    val a = LocalAccents.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "说一件事，\n剩下的交给它。",
            style = MaterialTheme.typography.displayMedium,
            color = a.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "订票、订日程、抓公众号文章要点、\n跑一个需要点很多次的网页流程 —— 都行。",
            style = MaterialTheme.typography.bodyMedium,
            color = a.inkSub,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionRow("明天下午3点提醒我开会", "→ 变成日程") { onPick("明天下午3点提醒我开会") }
            ActionRow("帮我订下周去上海的高铁票", "→ 花钱的会先问你") { onPick("帮我订下周去上海的高铁票") }
            ActionRow("这个公众号文章讲了什么", "→ 链接能收，正文要你自己开") { onPick("这个公众号文章讲了什么") }
        }
        Spacer(Modifier.height(24.dp))
        // 能力说明用纯文字，不做引导按钮
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CapabilityLine(Icons.Bolt, "主动执行", "日程、订票这类事，它会自己到点去做")
            CapabilityLine(Icons.Brain, "持久记忆", "记住你的偏好，下次不用重复交代")
            CapabilityLine(Icons.Monitor, "云端电脑", "需要点很多次的网页流程，它在云端替你点")
        }
        Spacer(Modifier.height(16.dp))
        // 诚实边界：不夸大能力，用户点之前就知道边界在哪
        Text(
            "现在的边界：拆解靠本地规则引擎，没接模型端点；\n公众号正文有防盗链，只能收链接，文章要你自己打开看。",
            style = MaterialTheme.typography.labelSmall,
            color = a.inkFaint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionRow(text: String, tag: String, onClick: () -> Unit) {
    val a = LocalAccents.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .semantics { contentDescription = "$text，$tag" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = a.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(tag, style = MaterialTheme.typography.labelSmall, color = a.inkFaint)
    }
}

@Composable
private fun CapabilityLine(
    icon: androidx.compose.ui.graphics.painter.Painter,
    name: String,
    desc: String
) {
    val a = LocalAccents.current
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = a.inkFaint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = a.inkSub
        )
        Spacer(Modifier.width(8.dp))
        Text(
            desc,
            style = MaterialTheme.typography.bodySmall,
            color = a.inkFaint,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ── 主链区块 ─────────────────────────────────────────── */

@Composable
private fun ChainBlock(
    msg: ChatMessage,
    tasks: List<Task>,
    onOpenTask: (String) -> Unit
) {
    val a = LocalAccents.current
    val isUser = msg.role == "user"
    val isProactive = msg.kind == "proactive"
    val isBlocked = msg.kind == "blocked"
    val task = msg.link?.let { id -> tasks.firstOrNull { it.id == id } }

    if (isUser) {
        // 用户的话：右对齐纯文字，无气泡
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(
                msg.text,
                style = MaterialTheme.typography.bodyLarge,
                color = a.inkSub,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.widthIn(max = 280.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                hhmm(msg.t),
                style = MaterialTheme.typography.labelSmall,
                color = a.inkFaint
            )
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        if (isProactive) {
            Text(
                "它自己发起的",
                style = MaterialTheme.typography.labelSmall,
                color = a.champagne,
                modifier = Modifier.padding(start = 32.dp, bottom = 4.dp)
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isProactive) a.champagne.copy(alpha = 0.16f) else a.elevated)
                    .border(
                        1.dp,
                        if (isProactive) a.champagne.copy(alpha = 0.36f) else a.hairline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (isProactive) Icons.Bolt else Icons.Spark,
                    contentDescription = null,
                    tint = if (isProactive) a.champagne else a.inkSub,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            if (task != null) {
                // 动作块：暴露步骤日志与进度，不让 AI 黑箱运行
                ActionBlock(task, msg.text, onOpenTask, Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f)) {
                    if (isBlocked) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(a.danger.copy(alpha = 0.10f))
                                .border(1.dp, a.danger.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = a.danger
                            )
                        }
                    } else {
                        Text(
                            msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = a.ink
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        hhmm(msg.t),
                        style = MaterialTheme.typography.labelSmall,
                        color = a.inkFaint
                    )
                }
            }
        }
    }
}

/** AI 动作块：状态 + 步骤日志 + 进度条 + 行内入口 */
@Composable
private fun ActionBlock(
    task: Task,
    summary: String,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val a = LocalAccents.current
    val done = task.status == "done"
    val failed = task.status == "failed"
    val waiting = task.status == "awaiting" || task.gate != null
    val steps = task.steps
    // 进度来自引擎真实状态：当前步索引 / 流程总步数（不编造）
    val total = DestinyData.FLOW[task.flowKey]?.size ?: 0
    val progress = if (total > 0) (task.idx.toFloat() / total).coerceIn(0f, 1f) else 0f
    val elapsedMs = steps.sumOf { it.ms.toLong() }

    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
            .padding(16.dp)
            // 无障碍：把动作块聚合成一个可朗读的整体，
            // 否则 TalkBack 会逐个子节点念，读出一堆碎片。
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(
                        when {
                            failed -> "没做成"
                            done -> "已完成"
                            waiting -> "等你点头"
                            task.status == "queued" -> "排队中"
                            task.status == "stopped" -> "已停止"
                            else -> "正在做"
                        }
                    )
                    append("，").append(summary)
                    if (steps.isNotEmpty()) {
                        append("，共 ${steps.size} 步，用时 ${(elapsedMs / 1000.0).let { String.format("%.1f", it) }} 秒")
                    }
                }
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            failed -> a.danger
                            done -> a.ok
                            waiting -> a.warn
                            else -> a.champagne
                        }
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    failed -> "没做成"
                    done -> "已完成"
                    waiting -> "等你点头"
                    task.status == "queued" -> "排队中"
                    task.status == "stopped" -> "已停止"
                    else -> "正在做"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    failed -> a.danger
                    done -> a.ok
                    waiting -> a.warn
                    else -> a.champagne
                }
            )
            Spacer(Modifier.weight(1f))
            if (steps.isNotEmpty()) {
                Text(
                    "${steps.size} 步 · ${(elapsedMs / 1000.0).let { String.format("%.1f", it) }} 秒",
                    style = MaterialTheme.typography.labelSmall,
                    color = a.inkFaint
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = a.ink
        )

        if (steps.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            // 步骤日志：动作 + 对象 + 结果
            steps.takeLast(3).forEach { st ->
                Row(
                    Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkFaint,
                        modifier = Modifier.width(12.dp)
                    )
                    Column {
                        Text(
                            st.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub
                        )
                        if (st.detail.isNotBlank()) {
                            Text(
                                st.detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = a.inkFaint
                            )
                        }
                    }
                }
            }
            // 进度条：细，不转圈
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(a.hairline)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (progress > 0f) progress else 0.02f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(a.champagne)
                )
            }
        }

        if (waiting && task.waitingFor != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "需要你确认：${task.waitingFor}",
                style = MaterialTheme.typography.bodySmall,
                color = a.warn
            )
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenTask(task.id) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "查看执行过程",
                style = MaterialTheme.typography.labelMedium,
                color = a.champagne
            )
        }
    }
}

/* ── 输入条 ───────────────────────────────────────────── */

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    onAttach: () -> Unit
) {
    val a = LocalAccents.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComposerIcon(Icons.Mic, "语音输入", onVoice)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "说一件事，或贴个公众号链接…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkFaint,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = LocalTextStyle.current.merge(
                        TextStyle(
                            color = a.ink,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    ),
                    cursorBrush = SolidColor(a.champagne),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        // 无障碍：给输入框一个可朗读的名字，TalkBack 用户才知道往哪打字
                        .semantics { contentDescription = "输入你要办的事" }
                )
            }
            ComposerIcon(Icons.Clip, "添加附件", onAttach)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    // P0：点击热区必须 ≥48dp（Android 无障碍标准），视觉圆点保持 40dp
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = value.isNotBlank()) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (value.isBlank()) a.elevated else a.champagne),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = Icons.Send,
                        contentDescription = "发送",
                        tint = if (value.isBlank()) a.inkFaint else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerIcon(
    icon: androidx.compose.ui.graphics.painter.Painter,
    desc: String,
    onClick: () -> Unit
) {
    val a = LocalAccents.current
    Box(
        Modifier
            // P0：热区放大到 48dp，图标本身仍保持 19dp 视觉尺寸
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = a.inkFaint, modifier = Modifier.size(19.dp))
    }
}

private fun hhmm(ts: Long): String {
    val f = SimpleDateFormat("HH:mm", Locale.getDefault())
    return f.format(Date(ts))
}
