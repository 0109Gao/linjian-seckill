# -*- coding: utf-8 -*-
"""
把 README.md 转成一个自包含、可直接双击打开的项目介绍页面。
- 图片用相对路径引用 docs/screenshots/，与 README 同目录，离线可看
- 内置 CSS，不依赖任何 CDN
"""
import io
import re

import markdown

SRC = r'D:/WorkBuddy/项目/linjian-seckill/README.md'
DST = r'D:/WorkBuddy/项目/linjian-seckill/项目介绍.html'

raw = io.open(SRC, encoding='utf-8').read()

# 首行 H1 单独拿出来做页头
m = re.match(r'^#\s+(.+?)\s*\n+(.+?)\n', raw, re.S)
title = m.group(1) if m else '林间秒杀'
raw = raw[m.end():] if m else raw

html_body = markdown.markdown(
    raw,
    extensions=['tables', 'fenced_code', 'toc', 'sane_lists', 'attr_list'],
    extension_configs={'toc': {'toc_depth': '2-3'}},
)

# 图片包一层 figure，便于加圆角和说明感
html_body = html_body.replace(
    '<p><img alt="', '<figure><img alt="'
).replace('" src="', '" src="')
html_body = re.sub(r'(<figure><img[^>]*>)', r'\1', html_body)
html_body = re.sub(
    r'<figure><img([^>]*?)alt="([^"]*)"([^>]*?)>',
    lambda mm: '<figure><img%salt="%s"%s>' % (mm.group(1), mm.group(2), mm.group(3)),
    html_body,
)
# 闭合：markdown 输出的 <p><img ...></p> -> figure + figcaption
html_body = re.sub(
    r'<figure><img([^>]*)></p>',
    lambda mm: '<figure><img%s><figcaption>%s</figcaption></figure>'
    % (mm.group(1), (re.search(r'alt="([^"]*)"', mm.group(1)) or [None, ''])[1]),
    html_body,
)

CSS = """
:root{
  --bg:#ffffff; --fg:#1f2328; --muted:#59636e; --line:#d8dee4;
  --accent:#e53935; --accent-soft:#fdecea; --code-bg:#1f2328; --soft:#f6f8fa;
}
*{box-sizing:border-box}
body{
  margin:0; background:var(--bg); color:var(--fg);
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC",
              "Hiragino Sans GB","Microsoft YaHei",sans-serif;
  font-size:16px; line-height:1.75; -webkit-font-smoothing:antialiased;
}
.hero{
  background:linear-gradient(135deg,#e53935 0%,#c62828 55%,#8e1f1b 100%);
  color:#fff; padding:64px 24px 56px; text-align:center;
}
.hero h1{margin:0 0 14px; font-size:40px; letter-spacing:1px; font-weight:700}
.hero .sub{font-size:17px; opacity:.94; max-width:760px; margin:0 auto; line-height:1.8}
.hero .tags{margin-top:26px; display:flex; gap:10px; justify-content:center; flex-wrap:wrap}
.hero .tags span{
  background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.3);
  padding:6px 15px; border-radius:999px; font-size:13px; backdrop-filter:blur(4px);
}
.wrap{max-width:1000px; margin:0 auto; padding:46px 24px 100px}
h2{
  font-size:27px; margin:64px 0 20px; padding-bottom:12px;
  border-bottom:2px solid var(--line); position:relative;
}
h2::after{
  content:''; position:absolute; left:0; bottom:-2px; width:72px; height:2px;
  background:var(--accent);
}
h3{font-size:20px; margin:38px 0 14px; color:#111}
h3:has(+ p), h3{scroll-margin-top:20px}
p{margin:14px 0}
a{color:var(--accent); text-decoration:none; border-bottom:1px solid rgba(229,57,53,.28)}
a:hover{border-bottom-color:var(--accent)}
strong{color:#111; font-weight:650}
ul,ol{padding-left:26px; margin:14px 0}
li{margin:8px 0}
code{
  background:var(--soft); padding:2px 6px; border-radius:5px;
  font-family:"JetBrains Mono",Consolas,"Courier New",monospace; font-size:13.5px;
  color:#b3302c; border:1px solid var(--line);
}
pre{
  background:var(--code-bg); color:#e6edf3; padding:20px 22px; border-radius:12px;
  overflow-x:auto; font-size:13.5px; line-height:1.68; margin:20px 0;
  box-shadow:0 6px 20px rgba(0,0,0,.13);
}
pre code{background:none; border:none; color:inherit; padding:0; font-size:inherit}
table{
  border-collapse:collapse; width:100%; margin:22px 0; font-size:14.5px;
  box-shadow:0 1px 3px rgba(0,0,0,.06); border-radius:10px; overflow:hidden;
}
th{background:var(--soft); text-align:left; font-weight:650; padding:12px 15px;
   border-bottom:2px solid var(--line); white-space:nowrap}
td{padding:11px 15px; border-bottom:1px solid #eaeef2; vertical-align:top}
tr:last-child td{border-bottom:none}
tbody tr:hover{background:#fafbfc}
figure{margin:30px 0; text-align:center}
figure img{
  max-width:100%; border-radius:12px; border:1px solid var(--line);
  box-shadow:0 10px 34px rgba(17,24,39,.14); display:block; margin:0 auto;
}
figcaption{margin-top:11px; font-size:13.5px; color:var(--muted)}
blockquote{
  margin:20px 0; padding:14px 20px; background:var(--accent-soft);
  border-left:4px solid var(--accent); border-radius:0 8px 8px 0; color:#5a2b28;
}
blockquote p{margin:6px 0}
hr{border:none; border-top:1px solid var(--line); margin:44px 0}
.foot{text-align:center; color:var(--muted); font-size:13px; padding:38px 24px;
      border-top:1px solid var(--line); background:var(--soft)}
@media (max-width:720px){
  .hero h1{font-size:28px} .hero{padding:44px 18px 38px}
  .wrap{padding:32px 16px 60px} h2{font-size:22px; margin-top:44px}
  table{font-size:13px} th,td{padding:9px 10px}
}
@media print{
  .hero{background:#c62828 !important; -webkit-print-color-adjust:exact}
  pre{box-shadow:none} figure img{box-shadow:none}
}
"""

# 从 markdown 里抓一句副标题
sub = '一套可直接上线的电商秒杀系统 · C 端抢购 + B 端运营后台'
TAGS = ['Spring Boot 3.2', 'Java 21', 'Redis + Lua', 'RabbitMQ',
        'Vue 3 + Vite', 'Docker Compose', '0 超卖', '90 项自动化断言']

page = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} · 项目介绍</title>
<style>{CSS}</style>
</head>
<body>
<header class="hero">
  <h1>{title}</h1>
  <p class="sub">{sub}</p>
  <div class="tags">{''.join('<span>%s</span>' % t for t in TAGS)}</div>
</header>
<main class="wrap">
{html_body}
</main>
<footer class="foot">
  林间秒杀 · 高并发限时秒杀系统 &nbsp;|&nbsp; 项目介绍文档 &nbsp;|&nbsp;
  完整源码与文档见同级目录
</footer>
</body>
</html>
"""

io.open(DST, 'w', encoding='utf-8').write(page)
print('已生成:', DST)
print('图片 figure 数:', page.count('<figure>'))
print('表格数:', page.count('<table>'))
print('大小: %.1f KB' % (len(page.encode('utf-8')) / 1024))
