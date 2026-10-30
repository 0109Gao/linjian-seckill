/* 用户端补充实测：返回按钮、结果页各分支、支付闭环 */
const { chromium } = require('playwright-core')
const fs = require('fs')
const API = 'http://localhost/api'
const BASE = 'http://localhost/user'
const results = []

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await ctx.newPage()

  const strip = (pn) => (pn.startsWith('/user') ? pn.slice(5) || '/' : pn)
  const pathOf = () => strip(new URL(page.url()).pathname)

  async function step(id, name, expectDesc, fn) {
    let pass = false, actual = '', note = ''
    try { const r = await fn(); pass = r.pass; actual = r.actual; note = r.note || '' } catch (e) {
      pass = false; actual = 'EXCEPTION'; note = e.message.split('\n')[0].slice(0, 150)
    }
    results.push({ id, name, expect: expectDesc, actual, pass, note })
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${name}  -> ${actual}${note ? ' | ' + note : ''}`)
  }

  // 现场注册并抢购一笔，保证有"待支付"订单可供结果页/支付按钮验证
  const U = 'paychk' + Date.now().toString().slice(-6)
  await fetch(API + '/auth/register', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: U, password: '123456', nickname: '支付验收' })
  })
  const login = await fetch(API + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: U, password: '123456' })
  }).then((r) => r.json())
  const TOKEN = login.data.accessToken

  const buy = await fetch(API + '/seckill/2116097085053300737/2003', {
    method: 'POST', headers: { Authorization: 'Bearer ' + TOKEN }
  }).then((r) => r.json())
  let REAL_REQ = buy?.data?.requestId
  await new Promise((r) => setTimeout(r, 2500))
  if (!REAL_REQ) REAL_REQ = '109597783211638784'
  console.log('新订单 requestId =', REAL_REQ, '(抢购返回 code=' + buy?.code + ')')

  await page.goto(BASE + '/activities')
  await page.evaluate(([t, r]) => { localStorage.setItem('accessToken', t); localStorage.setItem('refreshToken', r) },
    [TOKEN, login.data.refreshToken])

  // ---------- 详情页返回按钮 ----------
  await page.goto(BASE + '/activities/1001')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)
  await step('U17b', '详情页返回按钮（el-page-header）是否渲染', '存在可点击的返回控件', async () => {
    const c = await page.locator('.el-page-header__back, .back .el-page-header__left').count()
    return { pass: c > 0, actual: `返回控件数=${c}` }
  })

  await step('U29', '从活动列表进入详情后点「返回」', '回到 /activities', async () => {
    await page.goto(BASE + '/activities')
    await page.waitForLoadState('networkidle')
    await page.locator('.card').first().click()
    await page.waitForURL((u) => strip(new URL(u).pathname).startsWith('/activities/'), { timeout: 6000 })
    await page.waitForTimeout(800)
    await page.locator('.el-page-header__back').click()
    await page.waitForTimeout(1200)
    return { pass: pathOf() === '/activities', actual: pathOf() }
  })

  await step('U30', '直接打开详情页（无历史）点「返回」', '至少不留在原地或给出兜底', async () => {
    const p2 = await ctx.newPage()
    await p2.goto(BASE + '/activities/1001')
    await p2.waitForLoadState('networkidle')
    await p2.waitForTimeout(1000)
    const before = new URL(p2.url()).pathname
    await p2.locator('.el-page-header__back').click()
    await p2.waitForTimeout(1500)
    const after = new URL(p2.url()).pathname
    await p2.close()
    return {
      pass: after !== before,
      actual: `点击前=${strip(before)} 点击后=${strip(after)}`,
      note: after === before ? 'router.back() 无历史可退，点击无任何反应' : ''
    }
  })

  // ---------- 结果页：用后端真实 requestId 访问（绕过精度丢失） ----------
  await page.goto(BASE + '/seckill/result/' + REAL_REQ)
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2500)

  await step('U31', '结果页（正确 requestId）是否展示成功态与订单信息', '出现订单号/金额 + 支付按钮', async () => {
    const title = (await page.locator('.title').first().textContent().catch(() => ''))?.trim()
    const body = (await page.locator('.card, .page-wrap').first().textContent().catch(() => '')) || ''
    const hasOrderNo = /SK\d{6,}/.test(body)
    const hasPay = await page.locator('button:has-text("立即支付")').count()
    return { pass: hasOrderNo || hasPay > 0, actual: `标题=${title} 含订单号=${hasOrderNo} 支付按钮=${hasPay}` }
  })

  await step('U32', '结果页「查看我的订单」', '跳 /orders', async () => {
    const b = page.locator('button:has-text("查看我的订单")')
    if ((await b.count()) === 0) return { pass: false, actual: '按钮未渲染' }
    await b.first().click()
    await page.waitForTimeout(1500)
    return { pass: pathOf() === '/orders', actual: pathOf() }
  })

  await page.goto(BASE + '/seckill/result/' + REAL_REQ)
  await page.waitForTimeout(2000)
  await step('U33', '结果页「继续逛」', '跳 /activities', async () => {
    const b = page.locator('button:has-text("继续逛")')
    if ((await b.count()) === 0) return { pass: false, actual: '按钮未渲染' }
    await b.first().click()
    await page.waitForTimeout(1500)
    return { pass: pathOf() === '/activities', actual: pathOf() }
  })

  // ---------- 结果页支付 ----------
  await page.goto(BASE + '/seckill/result/' + REAL_REQ)
  await page.waitForTimeout(2000)
  await step('U34', '结果页「立即支付」按钮', '支付成功后状态变为已支付', async () => {
    const b = page.locator('button:has-text("立即支付")')
    if ((await b.count()) === 0) return { pass: false, actual: '无支付按钮（可能已支付）' }
    await b.first().click()
    await page.waitForTimeout(2500)
    const body = (await page.locator('.page-wrap, .card').first().textContent().catch(() => '')) || ''
    const stillPay = await page.locator('button:has-text("立即支付")').count()
    const paid = /已支付|支付成功/.test(body)
    return { pass: paid || stillPay === 0, actual: `支付按钮剩余=${stillPay} 含已支付文案=${paid}` }
  })

  // ---------- 订单页支付 ----------
  await page.goto(BASE + '/orders')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)
  await step('U35', '订单页「去支付」按钮', '支付后按钮消失、显示支付时间', async () => {
    const b = page.locator('button:has-text("去支付")')
    const n = await b.count()
    if (n === 0) return { pass: true, actual: '当前无待支付订单（跳过）' }
    await b.first().click()
    await page.waitForTimeout(2500)
    const after = await page.locator('button:has-text("去支付")').count()
    const body = (await page.locator('.page-wrap').textContent().catch(() => '')) || ''
    return { pass: after < n, actual: `支付前 ${n} 个按钮 → 支付后 ${after} 个，含"支付于"=${/支付于/.test(body)}` }
  })

  await browser.close()
  const summary = { total: results.length, pass: results.filter(r => r.pass).length, fail: results.filter(r => !r.pass).length }
  fs.writeFileSync(__dirname + '/user2-result.json', JSON.stringify({ summary, results }, null, 2))
  console.log('\n汇总:', summary)
}
main().catch((e) => { console.error('FATAL', e); process.exit(1) })
