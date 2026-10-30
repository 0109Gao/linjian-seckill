const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const p = await (await b.newContext({ viewport: { width: 1280, height: 900 } })).newPage()
  await p.goto('file:///C:/Users/gzp/Desktop/秒杀系统/项目介绍.html', { waitUntil: 'load' })
  await p.waitForTimeout(3000)
  const r = await p.evaluate(() => {
    const imgs = [...document.images]
    return {
      total: imgs.length,
      broken: imgs.filter(i => !i.complete || i.naturalWidth === 0).length,
      tables: document.querySelectorAll('table').length,
      figs: document.querySelectorAll('figure').length,
      h2: [...document.querySelectorAll('h2')].map(h => h.textContent)
    }
  })
  console.log('图片', r.total, '张，失败', r.broken, '| 表格', r.tables, '| 图注块', r.figs)
  console.log('章节:', r.h2.join(' / '))
  await b.close()
})()
