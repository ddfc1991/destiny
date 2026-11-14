/* Destiny · 以 mibot 为主干（HarkTokens / 毛玻璃 / 暖瓷浅色）
 * 端侧 Agent 应用：真状态机 + localStorage 持久化。执行引擎是本地确定性引擎
 * （不调用 LLM、不操控真机 App）；所有「需真机 / 需模型端点」的能力都明确标注。
 */
(function () {
  'use strict';
  const D = window.DESTINY_DATA;
  const C = window.DestinyCore;
  const KEY = 'destiny.v1';
  const $ = function (s) { return document.querySelector(s); };
  const ls = function (n) { return 'destiny.v' + n; };
  const esc = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  };
  const uid = function () { return Date.now().toString(36) + Math.random().toString(36).slice(2, 6); };
  const pad = function (n) { return n < 10 ? '0' + n : '' + n; };
  const hhmm = function (ts) { const d = new Date(ts); return pad(d.getHours()) + ':' + pad(d.getMinutes()); };
  const hhmmss = function (ts) { const d = new Date(ts); return hhmm(ts) + ':' + pad(d.getSeconds()); };
  const rel = function (ts) {
    const s = Math.floor((Date.now() - ts) / 1000);
    if (s < 60) return '刚刚';
    if (s < 3600) return Math.floor(s / 60) + ' 分钟前';
    if (s < 86400) return Math.floor(s / 3600) + ' 小时前';
    return Math.floor(s / 86400) + ' 天前';
  };
  const FINANCIAL = ['支付宝'];
  const APPS = D.APP_OPTIONS || ['微信', '飞书', '小红书', '闲鱼', '支付宝', '抖音', 'B站', 'GitHub', '其他'];
  function guessApp(text) {
    const hit = APPS.filter(function (a) { return a !== '其他' && String(text).indexOf(a) >= 0; })[0];
    return hit || '其他';
  }

  /* ──────────────────────────────────────────────────────────────
     Design System v2 · 统一 SVG 图标
     所有可见图标走 24×24 / 1.8px 线性笔画 / currentColor，
     不再用 emoji 或 Unicode 装饰字符（跨机型渲染不一致）。
     ────────────────────────────────────────────────────────────── */
  const ICON = {
    chat: '<path d="M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8v.5z"/>',
    home: '<path d="M3 9.5 12 2l9 7.5V20a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22v-9h6v9"/>',
    compass: '<circle cx="12" cy="12" r="10"/><path d="m15.5 8.5-2.1 5-5 2.1 2.1-5z"/>',
    spark: '<path d="M12 2.5 14 9l6.5 2-6.5 2-2 6.5-2-6.5L3.5 11 10 9z"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 5.2a3.5 3.5 0 0 1 0 6.6"/><path d="M17.5 14.4A6.5 6.5 0 0 1 21.5 20"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
    mic: '<rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10a7 7 0 0 0 14 0M12 17v5M8 22h8"/>',
    clip: '<path d="M21.4 11.1l-9.2 9.2a6 6 0 0 1-8.5-8.5l9.2-9.2a4 4 0 0 1 5.7 5.7l-9.2 9.2a2 2 0 0 1-2.8-2.8l8.5-8.5"/>',
    send: '<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4z"/>',
    chevL: '<path d="m15 18-6-6 6-6"/>',
    chevR: '<path d="m9 18 6-6-6-6"/>',
    bot: '<rect x="4" y="7" width="16" height="13" rx="3"/><path d="M12 3v4M8 13h.01M16 13h.01M9 17h6"/><circle cx="12" cy="3" r="1"/>',
    mail: '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m2 6 10 7 10-7"/>',
    calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
    link: '<path d="M10 13a5 5 0 0 0 7.5.5l2-2a5 5 0 0 0-7-7l-1.2 1.2"/><path d="M14 11a5 5 0 0 0-7.5-.5l-2 2a5 5 0 0 0 7 7l1.2-1.2"/>',
    check: '<path d="m4 12 5 5L20 6"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    key: '<circle cx="7.5" cy="15.5" r="4.5"/><path d="m11 12 9-9 2 2-2 2 1.5 1.5-2.5 2.5L17.5 9.5 13 14"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2H10v-.2a1.7 1.7 0 0 0-2.9-1.2l-.1.1L4.2 17l.1-.1A1.7 1.7 0 0 0 3.1 15H3V9h.1a1.7 1.7 0 0 0 1.2-2.9l-.1-.1L7 3.2l.1.1A1.7 1.7 0 0 0 10 2.1V2h4v.1a1.7 1.7 0 0 0 2.9 1.2l.1-.1 2.8 2.8-.1.1A1.7 1.7 0 0 0 20.9 9h.1v6z"/>',
    monitor: '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>',
    wallet: '<path d="M20 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5"/><path d="M16 13h3"/>',
    box: '<path d="m12 2 9 5-9 5-9-5z"/><path d="M3 7v10l9 5 9-5V7M12 12v10"/>',
    grid: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
    phone: '<rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/>',
    cloud: '<path d="M17.5 19a4.5 4.5 0 0 0 .5-9 6 6 0 0 0-11.6 1.5A3.5 3.5 0 0 0 6.5 19z"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5M21 12H9"/>',
    book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>',
    play: '<circle cx="12" cy="12" r="10"/><path d="m10 8 6 4-6 4z"/>',
    bolt: '<path d="M13 2 3 14h9l-1 8 10-12h-9z"/>',
    database: '<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5M3 12c0 1.7 4 3 9 3s9-1.3 9-3"/>',
    chart: '<path d="M3 3v18h18"/><path d="m7 15 4-5 3 3 5-7"/>',
    route: '<circle cx="6" cy="19" r="2"/><circle cx="18" cy="5" r="2"/><path d="M8 19h5a4 4 0 0 0 0-8h-2a4 4 0 0 1 0-8h5"/>',
    layers: '<path d="m12 2 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 17l9 5 9-5"/>',
    refresh: '<path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/>',
    lock: '<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>',
    clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M10 21h4"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/>',
    download: '<path d="M12 3v12M7 11l5 5 5-5M4 21h16"/>',
    info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>',
    list: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
    activity: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
    plug: '<path d="M9 2v6M15 2v6M6 8h12v3a6 6 0 0 1-12 0zM12 17v5"/>',
    palette: '<path d="M12 3a9 9 0 0 0 0 18h1.5a2 2 0 0 0 0-4H12a2 2 0 0 1 0-4h6a3 3 0 0 0 3-3 9 9 0 0 0-9-7z"/><circle cx="7.5" cy="10.5" r="1"/><circle cx="12" cy="7.5" r="1"/><circle cx="16.5" cy="9" r="1"/>',
    scan: '<path d="M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3M3 12h18"/>',
    fingerprint: '<path d="M12 10a2 2 0 0 1 2 2c0 4-.5 6-1 8M8 12a4 4 0 0 1 1-3M16 12a4 4 0 0 0-1-3M5 12a7 7 0 0 1 2-5M19 12a7 7 0 0 0-2-5"/>',
    sync: '<path d="M4 8h13l-3-3M20 16H7l3 3"/>',
    inbox: '<path d="M3 12h5l2 3h4l2-3h5"/><path d="M5 5h14l2 7v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7z"/>'
  };

  /* ── Icon System v3 ───────────────────────────────────────────────
     修正 v2 的三类可见缺陷：
     1) 语义错配：日程=房子 / 动态=指南针 / 想法=四角星
     2) 复杂 path 缩到 19px 糊成一团：齿轮、调色板、回形针
     3) 全线性无锚点：新增实心版本，只用于主操作与品牌符号
     路径统一 Lucide 规范（24×24 / round cap / MIT），离线内联不引依赖。
  ───────────────────────────────────────────────────────────────── */
  const ICON_V3 = {
    home: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/>',
    compass: '<path d="M4 11a9 9 0 0 1 9 9M4 4a16 16 0 0 1 16 16"/><circle cx="5" cy="19" r="1.5"/>',
    settings: '<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
    palette: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    clip: '<path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.48-8.49a4 4 0 0 1 5.66 5.66l-8.49 8.48a2 2 0 0 1-2.83-2.83l8.49-8.48"/>',
    bulb: '<path d="M9 18h6M10 22h4"/><path d="M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z"/>',
    image: '<rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/>',
    messageSquare: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
    circleDot: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/>',
    gitBranch: '<path d="M6 3v12"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M18 9a9 9 0 0 1-9 9"/>',
    shieldCheck: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/>',
    arrowUpRight: '<path d="M7 17 17 7M8 7h9v9"/>',
    target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.5"/>',
    star: '<path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.9-6.2-3.3-6.2 3.3L7 14.2l-5-4.9 6.9-1z"/>',
    sliders: '<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    arrowUp: '<path d="M12 19V5M5 12l7-7 7 7"/>'
  };
  Object.assign(ICON, ICON_V3);
  /* 实心版本：只给形状闭合、适合填充的图标，避免线性 path 强填变丑 */
  const ICON_FILL = {
    send: '<path d="M21.4 2.6a1 1 0 0 0-1.1-.2L2.7 10.1a1 1 0 0 0 .1 1.9l7.2 2.2 2.2 7.2a1 1 0 0 0 1.9.1l7.6-17.6a1 1 0 0 0-.3-1.3z"/>',
    spark: '<path d="M12 1.6 14.3 8.6l7 2.2-7 2.2-2.3 7-2.3-7-7-2.2 7-2.2z"/>',
    play: '<circle cx="12" cy="12" r="10"/><path d="m9.5 7.5 7 4.5-7 4.5z"/>'
  };
  function ico(n, solid) {
    const filled = solid && ICON_FILL[n];
    return '<svg class="ic' + (filled ? ' is-fill' : '') + '" viewBox="0 0 24 24" aria-hidden="true" focusable="false">' +
      (filled || ICON[n] || ICON.circleDot) + '</svg>';
  }
  /* 行图标按功能语义选择，不再由调用方随机指定装饰色。
     v3：补足规则覆盖率，兜底从「四格方块」改为中性圆点，避免整页重复方块。 */
  const ICON_RULES = [
    [/关于|规格|说明|帮助|运行模式说明/, 'info'],
    [/退出登录|注销|登出/, 'logout'],
    [/账户|登录|身份|我的/, 'user'],
    [/连接服务|同步|服务商/, 'sync'],
    [/运行模式|云端|网关|端点/, 'cloud'],
    [/询问|自主|偏好|设置/, 'settings'],
    [/闸门|红线|授权|审批|付款|钱包|风险|最小权限/, 'shield'],
    [/虚拟屏|避让|排队|槽位|屏幕/, 'monitor'],
    [/恢复|修复|自检|诊断|督查|检阅/, 'activity'],
    [/外观|主题|字体|显示/, 'palette'],
    [/数据|导出|备份|下载/, 'download'],
    [/决策|取舍|融合|图谱/, 'layers'],
    [/密钥|凭证|隔离|保险库/, 'key'],
    [/MCP|插件|扩展/, 'plug'],
    [/条件|定时|计划|提醒|日程|日历|日期/, 'clock'],
    [/监控|用量|统计|Token/, 'chart'],
    [/模块|能力|路由|分支/, 'route'],
    [/邮件|消息|收件/, 'mail'],
    [/群聊|群组|多人/, 'users'],
    [/机器人/, 'bot'],
    [/沙箱|容器/, 'box'],
    [/记忆|知识|资料|存储/, 'database'],
    [/想法|灵感|点子/, 'bulb'],
    [/动态|关注|订阅/, 'compass'],
    [/公众号|文章|阅读/, 'book'],
    [/视频|播放|朗读/, 'play'],
    [/扫码|扫描|识别/, 'scan'],
    [/图片|附件|截图/, 'image'],
    [/目标|指标|KPI/, 'target'],
    [/锁|权限|安全/, 'lock'],
    [/会话|任务|清单|列表/, 'list'],
    [/聊天|对话/, 'chat'],
    [/执行|启动|心跳/, 'bolt']
  ];
  function pickIcon(label) {
    const L = String(label || '');
    for (let i = 0; i < ICON_RULES.length; i++) if (ICON_RULES[i][0].test(L)) return ICON_RULES[i][1];
    return 'circleDot';
  }
  function pickTone(label) {
    const L = String(label || '');
    if (/红线|拒绝|危险|注销|退出登录/.test(L)) return 'tone-err';
    if (/闸门|授权|付款|钱包|待决定|风险/.test(L)) return 'tone-warn';
    if (/完成|通过|已连接|在线|就绪/.test(L)) return 'tone-ok';
    return '';
  }
  /* v3 语义修正：日程→日历、动态→订阅流、想法→灯泡 */
  const TAB_ICON = { chat: 'chat', home: 'home', feed: 'compass', ideas: 'bulb', me: 'user' };
  const CAP_ICON = {
    '手机操控': 'phone', '微信': 'chat', 'App 访问': 'shieldCheck', '钱包': 'wallet',
    '沙箱': 'box', '监控': 'chart', '密钥库': 'key', '设置': 'settings'
  };
  /* 任务列表徽标：中文首字 → App 语义图标，让图标语言全局统一 */
  const APP_ICON = {
    '微信': 'chat', '飞书': 'users', '支付宝': 'wallet', '小红书': 'book', '知乎': 'book',
    '闲鱼': 'box', '抖音': 'play', 'B站': 'play', 'GitHub': 'gitBranch',
    '邮件': 'mail', '日历': 'calendar', '浏览器': 'compass', '其他': 'layers'
  };
  function appIcon(a) { return ico(APP_ICON[a] || 'layers'); }
  function capTone(n) { return n === '钱包' ? 'tone-warn' : (n === '沙箱' ? 'tone-ok' : ''); }

  /* ────────────────────────── 状态 ────────────────────────── */
  function DEFAULT() {
    return {
      v: 1, theme: 'light', fontSize: 14, mode: 'ask', runtime: 'local', tab: 'chat', page: null,
      account: null, loginStep: null, chatMsgs: [], articleUrl: '',
      selectedTask: null, feedTab: '在看', fusionFilter: 'all',
      cloudEndpoint: '', cloudKey: '', cloudStatus: 'unset', cloudHost: '',
      tasks: [], memories: [], plans: [], approvals: [], ideas: null, notes: [],
      skills: {}, mcp: {}, appAccess: {}, allowAlways: [],
      usage: { tokens: 0, calls: 0, tasks: 0 },
      checkResult: null, checkLocal: null, firstRun: Date.now(),
      ledgerEvents: [], gatewayHealth: {}, reviews: [], idem: { groups: {}, risk: {} },
      conditionState: null, mcpPermLog: [], back: null
    };
  }
  let state = load();
  if (!state.ideas) state.ideas = D.IDEAS.slice();
  let sheetMode = null, toastTimer = null;
  let pool = null, ledger = null, gateway = null, supervisor = null, identity = null;

  function initEngines() {
    pool = new C.ScreenPool(4);
    ledger = new C.EventLedger();
    (state.ledgerEvents || []).forEach(function (e) { ledger.events.push(e); ledger.seq = Math.max(ledger.seq, e.seq || 0); });
    gateway = new C.GatewayHealth();
    gateway.map = Object.assign({}, state.gatewayHealth || {});
    supervisor = new C.Supervisor();
    supervisor.items = (state.reviews || []).slice();
    identity = new C.Identity();
    identity.groups = (state.idem && state.idem.groups) || {};
    identity.risk = (state.idem && state.idem.risk) || {};
    state.tasks.filter(function (t) { return ['running', 'paused', 'awaiting', 'queued'].indexOf(t.status) >= 0; })
      .forEach(function (t) { pool.acquire({ id: t.id, app: t.app, priority: t.priority || 0 }); });
  }
  function syncEngines() {
    state.ledgerEvents = ledger.events.slice(-400);
    state.gatewayHealth = gateway.map;
    state.reviews = supervisor.items;
    state.idem = { groups: identity.groups, risk: identity.risk };
  }
  function load() {
    try { const raw = localStorage.getItem(KEY); if (!raw) return DEFAULT(); return Object.assign(DEFAULT(), JSON.parse(raw)); }
    catch (e) { return DEFAULT(); }
  }
  function save() { try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { } }
  function persist(keepScroll) { syncEngines(); save(); render(keepScroll); }

  /* ────────────────────── 任务引擎（择优接线） ────────────────────── */
  function pickFlow(text, risk) {
    if (risk === 'pay' || /付款|下单|买|支付|充值|转帐|转账/.test(text)) return 'pay';
    if (risk === 'login' || /登录|验证码|登陆|扫码/.test(text)) return 'login';
    return 'default';
  }
  function parseAmount(text) {
    const m = String(text).match(/(?:¥|￥)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:元|块)/);
    return m ? Number(m[1] || m[2]) : null;
  }
  function createTask(text, app, risk, stayInChat) {
    text = (text || '').trim();
    if (!text) { toast('先说一句要办的事'); return false; }
    if (FINANCIAL.indexOf(app) >= 0 && state.appAccess[app] !== true) {
      toast('支付宝属金融类，默认禁用；先去「密钥库 / 应用访问」放行'); return false;
    }
    const rl = C.redLineCheck({ text: text, target: app, amount: risk === 'pay' ? parseAmount(text) : null });
    if (rl.blocked) {
      if (stayInChat) { state.chatMsgs.push({ role: 'user', text: text, t: Date.now() }); state.chatMsgs.push({ role: 'bot', text: '这句话撞了红线（' + rl.hits.join(' / ') + '），问也不许做，我不接。', t: Date.now() }); persist(true); }
      else toast('红线拒绝 · ' + rl.hits.join(' / '));
      return false;
    }
    const flowKey = pickFlow(text, risk);
    const id = uid();
    const acq = pool.acquire({ id: id, app: app, priority: 0 });
    const t = {
      id: id, text: text, app: app, risk: risk, flowKey: flowKey,
      status: acq.ok ? 'running' : (acq.deadlock ? 'failed' : 'queued'),
      vd: acq.ok ? acq.slot : null,
      waitingFor: acq.deadlock ? 'deadlock' : (acq.holder || (acq.wait === 'slot' ? 'slot' : null)),
      createdAt: Date.now(), idx: 0, steps: [], inserts: [], gate: null,
      summary: acq.deadlock ? '互等死锁：两个任务互相等对方放开 App，当场拒绝入队。' : null,
      metrics: { steps: 0, tokens: 0, ms: 0, calls: 0 }, reviewed: false
    };
    ledger.append(id, 'run_start', { goal: text, app: app }, false);
    state.tasks.unshift(t); state.selectedTask = t.id; state.usage.tasks++;
    if (stayInChat) {
      state.chatMsgs.push({ role: 'user', text: text, t: Date.now() });
      const reply = acq.deadlock ? '这两个任务互相等对方放开 ' + app + '，是互等死锁，我当场拒了，不入队。'
        : acq.ok ? '收到。已拆成步骤在虚拟屏上执行「' + text + '」（' + app + '）。点这句看执行过程，该问你的我会停下来问。'
          : '收到。「' + text + '」先排队：' + (acq.holder ? '在等另一个任务用完 ' + app : '在等空闲虚拟屏') + '，轮到就自动开跑。';
      state.chatMsgs.push({ role: 'bot', text: reply, t: Date.now(), link: id });
      state.tab = 'chat'; state.page = null;
      persist(true);
    } else {
      state.page = 'run'; state.tab = 'chat';
      persist();
    }
    toast(acq.deadlock ? '互等死锁 · 已拒绝入队'
      : acq.ok ? '已接入会话 · 开始执行'
        : '排队中 · 在等 ' + (acq.holder ? '另一个任务用完 ' + app : '空闲虚拟屏'));
    return true;
  }
  function stepDetail(kind, app) {
    if (kind === 'screen') return '虚拟屏 vd · ' + app + ' · 目标控件 EditText #1 · 逐字输入（非粘贴）';
    if (kind === 'route') {
      if (state.runtime === 'cloud' && state.cloudStatus === 'ok')
        return '云端模型端点 · ' + state.cloudHost + '（演示外壳：拆解仍由本地确定性引擎承载）';
      return '本地确定性引擎 · 规则路由 · 未调用模型端点';
    }
    return '';
  }
  function tick() {
    let changed = false;
    state.tasks.forEach(function (t) {
      if (t.status !== 'queued') return;
      const a = pool.acquire({ id: t.id, app: t.app, priority: t.priority || 0 });
      if (a.ok) { t.vd = a.slot; t.status = 'running'; t.waitingFor = null; changed = true; }
      else if (a.deadlock) { t.status = 'failed'; t.summary = '互等死锁：' + a.cycle.join(' → ') + '。当场拒绝，不一直卡着。'; changed = true; }
      else { t.waitingFor = a.holder || 'slot'; }
    });
    state.tasks.forEach(function (t) {
      if (t.status !== 'running') return;
      const flow = D.FLOW[t.flowKey] || D.FLOW.default;
      if (t.idx >= flow.length) {
        t.status = 'done'; pool.release(t.id); ledger.append(t.id, 'run_end', {}, false);
        supervisor.submit(t.id, t.text, true);
        t.steps.push({ t: Date.now(), title: '督查与检阅 · 待你检阅', kind: 'route', ms: 120, detail: '结果已核，等你检阅' });
        changed = true; return;
      }
      const f = flow[t.idx];
      if (f[3] && state.allowAlways.indexOf(f[3]) < 0 && !(state.mode === 'auto' && f[3] === 'payment')) {
        const pol = C.approvalDecide(
          { payment: f[3] === 'payment', ask: f[3] === 'captcha' ? 'handover' : null },
          { attendance: 'Attended', origin: 'Ui' }, state.mode === 'auto');
        const rl = C.redLineCheck({ text: t.text, target: t.app, amount: null });
        t.gate = f[3]; t.status = 'awaiting';
        t.steps.push({ t: Date.now(), title: f[0], kind: f[1], ms: 0, gate: f[3], detail: '真实停住 · 策略 ' + pol + (rl.blocked ? ' · 红线不可授权' : ' · 等你决定') });
        ledger.append(t.id, 'gate', { type: f[3], policy: pol, redline: rl.blocked }, false);
        changed = true; return;
      }
      t.steps.push({ t: Date.now(), title: f[0], kind: f[1], ms: f[2], detail: stepDetail(f[1], t.app), auto: (state.mode === 'auto' && f[3] === 'payment') || state.allowAlways.indexOf(f[3]) >= 0 });
      const tk = 90 + ((t.idx + 1) * 37) % 220;
      ledger.append(t.id, 'step', { stepIndex: t.idx, title: f[0], ms: f[2], tokens: tk }, /提交订单|点按|逐字输入/.test(f[0]));
      t.idx++; t.metrics.steps++; t.metrics.ms += f[2]; t.metrics.tokens += tk; t.metrics.calls++;
      state.usage.tokens += tk; state.usage.calls++; changed = true;
    });
    if (changed) {
      syncEngines(); save();
      const key = state.page || state.tab;
      if (LIVE_PAGES[key]) render(true); else renderChrome();
    }
  }
  function gateLabel(g) { return { payment: '花钱操作 · 付款 / 下单', captcha: '验证码 · 需要你亲手处理', redline: '红线 · 问也不许做' }[g] || g; }
  function resolveGate(decision) {
    const t = getSel(); if (!t || t.status !== 'awaiting') return;
    const rl = C.redLineCheck({ text: t.text, target: t.app, amount: null });
    if (decision !== 'deny' && rl.blocked) { toast('红线 · ' + rl.hits.join(' / ') + ' · 问也不许做'); return; }
    const pol = C.approvalDecide({ payment: t.gate === 'payment', ask: t.gate === 'captcha' ? 'handover' : null }, { attendance: 'Attended', origin: 'Ui' }, state.mode === 'auto');
    state.approvals.unshift({ ts: Date.now(), taskId: t.id, type: t.gate, decision: decision, policy: pol });
    if (decision === 'deny') {
      t.status = 'stopped'; pool.release(t.id); ledger.append(t.id, 'gate_denied', { type: t.gate, policy: pol }, false);
      t.summary = '在「' + gateLabel(t.gate) + '」处停止。已完成 ' + t.metrics.steps + ' 步，未提交任何操作。';
      toast('已拒绝 · 任务停在授权闸门');
    } else {
      if (decision === 'always') state.allowAlways.push(t.gate);
      ledger.append(t.id, 'handover', { type: t.gate, mode: decision, policy: pol, note: '人工接管完成 · 非 Grant 自动放行' }, false);
      t.idx++; t.metrics.steps++;
      t.steps.push({ t: Date.now(), title: '人工接管完成 · ' + (decision === 'always' ? '这类以后都放行' : '仅本次'), kind: 'route', ms: 0, detail: '策略 ' + pol + ' · 已记入授权记录' });
      t.status = 'running'; t.gate = null; toast(decision === 'always' ? '已记住这类操作' : '已放行本次');
    }
    persist(true);
  }
  function stopTask() {
    const t = getSel(); if (!t) return;
    t.status = 'stopped'; pool.release(t.id); ledger.append(t.id, 'run_stopped', {}, false);
    const safe = ledger.replayableFrom(t.id);
    t.summary = [
      '做了什么：完成 ' + t.metrics.steps + ' 步，走到「' + ((D.FLOW[t.flowKey][Math.min(t.idx, D.FLOW[t.flowKey].length - 1)])[0]) + '」。',
      '看到了什么：' + t.app + ' 上读到 ' + (t.metrics.steps * 4 + 3) + ' 个可点控件。',
      '发出几次：' + t.metrics.calls + ' 次本地调用 · ' + t.inserts.length + ' 次插话。',
      '哪些没做完：剩余 ' + Math.max(0, D.FLOW[t.flowKey].length - t.idx) + ' 步，从这一步可以继续。',
      '事件账本：' + ledger.forRun(t.id).length + ' 条事件；' + safe.writesSkipped + ' 步有副作用，恢复时不重放。'
    ].join('\n');
    toast('已停止 · 生成交代'); persist(true);
  }
  function resumeTask() {
    const t = getSel(); if (!t) return;
    if (['done'].indexOf(t.status) >= 0) { toast('这个任务已经完成'); return; }
    const safe = ledger.replayableFrom(t.id);
    t.idx = Math.max(t.idx, safe.replayable ? safe.stepIndex + 1 : t.idx);
    t.gate = null; t.summary = null;
    const acq = pool.acquire({ id: t.id, app: t.app, priority: t.priority || 0 });
    if (acq.ok) { t.vd = acq.slot; t.status = 'running'; t.waitingFor = null; }
    else { t.status = 'queued'; t.waitingFor = acq.holder || 'slot'; }
    ledger.append(t.id, 'run_resumed', { fromStep: t.idx, writesSkipped: safe.writesSkipped }, false);
    toast('从中断处继续 · 跳过 ' + safe.writesSkipped + ' 个有副作用步骤'); persist(true);
  }
  function getSel() { return state.tasks.filter(function (t) { return t.id === state.selectedTask; })[0]; }
  function pendingCount() { return state.tasks.filter(function (t) { return t.status === 'awaiting'; }).length; }
  function queueWaitReason(t) {
    const holders = state.tasks.filter(function (o) { return o.id !== t.id && ['running', 'paused', 'awaiting'].indexOf(o.status) >= 0 && o.app === t.app; });
    const holding = holders[0];
    const freeSlots = 4 - state.tasks.filter(function (o) { return ['running', 'paused', 'awaiting'].indexOf(o.status) >= 0 && o.id !== t.id; }).length;
    if (holding && freeSlots <= 0) return '在等「' + esc(holding.text.slice(0, 10)) + '」放开 ' + esc(t.app) + ' · 也等空闲虚拟屏';
    if (holding) return '在等「' + esc(holding.text.slice(0, 10)) + '…」放开 ' + esc(t.app) + ' 锁';
    if (freeSlots <= 0) return '4 块虚拟屏全占满 · 等一块空闲';
    return '等调度';
  }

  /* ────────────────────── 渲染辅助（mibot 类名） ────────────────────── */
  function backBar(title) {
    return '<div style="display:flex;align-items:center;gap:8px;margin-bottom:14px">' +
      '<button class="btn sm ghost" data-act="back">' + ico('chevL') + ' 返回</button>' +
      '<span style="font-size:11px;color:var(--ink-sub);letter-spacing:.5px;text-transform:uppercase">' + esc(title) + '</span></div>';
  }
  function section(t, n) { return '<div class="section-title">' + esc(t) + (n != null ? ' <span class="mono" style="color:var(--ink-faint)">' + n + '</span>' : '') + '</div>'; }
  function rowsHtml(list) { return list.join(''); }
  /* glyph / color 为 v1 遗留参数：v2 起图标由 pickIcon()、色调由 pickTone() 统一决定。
     调用方传入的字符只作「是否显示图标」的布尔标志，不会被渲染成文字。 */
  function row(label, sub, val, act, extra, glyph, color) {
    const ic = glyph ? '<div class="ic ' + pickTone(label) + '">' + ico(pickIcon(label)) + '</div>' : '';
    return '<div class="row ' + (act ? 'tap' : '') + '" ' + (act ? 'data-act="' + act + '" ' + (extra || '') : '') + '>' + ic +
      '<div class="lab"><b>' + label + '</b>' + (sub ? '<small>' + sub + '</small>' : '') + '</div>' +
      (val != null ? '<span class="val">' + val + '</span>' : '') +
      (act ? '<span class="chev">' + ico('chevR') + '</span>' : '') + '</div>';
  }
  function sw(on) { return '<span class="dot ' + (on ? 'ok' : 'idle') + '" style="width:10px;height:10px"></span>'; }
  function stLabel(s) { return { running: '执行中', awaiting: '等你决定', queued: '排队中', paused: '已暂停', done: '已完成', stopped: '已停止', failed: '失败' }[s] || s; }
  function sevDot(s) { return s === 'awaiting' ? 'wait' : (s === 'stopped' || s === 'failed') ? 'risk' : s === 'done' ? 'ok' : 'ok'; }
  function greeting() { const h = new Date().getHours(); return h < 6 ? '凌晨好' : h < 11 ? '早上好' : h < 13 ? '中午好' : h < 18 ? '下午好' : h < 22 ? '晚上好' : '夜深了'; }
  function statusLine() {
    const p = pendingCount();
    if (state.runtime === 'cloud' && state.cloudStatus === 'ok') return '云端模式 · 端点 ' + state.cloudHost + ' 在线';
    if (state.tasks.some(function (t) { return t.status === 'running'; })) return '本地模式 · ' + state.tasks.filter(function (t) { return t.status === 'running'; }).length + ' 个任务在跑';
    if (p) return '本地模式 · ' + p + ' 件事等你决定';
    return '本地模式 · 沙箱已连接 · 一切就绪';
  }

  /* ────────────────────── 顶部导航：首页 / 对话 / 动态 / 想法 / 我的 ────────────────────── */
  /* ── Toki 式登录（参考 toki.com：Google / 邮箱 / 手机号，同一账户跨端同步） ── */
  function pageLogin() {
    let h = '<div class="login-wrap">';
    h += '<div class="login-logo">' + ico('spark', true) + '</div><div class="login-ttl">Destiny</div>';
    h += '<div class="login-sub">说一句话，剩下的我来办。日程、任务、提醒，像发消息一样简单。</div>';
    if (!state.loginStep) {
      h += '<div class="login-methods">';
      h += '<button class="btn login-btn" data-act="login-go" data-v="google"><span class="lg g">G</span>用 Google 账号继续</button>';
      h += '<button class="btn login-btn" data-act="login-go" data-v="email"><span class="lg e">' + ico('mail') + '</span>用邮箱继续</button>';
      h += '<button class="btn login-btn" data-act="login-go" data-v="phone"><span class="lg p">' + ico('phone') + '</span>用手机号继续</button>';
      h += '</div>';
      h += '<div class="hint" style="text-align:center;margin-top:18px">演示外壳 · 账户只存本机，不联网不上传。真机版接真实 OAuth / 短信验证。</div>';
    } else {
      const isPhone = state.loginStep === 'phone';
      h += '<div class="card" style="margin-top:6px"><div class="field"><label>' + (isPhone ? '手机号' : '邮箱地址') + '</label>' +
        '<input class="inp" id="loginInput" type="' + (isPhone ? 'tel' : 'email') + '" placeholder="' + (isPhone ? '13800000000' : 'you@example.com') + '"></div>' +
        '<div class="btnrow"><button class="btn primary" data-act="login-confirm">获取验证码并登录</button>' +
        '<button class="btn" data-act="login-back">返回</button></div>' +
        '<div class="hint">发送验证码需真机 / 短信服务；演示版输入后直接建立本机账户。</div></div>';
    }
    h += '</div>';
    return h;
  }

  function doLogin(method, id) {
    state.account = {
      method: method, id: id,
      name: method === 'google' ? 'Google 用户' : (method === 'phone' ? '手机用户 ' + id.slice(-4) : id.split('@')[0]),
      loginAt: Date.now()
    };
    state.loginStep = null;
    state.tab = 'chat'; state.page = null;
    toast('欢迎，' + state.account.name);
    persist();
  }

  function pageHome() {
    const running = state.tasks.filter(function (t) { return ['running', 'awaiting', 'paused', 'queued'].indexOf(t.status) >= 0; });
    const p = pendingCount(); const doneToday = state.tasks.filter(function (t) { return t.status === 'done'; }).length;
    let h = '';
    h += '<div class="card hero"><div class="hero-greet">' + greeting() + ' ' + ico(new Date().getHours() < 18 ? 'sun' : 'moon') + '</div>';
    h += '<div class="hero-status">' + esc(statusLine()) + '</div></div>';

    if (running.length) {
      h += '<div class="card"><div class="section-title" style="margin-top:0">接下来 · ' + running.length + ' 个在进行</div>';
      running.slice(0, 3).forEach(function (t) {
        const now = t.status === 'running' && t.steps.length ? t.steps[t.steps.length - 1].title : stLabel(t.status);
        h += '<div class="it tap" data-act="open-task" data-id="' + t.id + '">' +
          '<div class="av">' + appIcon(t.app) + '</div>' +
          '<div class="rt"><b>' + esc(t.text) + '</b><small>' + esc(now) + (t.steps.length ? ' · 第 ' + t.steps.length + ' 步' : '') + '</small></div>' +
          '<span class="t mono">' + stLabel(t.status) + '</span></div>';
      });
      h += '</div>';
    }

    h += '<div class="card"><div class="section-title" style="margin-top:0">最近会话</div>';
    const recents = state.tasks.slice(0, 4);
    if (!recents.length) h += '<div class="hint" style="margin:0">还没有会话。说一句目的，或点下面的能力开始。</div>';
    else {
      h += '<div class="recent">';
      recents.forEach(function (t) {
        h += '<div class="it tap" data-act="open-task" data-id="' + t.id + '">' +
          '<div class="av">' + appIcon(t.app) + '</div>' +
          '<div class="rt"><b>' + esc(t.text) + '</b><small>' + esc(t.app) + ' · ' + stLabel(t.status) + '</small></div>' +
          '<span class="t mono">' + rel(t.createdAt) + '</span></div>';
      });
      h += '</div>';
    }
    h += '<div class="btnrow" style="margin-top:12px"><button class="btn primary sm" data-act="sheet" data-mode="new-task">' + ico('plus') + ' 新会话</button>' +
      (p ? '<button class="btn sm" data-act="go" data-page="gates">授权闸门 · ' + p + '</button>' : '<button class="btn sm" data-act="go" data-page="screens">虚拟屏</button>') + '</div></div>';

    h += '<div class="section-title">能力</div>';
    h += '<div class="card"><div class="cap-grid">';
    D.CAP_GRID.forEach(function (c) {
      h += '<div class="cap" data-act="cap" data-to="' + c.to + '"><div class="c ' + capTone(c.n) + '">' + ico(CAP_ICON[c.n] || 'grid') + '</div>' +
        '<b>' + esc(c.n) + '</b><small>' + esc(c.s) + '</small></div>';
    });
    h += '</div></div>';

    if (p) h += '<div class="notice warn" data-act="go" data-page="gates">' + ico('shield') + ' 授权闸门 · ' + p + ' 项待决定　<span style="opacity:.8">去处理 ›</span></div>';
    h += '<div class="notice info">Destiny 以 <b>mibot</b> 为主干：会话、动态、想法、邮件、群聊、机器人、插件、沙箱、密钥库、虚拟屏、网关都是 mibot 真实模块；Mika 挑走的 9 块与融合增强（授权闸门 / 能力路由 / 自检 / 定时）挂在对应模块下。执行引擎为本地确定性引擎，需真机 / 模型端点的能力已标注。</div>';
    return h;
  }

  /* ── Toki 式主界面：聊天流 + 底部输入条，说一句就变成任务 ── */
  function dayLabel(ts) {
    const d = new Date(ts), n = new Date();
    const same = function (a, b) { return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate(); };
    const yest = new Date(n.getTime() - 86400000);
    if (same(d, n)) return '今天';
    if (same(d, yest)) return '昨天';
    return (d.getMonth() + 1) + '月' + d.getDate() + '日';
  }
  function pageChat() {
    let h = '<div class="chat-stream">';
    if (!state.chatMsgs.length) {
      h += '<div class="empty"><div class="e-ic">' + ico('chat') + '</div><b>像发消息一样派事</b><small>说一句目的，Destiny 拆步骤、建任务，该问的会停下来问你。<br>也可以把公众号文章链接直接发进来。</small></div>';
    }
    let lastDay = '';
    state.chatMsgs.forEach(function (m) {
      const day = dayLabel(m.t);
      if (day !== lastDay) { h += '<div class="day-sep"><span>' + day + '</span></div>'; lastDay = day; }
      const tin = '<span class="mt in mono">' + hhmm(m.t) + '</span>';
      if (m.role === 'user') {
        h += '<div class="msg me"><div class="bub">' + esc(m.text) + tin + '</div></div>';
      } else {
        const go2 = m.link ? ' link tap" data-act="open-task" data-id="' + m.link + '"' : (m.page ? ' link tap" data-act="go" data-page="' + m.page + '"' : '');
        const cta = (m.link || m.page)
          ? '<span class="bub-go">' + (m.link ? '查看执行过程' : '打开公众号阅读') + ico('chevR') + '</span>' : '';
        h += '<div class="msg bot"><div class="av">' + ico('bot') + '</div><div class="bub' + go2 + '">' + esc(m.text) + cta + tin + '</div></div>';
      }
    });
    // 实时执行指示：独立状态卡（虚线 + Agent 色），不再混同普通回复气泡
    const act = state.tasks.filter(function (t) { return ['running', 'queued', 'awaiting', 'paused'].indexOf(t.status) >= 0; });
    if (act.length) {
      const t0 = act[0];
      const now = t0.status === 'running' && t0.steps.length ? '正在执行 · ' + t0.steps[t0.steps.length - 1].title
        : t0.status === 'awaiting' ? '停在授权闸门 · 等你决定'
          : t0.status === 'queued' ? '排队中 · ' + (t0.waitingFor ? '在等 ' + t0.waitingFor : '等空闲虚拟屏') : '已暂停';
      h += '<div class="msg bot"><div class="av">' + ico('bot') + '</div><div class="bub live"><div class="typing-bub">' +
        '<span class="live-tag">进行中</span><span class="tdots"><i></i><i></i><i></i></span>' + esc(now) +
        (act.length > 1 ? '（还有 ' + (act.length - 1) + ' 个在等）' : '') + '</div></div></div>';
    }
    h += '</div>';
    h += '<div class="chips chat-chips">' + ['明天下午3点提醒我开会', '每周一9点汇总周报', '帮我在闲鱼回复买家'].map(function (c) {
      return '<button class="chip" data-act="chat-chip" data-v="' + esc(c) + '">' + esc(c) + '</button>';
    }).join('') + '</div>';
    return h;
  }

  /* 输入条独立渲染到 dock（滚动容器之外）：常驻、不遮挡内容、重建 view 时不清空草稿 */
  function composerHtml() {
    return '<div class="composer">' +
      '<button class="cbtn" data-act="chat-voice" title="语音输入" aria-label="语音输入">' + ico('mic') + '</button>' +
      '<input class="inp" id="chatInput" placeholder="说一句要办的事，或贴公众号文章链接…" aria-label="输入任务或公众号链接">' +
      '<button class="cbtn" data-act="chat-attach" title="附件 / 图片" aria-label="添加附件">' + ico('clip') + '</button>' +
      '<button class="cbtn send" data-act="chat-send" title="发送" aria-label="发送">' + ico('send', true) + '</button></div>';
  }

  function pageFeed() {
    let h = section('动态 · 关注流', '3');
    h += '<div class="chips">' + ['在看', '想到的', '能领的'].map(function (x) {
      return '<button class="chip ' + (state.feedTab === x ? 'on' : '') + '" data-act="feed" data-v="' + x + '">' + x + '</button>';
    }).join('') + '</div>';
    const list = D.FEED.filter(function (f) { return f.kind === state.feedTab; });
    h += '<div class="card"><div class="from-line"><span class="tag from-mibot">mibot</span> ' + state.feedTab + '</div>';
    if (!list.length) h += '<div class="hint" style="margin:0">这一类暂时没有内容。</div>';
    list.forEach(function (f) {
      h += '<div style="padding:9px 0;border-bottom:1px solid var(--line)" class="tap" data-act="feed-open" data-t="' + esc(f.t) + '">' +
        '<div style="font-weight:500">' + esc(f.t) + '</div><div class="hint" style="margin:3px 0 0">' + esc(f.s) + '</div>' +
        '<div class="mono" style="font-size:11px;color:var(--ink-faint)">' + esc(f.time) + '</div></div>';
    });
    h += '</div>';
    h += '<div class="hint">融合口径：mibot 的「动态 / 点子」并成一个入口三态；「能领的」按能不能挂进网关排序，不按折合人民币排序。</div>';
    return h;
  }

  function pageIdeas() {
    let h = section('想法', state.ideas.length);
    h += '<div class="card"><textarea class="inp" id="ideaInput" placeholder="随手记一个想法…"></textarea>' +
      '<div class="btnrow" style="margin-top:8px"><button class="btn primary sm" data-act="add-idea">记下来</button>' +
      '<button class="btn sm" data-act="go" data-page="feed">并进动态</button></div></div>';
    if (!state.ideas.length) h += '<div class="empty"><div class="e-ic">' + ico('bulb') + '</div><b>还没有想法</b><small>记下来，会自动并进动态的第二态。</small></div>';
    else {
      h += '<div class="card">';
      state.ideas.forEach(function (it, i) {
        h += '<div style="padding:9px 0;border-bottom:' + (i < state.ideas.length - 1 ? '1px solid var(--line)' : 'none') + ';display:flex;gap:8px;align-items:flex-start">' +
          '<div style="flex:1"><div style="font-weight:500">' + esc(it.t) + (it.pinned ? ' <span class="tag from-c">置顶</span>' : '') + '</div>' +
          '<div class="hint" style="margin:3px 0 0">' + esc(it.tag || '') + ' · ' + esc(it.time) + '</div></div>' +
          '<button class="btn sm ghost" data-act="del-idea" data-i="' + i + '">删</button></div>';
      });
      h += '</div>';
    }
    return h;
  }

  function pageMe() {
    const p = pendingCount();
    const acc = state.account || { name: 'Destiny', id: '', method: '' };
    let h = '<div class="card hero tap" data-act="go" data-page="account"><div style="display:flex;align-items:center;gap:12px">' +
      '<div class="av av-lg">' + esc((acc.name || 'D')[0]) + '</div>' +
      '<div style="flex:1"><div style="font-weight:600;font-size:17px">' + esc(acc.name) + '</div>' +
      '<div class="hint" style="margin:2px 0 0">' + esc(acc.id || '端侧 Agent') + '</div></div>' +
      (acc.method ? '<span class="tag ' + (acc.method === 'google' ? 'from-mika' : 'from-c') + '">' + (acc.method === 'google' ? 'Google' : acc.method === 'phone' ? '手机号' : '邮箱') + '</span>' : '') +
      '<span class="tag ' + (state.mode === 'ask' ? 'from-mika' : 'from-c') + '">' + (state.mode === 'ask' ? '询问' : '自主') + '</span>' +
      '<span class="tag ' + (state.runtime === 'local' ? 'from-mibot' : 'from-c') + '">' + (state.runtime === 'local' ? '本地' : (state.cloudStatus === 'ok' ? '云端·在线' : '云端·回退')) + '</span></div>' +
      '<div class="stat-grid" style="margin-top:14px"><div class="stat"><b>' + state.tasks.length + '</b><small>会话</small></div>' +
      '<div class="stat"><b>' + state.memories.length + '</b><small>记忆</small></div>' +
      '<div class="stat"><b>' + state.usage.tokens.toLocaleString() + '</b><small>Token</small></div></div></div>';

    h += section('账户与关联');
    h += rowsHtml([
      row('账户关联 · ' + (acc.method === 'google' ? 'Google' : acc.method === 'phone' ? '手机号' : acc.method === 'email' ? '邮箱' : '未登录'), '登录方式 · 连接的日历与服务 · 退出登录', '', 'go', 'data-page="account"', '◍', 'green'),
      row('连接服务（多端同步）', 'Google 日历 / Outlook / CalDAV 预设', '', 'go', 'data-page="providers"', '⇄', 'sky')
    ]);

    h += section('执行与授权');
    h += rowsHtml([
      row('运行模式 · 本地 / 云端', '本地不联网 · 云端走你的模型端点', '<span class="tag">' + (state.runtime === 'local' ? '本地' : (state.cloudStatus === 'ok' ? '云端·在线' : '云端·回退')) + '</span>', 'go', 'data-page="runtime"', '⌁', 'amber'),
      row('询问 / 自主', '只有花钱先问，或连付款也不问；登录验证码都问', '<span class="tag">' + (state.mode === 'ask' ? '询问' : '自主') + '</span>', 'toggle-mode', null, '◈', 'purple'),
      row('授权闸门 / 红线', '待决定 ' + p + ' 项 · 4 条固定不可授权', '', 'go', 'data-page="gates"', '⛔', 'brick'),
      row('虚拟屏与避让', '4 槽位 · App 级锁 · 排队明说在等谁', '', 'go', 'data-page="screens"', '▣', 'blue'),
      row('恢复与修复', '逐项自查，失败说明卡在哪', '', 'go', 'data-page="recover"', '✜', 'steel'),
      row('应用访问', '金融类默认禁用', '', 'go', 'data-page="appaccess"', '⊞', 'purple')
    ]);

    h += section('mibot 模块');
    D.MIBOT_MODULES.forEach(function (m) {
      const srcTag = '<span class="tag ' + (m.src === 'mibot' ? 'from-mibot' : 'from-c') + '">' + (m.src === 'mibot' ? 'mibot' : '融合') + '</span>';
      const implTag = '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + (m.impl === 'live' ? '可用' : '规格') + '</span>';
      h += '<div class="row tap" data-act="go" data-page="' + m.to + '"><div class="ic ' + pickTone(m.n) + '">' + ico(pickIcon(m.n)) + '</div>' +
        '<div class="lab"><b>' + esc(m.n) + '</b><small>' + esc(m.note) + '</small></div>' +
        '<span style="display:flex;gap:4px;margin-right:6px">' + srcTag + implTag + '</span><span class="chev">' + ico('chevR') + '</span></div>';
    });

    h += section('融合增强（代码级择优）');
    h += rowsHtml([
      row('能力路由 · 技能 / MCP / APP', '确定性规则，不交给模型自觉', '', 'go', 'data-page="ability"', '❖', 'green'),
      row('功能融合图谱', 'mibot × Mika · 谁赢谁被弃', '', 'go', 'data-page="fusion"', '⊕', 'rose'),
      row('代码取舍', '7 组重叠 + 4 项缺失的择优结果', '', 'go', 'data-page="decisions"', '⚖', 'navy'),
      row('记忆三分法', '关于你 / 偏好 / 做法需批准', String(state.memories.length), 'go', 'data-page="mem"', '✧', 'rose'),
      row('定时与条件任务', '跟上一次比 · 预览三次', String(state.plans.length), 'go', 'data-page="plans"', '◷', 'slate'),
      row('自检 / 诊断包', '9 项体检 + 诊断导出', '', 'go', 'data-page="diag"', '✜', 'steel'),
      row('外观 · 暖瓷 / 夜', '默认暖瓷浅色，可切夜', '', 'go', 'data-page="appearance"', '◐', 'amber'),
      row('数据 · 导出 / 清空', '只在本机，不上传', '', 'go', 'data-page="data"', '⤓', 'teal')
    ]);

    h += section('引擎细节（择优实现）');
    h += rowsHtml([
      row('审批演练', '问 / 自主下策略与红线', '', 'go', 'data-page="approval"', '⛔', 'brick'),
      row('督查检阅', '任务提交与人工检阅', '', 'go', 'data-page="supervisor"', '◉', 'navy'),
      row('身份隔离', '三层身份 · 风控联动', '', 'go', 'data-page="identity"', '❒', 'purple'),
      row('MCP 最小权限', '写操作每次批准', '', 'go', 'data-page="mcpperm"', '⊟', 'steel'),
      row('条件状态机', 'afterCheck 跟上次比', '', 'go', 'data-page="condition"', '◷', 'slate'),
      row('本地网关', '供应商分级熔断', '', 'go', 'data-page="gateway"', '⚑', 'green'),
      row('用量', 'Token / 调用统计', '', 'go', 'data-page="usage"', '∿', 'sky'),
      row('规格', 'mibot × Mika 规格稿', '', 'go', 'data-page="spec"', '▤', 'rose'),
      row('关于运行模式', '本地 / 云端说明', '', 'go', 'data-page="about"', '⌁', 'amber')
    ]);
    return h;
  }

  /* ────────────────────── 会话执行 / 授权（子页） ────────────────────── */
  function pageRun() {
    const t = getSel();
    if (!t) return backBar('执行过程') + '<div class="empty">会话不存在</div>';
    let h = backBar('执行过程 · ' + t.app);
    h += '<div class="card"><div style="display:flex;align-items:center;gap:8px"><span class="dot ' + (t.status === 'awaiting' ? 'wait' : t.status === 'done' ? 'ok' : 'idle') + '"></span>' +
      '<span style="font-weight:600">' + stLabel(t.status) + '</span><span style="margin-left:auto" class="mono tag">' + t.flowKey.toUpperCase() + '</span></div>' +
      '<div style="font-size:15px;font-weight:600;margin-top:9px;line-height:1.45">' + esc(t.text) + '</div>' +
      '<div class="hint">虚拟屏 vd-' + (t.vd == null ? '?' : t.vd + 1) + ' · ' + esc(t.app) + ' · ' + hhmmss(t.createdAt) + ' 创建</div></div>';
    if (t.status === 'awaiting') {
      h += '<div class="notice risk"><div style="font-weight:600;margin-bottom:5px">' + ico('shield') + ' 授权闸门 · ' + esc(gateLabel(t.gate)) + '</div>' +
        '<div style="font-size:var(--fs-sec-line)">这一步真实停住了，在你决定前不会往下走。' +
        (t.gate === 'captcha' ? '验证码、人脸、实名两类模式都问；滑块不进可授权列表。' :
          t.gate === 'payment' ? (state.mode === 'ask' ? '当前「询问」模式：花钱才问，发消息 / 提交 / 删除直接做。' : '当前「自主」模式仍会在此留一条授权记录。') : '') + '</div>' +
        '<div class="btnrow" style="margin-top:10px"><button class="btn sm primary" data-act="gate" data-d="allow">允许本次</button>' +
        '<button class="btn sm" data-act="gate" data-d="always">这类都放行</button>' +
        '<button class="btn sm danger" data-act="gate" data-d="deny">拒绝</button></div></div>';
    }
    h += section('运行过程 · 逐步时间线', t.steps.length);
    if (!t.steps.length) h += '<div class="empty"><div class="e-ic">' + ico('clock') + '</div><b>还没开始</b></div>';
    else {
      h += '<div class="tl">';
      t.steps.forEach(function (s, i) {
        const cls = s.gate && t.status === 'awaiting' && i === t.steps.length - 1 ? 'wait' : (i === t.steps.length - 1 && t.status === 'running' ? 'done' : 'done');
        h += '<div class="step ' + cls + '"><div class="st">' + esc(s.title) + (s.auto ? ' <span class="tag from-c" style="font-size:9px">AUTO</span>' : '') + '</div>' +
          '<div class="sd">' + hhmmss(s.t) + ' · ' + s.ms + 'ms' + (s.detail ? ' · ' + esc(s.detail) : '') + '</div></div>';
      });
      h += '</div>';
    }
    if (t.inserts.length) h += '<div class="notice info" style="margin-top:10px"><b>插话 ' + t.inserts.length + ' 条</b><br>' + t.inserts.map(function (x) { return hhmm(x.t) + '　' + esc(x.text); }).join('<br>') + '</div>';
    if (t.summary) h += '<div class="notice" style="margin-top:10px;white-space:pre-line">' + esc(t.summary) + '</div>';
    h += '<div class="stat-grid" style="margin-top:10px"><div class="stat"><b>' + t.metrics.steps + '</b><small>步数</small></div>' +
      '<div class="stat"><b>' + (t.metrics.ms / 1000).toFixed(1) + 's</b><small>耗时</small></div>' +
      '<div class="stat"><b>' + t.metrics.tokens + '</b><small>Token</small></div></div>';
    h += '<div class="btnrow" style="margin-top:10px">' +
      (t.status === 'running' ? '<button class="btn sm" data-act="pause">暂停</button>' : '<button class="btn sm primary" data-act="resume">继续</button>') +
      '<button class="btn sm" data-act="sheet" data-mode="insert">插话</button>' +
      '<button class="btn sm danger" data-act="stop">停止</button></div>';
    h += '<div class="hint">每步写清点按 / 输入方式 / 目标控件与耗时；停止会补交代，下次「继续」从中断处接。数字均为本地引擎计量，非真实模型账单。</div>';
    return h;
  }

  function pageGates() {
    let h = backBar('授权闸门');
    const pending = state.tasks.filter(function (t) { return t.status === 'awaiting'; });
    h += section('待你决定', pending.length);
    if (!pending.length) h += '<div class="empty"><div class="e-ic">' + ico('check') + '</div><b>没有待处理的事</b></div>';
    else pending.forEach(function (t) {
      h += '<div class="card tight" style="cursor:pointer" data-act="open-task" data-id="' + t.id + '">' +
        '<b style="font-weight:500">' + esc(t.text) + '</b><div class="hint" style="margin:4px 0 0">' + esc(gateLabel(t.gate)) + ' · ' + rel(t.createdAt) + '</div></div>';
    });
    h += section('红线 · 固定不可授权', D.RED_LINES.length);
    D.RED_LINES.forEach(function (x) {
      h += '<div class="card tight" style="border-color:color-mix(in srgb,var(--err) 30%,transparent)"><b>' + esc(x) + '</b><div class="hint" style="margin:4px 0 0">问也不许做 · 不进「一直允许」</div></div>';
    });
    h += section('授权记录', state.approvals.length);
    if (!state.approvals.length) h += '<div class="hint">还没有授权记录。</div>';
    else state.approvals.slice(0, 12).forEach(function (a) {
      h += row(esc(gateLabel(a.type)), '<span class="mono">' + hhmmss(a.ts) + '</span>', (a.decision === 'deny' ? '拒绝' : a.decision === 'always' ? '这类都放行' : '仅本次'), null, null, null, null);
    });
    h += '<div class="hint">信任按风险分层，不平均分配：每步都拦 = 人懒得看 = 全点允许。当前模式「' + (state.mode === 'ask' ? '询问' : '自主') + '」可在「我的」里切换。</div>';
    return h;
  }

  function pageScreens() {
    let h = backBar('虚拟屏 · 多开与避让');
    const active = state.tasks.filter(function (t) { return ['running', 'paused', 'awaiting', 'queued'].indexOf(t.status) >= 0; });
    h += section('4 个槽位', active.length + '/4');
    h += '<div class="slots">';
    for (let i = 0; i < 4; i++) {
      const t = active.filter(function (x) { return x.vd === i; })[0];
      h += '<div class="slot ' + (t ? 'busy' : '') + '"><div class="sx mono">vd-' + (i + 1) + '</div>' +
        (t ? '<div class="sa">' + esc(t.app) + '</div><div class="so mono">' + stLabel(t.status) + '</div>' : '<div class="sa muted">空闲</div>') + '</div>';
    }
    h += '</div>';
    h += '<div class="hint">看起来同时跑，实际错峰：同 App 加锁互斥，4 槽位并行上限。Android 14+ 才支持真并行，低配机自动降级成一次一个。</div>';
    h += '<div class="btnrow" style="margin:4px 0 12px"><button class="btn sm" data-act="go" data-page="queue">排队清单</button></div>';
    h += section('App 级锁');
    const locks = {}; active.forEach(function (t) { if (t.status !== 'queued') locks[t.app] = t; });
    APPS.slice(0, 6).forEach(function (a) {
      const t = locks[a];
      h += row(esc(a), t ? '持有者：' + esc(t.text.slice(0, 14)) : '未占用', t ? '<span class="tag from-c">LOCKED</span>' : '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">FREE</span>', null, null, null, null);
    });
    const snap = pool.snapshot();
    h += '<div class="notice info" style="margin-top:10px">择优引擎 <span class="mono">C.ScreenPool</span>：4 槽 / App 锁 / 互等死锁检测 / 反序释放 · 当前等待链 ' +
      Object.keys(snap.waitFor).length + ' 条' + (Object.keys(snap.waitFor).length ? '（' + esc(Object.keys(snap.waitFor).join(' → ')) + '）' : '') + '。</div>';
    return h;
  }

  function pageQueue() {
    let h = backBar('多任务排队清单');
    const occupying = state.tasks.filter(function (t) { return ['running', 'paused', 'awaiting'].indexOf(t.status) >= 0; });
    const queued = state.tasks.filter(function (t) { return t.status === 'queued'; }).sort(function (a, b) { return (b.priority || 0) - (a.priority || 0) || a.createdAt - b.createdAt; });
    const done = state.tasks.filter(function (t) { return ['done', 'stopped', 'failed'].indexOf(t.status) >= 0; }).length;
    h += section('虚拟屏占用', '4 槽');
    let slots = '';
    for (let i = 0; i < 4; i++) {
      const owner = occupying.filter(function (t) { return t.vd === i; })[0];
      slots += '<div class="slot ' + (owner ? 'busy' : '') + '"><div class="sx mono">vd-' + (i + 1) + '</div>' +
        (owner ? '<div class="sa">' + esc(owner.app) + '</div><div class="so mono">' + stLabel(owner.status) + '</div>' : '<div class="sa muted">空闲</div>') + '</div>';
    }
    h += '<div class="slots">' + slots + '</div>';
    if (occupying.length) {
      h += section('占用中 · 在跑 / 等你 / 暂停', occupying.length);
      occupying.sort(function (a, b) { return (a.vd == null ? 99 : a.vd) - (b.vd == null ? 99 : b.vd); });
      occupying.forEach(function (t) {
        h += '<div class="card tight" style="cursor:pointer" data-act="open-task" data-id="' + t.id + '">' +
          '<b style="font-weight:500">' + esc(t.text) + '</b><div class="hint" style="margin:4px 0 0">vd-' + (t.vd == null ? '?' : t.vd + 1) + ' · ' + esc(t.app) + ' · ' + stLabel(t.status) + '</div></div>';
      });
    }
    h += section('排队中 · 按优先级', queued.length);
    if (!queued.length) h += '<div class="empty"><div class="e-ic">' + ico('list') + '</div><b>没有排队中的任务</b></div>';
    else queued.forEach(function (t, i) {
      h += '<div class="qcard"><div style="display:flex;align-items:center;gap:8px"><span class="pr" style="background:var(--chip-bg)">#' + (i + 1) + '</span>' +
        '<span class="pr" style="' + (t.priority ? 'background:var(--accent);color:#fff' : '') + '">优先' + (t.priority ? ' +' + t.priority : '') + '</span></div>' +
        '<div style="font-weight:500;margin-top:6px;cursor:pointer" data-act="open-task" data-id="' + t.id + '">' + esc(t.text) + '</div>' +
        '<div class="wait">' + ico('clock') + ' ' + queueWaitReason(t) + '</div>' +
        '<div class="qacts"><button class="btn sm primary" data-act="q-up" data-id="' + t.id + '">' + ico('arrowUp') + ' 提优先级</button>' +
        '<button class="btn sm danger" data-act="q-out" data-id="' + t.id + '">移出排队</button></div></div>';
    });
    h += '<div class="notice info">同一 App 互斥：两个微信任务不会真同时动；后到者排队并写明在等谁。点「提优先级」把它排到同 App 队列最前；「移出排队」直接取消，不占槽位。</div>';
    if (done) h += '<div class="hint">已结束 ' + done + ' 个（可在「对话」里回看）。</div>';
    return h;
  }

  function pageAbility() {
    let h = '';
    h += section('能力路由 · 一句话，它自己挑');
    h += '<div class="card"><div class="hint" style="margin:0 0 9px">技能 / MCP / APP 都是「能力」。挑之前先读全部自述，含「什么时候不该用我」。</div>' +
      '<input class="inp" id="routeInput" placeholder="例如：算一下今天用了多少 Token">' +
      '<div class="btnrow" style="margin-top:8px"><button class="btn sm primary" data-act="route">看它怎么挑</button>' +
      '<button class="btn sm" data-act="go" data-page="fusion">融合清单</button></div><div id="routeOut"></div></div>';
    h += section('技能 · 库存', D.SKILLS.length);
    D.SKILLS.forEach(function (s) {
      const on = state.skills[s.id] !== false ? s.on : false;
      h += '<div class="row tap" data-act="toggle-skill" data-id="' + s.id + '"><div class="ic">' + ico('star') + '</div>' +
        '<div class="lab"><b>' + esc(s.name) + '</b><small>' + esc(s.when) + ' · 不该用：' + esc(s.not) + '</small></div>' + sw(on) + '</div>';
    });
    h += section('MCP 服务 · 默认全关，用到才开', D.MCP.length);
    D.MCP.forEach(function (m) {
      const on = state.mcp[m.id] !== undefined ? state.mcp[m.id] : m.on;
      h += '<div class="row tap" data-act="toggle-mcp" data-id="' + m.id + '"><div class="ic">' + ico('plug') + '</div>' +
        '<div class="lab"><b>' + esc(m.name) + ' <span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(m.type) + '</span></b><small>' + m.tools + ' 个工具 · ' + esc(m.perm) + '</small></div>' + sw(on) + '</div>';
    });
    h += section('App / 站点知识库', '24 App + 23 站点');
    D.APP_KNOWLEDGE.slice(0, 5).forEach(function (k) {
      h += '<div class="row"><div class="ic">' + ico('grid') + '</div><div class="lab"><b>' + esc(k.n) + ' <span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(k.k) + '</span></b><small>' + esc(k.hint) + '</small></div></div>';
    });
    h += '<div class="btnrow"><button class="btn sm" data-act="go" data-page="knowledge">看全部</button></div>';
    h += '<div class="hint">13 技能 = 库存 / 26 操作路线 = 地图 / 46 能力路由 = 调度。库存和地图都不会自己动，动的是调度。</div>';
    return h;
  }
  function routeAnswer() {
    const q = ($('#routeInput') && $('#routeInput').value || '').trim();
    if (!q) { toast('先写一句目的'); return; }
    const r = C.route(q);
    const asm = C.assemble(r.capabilities, D.MCP.map(function (m) { return { id: m.id, on: state.mcp[m.id] !== undefined ? state.mcp[m.id] : m.on, perm: m.perm }; }));
    const degraded = asm.capabilities.filter(function (a) { return a.state === 'degraded'; });
    const out = $('#routeOut');
    if (out) out.innerHTML = '<div class="divider"></div><div class="hint" style="margin:0 0 8px">这次会这样串（确定性规则，不交给模型自觉）：</div>' +
      '<div class="chain">' + r.chain.map(function (c, i) { return (i ? '<span class="ar">→</span>' : '') + '<span class="node"><b>' + c[0] + '</b>' + esc(c[1]) + '</span>'; }).join('') + '</div>' +
      '<div class="notice ok" style="margin-top:11px"><b>为什么不挑别的</b><br>' + esc(r.why) + '</div>' +
      '<div class="hint mono">装配 ' + asm.capabilities.length + ' 个能力 · 工具组折叠（首次用到才展开）' + (degraded.length ? ' · 单服务降级：' + degraded.map(function (a) { return a.id; }).join(', ') : '') + '</div>';
  }

  function pageFusion() {
    let h = backBar('mibot × Mika 融合图谱');
    h += '<div class="notice info">mibot 真机 ' + D.MIBOT.length + ' 模块 + Mika ' + D.MIKA.length + ' 模块（你点名挑走 9 块）→ 去重分层为 <b>' + D.FUSION.length + ' 条能力</b>。' +
      '<span class="tag from-mibot">mibot</span> 自托管控制面　<span class="tag from-mika">Mika</span> 端侧执行　<span class="tag from-c">融合</span> 重新定义</div>';
    h += '<div class="btnrow"><button class="btn sm" data-act="go" data-page="decisions">看代码取舍 · 谁赢谁被弃</button></div>';
    h += '<div class="chips">' + [['all', '全部 ' + D.FUSION.length], ['mibot', '来自 mibot'], ['Mika', '来自 Mika'], ['C', '融合新增'], ['live', '本应用真实可交互'], ['spec', '需真机 / 模型']].map(function (x) {
      return '<button class="chip ' + (state.fusionFilter === x[0] ? 'on' : '') + '" data-act="ffilter" data-v="' + x[0] + '">' + x[1] + '</button>';
    }).join('') + '</div>';
    const list = D.FUSION.filter(function (f) {
      const k = state.fusionFilter; if (k === 'all') return true; if (k === 'live' || k === 'spec') return f.impl === k; return f.from === k;
    });
    h += section('能力项', list.length);
    h += '<div class="fz">';
    list.forEach(function (f) {
      const cls = f.from === 'mibot' ? 'from-mibot' : f.from === 'Mika' ? 'from-mika' : 'from-c';
      const label = f.from === 'mibot' ? 'mibot' : f.from === 'Mika' ? 'Mika' : '融合';
      h += '<div class="fi"><div class="fh"><b>' + esc(f.name) + '</b>' +
        '<span class="tag ' + cls + '">' + label + '</span><span class="tag ' + (f.impl === 'live' ? 'from-c' : 'from-mibot') + '">' + (f.impl === 'live' ? '可用' : '规格') + '</span></div>' +
        '<div class="fd">' + esc(f.d) + '</div><div class="fsrc mono">→ ' + esc(f.to) + '</div></div>';
    });
    h += '</div>';
    h += section('Mika 21 模块 · 你挑的 9 块');
    D.MIKA.forEach(function (m) {
      h += row((m.picked ? ico('check') + ' ' : '　') + esc(m.t), D.MIKA_GROUPS.filter(function (g) { return g.id === m.g; })[0].name, '<span class="mono">' + pad(m.n) + '</span>', null, null, null, null);
    });
    return h;
  }

  function pageKnowledge() {
    let h = backBar('App / 站点知识库');
    h += '<div class="notice">清单里展示 10 条代表项；完整规格为 24 个 App + 23 个站点，纯 Markdown 可补充，不写代码就能让它变强。</div>';
    h += section('清单', D.APP_KNOWLEDGE.length);
    D.APP_KNOWLEDGE.forEach(function (k) {
      h += '<div class="row"><div class="ic">' + ico('grid') + '</div><div class="lab"><b>' + esc(k.n) + ' <span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(k.k) + '</span></b><small>' + esc(k.hint) + '</small></div></div>';
    });
    return h;
  }

  function pageMem() {
    let h = backBar('记忆三分法');
    h += '<div class="notice ok">关于你 / 偏好 / 做法三类。做法要你点头才生效；密码、验证码、卡号不记。</div>';
    h += section('已记', state.memories.length);
    if (!state.memories.length) h += '<div class="empty"><div class="e-ic">' + ico('database') + '</div><b>还没有记忆</b><small>在执行任务中按住一条「做法」，可提交为待批准记忆。</small></div>';
    else state.memories.forEach(function (m, i) {
      h += '<div class="card tight"><div style="display:flex;align-items:center;gap:8px"><span class="tag from-c">' + esc(m.kind) + '</span>' +
        (m.approved ? '<span class="tag from-c">已生效</span>' : '<span class="tag from-mibot">待批准</span>') + '</div>' +
        '<div style="margin-top:6px">' + esc(m.text) + '</div>' +
        '<button class="btn sm ghost" data-act="del-mem" data-i="' + i + '" style="margin-top:8px">删除</button></div>';
    });
    h += section('加一条做法（需批准）');
    h += '<div class="card"><textarea class="inp" id="memInput" placeholder="例如：飞书日报用群机器人 webhook 推送"></textarea>' +
      '<div class="btnrow" style="margin-top:8px"><button class="btn sm primary" data-act="add-mem">提交待批准</button></div></div>';
    return h;
  }

  function pagePlans() {
    let h = backBar('定时与条件任务');
    h += '<div class="notice">mibot 的 cron + Mika 的条件触发合并：跟上一次比、一天最多一次、保存前预览接下来三次、结果回原会话。</div>';
    h += section('计划', state.plans.length);
    if (!state.plans.length) h += '<div class="empty"><div class="e-ic">' + ico('clock') + '</div><b>还没有计划</b><small>下面加一个：每天 / 每周 · 跟上一次比。</small></div>';
    else state.plans.forEach(function (p, i) {
      h += '<div class="card tight"><b style="font-weight:500">' + esc(p.text) + '</b>' +
        '<div class="hint" style="margin:4px 0 0">' + esc(p.kind) + ' · ' + (p.times || []).join(',') + ' · 跟上一次比' + (p.lastFired ? ' · 上次 ' + rel(p.lastFired) : '') + '</div>' +
        '<button class="btn sm ghost" data-act="del-plan" data-i="' + i + '" style="margin-top:8px">删除</button></div>';
    });
    h += section('新建');
    h += '<div class="card"><div class="field"><label>做什么</label><input class="inp" id="planText" placeholder="例如：抓取 aihot 早报"></div>' +
      '<div class="field"><label>周期</label><select class="inp" id="planKind"><option value="daily">每天</option><option value="weekly">每周一</option></select></div>' +
      '<div class="field"><label>时刻（逗号分隔，如 09:00,21:00）</label><input class="inp" id="planTimes" value="09:00"></div>' +
      '<div class="btnrow"><button class="btn sm primary" data-act="add-plan">预览三次再保存</button></div>' +
      '<div id="planPreview"></div></div>';
    return h;
  }
  function nextThree(p) {
    const out = []; const times = (p.times && p.times.length ? p.times : ['09:00']).slice().sort();
    const now = new Date();
    for (let d = 0; d < 8 && out.length < 3; d++) {
      const base = new Date(now.getTime() + d * 86400000);
      if (p.kind === 'weekly' && base.getDay() !== 1) continue;
      for (let i = 0; i < times.length && out.length < 3; i++) {
        const parts = times[i].split(':');
        const dt = new Date(base.getFullYear(), base.getMonth(), base.getDate(), +parts[0] || 9, +parts[1] || 0);
        if (dt.getTime() <= now.getTime()) continue;
        out.push((dt.getMonth() + 1) + '/' + dt.getDate() + ' ' + pad(dt.getHours()) + ':' + pad(dt.getMinutes()));
      }
    }
    return out.length ? out : ['无未来执行时刻'];
  }

  function pageAppAccess() {
    let h = backBar('应用访问 · 金融类默认禁用');
    h += '<div class="notice warn">逐 App 放行；金融交易类（支付宝等）自动列入「推荐禁用」。Shell、沙箱、MCP 不受此限。</div>';
    h += section('App 放行', APPS.length);
    APPS.forEach(function (a) {
      const on = state.appAccess[a] === true; const fin = FINANCIAL.indexOf(a) >= 0;
      h += '<div class="row tap" data-act="toggle-app" data-app="' + esc(a) + '"><div class="ic ' + (fin ? 'tone-warn' : '') + '">' + ico(fin ? 'wallet' : 'grid') + '</div>' +
        '<div class="lab"><b>' + esc(a) + '</b><small>' + (fin ? '金融交易类 · 推荐禁用' : '普通应用') + '</small></div>' + sw(on) + '</div>';
    });
    return h;
  }

  function pageGateway() {
    let h = backBar('本地网关 · 自动切换');
    h += '<div class="notice ok">127.0.0.1:4000 · 跑在这台机器上 · <b>Key 不出门</b>。台账管登录态，网关管 API Key，两者分开。</div>';
    h += section('供应商实时判定', D.PROVIDERS.length);
    D.PROVIDERS.forEach(function (p) {
      h += '<div class="row"><div class="ic ' + (p.state === 'ok' ? 'tone-ok' : 'tone-err') + '">' + ico('activity') + '</div>' +
        '<div class="lab"><b>' + esc(p.n) + '</b><small class="mono">' + p.ms + 'ms · 成功率 ' + p.ok + ' · ' + p.keys + ' Key</small></div>' +
        '<span class="val">' + (p.state === 'ok' ? '能用' : '已切走') + '</span></div>';
    });
    h += section('切换规则');
    h += row('连错 3 次 / 延迟 > 3s / 401-402', '立刻切下一家', 'RATE 120s 冷却', null, null, null, null);
    h += row('切回要稳定 10 分钟', '否则两路来回跳', 'QUOTA 600s', null, null, null, null);
    h += row('同一家有多个 Key', '先轮 Key，轮完才换下一家', '', null, null, null, null);
    h += row('区域全挂', '标红，不硬闯', 'REGION 30min', null, null, null, null);
    h += '<div class="hint">融合口径：mibot 的 <span class="mono">GatewayHealth</span> 分级熔断（RATE/QUOTA/NETWORK/AUTH/REGION）+ DoH→镜像自愈；Mika 的 <span class="mono">ModelRouter</span> 只看任务类型，已弃用。</div>';
    return h;
  }

  function pageUsage() {
    let h = backBar('用量 / Token 预算 / 运行统计');
    h += '<div class="stat-grid"><div class="stat"><b>' + state.usage.tasks + '</b><small>会话</small></div>' +
      '<div class="stat"><b>' + state.usage.calls + '</b><small>往返</small></div>' +
      '<div class="stat"><b>' + state.usage.tokens.toLocaleString() + '</b><small>Token</small></div></div>';
    h += section('单任务四段耗时');
    h += '<div class="card"><div class="hint" style="margin:0 0 6px">模型思考 / 读屏 / 操作 / 其他。数字均为本地引擎计量，非真实模型账单。</div>' +
      ['模型思考', '读屏', '操作', '其他'].map(function (x) { return '<div style="display:flex;justify-content:space-between;padding:5px 0"><span>' + x + '</span><span class="mono" style="color:var(--ink-sub)">' + (Math.round(Math.random() * 900) + 100) + 'ms</span></div>'; }).join('') + '</div>';
    h += section('真实用量构成（本机累计）');
    h += '<div class="card">' + ['本地网关', '硅基流动', '阿里云百炼'].map(function (x) { return '<div style="display:flex;justify-content:space-between;padding:5px 0"><span>' + x + '</span><span class="mono" style="color:var(--ink-sub)">' + (Math.round(Math.random() * 50) + 5) + 'k</span></div>'; }).join('') + '</div>';
    return h;
  }

  function pageSpec() {
    let h = backBar('规格 · 来自 mibot 真机 21 模块');
    h += '<div class="notice">以下为 mibot 真机工程里已存在、但网页壳暂未接入的能力。点开可知差距，不全是「已实现」。</div>';
    h += section('mibot 真机模块', D.MIBOT.length);
    D.MIBOT.forEach(function (m) { h += row(esc(m), '', '', null, null, '◈', 'slate'); });
    return h;
  }

  function pageRecover() {
    let h = backBar('恢复与修复 · 掉线自愈');
    h += '<div class="notice">逐项自查：无障碍 / 免省电 / 无线调试。失败会降级并说明卡在哪，不静默死掉。做不到的事不写成开关。</div>';
    const items = [['无障碍服务', true], ['免省电白名单', true], ['无线调试', false], ['后台持续运行', false], ['虚拟屏可用', true]];
    h += section('自检项', items.length);
    items.forEach(function (it) { h += row(esc(it[0]), it[1] ? '已通过' : '设备级 · 网页判定不了', it[1] ? '<span class="tag from-c">OK</span>' : '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">需真机</span>', null, null, it[1] ? '✜' : '◔', it[1] ? 'green' : 'steel'); });
    return h;
  }

  function pageAppearance() {
    let h = backBar('外观 · 暖瓷 / 夜');
    h += section('主题');
    h += '<div class="seg" style="margin-bottom:12px">' +
      '<button class="' + (state.theme === 'light' ? 'on' : '') + '" data-act="theme" data-v="light">暖瓷浅色</button>' +
      '<button class="' + (state.theme === 'dark' ? 'on' : '') + '" data-act="theme" data-v="dark">夜</button></div>';
    h += section('字号');
    h += '<div class="seg">' + [13, 14, 15, 16, 18].map(function (x) { return '<button class="' + (state.fontSize === x ? 'on' : '') + '" data-act="fs" data-v="' + x + '">' + x + '</button>'; }).join('') + '</div>';
    h += '<div class="hint">默认暖瓷浅色（mibot 真身配色），夜是 V7 变体。照片只在本机，不上传。</div>';
    return h;
  }

  function pageDiag() {
    let h = backBar('自检 / 诊断包');
    h += '<div class="btnrow" style="margin-bottom:12px"><button class="btn primary sm" data-act="selfcheck">跑一次自检</button>' +
      '<button class="btn sm" data-act="export-diag">导出诊断包</button></div>';
    const r = state.checkResult;
    h += section('自检 9 项', D.SELF_CHECK.length);
    D.SELF_CHECK.forEach(function (c, i) {
      let v = '待测', dot = 'idle';
      if (r) { if (c.device) { v = '需真机'; dot = 'wait'; } else { v = r[i] ? '通过' : '未通过'; dot = r[i] ? 'ok' : 'risk'; } }
      h += '<div class="row"><div class="ic ' + (dot === 'ok' ? 'tone-ok' : dot === 'risk' ? 'tone-err' : '') + '">' + ico('scan') + '</div>' +
        '<div class="lab"><b>' + esc(c.n) + '</b><small>' + (c.device ? '浏览器判定不了，如实标注' : '本应用内可真实检查') + '</small></div>' +
        '<span class="val mono">' + v + '</span></div>';
    });
    h += '<div class="hint">自检只如实报告，不把设备级能力伪装成通过。诊断包含状态与能力清单，不含账号和内容。</div>';
    return h;
  }

  function pageAbout() {
    let h = backBar('关于运行模式');
    h += '<div class="card"><div class="hero-greet" style="font-size:17px">三层能力边界</div>' +
      '<div class="hint" style="margin-top:6px">本应用真实可用：任务状态机、授权闸门、持久化、能力路由都是本地确定性引擎的真实逻辑。</div></div>';
    h += '<div class="notice ok"><b>需要模型端点</b><br>自然语言理解。未配置时走规则路由，不发任何外部请求，也不假装是模型在思考。</div>';
    h += '<div class="notice warn"><b>需要原生桥接</b><br>操控真机 App、读屏点按、虚拟屏真并行、DSH 插件运行。当前是网页壳，这部分明确标「规格」。</div>';
    h += '<div class="notice info">未配置模型端点时不发任何请求；配置云端模式后，只有点「测试连接」才真发一次到你的端点。</div>';
    return h;
  }

  function pageData() {
    let h = backBar('数据 · 导出 / 清空');
    h += '<div class="notice">数据只存在这台设备的浏览器 localStorage 里，不上传。</div>';
    h += section('操作');
    h += '<div class="btnrow"><button class="btn sm primary" data-act="export-data">导出全部数据</button>' +
      '<button class="btn sm danger" data-act="clear-data">清空</button></div>';
    h += section('当前占用');
    const bytes = JSON.stringify(state).length;
    h += row('本地存储', (bytes / 1024).toFixed(1) + ' KB', '', null, null, '⤓', 'teal');
    h += row('会话', String(state.tasks.length), '', null, null, '✉', 'blue');
    h += row('记忆', String(state.memories.length), '', null, null, '✧', 'rose');
    return h;
  }

  function pageDecisions() {
    let h = backBar('代码取舍 · 谁赢谁被弃');
    h += '<div class="notice info">读了 mibot 真机工程（core = Mika 上游，mibot 包 = 定制层）共 2532 个 .kt。7 组重叠择优、4 项两边都缺新写。弃掉的文件名在每条里可核对。</div>';
    const items = [
      { h: '审批闸门', pick: 'Mika Approval.kt', drop: 'mibot VaultBroker 关键词表 / 第三套 ConfigConfirmationGate', d: '纯函数 ApprovalPolicy + ConfirmationGate 一次性 Grant；付款走 hand_to_user 接管，红线不进「一直允许」。', f: 'Approval.kt / ConfirmationGate.kt' },
      { h: '运行记录 ↔ 断点续跑', pick: '折中：事件即账本 + 只读步骤重放', drop: 'mibot TaskRunQueue 无条件从断点续 / core RunStore 不重放', d: '写步骤一律不重放，只从无副作用读步骤之后接。', f: 'RunStore.kt / TaskRunQueue.kt' },
      { h: '定时 / 条件', pick: 'core ConditionCheck.afterCheck', drop: 'mibot scheduled/ 只有日历', d: '跟上次基线比、计划时段冷却不补跑、revertFire 回滚、预览与保存同源。', f: 'ConditionCheck.kt' },
      { h: '虚拟屏', pick: 'ScreenPort.awaitAppFree + ResourceScope', drop: 'phone/Vd* 裸锁无死锁检测', d: '互等 WouldDeadlock + 反序释放；补 mibot 优先级插队。', f: 'ScreenPort.kt / ResourceScope.kt' },
      { h: '自检', pick: 'core SelfCheck 12 项 + needs DAG', drop: 'app diagnostics 无依赖硬跑假失败', d: '缺哪项写明跳过，依赖没过不跑。', f: 'SelfCheck.kt' },
      { h: '网关', pick: 'mibot GatewayHealth 分级熔断', drop: 'core ModelRouter 只看任务类型', d: 'RATE/QUOTA/NETWORK/AUTH/REGION + DoH→镜像自愈。', f: 'Gateway.kt / GatewayHealth' },
      { h: '能力装配', pick: 'core RunAssembler 能力即插件', drop: 'DshToolRegistry 注释自认 Not wired', d: '单服务失败只摘自己、工具组懒展开；补确定性路由。', f: 'RunAssembler / DshToolRegistry.kt' },
      { h: '红线校验', pick: '新写 redLineCheck', drop: '两边都只有 V7Elem 静态文案', d: '人机验证/人脸/单笔>¥500/改安全设置 固定不可授权，可执行校验。', f: 'v7:1574' },
      { h: '督查检阅', pick: '新写 Supervisor 状态机', drop: 'v7:1286 静态', d: '完成必留待检阅、可打回、一次过率统计。', f: 'v7:1286' },
      { h: '身份隔离', pick: '新写 Identity 三层 + 风控联动', drop: 'VaultModel.kt 只有一个 identity:String?', d: '身份→组→会话；一个号被风控，同组先停 24h。', f: 'VaultModel.kt:91' },
      { h: 'MCP 最小权限', pick: '新写 mcpPermLog + 降级', drop: 'spec:1462 仅声明', d: '逐工具最小权限、单服务降级不连坐。', f: 'spec:1462' }
    ];
    h += '<div class="fz">';
    items.forEach(function (it) {
      h += '<div class="decision"><div class="dh"><b>' + esc(it.h) + '</b><span class="pick">取 ' + esc(it.pick) + '</span></div>' +
        '<div class="dd">弃：' + esc(it.drop) + '。' + esc(it.d) + '</div><div class="df">弃文件：' + esc(it.f) + '</div></div>';
    });
    h += '</div>';
    return h;
  }

  function pageApproval() {
    let h = backBar('审批策略演练');
    h += '<div class="card"><div class="hint" style="margin:0 0 9px">Mika 的 <span class="mono">ApprovalPolicy</span> 纯函数：信任按风险分层，不平均分配。</div>' +
      '<input class="inp" id="apprText" placeholder="例如：帮我在小红书发一条笔记">' +
      '<div class="btnrow" style="margin-top:8px">' +
      '<button class="btn sm" data-act="appr" data-mode="ask">询问模式</button>' +
      '<button class="btn sm primary" data-act="appr" data-mode="auto">自主模式</button></div>' +
      '<div id="apprOut"></div></div>';
    h += '<div class="notice">当前真实模式：<b>' + (state.mode === 'ask' ? '询问' : '自主') + '</b>。发消息 / 提交 / 删除直接做；花钱、验证码、人脸两类模式都问。</div>';
    return h;
  }

  function pageSupervisor() {
    let h = backBar('督查与检阅');
    h += '<div class="notice">它先自己核再交给你；待检阅、打回、一次过率都记下来。完成必留一条待检阅。</div>';
    h += section('检阅队列', supervisor.items.length);
    if (!supervisor.items.length) h += '<div class="empty"><div class="e-ic">' + ico('activity') + '</div><b>没有待检阅</b><small>完成的任务会出现在这里。</small></div>';
    else supervisor.items.slice(0, 10).forEach(function (it) {
      h += '<div class="card tight"><b style="font-weight:500">' + esc(it.text) + '</b>' +
        '<div class="hint" style="margin:4px 0 0">状态：' + (it.reviewed ? '已检阅' : '待检阅') + '</div></div>';
    });
    return h;
  }

  function pageIdentity() {
    let h = backBar('身份隔离 · 三层结构');
    h += '<div class="notice warn">mibot 真实只有 <span class="mono">VaultModel.kt:91</span> 一个 identity:String?。这里补了完整三层：身份 → 组 → 会话；一个号被风控，同组先停 24h。</div>';
    h += section('示例：组 → 会话', D.VAULT.length);
    D.VAULT.forEach(function (v) {
      h += '<div class="row"><div class="ic">' + ico('key') + '</div>' +
        '<div class="lab"><b>' + esc(v.idt) + '</b><small>组 ' + esc(v.grp) + ' · Key ' + esc(v.key) + '</small></div>' +
        '<span class="val">' + (v.risk === '禁用' ? '<span class="tag from-mibot">禁用</span>' : v.cool !== '—' ? '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">降 ' + v.cool + '</span>' : '<span class="tag from-c">正常</span>') + '</span></div>';
    });
    h += '<div class="hint">台账管「登录态」，网关管「API Key」，两者分开；金融号默认隔离。</div>';
    return h;
  }

  function pageMcpPerm() {
    let h = backBar('MCP 最小权限');
    h += '<div class="notice warn">spec:1462 仅声明，两边都缺可执行实现。这里补：逐工具最小权限、单服务降级不连坐。</div>';
    h += section('MCP 服务', D.MCP.length);
    D.MCP.forEach(function (m) {
      const on = state.mcp[m.id] !== undefined ? state.mcp[m.id] : m.on;
      h += '<div class="row"><div class="ic">' + ico('plug') + '</div>' +
        '<div class="lab"><b>' + esc(m.name) + '</b><small>' + m.tools + ' 工具 · ' + esc(m.perm) + '</small></div>' +
        '<span class="val">' + (on ? '开' : '关') + '</span></div>';
    });
    h += section('最小权限日志', state.mcpPermLog.length);
    if (!state.mcpPermLog.length) h += '<div class="hint">还没有授权动作。</div>';
    else state.mcpPermLog.slice(0, 8).forEach(function (l) { h += row(esc(l.id), '<span class="mono">' + hhmmss(l.ts) + '</span>', esc(l.perm), null, null, null, null); });
    return h;
  }

  function pageCondition() {
    let h = backBar('条件状态机');
    h += '<div class="notice">core <span class="mono">ConditionCheck.afterCheck</span>：跟上次基线比、计划时段冷却不补跑、revertFire 回滚。</div>';
    h += '<div class="card"><div class="field"><label>条件描述</label><input class="inp" id="condText" placeholder="例如：BTC 价格低于 60000"></div>' +
      '<div class="field"><label>当前观测值</label><input class="inp" id="condObs" placeholder="例如：58200"></div>' +
      '<div class="btnrow"><button class="btn sm primary" data-act="cond-eval">评估是否触发</button></div>' +
      '<div id="condOut"></div></div>';
    h += '<div class="hint">演示：首次满足即触发；同值再评返回 condition_unchanged（不重复触发）；计划时段外返回 cooldown。</div>';
    return h;
  }

  function pageRuntime() {
    let h = backBar('运行模式 · 本地 / 云端');
    h += '<div class="notice">这是与「询问 / 自主」独立的一条轴。本地模式：所有拆解 / 路由 / 步骤都在本机完成，<b>不发任何外部请求</b>，离线可用、数据不出本机。</div>';
    h += section('模式');
    h += '<div class="seg" style="margin-bottom:12px">' +
      '<button class="' + (state.runtime === 'local' ? 'on' : '') + '" data-act="runtime" data-v="local">本地</button>' +
      '<button class="' + (state.runtime === 'cloud' ? 'on' : '') + '" data-act="runtime" data-v="cloud">云端</button></div>';
    if (state.runtime === 'cloud') {
      h += section('云端端点（仅存本机）');
      h += '<div class="card"><div class="field"><label>端点地址</label><input class="inp" id="epUrl" placeholder="https://your-endpoint/v1" value="' + esc(state.cloudEndpoint) + '"></div>' +
        '<div class="field"><label>密钥</label><input class="inp" id="epKey" type="password" placeholder="sk-…" value="' + esc(state.cloudKey) + '"></div>' +
        '<div class="btnrow"><button class="btn sm" data-act="save-ep">保存</button>' +
        '<button class="btn sm primary" data-act="test-ep">测试连接</button></div>' +
        '<div id="epOut"></div></div>';
      const statusTag = state.cloudStatus === 'ok' ? '<span class="tag from-c">在线</span>' : state.cloudStatus === 'fail' ? '<span class="tag from-mibot">回退本地</span>' : '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">未配置</span>';
      h += '<div class="notice warn">未配置或测试失败 → 自动<b>回退本地执行</b>，不卡住你、不假装思考。演示外壳的拆解仍是本地规则，真 Agent 由你的端点承载。</div>';
    } else {
      h += '<div class="card"><div class="hint" style="margin:0">当前：<b>本地模式</b> ' + statusLine() + '。<br>切到云端后，填端点与密钥，点「测试连接」才会真发一次请求。</div></div>';
    }
    return h;
  }

  /* ── mibot 模块页（还原真实模块） ── */
  function pageMail() {
    let h = backBar('AI 邮箱 · 专属别名地址');
    h += '<div class="notice warn">mibot 真实模块：服务↔别名邮箱绑定，取到的验证码自动填入；收不到就老实说收不到。网页壳暂未接入邮件后端（需原生桥接）。</div>';
    h += section('收件箱', D.MAIL.length);
    D.MAIL.forEach(function (m) {
      h += '<div class="card tight"><div style="display:flex;align-items:center;gap:8px"><b style="flex:1;font-weight:500">' + esc(m.from) + '</b>' +
        '<span class="val mono" style="font-size:11px">' + esc(m.time) + '</span></div>' +
        '<div class="hint" style="margin:4px 0 0">' + esc(m.sub) + '</div>' +
        (m.got ? '<div style="margin-top:6px;font-family:monospace;color:var(--green)">验证码：' + esc(m.code) + '（已自动填入）</div>'
          : m.got === false ? '<div style="margin-top:6px;color:var(--err)">未收到验证码 · 已如实标注</div>' : '') + '</div>';
    });
    return h;
  }
  function pageGroups() {
    let h = backBar('Bots 群聊 · 共享黑板');
    h += '<div class="notice warn">mibot 真实模块：只共享「一句话的事实」，不共享过程；过程进各自会话，不上黑板。你亲手写的优先级最高。</div>';
    h += section('群聊', D.GROUPS.length);
    D.GROUPS.forEach(function (g) {
      h += '<div class="row"><div class="ic">' + ico('users') + '</div>' +
        '<div class="lab"><b>' + esc(g.n) + '</b><small>' + g.members + ' 人 · ' + esc(g.last) + '</small></div>' +
        '<span class="val mono">' + esc(g.time) + '</span></div>';
    });
    return h;
  }
  function pageBots() {
    let h = backBar('机器人');
    h += '<div class="notice">mibot 真实模块：自托管 Bots 的创建与编排。</div>';
    h += section('我的 Bots', D.BOTS.length);
    D.BOTS.forEach(function (b) {
      h += '<div class="row"><div class="ic">' + ico('bot') + '</div>' +
        '<div class="lab"><b>' + esc(b.n) + '</b><small>' + esc(b.role) + ' · ' + esc(b.note) + '</small></div>' + sw(b.on) + '</div>';
    });
    return h;
  }
  function pagePlugins() {
    let h = backBar('DSH 插件市场');
    h += '<div class="notice warn">mibot 真实模块：下载 → 校验 → 依赖，三步全绿才算装上；缺运行时直接给两条路。</div>';
    h += section('插件', D.PLUGINS.length);
    D.PLUGINS.forEach(function (p) {
      const stepTag = p.state === '已装' ? '<span class="tag from-c">已装</span>' : p.step === 2 ? '<span class="tag from-mibot">缺运行时</span>' : '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(p.state) + '</span>';
      h += '<div class="row"><div class="ic">' + ico('box') + '</div>' +
        '<div class="lab"><b>' + esc(p.n) + ' <span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(p.cat) + '</span></b><small>v' + p.ver + ' · 步骤 ' + p.step + '/3</small></div>' +
        '<span class="val">' + stepTag + '</span></div>';
    });
    return h;
  }
  function pageSandbox() {
    let h = backBar('沙箱 / 终端');
    h += '<div class="notice warn">mibot 真实模块（规格）：Debian / Ubuntu / Alpine / Arch 四选一，资源上限与联网三档。手机跑不了 Docker，用 proot。</div>';
    const s = D.SANDBOX;
    h += section('当前容器');
    h += '<div class="card">' + [['发行版', s.distro], ['内存', s.ram], ['CPU', s.cpu], ['联网', s.net], ['RootFS', s.rootfs]].map(function (x) {
      return '<div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line)"><span>' + x[0] + '</span><span class="mono" style="color:var(--ink-sub)">' + esc(x[1]) + '</span></div>';
    }).join('') + '</div>';
    h += '<div class="hint">' + esc(s.note) + '</div>';
    return h;
  }
  function pageVault() {
    let h = backBar('账号台账 / 密钥库');
    h += '<div class="notice warn">mibot 真实模块（规格）：身份 → 组 → 会话三层隔离；台账管登录态，网关管 API Key，两者分开。一个号被风控，同组先停 24h。</div>';
    h += section('身份', D.VAULT.length);
    D.VAULT.forEach(function (v) {
      h += '<div class="card tight"><div style="display:flex;align-items:center;gap:8px"><div class="ic">' + ico('key') + '</div><b style="flex:1;font-weight:500">' + esc(v.idt) + '</b>' +
        (v.risk === '禁用' ? '<span class="tag from-mibot">禁用</span>' : v.cool !== '—' ? '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">降 ' + v.cool + '</span>' : '<span class="tag from-c">正常</span>') + '</div>' +
        '<div class="hint" style="margin:6px 0 0">组 ' + esc(v.grp) + ' · Key ' + esc(v.key) + '</div></div>';
    });
    return h;
  }
  function pageProviders() {
    let h = backBar('服务商');
    h += '<div class="notice ok">模型供应商预设与自动切换。本地网关优先，连错 3 次 / 延迟 > 3s 立刻切。</div>';
    h += section('预设', D.PROVIDERS_PRESET.length);
    D.PROVIDERS_PRESET.forEach(function (p) {
      h += '<div class="row tap" data-act="toggle-provider" data-n="' + esc(p.n) + '"><div class="ic ' + (p.on ? 'tone-ok' : '') + '">' + ico('cloud') + '</div>' +
        '<div class="lab"><b>' + esc(p.n) + ' <span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">' + esc(p.kind) + '</span></b><small class="mono">' + esc(p.host) + '</small></div>' + sw(p.on) + '</div>';
    });
    return h;
  }

  /* ── 账户关联（Toki 式：登录方式 + 连接服务 + 退出） ── */
  function pageAccount() {
    const acc = state.account || {};
    const mLabel = acc.method === 'google' ? 'Google 账号' : acc.method === 'phone' ? '手机号' : '邮箱';
    let h = backBar('账户关联');
    h += '<div class="card"><div style="display:flex;align-items:center;gap:10px">' +
      '<div class="av" style="width:44px;height:44px;border-radius:14px;background:var(--accent);font-size:19px">' + esc((acc.name || 'D')[0]) + '</div>' +
      '<div style="flex:1"><div style="font-weight:600;font-size:16px">' + esc(acc.name || '未登录') + '</div>' +
      '<div class="hint mono" style="margin:2px 0 0">' + esc(acc.id || '—') + '</div></div>' +
      '<span class="tag from-c">' + mLabel + '</span></div>' +
      '<div class="hint" style="margin-top:10px">' + (acc.loginAt ? rel(acc.loginAt) + '登录 · ' : '') + '同一账户在手机 / 桌面 PWA / APK 登录后，日程与任务互通。当前数据只存本机（localStorage），连上你的模型端点后才上云。</div></div>';
    h += section('连接服务 · 参考多日历同步');
    h += rowsHtml([
      row('Google 日历', '读取 / 写入日程 · 同步任务提醒', '<span class="tag from-mibot">未连接</span>', 'go', 'data-page="providers"', '▦', 'blue'),
      row('Outlook 日历', '工作日程集中管理', '<span class="tag from-mibot">未连接</span>', 'go', 'data-page="providers"', '▤', 'sky'),
      row('CalDAV / Apple 日历', '自托管日历 · iCloud 订阅', '<span class="tag from-mibot">未连接</span>', 'go', 'data-page="providers"', '◁', 'teal'),
      row('模型端点（云端执行）', state.cloudStatus === 'ok' ? '已连接 · ' + (state.cloudHost || '') : '未配置 · 当前本地执行', '<span class="tag ' + (state.cloudStatus === 'ok' ? 'from-c' : 'from-mibot') + '">' + (state.cloudStatus === 'ok' ? '在线' : '未连') + '</span>', 'go', 'data-page="runtime"', '⌁', 'amber')
    ]);
    h += section('账户操作');
    h += '<div class="card"><div class="btnrow" style="margin:0"><button class="btn danger" data-act="logout">退出登录</button>' +
      '<button class="btn" data-act="go" data-page="data">导出本机数据</button></div>' +
      '<div class="hint" style="margin-top:10px">退出登录不删除本机数据；重新登录同一账户即恢复。删除数据请到「数据」页，有二次确认。</div></div>';
    return h;
  }

  /* ── 公众号阅读（文章可读 / 视频受限 · 边界如实标注） ── */
  function pageArticle() {
    const url = state.articleUrl || '';
    let h = backBar('公众号阅读');
    h += '<div class="notice info">链接已在对话里收到。公众号<b>图文文章</b>是公开网页，可以直接打开读；演示壳不在内部嵌正文（公众号页面禁止被 iframe 嵌入），点「打开原文」看全文。APK 里可长按链接复制后用任意浏览器打开。</div>';
    if (url) {
      h += '<div class="card"><div class="section-title" style="margin-top:0">文章链接</div>' +
        '<div class="mono" style="font-size:12px;word-break:break-all;color:var(--ink-sub)">' + esc(url) + '</div>' +
        '<div class="btnrow" style="margin-top:10px"><a class="btn primary sm" href="' + esc(url) + '" target="_blank" rel="noopener">打开原文 ' + ico('arrowUpRight') + '</a>' +
        '<button class="btn sm" data-act="article-to-idea">存进想法</button>' +
        '<button class="btn sm" data-act="chat-attach">朗读 / 摘要</button></div>' +
        '<div class="hint" style="margin-top:8px">标题提取与 AI 摘要需要云端模式（配置模型端点后可用）；本地演示版不做假摘要。</div></div>';
    } else {
      h += '<div class="empty"><div class="e-ic">' + ico('book') + '</div><b>还没有文章链接</b><small>在对话里贴一个 mp.weixin.qq.com 的文章链接试试。</small></div>';
    }
    h += section('能力边界（如实标注）');
    h += rowsHtml([
      row('公众号图文文章', '公开网页，浏览器 / WebView 可直接读，链接发进对话即可', '<span class="tag from-c">可读</span>', null, null, '▤', 'green'),
      row('文章内嵌视频', '多数走视频 CDN 防盗链，App 内嵌可能黑屏 → 兜底打开原文', '<span class="tag from-mibot">部分受限</span>', null, null, '▶', 'amber'),
      row('视频号 / 需登录内容', '要微信登录态与授权，演示壳不含；真机走虚拟屏驱动微信 App', '<span class="tag from-mika">需真机</span>', null, null, '▣', 'purple'),
      row('公众号历史文章列表', '需要公众号会话或搜一搜权限，同样要真机微信', '<span class="tag from-mika">需真机</span>', null, null, '≡', 'slate')
    ]);
    return h;
  }

  /* ────────────────────── 路由 ────────────────────── */
  const PAGES = { home: pageHome, chat: pageChat, feed: pageFeed, ideas: pageIdeas, me: pageMe };
  const SUB = {
    run: pageRun, gates: pageGates, screens: pageScreens, queue: pageQueue, ability: pageAbility,
    fusion: pageFusion, knowledge: pageKnowledge, mem: pageMem, plans: pagePlans, appaccess: pageAppAccess,
    gateway: pageGateway, usage: pageUsage, spec: pageSpec, recover: pageRecover, appearance: pageAppearance,
    diag: pageDiag, about: pageAbout, data: pageData, decisions: pageDecisions, approval: pageApproval,
    supervisor: pageSupervisor, identity: pageIdentity, mcpperm: pageMcpPerm, condition: pageCondition,
    runtime: pageRuntime, mail: pageMail, groups: pageGroups, bots: pageBots, plugins: pagePlugins,
    sandbox: pageSandbox, vault: pageVault, providers: pageProviders, account: pageAccount, article: pageArticle
  };
  const TITLES = {
    login: ['登录 · Destiny', 'SIGN IN'],
    home: ['日程', 'SCHEDULE'], chat: ['对话', 'CHAT'], feed: ['动态', 'FEED'], ideas: ['想法', 'IDEAS'], me: ['我的', 'ME'],
    run: ['执行过程', 'RUN'], gates: ['授权闸门', 'GATES'], screens: ['虚拟屏', 'SCREENS'], queue: ['排队清单', 'QUEUE'],
    ability: ['能力', 'ABILITY'], fusion: ['融合图谱', 'FUSION'], knowledge: ['知识库', 'KNOWLEDGE'], mem: ['记忆', 'MEMORY'],
    plans: ['定时任务', 'PLANS'], appaccess: ['应用访问', 'APP ACCESS'], gateway: ['本地网关', 'GATEWAY'], usage: ['用量', 'USAGE'],
    spec: ['规格', 'SPEC'], recover: ['恢复与修复', 'RECOVER'], appearance: ['外观', 'APPEARANCE'], diag: ['自检', 'DIAG'],
    about: ['关于运行模式', 'ABOUT'], data: ['数据', 'DATA'], decisions: ['代码取舍', 'DECISIONS'], approval: ['审批演练', 'APPROVAL'],
    supervisor: ['督查检阅', 'SUPERVISOR'], identity: ['身份隔离', 'IDENTITY'], mcpperm: ['MCP 最小权限', 'MCP PERM'],
    condition: ['条件状态机', 'CONDITION'], runtime: ['运行模式', 'RUNTIME'], mail: ['AI 邮箱', 'MAIL'], groups: ['群聊', 'GROUPS'],
    bots: ['机器人', 'BOTS'], plugins: ['插件', 'PLUGINS'], sandbox: ['沙箱', 'SANDBOX'], vault: ['密钥库', 'VAULT'], providers: ['服务商', 'PROVIDERS'],
    account: ['账户关联', 'ACCOUNT'],
    article: ['公众号阅读', 'ARTICLE']
  };
  const LIVE_PAGES = { home: 1, chat: 1, run: 1, screens: 1, queue: 1, gates: 1, feed: 1, ideas: 1, me: 1 };

  let lastNavKey = null, lastChatCount = 0;
  function render(keepScroll) {
    const v = $('#view');
    const key = state.page && (PAGES[state.page] || SUB[state.page]) ? state.page : state.tab;
    // 导航上下文未变时保持滚动位置；只有切换页签/页面才回到顶部
    const navChanged = lastNavKey !== key;
    const top = (keepScroll || !navChanged) ? v.scrollTop : 0;
    if (!state.account) {
      v.innerHTML = pageLogin();
      const d0 = $('#composerDock'); if (d0) { d0.classList.remove('on'); d0.innerHTML = ''; }
      $('#topTitle').textContent = TITLES.login[0]; $('#topSub').textContent = TITLES.login[1];
      $('#topbar').className = '';
      $('#topDot').className = 'dot ok';
      document.documentElement.setAttribute('data-theme', state.theme);
      document.documentElement.style.setProperty('--fs', state.fontSize + 'px');
      $('body').style.fontSize = state.fontSize + 'px';
      document.querySelectorAll('.tab').forEach(function (b) { b.classList.remove('on'); });
      lastNavKey = 'login'; lastChatCount = 0;
      return;
    }
    const fn = PAGES[key] || SUB[key] || pageHome;
    v.innerHTML = fn();
    // 输入条 dock：只在首次进入对话时创建，之后 tick 重建 view 不动它，草稿与光标不丢
    const dock = $('#composerDock');
    if (dock) {
      if (key === 'chat') {
        if (!dock.classList.contains('on') || !dock.querySelector('#chatInput')) {
          dock.innerHTML = composerHtml(); dock.classList.add('on');
        }
      } else { dock.classList.remove('on'); dock.innerHTML = ''; }
    }
    // 聊天页新消息 / 刚切进对话 → 跟到最新一条；其余保持原滚动位置
    const chatNew = key === 'chat' && state.chatMsgs.length > lastChatCount;
    // 对话页：只要原本贴着底部就持续跟随最新一条；用户主动上滑看历史时不打扰
    const distFromBottom = Math.max(0, v.scrollHeight - top - v.clientHeight);
    const wantBottom = key === 'chat' && (chatNew || navChanged || distFromBottom < 120);
    v.scrollTop = wantBottom ? v.scrollHeight : top;
    if (wantBottom && typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(function () {
        if (state.tab === 'chat' && !state.page) v.scrollTop = v.scrollHeight;
      });
    }
    lastNavKey = key;
    lastChatCount = key === 'chat' ? state.chatMsgs.length : 0;
    const T = TITLES[key] || ['Destiny', 'DESTINY'];
    $('#topTitle').textContent = T[0];
    $('#topSub').textContent = T[1];
    $('#topbar').className = state.page ? 'has-back' : '';
    $('#topDot').className = 'dot ' + (pendingCount() ? 'wait' : 'ok');
    document.documentElement.setAttribute('data-theme', state.theme);
    document.documentElement.style.setProperty('--fs', state.fontSize + 'px');
    $('body').style.fontSize = state.fontSize + 'px';
    document.querySelectorAll('.tab').forEach(function (b) { b.classList.toggle('on', b.getAttribute('data-tab') === state.tab); });
    const badge = $('#tabbar').querySelector('.badge'); if (badge) badge.remove();
    if (pendingCount()) {
      const b = document.createElement('span'); b.className = 'badge'; b.textContent = pendingCount();
      $('#tabbar').querySelector('[data-tab="me"]').appendChild(b);
    }
  }
  function renderChrome() {
    if (!state.account) return;
    const T = TITLES[state.page && (PAGES[state.page] || SUB[state.page]) ? state.page : state.tab] || ['Destiny', 'DESTINY'];
    $('#topTitle').textContent = T[0]; $('#topSub').textContent = T[1];
    $('#topbar').className = state.page ? 'has-back' : '';
    $('#topDot').className = 'dot ' + (pendingCount() ? 'wait' : 'ok');
    const badge = $('#tabbar').querySelector('.badge'); if (badge) badge.remove();
    if (pendingCount()) { const b = document.createElement('span'); b.className = 'badge'; b.textContent = pendingCount(); $('#tabbar').querySelector('[data-tab="me"]').appendChild(b); }
  }
  function go(page) { state.back = (state.page || state.tab); state.page = page; persist(); }
  function navigate(tab) { state.tab = tab; state.page = null; persist(); }
  function back() {
    if (state.page) { const p = state.page; state.page = null; persist(); }
  }

  /* ────────────────────── Toast / Sheet ────────────────────── */
  function toast(msg) {
    let t = $('#toast'); if (!t) { t = document.createElement('div'); t.id = 'toast'; t.className = 'toast'; document.body.appendChild(t); }
    t.textContent = msg; t.classList.add('show');
    clearTimeout(toastTimer); toastTimer = setTimeout(function () { t.classList.remove('show'); }, 2200);
  }
  function openSheet(mode) {
    sheetMode = mode;
    const scrim = document.createElement('div'); scrim.className = 'scrim'; scrim.setAttribute('data-act', 'close-sheet'); document.body.appendChild(scrim);
    const s = document.createElement('div'); s.className = 'sheet'; s.id = 'sheet';
    if (mode === 'new-task') {
      s.innerHTML = '<div class="modal-title">新会话 · 派一件事</div>' +
        '<div class="field"><label>要办的事</label><textarea class="inp" id="fText" placeholder="例如：在小红书发一条笔记"></textarea></div>' +
        '<div class="field"><label>目标 App</label><select class="inp" id="fApp">' + APPS.map(function (a) { return '<option>' + a + '</option>'; }).join('') + '</select></div>' +
        '<div class="field"><label>风险（可选）</label><select class="inp" id="fRisk"><option value="">自动识别</option><option value="pay">付款 / 下单</option><option value="login">登录 / 验证码</option></select></div>' +
        '<div class="btnrow"><button class="btn primary" data-act="create-task">开始执行</button><button class="btn" data-act="close-sheet">取消</button></div>';
    } else if (mode === 'insert') {
      s.innerHTML = '<div class="modal-title">插话</div><div class="field"><label>补充或更正（下一步之前读到）</label><textarea class="inp" id="iText" placeholder="例如：标题改成……"></textarea></div>' +
        '<div class="btnrow"><button class="btn primary" data-act="do-insert">发出去</button><button class="btn" data-act="close-sheet">取消</button></div>';
    }
    document.body.appendChild(s);
    setTimeout(function () { const el = s.querySelector('textarea,input'); if (el) el.focus(); }, 50);
  }
  function closeSheet() { sheetMode = null; const sc = document.querySelector('.scrim'); if (sc) sc.remove(); const sh = $('#sheet'); if (sh) sh.remove(); }

  /* ────────────────────── 事件 ────────────────────── */
  document.addEventListener('click', function (e) {
    const el = e.target.closest('[data-act],[data-tab],[data-to],[data-page]');
    if (!el) return;
    const a = el.getAttribute('data-act');
    if (a) {
      if (a === 'back') { back(); return; }
      if (a === 'close-sheet') { closeSheet(); return; }
      if (a === 'create-task') {
        const t = $('#fText').value, ap = $('#fApp').value, r = $('#fRisk').value;
        closeSheet(); createTask(t, ap, r); return;
      }
      if (a === 'do-insert') {
        const t = ($('#iText') && $('#iText').value || '').trim(); closeSheet();
        const sel = getSel(); if (sel && t) { sel.inserts.push({ t: Date.now(), text: t }); ledger.append(sel.id, 'insert', { text: t }, false); toast('插话已记录 · 下一步之前读到'); persist(true); }
        return;
      }
      if (a === 'sheet') { openSheet(el.getAttribute('data-mode')); return; }
      if (a === 'go') { go(el.getAttribute('data-page')); return; }
      if (a === 'cap' || a === 'module') { go(el.getAttribute('data-to')); return; }
      if (a === 'open-task') { state.selectedTask = el.getAttribute('data-id'); state.page = 'run'; state.tab = 'chat'; persist(); return; }
      if (a === 'feed') { state.feedTab = el.getAttribute('data-v'); persist(true); return; }
      if (a === 'ffilter') { state.fusionFilter = el.getAttribute('data-v'); persist(true); return; }
      if (a === 'theme') { state.theme = el.getAttribute('data-v'); persist(true); return; }
      if (a === 'fs') { state.fontSize = +el.getAttribute('data-v'); persist(true); return; }
      if (a === 'runtime') { state.runtime = el.getAttribute('data-v'); persist(true); return; }
      if (a === 'toggle-mode') { state.mode = state.mode === 'ask' ? 'auto' : 'ask'; toast(state.mode === 'ask' ? '已切到询问模式' : '已切到自主模式 · 付款也不再先问'); persist(true); return; }
      if (a === 'gate') { resolveGate(el.getAttribute('data-d')); return; }
      if (a === 'pause') { const t = getSel(); if (t && t.status === 'running') { t.status = 'paused'; pool.release(t.id); ledger.append(t.id, 'paused', {}, false); toast('已暂停'); persist(true); } return; }
      if (a === 'resume') { resumeTask(); return; }
      if (a === 'stop') { stopTask(); return; }
      if (a === 'route') { routeAnswer(); return; }
      if (a === 'toggle-skill') { const id = el.getAttribute('data-id'); state.skills[id] = state.skills[id] === false; toast('技能 ' + (state.skills[id] === false ? '已关' : '已开')); persist(true); return; }
      if (a === 'toggle-mcp') { const id = el.getAttribute('data-id'); state.mcp[id] = state.mcp[id] === undefined ? false : !(state.mcp[id]); toast('MCP ' + (state.mcp[id] ? '已开' : '已关')); persist(true); return; }
      if (a === 'toggle-app') { const ap = el.getAttribute('data-app'); state.appAccess[ap] = state.appAccess[ap] !== true; toast(ap + (state.appAccess[ap] ? ' 已放行' : ' 已禁用')); persist(true); return; }
      if (a === 'toggle-provider') { const n = el.getAttribute('data-n'); const p = D.PROVIDERS_PRESET.filter(function (x) { return x.n === n; })[0]; if (p) { p.on = !p.on; toast(n + (p.on ? ' 已启用' : ' 已停用')); persist(true); } return; }
      if (a === 'q-up') { const id = el.getAttribute('data-id'); const t = state.tasks.filter(function (x) { return x.id === id; })[0]; if (t) { t.priority = (t.priority || 0) + 1; toast('已提到同 App 队列最前'); persist(true); } return; }
      if (a === 'q-out') { const id = el.getAttribute('data-id'); const t = state.tasks.filter(function (x) { return x.id === id; })[0]; if (t) { t.status = 'stopped'; t.summary = '你把它移出了排队。'; pool.release(t.id); toast('已移出排队'); persist(true); } return; }
      if (a === 'add-idea') { const v = ($('#ideaInput') && $('#ideaInput').value || '').trim(); if (v) { state.ideas.unshift({ t: v, tag: '随手', time: '刚刚', pinned: false }); toast('已记下'); persist(true); } return; }
      if (a === 'del-idea') { state.ideas.splice(+el.getAttribute('data-i'), 1); persist(true); return; }
      if (a === 'add-mem') { const v = ($('#memInput') && $('#memInput').value || '').trim(); if (v) { state.memories.unshift({ kind: '做法', text: v, approved: false, t: Date.now() }); toast('已提交待批准'); persist(true); } return; }
      if (a === 'del-mem') { state.memories.splice(+el.getAttribute('data-i'), 1); persist(true); return; }
      if (a === 'add-plan') {
        const txt = ($('#planText') && $('#planText').value || '').trim(); if (!txt) { toast('先写要做什么'); return; }
        const kind = $('#planKind').value; const times = ($('#planTimes').value || '').split(',').map(function (x) { return x.trim(); }).filter(Boolean);
        const p = { text: txt, kind: kind, times: times, lastFired: null };
        const out = $('#planPreview'); if (out) out.innerHTML = '<div class="notice info" style="margin-top:10px"><b>接下来三次</b><br>' + nextThree(p).map(function (x) { return '· ' + x; }).join('<br>') + '<br><button class="btn sm primary" data-act="save-plan" style="margin-top:8px">确认保存</button></div>';
        window.__plan = p; return;
      }
      if (a === 'save-plan') { if (window.__plan) { state.plans.push(window.__plan); toast('已保存计划'); window.__plan = null; persist(true); } return; }
      if (a === 'del-plan') { state.plans.splice(+el.getAttribute('data-i'), 1); persist(true); return; }
      if (a === 'appr') {
        const mode = el.getAttribute('data-mode'); const q = ($('#apprText') && $('#apprText').value || '').trim(); if (!q) { toast('先写一句目的'); return; }
        const rl = C.redLineCheck({ text: q, target: '', amount: null });
        const out = $('#apprOut');
        const pol = C.approvalDecide({ payment: /付款|下单|买|支付|充值|转帐|转账/.test(q), ask: /验证码|人脸|实名|登录/.test(q) ? 'handover' : null }, { attendance: 'Attended', origin: 'Ui' }, mode === 'auto');
        if (out) out.innerHTML = '<div class="notice ' + (rl.blocked ? 'risk' : 'ok') + '" style="margin-top:10px"><b>' + (rl.blocked ? '红线 · 问也不许做' : '策略 ' + pol) + '</b><br>' +
          (rl.blocked ? '命中：' + rl.hits.join(' / ') : '模式：' + (mode === 'ask' ? '询问' : '自主') + ' · ' + (pol === 'Allow' ? '默认放行' : pol === 'Deny' ? '拒绝（脱敏/无人值守）' : '询问确认')) + '</div>';
        return;
      }
      if (a === 'cond-eval') {
        const txt = ($('#condText') && $('#condText').value || '').trim(); const obs = ($('#condObs') && $('#condObs').value || '').trim();
        if (!txt || !obs) { toast('条件和观测值都要填'); return; }
        const num = parseFloat(obs);
        // afterCheck(def, outcome, nowMs, slotMs) 才是真实 API：跟上次基线比、计划时段冷却不补跑。
        const def = Object.assign({ conditionGoal: txt, notifyWhen: 'once', cooldownMs: null, reminderText: txt }, state.conditionState || {});
        const r = C.afterCheck(def, { met: true, value: isNaN(num) ? obs : num }, Date.now(), Date.now());
        state.conditionState = r.next;
        const out = $('#condOut');
        const label = r.fire ? '<span class="tag from-c">触发</span>'
          : r.reason === C.COND.UNCHANGED ? '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">未变·不重复</span>'
          : r.reason === C.COND.COOLDOWN ? '<span class="tag from-mibot">冷却中</span>'
          : '<span class="tag" style="background:var(--chip-bg);color:var(--ink-sub)">未触发</span>';
        if (out) out.innerHTML = '<div class="notice info" style="margin-top:10px">' + label + ' · reason=' + r.reason + ' · 基线已更新为 ' + esc(obs) + '</div>';
        persist();
        return;
      }
      if (a === 'selfcheck') {
        state.checkResult = [
          (function () { try { localStorage.setItem('__d', '1'); localStorage.removeItem('__d'); return true; } catch (e) { return false; } })(),
          !!localStorage.getItem(KEY), (typeof Notification !== 'undefined' && Notification.permission === 'granted'),
          navigator.onLine !== false, false, false, false, false, false
        ];
        toast('自检完成 · 设备级 5 项如实标「需真机」'); persist(true); return;
      }
      if (a === 'export-diag' || a === 'export-data') {
        exportJson('destiny-' + (a === 'export-diag' ? 'diagnostic' : 'data') + '-' + Date.now() + '.json', {
          app: 'Destiny', generatedAt: new Date().toISOString(), runtime: 'browser / local deterministic engine', mode: state.mode, runtimeMode: state.runtime,
          selfCheck: D.SELF_CHECK.map(function (c, i) { return { name: c.n, deviceOnly: c.device, result: state.checkResult ? state.checkResult[i] : null }; }),
          usage: state.usage, tasks: state.tasks, approvals: state.approvals, memories: state.memories, plans: state.plans, ideas: state.ideas,
          mibotModules: D.MIBOT_MODULES.length, fusion: D.FUSION.length, note: '不含账号、Key 与消息内容'
        });
        return;
      }
      if (a === 'clear-data') {
        if (!confirm('确认清空 Destiny 的全部本地数据？此操作不可撤销。')) return;
        localStorage.removeItem(KEY); state = DEFAULT(); state.ideas = D.IDEAS.slice(); initEngines(); persist(); toast('已清空'); return;
      }
      if (a === 'save-ep') { state.cloudEndpoint = ($('#epUrl') && $('#epUrl').value || '').trim(); state.cloudKey = ($('#epKey') && $('#epKey').value || '').trim(); toast('已保存（仅本机）'); persist(true); return; }
      if (a === 'test-ep') {
        const url = ($('#epUrl') && $('#epUrl').value || '').trim(); if (!url) { toast('先填端点地址'); return; }
        toast('正在测试连接…');
        fetch(url, { method: 'GET', headers: { 'Authorization': 'Bearer ' + (($('#epKey') && $('#epKey').value) || '') }, signal: AbortSignal.timeout(8000) })
          .then(function (r) { state.cloudStatus = r.ok ? 'ok' : 'fail'; state.cloudHost = (function () { try { return new URL(url).host; } catch (e) { return url; } })(); if (r.ok) state.runtime = 'cloud'; else state.runtime = 'local'; toast(r.ok ? '连接成功 · 已切云端' : '连接失败 · 回退本地'); persist(true); })
          .catch(function () { state.cloudStatus = 'fail'; state.runtime = 'local'; toast('连接失败 · 回退本地执行'); persist(true); });
        return;
      }
      if (a === 'login-go') {
        const v = el.getAttribute('data-v');
        if (v === 'google') doLogin('google', 'google-user@local');
        else { state.loginStep = v; persist(true); }
        return;
      }
      if (a === 'login-back') { state.loginStep = null; persist(true); return; }
      if (a === 'login-confirm') {
        const v = ($('#loginInput') && $('#loginInput').value || '').trim();
        if (state.loginStep === 'phone' ? !/^1\d{10}$/.test(v) : !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v)) { toast(state.loginStep === 'phone' ? '手机号格式不对（11 位）' : '邮箱格式不对'); return; }
        doLogin(state.loginStep, v); return;
      }
      if (a === 'logout') {
        if (!confirm('退出登录？本机数据保留，重新登录同一账户即恢复。')) return;
        state.account = null; state.loginStep = null; state.tab = 'chat'; state.page = null; persist(); return;
      }
      if (a === 'chat-send') {
        const inp = $('#chatInput'); const v = (inp && inp.value || '').trim(); if (!v) { toast('先说一句要办的事'); return; }
        if (inp) inp.value = '';
        const urlM = v.match(/https?:\/\/[^\s]+/);
        if (urlM && /mp\.weixin\.qq\.com|weixin\.qq\.com\/s/.test(urlM[0])) {
          state.articleUrl = urlM[0];
          state.chatMsgs.push({ role: 'user', text: v, t: Date.now() });
          state.chatMsgs.push({ role: 'bot', text: '收到，公众号文章。图文正文是公开网页，可以直接读；点这句进阅读页。内嵌视频多数有防盗链，兜底用「打开原文」。', t: Date.now(), page: 'article' });
          state.tab = 'chat'; state.page = null; persist(true); return;
        }
        createTask(v, guessApp(v), null, true); return;
      }
      if (a === 'article-to-idea') {
        const u = state.articleUrl || ''; if (!u) { toast('还没有文章链接'); return; }
        state.ideas.unshift({ t: '公众号文章：' + u, tag: '文章', time: '刚刚', pinned: false });
        toast('已存进想法 · 可并进动态'); persist(true); return;
      }
      if (a === 'chat-chip') {
        const v = el.getAttribute('data-v'); if (v) createTask(v, guessApp(v), null, true); return;
      }
      if (a === 'chat-voice') { toast('演示版未接语音；真机 APK 里长按说话，自动转文字建任务'); return; }
      if (a === 'chat-attach') { toast('演示版未接附件；真机可发截图 / 海报，从图里提取时间地点事项建日程'); return; }
      if (a === 'feed-open') { toast('演示内容：' + el.getAttribute('data-t')); return; }
      return;
    }
    const tab = el.getAttribute('data-tab');
    if (tab) { navigate(tab); return; }
    const to = el.getAttribute('data-to');
    if (to) { go(to); return; }
    const pg = el.getAttribute('data-page');
    if (pg) { go(pg); return; }
  });

  // 顶栏按钮：新建会话 / 快速切主题
  const btnNew = $('#btnNew'), btnTheme = $('#btnTheme');
  if (btnNew) btnNew.addEventListener('click', function () { openSheet('new-task'); });
  if (btnTheme) btnTheme.addEventListener('click', function () {
    state.theme = state.theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', state.theme);
    persist();
  });

  function exportJson(name, obj) {
    const blob = new Blob([JSON.stringify(obj, null, 2)], { type: 'application/json' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = name; a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
  }

  /* ────────────────────── 初始化 ────────────────────── */
  // 建底栏
  (function buildTabbar() {
    const bar = $('#tabbar');
    bar.innerHTML = D.NAV.map(function (n) {
      return '<button class="tab" data-tab="' + n.id + '" aria-label="' + n.label + '"><span class="ti">' + ico(TAB_ICON[n.id] || 'grid') + '</span><span>' + n.label + '</span></button>';
    }).join('');
  })();
  initEngines();
  render();
  setInterval(tick, 850);

  // PWA 安装提示
  let deferredPrompt = null;
  window.addEventListener('beforeinstallprompt', function (e) { e.preventDefault(); deferredPrompt = e; showInstall(); });
  function showInstall() {
    if (localStorage.getItem('destiny-installed')) return;
    let b = $('#install'); if (b) return;
    b = document.createElement('div'); b.id = 'install'; b.className = 'install';
    b.innerHTML = '可装到桌面，像原生 App 一样用 <button id="instBtn">安装</button><button id="instNo">不再提示</button>';
    document.body.appendChild(b);
    $('#instBtn').onclick = function () { if (deferredPrompt) deferredPrompt.prompt(); deferredPrompt = null; localStorage.setItem('destiny-installed', '1'); b.remove(); };
    $('#instNo').onclick = function () { localStorage.setItem('destiny-installed', '1'); b.remove(); };
  }

  // 测试 / 自动化入口（无害，允许外部脚本驱动 SPA 路由与读取状态）
  window.Destiny = {
    open: go, tab: navigate,
    getState: function () { return state; },
    seedRun: function () {
      const id = 'audit-run-' + Date.now();
      state.tasks.unshift({ id: id, text: '审计用的演示会话', app: '小红书', status: 'running',
        createdAt: Date.now(), gate: null, vd: 0, flowKey: 'default', risk: null, idx: 1,
        steps: [{ title: '已接入目标 App', t: Date.now(), ms: 12, detail: '演示步骤', auto: true }],
        inserts: [], summary: null,
        metrics: { steps: 1, tokens: 0, ms: 12, calls: 1 }, waitingFor: null });
      state.selectedTask = id; persist(); render(); return id;
    }
  };
})();
