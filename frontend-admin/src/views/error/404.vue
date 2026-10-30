<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="text">页面不存在</p>
    <el-button type="primary" @click="router.push('/dashboard')">回到看板</el-button>
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
  font-size: 72px;
  font-weight: 800;
  color: #409eff;
  opacity: 0.7;
}

.text {
  color: #909399;
  font-size: 14px;
}
</style>
