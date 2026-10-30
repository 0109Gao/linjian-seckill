import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const TOKEN_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：带上 accessToken
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 刷新令牌。
 *
 * 这里用裸 axios 而不是 request 实例，否则 refresh 自身返回 401 时会再次进入
 * 拦截器，形成刷新 → 401 → 再刷新的死循环。
 *
 * 并发场景：一次页面加载可能同时发出 3 个请求同时 401。若各自去刷新，
 * 后端会签发 3 组新 token，最后写入 localStorage 的那组覆盖其余两组，
 * 被覆盖的 token 立刻失效——表现为「刚登录就掉线」。所以用单个 Promise
 * 在飞行中复用，后续请求挂在同一 Promise 上。
 */
let refreshPromise = null

function refreshToken() {
  if (refreshPromise) {
    return refreshPromise
  }
  const rt = localStorage.getItem(REFRESH_KEY)
  if (!rt) {
    return Promise.reject(new Error('no refresh token'))
  }
  refreshPromise = axios
    .post('/api/auth/refresh', { refreshToken: rt })
    .then((resp) => {
      const body = resp.data
      if (body.code !== 200 || !body.data) {
        throw new Error(body.message || '令牌刷新失败')
      }
      localStorage.setItem(TOKEN_KEY, body.data.accessToken)
      localStorage.setItem(REFRESH_KEY, body.data.refreshToken)
      return body.data.accessToken
    })
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

function redirectToLogin(message) {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  if (router.currentRoute.value.path !== '/login') {
    ElMessage.error(message || '登录已过期，请重新登录')
    router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async (error) => {
    const { response, config } = error

    if (!response) {
      ElMessage.error('网络异常，请稍后再试')
      return Promise.reject(error)
    }

    // 无感刷新：只重试一次，避免 token 配置错误时无限循环
    if (response.status === 401 && config && !config._retried) {
      config._retried = true
      try {
        const token = await refreshToken()
        config.headers = config.headers || {}
        config.headers.Authorization = 'Bearer ' + token
        return request(config)
      } catch (e) {
        redirectToLogin()
        return Promise.reject(error)
      }
    }

    if (response.status === 401) {
      redirectToLogin()
    } else if (response.status === 403) {
      ElMessage.error('无权限访问')
    } else if (response.status === 429) {
      ElMessage.warning('请求过于频繁，请稍后再试')
    } else {
      ElMessage.error(response.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
