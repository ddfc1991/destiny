/* Destiny 本地运行入口：node serve.js → http://127.0.0.1:8788
   用 http 打开而不是双击 file://，localStorage 与 PWA 才会正常工作。 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const ROOT = __dirname;
const PORT = Number(process.env.PORT || 8788);
const MIME = {
  '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css',
  '.svg': 'image/svg+xml', '.json': 'application/json', '.webmanifest': 'application/manifest+json'
};
http.createServer(function (req, res) {
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (p === '/') p = '/index.html';
  const f = path.join(ROOT, p);
  if (!f.startsWith(ROOT) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) {
    res.writeHead(404); res.end('404'); return;
  }
  res.writeHead(200, { 'Content-Type': (MIME[path.extname(f)] || 'application/octet-stream') + '; charset=utf-8' });
  res.end(fs.readFileSync(f));
}).listen(PORT, '127.0.0.1', function () {
  console.log('Destiny running at http://127.0.0.1:' + PORT);
});
