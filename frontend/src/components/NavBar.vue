<template>
  <header class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="logo">
        <span class="logo-mark" aria-hidden="true">
          <span class="bubble bubble-main">D</span>
          <span class="bubble bubble-dot"></span>
        </span>
        <span class="logo-copy">
          <span class="logo-text">DeepHaha</span>
          <span class="logo-sub">论坛</span>
        </span>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
        <router-link to="/messages" v-if="store.isLoggedIn()" class="nav-item" active-class="active">消息</router-link>
        <router-link to="/post/new" v-if="store.isLoggedIn()" class="nav-item nav-post-btn" active-class="active">
          <el-icon class="post-icon"><Plus /></el-icon>
          <span>发帖</span>
        </router-link>
        <router-link to="/admin/bots" v-if="store.userInfo?.role === 'ADMIN' || store.userInfo?.role === 'SUPERVISOR'" class="nav-item" active-class="active">AI 角色</router-link>
        <router-link to="/admin/users" v-if="store.userInfo?.role === 'ADMIN'" class="nav-item" active-class="active">用户管理</router-link>
      </nav>

      <div class="user-area">
        <template v-if="store.isLoggedIn()">
          <el-dropdown trigger="hover" @command="handleCommand" popper-class="user-dropdown-popper">
            <div class="user-trigger">
              <el-avatar :size="34" class="user-avatar" :src="store.userInfo?.avatar || undefined">
                {{ store.userInfo?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ store.userInfo?.username }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="dropdown-info">
                    <div class="drop-name">{{ store.userInfo?.username }}</div>
                    <div class="drop-email">{{ store.userInfo?.email }}</div>
                    <div class="drop-date">注册于 {{ formatDate(store.userInfo?.createdAt) }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="posts">我的帖子</el-dropdown-item>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item v-if="store.userInfo?.role === 'ADMIN'" command="users">用户管理</el-dropdown-item>
                <el-dropdown-item v-if="store.userInfo?.role === 'ADMIN' || store.userInfo?.role === 'SUPERVISOR'" command="bots">AI 角色管理</el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <span style="color: #ef4444;">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button class="btn-outline" size="small" @click="$router.push('/login')">登录</el-button>
          <el-button class="btn-primary" size="small" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const router = useRouter()

function handleCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } else if (cmd === 'profile') {
    router.push('/profile?tab=info')
  } else if (cmd === 'posts') {
    router.push('/profile?tab=posts')
  } else if (cmd === 'users') {
    router.push('/admin/users')
  } else if (cmd === 'bots') {
    router.push('/admin/bots')
  }
}

function formatDate(t) {
  if (!t) return ''
  return t.split('T')[0]
}
</script>

<style scoped>
.navbar {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
}
.navbar-inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 58px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-right: 36px;
  flex-shrink: 0;
}
.logo-mark {
  position: relative;
  width: 42px; height: 42px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eef2ff 0%, #ecfdf5 100%);
  border: 1px solid rgba(99, 102, 241, 0.16);
  display: grid;
  place-items: center;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}
.bubble {
  display: grid;
  place-items: center;
}
.bubble-main {
  width: 30px; height: 25px;
  border-radius: 11px 11px 11px 5px;
  background: linear-gradient(135deg, #4f46e5, #0ea5e9 52%, #10b981);
  color: #fff;
  font-weight: 850;
  font-size: 16px;
  line-height: 1;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.28);
}
.bubble-dot {
  position: absolute;
  right: 5px;
  bottom: 6px;
  width: 13px;
  height: 11px;
  border-radius: 6px 6px 3px 6px;
  background: #f59e0b;
  border: 2px solid #fff;
}
.logo-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1px;
}
.logo-text {
  font-size: 19px; font-weight: 800;
  color: var(--text);
  line-height: 1.05;
}
.logo-sub {
  font-size: 11px;
  font-weight: 650;
  color: var(--text-muted);
  line-height: 1;
}

/* Nav */
.nav-links { display: flex; gap: 4px; align-items: center; flex-shrink: 0; }
.nav-item {
  padding: 7px 15px; border-radius: 8px;
  font-size: 14px; color: var(--text-secondary);
  transition: all var(--transition); position: relative;
}
.nav-item:hover { color: var(--text); background: rgba(99, 102, 241, 0.06); }
.nav-item.active { color: var(--primary); font-weight: 600; background: transparent; }
.nav-item.active::after {
  content: ''; position: absolute; bottom: -1px; left: 50%; transform: translateX(-50%);
  width: 20px; height: 2.5px; border-radius: 2px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
}
.nav-post-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #eef2ff;
  border: 1px solid rgba(99, 102, 241, 0.18);
  color: #4f46e5 !important;
  margin-left: 6px;
  font-weight: 700;
  box-shadow: none;
}
.nav-post-btn:hover { background: #e0e7ff; color: #4338ca !important; }
.nav-item:active,
.nav-post-btn:active {
  transform: scale(0.98);
}
.nav-post-btn.active { color: #4f46e5 !important; background: #e0e7ff; }
.post-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #4f46e5;
  color: #fff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* User */
.user-area { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-shrink: 0; }
.user-trigger {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 5px 10px;
  border-radius: 10px; transition: all var(--transition);
}
.user-trigger:hover { background: rgba(99, 102, 241, 0.06); }
.user-trigger:active { transform: scale(0.98); }
.user-avatar {
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-weight: 700; font-size: 14px;
}
.username { color: var(--text); font-size: 14px; font-weight: 500; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.arrow-icon { color: var(--text-muted); font-size: 12px; transition: transform var(--transition); }
.user-trigger:hover .arrow-icon { transform: translateY(2px); }

.dropdown-info { padding: 4px 0; line-height: 1.6; min-width: 170px; }
.drop-name { font-weight: 600; color: var(--text); }
.drop-email { font-size: 12px; color: var(--text-secondary); }
.drop-date { font-size: 12px; color: var(--text-muted); }

/* Buttons */
.btn-outline {
  border: 1.5px solid var(--border);
  background: transparent; color: var(--text);
  font-weight: 500; border-radius: 8px;
  transition: all var(--transition);
}
.btn-outline:hover { border-color: var(--primary); color: var(--primary); }
.btn-primary {
  background: linear-gradient(135deg, #6366f1, #7c3aed, #f43f5e);
  border: none; color: #fff; font-weight: 600; border-radius: 8px;
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.22);
  transition: all var(--transition);
}
.btn-primary:hover { box-shadow: 0 4px 14px rgba(99, 102, 241, 0.34); opacity: 0.94; }

/* Mobile */
@media (max-width: 768px) {
  .navbar-inner { padding: 0 12px; height: 54px; gap: 8px; }
  .logo-copy { display: none; }
  .logo { margin-right: 0; }
  .logo-mark { width: 34px; height: 34px; border-radius: 13px; }
  .bubble-main { width: 25px; height: 21px; font-size: 14px; border-radius: 9px 9px 9px 4px; }
  .bubble-dot { right: 4px; bottom: 5px; width: 11px; height: 9px; }
  .nav-links { display: none; }
  .user-area { margin-left: auto; }
  .username { display: none; }
  .user-trigger { padding: 4px 6px; }
  .btn-outline,
  .btn-primary { padding: 5px 8px; }
}

@media (max-width: 420px) {
  .post-icon { width: 16px; height: 16px; font-size: 11px; }
}
</style>
