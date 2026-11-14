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
# ─────────────────────────────────────────────────────────────
set -u

PKG="com.destiny.agent"
ACT="com.destiny.agent.MainActivity"
OUT="${1:-shots}"
mkdir -p "$OUT"

A() { adb "$@"; }
S() { A shell "$@"; }

shot() {
  local name="$1"
  sleep "${2:-1.2}"
  A exec-out screencap -p > "$OUT/$name.png"
  local sz=$(wc -c < "$OUT/$name.png")
  echo "  📸 $name.png  (${sz} bytes)"
}

# 点击文本匹配的元素（uiautomator 定位，找不到就退化到坐标）
tap_text() {
  local want="$1"
  local fallback_x="${2:-}" fallback_y="${3:-}"
  S uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local coords
  coords=$(python3 - "$want" <<'PY'
import sys, re, xml.etree.ElementTree as ET
want = sys.argv[1]
try:
    tree = ET.parse('/tmp/ui.xml')
except Exception:
    print(''); sys.exit(0)
for node in tree.iter('node'):
    txt = node.get('text', '') or ''
    desc = node.get('content-desc', '') or ''
    if want in txt or want in desc:
        b = node.get('bounds', '')
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
print('')
PY
)
  if [ -n "$coords" ]; then
    echo "  👆 tap '$want' @ $coords"
    S input tap $coords
    return 0
  fi
  if [ -n "$fallback_x" ] && [ -n "$fallback_y" ]; then
    echo "  👆 tap '$want' @ fallback ($fallback_x,$fallback_y)"
    S input tap "$fallback_x" "$fallback_y"
    return 0
  fi
  echo "  ⚠️  未找到 '$want'"
  return 1
}

echo "── 等待设备就绪 ──"
A wait-for-device
for i in $(seq 1 40); do
  b=$(S getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  [ "$b" = "1" ] && break
  sleep 5
done
S input keyevent 82 >/dev/null 2>&1   # 解锁
echo "✅ 设备就绪"

echo "── 安装 APK ──"
A install -r -g apk.apk || A install -r -g apk.apk
echo "✅ 安装完成"

echo "── 启动应用 ──"
A shell am force-stop "$PKG"
S monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4

# ══════════════ 浅色模式 ══════════════
echo ""
echo "══ 浅色模式 ══"
S cmd uimode night no >/dev/null 2>&1
sleep 2
A shell am force-stop "$PKG"
sleep 1
S monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4

shot "01-login" 2

# 登录页往下滚到底（看完整布局）
S input swipe 360 900 360 300 300
shot "02-login-bottom" 1.5
S input swipe 360 300 360 900 300   # 滚回顶部

# 进入（点"先进去看看"）
tap_text "先进去看看" "360" "1150"
sleep 3
shot "03-talk-empty" 2

# 底栏：主动 / 记忆 / 云电脑 / 我的
echo "── 底栏导航 ──"
tap_text "主动" "216" "1180"
shot "04-proactive" 2

tap_text "记忆" "360" "1180"
shot "05-memory" 2

tap_text "云电脑" "504" "1180"
shot "06-cloud" 2

tap_text "我的" "648" "1180"
shot "07-me" 2

# 我的页往下滚，看设置区
S input swipe 360 900 360 300 300
shot "08-me-bottom" 1.5

# 回对话页，发一条消息看主链
tap_text "对话" "72" "1180"
sleep 2
tap_text "说一句话" "360" "1000"
S input text "mingtian_xiawu_3dian_zhengwo_kaihui"
sleep 1
S input keyevent 66   # 回车发送
sleep 3
shot "09-talk-reply" 2

# 再发一条，测行动块
tap_text "说一句话" "360" "1000"
S input text "bangwo_dingxiayuequ_shanghai_gaotie"
sleep 1
S input keyevent 66
sleep 5
shot "10-talk-action" 2.5

# ══════════════ 深色模式 ══════════════
echo ""
echo "══ 深色模式 ══"
S cmd uimode night yes >/dev/null 2>&1
sleep 3
A shell am force-stop "$PKG"
sleep 1
S monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
shot "11-login-dark" 2

tap_text "先进去看看" "360" "1150"
sleep 3
shot "12-talk-dark" 2

tap_text "主动" "216" "1180"
shot "13-proactive-dark" 2

tap_text "记忆" "360" "1180"
shot "14-memory-dark" 2

tap_text "云电脑" "504" "1180"
shot "15-cloud-dark" 2

tap_text "我的" "648" "1180"
shot "16-me-dark" 2

echo ""
echo "✅ 截图完成 → $OUT/"
ls -la "$OUT"/*.png 2>/dev/null | wc -l | xargs echo "文件数:"
