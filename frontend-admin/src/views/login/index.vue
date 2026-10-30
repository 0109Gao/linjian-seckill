<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <span class="brand-mark">秒</span>
        <div>
          <h2>秒杀运营后台</h2>
          <p>活动配置 · 库存调整 · 订单查询 · 实时看板</p>
        </div>
      </div>

      <el-alert
        v-if="denied"
        type="warning"
        :closable="false"
        show-icon
        title="当前账号不是管理员，无法进入后台"
        class="denied"
      />

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" size="large" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="tip">演示账号：admin / 123456（ADMIN 角色）</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const denied = ref(route.query.denied === '1')

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    const data = res.data

    // 先判断角色再落 token：普通用户登录后台是常见误操作，
    // 存了 token 再把他踢回登录页，会出现「刚登录又跳回来」的循环观感
    if (data.role !== 'ADMIN') {
      ElMessage.error('该账号不是管理员，无法登录后台')
      denied.value = true
      return
    }

    userStore.setLogin(data)
    ElMessage.success(`欢迎，${data.nickname || data.username}`)
    const redirect = route.query.redirect
    router.replace(redirect && redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (e) {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #304156 55%, #3a4a5e 100%);
}

.login-box {
  width: 100%;
  max-width: 390px;
  background: #fff;
  border-radius: 12px;
  padding: 34px 32px 22px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.28);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: #e93b3d;
  color: #fff;
  font-size: 21px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h2 {
  font-size: 17px;
  margin-bottom: 3px;
}

.brand p {
  font-size: 12px;
  color: #909399;
}

.denied {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
}

.tip {
  margin-top: 16px;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}
</style>
