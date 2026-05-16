import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/HomeView.vue') },
  { path: '/category/:id', name: 'Category', component: () => import('../views/CategoryView.vue') },
  { path: '/post/:id', name: 'PostDetail', component: () => import('../views/PostDetailView.vue') },
  { path: '/messages', name: 'Messages', component: () => import('../views/MessagesView.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
  { path: '/post/new', name: 'PostCreate', component: () => import('../views/PostCreateView.vue'), meta: { requiresAuth: true } },
  { path: '/admin/bots', name: 'AdminBots', component: () => import('../views/AdminBotsView.vue'), meta: { requiresAuth: true, requiresModerator: true } },
  { path: '/admin/users', name: 'AdminUsers', component: () => import('../views/AdminUsersView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { authPage: true } },
  { path: '/register', name: 'Register', component: () => import('../views/RegisterView.vue'), meta: { authPage: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (!userInfo || userInfo.role !== 'ADMIN') {
      next({ name: 'Home' })
      return
    }
  }
  if (to.meta.requiresModerator) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (!userInfo || (userInfo.role !== 'ADMIN' && userInfo.role !== 'SUPERVISOR')) {
      next({ name: 'Home' })
      return
    }
  }
  if (token && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Home' })
    return
  }
  next()
})

export default router
