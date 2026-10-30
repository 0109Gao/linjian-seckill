<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="text">页面不存在或已被移除</p>
    <el-button type="primary" round @click="router.push('/activities')">回到首页</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.code {
  font-size: 76px;
  font-weight: 800;
  color: var(--sk-primary);
  opacity: 0.85;
  letter-spacing: 2px;
}

.text {
  color: var(--sk-text-sub);
  font-size: 14px;
}
</style>
