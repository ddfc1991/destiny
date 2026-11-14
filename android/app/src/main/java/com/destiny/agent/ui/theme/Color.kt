package com.destiny.agent.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Destiny · 曜石香槟（Hark 合规版）
 *
 * 方向不变：墨黑基座 + 香槟金单强调色 —— 传达"这是一个替你做事的私人管家"。
 * 但按 Hark Pro 规则重新定值：全色板 HSL 饱和度 ≤ 0.28（莫兰迪低饱和），
 * 单页色彩 ≤ 4，状态色只做文字/图标弱提示，绝不做色块或红色角标。
 *
 * 饱和度实测（HSL S 值）：
 *   Champagne #B09278 = 0.262 · ChampagneBright #C3AC98 = 0.264
 *   OK #5E8C6E = 0.197 · Warn #9C8560 = 0.238
 *   Danger #A56E6A = 0.247 · Info #7C93A6 = 0.191
 * 对比度：Champagne / 墨底 = 6.81:1 · PaperChampagne / 纸底 = 5.60:1
 */

// ── 墨黑基座（深色为主基调：Agent 在后台运行，深色更贴合"在场但不打扰"） ──
val Obsidian900 = Color(0xFF0A0A0C)   // 最底
val Obsidian850 = Color(0xFF101014)   // 页面底
val Obsidian800 = Color(0xFF16161B)   // 卡片
val Obsidian750 = Color(0xFF1C1C22)   // 卡片抬升
val Obsidian700 = Color(0xFF23232B)   // 输入/悬浮
val Obsidian600 = Color(0xFF2E2E38)   // 分隔

// ── 香槟金（唯一强调色 · 低饱和版） ──
val Champagne = Color(0xFFB09278)
val ChampagneBright = Color(0xFFC3AC98)
val ChampagneDim = Color(0xFF7A6048)
val ChampagneWash = Color(0x1FB09278)

// ── 文本层级 ──
val InkPrimary = Color(0xFFF2F0EC)    // 正文/标题
val InkSecondary = Color(0xFFA8A5AE)  // 次要
val InkTertiary = Color(0xFF82808A)   // 弱化/时间戳（对墨底 5.09:1）
val InkDisabled = Color(0xFF4A4850)

// ── 语义状态（低饱和弱提示：只出现在文字/图标，绝不做大面积色块） ──
val SignalOK = Color(0xFF5E8C6E)       // 已完成 / 通过
val SignalWarn = Color(0xFF9C8560)     // 等你决定
val SignalDanger = Color(0xFFA56E6A)   // 红线 / 拒绝（仅文字）
val SignalInfo = Color(0xFF7C93A6)     // 系统 / 引擎

// ── 日间模式（暖白纸感，做长文阅读与白天使用） ──
val Paper000 = Color(0xFFF5F3F0)
val Paper050 = Color(0xFFEDEAE2)
val Paper100 = Color(0xFFE3DFD8)
val Paper200 = Color(0xFFD6D1C7)
val PaperInk = Color(0xFF1A1917)
val PaperInkSub = Color(0xFF5E5A52)
val PaperChampagne = Color(0xFF7A6048)
val PaperChampagneBright = Color(0xFF9C8560)
