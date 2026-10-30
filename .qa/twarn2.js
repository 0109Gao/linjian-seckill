const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await b.newContext({ viewport: { width: 1600, height: 950 } })
  const p = await ctx.newPage()
  const seen = []
  p.on('console', (m) => {
    if (m.type() !== 'error' && m.type() !== 'warning') return
    const l = m.location() || {}
    seen.push(`[${m.type()}] ${m.text().slice(0,120)} @ ${(l.url||'').split('/').pop()}:${l.lineNumber}`)
  })
  const lg = await fetch('http://localhost/api/auth/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:'admin',password:'123456'}) }).then(r=>r.json())
  await p.goto('http://localhost/admin/login', { waitUntil:'domcontentloaded' })
  await p.evaluate(([t,r])=>{localStorage.setItem('accessToken',t);localStorage.setItem('refreshToken',r)},[lg.data.accessToken, lg.data.refreshToken])
  for (const path of ['/dashboard','/goods','/activities','/orders','/stock']) {
    await p.goto('http://localhost/admin' + path, { waitUntil:'domcontentloaded' })
    await p.waitForTimeout(1800)
    console.log(`-- ${path}: 累计 ${seen.length} 条`)
  }
  console.log('\n===== 告警明细 =====')
  if (!seen.length) console.log('(无)')
  seen.forEach(s => console.log(s))
  await b.close()
})().catch(e => { console.log('ERR', e.message); process.exit(1) })
