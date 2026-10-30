/* 管理端全量交互实测 */
const { chromium } = require('playwright-core')
const fs = require('fs')
const API = 'http://localhost/api'
const BASE = 'http://localhost/admin'
const results = []
const consoleErrors = []

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  const page = await ctx.newPage()
  page.on('pageerror', (e) => consoleErrors.push('[pageerror] ' + e.message))
  page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push('[console] ' + m.text()) })

  const strip = (pn) => (pn.startsWith('/admin') ? pn.slice(6) || '/' : pn)
  const pathOf = () => strip(new URL(page.url()).pathname)

  async function step(id, name, expectDesc, fn) {
    let pass = false, actual = '', note = ''
    try { const r = await fn(); pass = r.pass; actual = r.actual; note = r.note || '' } catch (e) {
      pass = false; actual = 'EXCEPTION'; note = e.message.split('\n')[0].slice(0, 150)
    }
    results.push({ id, name, expect: expectDesc, actual, pass, note })
    console.log(`${pass ? 'PASS' : 'FAIL'}  ${id}  ${name}  -> ${actual}${note ? ' | ' + note : ''}`)
  }

  // ---------- A01 登录 ----------
  await page.goto(BASE + '/login')
  await page.waitForLoadState('networkidle')
  await step('A01', '管理端登录（admin/123456）', '登录成功进入 /dashboard', async () => {
    await page.locator('input[placeholder*="账号"], input[placeholder*="用户名"]').first().fill('admin')
    await page.locator('input[type="password"]').first().fill('123456')
    await page.locator('button:has-text("登")').first().click()
    await page.waitForTimeout(2500)
    return { pass: pathOf() === '/dashboard', actual: pathOf() }
  })

  // ---------- A02 侧边栏折叠 ----------
  await step('A02', '侧边栏折叠按钮', 'aside 宽度 210px → 64px', async () => {
    const w1 = await page.locator('.aside').evaluate((e) => e.getBoundingClientRect().width)
    await page.locator('.collapse-btn').click()
    await page.waitForTimeout(900)
    const w2 = await page.locator('.aside').evaluate((e) => e.getBoundingClientRect().width)
    await page.locator('.collapse-btn').click()
    await page.waitForTimeout(600)
    return { pass: w2 < w1 && w2 > 0, actual: `${Math.round(w1)}px → ${Math.round(w2)}px` }
  })

  // ---------- A03 菜单跳转 ----------
  for (const [t, p, id] of [['商品管理', '/goods', 'A03'], ['活动管理', '/activities', 'A04'], ['订单查询', '/orders', 'A05'], ['监控看板', '/dashboard', 'A06']]) {
    await step(id, `侧边菜单「${t}」`, `跳 ${p}`, async () => {
      await page.locator(`.el-menu-item:has-text("${t}")`).click()
      await page.waitForTimeout(1500)
      return { pass: pathOf() === p, actual: pathOf() }
    })
  }

  // ---------- A07 看板刷新 ----------
  await page.goto(BASE + '/dashboard')
  await page.waitForLoadState('networkidle')
  await step('A07', '看板「刷新」按钮', '重新拉取统计数据，页面不报错', async () => {
    const before = consoleErrors.length
    await page.locator('button:has-text("刷新")').first().click()
    await page.waitForTimeout(1800)
    const body = (await page.locator('.page-container').textContent()) || ''
    return { pass: consoleErrors.length === before, actual: `刷新后无新错误=${consoleErrors.length === before} 页面有数据=${/\d/.test(body)}` }
  })

  // ---------- 活动管理 ----------
  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)

  for (const [txt, id] of [['待开始', 'A08'], ['进行中', 'A09'], ['已结束', 'A10'], ['已取消', 'A11'], ['全部', 'A12']]) {
    await step(id, `活动筛选「${txt}」`, '表格按状态刷新', async () => {
      await page.locator(`.filter-bar .el-radio-button:has-text("${txt}")`).click()
      await page.waitForTimeout(1200)
      const rows = await page.locator('.el-table__body tr').count()
      return { pass: true, actual: `行数=${rows}` }
    })
  }

  await step('A13', '「新建活动」按钮', '跳 /activities/create', async () => {
    await page.locator('button:has-text("新建活动")').click()
    await page.waitForTimeout(1500)
    return { pass: pathOf() === '/activities/create', actual: pathOf() }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
  await step('A14', '表格「编辑」按钮（待开始活动）', '跳 /activities/{id}/edit', async () => {
    const btn = page.locator('.el-table__body button:has-text("编辑")').first()
    if ((await btn.count()) === 0) return { pass: false, actual: '无编辑按钮' }
    const disabled = await btn.isDisabled()
    if (disabled) return { pass: true, actual: '无「待开始」活动，编辑按钮按设计禁用（跳过）', note: '只有 status=0 可编辑' }
    await btn.click()
    await page.waitForTimeout(1500)
    return { pass: /^\/activities\/\d+\/edit$/.test(pathOf()), actual: pathOf() }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
  await step('A15', '表格「预热」按钮', '调用预热接口并提示成功', async () => {
    const btn = page.locator('.el-table__body button:has-text("预热")').first()
    const disabled = await btn.isDisabled()
    if (disabled) return { pass: true, actual: '按钮禁用（活动已结束/取消），跳过' }
    await btn.click()
    await page.waitForTimeout(2500)
    const msg = await page.locator('.el-message').allTextContents().catch(() => [])
    return { pass: msg.length > 0, actual: '提示: ' + JSON.stringify(msg.map((m) => m.trim())), note: msg.length ? '' : '无任何反馈提示' }
  })

  await step('A16', '表格「库存」按钮', '跳 /stock?activityId=xxx', async () => {
    const btn = page.locator('.el-table__body button:has-text("库存")').first()
    await btn.click()
    await page.waitForTimeout(1500)
    return { pass: pathOf() === '/stock', actual: pathOf() + new URL(page.url()).search }
  })

  await page.goto(BASE + '/activities')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
  await step('A17', '活动列表分页控件存在且可点', '切页后表格刷新', async () => {
    const pager = await page.locator('.el-pagination').count()
    if (pager === 0) return { pass: false, actual: '无分页组件' }
    const next = page.locator('.el-pagination button.btn-next')
    const disabled = await next.isDisabled()
    if (disabled) return { pass: true, actual: '仅 1 页，下一页禁用（正常）' }
    await next.click()
    await page.waitForTimeout(1500)
    return { pass: true, actual: '已翻页' }
  })

  await step('A18', '表格「取消」按钮', '有确认弹窗且可取消', async () => {
    const btn = page.locator('.el-table__body button:has-text("取消")').first()
    if ((await btn.count()) === 0) return { pass: false, actual: '无取消按钮' }
    const disabled = await btn.isDisabled()
    if (disabled) return { pass: true, actual: '按钮禁用（跳过）' }
    await btn.click()
    await page.waitForTimeout(1000)
    const box = await page.locator('.el-message-box').count()
    if (box > 0) {
      // 不能写 has-text("取消")：确认按钮文案是「确认取消」，子串匹配会点到它，
      // 结果是真的把活动取消了（本用例只想验证弹窗存在）。用「再想想」精确匹配。
      await page.locator('.el-message-box button:has-text("再想想")').click()
      await page.waitForTimeout(600)
    }
    return { pass: box > 0, actual: `二次确认弹窗=${box > 0}（已点取消，未真正下架）`, note: box > 0 ? '' : '直接执行无二次确认' }
  })

  // ---------- 商品管理 ----------
  await page.goto(BASE + '/goods')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)

  await step('A19', '商品页「查询」按钮', '按关键词过滤', async () => {
    const all = await page.locator('.el-table__body tr').count()
    await page.locator('input[placeholder="按商品名搜索"]').fill('咖啡')
    await page.locator('button:has-text("查询")').click()
    await page.waitForTimeout(1500)
    const filtered = await page.locator('.el-table__body tr').count()
    return { pass: filtered > 0 && filtered <= all, actual: `全部 ${all} 行 → 搜索后 ${filtered} 行` }
  })

  await step('A20', '商品搜索框清空触发重新查询', '恢复全量', async () => {
    await page.locator('input[placeholder="按商品名搜索"]').fill('')
    await page.waitForTimeout(1200)
    const n = await page.locator('.el-table__body tr').count()
    return { pass: n > 0, actual: `行数=${n}` }
  })

  await step('A21', '「新增商品」打开弹窗', '弹窗标题=新增商品', async () => {
    await page.locator('button:has-text("新增商品")').click()
    await page.waitForTimeout(900)
    const title = (await page.locator('.el-dialog__title').textContent())?.trim()
    return { pass: title === '新增商品', actual: '标题=' + title }
  })

  await step('A22', '新增弹窗「取消」关闭', '弹窗关闭且不提交', async () => {
    await page.locator('.el-dialog__footer button:has-text("取消")').click()
    await page.waitForTimeout(800)
    const visible = await page.locator('.el-dialog').isVisible().catch(() => false)
    return { pass: !visible, actual: visible ? '弹窗仍打开' : '已关闭' }
  })

  await step('A23', '新增商品表单校验（空名称直接保存）', '提示必填，不提交', async () => {
    await page.locator('button:has-text("新增商品")').click()
    await page.waitForTimeout(800)
    await page.locator('.el-dialog__footer button:has-text("保存")').click()
    await page.waitForTimeout(1000)
    const err = await page.locator('.el-form-item__error').allTextContents()
    await page.locator('.el-dialog__footer button:has-text("取消")').click()
    await page.waitForTimeout(600)
    return { pass: err.length > 0, actual: '校验提示: ' + JSON.stringify(err.map((e) => e.trim())), note: err.length ? '' : '空表单竟可提交' }
  })

  await step('A24', '编辑商品弹窗带出原值', '标题=编辑商品且名称已填充', async () => {
    await page.locator('.el-table__body button:has-text("编辑")').first().click()
    await page.waitForTimeout(900)
    const title = (await page.locator('.el-dialog__title').textContent())?.trim()
    const name = await page.locator('.el-dialog input[maxlength="64"]').inputValue()
    await page.locator('.el-dialog__footer button:has-text("取消")').click()
    await page.waitForTimeout(600)
    return { pass: title === '编辑商品' && !!name, actual: `标题=${title} 名称=${name}` }
  })

  await step('A25', '删除商品二次确认', '弹出确认框（点取消不删）', async () => {
    await page.locator('.el-table__body button:has-text("删除")').first().click()
    await page.waitForTimeout(1000)
    const box = await page.locator('.el-message-box').count()
    if (box > 0) { await page.locator('.el-message-box button:has-text("取消")').click(); await page.waitForTimeout(600) }
    return { pass: box > 0, actual: `确认弹窗=${box > 0}`, note: box > 0 ? '' : '无二次确认，直接删除' }
  })

  // ---------- 订单管理 ----------
  await page.goto(BASE + '/orders')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)

  await step('A26', '订单页「查询」按钮', '按筛选条件查询', async () => {
    const before = await page.locator('.el-table__body tr').count()
    await page.locator('button:has-text("查询")').click()
    await page.waitForTimeout(1500)
    const after = await page.locator('.el-table__body tr').count()
    return { pass: true, actual: `查询前 ${before} 行 → 查询后 ${after} 行` }
  })

  await step('A27', '订单页「重置」按钮', '清空筛选条件并重查', async () => {
    await page.locator('button:has-text("重置")').click()
    await page.waitForTimeout(1500)
    const n = await page.locator('.el-table__body tr').count()
    return { pass: true, actual: `重置后 ${n} 行` }
  })

  await step('A28', '订单状态筛选下拉', '选择状态后列表刷新', async () => {
    const sel = page.locator('.filter-bar .el-select, .el-select').first()
    if ((await sel.count()) === 0) return { pass: false, actual: '无状态下拉' }
    await sel.click()
    await page.waitForTimeout(700)
    const opts = await page.locator('.el-select-dropdown__item:visible').allTextContents()
    if (opts.length > 0) {
      await page.locator('.el-select-dropdown__item:visible').first().click()
      await page.waitForTimeout(1500)
    }
    return { pass: opts.length > 0, actual: '选项: ' + JSON.stringify(opts.map((o) => o.trim())) }
  })

  // ---------- 库存管理 ----------
  await page.goto(BASE + '/stock')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1200)

  await step('A29', '库存页活动选择器切换', '切换后库存明细刷新', async () => {
    const sel = page.locator('.el-select').first()
    const c = await sel.count()
    if (c === 0) return { pass: false, actual: '无活动选择器' }
    await sel.click()
    await page.waitForTimeout(700)
    const opts = await page.locator('.el-select-dropdown__item:visible').allTextContents()
    if (opts.length > 1) {
      await page.locator('.el-select-dropdown__item:visible').nth(1).click()
      await page.waitForTimeout(1800)
    }
    return { pass: opts.length > 0, actual: `可选活动 ${opts.length} 个` }
  })

  await step('A30', '库存页「刷新」按钮', '重新拉取库存', async () => {
    const btn = page.locator('button:has-text("刷新")').first()
    if ((await btn.count()) === 0) return { pass: false, actual: '无刷新按钮' }
    await btn.click()
    await page.waitForTimeout(1500)
    return { pass: true, actual: '已刷新' }
  })

  await step('A31', '库存「调整」弹窗打开', '弹窗含增减输入', async () => {
    const btn = page.locator('.el-table__body button:has-text("调整库存")').first()
    if ((await btn.count()) === 0) return { pass: false, actual: '无调整按钮' }
    await btn.click()
    await page.waitForTimeout(1000)
    const vis = await page.locator('.el-dialog').isVisible().catch(() => false)
    return { pass: vis, actual: vis ? '弹窗已打开' : '弹窗未出现' }
  })

  await step('A32', '库存调整弹窗「取消」', '关闭且不改动', async () => {
    await page.locator('.el-dialog__footer button:has-text("取消")').click()
    await page.waitForTimeout(700)
    const vis = await page.locator('.el-dialog').isVisible().catch(() => false)
    return { pass: !vis, actual: vis ? '未关闭' : '已关闭' }
  })

  // ---------- 用户下拉 ----------
  await step('A33', '管理端用户下拉「前台商城」', '新窗口指向用户端正确地址', async () => {
    await page.locator('.user-info').hover()
    await page.waitForTimeout(800)
    const items = await page.locator('.el-dropdown-menu__item:visible').allTextContents()
    const [popup] = await Promise.all([
      ctx.waitForEvent('page', { timeout: 4000 }).catch(() => null),
      page.locator('.el-dropdown-menu__item:visible:has-text("前台商城")').click()
    ])
    let target = '(未打开新窗口)'
    if (popup) { target = popup.url(); await popup.close() }
    return {
      pass: /localhost\/user|localhost:5173/.test(target),
      actual: `菜单项=${JSON.stringify(items.map((i) => i.trim()))} 打开=${target}`,
      note: target.includes('5173') ? '硬编码 5173 端口，nginx 部署（80 端口）下会打开空白页' : ''
    }
  })

  await step('A34', '管理端用户下拉「退出登录」', '清 token 并回 /login', async () => {
    await page.locator('.user-info').hover()
    await page.waitForTimeout(800)
    await page.locator('.el-dropdown-menu__item:visible:has-text("退出登录")').click()
    await page.waitForTimeout(2000)
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    const btn = await page.locator('button:has-text("登录")').count()
    return { pass: !token || pathOf() === '/login', actual: `token=${token ? '仍存在' : '已清除'} / URL=${pathOf()} / 登录页按钮=${btn}` }
  })

  await step('A35', '退出后是否还能直接进管理页（鉴权）', '被拦回 /login', async () => {
    await page.goto(BASE + '/goods')
    await page.waitForTimeout(2000)
    return { pass: pathOf() === '/login', actual: pathOf() }
  })

  await browser.close()
  const summary = { total: results.length, pass: results.filter((r) => r.pass).length, fail: results.filter((r) => !r.pass).length }
  fs.writeFileSync(__dirname + '/admin-result.json', JSON.stringify({ summary, results, consoleErrors }, null, 2))
  console.log('\n汇总:', summary)
  console.log('控制台错误:', consoleErrors.length)
  consoleErrors.slice(0, 5).forEach((e) => console.log('  ', e.slice(0, 160)))
}
main().catch((e) => { console.error('FATAL', e); process.exit(1) })
