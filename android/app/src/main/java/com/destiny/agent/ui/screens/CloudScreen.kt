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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.destiny.agent.data.ComputerStep
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.data.RemoteSession
import com.destiny.agent.ui.Dot
import com.destiny.agent.ui.EmptyState
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
 * 云电脑页 —— Computer Use
 *
 * 定位：这不是「远程桌面播放器」，而是「把一件复杂网页事务整个交出去」的地方。
 *   · 简单 App 操作 → 对话页的虚拟屏就够
 *   · 需要跨站点、需要登录态、需要填表提交的复杂流程 → 走云端电脑
 *
 * 诚实边界（不夸大）：
 *   · 云端实例是干净环境，你的登录态不在里面，需要登录的环节必然停下来交给你
 *   · 付款、验证码、人脸一律不由它代办
 */
@Composable
fun CloudScreen(store: DestinyStore) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()
    var goal by remember { mutableStateOf("") }

    val sessions = state.remote
    val running = sessions.count { it.status == "running" }
    val blocked = sessions.count { it.status == "blocked" }
    val done = sessions.count { it.status == "done" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp)
    ) {
        item {
            HeroPanel {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(a.champagne.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = Icons.Monitor,
                                contentDescription = null,
                                tint = a.champagne,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(
                            "云端电脑",
                            style = MaterialTheme.typography.displaySmall,
                            color = a.ink
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "给它一件需要跨网页、跨站点的复杂事。它在一台干净的云端实例上打开浏览器，" +
                            "自己点、自己填、自己核对；遇到登录、验证码、付款，它会停下来交给你。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        StatCell(value = running.toString(), label = "正在跑", accent = running > 0)
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = blocked.toString(), label = "等你接手")
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = done.toString(), label = "已交付")
                    }
                }
            }
        }

        item {
            SectionLabel("交一件复杂事给它")
        }

        item {
            Panel {
                Column {
                    Text(
                        "写清楚目标，别写步骤。",
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "它自己决定开哪个站点、点哪里、填什么。写清目标比写清路径准。",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(16.dp))
                    CloudGoalField(goal) { goal = it }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "在云端电脑上开始",
                        icon = Icons.Play,
                        enabled = goal.isNotBlank()
                    ) {
                        store.startRemote(goal.trim())
                        goal = ""
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(a.elevated)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            painter = Icons.Info,
                            contentDescription = null,
                            tint = a.inkFaint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "示例：帮我订下周去上海的高铁票 · 帮我在三个招聘网站上投同一份简历",
                            style = MaterialTheme.typography.labelSmall,
                            color = a.inkFaint
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("会话记录", sessions.size)
        }

        if (sessions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Monitor,
                    title = "还没有云端会话",
                    hint = "上面写一句目标，它会自己开浏览器干起来。"
                )
            }
        } else {
            items(sessions, key = { it.id }) { s ->
                SessionCard(
                    session = s,
                    onHandover = { store.handoverRemote(s.id) },
                    onFinish = { store.finishRemote(s.id, "已由你手动收尾，结果记入本会话。") }
                )
            }
        }

        item {
            SectionLabel("它能做什么，不能做什么")
        }

        item {
            Panel {
                Column {
                    BoundaryRow(
                        ok = true,
                        text = "读网页、填表单、点按钮、多站点串起来核对 —— 全自动。"
                    )
                    Spacer(Modifier.height(8.dp))
                    BoundaryRow(
                        ok = true,
                        text = "填完之后截图留证，并把结论回到对话里。"
                    )
                    Spacer(Modifier.height(8.dp))
                    BoundaryRow(
                        ok = false,
                        text = "登录、验证码、人脸实名 —— 必须你亲手做，它会停下等你。"
                    )
                    Spacer(Modifier.height(8.dp))
                    BoundaryRow(
                        ok = false,
                        text = "付款 —— 它能走到付款页，但按下去之前一定先问你。"
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "云端实例是干净环境，你的登录态不在里面 —— 这是设计如此，不是故障。",
                        style = MaterialTheme.typography.labelSmall,
                        color = a.inkFaint
                    )
                }
            }
        }
    }
}

/* ── 目标输入 ─────────────────────────────────────────── */

@Composable
private fun CloudGoalField(value: String, onChange: (String) -> Unit) {
    val a = LocalAccents.current
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, a.hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                "例：帮我在携程订下周去上海的高铁票，二等座",
                style = MaterialTheme.typography.bodyMedium,
                color = a.inkFaint
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = LocalTextStyle.current.merge(
                TextStyle(color = a.ink, fontSize = 14.sp, lineHeight = 21.sp)
            ),
            cursorBrush = SolidColor(a.champagne),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ── 会话卡 ───────────────────────────────────────────── */

@Composable
private fun SessionCard(
    session: RemoteSession,
    onHandover: () -> Unit,
    onFinish: () -> Unit
) {
    val a = LocalAccents.current
    val tone = when (session.status) {
        "running" -> a.ok
        "blocked" -> a.warn
        "done" -> a.inkFaint
        else -> a.info
    }
    Panel(border = tone.copy(alpha = 0.22f)) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        session.goal,
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "云端实例 · ${stamp(session.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = a.inkFaint
                    )
                }
                Spacer(Modifier.width(9.dp))
                Pill(
                    when (session.status) {
                        "running" -> "执行中"
                        "blocked" -> "等你接手"
                        "done" -> "已交付"
                        else -> "待命"
                    },
                    when (session.status) {
                        "running" -> Tone.Ok
                        "blocked" -> Tone.Warn
                        "done" -> Tone.Neutral
                        else -> Tone.Info
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 步骤流水
            session.steps.forEachIndexed { i, s ->
                StepRow(
                    step = s,
                    last = i == session.steps.lastIndex,
                    first = i == 0
                )
            }

            if (session.needsHuman != null) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(a.warn.copy(alpha = 0.10f))
                        .border(1.dp, a.warn.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = Icons.Alert,
                                contentDescription = null,
                                tint = a.warn,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "这一步要你本人处理",
                                style = MaterialTheme.typography.headlineSmall,
                                color = a.warn
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            session.needsHuman,
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            PrimaryButton(
                                "我处理完了，交还给它",
                                modifier = Modifier.weight(1f),
                                icon = Icons.Check
                            ) { onHandover() }
                            Spacer(Modifier.width(9.dp))
                            GhostButton("结束", tone = a.inkFaint) { onFinish() }
                        }
                    }
                }
            }

            if (session.result != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(a.ok.copy(alpha = 0.09f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = Icons.CheckCircle,
                        contentDescription = null,
                        tint = a.ok,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        session.result,
                        style = MaterialTheme.typography.bodySmall,
                        color = a.ink
                    )
                }
            }
        }
    }
}

/* ── 步骤行（竖向时间轴） ─────────────────────────────── */

@Composable
private fun StepRow(step: ComputerStep, last: Boolean, first: Boolean) {
    val a = LocalAccents.current
    val dotColor = when (step.state) {
        "ok" -> a.ok
        "running" -> a.champagne
        "blocked" -> a.danger
        "needs-human" -> a.warn
        else -> a.inkFaint
    }
    Row(Modifier.fillMaxWidth()) {
        // 轨道
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (!last) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(a.hairline)
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.padding(bottom = if (last) 0.dp else 9.dp)) {
            Text(
                step.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (step.state == "needs-human") a.warn else a.ink
            )
            if (step.detail.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    step.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = a.inkFaint
                )
            }
        }
    }
}

/* ── 能力边界行 ───────────────────────────────────────── */

@Composable
private fun BoundaryRow(ok: Boolean, text: String) {
    val a = LocalAccents.current
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = if (ok) Icons.CheckCircle else Icons.Alert,
            contentDescription = null,
            tint = if (ok) a.ok else a.warn,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = a.inkSub
        )
    }
}

private fun stamp(ts: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
