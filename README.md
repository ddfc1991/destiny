# Destiny · 聊天式端侧 Agent

mibot（自托管控制面）× Mika（端侧执行）融合的 Agent 应用。**UI 与账户体系参考 [Toki](https://toki.com)**：聊天流为主界面，说一句话就变成任务；登录支持 Google / 邮箱 / 手机号；账户页含连接服务（Google 日历 / Outlook / CalDAV / 模型端点）。

## 特性

- **Design System v2（重设计）**：语义化颜色（陶土橙 = 用户/主操作，靛青 = Agent/系统）、三级卡片层级、全套内联 SVG 线性图标、选中态胶囊导航、≥44px 点击热区；输入条独立常驻 dock（不遮挡内容、tick 重建不清空草稿）；浅/暗双主题、320px 小屏与大字体验证
- **Icon System v3（图标重做）**：路径统一 Lucide 规范并修正语义错配（日程=日历 / 动态=订阅流 / 想法=灯泡）；笔画 `1.8 → 2`、尺寸整体提一档（底栏 22 / 列表 20 / 宫格 24）；新增实心形态用于主操作与品牌锚点；任务列表中文单字徽标、空态 emoji、问候语 👋 全部收口成 SVG；行图标兜底从「四格方块」改为中性圆点，全站 35 种图标、兜底命中率 0/254
- **Toki 式主界面**：对话即入口——底部输入条（语音 / 附件 / 发送）+ 快捷指令 chips，用户气泡与 Agent 回复成对出现，回复内显性「查看执行过程」CTA；日期分隔 + 独立「进行中」状态气泡；日程页「接下来」卡片
- **公众号阅读**：把 mp.weixin.qq.com 文章链接发进对话 → 自动进阅读页（打开原文 / 存进想法）；能力边界如实标注——图文可读、内嵌视频多数防盗链兜底打开原文、视频号与历史文章列表需真机微信登录态
- **账户关联**：首启登录页（Google / 邮箱 / 手机号，演示版为本地账户）；我的页账户卡 + 账户关联页（登录方式 / 连接服务 / 退出登录 / 数据导出）
- **真实状态机**：4 槽位虚拟屏 + App 级互斥锁 + 多任务排队清单；授权闸门真实阻断（付款 / 验证码）；插话 / 停止交代 / 断点恢复
- **本地模式 / 云端模式**：本地确定性引擎不联网；云端填模型端点，测试连接通过才切
- **红线不可授权**：人机验证、人脸实名、单笔 >¥500、改系统安全设置、改银行/支付密码
- **mibot 主干模块**：AI 邮箱 / 群聊 / 机器人 / 插件 / 沙箱 / 密钥库 / 服务商 / 本地网关 / 用量
- PWA 可装桌面；深 / 浅主题（默认暖瓷浅色）

## 本地运行

```bash
node serve.js   # http://127.0.0.1:8788
```

## 安卓 APK

推送到 `main` 后 GitHub Actions 自动构建（Capacitor 6 包装 Web 应用）：

- 产物：Actions Artifact `Destiny-debug-apk`，并自动发布到 Releases 的 **apk-latest**
- 打 `v*` tag 会发布对应版本 Release

## 结构

| 文件 | 说明 |
| --- | --- |
| `destiny-core.js` | 纯逻辑引擎（UMD，Node 可 require）：审批 / 虚拟屏 / 事件账本 / 网关健康 / 条件状态机 / 红线 |
| `destiny-data.js` | 融合口径数据（mibot 21 模块 × Mika 9 块 → 32 条能力） |
| `destiny-app.js` | 界面与接线（聊天流 / 登录 / 排队 / 闸门 / 记忆 / 定时 / 图标系统） |
| `destiny.css` | Design System v2 + Icon System v3：语义 tokens / 组件 / 图标几何 / 双主题 / 响应式 |
| `.github/workflows/build-apk.yml` | CI：Capacitor 打包 APK |

## 测试

本地开发验收脚本（依赖本机 Playwright-core + Edge，不随仓库分发）：

```bash
node _coretest.js            # 纯逻辑引擎 62
node _chat_test.js           # 聊天 / 账户 / 公众号 16
node _selftest.js            # 全流程交互 16
node _queue_runtime_test.js  # 排队 + 本地/云端 15
node _audit.js               # 37 个页面入口体检
node _v2_check.js            # v2 设计验收 22（几何 / 图标 / 热区 / 遮挡 / 暗色）
node _icon_check.js          # v3 图标验收 21（语义 / 几何 / 实心态 / 兜底率 / emoji 清零）
```

合计 **189 项，0 JS 运行时错误**。

## 诚实边界

演示外壳为本地确定性引擎，不调用 LLM、不操控真机 App；登录为本地演示账户（真机版接真实 OAuth / 短信）；「需真机」的能力在自检页如实标注。
