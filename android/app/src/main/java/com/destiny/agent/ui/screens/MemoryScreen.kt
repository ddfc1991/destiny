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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.data.MemoryItem
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

/**
 * 记忆页 —— 持久记忆
 *
 * 三分法（来自 Mika core 的 MemoryStore + mibot 的审计口径）：
 *   关于你 fact / 偏好 preference / 做法 procedure
 * 做法类必须你点头才生效；密码、验证码、卡号一律不记（引擎层脱敏）。
 */
@Composable
fun MemoryScreen(store: DestinyStore) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("fact") }

    val pending = state.memories.filter { it.status == "pending" }
    val facts = state.memories.filter { it.kind == "fact" && it.status == "active" }
    val prefs = state.memories.filter { it.kind == "preference" && it.status == "active" }
    val procs = state.memories.filter { it.kind == "procedure" && it.status == "active" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)
    ) {
        item {
            HeroPanel {
                Column {
                    Text(
                        "它记得的",
                        style = MaterialTheme.typography.displaySmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "关于你 ${state.memories.count { it.kind == "fact" }} 条、偏好 ${state.memories.count { it.kind == "preference" }} 条、" +
                            "做法 ${state.memories.count { it.kind == "procedure" }} 条。\n" +
                            "密码、验证码、卡号不会进来 —— 那是引擎层的硬规则，不是承诺。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(18.dp))
                    Row {
                        StatCell(value = facts.size.toString(), label = "关于你")
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = prefs.size.toString(), label = "偏好")
                        Spacer(Modifier.width(28.dp))
                        StatCell(
                            value = procs.size.toString(),
                            label = "做法",
                            accent = procs.isNotEmpty()
                        )
                    }
                }
            }
        }

        item {
            SectionLabel("记一条", null)
            Panel {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        KindChip("关于你", kind == "fact") { kind = "fact" }
                        KindChip("偏好", kind == "preference") { kind = "preference" }
                        KindChip("做法", kind == "procedure") { kind = "procedure" }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, a.hairline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 13.dp, vertical = 11.dp)
                    ) {
                        if (input.isEmpty()) {
                            Text(
                                if (kind == "procedure") "写一个你希望它照着做的办法…" else "写一条关于你的事…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = a.inkFaint
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = a.ink, fontSize = 14.sp, lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(a.champagne),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton(
                        "记住",
                        icon = Icons.Plus,
                        enabled = input.isNotBlank()
                    ) {
                        store.addMemory(input, kind)
                        input = ""
                    }
                    if (kind == "procedure") {
                        Spacer(Modifier.height(9.dp))
                        Text(
                            "做法类会先进「待批准」，你点头之后才会真正生效。",
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkFaint
                        )
                    }
                }
            }
        }

        if (pending.isNotEmpty()) {
            item { SectionLabel("等你点头", pending.size) }
            items(pending, key = { it.id }) { m ->
                MemoryCard(m, needApprove = true,
                    onApprove = { store.approveMemory(m.id) },
                    onDelete = { store.deleteMemory(m.id) })
            }
        }

        item { SectionLabel("关于你", facts.size) }
        if (facts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.User,
                    title = "还没有关于你的记录",
                    hint = "记一条，它下次就知道该怎么跟你打交道。"
                )
            }
        } else {
            items(facts, key = { it.id }) { m ->
                MemoryCard(m, needApprove = false, onDelete = { store.deleteMemory(m.id) })
            }
        }

        item { SectionLabel("偏好", prefs.size) }
        if (prefs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Sliders,
                    title = "还没有偏好",
                    hint = "偏好是它主动出手的依据 —— 记一条，它才知道什么时候该替你做事。"
                )
            }
        } else {
            items(prefs, key = { it.id }) { m ->
                MemoryCard(m, needApprove = false, onDelete = { store.deleteMemory(m.id) })
            }
        }

        item { SectionLabel("做法", procs.size) }
        if (procs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Route,
                    title = "还没有沉淀的做法",
                    hint = "同一件事你手动教过它一次，之后它就按你的做法来。"
                )
            }
        } else {
            items(procs, key = { it.id }) { m ->
                MemoryCard(m, needApprove = false, onDelete = { store.deleteMemory(m.id) })
            }
        }
    }
}

@Composable
private fun KindChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val a = LocalAccents.current
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) a.champagne.copy(alpha = 0.15f) else a.elevated)
            .border(
                1.dp,
                if (selected) a.champagne.copy(alpha = 0.4f) else a.hairline,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) a.champagne else a.inkSub
        )
    }
}

@Composable
private fun MemoryCard(
    m: MemoryItem,
    needApprove: Boolean,
    onApprove: () -> Unit = {},
    onDelete: () -> Unit
) {
    val a = LocalAccents.current
    Panel(border = if (needApprove) a.warn.copy(alpha = 0.3f) else null) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        m.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = a.ink
                    )
                    Spacer(Modifier.height(7.dp))
                    Row {
                        Pill(
                            kindLabel(m.kind),
                            when (m.kind) {
                                "procedure" -> Tone.Champagne
                                "preference" -> Tone.Info
                                else -> Tone.Neutral
                            }
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            sourceLabel(m.source) + " · 第${m.v}版",
                            style = MaterialTheme.typography.labelSmall,
                            color = a.inkFaint
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = Icons.Trash,
                    contentDescription = "删除",
                    tint = a.inkFaint,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .padding(6.dp)
                )
            }
            if (needApprove) {
                Spacer(Modifier.height(13.dp))
                Row {
                    PrimaryButton(
                        "同意它生效",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Check
                    ) { onApprove() }
                    Spacer(Modifier.width(9.dp))
                    GhostButton("先不用", tone = a.inkFaint) { onDelete() }
                }
            }
        }
    }
}

private fun kindLabel(k: String): String = when (k) {
    "procedure" -> "做法"
    "preference" -> "偏好"
    else -> "关于你"
}

private fun sourceLabel(s: String): String = when (s) {
    "observed" -> "它观察到"
    "inferred" -> "它推断"
    else -> "你告诉它"
}
