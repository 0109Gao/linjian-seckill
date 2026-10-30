# 自动化验收脚本

这套脚本用 **Playwright 驱动本机 Edge（headless）** 真实点击每个按钮、断言 DOM 与 URL 变化，
不是读代码推断，也不是 curl 打接口。共 **90 条断言**。

## 为什么不用 MCP 浏览器 / 截图肉眼判断

试过两种更省事的方式，都不可靠：

- **DOM 快照会返回缓存旧数据**：URL 已是 `/activities`，快照还给订单页的 DOM，会误判路由失效
- **后台标签页截图有渲染节流**：会截到半透明中间态，差点把正常路由切换判成「页面全白」

最终方案：`playwright-core` + 本机 Edge，自己拿 DOM、监听 `pageerror` / `console`，每项都有确定结论。

## 安装

```bash
cd .qa
npm init -y
npm i playwright-core
```

不需要下载浏览器内核（直接用系统的 Edge）。

## 运行

前提：全栈已启动（`docker compose -f docker/docker-compose.full.yml up -d`），
且演示数据处于可用状态（活动进行中、库存 > 0）。

```bash
node user.spec.js     # 用户端 28 项：登录注册、列表、详情、抢购、结果、订单、筛选、登出
node user2.spec.js    # 用户端补充 8 项：返回按钮、支付闭环、结果页各分支
node admin.spec.js    # 管理端 35 项：菜单、筛选、增删改、预热、库存、分页、退出
node admin2.spec.js   # 管理端补充 10 项：库存调整写操作、ID 精度、菜单跳转
node verify.js        # 修复专项 9 项：ID 精度、登出刷新、返回白屏、前台商城
```

## 辅助脚本

| 脚本 | 用途 |
|---|---|
| `seed_demo.py` | 通过管理端接口创建演示商品与活动（不直接改库） |
| `prep_orders.py` | 建演示账号并抢 2 单（1 已支付 / 1 待支付） |
| `shot.js` | 批量截取 11 张项目截图（2x 高清） |
| `make_html.py` | 把 README 转成可双击打开的项目介绍页 |
| `check_html.js` | 校验介绍页图片加载与排版溢出 |

## 两个踩过的坑（写脚本时注意）

**1. `has-text` 是子串匹配，会点错按钮**

活动取消弹窗的确认按钮文案是「确认取消」，`button:has-text("取消")` 会匹配到它 ——
本来只想验证弹窗存在，结果真的把活动取消了。要用「再想想」（取消按钮）精确匹配。

**2. 已登录状态访问 `/login` 会被守卫重定向**

测注册流程前必须先清 localStorage，否则测的根本不是登录页。

## 脚本会改数据吗

会。`admin2.spec.js` 会真实调整库存（测完复原），`user.spec.js` 会真实抢购并支付。
**不要指向生产环境。** 建议每次跑完用 `seed_demo.py` 重置演示数据。
