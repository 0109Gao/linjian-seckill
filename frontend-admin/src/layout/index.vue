<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '210px'" class="aside">
      <div class="logo">
        <span class="logo-mark">秒</span>
        <span v-if="!isCollapse" class="logo-text">运营后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="page-title-text">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag size="small" type="success" effect="plain" class="role-tag">ADMIN</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.displayName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="shop" divided>前台商城</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout as logoutApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const menus = [
  { path: '/dashboard', title: '监控看板', icon: 'DataLine' },
  { path: '/goods', title: '商品管理', icon: 'Goods' },
  { path: '/activities', title: '活动管理', icon: 'Calendar' },
  { path: '/orders', title: '订单查询', icon: 'Tickets' },
  { path: '/stock', title: '库存调整', icon: 'Box' }
]

const activeMenu = computed(() => route.meta.activeMenu || route.path)

const currentTitle = computed(() => route.meta.title || '')

async function handleCommand(command) {
  if (command === 'shop') {
    // 用户端与后台共用同一个 nginx（用户端挂在 /user/），生产必须用相对路径。
    // 写死 http://localhost:5173 只在本地 dev server 下成立，线上点了是空白页。
    const shopUrl = import.meta.env.DEV ? 'http://localhost:5173/user/' : '/user/'
    window.open(shopUrl, '_blank')
    return
  }
  if (command === 'logout') {
    try {
      await logoutApi()
    } catch (e) {
      // token 可能已过期，服务端删不掉，本地照样清
    }
    userStore.clear()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #304156;
  transition: width 0.2s;
  overflow-x: hidden;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #e93b3d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.header {
  background: #fff;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e8eb;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: #606266;
}

.page-title-text {
  font-size: 15px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  font-size: 11px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  font-size: 14px;
  outline: none;
}

.main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
