<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <span class="brand-mark">秒</span>
        <div>
          <h1>限时秒杀</h1>
          <p>Redis 预扣 · MQ 异步落单 · 零超卖</p>
        </div>
      </div>

      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form>
          <p class="hint">还没有账号？切换到「注册」创建，或使用种子账号 linjian / 123456</p>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名（4~32 位字母数字下划线）" size="large" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（可选）" size="large" :prefix-icon="Avatar" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（6~64 位）" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="registering" @click="handleRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { login, register } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tab = ref('login')
const loading = ref(false)
const registering = ref(false)

const loginRef = ref(null)
const registerRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '只能包含字母、数字、下划线，长度 4~32', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6~64 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function afterLogin(data) {
  userStore.setLogin(data)
  ElMessage.success(`欢迎，${data.nickname || data.username}`)
  const redirect = route.query.redirect
  router.replace(redirect && redirect.startsWith('/') ? redirect : '/activities')
}

async function handleLogin() {
  await loginRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: loginForm.username, password: loginForm.password })
    afterLogin(res.data)
  } catch (e) {
    // 错误提示由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerRef.value.validate()
  registering.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || undefined
    })
    ElMessage.success('注册成功，正在登录')
    const res = await login({ username: registerForm.username, password: registerForm.password })
    afterLogin(res.data)
  } catch (e) {
    // 同上
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  background: radial-gradient(circle at 20% 20%, #ffe8e6 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, #fff3e0 0%, transparent 45%), #f5f6f7;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 14px;
  padding: 32px 32px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--sk-primary);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h1 {
  font-size: 19px;
  margin-bottom: 2px;
}

.brand p {
  font-size: 12px;
  color: var(--sk-text-sub);
}

.tabs {
  margin-top: 4px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.hint {
  margin-top: 14px;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}
</style>
