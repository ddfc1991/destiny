package com.destiny.agent.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destiny.agent.ui.theme.LocalAccents

/** 底栏五个目的地 —— 直接对应产品定位 */
enum class Tab(
    val label: String,
    val iconRes: Int,
    val hint: String
) {
    Talk("对话", com.destiny.agent.R.drawable.ic_chat, "说一句话，它去办事"),
    Proactive("主动", com.destiny.agent.R.drawable.ic_bolt, "它自己发起的动作"),
    Memory("记忆", com.destiny.agent.R.drawable.ic_brain, "它记得的关于你的事"),
    Cloud("云电脑", com.destiny.agent.R.drawable.ic_monitor, "在云端电脑上操作网页"),
    Me("我的", com.destiny.agent.R.drawable.ic_user, "账户 · 模块 · 设置")
}

/**
 * 自绘底部导航
 * - 选中项：香槟金胶囊底 + 图标上浮 + 文字加粗
 * - 未选中：仅图标，弱化
 * - 触摸目标 ≥ 48dp
 */
@Composable
fun DestinyTabBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0
) {
    val a = LocalAccents.current
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(a.hairline)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == current,
                    badge = if (tab == Tab.Proactive && pendingCount > 0) pendingCount else 0,
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    selected: Boolean,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val a = LocalAccents.current
    val tint by animateColorAsState(
        if (selected) a.champagne else a.inkFaint,
        tween(180), label = "tabTint"
    )
    val scale by animateFloatAsState(
        if (selected) 1f else 0.94f,
        tween(180), label = "tabScale"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        Modifier
            .width(62.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .height(30.dp)
                .width(if (selected) 46.dp else 34.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) a.champagne.copy(alpha = 0.14f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier
                    .size(21.dp)
                    .scale(scale)
            )
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-5).dp)
                        .size(if (badge > 9) 16.dp else 14.dp)
                        .clip(CircleShape)
                        // Hark：禁用红色角标，改用低饱和浅蓝弱提示
                        .background(a.info),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
