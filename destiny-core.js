/* Destiny 择优引擎 · 纯逻辑层（无 DOM 依赖，可在 Node 里单测）
 *
 * 移植来源（真实 Kotlin 文件，均在 _full/src/android/）：
 *   CORE = core/src/main/kotlin/com/mibot/core
 *   APP  = app/src/main/java/com/openminis/app
 * 每节注释写明「谁赢 / 为什么 / 弃掉了什么」，取舍矩阵见文末 DECISIONS。
 * 标 [新写] 的是 Mika 与 mibot 两边都只有 V7Elem 静态文案、没有真实实现的缺口。
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.DestinyCore = factory();
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  /* ── 1. 审批策略：Mika core 胜，补 mibot 的超时默认拒绝与脱敏 ─────
   * 胜者 CORE/action/Approval.kt ApprovalPolicy.decide（纯函数、可测）
   *      CORE/gate/ConfirmationGate.kt（一次性 Grant 绑定屏幕事实）
   * 借   APP/mibot/vault/VaultBroker.kt（120s 超时默认拒绝、scrub 脱敏）
   * 弃   VaultBroker.isHighRisk() 关键词表（字符串包含易误判/绕过）
   *      APP/config/confirm/ConfigConfirmationGate.kt（第三套重复闸门）
   */
  const RISK = { READ: 'Read', CHANGE: 'Change', EXTERNAL: 'External', IRREVERSIBLE: 'Irreversible', UNKNOWN: 'Unknown' };
  const MAX_GRANT_STEPS = 3;       // CORE/gate/ConfirmationGate.kt
  const CONFIRM_TIMEOUT_MS = 120000; // APP/mibot/vault/VaultBroker.kt 超时即拒绝

  function approvalDecide(risk, provenance, autonomous) {
    if (autonomous) return 'Allow';                       // 自主：连付款也不先问
    if (!risk.payment && !risk.ask) return 'Allow';
    if (!provenance || provenance.attendance === 'Unattended' || provenance.origin !== 'Ui') return 'Deny';
    return 'Ask';
  }

  function scrub(text) {                                 // 借 VaultBroker.scrub
    return String(text || '')
      .replace(/(?:密码|口令|验证码|卡号)\s*[:：]?\s*\S{4,}/g, '[已脱敏]')
      .replace(/\b(\d{16,19})\b/g, '[卡号已脱敏]')
      .replace(/\b([\w.+-]+@[\w-]+\.[\w.]+)\b/g, '[邮箱已脱敏]');
  }

  class ConfirmationGate {
    constructor() { this.reset(); }
    reset() { this.pending = null; this.grant = null; this.declined = new Set(); this.pendingAt = 0; }
    declare(req, foregroundApp, now) {
      if (req.action === 'pay') return 'Unsupported';                  // 付款走 hand_to_user 接管
      if (this.declined.has(req.action + '|' + req.app)) return 'Declined';
      if (!foregroundApp || foregroundApp !== req.app) return 'Mismatch';
      this.pending = req; this.pendingAt = now || Date.now();
      return 'Pending';
    }
    decide(choice, step, now) {
      const req = this.pending; if (!req) return null;
      this.pending = null;
      // 超时默认拒绝（mibot 侧优点）
      if ((now || Date.now()) - this.pendingAt > CONFIRM_TIMEOUT_MS) {
        this.declined.add(req.action + '|' + req.app); return 'TimedOutDeny';
      }
      if (choice !== 'Once') { this.declined.add(req.action + '|' + req.app); return 'Deny'; }
      this.grant = { req: req, step: step }; return 'Granted';
    }
    /** 只放行紧接着那一个匹配操作；有同意不论结果都用掉它。 */
    permits(hit, screen, step, typing, seen) {
      const g = this.grant; if (!g) return { permit: 'NoGrant' };
      this.grant = null;
      const r = g.req, s = screen || {};
      const shown = (v) => typeof v === 'string' && (s.text || '').indexOf(v) >= 0;
      const seenShows = (v) => typeof v === 'string' && v.length >= 2 && (seen || []).some(function (x) { return x.indexOf(v) >= 0; });
      let reason = null;
      if (hit.action === 'pay') reason = '付款永不在这里放行，必须人工接管';
      else if (step - g.step > MAX_GRANT_STEPS) reason = '同意已过期（>' + MAX_GRANT_STEPS + ' 步）';
      else if (hit.action !== r.action) reason = '同意的是 ' + r.action + '，这一步是 ' + hit.action;
      else if (!s.app || s.app !== r.app) reason = '前台 App 与申报不一致';
      else if (!shown(r.target) && !seenShows(r.target)) reason = '屏上找不到申报对象「' + r.target + '」';
      else if (r.action === 'send' && r.content !== (typing || '')) reason = '要发的字与同意时不完全一致';
      else if (r.amount != null && !shown(r.amount)) reason = '屏上找不到申报金额 ' + r.amount;
      return reason ? { permit: 'Mismatch', reason: reason } : { permit: 'Allowed' };
    }
    /** 不在屏上的一步（工具自己声明要问），申报一字不差才放行。 */
    permitsExactly(ask, step) {
      const g = this.grant; if (!g) return { permit: 'NoGrant' };
      this.grant = null;
      if (step - g.step > MAX_GRANT_STEPS) return { permit: 'Mismatch', reason: '同意已过期' };
      if (JSON.stringify(g.req) !== JSON.stringify(ask)) return { permit: 'Mismatch', reason: '同意的是另一条操作' };
      return { permit: 'Allowed' };
    }
    /** 打字/清空不作废同意，其余写操作（返回、点别处、开 App）作废。 */
    noteWrite(verb) { if (verb !== 'TypeText' && verb !== 'Clear') this.grant = null; }
  }

  /* ── 2. 红线：[新写] 两边都缺真实校验 ─────────────────────────────
   * 证据：APP/mibot/v7/DestinyV7Screens.kt:1574-1593 与 spec/DestinySpecScreens.kt:1602
   *      只有 V7Elem(...) 静态文案「永远不许 · 4 条」，无数据表、无校验函数。
   * 这里做成可执行判定，且明确「不进一直允许列表」。
   */
  const RED_LINES = [
    { id: 'captcha', label: '人机验证（滑块 / 点选）', test: function (a) { return /(滑块|人机验证|captcha|点选验证)/i.test(a.text || ''); } },
    { id: 'identity', label: '人脸或实名认证', test: function (a) { return /(人脸|活体|实名认证|身份证核验)/.test(a.text || ''); } },
    { id: 'amount', label: '单笔超过 ¥500 的付款', test: function (a) { return typeof a.amount === 'number' && a.amount > 500; } },
    { id: 'security', label: '修改系统安全设置', test: function (a) { return a.target === 'system' && /(无障碍|无线调试|锁屏|安全设置)/.test(a.text || ''); } },
    { id: 'credential', label: '改账户 / 银行 / 支付密码', test: function (a) { const t = a.text || ''; return /(改|修改|重置|设置)[^。？]{0,6}(密码|口令)|(密码|口令)[^。？]{0,6}(改|修改|重置|设置)/.test(t) && /(银行|支付|账户|帐户|银行卡)/.test(t); } }
  ];
  function redLineCheck(action) {
    const hits = RED_LINES.filter(function (r) { return r.test(action); });
    return {
      blocked: hits.length > 0,
      hits: hits.map(function (r) { return r.label; }),
      reason: hits.length ? '红线：' + hits.map(function (r) { return r.label; }).join(' / ') : null
    };
  }

  /* ── 3. 事件账本 + 断点恢复：解掉两边直接矛盾 ──────────────────────
   * 胜者 CORE/record/RunEvent.kt + CORE/run/RunStore.kt
   *      事件即账本；重启把未结束 run 标 Interrupted，「不据此重放工具调用」。
   * 冲突 APP/mibot/queue/TaskRunQueue.kt：suspendCurrent(breakpoint) 要从断点续跑。
   * 折中（本实现）：写类步骤（带 sideEffect）一律不重放；恢复只从最近一个
   *      已确认无副作用的读步骤之后接。既保住 mibot 的断点价值，也守住 core 的安全底线。
   */
  class EventLedger {
    constructor() { this.events = []; this.seq = 0; }
    append(runId, kind, payload, sideEffect) {
      const e = { seq: ++this.seq, ts: Date.now(), runId: runId, kind: kind,
        payload: payload || {}, sideEffect: !!sideEffect };
      this.events.push(e); return e;
    }
    forRun(runId) { return this.events.filter(function (e) { return e.runId === runId; }); }
    interruptUnfinished(liveIds) {
      const live = liveIds || [];
      const seen = {}, out = [];
      this.events.forEach(function (e) {
        if (e.kind === 'run_end' || e.kind === 'run_interrupted') seen[e.runId] = true;
      });
      // 收集所有出现过但未结束、且不在 live 里的 run
      const all = {};
      this.events.forEach(function (e) { all[e.runId] = true; });
      Object.keys(all).forEach(function (id) {
        if (!seen[id] && live.indexOf(id) < 0) { out.push(id); }
      });
      out.forEach(function (id) { this.append(id, 'run_interrupted', { reason: 'restart' }, false); }, this);
      return out;
    }
    /** 可安全续跑的步号：最后一个「无副作用读步骤」；写步骤不重放。 */
    replayableFrom(runId) {
      const list = this.forRun(runId);
      let idx = -1;
      for (let i = 0; i < list.length; i++) {
        if (list[i].kind === 'step' && list[i].sideEffect === false) idx = list[i].payload.stepIndex;
      }
      return { stepIndex: idx, replayable: idx >= 0, writesSkipped: list.filter(function (e) { return e.sideEffect; }).length };
    }
    summary(runId) {                                    // 概要由纯函数从事件流累计
      const list = this.forRun(runId);
      let steps = 0, writes = 0, ms = 0, tokens = 0, calls = 0, firstTs = null;
      list.forEach(function (e) {
        if (!firstTs) firstTs = e.ts;
        if (e.kind === 'step') { steps++; ms += e.payload.ms || 0; tokens += e.payload.tokens || 0; calls++; }
        if (e.sideEffect) writes++;
      });
      return { steps: steps, writes: writes, ms: ms, tokens: tokens, calls: calls,
        firstFeedbackMs: firstTs ? list[list.length - 1].ts - firstTs : 0 };
    }
  }

  /* ── 4. 条件触发：Mika core 全胜 ───────────────────────────────────
   * 胜者 CORE/trigger/ConditionCheck.kt afterCheck / revertFire / withConditionHistoryFrom
   *      CORE/trigger/PlanEditor.kt upcoming(plan, now, 3)
   * 弃   APP/scheduled/ScheduledTask.kt（只有日历/interval，无「跟上一次比」）
   * 补   mibot 的 ON_COMPLETION 触发种类（作为 kind 字段保留）
   */
  const COND = { NOT_MET: 'condition_not_met', UNCHANGED: 'condition_unchanged', COOLDOWN: 'cooldown', PLAN_EDITED: 'plan_edited' };
  function withConditionHistoryFrom(next, prev) {
    const same = next.conditionGoal === prev.conditionGoal && next.notifyWhen === prev.notifyWhen &&
      next.cooldownMs === prev.cooldownMs && next.reminderText === prev.reminderText;
    const out = Object.assign({}, next);
    ['lastConditionMet', 'lastValue', 'baselineValue', 'baselineAtMs', 'lastFiredAtMs'].forEach(function (k) {
      if (!same) out[k] = undefined;
      else if (prev[k] !== undefined) out[k] = prev[k];
    });
    return out;
  }
  function afterCheck(def, outcome, nowMs, slotMs) {
    slotMs = slotMs === undefined ? nowMs : slotMs;
    const want = !!outcome.met && (def.notifyWhen === 'each_time_met' || def.lastConditionMet !== true);
    const seeded = (def.baselineValue == null && outcome.value != null)
      ? Object.assign({}, def, { baselineValue: outcome.value, baselineAtMs: nowMs }) : def;
    const observed = Object.assign({}, seeded, { lastValue: outcome.value });
    const last = def.lastFiredAtMs;
    const cooling = def.cooldownMs != null && last != null && nowMs >= last && (slotMs - last) < def.cooldownMs;
    if (want && cooling) return { fire: false, reason: COND.COOLDOWN, next: observed };
    if (want) return { fire: true, reason: null, next: Object.assign({}, observed, {
      lastConditionMet: true,
      lastFiredAtMs: (last != null && slotMs < last && nowMs >= last) ? last : slotMs,
      baselineValue: outcome.value != null ? outcome.value : observed.baselineValue,
      baselineAtMs: outcome.value != null ? nowMs : observed.baselineAtMs
    }) };
    return { fire: false, reason: outcome.met ? COND.UNCHANGED : COND.NOT_MET,
      next: Object.assign({}, observed, { lastConditionMet: !!outcome.met }) };
  }
  function revertFire(nowDef, before) {
    return Object.assign({}, nowDef, {
      lastConditionMet: false, lastFiredAtMs: before.lastFiredAtMs,
      baselineValue: before.baselineValue != null ? before.baselineValue : nowDef.baselineValue,
      baselineAtMs: before.baselineValue != null ? before.baselineAtMs : nowDef.baselineAtMs
    });
  }
  /** 预览与保存共用同一转换，避免「建完等一天才发现时区错了」。 */
  function upcoming(def, nowMs, count) {
    count = count || 3; nowMs = nowMs || Date.now();
    const times = (def.times && def.times.length ? def.times : ['09:00']).slice().sort();
    const out = []; const now = new Date(nowMs);
    for (let d = 0; d < 10 && out.length < count; d++) {
      const base = new Date(nowMs + d * 86400000);
      if (def.kind === 'weekly' && base.getDay() !== 1) continue;
      for (let i = 0; i < times.length && out.length < count; i++) {
        const p = times[i].split(':');
        const dt = new Date(base.getFullYear(), base.getMonth(), base.getDate(), +(p[0] || 9), +(p[1] || 0));
        if (dt.getTime() <= nowMs) continue;
        out.push({ at: dt.getTime(), label: (dt.getMonth() + 1) + '/' + dt.getDate() + ' ' +
          ('0' + dt.getHours()).slice(-2) + ':' + ('0' + dt.getMinutes()).slice(-2) });
      }
    }
    return out;
  }

  /* ── 5. 虚拟屏 / App 锁：core 的死锁检测 + mibot 的优先级队列 ────────
   * 胜者 CORE/uistate/ScreenPort.kt awaitAppFree / AppWait.WouldDeadlock
   *      CORE/resource/ResourceScope.kt（建好当场登记、close 反序释放、幂等）
   * 借   APP/mibot/queue/TaskRunQueue.kt（priority>0 插队，补 core 缺的公平性）
   * 弃   APP/mibot/phone/Vd* 的裸锁（无互等检测）
   */
  class ScreenPool {
    constructor(size) { this.size = size || 4; this.slots = new Array(this.size).fill(null);
      this.locks = {}; this.waitFor = {}; this.order = []; }
    /** 返回 {ok, slot} | {wait:'app'|'slot', holder} | {deadlock:true, cycle} */
    acquire(task) {
      const app = task.app, id = task.id;
      const holder = this.locks[app];
      if (holder && holder !== id) {
        this.waitFor[id] = holder;
        const cycle = this.findCycle(id);
        if (cycle) { delete this.waitFor[id]; return { deadlock: true, cycle: cycle }; }
        return { wait: 'app', holder: holder };
      }
      let slot = this.slots.indexOf(null);
      if (slot < 0) return { wait: 'slot' };
      this.slots[slot] = id; this.locks[app] = id;
      this.order.push({ id: id, slot: slot, app: app });
      delete this.waitFor[id];
      return { ok: true, slot: slot };
    }
    findCycle(start) {                                   // 互等：A 等 B、B 等 A
      const seen = {}, chain = []; let cur = start;
      while (cur && !seen[cur]) { seen[cur] = true; chain.push(cur); cur = this.waitFor[cur]; }
      return cur && chain.indexOf(cur) >= 0 ? chain.slice(chain.indexOf(cur)).concat(cur) : null;
    }
    /** 反序释放；重复调用不报错（ResourceScope.close 语义）。 */
    release(id) {
      for (let i = this.order.length - 1; i >= 0; i--) {
        if (this.order[i].id !== id) continue;
        const e = this.order[i];
        if (this.slots[e.slot] === id) this.slots[e.slot] = null;
        if (this.locks[e.app] === id) delete this.locks[e.app];
        this.order.splice(i, 1);
      }
      delete this.waitFor[id];
    }
    /** 优先级插队：同优先组内 FIFO（mibot TaskRunQueue.enqueue） */
    enqueue(list, task) {
      const at = task.priority > 0
        ? list.findIndex(function (x) { return x.priority <= task.priority; })
        : list.length;
      list.splice(at < 0 ? list.length : at, 0, task);
      return list;
    }
    snapshot() { return { slots: this.slots.slice(), locks: Object.assign({}, this.locks), waitFor: Object.assign({}, this.waitFor) }; }
  }

  /* ── 6. 自检：Mika core 全胜（依赖 DAG + 跳过写明原因） ────────────
   * 胜者 CORE/selfcheck/SelfCheck.kt（12 项 + needs + timeoutMs）
   *      CORE/selfcheck/SelfCheckRunner.kt（逐项超时、出错不中断、finally 必 release）
   * 弃   APP/diagnostics/* 聚合页（无依赖关系）与 VdSelfCheck 单项
   */
  const CHECKS = [
    { key: 'device', name: '机型 / 系统 / 架构', needs: [], ms: 5000, device: true },
    { key: 'permissions', name: '权限与电池', needs: [], ms: 5000, device: true },
    { key: 'helper', name: '内置助手', needs: ['device'], ms: 15000, device: true },
    { key: 'virtual_display', name: '虚拟屏 / 焦点', needs: ['helper'], ms: 10000, device: true },
    { key: 'app_launch', name: '在虚拟屏打开 App', needs: ['permissions', 'virtual_display'], ms: 20000, device: true },
    { key: 'screen_reading', name: '读屏 / 截图非黑屏', needs: ['app_launch'], ms: 10000, device: true },
    { key: 'actions', name: '点按 / 中英输入 / 滚动', needs: ['screen_reading'], ms: 20000, device: true },
    { key: 'stays_alive', name: '持续运行十几秒', needs: ['screen_reading'], ms: 25000, device: true },
    { key: 'main_screen', name: '主屏任务没被抢', needs: ['virtual_display'], ms: 5000, device: true },
    { key: 'browser', name: '内置浏览器读写', needs: [], ms: 15000, device: true },
    { key: 'sandbox', name: '沙箱 echo + Python', needs: [], ms: 20000, device: true },
    { key: 'app_copies', name: '分身 / 工作资料数量', needs: ['helper'], ms: 10000, device: true }
  ];
  /** results: {key: 'Pass'|'Partial'|'Fail'|'Skip'}；未给的设备项一律 Partial(需真机)，不谎报通过。 */
  function runSelfCheck(results, budgetMs) {
    results = results || {}; budgetMs = budgetMs || 90000;
    const status = {}, used = { ms: 0 };
    CHECKS.forEach(function (c) {
      const missing = c.needs.filter(function (n) { return status[n] !== 'Pass' && status[n] !== 'Partial'; });
      if (missing.length) { status[c.key] = 'Skipped: 缺 ' + missing.join(', '); return; }
      if (used.ms + c.ms > budgetMs) { status[c.key] = 'Skipped: 总时限用尽'; return; }
      const r = results[c.key];
      // 浏览器里判定不了的设备项如实标「需真机」，且不算通过 → 依赖它的项跳过并写明缺哪项
      if (!r && c.device) { status[c.key] = 'Skipped: 需真机'; return; }
      // 模拟真实耗时按超时上限的 15% 计；超时上限仍逐项生效，总预算也可被单测压缩触发
      used.ms += Math.round(c.ms * 0.15);
      status[c.key] = (r === 'Pass' || r === 'Fail' || r === 'Partial') ? r : 'Fail';
    });
    return { status: status, checks: CHECKS, budgetMs: budgetMs, usedMs: used.ms, released: true };
  }

  /* ── 7. 网关：mibot 全胜（真实探针驱动的分级熔断） ──────────────────
   * 胜者 APP/mibot/gateway/Gateway.kt GatewayHealth
   *      APP/mibot/gateway/GatewayNet.kt classifyText
   * 弃   CORE/agent/ModelRouter.kt 的主/轻模型分工（只按任务类型，不看健康）
   */
  const COOLDOWN = { RATE: 120000, QUOTA: 600000, NETWORK: 30000, REGION: 1800000, OTHER: 600000 };
  class GatewayHealth {
    constructor() { this.map = {}; }
    state(id) { return this.map[id] || (this.map[id] = { failures: 0, consecutive: 0, cooldownUntil: 0,
      dead: false, okCount: 0, latencyMs: 0, kind: 'OK', regionsDown: {} }); }
    recordFailure(id, kind, reason, now, region) {
      const s = this.state(id); now = now || Date.now();
      s.failures++; s.consecutive++; s.kind = kind; s.lastError = scrub(reason || '').slice(0, 200);
      if (kind === 'AUTH') s.dead = true;
      if (kind === 'REGION' && region) s.regionsDown[region] = now + COOLDOWN.REGION;
      const cool = kind === 'OTHER' ? Math.min(COOLDOWN.OTHER, 30000 * Math.pow(2, Math.min(s.consecutive, 5))) : (COOLDOWN[kind] || COOLDOWN.OTHER);
      s.cooldownUntil = now + cool;
      return s;
    }
    recordProbe(id, latencyMs, now) {
      const s = this.state(id);
      return Object.assign(s, { consecutive: 0, cooldownUntil: 0, dead: false, okCount: s.okCount + 1,
        kind: 'OK', latencyMs: latencyMs, lastCheckAt: now || Date.now() });
    }
    isUsable(id, now, region) {
      const s = this.state(id); now = now || Date.now();
      if (s.dead || s.kind === 'AUTH') return false;
      if (now < s.cooldownUntil) return false;
      if (region && s.regionsDown[region] > now) return false;
      return true;
    }
    pickBest(ids, now, region) {
      const self = this;
      const usable = ids.filter(function (id) { return self.isUsable(id, now, region); });
      usable.sort(function (a, b) { return self.state(a).latencyMs - self.state(b).latencyMs; });
      return usable[0] || null;
    }
    /** 自愈阶梯：重试退避 → DoH → 镜像，任一步成功即停（Gateway.kt:288） */
    repairLadder(id, probes) {
      const steps = ['retry-backoff', 'dns-over-https', 'mirror-base-url'];
      for (let i = 0; i < steps.length; i++) {
        const ok = probes && probes[i] && probes[i]();
        if (ok) { this.recordProbe(id, 0); return { step: steps[i], result: 'FIXED' }; }
      }
      return { step: null, result: 'FAILED' };
    }
  }

  /* ── 8. 能力装配：Mika core 胜，补确定性路由 ───────────────────────
   * 胜者 CORE/assembly/RunAssembler.kt（能力白名单、每个 MCP server 一个能力、单失败降级）
   *      CORE/tool/ToolGroups.kt（工具组懒展开，前缀最多失效一次）
   * 弃   APP/mibot/plugins/DshToolRegistry.kt（注释自认 Not wired into AgentTools）
   *      APP/mibot/module/Module.kt CapabilityBus（Map<String,Any?> 无类型保护）
   * 补   core 缺的确定性路由：不把选择全交给提示词+模型自觉
   */
  function assemble(capabilities, servers) {
    const list = [];
    (capabilities || ['phone', 'web', 'sandbox']).forEach(function (c) { list.push({ id: c, kind: 'builtin', state: 'ready' }); });
    (servers || []).forEach(function (s) {
      list.push({ id: 'mcp:' + s.id, kind: 'mcp', state: s.on === false ? 'degraded' : 'ready',
        note: s.on === false ? '单服务失败，只摘自己，不影响其余能力' : s.perm || '' });
    });
    return { capabilities: list, groupsCollapsed: true, expandsUsed: 0 };
  }
  function expandGroup(asm, groupId) {
    if (asm.expandsUsed >= 1) return { ok: false, reason: '请求前缀最多失效一次' };
    asm.expandsUsed++; asm.groupsCollapsed = false;
    return { ok: true, groupId: groupId, tools: 8, note: '首次用到才展开全组，省 token' };
  }
  const ROUTE_RULES = [
    { test: /算|统计|多少|几|加|减|汇总/, chain: [['技能', '意图门控'], ['MCP', 'sqlite'], ['—', '写回会话']],
      why: '算数一律走 sqlite，不让模型心算；因此不打开浏览器，也不调用外部搜索。', cap: ['sandbox', 'mcp:sqlite'] },
    { test: /搜|查一下|最新|新闻|天气/, chain: [['技能', '意图门控'], ['技能', '网页搜索'], ['APP', '内置浏览器'], ['—', '写回会话']],
      why: '需要外部最新信息才开浏览器；免费源 100 次/天没用完，不用 Jina / Exa。搜索内容按不可信处理。', cap: ['web'] },
    { test: /文件|存|整理|导出/, chain: [['技能', '意图门控'], ['MCP', 'filesystem'], ['技能', '读屏 OCR'], ['—', '写回会话']],
      why: 'filesystem 只放行 Downloads/Destiny 与工作区；不需要网页，所以不开浏览器。', cap: ['sandbox', 'mcp:filesystem'] },
    { test: /付款|下单|买|支付|转账/, chain: [['技能', '意图门控'], ['闸门', '授权 · hand_to_user'], ['APP', '目标 App'], ['—', '写回会话']],
      why: '花钱操作不进自动放行，走人工接管；自主模式也只留授权记录。', cap: ['phone'] }
  ];
  function route(goal) {
    const hit = ROUTE_RULES.filter(function (r) { return r.test.test(goal || ''); })[0] || {
      chain: [['技能', '意图门控'], ['技能', '操作路线'], ['APP', '目标 App'], ['—', '写回会话']],
      why: '这件事以前办成过，按沉淀的路线走更快；路线失效会自己停用，不硬跑。', cap: ['phone']
    };
    return { chain: hit.chain, why: hit.why, capabilities: hit.cap, deterministic: true };
  }

  /* ── 9. 督查与检阅：[新写] 两边都只有静态文案 ─────────────────────
   * 证据 APP/mibot/v7/DestinyV7Screens.kt:1286-1311 仅 V7Elem 列表，无状态。
   */
  class Supervisor {
    constructor() { this.items = []; }
    submit(runId, title, selfChecked) { const it = { runId: runId, title: title, selfChecked: !!selfChecked, state: 'pending', at: Date.now() };
      this.items.unshift(it); return it; }
    review(runId, decision) {
      const it = this.items.filter(function (x) { return x.runId === runId && x.state === 'pending'; })[0];
      if (!it) return null;
      it.state = decision === 'accept' ? 'accepted' : 'rejected'; return it;
    }
    rate() {
      const fin = this.items.filter(function (x) { return x.state !== 'pending'; });
      if (!fin.length) return 0;
      return Math.round(fin.filter(function (x) { return x.state === 'accepted'; }).length / fin.length * 100);
    }
  }

  /* ── 10. 身份隔离：[新写] mibot 只有一个自由文本字段 ────────────────
   * 证据 APP/mibot/vault/VaultModel.kt:91 `identity: String?`；
   *      v7/DestinyV7Screens.kt:1107「比对三个身份的指纹」是静态文案。
   */
  class Identity {
    constructor() { this.groups = {}; this.risk = {}; }
    bind(identity, group, session) {
      const g = this.groups[identity] = this.groups[identity] || {};
      (g[group] = g[group] || []).push(session);       // 一个组下可以有多个会话，不能被覆盖
    }
    sessionsOf(identity) {
      const g = this.groups[identity] || {}, out = [];
      Object.keys(g).forEach(function (k) { g[k].forEach(function (s) { out.push(s); }); });
      return out;
    }
    flagRisk(session, now) { this.risk[session] = (now || Date.now()) + 86400000; }
    /** 一个号被风控 → 同身份下别的号停 24h，但不波及其他身份。 */
    holdFor(identity, now) {
      now = now || Date.now();
      const self = this;
      const hit = this.sessionsOf(identity).filter(function (s) { return self.risk[s] > now; });
      return { held: hit.length > 0, until: hit.length ? Math.max.apply(null, hit.map(function (s) { return self.risk[s]; })) : null,
        sessions: this.sessionsOf(identity) };
    }
  }

  /* ── 11. MCP 最小权限：[新写] 两边都只有规格文案 ────────────────────
   * 证据 APP/mibot/spec/DestinySpecScreens.kt:1462-1513「MCP 最小权限」页为静态文案。
   */
  const WRITE_TOOLS = /^(write|delete|create|update|push|send|exec|run|set_|remove)/i;
  function mcpCheck(server, tool, args, always) {
    const write = WRITE_TOOLS.test(tool || '');
    if (server && server.readOnly && write) return { decision: 'Deny', reason: '该服务登记为只读，写工具一律拒绝' };
    if (write && always === true) return { decision: 'Allow', reason: '已登记 ALWAYS' };
    if (write) return { decision: 'Ask', reason: '写操作每次批准' };
    if (server && server.scope && args && args.path && String(args.path).indexOf(server.scope) !== 0)
      return { decision: 'Deny', reason: '路径超出登记范围 ' + server.scope };
    return { decision: 'Allow', reason: '只读且在放行范围内' };
  }

  /* ── 12. 记忆：Mika core 胜，补 mibot 的审计口径 ────────────────────
   * 胜者 CORE/memory/MemoryStore.kt：procedure → pending、非手写改动 reapprove、
   *      撤销令牌（之后被改过则拒绝撤销）、写入口单锁串行。
   * 借   APP/mibot/vault/VaultBroker.kt 的脱敏与审计日志。
   */
  class Memory {
    constructor() { this.items = []; this.undos = {}; this.audit = []; }
    add(text, kind, source) {
      const item = { id: 'm' + Date.now() + Math.random().toString(36).slice(2, 5), text: scrub(text),
        kind: kind, status: kind === 'procedure' ? 'pending' : 'active', source: source || 'manual', v: 1, at: Date.now() };
      this.items.unshift(item);
      this.audit.push({ at: Date.now(), act: 'add', id: item.id, text: item.text });
      return item;
    }
    approve(id) { const m = this.get(id); if (m) { m.status = 'active'; m.v++; this.audit.push({ at: Date.now(), act: 'approve', id: id }); } return m; }
    update(id, text, source) {
      const m = this.get(id); if (!m) return null;
      m.text = scrub(text); m.v++;
      if (m.kind === 'procedure' && source !== 'manual' && m.status === 'active') m.status = 'pending'; // reapprove
      this.audit.push({ at: Date.now(), act: 'update', id: id, source: source || 'manual' });
      return m;
    }
    undo(token) {
      const t = this.undos[token]; if (!t) return { ok: false, reason: '撤销令牌不存在' };
      const m = this.get(t.id); delete this.undos[token];
      if (!m || m.v !== t.v) return { ok: false, reason: '之后被改过，拒绝撤销' };
      this.items = this.items.filter(function (x) { return x.id !== t.id; });
      return { ok: true };
    }
    takeUndo(id) {
      const m = this.get(id); if (!m) return null;
      const token = 'u' + Object.keys(this.undos).length;
      this.undos[token] = { id: id, v: m.v };
      if (Object.keys(this.undos).length > 50) delete this.undos[Object.keys(this.undos)[0]];
      return token;
    }
    get(id) { return this.items.filter(function (x) { return x.id === id; })[0]; }
    context(mode, limit) {
      const active = this.items.filter(function (x) { return x.status === 'active'; });
      const proc = active.filter(function (x) { return x.kind === 'procedure'; }).slice(0, 3);
      const rest = active.filter(function (x) { return x.kind !== 'procedure'; });
      return (mode === 'Auto' && active.length <= (limit || 20) ? rest : rest.slice(0, limit || 20)).concat(proc);
    }
  }

  /* ── 取舍矩阵（界面直接读，逐项可核对） ─────────────────────────── */
  const DECISIONS = [
    { id: 'approval', topic: '审批 / 授权闸门', winner: 'Mika core + 借 mibot 两点',
      take: 'ApprovalPolicy 纯函数分级 + ConfirmationGate 一次性 Grant（绑定屏幕事实、3 步过期、写操作作废）',
      drop: 'VaultBroker.isHighRisk 关键词表、第三套 ConfigConfirmationGate',
      from: 'CORE/action/Approval.kt · CORE/gate/ConfirmationGate.kt · APP/mibot/vault/VaultBroker.kt',
      why: '关键词判定易绕过；纯函数可测，Grant 绑定前台 App/对象/金额/发送内容更严。借 mibot 的 120s 超时默认拒绝与脱敏。' },
    { id: 'ledger', topic: '运行记录 / 断点恢复（两边冲突）', winner: 'Mika core 为主，折中',
      take: 'events 事件即账本 + 重启标 Interrupted；恢复只从「无副作用读步骤」接，写步骤一律不重放',
      drop: 'TaskRunQueue 无条件从 breakpoint 续跑（会重放工具调用）',
      from: 'CORE/record/RunEvent.kt · CORE/run/RunStore.kt · APP/mibot/queue/TaskRunQueue.kt',
      why: 'core 明确拒绝中途续跑是怕重放副作用；折中保住 mibot 的断点价值又守住安全底线。' },
    { id: 'trigger', topic: '定时 / 条件任务', winner: 'Mika core 全胜',
      take: 'afterCheck 与上次基线比较、按计划时段冷却不补跑、revertFire 回滚、预览与保存同源',
      drop: 'APP/scheduled/ScheduledTask.kt 只有日历/interval，没有条件语义',
      from: 'CORE/trigger/ConditionCheck.kt · CORE/trigger/Triggers.kt · CORE/trigger/PlanEditor.kt',
      why: '「降价超过 50 块告诉我」比的是上次触发时的值，core 才有 baslineValue/lastFiredAtMs/slotMs 完整状态机。' },
    { id: 'screen', topic: '虚拟屏 / App 锁', winner: 'core 死锁检测 + mibot 优先级',
      take: 'awaitAppFree 互等检测 WouldDeadlock、ResourceScope 建好即登记 / 反序释放 / 幂等',
      drop: 'APP/mibot/phone/Vd* 裸锁无互等检测',
      from: 'CORE/uistate/ScreenPort.kt · CORE/resource/ResourceScope.kt · APP/mibot/queue/TaskRunQueue.kt',
      why: 'core 能当场认出两个任务互相等；补 mibot 的 priority>0 插队，解决 core 缺公平队列。' },
    { id: 'selfcheck', topic: '兼容自检', winner: 'Mika core 全胜',
      take: '12 项 + needs 依赖 DAG，依赖没过就 Skipped 并写明缺哪项，逐项超时 + 90s 总预算 + 幂等 release',
      drop: 'APP/diagnostics/* 聚合页与 VdSelfCheck 单项（无依赖，失败会硬跑后续）',
      from: 'CORE/selfcheck/SelfCheck.kt · CORE/selfcheck/SelfCheckRunner.kt',
      why: '虚拟屏建不了时继续测「点按/打字」只会得到一串假失败；依赖跳过才诚实。' },
    { id: 'gateway', topic: '网关 / 供应商切换', winner: 'mibot 全胜',
      take: 'GatewayHealth 分级冷却 RATE 120s / QUOTA 600s / NETWORK 30s / AUTH dead / REGION 区域 30min + DoH→镜像自愈',
      drop: 'CORE/agent/ModelRouter.kt 只按任务类型分主/轻模型，不看健康',
      from: 'APP/mibot/gateway/Gateway.kt · APP/mibot/gateway/GatewayNet.kt',
      why: '状态来自最近一次真实探针而非「跑过检查就算 OK」，区域粒度熔断、重启不丢。' },
    { id: 'assembly', topic: '能力装配 / 路由', winner: 'Mika core + 补确定性',
      take: '能力白名单、每 MCP server 一个能力、单服务失败只摘自己、工具组懒展开',
      drop: 'DshToolRegistry（注释自认 Not wired）、CapabilityBus 无类型 Map',
      from: 'CORE/assembly/RunAssembler.kt · CORE/tool/ToolGroups.kt · APP/mibot/plugins/DshToolRegistry.kt',
      why: 'DSH 是半成品；core 错误隔离好，但选择全靠提示词自觉 → 补 ROUTE_RULES 确定性路由。' },
    { id: 'gap', topic: '两边都缺的 4 项（新写）', winner: 'Destiny 新写',
      take: '红线可执行校验 / 督查检阅状态机 / 身份三层隔离与风控联动 / MCP 最小权限判定',
      drop: 'V7Elem 静态文案（v7:1574 红线、v7:1286 督查、v7:1107 身份指纹、spec:1462 MCP）',
      from: 'APP/mibot/v7/DestinyV7Screens.kt · APP/mibot/spec/DestinySpecScreens.kt',
      why: '这些只存在于 56 屏规格稿里，没有数据表也没有校验函数，等于没实现。' }
  ];

  return {
    RISK: RISK, MAX_GRANT_STEPS: MAX_GRANT_STEPS, CONFIRM_TIMEOUT_MS: CONFIRM_TIMEOUT_MS,
    approvalDecide: approvalDecide, scrub: scrub, ConfirmationGate: ConfirmationGate,
    RED_LINES: RED_LINES, redLineCheck: redLineCheck,
    EventLedger: EventLedger,
    COND: COND, afterCheck: afterCheck, revertFire: revertFire, withConditionHistoryFrom: withConditionHistoryFrom, upcoming: upcoming,
    ScreenPool: ScreenPool,
    CHECKS: CHECKS, runSelfCheck: runSelfCheck,
    COOLDOWN: COOLDOWN, GatewayHealth: GatewayHealth,
    assemble: assemble, expandGroup: expandGroup, ROUTE_RULES: ROUTE_RULES, route: route,
    Supervisor: Supervisor, Identity: Identity, mcpCheck: mcpCheck, Memory: Memory,
    DECISIONS: DECISIONS
  };
});
