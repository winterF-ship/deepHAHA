<template>
  <div id="forum-app" :class="{ 'auth-shell': route.meta.authPage }">
    <NavBar v-if="!route.meta.authPage" />
    <main :class="route.meta.authPage ? 'auth-content' : 'main-content'">
      <router-view v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </router-view>
    </main>
    <nav v-if="!route.meta.authPage" class="mobile-tabbar" aria-label="移动端底部导航">
      <button
        v-for="item in mobileTabs"
        :key="item.key"
        type="button"
        class="mobile-tab"
        :class="{ active: item.active() }"
        @click="goMobileTab(item)"
      >
        <el-icon class="mobile-tab-icon"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { HomeFilled, EditPen, ChatDotSquare, Cpu, User } from '@element-plus/icons-vue'
import NavBar from './components/NavBar.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const mobileTabs = computed(() => {
  const tabs = [
    {
      key: 'home',
      label: '首页',
      to: '/',
      icon: HomeFilled,
      active: () => route.path === '/' || route.path.startsWith('/category') || route.path.startsWith('/post/')
    },
    {
      key: 'post',
      label: '发帖',
      to: store.isLoggedIn() ? '/post/new' : '/login?redirect=/post/new',
      icon: EditPen,
      active: () => route.path === '/post/new'
    },
    {
      key: 'messages',
      label: '消息',
      to: store.isLoggedIn() ? '/messages' : '/login?redirect=/messages',
      icon: ChatDotSquare,
      active: () => route.path.startsWith('/messages')
    }
  ]

  if (store.userInfo?.role === 'ADMIN' || store.userInfo?.role === 'SUPERVISOR') {
    tabs.push({
      key: 'bots',
      label: 'AI角色',
      to: '/admin/bots',
      icon: Cpu,
      active: () => route.path.startsWith('/admin/bots')
    })
  }

  tabs.push({
    key: 'me',
    label: '我的',
    to: store.isLoggedIn() ? '/profile?tab=posts' : '/login',
    icon: User,
    active: () => route.path.startsWith('/profile')
  })

  return tabs
})

function goMobileTab(item) {
  router.push(item.to)
}

onMounted(() => {
  store.refreshUserInfo()
})
</script>

<style>
:root {
  --primary: #6366f1;
  --primary-light: #818cf8;
  --primary-soft: rgba(99, 102, 241, 0.08);
  --primary-glow: rgba(99, 102, 241, 0.16);
  --bg: #f8fafc;
  --bg-card: #ffffff;
  --text: #1e293b;
  --text-secondary: #64748b;
  --text-muted: #94a3b8;
  --border: #e2e8f0;
  --border-light: #f1f5f9;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 4px 6px rgba(15, 23, 42, 0.04), 0 2px 4px rgba(15, 23, 42, 0.03);
  --shadow-lg: 0 10px 25px rgba(15, 23, 42, 0.06), 0 4px 10px rgba(15, 23, 42, 0.03);
  --radius-sm: 10px;
  --radius: 16px;
  --radius-lg: 22px;
  --transition: 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --mobile-tabbar-height: 64px;

  /* Category colors */
  --c-indigo: #6366f1;   --c-indigo-soft: rgba(99,102,241,0.10);   --c-indigo-glow: rgba(99,102,241,0.18);
  --c-amber: #f59e0b;    --c-amber-soft: rgba(245,158,11,0.10);    --c-amber-glow: rgba(245,158,11,0.18);
  --c-emerald: #10b981;  --c-emerald-soft: rgba(16,185,129,0.10);  --c-emerald-glow: rgba(16,185,129,0.18);
  --c-rose: #f43f5e;     --c-rose-soft: rgba(244,63,94,0.10);      --c-rose-glow: rgba(244,63,94,0.18);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

html, body, #app { min-height: 100%; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
  background: var(--bg);
  color: var(--text);
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#forum-app { min-height: 100vh; }

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.el-button {
  transition:
    transform 180ms ease,
    filter 180ms ease,
    box-shadow 180ms ease,
    background-color 180ms ease,
    border-color 180ms ease,
    color 180ms ease;
}
.el-button:hover {
  filter: brightness(1.03);
}
.el-button:active {
  transform: scale(0.98);
}

.auth-shell {
  background:
    radial-gradient(circle at 18% 12%, var(--c-indigo-glow), transparent 28%),
    radial-gradient(circle at 88% 18%, var(--c-amber-glow), transparent 24%),
    radial-gradient(circle at 62% 90%, var(--c-emerald-glow), transparent 26%),
    radial-gradient(circle at 82% 82%, var(--c-rose-glow), transparent 22%),
    linear-gradient(160deg, #eef1fa 0%, #f6f8fd 48%, #f1f4fb 100%);
}

.main-content {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

.auth-content {
  min-height: 100vh;
}

a { text-decoration: none; color: inherit; }

/* Element Plus global overrides */
.el-button--primary {
  --el-button-bg-color: var(--primary);
  --el-button-border-color: var(--primary);
  --el-button-hover-bg-color: var(--primary-light);
  --el-button-hover-border-color: var(--primary-light);
  --el-button-active-bg-color: var(--primary);
  --el-button-active-border-color: var(--primary);
}
.el-tag--primary {
  --el-tag-bg-color: var(--primary-soft);
  --el-tag-border-color: transparent;
  --el-tag-text-color: var(--primary);
}

/* responsive */
@media (max-width: 768px) {
  .main-content {
    padding: 16px 12px calc(var(--mobile-tabbar-height) + 24px + env(safe-area-inset-bottom));
  }

  .el-dialog {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px);
    margin-top: 8vh !important;
  }

  .el-message-box {
    width: calc(100vw - 32px) !important;
    max-width: calc(100vw - 32px);
  }
}

.mobile-tabbar {
  display: none;
}

@media (max-width: 768px) {
  .mobile-tabbar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 1000;
    min-height: var(--mobile-tabbar-height);
    padding: 6px 10px calc(6px + env(safe-area-inset-bottom));
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
    gap: 4px;
    background: rgba(255, 255, 255, 0.94);
    border-top: 1px solid rgba(226, 232, 240, 0.86);
    box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.08);
    backdrop-filter: blur(16px) saturate(180%);
    -webkit-backdrop-filter: blur(16px) saturate(180%);
  }

  .mobile-tab {
    min-width: 0;
    min-height: 48px;
    border: none;
    border-radius: 14px;
    background: transparent;
    color: var(--text-muted);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    font: inherit;
    font-size: 11px;
    font-weight: 650;
    cursor: pointer;
    transition: background-color 180ms ease, color 180ms ease, transform 160ms ease;
  }

  .mobile-tab:active {
    transform: scale(0.97);
  }

  .mobile-tab-icon {
    font-size: 19px;
  }

  .mobile-tab.active {
    color: var(--primary);
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(14, 165, 233, 0.08));
  }
}

@media (hover: none) {
  .el-button:hover {
    filter: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 1ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 1ms !important;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    transform: none;
  }
}
</style>
