/* 管理端补测：修正选择器 + 写操作 + 控制台错误取证 */
const { chromium } = require('playwright-core')
const fs = require('fs')
const API = 'http://localhost/api'
const BASE = 'http://localhost/admin'
const results = []
const consoleMsgs = []

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  const page = await ctx.newPage()
  page.on('pageerror', (e) => consoleMsgs.push('[pageerror] ' + e.message))
  page.on('console', (m) => {
    if (m.type() === 'error' || m.type() === 'warning') {
      consoleMsgs.push(`[${m.type()}] ${m.text()}`)
    }
  })
  page.on('requestfailed', (r) => consoleMsgs.push('[404/failed] ' + r.url()))

  const strip = (pn) => (pn.startsWith('/admin') ? pn.slice(6) || '/' : pn)
  const pathOf = () => strip(new URL(page.url()).pathname)

  async function step(id, name, expectDesc, fn) {
    let pass = false, actual = '', note = ''
    try { const r = await fn(); pass = r.pass; actual = r.actual; note = r.note || '' } catch (e) {
      pass = false; actual = 'EXCEPTION'; note = e.message.split('\n')[0].slice(0, 140)
    }
    results.push({ id, name, expect: expectDesc, actual, pass, note })
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${name}  -> ${actual}${note ? ' | ' + note : ''}`)
  }

  // 登录
  const lg = await fetch(API + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: '123456' })
  }).then((r) => r.json())
  await page.goto(BASE + '/login')
  await page.evaluate(([t, r]) => { localStorage.setItem('accessToken', t); localStorage.setItem('refreshToken', r) },
    [lg.data.accessToken, lg.data.refreshToken])
  await page.goto(BASE + '/dashboard')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)

  // 菜单文案全量
  const menuTexts = await page.locator('.el-menu-item').allTextContents()
  console.log('实际菜单项:', JSON.stringify(menuTexts.map((t) => t.trim())))

  for (const [t, p, id] of [['订单查询', '/orders', 'A05b'], ['监控看板', '/dashboard', 'A06b'], ['库存调整', '/stock', 'A36']]) {
    await step(id, `侧边菜单「${t}」`, `跳 ${p}`, async () => {
      await page.locator(`.el-menu-item:has-text("${t}")`).click()
      await page.waitForTimeout(1600)
      return { pass: pathOf() === p, actual: pathOf() }
    })
  }

  // 库存调整
  await page.goto(BASE + '/stock?activityId=1001')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)

  await step('A31b', '库存页「调整库存」弹窗打开', '弹窗含当前库存与调整输入', async () => {
    const btn = page.locator('.el-table__body button:has-text("调整库存")').first()
    if ((await btn.count()) === 0) return { pass: false, actual: '无调整库存按钮' }
    await btn.click()
    await page.waitForTimeout(1000)
    const vis = await page.locator('.el-dialog').isVisible().catch(() => false)
    const title = (await page.locator('.el-dialog__title').textContent().catch(() => ''))?.trim()
    const hasInput = await page.locator('.el-dialog .el-input-number').count()
    return { pass: vis && hasInput > 0, actual: `弹窗=${vis} 标题=${title} 数字输入框=${hasInput}` }
  })

  await step('A32b', '弹窗「取消」不落库', '关闭弹窗', async () => {
    await page.locator('.el-dialog__footer button:has-text("取消")').click()
    await page.waitForTimeout(800)
    const vis = await page.locator('.el-dialog').isVisible().catch(() => false)
    return { pass: !vis, actual: vis ? '未关闭' : '已关闭' }
  })

  // 真实调整：+1 再 -1 复原
  const before = await fetch(API + '/admin/stock/activity/1001', { headers: { Authorization: 'Bearer ' + lg.data.accessToken } }).then((r) => r.json())
  const row0 = before?.data?.items?.[0] || before?.data?.[0]
  const goodsId = row0?.activityGoodsId
  const stock0 = row0?.stock

  await step('A37', '库存调整真实提交（+1）', 'DB 库存 +1 并提示成功', async () => {
    await page.locator('.el-table__body button:has-text("调整库存")').first().click()
    await page.waitForTimeout(900)
    const num = page.locator('.el-dialog .el-input-number input')
    await num.fill('1')
    await num.blur()
    await page.waitForTimeout(400)
    await page.locator('.el-dialog__footer button:has-text("确认调整")').click()
    await page.waitForTimeout(2500)
    const msg = await page.locator('.el-message').allTextContents().catch(() => [])
    const after = await fetch(API + '/admin/stock/activity/1001', { headers: { Authorization: 'Bearer ' + lg.data.accessToken } }).then((r) => r.json())
    const row1 = after?.data?.items?.[0] || after?.data?.[0]
    return {
      pass: row1?.stock === stock0 + 1,
      actual: `库存 ${stock0} → ${row1?.stock}，提示=${JSON.stringify(msg.map((m) => m.trim()))}`
    }
  })

  await step('A38', '库存调整复原（-1）', '库存回到原值', async () => {
    // 用接口复原，避免脚本脆弱导致脏数据
    const r = await fetch(API + '/admin/stock/adjust', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + lg.data.accessToken, 'Content-Type': 'application/json' },
      body: JSON.stringify({ activityGoodsId: goodsId, delta: -1, remark: 'QA 复原' })
    }).then((r) => r.json())
    const after = await fetch(API + '/admin/stock/activity/1001', { headers: { Authorization: 'Bearer ' + lg.data.accessToken } }).then((r) => r.json())
    const row1 = after?.data?.items?.[0] || after?.data?.[0]
    return { pass: row1?.stock === stock0, actual: `复原后库存=${row1?.stock}（原始 ${stock0}）, 接口 code=${r.code}` }
  })

  // 表格 ID 精度
  await page.goto(BASE + '/goods')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)
  await step('A39', '商品表格 ID 列显示是否精确', 'ID 与后端返回值一致', async () => {
    const shown = (await page.locator('.el-table__body tr').first().locator('td').first().textContent())?.trim()
    const raw = await (await fetch(API + '/admin/goods?pageNum=1&pageSize=5', { headers: { Authorization: 'Bearer ' + lg.data.accessToken } })).text()
    const real = raw.match(/"id":"?(\d+)"?/)?.[1]
    const parsed = JSON.parse('[' + real + ']')[0]
    return {
      pass: shown === real,
      actual: `后端=${real} / 表格显示=${shown} / JS 解析后=${parsed}`,
      note: shown !== real ? '雪花 ID 超 JS 安全整数，表格显示错误 ID' : ''
    }
  })

  await page.goto(BASE + '/orders')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)
  await step('A40', '订单表格 ID / 用户ID 列是否精确', '与后端一致', async () => {
    const cells = await page.locator('.el-table__body tr').first().locator('td').allTextContents()
    const raw = await (await fetch(API + '/admin/orders?pageNum=1&pageSize=1', { headers: { Authorization: 'Bearer ' + lg.data.accessToken } })).text()
    // 订单表格展示的是 orderNo 与 userId，订单主键 id 不在列里，只校验 userId
    const uid = raw.match(/"userId":"?(\d{15,})"?/)?.[1]
    const shownTxt = cells.join(' | ').replace(/\s+/g, ' ')
    const ok = !uid || shownTxt.includes(uid)
    return {
      pass: ok,
      actual: `后端 userId=${uid} / 表格首行=${shownTxt.slice(0, 90)}`,
      note: ok ? '' : 'userId 在表格中显示为失真值'
    }
  })

  // 退出后登录页渲染
  await step('A41', '退出登录后 /login 页面是否正常渲染', '出现账号/密码输入框与登录按钮', async () => {
    await page.locator('.user-info').hover()
    await page.waitForTimeout(800)
    await page.locator('.el-dropdown-menu__item:visible:has-text("退出登录")').click()
    await page.waitForTimeout(2500)
    const inputs = await page.locator('input').count()
    const btn = await page.locator('button').count()
    return { pass: inputs >= 2 && btn >= 1, actual: `路径=${pathOf()} 输入框=${inputs} 按钮=${btn}` }
  })

  await browser.close()
  const summary = { total: results.length, pass: results.filter((r) => r.pass).length, fail: results.filter((r) => !r.pass).length }
  fs.writeFileSync(__dirname + '/admin2-result.json', JSON.stringify({ summary, results, consoleMsgs }, null, 2))
  console.log('\n汇总:', summary)
  console.log('\n===== 控制台错误/警告 =====')
  consoleMsgs.slice(0, 12).forEach((e) => console.log('  ' + e.slice(0, 180)))
}
main().catch((e) => { console.error('FATAL', e); process.exit(1) })
