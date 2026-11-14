#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# Destiny 真机截图脚本
#
# 在已连接的 Android 设备/模拟器上：
#   1. 安装 APK
#   2. 逐屏操作并截图（浅色 + 深色各一套）
#   3. 输出到 shots/
#
# 元素定位用 uiautomator dump + 文本匹配，不写死坐标，
# 这样改版后脚本仍然有效。
#
# 关键：每一步都等 UI 真正响应（窗口不再是 NOT_RESUMED 且
# 无 ANR 对话框），否则会截到同一张冻结画面。
# ─────────────────────────────────────────────────────────────
set -u

PKG="com.destiny.agent"
OUT="${1:-shots}"
mkdir -p "$OUT"

A() { adb "$@"; }
S() { A shell "$@"; }

log() { echo "  $1"; }

# ── 当前前台窗口 ──
focused() {
  S dumpsys window 2>/dev/null | grep -m1 -oE 'mCurrentFocus=[^ ]*' | cut -d= -f2
}

# ── ANR / 无响应弹窗检测 ──
has_anr() {
  S uiautomator dump /sdcard/anr.xml >/dev/null 2>&1
  A pull /sdcard/anr.xml /tmp/anr.xml >/dev/null 2>&1
  grep -qiE "isn.t responding|not responding|ANR" /tmp/anr.xml 2>/dev/null && return 0
  # 系统 ANR 弹窗也可能在别的包里
  S dumpsys activity processes 2>/dev/null | grep -q "ANR in $PKG" && return 0
  return 1
}

# ── 等应用就绪：前台窗口属于本应用 + 无 ANR ──
wait_ready() {
  local tries="${1:-25}"
  for i in $(seq 1 "$tries"); do
    local f
    f=$(focused)
    if echo "$f" | grep -q "$PKG"; then
      if ! has_anr; then
        return 0
      fi
      log "⚠️  检测到 ANR 对话框，尝试关掉"
      S input keyevent KEYCODE_BACK >/dev/null 2>&1
    fi
    sleep 1
  done
  log "⚠️  等待超时（前台=${f:-unknown}）"
  return 1
}

shot() {
  local name="$1"
  local wait="${2:-1.5}"
  sleep "$wait"
  A exec-out screencap -p > "$OUT/$name.png" 2>/dev/null
  local sz=$(wc -c < "$OUT/$name.png" 2>/dev/null || echo 0)
  # 去重：跟上一张完全一样说明界面没变，标注出来
  local h=$(md5sum "$OUT/$name.png" 2>/dev/null | cut -c1-8)
  local prev=""
  [ -f /tmp/.last_shot_hash ] && prev=$(cat /tmp/.last_shot_hash)
  local mark=""
  [ "$h" = "$prev" ] && mark="  ⚠️  与上一张完全相同（界面可能没切换）"
  echo "$h" > /tmp/.last_shot_hash
  echo "  📸 $name.png  (${sz} bytes, $h)$mark"
}

# 点击文本匹配的元素（uiautomator 定位，找不到就退化到坐标）
tap_text() {
  local want="$1"
  local fallback_x="${2:-}" fallback_y="${3:-}"
  local coords=""
  for attempt in 1 2; do
    S uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    A pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
    coords=$(python3 - "$want" <<'PY'
import sys, re, xml.etree.ElementTree as ET
want = sys.argv[1]
try:
    tree = ET.parse('/tmp/ui.xml')
except Exception:
    print(''); sys.exit(0)
best = None
for node in tree.iter('node'):
    txt = node.get('text', '') or ''
    desc = node.get('content-desc', '') or ''
    if txt == want or desc == want:
        best = node; break
    if best is None and (want in txt or want in desc):
        best = node
if best is not None:
    b = best.get('bounds', '')
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
print('')
PY
)
    [ -n "$coords" ] && break
    sleep 1
  done
  if [ -n "$coords" ]; then
    log "👆 tap '$want' @ $coords"
    S input tap $coords
    return 0
  fi
  if [ -n "$fallback_x" ] && [ -n "$fallback_y" ]; then
    log "👆 tap '$want' @ fallback ($fallback_x,$fallback_y)"
    S input tap "$fallback_x" "$fallback_y"
    return 0
  fi
  log "⚠️  未找到 '$want'"
  return 1
}

# 底栏导航：直接按屏幕宽度均分五格，坐标稳定
tap_tab() {
  local idx="$1"   # 1..5
  local w
  w=$(S wm size 2>/dev/null | grep -oE '[0-9]+' | tail -1)
  [ -z "$w" ] && w=1080
  local h
  h=$(S wm size 2>/dev/null | grep -oE '[0-9]+' | head -1)
  [ -z "$h" ] && h=1920
  local x=$(( w * idx / 6 ))
  local y=$(( h - h / 12 ))
  log "👆 tab#$idx @ ($x,$y)"
  S input tap "$x" "$y"
}

echo "── 等待设备就绪 ──"
A wait-for-device
for i in $(seq 1 60); do
  b=$(S getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  [ "$b" = "1" ] && break
  sleep 5
done
S input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
S input keyevent KEYCODE_MENU >/dev/null 2>&1
log "✅ 设备就绪"

echo "── 屏幕尺寸 ──"
S wm size
S wm density

echo "── 安装 APK ──"
A install -r -g apk.apk
log "✅ 安装完成"

# ══════════════ 浅色模式 ══════════════
echo ""
echo "══ 浅色模式 ══"
S cmd uimode night no >/dev/null 2>&1
sleep 3
A shell am force-stop "$PKG"
sleep 2
S monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
wait_ready 30

shot "01-login" 2

# 登录页往下滚（看完整布局）
S input swipe 360 900 360 250 400
shot "02-login-bottom" 2
S input swipe 360 250 360 950 400

# 进去（"先进去看看"）
tap_text "先进去看看" "360" "1150"
wait_ready 20
shot "03-talk-empty" 2.5

# ── 底栏导航 ──
echo "── 底栏导航 ──"
tap_tab 2; wait_ready 12; shot "04-proactive" 2
tap_tab 3; wait_ready 12; shot "05-memory" 2
tap_tab 4; wait_ready 12; shot "06-cloud" 2
tap_tab 5; wait_ready 12; shot "07-me" 2

S input swipe 360 900 360 250 400
shot "08-me-bottom" 2
S input swipe 360 250 360 950 400

# 回对话页，发消息看主链
tap_tab 1; wait_ready 12
tap_text "说一句话" "360" "1000"
sleep 1
S input text "mingtian_xiawu_3dian_zhengwo_kaihui"
sleep 1
S input keyevent KEYCODE_ENTER
wait_ready 20
shot "09-talk-reply" 3

tap_text "说一句话" "360" "1000"
sleep 1
S input text "bangwo_dingxiayuequ_shanghai_gaotie"
sleep 1
S input keyevent KEYCODE_ENTER
wait_ready 20
shot "10-talk-action" 4

# ══════════════ 深色模式 ══════════════
echo ""
echo "══ 深色模式 ══"
S cmd uimode night yes >/dev/null 2>&1
sleep 3
A shell am force-stop "$PKG"
sleep 2
S monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
wait_ready 30
shot "11-login-dark" 2

tap_text "先进去看看" "360" "1150"
wait_ready 20
shot "12-talk-dark" 2.5

tap_tab 2; wait_ready 12; shot "13-proactive-dark" 2
tap_tab 3; wait_ready 12; shot "14-memory-dark" 2
tap_tab 4; wait_ready 12; shot "15-cloud-dark" 2
tap_tab 5; wait_ready 12; shot "16-me-dark" 2

echo ""
echo "── 崩溃/ANR 检查 ──"
S dumpsys activity exit-info "$PKG" 2>/dev/null | head -30 || true

echo ""
echo "✅ 截图完成 → $OUT/"
ls "$OUT"/*.png 2>/dev/null | wc -l | xargs echo "文件数:"
