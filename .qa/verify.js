/* 修复验收：逐条验证 D1~D4 是否真的修好 */
const { chromium } = require('playwright-core')
const API = 'http://localhost/api'
const U = 'http://localhost/user'
const A = 'http://localhost/admin'
const out = []
const ACT = '2116097085053300737'   // 进行中的压测专场
const P = (id, name, pass, actual, note = '') => {
  out.push({ id, name, pass, actual, note })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${name}  -> ${actual}${note ? ' | ' + note : ''}`)
}

async function main() {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await b.newContext({ viewport: { width: 1600, height: 950 } })
  const page = await ctx.newPage()
  const strip = (pn, pre) => (pn.startsWith(pre) ? pn.slice(pre.length) || '/' : pn)
  const upath = () => strip(new URL(page.url()).pathname, '/user')
  const apath = () => strip(new URL(page.url()).pathname, '/admin')

  // ============ V1: 接口层的 ID 精度 ============
  const lg = await fetch(API + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: '123456' })
  }).then((r) => r.json())
  const AT = lg.data.accessToken

  const ordersRaw = await (await fetch(API + '/admin/orders?pageNum=1&pageSize=1', { headers: { Authorization: 'Bearer ' + AT } })).text()
  const rq = ordersRaw.match(/"requestId":"?(\d+)"?/)?.[1] || ordersRaw.match(/"orderNo":"([^"]+)"/)?.[1]

  P('V1', '接口返回的雪花 ID 是否为精确字符串', /"id":\s*"\d{16,}"/.test(ordersRaw),
    `订单列表 id 字段片段: ${ordersRaw.match(/"id":\s*[^,]+/)?.[0] || '?'}`)

  P('V2', '分页 total 是否仍为数字（没被字符串化）', /"total":\s*\d+/.test(ordersRaw),
    `total 片段: ${ordersRaw.match(/"total":\s*[^,}]+/)?.[0] || '?'}`)

  // ============ V3: 一条完整抢购，验证前端 requestId 与 DB 一致 ============
  const uname = 'fixchk' + Date.now().toString().slice(-6)
  await fetch(API + '/auth/register', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: uname, password: '123456', nickname: '修复验收' })
  })
  const ulg = await fetch(API + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: uname, password: '123456' })
  }).then((r) => r.json())

  await page.goto(U + '/activities')
  await page.evaluate(([t, r]) => {
    localStorage.setItem('accessToken', t); localStorage.setItem('refreshToken', r)
  }, [ulg.data.accessToken, ulg.data.refreshToken])

  await page.goto(U + '/activities/' + ACT)
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1200)
  await page.locator('.goods-action button').first().click()
  await page.waitForTimeout(3500)

  const frontReq = upath().split('/').pop()
  // 直接用前端拿到的 requestId 反查结果接口：能查到订单即证明 ID 精确
  const probe = await fetch(API + '/seckill/result/' + frontReq, {
    headers: { Authorization: 'Bearer ' + ulg.data.accessToken }
  }).then((r) => r.json()).catch(() => ({}))

  P('V3', '用前端 URL 的 requestId 能查到真实订单', probe?.code === 200 && !!probe?.data?.orderNo,
    `前端 requestId=${frontReq} → 接口 code=${probe?.code} 订单号=${probe?.data?.orderNo || '无'}`)

  // 结果页应能在 10s 内出终态
  let finalTitle = ''
  for (let i = 0; i < 12; i++) {
    finalTitle = (await page.locator('.title').first().textContent().catch(() => ''))?.trim()
    if (/抢购成功|失败|抢光|限购/.test(finalTitle || '')) break
    await page.waitForTimeout(1000)
  }
  P('V4', '结果页能出终态（不再是「结果确认中」）', !/结果确认中/.test(finalTitle || ''),
    `最终标题=${finalTitle}`)

  // ============ V5: 登出后 UI 立即刷新 ============
  await page.goto(U + '/activities')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
  const beforeUser = await page.locator('.topbar .user').count()
  await page.locator('.topbar .user').hover()
  await page.waitForTimeout(700)
  await page.locator('.el-dropdown-menu__item:visible:has-text("退出登录")').click()
  await page.waitForTimeout(1500)
  const afterUser = await page.locator('.topbar .user').count()
  const afterLoginBtn = await page.locator('.topbar button:has-text("登录")').count()
  const token = await page.evaluate(() => localStorage.getItem('accessToken'))

  P('V5', '退出登录后顶部立即变回「登录/注册」', afterUser === 0 && afterLoginBtn > 0,
    `登出前 .user=${beforeUser} → 登出后 .user=${afterUser}，登录按钮=${afterLoginBtn}，token=${token ? '仍在' : '已清'}`)

  // 反向：再登录后顶部应立刻出现用户名
  await page.goto(U + '/login')
  await page.waitForLoadState('networkidle')
  await page.locator('input[placeholder="用户名"]').fill(uname)
  await page.locator('input[placeholder="密码"]').fill('123456')
  await page.locator('button:has-text("登 录")').click()
  await page.waitForTimeout(2500)
  const afterLoginUser = await page.locator('.topbar .user').count()
  P('V6', '登录后顶部立即显示用户名', afterLoginUser > 0, `.user 元素=${afterLoginUser}`)

  // ============ V7: 直接打开详情页点返回 ============
  const p2 = await ctx.newPage()
  await p2.goto(U + '/activities/' + ACT)
  await p2.waitForLoadState('networkidle')
  await p2.waitForTimeout(1200)
  await p2.locator('.el-page-header__back').click()
  await p2.waitForTimeout(1500)
  const backUrl = p2.url()
  await p2.close()
  P('V7', '直接打开详情页点「返回」不再白屏', backUrl.includes('/user/activities'),
    `返回后 URL=${backUrl.replace('http://localhost', '')}`)

  // ============ V8: 管理端（独立标签页，避免跨 SPA 状态污染） ============
  const ctx2 = await b.newContext({ viewport: { width: 1600, height: 950 } })
  const p3 = await ctx2.newPage()
  await p3.goto(A + '/login', { waitUntil: 'domcontentloaded' })
  await p3.evaluate(([t, r]) => {
    localStorage.setItem('accessToken', t); localStorage.setItem('refreshToken', r)
  }, [AT, lg.data.refreshToken])
  await p3.goto(A + '/dashboard', { waitUntil: 'domcontentloaded' })
  await p3.waitForLoadState('networkidle').catch(() => {})
  await p3.waitForTimeout(1000)
  await p3.locator('.user-info').hover()
  await p3.waitForTimeout(800)
  const [popup] = await Promise.all([
    ctx2.waitForEvent('page', { timeout: 6000 }).catch(() => null),
    p3.locator('.el-dropdown-menu__item:visible:has-text("前台商城")').click()
  ])
  let target = '(未打开)'
  let popupOk = false
  if (popup) {
    target = popup.url()
    await popup.waitForLoadState('networkidle').catch(() => {})
    const bodyLen = (await popup.evaluate(() => document.body.innerText)).length
    popupOk = bodyLen > 50
    await popup.close()
  }
  P('V8', '管理端「前台商城」打开的是可访问的用户端', popupOk,
    `打开=${target}`, target.includes('5173') ? '仍指向 5173 开发端口' : '')

  // ============ V9: 管理端订单表 ID 显示 ============
  await p3.goto(A + '/orders', { waitUntil: 'domcontentloaded' })
  await p3.waitForLoadState('networkidle').catch(() => {})
  await p3.waitForTimeout(1500)
  const cellTxt = (await p3.locator('.el-table__body tr').first().locator('td').allTextContents()).join(' | ').replace(/\s+/g, ' ')
  const raw2 = await (await fetch(API + '/admin/orders?pageNum=1&pageSize=1', { headers: { Authorization: 'Bearer ' + AT } })).text()
  const uid = raw2.match(/"userId":"?(\d+)"?/)?.[1]
  P('V9', '管理端订单表用户 ID 显示精确', !uid || cellTxt.includes(uid),
    `后端 userId=${uid} / 表格首行=${cellTxt.slice(0, 80)}`)

  await b.close()
  const s = { total: out.length, pass: out.filter((r) => r.pass).length, fail: out.filter((r) => !r.pass).length }
  console.log('\n修复验收汇总:', s)
  require('fs').writeFileSync(__dirname + '/verify-result.json', JSON.stringify({ summary: s, out }, null, 2))
}
main().catch((e) => { console.error('FATAL', e.message); process.exit(1) })
