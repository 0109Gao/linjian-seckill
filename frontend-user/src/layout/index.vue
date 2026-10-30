<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/activities" class="brand">
          <span class="brand-mark">秒</span>
          <span class="brand-name">限时秒杀</span>
        </router-link>

        <nav class="nav">
          <router-link to="/activities" class="nav-item" active-class="active">全部活动</router-link>
          <router-link to="/orders" class="nav-item" active-class="active">我的订单</router-link>
        </nav>

        <div class="right">
          <template v-if="userStore.isLogin">
            <el-dropdown @command="handleCommand">
              <span class="user">
                <el-avatar :size="26" class="avatar">{{ avatarText }}</el-avatar>
                <span class="user-name">{{ userStore.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="goLogin">登录</el-button>
            <el-button type="primary" round @click="goLogin">注册 / 登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      限时秒杀 · Redis 预扣 + RabbitMQ 异步落单 · 单机压测项目
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout as logoutApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const avatarText = computed(() => {
  const name = userStore.displayName
  return name ? name.charAt(0).toUpperCase() : 'U'
})

onMounted(() => {
  // 有 token 但 store 里没用户信息（刷新页面），补一次；失败由拦截器接管
  if (userStore.isLogin && !userStore.loaded) {
    userStore.fetchUser().catch(() => {})
  }
})

function goLogin() {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

async function handleCommand(command) {
  if (command === 'orders') {
    router.push('/orders')
    return
  }
  if (command === 'logout') {
    try {
      await logoutApi()
    } catch (e) {
      // 登出失败不阻塞本地清理：token 可能已过期，服务端本来就删不掉
    }
    userStore.clear()
    ElMessage.success('已退出登录')
    router.push('/activities')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid var(--sk-border);
}

.topbar-inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 60px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 17px;
}

.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--sk-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-item {
  padding: 6px 14px;
  border-radius: 8px;
  color: var(--sk-text-sub);
  font-size: 14px;
  transition: all 0.15s;
}

.nav-item:hover {
  color: var(--sk-primary);
  background: var(--sk-primary-light);
}

.nav-item.active {
  color: var(--sk-primary);
  background: var(--sk-primary-light);
  font-weight: 600;
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  font-size: 14px;
}

.avatar {
  background: var(--sk-primary);
  color: #fff;
  font-size: 13px;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  flex: 1;
}

.footer {
  padding: 20px;
  text-align: center;
  color: #a8abb2;
  font-size: 12px;
  border-top: 1px solid var(--sk-border);
  background: #fff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
