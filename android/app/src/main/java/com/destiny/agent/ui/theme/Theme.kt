package com.destiny.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 圆角体系：卡片 16 / 行 12 / 药丸 999（Apple HIG 10 + Pinterest 柔和化） */
val DestinyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val DarkScheme = darkColorScheme(
    primary = Champagne,
    onPrimary = Obsidian900,
    primaryContainer = ChampagneWash,
    onPrimaryContainer = ChampagneBright,
    secondary = InkSecondary,
    onSecondary = Obsidian850,
    tertiary = SignalInfo,
    onTertiary = Obsidian900,
    background = Obsidian850,
    onBackground = InkPrimary,
    surface = Obsidian800,
    onSurface = InkPrimary,
    surfaceVariant = Obsidian750,
    onSurfaceVariant = InkSecondary,
    surfaceContainer = Obsidian800,
    surfaceContainerHigh = Obsidian750,
    surfaceContainerHighest = Obsidian700,
    surfaceContainerLow = Obsidian850,
    surfaceContainerLowest = Obsidian900,
    outline = Obsidian600,
    outlineVariant = Color(0xFF2A2A33),
    error = SignalDanger,
    onError = Color(0xFF1A0F0F),
    errorContainer = Color(0x332B1A1A),
    onErrorContainer = Color(0xFFE8A9A4),
    scrim = Color(0xFF000000)
)

private val LightScheme = lightColorScheme(
    primary = PaperChampagne,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E7C4),
    onPrimaryContainer = Color(0xFF4A3A08),
    secondary = PaperInkSub,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF3F6E9E),
    onTertiary = Color(0xFFFFFFFF),
    background = Paper000,
    onBackground = PaperInk,
    surface = Color(0xFFFFFFFF),
    onSurface = PaperInk,
    surfaceVariant = Paper050,
    onSurfaceVariant = PaperInkSub,
    surfaceContainer = Paper050,
    surfaceContainerHigh = Paper050,
    surfaceContainerHighest = Paper100,
    surfaceContainerLow = Paper000,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Paper200,
    outlineVariant = Paper100,
    error = Color(0xFF9B3A33),
    onError = Color(0xFFFFFFFF),
    scrim = Color(0x66000000)
)

/** 语义色（不进入 Material 色板，供业务组件直接引用） */
data class DestinyAccents(
    val ink: Color,
    val inkSub: Color,
    val inkFaint: Color,
    val champagne: Color,
    val champagneBright: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val info: Color,
    val hairline: Color,
    val elevated: Color,
    val agentTint: Color
)

private val DarkAccents = DestinyAccents(
    ink = InkPrimary,
    inkSub = InkSecondary,
    inkFaint = InkTertiary,
    champagne = Champagne,
    champagneBright = ChampagneBright,
    ok = SignalOK,
    warn = SignalWarn,
    danger = SignalDanger,
    info = SignalInfo,
    hairline = Color(0xFF23232B),
    elevated = Obsidian750,
    agentTint = Color(0xFF5B87B8)
)

private val LightAccents = DestinyAccents(
    ink = PaperInk,
    inkSub = PaperInkSub,
    inkFaint = Color(0xFF8C877D),
    champagne = PaperChampagne,
    champagneBright = PaperChampagneBright,
    ok = Color(0xFF3F7A56),
    warn = Color(0xFFA9701F),
    danger = Color(0xFF9B3A33),
    info = Color(0xFF3F6E9E),
    hairline = Paper100,
    elevated = Paper050,
    agentTint = Color(0xFF3F6E9E)
)

val LocalAccents = staticCompositionLocalOf { DarkAccents }

@Composable
fun DestinyTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accents = if (dark) DarkAccents else LightAccents
    CompositionLocalProvider(LocalAccents provides accents) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = DestinyTypography,
            shapes = DestinyShapes,
            content = content
        )
    }
}
