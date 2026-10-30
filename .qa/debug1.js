const { chromium } = require('playwright-core')
;(async () => {
  const b = await chromium.launch({ channel: 'msedge', headless: true })
  const p = await (await b.newContext({ viewport: { width: 1440, height: 900 } })).newPage()
  p.on('pageerror', (e) => console.log('PAGEERROR:', e.message))
  p.on('console', (m) => { if (m.type() === 'error') console.log('CONSOLE:', m.text()) })

  await p.goto('http://localhost/user/login')
  await p.waitForLoadState('networkidle')
  await p.waitForTimeout(800)

  console.log('===== tabs 结构 =====')
  const tabs = await p.locator('.el-tabs__item').evaluateAll((els) =>
    els.map((e) => ({ text: e.textContent.trim(), role: e.getAttribute('role'), cls: e.className }))
  )
  console.log(JSON.stringify(tabs, null, 2))

  console.log('===== 尝试点击「注册」 =====')
  try {
    await p.locator('#tab-register').click({ timeout: 5000 })
    await p.waitForTimeout(800)
    console.log('点击 #tab-register OK, 注册表单可见 =', await p.locator('input[placeholder^="确认密码"]').isVisible())
  } catch (e) {
    console.log('点击失败:', e.message.split('\n')[0])
  }

  // U28 复现：登出后 getter 是否刷新
  console.log('\n===== U28 复现：登出后 UI =====')
  const login = await fetch('http://localhost/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'uitester696579', password: '123456' })
  }).then((r) => r.json())
  await p.goto('http://localhost/user/activities')
  await p.evaluate(([t, r]) => { localStorage.setItem('accessToken', t); localStorage.setItem('refreshToken', r) },
    [login.data.accessToken, login.data.refreshToken])
  await p.reload(); await p.waitForLoadState('networkidle'); await p.waitForTimeout(800)
  console.log('登录后 .user 元素数 =', await p.locator('.topbar .user').count())

  await p.locator('.topbar .user').hover(); await p.waitForTimeout(600)
  await p.locator('.el-dropdown-menu__item:visible:has-text("退出登录")').click()
  await p.waitForTimeout(1500)
  const token = await p.evaluate(() => localStorage.getItem('accessToken'))
  console.log('登出后 token =', token)
  console.log('登出后 .user 元素数 =', await p.locator('.topbar .user').count())
  console.log('登出后 登录按钮数 =', await p.locator('.topbar button:has-text("登录")').count())

  // 强制刷新后是否恢复
  await p.reload(); await p.waitForLoadState('networkidle'); await p.waitForTimeout(600)
  console.log('刷新后 .user 元素数 =', await p.locator('.topbar .user').count())
  console.log('刷新后 登录按钮数 =', await p.locator('.topbar button:has-text("登录")').count())

  await b.close()
})()
