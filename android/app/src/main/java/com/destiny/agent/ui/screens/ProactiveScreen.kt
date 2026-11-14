package com.destiny.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.data.ScheduleItem
import com.destiny.agent.data.Suggestion
import com.destiny.agent.ui.Dot
import com.destiny.agent.ui.EmptyState
import com.destiny.agent.ui.GhostButton
import com.destiny.agent.ui.HeroPanel
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.ListRow
import com.destiny.agent.ui.Panel
import com.destiny.agent.ui.Pill
import com.destiny.agent.ui.PrimaryButton
import com.destiny.agent.ui.SectionLabel
import com.destiny.agent.ui.StatCell
import com.destiny.agent.ui.Tone
import com.destiny.agent.ui.theme.LocalAccents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 主动页 —— 它自己发起的动作
 *
 * 这是「不是聊天机器人」的核心体现：
 *   · 它基于你确认过的偏好 / 日程，主动提出该做的事
 *   · 到点自动执行，不需要你再发一句话
 */
@Composable
fun ProactiveScreen(store: DestinyStore, onOpenTask: (String) -> Unit) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()

    val pending = state.suggestions.filter { !it.dismissed }
    val running = state.tasks.filter { it.status in listOf("running", "awaiting", "queued") }
    val upcoming = state.schedule.filter { !it.done }.sortedBy { it.at }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)
    ) {
        item {
            HeroPanel {
                Column {
                    Text(
                        "它在替你盯着",
                        style = MaterialTheme.typography.displaySmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        proactiveSummary(state.tasks.size, pending.size, upcoming.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        StatCell(
                            value = running.size.toString(),
                            label = "正在跑",
                            accent = running.isNotEmpty()
                        )
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = pending.size.toString(), label = "等你点头")
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = upcoming.size.toString(), label = "已排期")
                    }
                }
            }
        }

        item {
            SectionLabel("它建议做的", pending.size)
        }

        if (pending.isEmpty()) {
            item {
                Panel {
                    Column {
                        Text(
                            "暂时没有建议",
                            style = MaterialTheme.typography.headlineSmall,
                            color = a.ink
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "它只在你确认过的偏好、待批准的做法、或者手头确实空着的时候才开口。\n" +
                                "多记几条偏好，它会越来越懂该主动做什么。",
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub
                        )
                        Spacer(Modifier.height(16.dp))
                        GhostButton(
                            "让它现在想一想",
                            icon = Icons.Wand,
                            tone = a.champagne
                        ) { store.generateSuggestions() }
                    }
                }
            }
        } else {
            items(pending, key = { it.id }) { sug ->
                SuggestionCard(sug, onAccept = { store.acceptSuggestion(sug.id) },
                    onDismiss = { store.dismissSuggestion(sug.id) })
            }
        }

        item { SectionLabel("正在执行", running.size) }

        if (running.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Bolt,
                    title = "现在没有在跑的事",
                    hint = "让它主动排个日程，或者直接说一句话让它开始。"
                )
            }
        } else {
            items(running, key = { it.id }) { t ->
                Panel(onClick = { onOpenTask(t.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(
                            color = when (t.status) {
                                "awaiting" -> a.warn
                                "queued" -> a.inkFaint
                                else -> a.ok
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = a.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${t.app} · ${statusLabel(t.status)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = a.inkSub
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Pill(
                            when (t.status) {
                                "awaiting" -> "等你决定"
                                "queued" -> "排队中"
                                else -> "执行中"
                            },
                            when (t.status) {
                                "awaiting" -> Tone.Warn
                                "queued" -> Tone.Neutral
                                else -> Tone.Ok
                            }
                        )
                    }
                }
            }
        }

        item { SectionLabel("已排期", upcoming.size) }

        if (upcoming.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Clock,
                    title = "没有排期",
                    hint = "排一个它会到点自己去做。"
                )
            }
        } else {
            items(upcoming, key = { it.id }) { item ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(a.champagne.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    monthDay(item.at),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = a.champagne
                                )
                                Text(
                                    hhmm(item.at),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = a.ink
                                )
                            }
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = a.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${item.app} · ${if (item.repeat == "once") "只这一次" else "重复"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = a.inkSub
                            )
                        }
                        GhostButton("取消", tone = a.inkFaint) {
                            store.completeSchedule(item.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(sug: Suggestion, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val a = LocalAccents.current
    val tone = when (sug.tone) {
        "warn" -> a.warn
        "urgent" -> a.danger
        else -> a.info
    }
    Panel(border = tone.copy(alpha = 0.28f)) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tone.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = Icons.Bolt,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sug.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        sug.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                PrimaryButton(
                    "就这么办",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Check
                ) { onAccept() }
                Spacer(Modifier.width(9.dp))
                GhostButton("不用了", tone = a.inkFaint) { onDismiss() }
            }
        }
    }
}

private fun proactiveSummary(total: Int, pending: Int, upcoming: Int): String = when {
    pending > 0 -> "有 $pending 件事它想跟你确认，确认完就会自己去做。"
    upcoming > 0 -> "已经排了 $upcoming 件事，到点它会自己开跑，不用你再催。"
    else -> "手头没有在跑的事。它在等你的下一句话，或者你记一条偏好，它就知道该主动做什么。"
}

fun statusLabel(s: String): String = when (s) {
    "running" -> "执行中"
    "awaiting" -> "等你决定"
    "queued" -> "排队中"
    "paused" -> "已暂停"
    "done" -> "已完成"
    "stopped" -> "已停止"
    "failed" -> "失败"
    else -> s
}

private fun monthDay(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日"
}

private fun hhmm(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
