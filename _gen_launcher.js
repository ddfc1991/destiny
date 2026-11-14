/* Destiny 启动图标生成器（零依赖，Node 内置 zlib 手写 PNG）
 *
 * 设计：曜石香槟 —— 墨黑圆角方块 + 香槟金四角星。
 * 与 Compose 端的 Color.kt 保持一致：Obsidian900 #0A0A0C / Champagne #C9A227。
 *
 * 产出：
 *   android/app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png
 *   android/app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.png
 *   android-icons/ic_launcher_{48,72,96,144,192}.png  （源文件）
 *
 * 用法：node _gen_launcher.js
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const OBSIDIAN = [0x0a, 0x0a, 0x0c];
const CHAMPAGNE = [0xc9, 0xa2, 0x27];

/* ── PNG 编码 ─────────────────────────────────────────────── */

const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = -1;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}

function encodePng(width, height, rgba) {
  const stride = width * 4;
  const raw = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (stride + 1)] = 0; // filter: none
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // color type RGBA
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  return Buffer.concat([
    sig,
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

/* ── 形状 ─────────────────────────────────────────────────── */

/** 四角星顶点（相对中心，108 画布坐标）——与 adaptive 图标同一套形 */
function starPoints(size) {
  const c = size / 2;
  const k = size / 108;
  const P = (x, y) => [c + x * k, c + y * k];
  return [
    P(0, -30), P(6, -6), P(30, 0), P(6, 6),
    P(0, 30), P(-6, 6), P(-30, 0), P(-6, -6)
  ];
}

function inPolygon(px, py, poly) {
  let inside = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const xi = poly[i][0], yi = poly[i][1];
    const xj = poly[j][0], yj = poly[j][1];
    if ((yi > py) !== (yj > py) && px < ((xj - xi) * (py - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
  }
  return inside;
}

/** 圆角矩形（圆角比例 r） */
function inRoundRect(px, py, size, radius) {
  const r = radius;
  const cx = Math.min(Math.max(px, r), size - r);
  const cy = Math.min(Math.max(py, r), size - r);
  const dx = px - cx, dy = py - cy;
  return dx * dx + dy * dy <= r * r;
}

function inCircle(px, py, size) {
  const c = size / 2;
  const dx = px - c, dy = py - c;
  return dx * dx + dy * dy <= (c - 1) * (c - 1);
}

/**
 * 画一张图标
 * @param size  画布边长
 * @param round true = 圆形（ic_launcher_round），false = 圆角方块
 */
function renderIcon(size, round) {
  const rgba = Buffer.alloc(size * size * 4);
  const poly = starPoints(size);
  const SS = 4; // 超采样
  const radius = size * 0.22;

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let bgHits = 0, fgHits = 0;
      for (let sy = 0; sy < SS; sy++) {
        for (let sx = 0; sx < SS; sx++) {
          const px = x + (sx + 0.5) / SS;
          const py = y + (sy + 0.5) / SS;
          const inBg = round ? inCircle(px, py, size) : inRoundRect(px, py, size, radius);
          if (inBg) {
            bgHits++;
            if (inPolygon(px, py, poly)) fgHits++;
          }
        }
      }
      const total = SS * SS;
      const bgA = bgHits / total;
      const fgA = fgHits / total;

      // 底色（墨黑）带 alpha，星（金）覆盖
      const a = bgA;
      let r = OBSIDIAN[0], g = OBSIDIAN[1], b = OBSIDIAN[2];
      if (fgA > 0 && bgA > 0) {
        const t = fgA / bgA;                 // 星在底内的覆盖率
        r = Math.round(OBSIDIAN[0] * (1 - t) + CHAMPAGNE[0] * t);
        g = Math.round(OBSIDIAN[1] * (1 - t) + CHAMPAGNE[1] * t);
        b = Math.round(OBSIDIAN[2] * (1 - t) + CHAMPAGNE[2] * t);
      }
      const o = (y * size + x) * 4;
      rgba[o] = r; rgba[o + 1] = g; rgba[o + 2] = b;
      rgba[o + 3] = Math.round(a * 255);
    }
  }
  return encodePng(size, size, rgba);
}

/* ── 输出 ─────────────────────────────────────────────────── */

const ROOT = __dirname;
const RES = path.join(ROOT, 'android', 'app', 'src', 'main', 'res');
const ICONS = path.join(ROOT, 'android-icons');

const DENSITIES = [
  ['mdpi', 48],
  ['hdpi', 72],
  ['xhdpi', 96],
  ['xxhdpi', 144],
  ['xxxhdpi', 192]
];

function write(file, buf) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, buf);
  console.log('  →', path.relative(ROOT, file), buf.length + 'B');
}

console.log('Destiny 启动图标（曜石香槟）');
DENSITIES.forEach(([dpi, px]) => {
  const square = renderIcon(px, false);
  const round = renderIcon(px, true);
  write(path.join(RES, 'mipmap-' + dpi, 'ic_launcher.png'), square);
  write(path.join(RES, 'mipmap-' + dpi, 'ic_launcher_round.png'), round);
  write(path.join(ICONS, 'ic_launcher_' + px + '.png'), square);
});
console.log('完成。');
