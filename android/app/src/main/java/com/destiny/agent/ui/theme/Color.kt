package com.destiny.agent.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Destiny · 曜石香槟（Obsidian & Champagne）
 *
 * 方向定位：不做"科技蓝紫渐变"的通用 AI 味，也不做 mibot 的暖瓷家居感。
 * 走「奢华金融 / 编辑排版」路线 —— 墨黑底 + 香槟金单强调色 + 衬线展示字，
 * 传达"这是一个替你做事的私人管家，而不是又一个聊天机器人"。
 *
 * 配比（frontend-design 基准）：主色 60% / 辅助 30% / 强调 10%。
 * 强调色全 App 只用香槟金一处，绝不彩虹渐变。
 */

// ── 墨黑基座（深色为主基调：Agent 在后台运行，深色更贴合"在场但不打扰"） ──
val Obsidian900 = Color(0xFF0A0A0C)   // 最底
val Obsidian850 = Color(0xFF101014)   // 页面底
val Obsidian800 = Color(0xFF16161B)   // 卡片
val Obsidian750 = Color(0xFF1C1C22)   // 卡片抬升
val Obsidian700 = Color(0xFF23232B)   // 输入/悬浮
val Obsidian600 = Color(0xFF2E2E38)   // 分隔

// ── 香槟金（唯一强调色） ──
val Champagne = Color(0xFFC9A227)
val ChampagneBright = Color(0xFFE0BC52)
val ChampagneDim = Color(0xFF8A6F1A)
val ChampagneWash = Color(0x1FC9A227)

// ── 文本层级 ──
val InkPrimary = Color(0xFFF2F0EC)    // 正文/标题
val InkSecondary = Color(0xFFA8A5AE)  // 次要
val InkTertiary = Color(0xFF6E6B76)   // 弱化/时间戳
val InkDisabled = Color(0xFF4A4850)

// ── 语义状态（克制使用，不做大面积色块） ──
val SignalOK = Color(0xFF4E9A6B)       // 已完成 / 通过
val SignalWarn = Color(0xFFD08C3A)     // 等你决定
val SignalDanger = Color(0xFFB5504A)   // 红线 / 拒绝
val SignalInfo = Color(0xFF5B87B8)     // 系统 / 引擎

// ── 日间模式（暖白纸感，做长文阅读与白天使用） ──
val Paper000 = Color(0xFFFBFAF7)
val Paper050 = Color(0xFFF4F2EC)
val Paper100 = Color(0xFFEDEAE2)
val Paper200 = Color(0xFFE0DCD2)
val PaperInk = Color(0xFF1A1917)
val PaperInkSub = Color(0xFF5E5A52)
val PaperChampagne = Color(0xFF9A7B18)
val PaperChampagneBright = Color(0xFFC9A227)
