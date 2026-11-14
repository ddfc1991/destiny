package com.destiny.agent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.destiny.agent.ui.theme.HarkRadius
import com.destiny.agent.ui.theme.LocalAccents

/* ════════════════════════════════════════════════════════════
   核心组件库 · Hark 规范版
   圆角：所有可交互元素统一 8pt，不随尺寸放大
   阴影：不透明度 22%，模糊 12，偏移 2（≤16px）
   栅格：所有间距取 8 的倍数
   强调色：香槟金仅用于「当前 / 需要你 / 主操作」
   ════════════════════════════════════════════════════════════ */

private val R = RoundedCornerShape(HarkRadius)

/** 页面标题：大字 + 可选副标 */
@Composable
fun PageTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(bottom = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.displayMedium,
            color = LocalAccents.current.ink
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAccents.current.inkSub
            )
        }
    }
}

/** 分组小标题：上方一条 hairline + 字距拉开的标签 */
@Composable
fun SectionLabel(text: String, count: Int? = null, modifier: Modifier = Modifier) {
    val a = LocalAccents.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = a.inkFaint
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = a.champagne
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .weight(2f)
                .height(1.dp)
                .background(a.hairline)
        )
    }
}

/** L1 强调面板：纯色抬升面 + 细边框（无装饰性渐变） */
@Composable
fun HeroPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val a = LocalAccents.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(R)
            .background(a.elevated)
            .border(1.dp, a.champagne.copy(alpha = 0.22f), R)
            .padding(24.dp)
    ) { content() }
}

/** L2 面板：标准卡片（阴影 22% / 模糊 12） */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    border: Color? = null,
    content: @Composable () -> Unit
) {
    val a = LocalAccents.current
    Box(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = R,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(R)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, border ?: a.hairline, R)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(padding)
    ) { content() }
}

/** L3 列表行：图标 + 标题 + 副标 + 值 + 箭头 */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    icon: Painter? = null,
    iconTint: Color? = null,
    iconFilled: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val a = LocalAccents.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconChip(
                icon = icon,
                tint = iconTint ?: a.inkSub,
                filled = iconFilled
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = a.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = a.inkSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = a.inkFaint,
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (showChevron && onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = Icons.ChevronRight,
                contentDescription = null,
                tint = a.inkFaint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 图标底片：细边框圆角方块 */
@Composable
fun IconChip(
    icon: Painter,
    tint: Color? = null,
    filled: Boolean = false,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val a = LocalAccents.current
    val t = tint ?: a.inkSub
    Box(
        Modifier
            .size(size)
            .clip(R)
            .background(if (filled) t.copy(alpha = 0.14f) else a.elevated)
            .border(
                1.dp,
                if (filled) t.copy(alpha = 0.26f) else a.hairline,
                R
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = t,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 药丸标签：语义色（ok / warn / danger / info / champagne / neutral） */
@Composable
fun Pill(
    text: String,
    tone: Tone = Tone.Neutral,
    modifier: Modifier = Modifier
) {
    val a = LocalAccents.current
    val c = when (tone) {
        Tone.Ok -> a.ok
        Tone.Warn -> a.warn
        Tone.Danger -> a.danger
        Tone.Info -> a.info
        Tone.Champagne -> a.champagne
        Tone.Neutral -> a.inkFaint
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(c.copy(alpha = 0.13f))
            .border(1.dp, c.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = c,
            maxLines = 1
        )
    }
}

enum class Tone { Ok, Warn, Danger, Info, Champagne, Neutral }

/** 主操作按钮：香槟金实心（文字用 onPrimary 保证对比度达标） */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val a = LocalAccents.current
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(R)
            .background(if (enabled) a.champagne else a.hairline)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (enabled) onPrimary else a.inkFaint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) onPrimary else a.inkFaint
        )
    }
}

/** 次操作按钮：描边 */
@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    tone: Color? = null,
    onClick: () -> Unit
) {
    val a = LocalAccents.current
    val t = tone ?: a.inkSub
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(R)
            .border(1.dp, a.hairline, R)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = t, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = t)
    }
}

/** 数据格：数字 + 标签 */
@Composable
fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val a = LocalAccents.current
    Column(modifier.padding(vertical = 4.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            color = if (accent) a.champagne else a.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = a.inkFaint
        )
    }
}

/** 状态点 */
@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/** 细分隔线 */
@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalAccents.current.hairline)
    )
}

/** 空态：细边框框 + 一句说明 */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val a = LocalAccents.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(R)
            .border(1.dp, a.hairline, R)
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = a.inkFaint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = a.ink
        )
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = a.inkSub
        )
    }
}
