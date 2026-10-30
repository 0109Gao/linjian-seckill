import { defineStore } from 'pinia'
import { getCurrentUser } from '../api'

const TOKEN_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    loaded: false,
    // token 放进 state：getter 依赖响应式数据，登录/登出才会立即驱动 UI 更新。
    // 直接读 localStorage 的话，Pinia getter（底层 computed）会把结果永久缓存，
    // 导致登出后顶部仍显示用户名头像，必须刷新页面才恢复。
    token: localStorage.getItem(TOKEN_KEY)
  }),
  getters: {
    isLogin: (state) => !!state.token,
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录'
  },
  actions: {
    setLogin(data) {
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(REFRESH_KEY, data.refreshToken)
      this.token = data.accessToken
      this.user = {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        role: data.role
      }
      this.loaded = true
    },
    async fetchUser() {
      if (this.loaded) {
        return this.user
      }
      const res = await getCurrentUser()
      this.user = res.data
      this.loaded = true
      return this.user
    },
    clear() {
      this.user = null
      this.loaded = false
      this.token = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_KEY)
    }
  }
})
