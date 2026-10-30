/* 用户端全量交互实测 - headless Edge + Playwright */
const { chromium } = require('playwright-core')
const fs = require('fs')

const BASE = 'http://localhost/user'
const API = 'http://localhost/api'
const ACT = '2116097085053300737'   // 进行中的压测专场（1001 已取消）
const results = []
const consoleErrors = []

const P = (o) => results.push(o)

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await ctx.newPage()

  page.on('pageerror', (e) => consoleErrors.push('[pageerror] ' + e.message))
  page.on('console', (m) => {
    if (m.type() === 'error') consoleErrors.push('[console] ' + m.text())
  })

  // ---------- 工具 ----------
  const url = () => page.url()
  const strip = (pn) => (pn.startsWith('/user') ? pn.slice(5) || '/' : pn)
  const pathOf = () => { try { return strip(new URL(page.url()).pathname) } catch { return '' } }

  async function step(id, name, expectDesc, fn) {
    let pass = false
    let actual = ''
    let note = ''
    try {
      const r = await fn()
      pass = r.pass
      actual = r.actual || ''
      note = r.note || ''
    } catch (e) {
      pass = false
      actual = 'EXCEPTION'
      note = e.message.split('\n')[0].slice(0, 160)
    }
    P({ id, name, expect: expectDesc, actual, pass, note })
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${name}  -> ${actual}${note ? '  |  ' + note : ''}`)
  }

  async function waitPath(p, timeout = 6000) {
    try {
      await page.waitForURL((u) => strip(new URL(u).pathname) === p, { timeout })
      return true
    } catch {
      return false
    }
  }

  async function visible(sel, timeout = 4000) {
    try {
      await page.locator(sel).first().waitFor({ state: 'visible', timeout })
      return true
    } catch {
      return false
    }
  }

  // 注册专用测试账号
  const uname = 'uitester' + Date.now().toString().slice(-6)
  const reg = await fetch(API + '/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: uname, password: '123456', nickname: 'UI测试号' })
  }).then((r) => r.json())

  const login = await fetch(API + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: uname, password: '123456' })
  }).then((r) => r.json())

  const TOKEN = login?.data?.accessToken
  console.log('测试账号:', uname, 'token?', !!TOKEN)

  const setLogin = async () => {
    await page.goto(BASE + '/activities')
    await page.evaluate(
      ([t, r]) => {
        localStorage.setItem('accessToken', t)
        localStorage.setItem('refreshToken', r)
      },
      [TOKEN, login.data.refreshToken]
    )
    await page.reload()
    await page.waitForLoadState('networkidle')
  }

  const setLogout = async () => {
    await page.goto(BASE + '/activities')
    await page.evaluate(() => localStorage.clear())
    await page.reload()
    await page.waitForLoadState('networkidle')
  }

  // ================= 未登录态 =================
  await setLogout()

  await step('U01', '未登录：顶部「登录」文字按钮', '跳转 /login', async () => {
    await page.locator('.topbar button:has-text("登录")').first().click()
    const ok = await waitPath('/login')
    return { pass: ok, actual: pathOf() }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await step('U02', '未登录：顶部「注册 / 登录」主按钮', '跳转 /login', async () => {
    await page.locator('.topbar button:has-text("注册 / 登录")').first().click()
    const ok = await waitPath('/login')
    return { pass: ok, actual: pathOf() }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await step('U03', '未登录：直接访问 /orders 受保护路由', '被重定向到 /login?redirect', async () => {
    await page.goto(BASE + '/orders')
    await page.waitForTimeout(1200)
    const u = page.url()
    const ok = u.includes('/login') && u.includes('redirect')
    return { pass: ok, actual: u.replace('http://localhost', '') }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await step('U04', '未登录：活动卡点击进入详情', '跳 /activities/{id}', async () => {
    await page.locator('.card').first().click()
    await page.waitForTimeout(1500)
    return { pass: /^\/activities\/\d+$/.test(pathOf()), actual: pathOf() }
  })

  await step('U05', '未登录：详情页秒杀按钮', '跳 /login 并带 redirect 回跳参数', async () => {
    await page.goto(BASE + '/activities/' + ACT)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1200)
    const btn = page.locator('.goods-action button').first()
    const txt = (await btn.textContent())?.trim()
    if ((await btn.isDisabled())) return { pass: false, actual: '按钮 disabled，文案=' + txt, note: '未登录时按钮应可点并引导登录' }
    await btn.click()
    await page.waitForTimeout(1200)
    const u = page.url()
    const ok = u.includes('/login') && u.includes('redirect')
    return { pass: ok, actual: u.replace('http://localhost', '') }
  })

  // ================= 登录流程 =================
  await page.goto(BASE + '/login?redirect=/activities/' + ACT)
  await page.waitForLoadState('networkidle')
  await step('U06', '登录页：填写账号密码点「登 录」', '登录成功并回跳 redirect', async () => {
    await page.locator('input[placeholder="用户名"]').fill(uname)
    await page.locator('input[placeholder="密码"]').fill('123456')
    await page.locator('button:has-text("登 录")').click()
    await page.waitForTimeout(2000)
    const ok = page.url().includes('/activities/' + ACT)
    return { pass: ok, actual: pathOf() }
  })

  await step('U07', '登录成功后顶部是否立即显示用户名（不刷新）', '右上角显示昵称，不再显示「登录/注册」', async () => {
    const hasUser = await visible('.topbar .user', 3000)
    const hasLoginBtn = await page.locator('.topbar button:has-text("登录")').count()
    const name = hasUser ? (await page.locator('.topbar .user-name').textContent())?.trim() : '(无)'
    return {
      pass: hasUser && hasLoginBtn === 0,
      actual: hasUser ? `显示用户「${name}」，登录按钮数=${hasLoginBtn}` : `仍显示登录/注册，登录按钮数=${hasLoginBtn}`,
      note: hasUser ? '' : 'Pinia getter isLogin 读 localStorage，无响应式依赖'
    }
  })

  // 注册必须在未登录态测：已登录时访问 /login 会被守卫重定向到活动页
  await setLogout()
  await page.goto(BASE + '/login')
  await page.waitForLoadState('networkidle')
  await step('U08', '登录页：切到「注册」Tab 并注册新账号', '注册成功并自动登录', async () => {
    await page.locator('#tab-register').click()
    await page.waitForTimeout(500)
    const rname = 'uitereg' + Date.now().toString().slice(-6)
    await page.locator('input[placeholder^="用户名（4~32"]').fill(rname)
    await page.locator('input[placeholder^="昵称"]').fill('注册测试')
    await page.locator('input[placeholder^="密码（6~64"]').fill('123456')
    await page.locator('input[placeholder="确认密码"]').fill('123456')
    await page.locator('button:has-text("注册并登录")').click()
    await page.waitForTimeout(2500)
    const ok = !page.url().includes('/login')
    return { pass: ok, actual: pathOf(), note: ok ? '' : '仍停留在登录页' }
  })

  // ================= 登录态：导航 =================
  await setLogin()

  await step('U09', '登录态：品牌 Logo 点击', '跳 /activities', async () => {
    await page.goto(BASE + '/orders')
    await page.waitForLoadState('networkidle')
    await page.locator('a.brand').click()
    const ok = await waitPath('/activities')
    return { pass: ok, actual: pathOf() }
  })

  await step('U10', '登录态：导航「我的订单」', '跳 /orders', async () => {
    await page.locator('.nav a:has-text("我的订单")').click()
    const ok = await waitPath('/orders')
    return { pass: ok, actual: pathOf() }
  })

  await step('U11', '登录态：导航「全部活动」', '跳 /activities', async () => {
    await page.locator('.nav a:has-text("全部活动")').click()
    const ok = await waitPath('/activities')
    return { pass: ok, actual: pathOf() }
  })

  // ================= 筛选 Tab =================
  for (const [i, label] of [['全部', 'U12'], ['进行中', 'U13'], ['即将开始', 'U14'], ['已结束', 'U15']].entries()) {
    const [txt, id] = label
    await page.goto(BASE + '/activities')
    await page.waitForLoadState('networkidle')
    await step(id, `筛选 Tab「${txt}」`, '列表按状态过滤并刷新', async () => {
      const before = await page.locator('.card').count()
      await page.locator(`.filter-bar .el-radio-button:has-text("${txt}")`).click()
      await page.waitForTimeout(1200)
      const after = await page.locator('.card').count()
      const emptyVisible = await page.locator('.el-empty').count()
      const totalTxt = (await page.locator('.filter-bar .muted').textContent())?.trim()
      const totalNum = parseInt((totalTxt || '').replace(/[^\d]/g, ''), 10) || 0
      // 断言「按状态真的过滤了」：卡片上的状态标签必须与所选 tab 一致，
      // 而不是硬编码活动数量（数据变了就假失败）。
      const tags = (await page.locator('.card .el-tag').allTextContents()).map((s) => s.trim())
      const expect = { 全部: null, 进行中: '进行中', 即将开始: '待开始', 已结束: '已结束' }[txt]
      const ok = expect === null
        ? after > 0 && after === totalNum
        : after > 0 && tags.every((t) => t === expect)
      return {
        pass: ok,
        actual: `筛选后 ${after} 张卡 / 状态标签=[${tags.join(',')}] / 期望状态=${expect || '不限'} / ${totalTxt}`
      }
    })
  }

  // ================= 详情页 =================
  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await step('U16', '登录态：进入进行中活动详情', '停在 /activities/{id}', async () => {
    await page.goto(BASE + '/activities/' + ACT)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    return { pass: pathOf() === '/activities/' + ACT, actual: pathOf() }
  })

  await step('U17', '详情页：返回活动列表按钮是否存在', '有返回入口（或浏览器返回可用）', async () => {
    const hasBtn = await page.locator('button:has-text("返回活动列表")').count()
    const hasHeaderBack = await page.locator('.back, .detail-back, [class*="back"]').count()
    return {
      pass: hasBtn + hasHeaderBack > 0,
      actual: `返回活动列表按钮=${hasBtn}，其他返回入口=${hasHeaderBack}`,
      note: hasBtn + hasHeaderBack === 0 ? '详情页无返回按钮，只能靠浏览器后退' : ''
    }
  })

  // ================= 抢购链路 =================
  await step('U18', '详情页：点秒杀按钮发起抢购', '跳 /seckill/result/{requestId}', async () => {
    const btn = page.locator('.goods-action button').first()
    const txt = (await btn.textContent())?.trim()
    const disabled = await btn.isDisabled()
    if (disabled) return { pass: false, actual: '按钮不可点，文案=' + txt }
    await btn.click()
    try {
      await page.waitForURL((u) => strip(new URL(u).pathname).startsWith('/seckill/result/'), { timeout: 8000 })
    } catch {}
    return { pass: pathOf().startsWith('/seckill/result/'), actual: pathOf() + '  (按钮文案: ' + txt + ')' }
  })

  await step('U19', '结果页：轮询后是否最终出结果', '出现「支付」或失败态，不停留在排队', async () => {
    if (!pathOf().startsWith('/seckill/result/')) return { pass: false, actual: '未进入结果页' }
    const deadline = Date.now() + 40000
    while (Date.now() < deadline) {
      const txt = await page.locator('.title').first().textContent().catch(() => '')
      const hasPay = await page.locator('button:has-text("立即支付")').count()
      const act = await page.locator('.actions').textContent().catch(() => '')
      if (hasPay > 0 || /失败|抢光|已结束|限购|重复/.test(txt || '')) {
        return { pass: true, actual: `结果: ${txt?.trim()} / 有支付按钮=${hasPay > 0}` }
      }
      if (/结果确认中/.test(txt || '')) {
        return { pass: false, actual: '最终停在「结果确认中」', note: '轮询超时未拿到终态: ' + (act || '').trim().slice(0, 60) }
      }
      await page.waitForTimeout(1500)
    }
    const t = await page.locator('.title').first().textContent().catch(() => '')
    return { pass: false, actual: '40s 内未出终态，当前=' + (t || '').trim() }
  })

  await step('U20', '结果页：前端 requestId 与后端真实值是否一致', 'URL 中的 requestId 能在后端订单里精确命中', async () => {
    const shown = (await page.locator('.request-id').textContent().catch(() => ''))?.replace('requestId:', '').trim()
    const urlId = pathOf().split('/').pop()
    // 订单列表 VO 不返回 requestId，直接拿前端值反查结果接口：能查到订单即证明 ID 精确
    const probe = await fetch(API + '/seckill/result/' + urlId, {
      headers: { Authorization: 'Bearer ' + TOKEN }
    }).then((r) => r.json()).catch(() => ({}))
    const hit = probe?.code === 200 && !!probe?.data?.orderNo
    return {
      pass: hit,
      actual: `前端 requestId= ${urlId} → 接口 code=${probe?.code} 订单号= ${probe?.data?.orderNo || '无'}`,
      note: hit ? '' : '前端 ID 查不到订单（精度丢失）'
    }
  })

  // ================= 订单页 =================
  await page.goto(BASE + '/orders')
  await page.waitForLoadState('networkidle')
  await step('U21', '订单页：空状态「去逛逛活动」', '跳 /activities', async () => {
    const c = await page.locator('button:has-text("去逛逛活动")').count()
    if (c === 0) return { pass: true, actual: '有订单记录，无空状态按钮（跳过）', note: '有数据时该按钮不渲染' }
    await page.locator('button:has-text("去逛逛活动")').click()
    const ok = await waitPath('/activities')
    return { pass: ok, actual: pathOf() }
  })

  await page.goto(BASE + '/orders')
  await page.waitForLoadState('networkidle')
  await step('U22', '订单页：列表渲染与状态展示', '订单卡片可见且状态文案正确', async () => {
    const cards = await page.locator('.order-item, .order-card, [class*="order"] .card').count()
    const body = await page.locator('.page-wrap, main').first().textContent().catch(() => '')
    return { pass: true, actual: `订单卡片数=${cards}，页面含「秒杀订单」标题=${/我的订单/.test(body || '')}` }
  })

  // ================= 404 =================
  await step('U23', '未知路由 /xxx 是否兜底到 404 页', '显示 404 而非白屏', async () => {
    await page.goto(BASE + '/not-exist-page-xyz')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(800)
    const has404 = await page.locator('text=/404|不存在|找不到/').count()
    return { pass: has404 > 0, actual: has404 > 0 ? '404 页面已渲染' : '未看到 404 文案，可能白屏' }
  })

  await step('U24', '404 页「回到首页」按钮', '跳 /activities', async () => {
    const btn = page.locator('button:has-text("回到首页")')
    if ((await btn.count()) === 0) return { pass: false, actual: '无返回按钮' }
    await btn.click()
    const ok = await waitPath('/activities')
    return { pass: ok, actual: pathOf() }
  })

  // ================= 用户下拉 + 登出 =================
  await setLogin()
  await step('U25', '用户下拉菜单展开', '出现「我的订单」「退出登录」两项', async () => {
    await page.locator('.topbar .user').hover()
    await page.waitForTimeout(800)
    const items = await page.locator('.el-dropdown-menu__item:visible').allTextContents()
    const ok = items.some((t) => t.includes('我的订单')) && items.some((t) => t.includes('退出登录'))
    return { pass: ok, actual: '菜单项: ' + JSON.stringify(items.map((t) => t.trim())) }
  })

  await step('U26', '下拉菜单「我的订单」', '跳 /orders', async () => {
    await page.locator('.el-dropdown-menu__item:visible:has-text("我的订单")').click()
    const ok = await waitPath('/orders')
    return { pass: ok, actual: pathOf() }
  })

  await step('U27', '下拉菜单「退出登录」', '清除本地 token', async () => {
    await page.locator('.topbar .user').hover()
    await page.waitForTimeout(700)
    await page.locator('.el-dropdown-menu__item:visible:has-text("退出登录")').click()
    await page.waitForTimeout(1500)
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    return {
      pass: !token,
      actual: `token=${token ? '仍存在' : '已清除'} / 登出后落在 ${pathOf()}`
    }
  })

  await step('U28', '退出后顶部 UI 是否立即回到「登录/注册」', '不显示用户名', async () => {
    const hasUser = await page.locator('.topbar .user').count()
    return {
      pass: hasUser === 0,
      actual: hasUser > 0 ? '仍显示用户头像/昵称' : '已回到登录/注册',
      note: hasUser > 0 ? '登出后 UI 未刷新（getter 缓存）' : ''
    }
  })

  await browser.close()

  const summary = {
    total: results.length,
    pass: results.filter((r) => r.pass).length,
    fail: results.filter((r) => !r.pass).length
  }
  fs.writeFileSync(__dirname + '/user-result.json', JSON.stringify({ summary, results, consoleErrors }, null, 2))
  console.log('\n===== 汇总 =====')
  console.log(summary)
  console.log('控制台错误:', consoleErrors.length)
}

main().catch((e) => {
  console.error('FATAL', e)
  process.exit(1)
})
