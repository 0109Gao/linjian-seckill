/**
 * 项目截图脚本：2x 高清截取用户端与管理端真实页面。
 * 运行：node shot.js
 * 依赖：playwright-core + 本机 Edge
 */
const { chromium } = require('playwright-core')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve('D:/WorkBuddy/项目/linjian-seckill/docs/screenshots')
const USER = 'http://localhost/user'
const ADMIN = 'http://localhost/admin'
const ACT = '2116127895112880130'        // 双十一预热 · 咖啡豆专场（3 商品）
const ACT2 = '2116127895213543425'       // 器具焕新 · 手冲装备场
const REQ = '109612440571871232'         // 待支付的咖啡豆订单 -> 结果页

const log = (m) => console.log(m)

async function waitImg(page, timeout = 12000) {
  await page.evaluate(async (t) => {
    const imgs = [...document.images]
    await Promise.all(imgs.map((i) => i.complete ? null : new Promise((r) => {
      i.onload = i.onerror = r
      setTimeout(r, t)
    })))
  }, timeout).catch(() => {})
  await page.waitForTimeout(700)
}

async function series(browser, ctx, tasks, label) {
  for (const t of tasks) {
    const page = await ctx.newPage()
    page.on('pageerror', (e) => log(`   ! pageerror ${t.file}: ${e.message}`))
    try {
      await page.goto(t.url, { waitUntil: 'domcontentloaded', timeout: 20000 })
      await page.waitForTimeout(t.wait || 1800)
      await waitImg(page)
      if (t.before) await t.before(page)
      const file = path.join(OUT, t.file)
      await page.screenshot({
        path: file,
        fullPage: !!t.full,
        animations: 'disabled'
      })
      const kb = Math.round(fs.statSync(file).size / 1024)
      log(`  OK  ${t.file.padEnd(34)} ${kb} KB  ${page.url().replace('http://localhost', '')}`)
    } catch (e) {
      log(`  FAIL ${t.file}: ${e.message.split('\n')[0]}`)
    }
    await page.close()
  }
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const b = await chromium.launch({ channel: 'msedge', headless: true })

  // 取 token
  const login = async (u, p) => {
    const r = await fetch('http://localhost/api/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p })
    }).then((x) => x.json())
    return r.data
  }
  const u = await login('demo_linjian', '123456')
  const a = await login('admin', '123456')
  log(`token: user=${!!u.accessToken} admin=${!!a.accessToken}\n`)

  // ---------- 1. 未登录：两个登录页 ----------
  log('[未登录]')
  const anon = await b.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 })
  await series(b, anon, [
    { file: '01-user-login.png', url: `${USER}/login`, wait: 2000 },
    { file: '06-admin-login.png', url: `${ADMIN}/login`, wait: 2000 },
  ], 'anon')
  await anon.close()

  // ---------- 2. 用户端（登录态） ----------
  log('\n[用户端]')
  const uc = await b.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 })
  await uc.addInitScript(([t, r]) => {
    localStorage.setItem('accessToken', t)
    localStorage.setItem('refreshToken', r)
  }, [u.accessToken, u.refreshToken])

  await series(b, uc, [
    { file: '02-user-activity-list.png', url: `${USER}/activities`, wait: 2200, full: true },
    { file: '03-user-activity-detail.png', url: `${USER}/activities/${ACT}`, wait: 2600, full: true },
    { file: '04-user-seckill-result.png', url: `${USER}/seckill/result/${REQ}`, wait: 4000 },
    { file: '05-user-orders.png', url: `${USER}/orders`, wait: 2200, full: true },
  ], 'user')
  await uc.close()

  // ---------- 3. 管理端（独立 context，避免跨 SPA 状态污染） ----------
  log('\n[管理端]')
  const ac = await b.newContext({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 2 })
  await ac.addInitScript(([t, r]) => {
    localStorage.setItem('accessToken', t)
    localStorage.setItem('refreshToken', r)
  }, [a.accessToken, a.refreshToken])

  await series(b, ac, [
    { file: '07-admin-dashboard.png', url: `${ADMIN}/dashboard`, wait: 2600 },
    { file: '08-admin-goods.png', url: `${ADMIN}/goods`, wait: 2400, full: true },
    { file: '09-admin-activities.png', url: `${ADMIN}/activities`, wait: 2400, full: true },
    { file: '10-admin-orders.png', url: `${ADMIN}/orders`, wait: 2400, full: true },
    { file: '11-admin-stock.png', url: `${ADMIN}/stock?activityId=${ACT2}`, wait: 2400, full: true },
  ], 'admin')
  await ac.close()

  await b.close()
  log('\n全部完成 -> ' + OUT)
})()
