const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await b.newContext({ viewport: { width: 1600, height: 950 } })
  const p = await ctx.newPage()
  p.on('pageerror', e => console.log('PAGEERROR:', e.message))
  p.on('console', m => { if (m.type()==='error') console.log('CONSOLE:', m.text().slice(0,120)) })

  const lg = await fetch('http://localhost/api/auth/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:'admin',password:'123456'}) }).then(r=>r.json())
  console.log('admin token?', !!lg?.data?.accessToken)

  await p.goto('http://localhost/admin/login', { waitUntil:'domcontentloaded' })
  await p.waitForTimeout(1500)
  console.log('登录页 URL:', p.url())
  await p.evaluate(([t,r])=>{localStorage.setItem('accessToken',t);localStorage.setItem('refreshToken',r)},[lg.data.accessToken,lg.data.refreshToken])
  console.log('注入 token 完成')

  try {
    await p.goto('http://localhost/admin/dashboard', { waitUntil:'domcontentloaded', timeout: 15000 })
    console.log('dashboard 打开 OK:', p.url())
  } catch (e) {
    console.log('dashboard 超时，当前 URL:', p.url())
  }
  await p.waitForTimeout(2000)
  console.log('最终 URL:', p.url())
  console.log('页面有内容:', (await p.evaluate(()=>document.body.innerText)).slice(0,60).replace(/\n/g,' '))
  await b.close()
})()
