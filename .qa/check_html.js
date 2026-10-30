const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const p = await (await b.newContext({ viewport: { width: 1280, height: 900 }, deviceScaleFactor: 1 })).newPage()
  const bad = []
  p.on('requestfailed', r => bad.push(r.url().slice(-60)))
  await p.goto('file:///D:/WorkBuddy/项目/linjian-seckill/项目介绍.html', { waitUntil: 'load' })
  await p.waitForTimeout(2500)
  const info = await p.evaluate(() => {
    const imgs = [...document.images]
    return {
      imgs: imgs.length,
      broken: imgs.filter(i => !i.complete || i.naturalWidth === 0).map(i => i.src.slice(-40)),
      bodyH: document.body.scrollHeight,
      overflowX: document.documentElement.scrollWidth > window.innerWidth + 2
    }
  })
  console.log('图片总数:', info.imgs, '| 加载失败:', info.broken.length ? info.broken : '(无)')
  console.log('页面高度:', info.bodyH, '| 横向溢出:', info.overflowX)
  console.log('资源失败:', bad.length ? bad : '(无)')
  await p.screenshot({ path: '/tmp/html_top.png' })
  // 滚到截图区域
  await p.evaluate(() => { const f = document.querySelectorAll('figure')[1]; f && f.scrollIntoView({block:'center'}) })
  await p.waitForTimeout(800)
  await p.screenshot({ path: '/tmp/html_shot.png' })
  await b.close()
})()
