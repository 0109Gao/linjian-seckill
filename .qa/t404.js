const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 } })
  const lg = await fetch('http://localhost/api/auth/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:'admin',password:'123456'}) }).then(r=>r.json())

  for (const [label, url, needAuth] of [
    ['user/login', 'http://localhost/user/login', false],
    ['user/activities', 'http://localhost/user/activities', false],
    ['admin/login', 'http://localhost/admin/login', false],
    ['admin/dashboard', 'http://localhost/admin/dashboard', true],
  ]) {
    const p = await ctx.newPage()
    const bad = []
    const msgs = []
    p.on('response', r => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`) })
    p.on('requestfailed', r => bad.push(`FAILED ${r.url()}`))
    p.on('console', m => { if (m.type()==='error') msgs.push(m.text().slice(0,200)) })
    await p.goto(url, { waitUntil: 'domcontentloaded' })
    if (needAuth) {
      await p.evaluate(([t,r])=>{localStorage.setItem('accessToken',t);localStorage.setItem('refreshToken',r)},[lg.data.accessToken, lg.data.refreshToken])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
    }
    await p.waitForTimeout(2500)
    console.log(`\n===== ${label} =====`)
    console.log('失败请求:', bad.length ? bad.join(' | ') : '(无)')
    console.log('控制台错误:', msgs.length ? msgs.join(' || ') : '(无)')
    await p.close()
  }
  await b.close()
})()
