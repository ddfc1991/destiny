package com.destiny.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destiny.agent.data.ChatMessage
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.Pill
import com.destiny.agent.ui.Tone
import com.destiny.agent.ui.theme.LocalAccents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 对话页 —— 入口：你说一句话，它去办事
 *
 * 保留 Toki 的聊天流骨架，但按新定位调整：
 *  - 顶部有「它能做什么」的能力速览（三大核心能力）
 *  - 气泡区分「你」与「它」，「它」的主动发言有独立标记
 *  - 输入条常驻在底栏之上，不被内容遮挡
 */
@Composable
fun TalkScreen(
    store: DestinyStore,
    onOpenTask: (String) -> Unit,
    onOpenProactive: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val msgs = state.chatMsgs
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // 能力速览：直接告诉用户它具备什么，而不是藏着
        CapabilityStrip(
            onTalk = { },
            onProactive = onOpenProactive,
            onMemory = onOpenMemory,
            onCloud = onOpenCloud
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (msgs.isEmpty()) {
                item { TalkEmptyState(onProactive = onOpenProactive, onCloud = onOpenCloud) }
            }
            items(msgs, key = { "${it.t}-${it.text.hashCode()}" }) { msg ->
                ChatBubble(msg, onOpenTask)
            }
            item { Spacer(Modifier.height(4.dp)) }
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
            onVoice = { store.sendChat("（语音输入 · 演示版未接 ASR）") },
            onAttach = { store.sendChat("（附件 · 演示版未接文件选择器）") }
        )
    }
}

/* ── 能力速览条 ─────────────────────────────────────────── */

@Composable
private fun CapabilityStrip(
    onTalk: () -> Unit,
    onProactive: () -> Unit,
    onMemory: () -> Unit,
    onCloud: () -> Unit
) {
    val a = LocalAccents.current
    Column(Modifier.padding(top = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CapChip(Icons.Bolt, "主动执行", onProactive, a.champagne)
            CapChip(Icons.Brain, "持久记忆", onMemory, a.info)
            CapChip(Icons.Monitor, "云端电脑", onCloud, a.ok)
        }
    }
}

@Composable
private fun CapChip(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    val a = LocalAccents.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/* ── 空态 ─────────────────────────────────────────────── */

@Composable
private fun TalkEmptyState(onProactive: () -> Unit, onCloud: () -> Unit) {
    val a = LocalAccents.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "说一件事，\n剩下的交给它。",
            style = MaterialTheme.typography.displayMedium,
            color = a.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "订票、订日程、抓公众号文章要点、\n跑一个需要点很多次的网页流程 —— 都行。",
            style = MaterialTheme.typography.bodyMedium,
            color = a.inkSub,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionRow("明天下午3点提醒我开会", "→ 变成日程") {
                onProactive()
            }
            SuggestionRow("帮我订下周去上海的高铁票", "→ 花钱的会先问你") {
                onProactive()
            }
            SuggestionRow("这个公众号文章讲了什么", "→ 抓要点存进想法") {
                onCloud()
            }
        }
    }
}

@Composable
private fun SuggestionRow(text: String, tag: String, onClick: () -> Unit) {
    val a = LocalAccents.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, a.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
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
        Spacer(Modifier.width(10.dp))
        Text(tag, style = MaterialTheme.typography.labelSmall, color = a.inkFaint)
    }
}

/* ── 气泡 ─────────────────────────────────────────────── */

@Composable
private fun ChatBubble(msg: ChatMessage, onOpenTask: (String) -> Unit) {
    val a = LocalAccents.current
    val isUser = msg.role == "user"
    val isProactive = msg.kind == "proactive"
    val isBlocked = msg.kind == "blocked"

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProactive) a.champagne.copy(alpha = 0.18f) else a.elevated
                    )
                    .border(
                        1.dp,
                        if (isProactive) a.champagne.copy(alpha = 0.4f) else a.hairline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (isProactive) Icons.Bolt else Icons.Spark,
                    contentDescription = null,
                    tint = if (isProactive) a.champagne else a.inkSub,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
        }

        Column(
            Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isProactive) {
                Text(
                    "它自己发起的",
                    style = MaterialTheme.typography.labelSmall,
                    color = a.champagne,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            Box(
                Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 5.dp,
                            bottomEnd = if (isUser) 5.dp else 18.dp
                        )
                    )
                    .background(
                        when {
                            isUser -> a.champagne
                            isBlocked -> a.danger.copy(alpha = 0.10f)
                            isProactive -> a.champagne.copy(alpha = 0.07f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            isUser -> Color.Transparent
                            isBlocked -> a.danger.copy(alpha = 0.32f)
                            isProactive -> a.champagne.copy(alpha = 0.22f)
                            else -> a.hairline
                        },
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 5.dp,
                            bottomEnd = if (isUser) 5.dp else 18.dp
                        )
                    )
                    .then(
                        if (msg.link != null || msg.page != null) {
                            Modifier.clickable {
                                msg.link?.let(onOpenTask)
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isUser -> Color(0xFF0A0A0C)
                        isBlocked -> a.danger
                        else -> a.ink
                    }
                )
                if (msg.link != null) {
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "查看执行过程",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUser) Color(0xFF0A0A0C) else a.champagne
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = Icons.ChevronRight,
                            contentDescription = null,
                            tint = if (isUser) Color(0xFF0A0A0C) else a.champagne,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                hhmm(msg.t),
                style = MaterialTheme.typography.labelSmall,
                color = a.inkFaint,
                modifier = Modifier.padding(horizontal = 4.dp)
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
            .padding(horizontal = 14.dp)
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, a.hairline, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComposerIcon(Icons.Mic, "语音", onVoice)
            Spacer(Modifier.width(4.dp))
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
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            ComposerIcon(Icons.Clip, "附件", onAttach)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (value.isBlank()) a.elevated else a.champagne)
                    .clickable(enabled = value.isNotBlank()) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = Icons.Send,
                    contentDescription = "发送",
                    tint = if (value.isBlank()) a.inkFaint else Color(0xFF0A0A0C),
                    modifier = Modifier.size(18.dp)
                )
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
            .size(40.dp)
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
