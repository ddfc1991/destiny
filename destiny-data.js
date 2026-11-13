/* Destiny · 数据源（以 mibot 为主干）
 * 主干 = mibot 真机工程里的真实模块（HarkTokens / MibotUi / 首页能力组 / GlassNavigation）
 * 融合层 = Mika 挑走的 9 块 + 两边重叠后重新定义的能力，挂在对应 mibot 模块下
 * 本文件只放规格数据，不含界面逻辑。
 */
window.DESTINY_DATA = (function () {
  /* ---------- 融合图谱（保留：能力路由/融合页用） ---------- */
  const MIKA_GROUPS = [
    { id: 'ops', name: '屏幕与操控', note: '决定它能不能真干活' },
    { id: 'mem', name: '记忆与学习', note: '越用越懂你' },
    { id: 'time', name: '定时与条件', note: '不用你催' },
    { id: 'talk', name: '沟通与人', note: 'mibot 完全没有的一整块' },
    { id: 'web', name: '浏览与文件', note: '网页是一等公民' },
    { id: 'cost', name: '成本与模型', note: '自托管最该有' }
  ];
  const MIKA = [
    { n: 1, g: 'ops', t: '后台屏 · 多任务并行', picked: true },
    { n: 2, g: 'ops', t: '应用访问 · 按 App 放行', picked: true },
    { n: 3, g: 'ops', t: '恢复与修复 · 掉线自愈', picked: true },
    { n: 4, g: 'ops', t: '兼容性自检 · 9 项体检', picked: false },
    { n: 5, g: 'mem', t: '记忆三分法 + 待批准', picked: true },
    { n: 6, g: 'mem', t: '记住的操作路线', picked: true },
    { n: 7, g: 'mem', t: 'App / 站点知识库', picked: false },
    { n: 8, g: 'time', t: '条件触发 · 跟上一次比', picked: false },
    { n: 9, g: 'time', t: '保存前预览三次执行', picked: false },
    { n: 10, g: 'time', t: '结果回到原对话', picked: false },
    { n: 11, g: 'talk', t: 'AI 接打电话', picked: true },
    { n: 12, g: 'talk', t: '微信派单', picked: true },
    { n: 13, g: 'talk', t: '远程网关配对', picked: true },
    { n: 14, g: 'web', t: '内置浏览器', picked: true },
    { n: 15, g: 'web', t: '保留网页 · keep open', picked: false },
    { n: 16, g: 'web', t: '划词追问', picked: false },
    { n: 17, g: 'web', t: '文件库 · 跨会话找东西', picked: false },
    { n: 18, g: 'cost', t: 'Token 用量页', picked: false },
    { n: 19, g: 'cost', t: '单任务 Token 预算', picked: false },
    { n: 20, g: 'cost', t: '运行统计 · 拆开看', picked: false },
    { n: 21, g: 'cost', t: '按住说话', picked: false }
  ];
  const MIBOT = [
    '会话', '动态', '点子', 'Bots 群聊', 'AI 邮箱', '工作台', '账号台账', '服务商',
    'AI 网关', '模型组', '沙箱 / 终端', '文件', '技能', '记忆', 'MCP',
    '定时任务', '人格', '后台操控 / 虚拟屏', 'DSH 插件市场', '诊断', 'Tailscale 远程接入'
  ];
  const FUSION = [
    { id: 'chat', name: '一句话派活', from: 'C', to: 'chat', impl: 'live',
      d: '你只说一句目的，Destiny 拆成任务；不用先选 App、不用先配工具。' },
    { id: 'router', name: '能力路由 · 技能 / MCP / APP', from: 'mibot', to: 'ability', impl: 'live',
      d: '三类东西都是「能力」。挑之前先读每条自述，含「什么时候不该用我」，所以问今天几号不会开浏览器。' },
    { id: 'vdisplay', name: '虚拟屏多开与错峰', from: 'C', to: 'screens', impl: 'live',
      d: 'mibot 的后台操控 + Mika 的独立后台屏合并：4 个槽位、按 App 加锁、排队时明说在等谁。看起来同时，实际错开。' },
    { id: 'timeline', name: '运行过程 · 逐步时间线', from: 'Mika', to: 'run', impl: 'live',
      d: '每步按时间往下长，写清点了什么、输入方式、目标控件、耗时；任务跑着也能看。' },
    { id: 'gate', name: '授权闸门 · 按风险分层', from: 'C', to: 'gate', impl: 'live',
      d: 'mibot 的三档审批 + Mika 的「只有花钱才问」合并：信任按风险分层。花钱先问，发消息直接做，登录验证码两种模式都问。' },
    { id: 'interrupt', name: '随时插话 / 随时停', from: 'Mika', to: 'run', impl: 'live',
      d: '任务进行中补充或更正，下一步之前读到；按停止会补几行交代，下次「继续」知道从哪接。' },
    { id: 'resume', name: '断点恢复 / 失败归因', from: 'mibot', to: 'run', impl: 'live',
      d: '接上之前先核三件事；失败固定三行：哪一步、为什么、能做什么。' },
    { id: 'supervise', name: '督查与检阅', from: 'mibot', to: 'run', impl: 'live',
      d: '它先自己核再交给你；待检阅、打回、一次过率都记下来。' },
    { id: 'memory', name: '记忆三分法 · 做法待批准', from: 'Mika', to: 'mem', impl: 'live',
      d: '关于你 / 偏好 / 做法三类。做法要你点头才生效；密码、验证码、卡号不记。' },
    { id: 'route', name: '操作路线 · 越用越快', from: 'Mika', to: 'ability', impl: 'live',
      d: '同一件事办成功后沉淀成路线，第二次显示「按以往做法快速执行」；失效会自己停用。' },
    { id: 'knowledge', name: 'App / 站点知识库', from: 'Mika', to: 'ability', impl: 'live',
      d: '24 个 App + 23 个网站的使用知识，纯文本可补充：入口在哪、常见做法、坑在哪。' },
    { id: 'plans', name: '定时与条件任务', from: 'C', to: 'plans', impl: 'live',
      d: 'mibot 的 cron + Mika 的条件触发合并：跟上一次比、一天最多一次、保存前预览接下来三次、结果回原会话。' },
    { id: 'gateway', name: '本地网关 · 自动切换', from: 'mibot', to: 'gateway', impl: 'live',
      d: '127.0.0.1:4000 跑在本机，Key 不出门。连错 3 次 / 延迟 >3s / 401-402 立刻切；切回要稳定 10 分钟。' },
    { id: 'sandbox', name: '沙箱 · 多发行版', from: 'mibot', to: 'sandbox', impl: 'spec',
      d: 'Debian / Ubuntu / Alpine / Arch 四选一，资源上限与联网三档。手机跑不了 Docker，用 proot。' },
    { id: 'accounts', name: '身份隔离 · 三层结构', from: 'mibot', to: 'vault', impl: 'spec',
      d: '身份 → 组 → 会话。台账管「登录态」，网关管「API Key」，两者分开；一个号被风控，另一个先停 24h。' },
    { id: 'mail', name: 'AI 邮箱 · 专属地址', from: 'mibot', to: 'mail', impl: 'spec',
      d: '服务↔别名邮箱绑定，取到的验证码自动填入；收不到就老实说收不到。' },
    { id: 'browser', name: '内置浏览器', from: 'Mika', to: 'screens', impl: 'spec',
      d: '后台开真网页、电脑版 UA、保留登录、翻页有节奏，认出风控页就交人工，不硬闯。' },
    { id: 'appaccess', name: '应用访问 · 金融类默认禁用', from: 'Mika', to: 'appaccess', impl: 'live',
      d: '逐 App 放行；金融交易类自动列入「推荐禁用」。Shell、沙箱、MCP 不受此限。' },
    { id: 'recover', name: '恢复与修复 · 掉线自愈', from: 'Mika', to: 'recover', impl: 'live',
      d: '无障碍 / 免省电 / 无线调试逐项自查，失败会降级并说明卡在哪，不静默死掉。' },
    { id: 'handover', name: '接管 · 该你亲手做的', from: 'Mika', to: 'gate', impl: 'live',
      d: '登录、验证码、付款停下，App 挪到主屏；你处理完交还，它重新核实再继续。' },
    { id: 'call', name: 'AI 接打电话', from: 'Mika', to: 'me', impl: 'spec',
      d: '代接快递外卖、代打预约，接听交代带有效期，人工随时接管。' },
    { id: 'wechat', name: '微信派单', from: 'Mika', to: 'vault', impl: 'spec',
      d: '扫码绑定后微信私聊直接派任务，双向会话，不是单向通知。' },
    { id: 'pair', name: '网关配对码', from: 'Mika', to: 'gateway', impl: 'spec',
      d: '一次性配对码替代裸 token；断线自动重连，失效明确说「请重新配对」。' },
    { id: 'feed', name: '动态 · 在看 / 想到的 / 能领的', from: 'mibot', to: 'feed', impl: 'live',
      d: '点子并进动态做第二态。能领的额度按「能不能挂进网关」排，不按折合人民币排。' },
    { id: 'groups', name: 'Bots 群聊 · 共享黑板', from: 'mibot', to: 'groups', impl: 'spec',
      d: '只共享「一句话的事实」，不共享过程；过程进各自会话，不上黑板。你亲手写的优先级最高。' },
    { id: 'voice', name: '按住说话', from: 'Mika', to: 'me', impl: 'spec',
      d: '松手默认填进输入框给改，不抢着发；任务在跑时发出去就是插话。' },
    { id: 'usage', name: '用量 / Token 预算 / 运行统计', from: 'C', to: 'usage', impl: 'live',
      d: 'mibot 的用量页 + Mika 的单任务预算与四段耗时合并。' },
    { id: 'redline', name: '红线 · 问也不许做', from: 'mibot', to: 'gate', impl: 'live',
      d: '人机验证、人脸实名、单笔超过 ¥500、改系统安全设置 —— 四条固定不可授权。' },
    { id: 'dsh', name: 'DSH 插件 · 三步才算装上', from: 'mibot', to: 'plugins', impl: 'spec',
      d: '下载 → 校验 → 依赖，三步全绿才叫装上；缺运行时直接给两条路。' },
    { id: 'tailscale', name: 'Tailscale 远程接入', from: 'mibot', to: 'gateway', impl: 'spec',
      d: '地址变成 169.254 就是没连上，不是网络慢；出口节点默认关。' },
    { id: 'diag', name: '诊断包 / 自检 9 项', from: 'C', to: 'diag', impl: 'live',
      d: '一键收机型 / 权限 / 结果，可复制可导出；自检如实标出哪些项浏览器里判定不了。' },
    { id: 'appearance', name: '外观 · 暖瓷 / 夜 · 字号', from: 'mibot', to: 'appearance', impl: 'live',
      d: '默认暖瓷浅色 + 夜双主题、字号档位、文字对比度保护；照片只在本机。' }
  ];

  /* ---------- 引擎所需数组（保留） ---------- */
  const SKILLS = [
    { id: 'sk-file', name: '文件系统', kind: '技能', when: '读、写、整理本地文件', not: '不碰沙箱外路径', on: true, authority: '只放行两个目录' },
    { id: 'sk-route', name: '操作路线', kind: '技能', when: '同一 App 里办过的事再办一次', not: '路线失效时不要硬跑', on: true, authority: '只读 + 点击' },
    { id: 'sk-ocr', name: '读屏 OCR', kind: '技能', when: '控件没有名字、界面读不出来', not: '有人脸或验证码时不要用', on: true, authority: '仅当前虚拟屏' },
    { id: 'sk-search', name: '网页搜索', kind: '技能', when: '需要外部最新信息', not: '搜索内容一律按不可信处理', on: true, authority: '免费源 100 次/天' }
  ];
  const MCP = [
    { id: 'mcp-sqlite', name: 'sqlite', type: 'stdio · 本地', tools: 6, on: true, perm: '算数一律走它，不让模型心算' },
    { id: 'mcp-github', name: 'github', type: 'http · 远程', tools: 11, on: false, perm: '只读；写操作每次批准' },
    { id: 'mcp-fs', name: 'filesystem', type: 'stdio · 本地', tools: 8, on: true, perm: '只碰 Downloads/Destiny 与工作区' },
    { id: 'mcp-browser', name: 'browser', type: 'stdio · 本地', tools: 3, on: false, perm: '缺 chromium，启动会失败' }
  ];
  const APP_KNOWLEDGE = [
    { n: '微信', k: 'App', hint: '联系人从搜索进；聊天记录要逐屏翻' },
    { n: '飞书', k: 'App', hint: '日报推送走群机器人 webhook' },
    { n: '小红书', k: 'App', hint: '分类要点两次；发布前会弹确认' },
    { n: '闲鱼', k: 'App', hint: '卖闲置按钮在右下角；多账号要隔离' },
    { n: '支付宝', k: 'App', hint: '金融类 · 推荐禁用' },
    { n: '抖音', k: 'App', hint: '长列表逐屏读，别一次滑到底' },
    { n: 'B 站', k: 'App', hint: '评论区折叠层级多' },
    { n: '知乎', k: 'App', hint: '回答页有折叠，先展开再读' },
    { n: 'GitHub', k: '站点', hint: '电脑版 UA；风控页交人工' },
    { n: 'aihot.news', k: '站点', hint: '免费额度按能不能挂进网关排序' }
  ];
  const PROVIDERS = [
    { n: '本地网关 · 127.0.0.1:4000', ms: 210, ok: '99.4%', state: 'ok', keys: 3 },
    { n: '硅基流动', ms: 620, ok: '98.1%', state: 'ok', keys: 2 },
    { n: '阿里云百炼', ms: 880, ok: '97.6%', state: 'ok', keys: 1 },
    { n: 'OpenRouter', ms: 1400, ok: '81.2%', state: 'bad', keys: 2 }
  ];
  const RED_LINES = [
    '人机验证（滑块 / 点选）——前提是「有人在」',
    '人脸或实名认证',
    '单笔超过 ¥500 的付款',
    '修改系统安全设置（无障碍、无线调试、锁屏）'
  ];
  const SELF_CHECK = [
    { n: '本地存储可用', device: false },
    { n: '任务状态持久化', device: false },
    { n: '通知权限', device: false },
    { n: '网络连通', device: false },
    { n: '无障碍服务已开启', device: true },
    { n: '内置助手已配对', device: true },
    { n: '虚拟屏可创建', device: true },
    { n: '读屏 / 点按打字', device: true },
    { n: '后台持续运行', device: true }
  ];
  const FLOW = {
    default: [
      ['意图门控 · 拆出 1 个目标', 'route', 180, ''],
      ['读能力自述 · 选中能力链路', 'route', 240, ''],
      ['申请虚拟屏 vd', 'screen', 160, ''],
      ['打开目标 App · 停在上一页', 'screen', 420, ''],
      ['读屏 · 提取 12 个可点控件', 'screen', 380, ''],
      ['点按 · 进入目标入口', 'screen', 260, ''],
      ['逐字输入 · EditText #1', 'screen', 340, ''],
      ['结果判定 · 核对页面回执', 'route', 300, ''],
      ['写回原会话 · 附来源', 'route', 200, '']
    ],
    pay: [
      ['意图门控 · 识别到花钱操作', 'route', 180, ''],
      ['读能力自述 · 选中能力链路', 'route', 220, ''],
      ['申请虚拟屏 vd', 'screen', 160, ''],
      ['打开目标 App · 停在上一页', 'screen', 400, ''],
      ['读屏 · 定位结算按钮', 'screen', 360, ''],
      ['停在付款前 · 等你决定', 'screen', 0, 'payment'],
      ['提交订单 · 逐字输入', 'screen', 420, ''],
      ['结果判定 · 核对回执', 'route', 300, ''],
      ['写回原会话 · 附来源', 'route', 200, '']
    ],
    login: [
      ['意图门控 · 识别到需要登录态', 'route', 180, ''],
      ['查身份台账 · cookie 已过期', 'route', 260, ''],
      ['申请虚拟屏 vd', 'screen', 160, ''],
      ['打开目标 App · 登录页', 'screen', 400, ''],
      ['停在验证码前 · 请你亲手处理', 'screen', 0, 'captcha'],
      ['重新核实登录态', 'screen', 340, ''],
      ['结果判定 · 核对回执', 'route', 300, ''],
      ['写回原会话 · 附来源', 'route', 200, '']
    ]
  };

  /* ============================================================
     mibot 主干数据（新增 · 还原自真实工程）
     ============================================================ */

  // 底部毛玻璃导航（Toki 式聊天优先：对话 / 日程 / 动态 / 想法 / 我的）
  const NAV = [
    { id: 'chat', label: '对话', glyph: '✉' },
    { id: 'home', label: '日程', glyph: '⌂' },
    { id: 'feed', label: '动态', glyph: '◉' },
    { id: 'ideas', label: '想法', glyph: '✦' },
    { id: 'me', label: '我的', glyph: '☻' }
  ];

  // 首页能力宫格（CapabilityGroup · 真机 DestinyHomeHeader）
  const CAP_GRID = [
    { n: '手机操控', s: '虚拟屏 · 多开', g: '◈', c: 'blue', to: 'screens' },
    { n: '微信', s: '扫码 · 任务', g: '▣', c: 'green', to: 'vault' },
    { n: 'App 访问', s: '按 App 放行', g: '⊞', c: 'purple', to: 'appaccess' },
    { n: '钱包', s: '付款 · 授权', g: '¤', c: 'amber', to: 'gate' },
    { n: '沙箱', s: '多发行版', g: '▦', c: 'teal', to: 'sandbox' },
    { n: '监控', s: '用量 · 运行', g: '◔', c: 'slate', to: 'usage' },
    { n: '密钥库', s: '身份隔离', g: '⚿', c: 'navy', to: 'vault' },
    { n: '设置', s: '全部模块', g: '⚙', c: 'brick', to: 'me' }
  ];

  // mibot 真实模块清单（模块中心 · 我的 → 模块）
  const MIBOT_MODULES = [
    { id: 'chat', n: '会话', g: '✉', c: 'blue', to: 'chat', src: 'mibot', impl: 'live', note: '一句话派活、任务时间线、授权闸门都在这里发生' },
    { id: 'feed', n: '动态', g: '◉', c: 'green', to: 'feed', src: 'mibot', impl: 'live', note: '在看 / 想到的 / 能领的，三态信息流' },
    { id: 'ideas', n: '想法', g: '✦', c: 'purple', to: 'ideas', src: 'mibot', impl: 'live', note: '随手记，自动并进动态做第二态' },
    { id: 'mail', n: 'AI 邮箱', g: '✉', c: 'slate', to: 'mail', src: 'mibot', impl: 'spec', note: '专属别名地址，验证码自动填；收不到就直说' },
    { id: 'groups', n: 'Bots 群聊', g: '⌘', c: 'rose', to: 'groups', src: 'mibot', impl: 'spec', note: '共享黑板只放一句话事实，过程进各自会话' },
    { id: 'bots', n: '机器人', g: '⚇', c: 'navy', to: 'bots', src: 'mibot', impl: 'spec', note: '自托管 Bots 的创建与编排' },
    { id: 'plugins', n: 'DSH 插件市场', g: '⧉', c: 'brick', to: 'plugins', src: 'mibot', impl: 'spec', note: '下载→校验→依赖，三步全绿才算装上' },
    { id: 'sandbox', n: '沙箱 / 终端', g: '▦', c: 'teal', to: 'sandbox', src: 'mibot', impl: 'spec', note: 'Debian/Ubuntu/Alpine/Arch，proot 跑' },
    { id: 'vault', n: '账号台账 / 密钥库', g: '⚿', c: 'navy', to: 'vault', src: 'mibot', impl: 'spec', note: '身份→组→会话三层隔离；Key 不出门' },
    { id: 'providers', n: '服务商', g: '◈', c: 'sky', to: 'providers', src: 'mibot', impl: 'live', note: '模型供应商预设与自动切换' },
    { id: 'gateway', n: 'AI 网关', g: '⌁', c: 'amber', to: 'gateway', src: 'mibot', impl: 'live', note: '127.0.0.1:4000 本地网关，分级熔断' },
    { id: 'screens', n: '后台操控 / 虚拟屏', g: '▣', c: 'blue', to: 'screens', src: 'mibot', impl: 'live', note: '4 槽位多开、App 锁、排队错峰' },
    { id: 'skills', n: '技能', g: '❖', c: 'green', to: 'ability', src: 'mibot', impl: 'live', note: '文件系统 / 读屏 OCR / 搜索 / 操作路线' },
    { id: 'mcp', n: 'MCP', g: '⧓', c: 'purple', to: 'ability', src: 'mibot', impl: 'live', note: 'sqlite / github / filesystem / browser' },
    { id: 'memory', n: '记忆', g: '✧', c: 'rose', to: 'mem', src: 'mibot', impl: 'live', note: '关于你 / 偏好 / 做法三分法' },
    { id: 'plans', n: '定时任务', g: '◷', c: 'slate', to: 'plans', src: 'mibot', impl: 'live', note: 'cron + 条件触发，预览三次' },
    { id: 'diagnose', n: '诊断', g: '✜', c: 'steel', to: 'diag', src: 'mibot', impl: 'live', note: '自检 9 项 + 诊断包导出' },
    { id: 'tailscale', n: 'Tailscale 远程', g: '⊜', c: 'taupe', to: 'gateway', src: 'mibot', impl: 'spec', note: '169.254 即未连，不是慢' }
  ];

  // 动态 Feed（三态：在看 / 想到的 / 能领的）
  const FEED = [
    { kind: '在看', t: 'aihot.news 今日 AI 早报已抓取', s: '12 条 · 3 条可挂进网关', time: '14分钟前', src: 'mibot' },
    { kind: '想到的', t: '把「飞书日报」沉淀成操作路线，月底自动跑', s: '想法 #21 · 已并流', time: '1小时前', src: 'mibot' },
    { kind: '能领的', t: '硅基流动新用户 2000 万 tokens 试用', s: '已挂网关 · 可直接用', time: '今天 09:12', src: 'mibot' },
    { kind: '在看', t: '小红书分类改版，发布前确认弹窗位置变了', s: '站点知识库已更新', time: '昨天', src: 'mibot' },
    { kind: '能领的', t: 'OpenRouter 免费层又缩水，建议切回本地网关', s: '网关已标红', time: '昨天', src: 'mibot' }
  ];

  // 想法 Ideas
  const IDEAS = [
    { t: '让 Caeser 胜率>60% 时自动更新实盘配置', tag: '交易', pinned: true, time: '今天 10:30' },
    { t: '虚拟屏低配机降级成一次一个，别硬并行', tag: '性能', time: '昨天 22:10' },
    { t: '账单类邮件自动归档到「钱包」会话', tag: '邮件', time: '周一' },
    { t: '长按 App 图标直接派常用任务', tag: '交互', time: '上周' }
  ];

  // AI 邮箱 Mail（spec：需原生桥接）
  const MAIL = [
    { from: 'service@github.com', sub: 'Your password reset code', code: '829 104', time: '刚刚', got: true },
    { from: 'no-reply@alipay.com', sub: '付款验证码', code: '——', time: '10分钟前', got: false },
    { from: 'hermes@studio.local', sub: 'Caesar 每日进化报告', code: '', time: '今天 08:00', got: true }
  ];

  // Bots 群聊 GroupChats（共享黑板）
  const GROUPS = [
    { n: '工作室运营', members: 4, last: 'Hermes：今日胜率 62.3%，已更新实盘', time: '8分钟前' },
    { n: 'Caesar 策略组', members: 2, last: 'BTC 仅 51.5%，实盘降权', time: '1小时前' },
    { n: 'HongProxy 测试', members: 3, last: 'APK 已装进真机', time: '昨天' }
  ];

  // 机器人 Bots
  const BOTS = [
    { n: 'Hermes', role: '自动进化引擎', on: true, note: '每日训练 + 飞书日报' },
    { n: 'Caesar', role: '量化交易策略', on: true, note: 'StochRSI + 白名单 + 追踪止损' },
    { n: 'Longcat 轮询', role: 'API 中继', on: true, note: 'CAP_Relay 驱动' }
  ];

  // DSH 插件 Plugins（三步装）
  const PLUGINS = [
    { n: '网页摘要', cat: '能力', ver: '1.4.2', step: 3, state: '已装', on: true },
    { n: '飞书推送', cat: '通知', ver: '2.0.1', step: 3, state: '已装', on: true },
    { n: '图像 OCR', cat: '能力', ver: '0.9.3', step: 2, state: '缺运行时', on: false },
    { n: '小红书发布', cat: 'App 桥', ver: '1.1.0', step: 1, state: '待校验', on: false }
  ];

  // 沙箱 Sandbox（spec）
  const SANDBOX = { distro: 'Ubuntu 22.04', ram: '2 GB', cpu: '2 核', net: '受限', rootfs: '8.4 GB', note: '手机无 Docker，proot 容器' };

  // 密钥库 Vault / 身份隔离（spec）
  const VAULT = [
    { idt: '工作室主号', grp: 'studio', key: '硅基流动 sk-…3f2a', risk: '正常', cool: '—' },
    { idt: '支付宝小号', grp: 'finance', key: '—— 金融隔离', risk: '禁用', cool: '24h' },
    { idt: 'GitHub', grp: 'dev', key: 'ghp_…9c41', risk: '正常', cool: '—' }
  ];

  // 供应商预设 Providers（live）
  const PROVIDERS_PRESET = [
    { n: '本地网关', host: '127.0.0.1:4000', kind: '自托管', on: true },
    { n: '硅基流动', host: 'api.siliconflow.cn', kind: '云端', on: true },
    { n: '阿里云百炼', host: 'dashscope.aliyun.com', kind: '云端', on: false },
    { n: 'OpenRouter', host: 'openrouter.ai', kind: '云端', on: false }
  ];

  // 派活可选 App（new-task sheet）
  const APP_OPTIONS = ['微信', '小红书', '闲鱼', '抖音', 'B站', '知乎', '飞书', '支付宝', 'GitHub', '其他'];

  return {
    MIKA_GROUPS, MIKA, MIBOT, FUSION, SKILLS, MCP, APP_KNOWLEDGE, PROVIDERS, RED_LINES, SELF_CHECK, FLOW,
    NAV, CAP_GRID, MIBOT_MODULES, FEED, IDEAS, MAIL, GROUPS, BOTS, PLUGINS, SANDBOX, VAULT, PROVIDERS_PRESET, APP_OPTIONS
  };
})();
