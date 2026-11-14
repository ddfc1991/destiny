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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.destiny.agent.R
import com.destiny.agent.core.DestinyCore
import com.destiny.agent.data.DestinyData
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.ui.GhostButton
import com.destiny.agent.ui.HairLine
import com.destiny.agent.ui.HeroPanel
import com.destiny.agent.ui.Icons
import com.destiny.agent.ui.ListRow
import com.destiny.agent.ui.Panel
import com.destiny.agent.ui.Pill
import com.destiny.agent.ui.SectionLabel
import com.destiny.agent.ui.StatCell
import com.destiny.agent.ui.Tone
import com.destiny.agent.ui.theme.LocalAccents

/**
 * 我的 —— 账户、模块、设置、取舍依据
 *
 * 这一页的取舍：
 *   · 账户关联按 Toki 的做法：先给"能关联什么"，点一下才绑，绑了能看到状态
 *   · mibot 的 18 个模块全部保留，标清 live / spec，不偷偷藏功能
 *   · 最后放「取舍矩阵」—— 每条能力从哪儿来、丢了什么、为什么这么选，逐条可核对
 */
@Composable
fun MeScreen(store: DestinyStore) {
    val a = LocalAccents.current
    val state by store.state.collectAsState()
    var expandedModule by remember { mutableStateOf<String?>(null) }
    var showDecisions by remember { mutableStateOf(false) }

    val acct = state.account
    val linked = state.linkedServices

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp)
    ) {
        /* ── 账户 ── */
        item {
            HeroPanel {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(a.champagne.copy(alpha = 0.16f))
                                .border(1.dp, a.champagne.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (acct?.name?.firstOrNull() ?: "D").toString(),
                                style = MaterialTheme.typography.displaySmall,
                                color = a.champagne
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                acct?.name ?: "未登录",
                                style = MaterialTheme.typography.displaySmall,
                                color = a.ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (acct?.method) {
                                    "google" -> "Google 账号 · 已关联"
                                    "phone" -> "手机号 · 已关联"
                                    "email" -> "邮箱 · 已关联"
                                    else -> "登录后可同步记忆与日程"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = a.inkSub
                            )
                        }
                        if (acct != null) {
                            GhostButton("退出", icon = Icons.Logout, tone = a.inkFaint) {
                                store.logout()
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row {
                        StatCell(value = state.memories.size.toString(), label = "条记忆")
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = linked.size.toString(), label = "个已关联")
                        Spacer(Modifier.width(28.dp))
                        StatCell(value = state.usage.tasks.toString(), label = "办过的事")
                    }
                }
            }
        }

        /* ── 账户关联 ── */
        item { SectionLabel("账户关联", linked.size) }

        items(LINKABLE.size) { i ->
            val (name, iconRes, note) = LINKABLE[i]
            val on = linked.contains(name)
            Panel(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.destiny.agent.ui.IconChip(
                        icon = painterResource(iconRes),
                        filled = on,
                        tint = if (on) a.champagne else a.inkSub
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = a.ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = on,
                        onCheckedChange = { checked ->
                            if (checked) store.linkService(name) else store.unlinkService(name)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0A0A0C),
                            checkedTrackColor = a.champagne,
                            checkedBorderColor = a.champagne,
                            uncheckedThumbColor = a.inkFaint,
                            uncheckedTrackColor = Color.Transparent,
                            uncheckedBorderColor = a.hairline
                        )
                    )
                }
            }
        }

        /* ── 运行模式 ── */
        item { SectionLabel("运行模式") }

        item {
            Panel {
                Column {
                    Segmented(
                        label = "执行环境",
                        options = listOf("local" to "本地", "cloud" to "云端"),
                        current = state.runtime,
                        hint = if (state.runtime == "local") {
                            "拆解与调度都在这台手机上，Key 不出门。"
                        } else {
                            "云端模型端点：${state.cloudEndpoint.ifBlank { "未配置" }}"
                        }
                    ) { store.setRuntime(it) }

                    Spacer(Modifier.height(16.dp))

                    Segmented(
                        label = "授权策略",
                        options = listOf("ask" to "先问我", "auto" to "自动"),
                        current = state.mode,
                        hint = if (state.mode == "ask") {
                            "花钱、登录验证码会停下来等你；发消息这类直接做。"
                        } else {
                            "只在红线范围内自动。红线永远不可自动。"
                        }
                    ) { store.setMode(it) }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "深色主题",
                                style = MaterialTheme.typography.headlineSmall,
                                color = a.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "曜石香槟 · 夜里不刺眼",
                                style = MaterialTheme.typography.bodySmall,
                                color = a.inkSub
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = state.theme == "dark",
                            onCheckedChange = { store.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0A0A0C),
                                checkedTrackColor = a.champagne,
                                checkedBorderColor = a.champagne,
                                uncheckedThumbColor = a.inkFaint,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = a.hairline
                            )
                        )
                    }
                }
            }
        }

        /* ── 应用访问 ── */
        item { SectionLabel("应用访问", DestinyData.APP_OPTIONS.size) }

        item {
            Panel(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                Column {
                    DestinyData.APP_OPTIONS.forEach { app ->
                        val risky = DestinyData.FINANCIAL.contains(app)
                        val on = state.appAccess[app] ?: false
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        app,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = a.ink
                                    )
                                    if (risky) {
                                        Spacer(Modifier.width(7.dp))
                                        Pill("推荐禁用", Tone.Danger)
                                    }
                                }
                                if (risky && !on) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "金融交易类默认不放行 —— 要用请明确打开。",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = a.inkFaint
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Switch(
                                checked = on,
                                onCheckedChange = { store.setAppAccess(app, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0A0A0C),
                                    checkedTrackColor = a.champagne,
                                    checkedBorderColor = a.champagne,
                                    uncheckedThumbColor = a.inkFaint,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = a.hairline
                                )
                            )
                        }
                    }
                }
            }
        }

        /* ── 能力：技能 + MCP ── */
        item { SectionLabel("能力 · 技能与 MCP") }

        item {
            Panel(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                Column {
                    DestinyData.SKILLS.forEach { sk ->
                        ToggleRow(
                            title = sk.name,
                            subtitle = "什么时候用：${sk.when_}\n什么时候别用：${sk.not}",
                            on = state.skills[sk.id] ?: sk.on,
                            onChange = { store.toggleSkill(sk.id) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    HairLine()
                    Spacer(Modifier.height(6.dp))
                    DestinyData.MCP.forEach { m ->
                        ToggleRow(
                            title = m.name,
                            subtitle = "${m.type} · ${m.tools} 个工具 · ${m.perm}",
                            on = state.mcp[m.id] ?: m.on,
                            onChange = { store.toggleMcp(m.id) }
                        )
                    }
                }
            }
        }

        /* ── 网关与服务商 ── */
        item { SectionLabel("网关与服务商") }

        items(DestinyData.PROVIDERS.size) { i ->
            val p = DestinyData.PROVIDERS[i]
            Panel(padding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.destiny.agent.ui.IconChip(
                        icon = if (p.state == "ok") Icons.Cloud else Icons.Alert,
                        filled = p.state == "ok",
                        tint = if (p.state == "ok") a.ok else a.warn
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.n,
                            style = MaterialTheme.typography.headlineSmall,
                            color = a.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${p.ms} ms · 可用率 ${p.ok} · ${p.keys} 个 Key",
                            style = MaterialTheme.typography.bodySmall,
                            color = a.inkSub
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Pill(
                        if (p.state == "ok") "正常" else "已熔断",
                        if (p.state == "ok") Tone.Ok else Tone.Warn
                    )
                }
            }
        }

        /* ── mibot 18 模块中心 ── */
        item { SectionLabel("模块中心", DestinyData.MIBOT_MODULES.size) }

        item {
            Panel {
                Column {
                    Text(
                        "mibot 的 ${DestinyData.MIBOT_MODULES.size} 个模块一个没删。",
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "live = 这版就能用；spec = 规格已定、还没写完。点开看它到底做什么。",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(14.dp))
                    DestinyData.MIBOT_MODULES.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { m ->
                                ModuleCell(
                                    name = m.n,
                                    note = m.note,
                                    impl = m.impl,
                                    expanded = expandedModule == m.id,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        expandedModule = if (expandedModule == m.id) null else m.id
                                    }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        /* ── 取舍矩阵 ── */
        item { SectionLabel("取舍依据", DestinyCore.DECISIONS.size) }

        item {
            Panel {
                Column {
                    Text(
                        "每一块能力从哪儿来、丢了什么、为什么这么选 —— 逐条可核对。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = a.inkSub
                    )
                    Spacer(Modifier.height(12.dp))
                    GhostButton(
                        if (showDecisions) "收起取舍矩阵" else "展开取舍矩阵",
                        icon = Icons.List,
                        tint = a.champagne
                    ) { showDecisions = !showDecisions }

                    if (showDecisions) {
                        Spacer(Modifier.height(14.dp))
                        DestinyCore.DECISIONS.forEachIndexed { i, d ->
                            DecisionRow(
                                topic = d.topic,
                                winner = d.winner,
                                take = d.take,
                                drop = d.drop,
                                why = d.why,
                                from = d.from
                            )
                            if (i != DestinyCore.DECISIONS.lastIndex) {
                                Spacer(Modifier.height(13.dp))
                                HairLine()
                                Spacer(Modifier.height(13.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Panel {
                Column {
                    Text(
                        "关于",
                        style = MaterialTheme.typography.headlineSmall,
                        color = a.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Destiny · 原生 Android 版（Kotlin + Jetpack Compose）。\n" +
                            "引擎逻辑自 destiny-core.js 逐节移植，UI 为原生重写，不是网页套壳。",
                        style = MaterialTheme.typography.bodySmall,
                        color = a.inkSub
                    )
                }
            }
        }
    }
}

/* ── 账户关联清单 ─────────────────────────────────────── */
/* 用资源 ID 而不是 Icons.X —— 顶层常量不在组合上下文里，取不到 painterResource */

private val LINKABLE: List<Triple<String, Int, String>> = listOf(
    Triple("微信", R.drawable.ic_chat, "扫码绑定后微信里直接派任务"),
    Triple("飞书", R.drawable.ic_message_square, "日报 / 通知推送走群机器人"),
    Triple("GitHub", R.drawable.ic_git_branch, "只读；写操作每次单独批准"),
    Triple("Gmail", R.drawable.ic_mail, "验证码邮件自动填入"),
    Triple("系统日历", R.drawable.ic_calendar, "读你的日程，主动排期"),
    Triple("支付宝", R.drawable.ic_wallet, "金融类 · 默认不放行"),
    Triple("小红书", R.drawable.ic_star, "发布前会弹确认"),
    Triple("抖音", R.drawable.ic_play, "长列表逐屏读")
)

/* ── 子组件 ───────────────────────────────────────────── */

@Composable
private fun Segmented(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    hint: String,
    onSelect: (String) -> Unit
) {
    val a = LocalAccents.current
    Column {
        Text(label, style = MaterialTheme.typography.headlineSmall, color = a.ink)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(a.elevated)
                .padding(3.dp)
        ) {
            options.forEach { (key, text) ->
                val on = key == current
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) a.champagne.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onSelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) a.champagne else a.inkFaint
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(hint, style = MaterialTheme.typography.labelSmall, color = a.inkFaint)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    on: Boolean,
    onChange: (Boolean) -> Unit
) {
    val a = LocalAccents.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = a.ink)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = a.inkFaint
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = on,
            onCheckedChange = { onChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0A0C),
                checkedTrackColor = a.champagne,
                checkedBorderColor = a.champagne,
                uncheckedThumbColor = a.inkFaint,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = a.hairline
            )
        )
    }
}

@Composable
private fun ModuleCell(
    name: String,
    note: String,
    impl: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val a = LocalAccents.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (expanded) a.champagne.copy(alpha = 0.09f) else a.elevated)
            .border(
                1.dp,
                if (expanded) a.champagne.copy(alpha = 0.32f) else a.hairline,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = a.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (impl == "live") a.ok else a.inkFaint)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            note,
            style = MaterialTheme.typography.labelSmall,
            color = if (expanded) a.inkSub else a.inkFaint,
            maxLines = if (expanded) 4 else 2,
            overflow = TextOverflow.Ellipsis
        )
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Pill(if (impl == "live") "已实现" else "规格已定", if (impl == "live") Tone.Ok else Tone.Neutral)
        }
    }
}

@Composable
private fun DecisionRow(
    topic: String,
    winner: String,
    take: String,
    drop: String,
    why: String,
    from: String
) {
    val a = LocalAccents.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(topic, style = MaterialTheme.typography.headlineSmall, color = a.ink)
            Spacer(Modifier.width(8.dp))
            Pill(winner, Tone.Champagne)
        }
        Spacer(Modifier.height(8.dp))
        Text("取：$take", style = MaterialTheme.typography.bodySmall, color = a.ok)
        Spacer(Modifier.height(4.dp))
        Text("舍：$drop", style = MaterialTheme.typography.bodySmall, color = a.inkFaint)
        Spacer(Modifier.height(6.dp))
        Text("为什么：$why", style = MaterialTheme.typography.bodySmall, color = a.inkSub)
        Spacer(Modifier.height(5.dp))
        Text("出处：$from", style = MaterialTheme.typography.labelSmall, color = a.inkFaint)
    }
}
