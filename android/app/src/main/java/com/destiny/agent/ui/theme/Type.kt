package com.destiny.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * 排版系统 · Hark 规范版
 *
 * 一套无衬线族（Hark 禁用衬线展示字与艺术字体）。中文落系统黑体，
 * 中英混排不会出现"拉丁好看、中文变衬线"的割裂。
 *
 * 层级阶梯（h1 标题层 → h2 正文带 → h3 附属）：
 *   h1  28 / 22 / 18   Bold     ← 页面标题、区块标题、关键数字
 *   h2  15 / 14 / 13   Regular  ← 正文
 *   h3  13 / 12 / 11   Medium   ← 标签、时间戳、辅助说明
 * 主要层级差 = 18 → 15/14 = 4sp ✓（相邻标题层差 6 / 4 ✓）
 *
 * 层级只靠字号 + 字重 + 间距建立，不用颜色强行区分。
 */
private val Body = FontFamily.SansSerif

private val lh = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val DestinyTypography = Typography(
    // ── h1 标题层 ──
    displayLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp,
        lineHeightStyle = lh
    ),
    displayMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
        lineHeightStyle = lh
    ),
    displaySmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    headlineLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    headlineMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    headlineSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    titleLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    titleMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    titleSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        lineHeightStyle = lh
    ),

    // ── h2 正文带 ──
    bodyLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    bodyMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    bodySmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        lineHeightStyle = lh
    ),

    // ── h3 附属 ──
    labelLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        lineHeightStyle = lh
    ),
    labelMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp,
        lineHeightStyle = lh
    ),
    labelSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp,
        lineHeightStyle = lh
    )
)
