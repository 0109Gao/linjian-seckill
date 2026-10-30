import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 静态路由。
 *
 * 基座（后台管理系统）用的是「后端返回菜单 → 动态生成路由」，那套要配
 * 菜单表与 RBAC 权限点。秒杀项目只有 USER / ADMIN 两种角色，没有菜单表，
 * 直接写死更清楚：少一层间接，也少一个「菜单表配错导致页面进不去」的故障源。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '监控看板', icon: 'DataLine' }
      },
      {
        path: 'goods',
        name: 'Goods',
        component: () => import('@/views/goods/index.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'activities',
        name: 'ActivityList',
        component: () => import('@/views/activity/index.vue'),
        meta: { title: '活动管理', icon: 'Calendar' }
      },
      {
        path: 'activities/create',
        name: 'ActivityCreate',
        component: () => import('@/views/activity/edit.vue'),
        meta: { title: '新建活动', hidden: true, activeMenu: '/activities' }
      },
      {
        path: 'activities/:id/edit',
        name: 'ActivityEdit',
        component: () => import('@/views/activity/edit.vue'),
        meta: { title: '编辑活动', hidden: true, activeMenu: '/activities' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单查询', icon: 'Tickets' }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/stock/index.vue'),
        meta: { title: '库存调整', icon: 'Box' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const token = localStorage.getItem('accessToken')

  if (to.meta.guestOnly) {
    return token ? { path: '/dashboard' } : true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 角色校验必须在路由层做一次：后端每个管理接口都有 @PreAuthorize，
  // 但普通用户点进管理页只会看到满屏 403，体验上等同白屏
  const userStore = useUserStore()
  if (!userStore.loaded) {
    try {
      await userStore.fetchUser()
    } catch (e) {
      return { path: '/login' }
    }
  }
  if (!userStore.isAdmin) {
    return { path: '/login', query: { denied: '1' } }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 秒杀运营后台` : '秒杀运营后台'
})

export default router
