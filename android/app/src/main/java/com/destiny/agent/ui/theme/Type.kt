package com.destiny.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * 排版系统：衬线展示字 + 无衬线正文（编辑排版风）
 *
 * 展示字用系统衬线族（设备上落 Noto Serif / Noto Serif CJK），中文直接命中
 * 衬线中文字形，不需要额外字体文件，也不会出现"拉丁字好看、中文变黑体"的割裂。
 * 正文用无衬线，字距收紧、行高放松 —— 长对话与长文都耐读。
 */
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

private val lh = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val DestinyTypography = Typography(
    // 品牌标识 / 页面主标题
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp,
        lineHeightStyle = lh
    ),
    displayMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp,
        lineHeightStyle = lh
    ),
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = (-0.4).sp,
        lineHeightStyle = lh
    ),
    // 区块标题
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp,
        lineHeightStyle = lh
    ),
    headlineMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp,
        lineHeightStyle = lh
    ),
    headlineSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp,
        lineHeightStyle = lh
    ),
    // 正文
    bodyLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    bodyMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.sp,
        lineHeightStyle = lh
    ),
    bodySmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        lineHeightStyle = lh
    ),
    // 标签
    labelLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp,
        lineHeightStyle = lh
    ),
    labelMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp,
        lineHeightStyle = lh
    ),
    labelSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.6.sp,
        lineHeightStyle = lh
    )
)
