package com.destiny.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.destiny.agent.R

/**
 * 图标索引：全部走 res/drawable 里的原生 VectorDrawable（由 Web 版 Lucide 路径转换而来），
 * 弧线已统一转为三次贝塞尔，任何 API 级别渲染一致；不引第三方图标库。
 *
 * painterResource 是 @Composable 的，所以这里用「@Composable getter」暴露 Painter：
 * 只能在组合上下文里取（Icon(painter = Icons.Bolt) ✓）。
 * 需要在组合上下文之外用（枚举常量、顶层常量）时，改用 res/drawable 的资源 ID。
 */
object Icons {
    // 导航
    val Chat: Painter
    @Composable get() = painterResource(R.drawable.ic_chat)

    val Calendar: Painter
    @Composable get() = painterResource(R.drawable.ic_calendar)

    val Compass: Painter
    @Composable get() = painterResource(R.drawable.ic_compass)

    val Bulb: Painter
    @Composable get() = painterResource(R.drawable.ic_bulb)

    val User: Painter
    @Composable get() = painterResource(R.drawable.ic_user)

    val Users: Painter
    @Composable get() = painterResource(R.drawable.ic_users)

    val Home: Painter
    @Composable get() = painterResource(R.drawable.ic_home)

    // 动作
    val Plus: Painter
    @Composable get() = painterResource(R.drawable.ic_plus)

    val Send: Painter
    @Composable get() = painterResource(R.drawable.ic_send_fill)

    val Mic: Painter
    @Composable get() = painterResource(R.drawable.ic_mic)

    val Clip: Painter
    @Composable get() = painterResource(R.drawable.ic_clip)

    val ChevronRight: Painter
    @Composable get() = painterResource(R.drawable.ic_chev_r)

    val ChevronLeft: Painter
    @Composable get() = painterResource(R.drawable.ic_chev_l)

    val ChevronDown: Painter
    @Composable get() = painterResource(R.drawable.ic_chevron_down)

    val ChevronUp: Painter
    @Composable get() = painterResource(R.drawable.ic_chevron_up)

    val Search: Painter
    @Composable get() = painterResource(R.drawable.ic_search)

    val Close: Painter
    @Composable get() = painterResource(R.drawable.ic_close)

    val Check: Painter
    @Composable get() = painterResource(R.drawable.ic_check)

    val CheckCircle: Painter
    @Composable get() = painterResource(R.drawable.ic_check_circle)

    val Alert: Painter
    @Composable get() = painterResource(R.drawable.ic_alert)

    val Edit: Painter
    @Composable get() = painterResource(R.drawable.ic_edit)

    val Trash: Painter
    @Composable get() = painterResource(R.drawable.ic_trash)

    val Copy: Painter
    @Composable get() = painterResource(R.drawable.ic_copy)

    val More: Painter
    @Composable get() = painterResource(R.drawable.ic_more)

    val Refresh: Painter
    @Composable get() = painterResource(R.drawable.ic_refresh)

    val Download: Painter
    @Composable get() = painterResource(R.drawable.ic_download)

    val ArrowUp: Painter
    @Composable get() = painterResource(R.drawable.ic_arrow_up)

    val ArrowUpRight: Painter
    @Composable get() = painterResource(R.drawable.ic_arrow_up_right)

    val Filter: Painter
    @Composable get() = painterResource(R.drawable.ic_filter)

    val Settings: Painter
    @Composable get() = painterResource(R.drawable.ic_settings)

    val Sliders: Painter
    @Composable get() = painterResource(R.drawable.ic_sliders)

    // 主题
    val Sun: Painter
    @Composable get() = painterResource(R.drawable.ic_sun)

    val Moon: Painter
    @Composable get() = painterResource(R.drawable.ic_moon)

    val Spark: Painter
    @Composable get() = painterResource(R.drawable.ic_spark_fill)

    val Palette: Painter
    @Composable get() = painterResource(R.drawable.ic_palette)

    // 三大核心能力
    val Bolt: Painter
    @Composable get() = painterResource(R.drawable.ic_bolt)

    val Brain: Painter
    @Composable get() = painterResource(R.drawable.ic_brain)

    val Monitor: Painter
    @Composable get() = painterResource(R.drawable.ic_monitor)

    val Wand: Painter
    @Composable get() = painterResource(R.drawable.ic_wand)

    val Gauge: Painter
    @Composable get() = painterResource(R.drawable.ic_gauge)

    val Terminal: Painter
    @Composable get() = painterResource(R.drawable.ic_terminal)

    val Cpu: Painter
    @Composable get() = painterResource(R.drawable.ic_cpu)

    val Wifi: Painter
    @Composable get() = painterResource(R.drawable.ic_wifi)

    val Globe: Painter
    @Composable get() = painterResource(R.drawable.ic_globe)

    // 功能
    val Shield: Painter
    @Composable get() = painterResource(R.drawable.ic_shield)

    val ShieldCheck: Painter
    @Composable get() = painterResource(R.drawable.ic_shield_check)

    val Lock: Painter
    @Composable get() = painterResource(R.drawable.ic_lock)

    val Key: Painter
    @Composable get() = painterResource(R.drawable.ic_key)

    val Cloud: Painter
    @Composable get() = painterResource(R.drawable.ic_cloud)

    val Sync: Painter
    @Composable get() = painterResource(R.drawable.ic_sync)

    val Logout: Painter
    @Composable get() = painterResource(R.drawable.ic_logout)

    val Clock: Painter
    @Composable get() = painterResource(R.drawable.ic_clock)

    val ClockCircle: Painter
    @Composable get() = painterResource(R.drawable.ic_clock_circle)

    val CalendarCheck: Painter
    @Composable get() = painterResource(R.drawable.ic_calendar_check)

    val Book: Painter
    @Composable get() = painterResource(R.drawable.ic_book)

    val Article: Painter
    @Composable get() = painterResource(R.drawable.ic_article)

    val Play: Painter
    @Composable get() = painterResource(R.drawable.ic_play)

    val Link: Painter
    @Composable get() = painterResource(R.drawable.ic_link)

    val Mail: Painter
    @Composable get() = painterResource(R.drawable.ic_mail)

    val Inbox: Painter
    @Composable get() = painterResource(R.drawable.ic_inbox)

    val Bell: Painter
    @Composable get() = painterResource(R.drawable.ic_bell)

    val Database: Painter
    @Composable get() = painterResource(R.drawable.ic_database)

    val Route: Painter
    @Composable get() = painterResource(R.drawable.ic_route)

    val Layers: Painter
    @Composable get() = painterResource(R.drawable.ic_layers)

    val Chart: Painter
    @Composable get() = painterResource(R.drawable.ic_chart)

    val Activity: Painter
    @Composable get() = painterResource(R.drawable.ic_activity)

    val List: Painter
    @Composable get() = painterResource(R.drawable.ic_list)

    val Box: Painter
    @Composable get() = painterResource(R.drawable.ic_box)

    val Wallet: Painter
    @Composable get() = painterResource(R.drawable.ic_wallet)

    val Grid: Painter
    @Composable get() = painterResource(R.drawable.ic_grid)

    val CircleDot: Painter
    @Composable get() = painterResource(R.drawable.ic_circle_dot)

    val MessageSquare: Painter
    @Composable get() = painterResource(R.drawable.ic_message_square)

    val GitBranch: Painter
    @Composable get() = painterResource(R.drawable.ic_git_branch)

    val Target: Painter
    @Composable get() = painterResource(R.drawable.ic_target)

    val Eye: Painter
    @Composable get() = painterResource(R.drawable.ic_eye)

    val File: Painter
    @Composable get() = painterResource(R.drawable.ic_file)

    val Info: Painter
    @Composable get() = painterResource(R.drawable.ic_info)

    val Plug: Painter
    @Composable get() = painterResource(R.drawable.ic_plug)

    val Scan: Painter
    @Composable get() = painterResource(R.drawable.ic_scan)

    val Fingerprint: Painter
    @Composable get() = painterResource(R.drawable.ic_fingerprint)

    val Bot: Painter
    @Composable get() = painterResource(R.drawable.ic_bot)

    val Phone: Painter
    @Composable get() = painterResource(R.drawable.ic_phone)

    val Image: Painter
    @Composable get() = painterResource(R.drawable.ic_image)

    val Star: Painter
    @Composable get() = painterResource(R.drawable.ic_star)

    val MessageSquareAlt: Painter
    @Composable get() = painterResource(R.drawable.ic_message_square)
}
