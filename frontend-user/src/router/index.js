import { createRouter, createWebHistory } from 'vue-router'

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
    redirect: '/activities',
    children: [
      {
        path: 'activities',
        name: 'ActivityList',
        component: () => import('@/views/activity/list.vue'),
        meta: { title: '全部活动' }
      },
      {
        path: 'activities/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/activity/detail.vue'),
        meta: { title: '活动详情' }
      },
      {
        path: 'orders',
        name: 'MyOrders',
        component: () => import('@/views/order/my.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'seckill/result/:requestId',
        name: 'SeckillResult',
        component: () => import('@/views/order/result.vue'),
        meta: { title: '抢购结果', requiresAuth: true }
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
  history: createWebHistory('/user/'),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const token = localStorage.getItem('accessToken')

  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录还去登录页，直接回活动列表，避免重复登录
  if (to.meta.guestOnly && token) {
    return { path: '/activities' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 限时秒杀` : '限时秒杀'
})

export default router
