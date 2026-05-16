<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="brand-row">
        <div class="brand-mark">D</div>
        <div>
          <p class="eyebrow">DeepHaha Forum</p>
          <h1>欢迎回来</h1>
        </div>
      </div>

      <el-form class="auth-form" :model="form" @submit.prevent="submit" label-position="top">
        <el-form-item>
          <el-input
            v-model="form.username"
            size="large"
            placeholder="用户名"
            clearable
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              size="large"
              placeholder="验证码"
              maxlength="4"
              class="captcha-input"
            />
            <img :src="captchaImage" class="captcha-img" @click="fetchCaptcha" title="点击刷新验证码" />
          </div>
        </el-form-item>
        <el-button class="submit-btn" type="primary" size="large" @click="submit" :loading="loading">
          登录
        </el-button>
      </el-form>

      <p class="tips">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { login, getCaptcha } from '../api/auth'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const form = ref({ username: '', password: '', captchaCode: '' })
const loading = ref(false)
const captchaId = ref('')
const captchaImage = ref('')

async function fetchCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.imageBase64
  } catch { /* interceptor shows error */ }
}
onMounted(fetchCaptcha)

async function submit() {
  if (!form.value.username.trim()) { ElMessage.warning('用户名不能为空'); return }
  if (!form.value.password) { ElMessage.warning('密码不能为空'); return }
  if (!form.value.captchaCode.trim()) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const res = await login(form.value.username.trim(), form.value.password, captchaId.value, form.value.captchaCode.trim())
    store.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch {
    form.value.captchaCode = ''
    fetchCaptcha()
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
.captcha-row { display: flex; gap: 10px; }
.captcha-input { flex: 1; }
.captcha-input :deep(.el-input__wrapper) {
  min-height: 48px; border-radius: 10px; box-shadow: none;
  border: 1.5px solid var(--border); transition: all var(--transition);
}
.captcha-input :deep(.el-input__wrapper:hover) { border-color: #c0c8d6; }
.captcha-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow);
}
.captcha-img {
  width: 130px; height: 48px; border-radius: 10px; cursor: pointer;
  border: 1.5px solid var(--border); flex-shrink: 0;
}
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
  .captcha-row { gap: 8px; }
  .captcha-img { width: 112px; }
}

@media (max-width: 360px) {
  .captcha-row { flex-direction: column; }
  .captcha-img { width: 100%; object-fit: cover; }
}
</style>
