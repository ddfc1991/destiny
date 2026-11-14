package com.destiny.agent.data

/**
 * Destiny 数据源（以 mibot 为主干）· 原生 Kotlin 移植版
 *
 * 主干 = mibot 真机工程里的真实模块（HarkTokens / MibotUi / 首页能力组 / GlassNavigation）
 * 融合层 = Mika 挑走的 9 块 + 两边重叠后重新定义的能力
 * 本文件只放规格数据，不含界面逻辑。
 */
object DestinyData {

    // ── 融合图谱 ─────────────────────────────────────────────────────────
    data class MikaGroup(val id: String, val name: String, val note: String)

    data class MikaItem(
        val n: Int,
        val g: String,
        val t: String,
        val picked: Boolean
    )

    val MIKA_GROUPS = listOf(
        MikaGroup("ops", "屏幕与操控", "决定它能不能真干活"),
        MikaGroup("mem", "记忆与学习", "越用越懂你"),
        MikaGroup("time", "定时与条件", "不用你催"),
        MikaGroup("talk", "沟通与人", "mibot 完全没有的一整块"),
        MikaGroup("web", "浏览与文件", "网页是一等公民"),
        MikaGroup("cost", "成本与模型", "自托管最该有")
    )

    val MIKA = listOf(
        MikaItem(1, "ops", "后台屏 · 多任务并行", true),
        MikaItem(2, "ops", "应用访问 · 按 App 放行", true),
        MikaItem(3, "ops", "恢复与修复 · 掉线自愈", true),
        MikaItem(4, "ops", "兼容性自检 · 9 项体检", false),
        MikaItem(5, "mem", "记忆三分法 + 待批准", true),
        MikaItem(6, "mem", "记住的操作路线", true),
        MikaItem(7, "mem", "App / 站点知识库", false),
        MikaItem(8, "time", "条件触发 · 跟上一次比", false),
        MikaItem(9, "time", "保存前预览三次执行", false),
        MikaItem(10, "time", "结果回到原对话", false),
        MikaItem(11, "talk", "AI 接打电话", true),
        MikaItem(12, "talk", "微信派单", true),
        MikaItem(13, "talk", "远程网关配对", true),
        MikaItem(14, "web", "内置浏览器", true),
        MikaItem(15, "web", "保留网页 · keep open", false),
        MikaItem(16, "web", "划词追问", false),
        MikaItem(17, "web", "文件库 · 跨会话找东西", false),
        MikaItem(18, "cost", "Token 用量页", false),
        MikaItem(19, "cost", "单任务 Token 预算", false),
        MikaItem(20, "cost", "运行统计 · 拆开看", false),
        MikaItem(21, "cost", "按住说话", false)
    )

    val MIBOT = listOf(
        "会话", "动态", "点子", "Bots 群聊", "AI 邮箱", "工作台", "账号台账", "服务商",
        "AI 网关", "模型组", "沙箱 / 终端", "文件", "技能", "记忆", "MCP",
        "定时任务", "人格", "后台操控 / 虚拟屏", "DSH 插件市场", "诊断", "Tailscale 远程接入"
    )

    data class Fusion(
        val id: String,
        val name: String,
        val from: String,
        val to: String,
        val impl: String,
        val d: String
    )

    val FUSION = listOf(
        Fusion("chat", "一句话派活", "C", "chat", "live", "你只说一句目的，Destiny 拆成任务；不用先选 App、不用先配工具。"),
        Fusion("router", "能力路由 · 技能 / MCP / APP", "mibot", "ability", "live", "三类东西都是「能力」。挑之前先读每条自述，含「什么时候不该用我」，所以问今天几号不会开浏览器。"),
        Fusion("vdisplay", "虚拟屏多开与错峰", "C", "screens", "live", "mibot 的后台操控 + Mika 的独立后台屏合并：4 个槽位、按 App 加锁、排队时明说在等谁。看起来同时，实际错开。"),
        Fusion("timeline", "运行过程 · 逐步时间线", "Mika", "run", "live", "每步按时间往下长，写清点了什么、输入方式、目标控件、耗时；任务跑着也能看。"),
        Fusion("gate", "授权闸门 · 按风险分层", "C", "gate", "live", "mibot 的三档审批 + Mika 的「只有花钱才问」合并：信任按风险分层。花钱先问，发消息直接做，登录验证码两种模式都问。"),
        Fusion("interrupt", "随时插话 / 随时停", "Mika", "run", "live", "任务进行中补充或更正，下一步之前读到；按停止会补几行交代，下次「继续」知道从哪接。"),
        Fusion("resume", "断点恢复 / 失败归因", "mibot", "run", "live", "接上之前先核三件事；失败固定三行：哪一步、为什么、能做什么。"),
        Fusion("supervise", "督查与检阅", "mibot", "run", "live", "它先自己核再交给你；待检阅、打回、一次过率都记下来。"),
        Fusion("memory", "记忆三分法 · 做法待批准", "Mika", "mem", "live", "关于你 / 偏好 / 做法三类。做法要你点头才生效；密码、验证码、卡号不记。"),
        Fusion("route", "操作路线 · 越用越快", "Mika", "ability", "live", "同一件事办成功后沉淀成路线，第二次显示「按以往做法快速执行」；失效会自己停用。"),
        Fusion("knowledge", "App / 站点知识库", "Mika", "ability", "live", "24 个 App + 23 个网站的使用知识，纯文本可补充：入口在哪、常见做法、坑在哪。"),
        Fusion("plans", "定时与条件任务", "C", "plans", "live", "mibot 的 cron + Mika 的条件触发合并：跟上一次比、一天最多一次、保存前预览接下来三次、结果回原对话。"),
        Fusion("gateway", "本地网关 · 自动切换", "mibot", "gateway", "live", "127.0.0.1:4000 跑在本机，Key 不出门。连错 3 次 / 延迟 >3s / 401-402 立刻切；切回要稳定 10 分钟。"),
        Fusion("sandbox", "沙箱 · 多发行版", "mibot", "sandbox", "spec", "Debian / Ubuntu / Alpine / Arch 四选一，资源上限与联网三档。手机跑不了 Docker，用 proot。"),
        Fusion("accounts", "身份隔离 · 三层结构", "mibot", "vault", "spec", "身份 → 组 → 会话。台账管「登录态」，网关管「API Key」，两者分开；一个号被风控，另一个先停 24h。"),
        Fusion("mail", "AI 邮箱 · 专属地址", "mibot", "mail", "spec", "服务↔别名邮箱绑定，取到的验证码自动填入；收不到就老实说收不到。"),
        Fusion("browser", "内置浏览器", "Mika", "screens", "spec", "后台开真网页、电脑版 UA、保留登录、翻页有节奏，认出风控页就交人工，不硬闯。"),
        Fusion("appaccess", "应用访问 · 金融类默认禁用", "Mika", "appaccess", "live", "逐 App 放行；金融交易类自动列入「推荐禁用」。Shell、沙箱、MCP 不受此限。"),
        Fusion("recover", "恢复与修复 · 掉线自愈", "Mika", "recover", "live", "无障碍 / 免省电 / 无线调试逐项自查，失败会降级并说明卡在哪，不静默死掉。"),
        Fusion("handover", "接管 · 该你亲手做的", "Mika", "gate", "live", "登录、验证码、付款停下，App 挪到主屏；你处理完交还，它重新核实再继续。"),
        Fusion("call", "AI 接打电话", "Mika", "me", "spec", "代接快递外卖、代打预约，接听交代带有效期，人工随时接管。"),
        Fusion("wechat", "微信派单", "Mika", "vault", "spec", "扫码绑定后微信私聊直接派任务，双向会话，不是单向通知。"),
        Fusion("pair", "网关配对码", "Mika", "gateway", "spec", "一次性配对码替代裸 token；断线自动重连，失效明确说「请重新配对」。"),
        Fusion("feed", "动态 · 在看 / 想到的 / 能领的", "mibot", "feed", "live", "点子并进动态做第二态。能领的额度按「能不能挂进网关」排，不按折合人民币排。"),
        Fusion("groups", "Bots 群聊 · 共享黑板", "mibot", "groups", "spec", "只共享「一句话的事实」，不共享过程；过程进各自会话，不上黑板。你亲手写的优先级最高。"),
        Fusion("voice", "按住说话", "Mika", "me", "spec", "松手默认填进输入框给改，不抢着发；任务在跑时发出去就是插话。"),
        Fusion("usage", "用量 / Token 预算 / 运行统计", "C", "usage", "live", "mibot 的用量页 + Mika 的单任务预算与四段耗时合并。"),
        Fusion("redline", "红线 · 问也不许做", "mibot", "gate", "live", "人机验证、人脸实名、单笔超过 ¥500、改系统安全设置 —— 四条固定不可授权。"),
        Fusion("dsh", "DSH 插件 · 三步才算装上", "mibot", "plugins", "spec", "下载 → 校验 → 依赖，三步全绿才叫装上；缺运行时直接给两条路。"),
        Fusion("tailscale", "Tailscale 远程接入", "mibot", "gateway", "spec", "地址变成 169.254 就是没连上，不是网络慢；出口节点默认关。"),
        Fusion("diag", "诊断包 / 自检 9 项", "C", "diag", "live", "一键收机型 / 权限 / 结果，可复制可导出；自检如实标出哪些项浏览器里判定不了。"),
        Fusion("appearance", "外观 · 暖瓷 / 夜 · 字号", "mibot", "appearance", "live", "默认暖瓷浅色 + 夜双主题、字号档位、文字对比度保护；照片只在本机。")
    )

    // ── 引擎所需数组 ─────────────────────────────────────────────────────
    data class Skill(
        val id: String,
        val name: String,
        val kind: String,
        val when_: String,
        val not: String,
        val on: Boolean,
        val authority: String
    )

    val SKILLS = listOf(
        Skill("sk-file", "文件系统", "技能", "读、写、整理本地文件", "不碰沙箱外路径", true, "只放行两个目录"),
        Skill("sk-route", "操作路线", "技能", "同一 App 里办过的事再办一次", "路线失效时不要硬跑", true, "只读 + 点击"),
        Skill("sk-ocr", "读屏 OCR", "技能", "控件没有名字、界面读不出来", "有人脸或验证码时不要用", true, "仅当前虚拟屏"),
        Skill("sk-search", "网页搜索", "技能", "需要外部最新信息", "搜索内容一律按不可信处理", true, "免费源 100 次/天")
    )

    data class McpEntry(
        val id: String,
        val name: String,
        val type: String,
        val tools: Int,
        val on: Boolean,
        val perm: String
    )

    val MCP = listOf(
        McpEntry("mcp-sqlite", "sqlite", "stdio · 本地", 6, true, "算数一律走它，不让模型心算"),
        McpEntry("mcp-github", "github", "http · 远程", 11, false, "只读；写操作每次批准"),
        McpEntry("mcp-fs", "filesystem", "stdio · 本地", 8, true, "只碰 Downloads/Destiny 与工作区"),
        McpEntry("mcp-browser", "browser", "stdio · 本地", 3, false, "缺 chromium，启动会失败")
    )

    data class AppKnowledge(val n: String, val k: String, val hint: String)

    val APP_KNOWLEDGE = listOf(
        AppKnowledge("微信", "App", "联系人从搜索进；聊天记录要逐屏翻"),
        AppKnowledge("飞书", "App", "日报推送走群机器人 webhook"),
        AppKnowledge("小红书", "App", "分类要点两次；发布前会弹确认"),
        AppKnowledge("闲鱼", "App", "卖闲置按钮在右下角；多账号要隔离"),
        AppKnowledge("支付宝", "App", "金融类 · 推荐禁用"),
        AppKnowledge("抖音", "App", "长列表逐屏读，别一次滑到底"),
        AppKnowledge("B 站", "App", "评论区折叠层级多"),
        AppKnowledge("知乎", "App", "回答页有折叠，先展开再读"),
        AppKnowledge("GitHub", "站点", "电脑版 UA；风控页交人工"),
        AppKnowledge("aihot.news", "站点", "免费额度按能不能挂进网关排序")
    )

    data class Provider(val n: String, val ms: Int, val ok: String, val state: String, val keys: Int)

    val PROVIDERS = listOf(
        Provider("本地网关 · 127.0.0.1:4000", 210, "99.4%", "ok", 3),
        Provider("硅基流动", 620, "98.1%", "ok", 2),
        Provider("阿里云百炼", 880, "97.6%", "ok", 1),
        Provider("OpenRouter", 1400, "81.2%", "bad", 2)
    )

    val RED_LINES = listOf(
        "人机验证（滑块 / 点选）——前提是「有人在」",
        "人脸或实名认证",
        "单笔超过 ¥500 的付款",
        "修改系统安全设置（无障碍、无线调试、锁屏）"
    )

    val SELF_CHECK = listOf(
        "本地存储可用" to false,
        "任务状态持久化" to false,
        "通知权限" to false,
        "网络连通" to false,
        "无障碍服务已开启" to true,
        "内置助手已配对" to true,
        "虚拟屏可创建" to true,
        "读屏 / 点按打字" to true,
        "后台持续运行" to true
    )

    /** 流程步骤：[标题, 类别, 耗时ms, 闸门类型] */
    data class FlowStep(val title: String, val kind: String, val ms: Int, val gate: String?)

    val FLOW: Map<String, List<FlowStep>> = mapOf(
        "default" to listOf(
            FlowStep("意图门控 · 拆出 1 个目标", "route", 180, null),
            FlowStep("读能力自述 · 选中能力链路", "route", 240, null),
            FlowStep("申请虚拟屏 vd", "screen", 160, null),
            FlowStep("打开目标 App · 停在上一页", "screen", 420, null),
            FlowStep("读屏 · 提取 12 个可点控件", "screen", 380, null),
            FlowStep("点按 · 进入目标入口", "screen", 260, null),
            FlowStep("逐字输入 · EditText #1", "screen", 340, null),
            FlowStep("结果判定 · 核对页面回执", "route", 300, null),
            FlowStep("写回原会话 · 附来源", "route", 200, null)
        ),
        "pay" to listOf(
            FlowStep("意图门控 · 识别到花钱操作", "route", 180, null),
            FlowStep("读能力自述 · 选中能力链路", "route", 220, null),
            FlowStep("申请虚拟屏 vd", "screen", 160, null),
            FlowStep("打开目标 App · 停在上一页", "screen", 400, null),
            FlowStep("读屏 · 定位结算按钮", "screen", 360, null),
            FlowStep("停在付款前 · 等你决定", "screen", 0, "payment"),
            FlowStep("提交订单 · 逐字输入", "screen", 420, null),
            FlowStep("结果判定 · 核对回执", "route", 300, null),
            FlowStep("写回原会话 · 附来源", "route", 200, null)
        ),
        "login" to listOf(
            FlowStep("意图门控 · 识别到需要登录态", "route", 180, null),
            FlowStep("查身份台账 · cookie 已过期", "route", 260, null),
            FlowStep("申请虚拟屏 vd", "screen", 160, null),
            FlowStep("打开目标 App · 登录页", "screen", 400, null),
            FlowStep("停在验证码前 · 请你亲手处理", "screen", 0, "captcha"),
            FlowStep("重新核实登录态", "screen", 340, null),
            FlowStep("结果判定 · 核对回执", "route", 300, null),
            FlowStep("写回原会话 · 附来源", "route", 200, null)
        )
    )

    // ── mibot 主干数据 ───────────────────────────────────────────────────
    data class NavTab(val id: String, val label: String)

    val NAV = listOf(
        NavTab("chat", "对话"),
        NavTab("home", "日程"),
        NavTab("feed", "动态"),
        NavTab("ideas", "想法"),
        NavTab("me", "我的")
    )

    data class CapItem(val n: String, val s: String, val icon: String, val tone: String, val to: String)

    val CAP_GRID = listOf(
        CapItem("手机操控", "虚拟屏 · 多开", "phone", "blue", "screens"),
        CapItem("微信", "扫码 · 任务", "chat", "green", "vault"),
        CapItem("App 访问", "按 App 放行", "shield", "purple", "appaccess"),
        CapItem("钱包", "付款 · 授权", "wallet", "amber", "gate"),
        CapItem("沙箱", "多发行版", "box", "teal", "sandbox"),
        CapItem("监控", "用量 · 运行", "chart", "slate", "usage"),
        CapItem("密钥库", "身份隔离", "key", "navy", "vault"),
        CapItem("设置", "全部模块", "settings", "brick", "me")
    )

    data class Module(
        val id: String,
        val n: String,
        val icon: String,
        val tone: String,
        val to: String,
        val src: String,
        val impl: String,
        val note: String
    )

    val MIBOT_MODULES = listOf(
        Module("chat", "会话", "chat", "blue", "chat", "mibot", "live", "一句话派活、任务时间线、授权闸门都在这里发生"),
        Module("feed", "动态", "compass", "green", "feed", "mibot", "live", "在看 / 想到的 / 能领的，三态信息流"),
        Module("ideas", "想法", "bulb", "purple", "ideas", "mibot", "live", "随手记，自动并进动态做第二态"),
        Module("mail", "AI 邮箱", "mail", "slate", "mail", "mibot", "spec", "专属别名地址，验证码自动填；收不到就直说"),
        Module("groups", "Bots 群聊", "users", "rose", "groups", "mibot", "spec", "共享黑板只放一句话事实，过程进各自会话"),
        Module("bots", "机器人", "bot", "navy", "bots", "mibot", "spec", "自托管 Bots 的创建与编排"),
        Module("plugins", "DSH 插件市场", "layers", "brick", "plugins", "mibot", "spec", "下载→校验→依赖，三步全绿才算装上"),
        Module("sandbox", "沙箱 / 终端", "box", "teal", "sandbox", "mibot", "spec", "Debian/Ubuntu/Alpine/Arch，proot 跑"),
        Module("vault", "账号台账 / 密钥库", "key", "navy", "vault", "mibot", "spec", "身份→组→会话三层隔离；Key 不出门"),
        Module("providers", "服务商", "cloud", "sky", "providers", "mibot", "live", "模型供应商预设与自动切换"),
        Module("gateway", "AI 网关", "sync", "amber", "gateway", "mibot", "live", "127.0.0.1:4000 本地网关，分级熔断"),
        Module("screens", "后台操控 / 虚拟屏", "monitor", "blue", "screens", "mibot", "live", "4 槽位多开、App 锁、排队错峰"),
        Module("skills", "技能", "spark", "green", "ability", "mibot", "live", "文件系统 / 读屏 OCR / 搜索 / 操作路线"),
        Module("mcp", "MCP", "plug", "purple", "ability", "mibot", "live", "sqlite / github / filesystem / browser"),
        Module("memory", "记忆", "database", "rose", "mem", "mibot", "live", "关于你 / 偏好 / 做法三分法"),
        Module("plans", "定时任务", "clock", "slate", "plans", "mibot", "live", "cron + 条件触发，预览三次"),
        Module("diagnose", "诊断", "activity", "steel", "diag", "mibot", "live", "自检 9 项 + 诊断包导出"),
        Module("tailscale", "Tailscale 远程", "link", "taupe", "gateway", "mibot", "spec", "169.254 即未连，不是慢")
    )

    data class FeedItem(val kind: String, val t: String, val s: String, val time: String, val src: String)

    val FEED = listOf(
        FeedItem("在看", "aihot.news 今日 AI 早报已抓取", "12 条 · 3 条可挂进网关", "14分钟前", "mibot"),
        FeedItem("想到的", "把「飞书日报」沉淀成操作路线，月底自动跑", "想法 #21 · 已并流", "1小时前", "mibot"),
        FeedItem("能领的", "硅基流动新用户 2000 万 tokens 试用", "已挂网关 · 可直接用", "今天 09:12", "mibot"),
        FeedItem("在看", "小红书分类改版，发布前确认弹窗位置变了", "站点知识库已更新", "昨天", "mibot"),
        FeedItem("能领的", "OpenRouter 免费层又缩水，建议切回本地网关", "网关已标红", "昨天", "mibot")
    )

    data class Idea(val t: String, val tag: String, val pinned: Boolean, val time: String)

    val IDEAS = listOf(
        Idea("让 Caeser 胜率>60% 时自动更新实盘配置", "交易", true, "今天 10:30"),
        Idea("虚拟屏低配机降级成一次一个，别硬并行", "性能", false, "昨天 22:10"),
        Idea("账单类邮件自动归档到「钱包」会话", "邮件", false, "周一"),
        Idea("长按 App 图标直接派常用任务", "交互", false, "上周")
    )

    data class MailItem(val from: String, val sub: String, val code: String, val time: String, val got: Boolean)

    val MAIL = listOf(
        MailItem("service@github.com", "Your password reset code", "829 104", "刚刚", true),
        MailItem("no-reply@alipay.com", "付款验证码", "——", "10分钟前", false),
        MailItem("hermes@studio.local", "Caesar 每日进化报告", "", "今天 08:00", true)
    )

    data class Group(val n: String, val members: Int, val last: String, val time: String)

    val GROUPS = listOf(
        Group("工作室运营", 4, "Hermes：今日胜率 62.3%，已更新实盘", "8分钟前"),
        Group("Caesar 策略组", 2, "BTC 仅 51.5%，实盘降权", "1小时前"),
        Group("HongProxy 测试", 3, "APK 已装进真机", "昨天")
    )

    data class Bot(val n: String, val role: String, val on: Boolean, val note: String)

    val BOTS = listOf(
        Bot("Hermes", "自动进化引擎", true, "每日训练 + 飞书日报"),
        Bot("Caesar", "量化交易策略", true, "StochRSI + 白名单 + 追踪止损"),
        Bot("Longcat 轮询", "API 中继", true, "CAP_Relay 驱动")
    )

    data class Plugin(val n: String, val cat: String, val ver: String, val step: Int, val state: String, val on: Boolean)

    val PLUGINS = listOf(
        Plugin("网页摘要", "能力", "1.4.2", 3, "已装", true),
        Plugin("飞书推送", "通知", "2.0.1", 3, "已装", true),
        Plugin("图像 OCR", "能力", "0.9.3", 2, "缺运行时", false),
        Plugin("小红书发布", "App 桥", "1.1.0", 1, "待校验", false)
    )

    data class Sandbox(val distro: String, val ram: String, val cpu: String, val net: String, val rootfs: String, val note: String)

    val SANDBOX = Sandbox("Ubuntu 22.04", "2 GB", "2 核", "受限", "8.4 GB", "手机无 Docker，proot 容器")

    data class Vault(val idt: String, val grp: String, val key: String, val risk: String, val cool: String)

    val VAULT = listOf(
        Vault("工作室主号", "studio", "硅基流动 sk-…3f2a", "正常", "—"),
        Vault("支付宝小号", "finance", "—— 金融隔离", "禁用", "24h"),
        Vault("GitHub", "dev", "ghp_…9c41", "正常", "—")
    )

    data class ProviderPreset(val n: String, val host: String, val kind: String, val on: Boolean)

    val PROVIDERS_PRESET = listOf(
        ProviderPreset("本地网关", "127.0.0.1:4000", "自托管", true),
        ProviderPreset("硅基流动", "api.siliconflow.cn", "云端", true),
        ProviderPreset("阿里云百炼", "dashscope.aliyun.com", "云端", false),
        ProviderPreset("OpenRouter", "openrouter.ai", "云端", false)
    )

    val APP_OPTIONS = listOf("微信", "小红书", "闲鱼", "抖音", "B站", "知乎", "飞书", "支付宝", "GitHub", "其他")

    val FINANCIAL = listOf("支付宝")
}
