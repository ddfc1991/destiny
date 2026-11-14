package com.destiny.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.data.Task
import com.destiny.agent.data.TaskStep
import com.destiny.agent.ui.Dot
import com.destiny.agent.ui.DestinyTopBar
import com.destiny.agent.ui.GhostButton
import com.destiny.agent.ui.HeroPanel
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.Panel
import com.destiny.agent.ui.Pill
import com.destiny.agent.ui.PrimaryButton
import com.destiny.agent.ui.SectionLabel
import com.destiny.agent.ui.StatCell
import com.destiny.agent.ui.Tone
import com.destiny.agent.ui.theme.LocalAccents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务详情 —— 「查看执行过程」落地页
 *
 * 时间线 + 指标 + 授权闸门，全部来自引擎的真实账本，不是演出来的进度条。
 */
@Composable
fun TaskDetailScreen(store: DestinyStore, taskId: String, onBack: () -> Unit) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()
    val task = state.tasks.firstOrNull { it.id == taskId }

    if (task == null) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Panel {
                    Text("这个任务已经不在列表里了。", style = MaterialTheme.typography.bodyLarge, color = a.ink)
                }
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp)
    ) {
        item {
            DestinyTopBar(
                title = "执行过程",
                subtitle = "${task.app} · ${stamp(task.createdAt)}",
                onBack = onBack
            )
        }
        item {
            HeroPanel {
                Column {
                    Text(
                        task.text,
                        style = MaterialTheme.typography.displaySmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(task.app, Tone.Champagne)
                        Spacer(Modifier.width(7.dp))
                        Pill(
                            statusLabel(task.status),
                            when (task.status) {
                                "awaiting" -> Tone.Warn
                                "queued" -> Tone.Neutral
                                "done" -> Tone.Ok
                                "failed", "stopped" -> Tone.Danger
                                else -> Tone.Info
                            }
                        )
                        if (task.vd != null) {
                            Spacer(Modifier.width(7.dp))
                            Pill("虚拟屏 vd${task.vd}", Tone.Neutral)
                        }
                        if (task.source != "chat") {
                            Spacer(Modifier.width(7.dp))
                            Pill(
                                when (task.source) {
                                    "proactive" -> "它主动发起"
                                    "schedule" -> "定时触发"
                                    "computer" -> "云电脑"
                                    else -> task.source
                                },
                                Tone.Info
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        StatCell(value = task.metrics.steps.toString(), label = "步")
                        Spacer(Modifier.width(26.dp))
                        StatCell(value = "${task.metrics.ms}ms", label = "耗时")
                        Spacer(Modifier.width(26.dp))
                        StatCell(value = task.metrics.calls.toString(), label = "调用")
                        Spacer(Modifier.width(26.dp))
                        StatCell(value = task.metrics.tokens.toString(), label = "tokens")
                    }
                }
            }
        }

        // 授权闸门
        if (task.status == "awaiting") {
            item {
                Box(Modifier.padding(top = 16.dp)) {
                    GateCard(task, store)
                }
            }
        }

        // 停止后的交代
        if (task.summary != null) {
            item {
                SectionLabel("交代")
                Panel {
                    Text(
                        task.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                }
            }
        }

        item {
            SectionLabel("执行过程", task.steps.size)
        }

        if (task.steps.isEmpty()) {
            item {
                Panel {
                    Text(
                        "还没有步骤。它正在排队等一块空闲的虚拟屏。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                }
            }
        } else {
            items(task.steps) { s ->
                StepLine(s)
            }
        }

        item {
            SectionLabel("操作")
            Panel {
                Column {
                    if (task.status == "running" || task.status == "queued" || task.status == "awaiting") {
                        GhostButton("停下来", icon = Icons.Close, tone = a.danger) { store.stopTask() }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (task.status == "stopped" || task.status == "failed") {
                        PrimaryButton("从断点继续", icon = Icons.Refresh) { store.resumeTask() }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "继续前会先核三件事：当前页面还是不是那个页面、输入框里有没有多东西、" +
                                "上次那步有没有副作用。有副作用的步不会重放。",
                            style = MaterialTheme.typography.labelSmall,
                            color = a.inkFaint
                        )
                    }
                    if (task.status == "done") {
                        Text(
                            "已完成。它自己先核过一遍再交给你 —— 要打回就回到对话里说哪一步不对。",
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub
                        )
                    }
                }
            }
        }
    }
}

/* ── 授权闸门卡 ───────────────────────────────────────── */

@Composable
private fun GateCard(task: Task, store: DestinyStore) {
    val a = LocalAccents.current
    Panel(border = a.warn.copy(alpha = 0.30f)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = Icons.Alert,
                    contentDescription = null,
                    tint = a.warn,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    store.gateLabel(task.gate),
                    style = MaterialTheme.typography.headlineMedium,
                    color = a.warn
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "它停在这里了 —— 不是卡住，是这一步不该由它自己决定。\n" +
                    "你点头它才往下走；红线范围内它连问都不会问。",
                style = MaterialTheme.typography.bodySmall,
                color = a.inkSub
            )
            Spacer(Modifier.height(16.dp))
            Row {
                PrimaryButton("放行这一次", modifier = Modifier.weight(1f), icon = Icons.Check) {
                    store.resolveGate("allow")
                }
                Spacer(Modifier.width(9.dp))
                GhostButton("以后这类都放行", tone = a.champagne) {
                    store.resolveGate("always")
                }
                Spacer(Modifier.width(9.dp))
                GhostButton("拒绝", tone = a.danger) {
                    store.resolveGate("deny")
                }
            }
        }
    }
}

/* ── 步骤时间线 ───────────────────────────────────────── */

@Composable
private fun StepLine(s: TaskStep) {
    val a = LocalAccents.current
    val color = when {
        s.gate != null -> a.warn
        s.auto -> a.ok
        else -> a.inkSub
    }
    Row(Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = a.ink,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (s.ms > 0) "${s.ms}ms" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = a.inkFaint
                )
            }
            if (s.detail.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    s.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = a.inkFaint,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (s.gate != null) {
                Spacer(Modifier.height(8.dp))
                Pill("授权闸门 · ${s.gate}", Tone.Warn)
            } else if (s.auto) {
                Spacer(Modifier.height(8.dp))
                Pill("已自动放行", Tone.Ok)
            }
        }
    }
}

private fun stamp(ts: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
