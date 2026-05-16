<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="brand-row">
        <div class="brand-mark">D</div>
        <div>
          <p class="eyebrow">DeepHaha Forum</p>
          <h1>创建账号</h1>
        </div>
      </div>

      <el-form class="auth-form" :model="form" @submit.prevent="submit" label-position="top">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名（2-20个字符）" clearable :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" placeholder="密码（至少4位）" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" size="large" placeholder="邮箱" clearable :prefix-icon="Message" />
        </el-form-item>
        <el-button class="submit-btn" type="primary" size="large" @click="submit" :loading="loading">
          注册
        </el-button>
      </el-form>

      <p class="tips">已有账号？<router-link to="/login">去登录</router-link></p>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { register } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ username: '', password: '', email: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.username.trim()) { ElMessage.warning('用户名不能为空'); return }
  if (form.value.username.trim().length < 2) { ElMessage.warning('用户名至少2个字符'); return }
  if (!form.value.password) { ElMessage.warning('密码不能为空'); return }
  if (form.value.password.length < 4) { ElMessage.warning('密码至少4位'); return }
  if (!form.value.email.trim()) { ElMessage.warning('邮箱不能为空'); return }
  loading.value = true
  try {
    await register(form.value.username.trim(), form.value.password, form.value.email.trim())
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally { loading.value = false }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  padding: 32px 20px;
}
.auth-card {
  width: min(420px, 100%);
  background: rgba(255,255,255,0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 36px 36px 32px;
}
.brand-row { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
.brand-mark {
  width: 46px; height: 46px; border-radius: 12px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, #5b6eeb, #8b5cf6);
  color: #fff; font-size: 22px; font-weight: 800;
  box-shadow: 0 4px 12px rgba(91,110,235,0.30);
}
.eyebrow { color: var(--text-muted); font-size: 12px; line-height: 1.2; margin-bottom: 4px; letter-spacing: 0.5px; }
h1 { color: var(--text); font-size: 26px; line-height: 1.2; font-weight: 750; letter-spacing: -0.3px; }
.auth-form { width: 100%; }
.auth-form :deep(.el-form-item) { margin-bottom: 16px; }
.auth-form :deep(.el-input__wrapper) {
  min-height: 48px; border-radius: 10px; box-shadow: none;
  border: 1.5px solid var(--border); transition: all var(--transition);
}
.auth-form :deep(.el-input__wrapper:hover) { border-color: #c0c8d6; }
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow);
}
.auth-form :deep(.el-input__inner) { font-size: 15px; line-height: 48px; }
.auth-form :deep(.el-input__prefix) { color: var(--text-muted); margin-right: 8px; }
.submit-btn {
  width: 100%; height: 48px; border-radius: 10px; font-size: 16px; font-weight: 700;
  background: linear-gradient(135deg, #5b6eeb, #7c5ce7) !important;
  border: none !important; margin-top: 4px;
  box-shadow: 0 4px 14px rgba(91,110,235,0.28);
  transition: all var(--transition);
}
.submit-btn:hover { box-shadow: 0 6px 20px rgba(91,110,235,0.36); transform: translateY(-1px); }
.tips { text-align: center; font-size: 14px; color: var(--text-secondary); margin-top: 22px; }
.tips a { color: var(--primary); font-weight: 700; }

@media (max-width: 520px) {
  .auth-page {
    align-items: center;
    min-height: 100dvh;
    padding: 24px 12px;
  }
  .auth-card { padding: 24px 18px 22px; border-radius: var(--radius); }
  .brand-row { gap: 12px; margin-bottom: 22px; }
  .brand-mark { width: 40px; height: 40px; font-size: 20px; border-radius: 11px; }
  h1 { font-size: 22px; }
}
</style>
