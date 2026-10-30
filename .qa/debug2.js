const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } })

  for (const [label, url] of [['用户端', 'http://localhost/user/activities'], ['管理端', 'http://localhost/admin/login']]) {
    const p = await ctx.newPage()
    const bad = []
    p.on('response', (r) => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`) })
    p.on('requestfailed', (r) => bad.push(`FAILED ${r.url()} ${r.failure()?.errorText}`))
    p.on('pageerror', (e) => bad.push('PAGEERROR ' + e.message))
    await p.goto(url)
    await p.waitForLoadState('networkidle')
    await p.waitForTimeout(1500)
    console.log(`===== ${label} 失败请求 =====`)
    console.log(bad.length ? bad.join('\n') : '(无)')
    await p.close()
  }

  // U30 blank 确认
  console.log('\n===== U30：直接打开详情页点返回 =====')
  const p = await ctx.newPage()
  await p.goto('http://localhost/user/activities/1001')
  await p.waitForLoadState('networkidle')
  await p.waitForTimeout(1200)
  console.log('history.length =', await p.evaluate(() => history.length))
  console.log('点击前 URL =', p.url())
  await p.locator('.el-page-header__back').click()
  await p.waitForTimeout(1500)
  console.log('点击后 URL =', p.url())
  console.log('body 文本前 80 =', (await p.evaluate(() => document.body.innerText)).slice(0, 80).replace(/\n/g, ' '))
  await b.close()
})()
