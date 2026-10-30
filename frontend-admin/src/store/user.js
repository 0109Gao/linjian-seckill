import { defineStore } from 'pinia'
import { getCurrentUser } from '../api'

const TOKEN_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    loaded: false
  }),
  getters: {
    isLogin: () => !!localStorage.getItem(TOKEN_KEY),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录'
  },
  actions: {
    setLogin(data) {
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(REFRESH_KEY, data.refreshToken)
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
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_KEY)
    }
  }
})
