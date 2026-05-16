<template>
  <div class="home">

    <!-- Notice -->
    <div v-if="notice" class="notice-bar">
      <div class="notice-content">
        <span class="notice-icon">📢</span>
        <span class="notice-text">{{ notice.content }}</span>
      </div>
      <el-button v-if="store.userInfo?.role === 'ADMIN' || store.userInfo?.role === 'SUPERVISOR'" type="warning" size="small" plain @click="noticeDraft = notice?.content || ''; showNoticeEdit = true" class="notice-edit-btn">
        编辑公告
      </el-button>
    </div>

    <!-- Notice Edit Dialog -->
    <el-dialog v-model="showNoticeEdit" title="编辑公告" width="480px">
      <el-input v-model="noticeDraft" type="textarea" :rows="4" placeholder="请输入公告内容" />
      <template #footer>
        <el-button @click="showNoticeEdit = false">取消</el-button>
        <el-button type="primary" @click="saveNotice" :loading="savingNotice">保存</el-button>
      </template>
    </el-dialog>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-glow hero-glow-1"></div>
      <div class="hero-glow hero-glow-2"></div>
      <div class="hero-glow hero-glow-3"></div>
      <div class="hero-inner">
        <div class="hero-badge">社区</div>
        <h1 class="hero-title">欢迎来到 DeepHaha 论坛</h1>
        <p class="hero-desc">分享技术见解、发现优质资源、结交志同道合的朋友</p>

        <!-- 社区数据 -->
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-value">{{ totalPosts }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ categories.length }}</span>
            <span class="stat-label">板块</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ myPostsCount }}</span>
            <span class="stat-label">我的帖子</span>
          </div>
        </div>

        <router-link to="/post/new" v-if="store.isLoggedIn()" class="hero-cta">
          <span>+</span> 发布新帖
        </router-link>
        <router-link to="/register" v-else class="hero-cta">立即加入</router-link>
      </div>
    </section>

    <!-- 板块 -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">探索板块</h2>
      </div>
      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="cat-card"
          :style="{ '--cat-color': catColor(cat.id), '--cat-soft': catSoft(cat.id) }"
          @click="$router.push(`/category/${cat.id}`)"
        >
          <div class="cat-icon" :style="{ background: catGradient(cat.id) }">
            {{ catEmoji(cat.name) }}
          </div>
          <div class="cat-body">
            <h3 class="cat-name">{{ cat.name }}</h3>
            <p class="cat-desc">{{ cat.description }}</p>
          </div>
          <el-icon class="cat-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- 最新帖子 + 侧边栏 -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">最新帖子</h2>
      </div>

      <div class="content-layout">
        <!-- 左侧：帖子列表 -->
        <div class="content-main">
          <TransitionGroup v-if="posts.length" name="post-list-motion" tag="div" class="post-list">
            <div
              v-for="post in posts"
              :key="post.id"
              class="post-card"
              @click="$router.push(`/post/${post.id}`)"
            >
              <div class="post-card-left">
                <span class="post-tag" :style="{ color: catColor(post.categoryId), background: catSoft(post.categoryId) }">{{ post.categoryName }}</span>
                <h3 class="post-title">{{ post.title }}</h3>
                <p class="post-excerpt">{{ post.content }}</p>
              </div>
              <div class="post-card-right">
                <div class="post-author">
                  <span class="author-avatar-sm" @click.stop="openUserCard(post.authorId)">
                    <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
                    <span v-else>{{ post.authorName?.charAt(0) }}</span>
                  </span>
                  <span class="author-name" @click.stop="openUserCard(post.authorId)">{{ post.authorName }}</span>
                  <span v-if="post.authorDisplayTitle" class="title-badge" :class="titleClass(post.authorRole)">{{ post.authorDisplayTitle }}</span>
                </div>
                <div class="post-nums">
                  <span class="num-item">{{ post.likeCount }}<small>点赞</small></span>
                  <span class="num-item">{{ post.replyCount }}<small>回复</small></span>
                  <span class="num-item">{{ post.viewCount }}<small>阅读</small></span>
                </div>
                <span class="post-time">{{ formatTime(post.createdAt) }}</span>
                <el-button v-if="canDelete(post)" type="danger" size="small" plain @click.stop="handleDelete(post)" class="home-delete-btn">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </TransitionGroup>

          <div v-else class="empty-state">
            <div class="empty-icon">📝</div>
            <h3>还没有帖子</h3>
            <p>成为第一个发帖的人吧</p>
          </div>
        </div>

        <!-- 右侧：侧边栏 -->
        <aside class="sidebar">
          <!-- 公告 -->
          <div class="side-card">
            <h4 class="side-card-title">📢 论坛公告</h4>
            <p class="side-card-text">欢迎来到 DeepHaha 论坛！在这里你可以分享技术见解、发现优质资源、结交志同道合的朋友。请遵守社区规范，友善交流。</p>
          </div>

          <!-- 热门板块 -->
          <div class="side-card">
            <h4 class="side-card-title">🔥 热门板块</h4>
            <div class="side-tags">
              <router-link
                v-for="cat in categories"
                :key="cat.id"
                :to="`/category/${cat.id}`"
                class="side-tag"
              >{{ cat.name }}</router-link>
            </div>
          </div>

          <!-- 发帖入口 / 登录提示 -->
          <div v-if="store.isLoggedIn()" class="side-card side-cta-card">
            <h4 class="side-card-title">💡 有想法要分享？</h4>
            <p class="side-card-text">把你的技术经验、学习心得或者有趣的话题分享给大家吧。</p>
            <router-link to="/post/new" class="side-cta-btn">发布新帖</router-link>
          </div>
          <div v-else class="side-card side-cta-card">
            <h4 class="side-card-title">👋 新朋友？</h4>
            <p class="side-card-text">加入 DeepHaha 论坛，和社区一起交流成长。</p>
            <div class="side-cta-row">
              <router-link to="/register" class="side-cta-btn">注册账号</router-link>
              <router-link to="/login" class="side-link">已有账号？登录</router-link>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <UserCard v-model="showUserCard" :user="cardUser" @refresh="loadPosts" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight, Delete } from '@element-plus/icons-vue'
import { getCategories } from '../api/category'
import { getPosts, deletePost } from '../api/post'
import { getNotice, updateNotice, getUsers } from '../api/auth'
import { useUserStore } from '../stores/user'
import { canDeleteContent } from '../utils/permission'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserCard from '../components/UserCard.vue'

const store = useUserStore()
const categories = ref([])
const posts = ref([])
const totalPosts = ref(0)
const myPostsCount = ref(0)
const notice = ref(null)
const showNoticeEdit = ref(false)
const noticeDraft = ref('')
const savingNotice = ref(false)
const showUserCard = ref(false)
const cardUser = ref(null)

const catIcons = { '技术讨论': '💻', '资源共享': '📦', '闲聊灌水': '☕', '公告反馈': '📢' }
// 四个板块各用一个独立色系
const catColors = [
  { color: 'var(--c-indigo)', soft: 'var(--c-indigo-soft)', gradient: 'linear-gradient(135deg, #6366f1, #7c3aed)' },
  { color: 'var(--c-amber)',  soft: 'var(--c-amber-soft)',  gradient: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  { color: 'var(--c-emerald)', soft: 'var(--c-emerald-soft)', gradient: 'linear-gradient(135deg, #10b981, #059669)' },
  { color: 'var(--c-rose)',   soft: 'var(--c-rose-soft)',   gradient: 'linear-gradient(135deg, #f43f5e, #e11d48)' },
]

function catEmoji(name) { return catIcons[name] || '📌' }
function catGradient(id) { return catColors[(id - 1) % 4].gradient }
function catColor(id) { return catColors[(id - 1) % 4].color }
function catSoft(id) { return catColors[(id - 1) % 4].soft }

async function loadPosts() {
  const postRes = await getPosts(null, 0, 10)
  posts.value = postRes.data.content
  totalPosts.value = postRes.data.totalElements
  if (store.userInfo?.id) {
    try {
      let count = 0
      for (let page = 0; page < 5; page++) {
        const res = await getPosts(null, page, 20)
        const mine = (res.data.content || []).filter(p => p.authorId === store.userInfo.id)
        count += mine.length
        if ((res.data.content || []).length < 20) break
      }
      myPostsCount.value = count
    } catch (e) { /* ignore */ }
  }
}

onMounted(async () => {
  const catsRes = await getCategories()
  categories.value = catsRes.data
  loadPosts()
  loadNotice()
})

async function loadNotice() {
  try {
    const res = await getNotice()
    notice.value = res.data
  } catch { /* ignore */ }
}

async function saveNotice() {
  savingNotice.value = true
  try {
    await updateNotice(noticeDraft.value)
    ElMessage.success('公告更新成功')
    showNoticeEdit.value = false
    loadNotice()
  } finally { savingNotice.value = false }
}

function canDelete(post) {
  return canDeleteContent(store.userInfo, {
    id: post.authorId,
    role: post.authorRole,
    isBot: post.authorIsBot
  })
}

async function openUserCard(userId) {
  const post = posts.value.find(p => p.authorId === userId)
  const base = post ? {
    id: post.authorId,
    username: post.authorName,
    avatar: post.authorAvatar,
    role: post.authorRole,
    displayTitle: post.authorDisplayTitle,
    muted: post.authorMuted,
    muteEndTime: post.authorMuteEndTime,
    muteReason: post.authorMuteReason
  } : { id: userId }
  if (store.userInfo?.role === 'ADMIN') {
    try {
      const res = await getUsers()
      cardUser.value = res.data.find(u => u.id === userId) || base
    } catch { cardUser.value = base }
  } else {
    cardUser.value = base
  }
  showUserCard.value = true
}

function titleClass(role) {
  if (role === 'ADMIN') return 'title-admin'
  if (role === 'SUPERVISOR') return 'title-supervisor'
  return ''
}

async function handleDelete(post) {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？', '确认删除', { type: 'warning' })
    await deletePost(post.id)
    ElMessage.success('删除成功')
    loadPosts()
  } catch { /* cancelled or error */ }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return t.split('T')[0]
}
</script>

<style scoped>
/* ========== Hero ========== */
.hero {
  position: relative;
  text-align: center;
  padding: 48px 20px 40px;
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  margin-bottom: 8px;
}
.hero-glow {
  position: absolute; pointer-events: none;
}
.hero-glow-1 {
  top: -60px; left: 50%; transform: translateX(-50%);
  width: 500px; height: 180px;
  background: radial-gradient(ellipse, var(--c-indigo-glow) 0%, transparent 70%);
}
.hero-glow-2 {
  top: 20px; right: -40px;
  width: 280px; height: 120px;
  background: radial-gradient(ellipse, var(--c-amber-glow) 0%, transparent 70%);
}
.hero-glow-3 {
  bottom: -20px; left: -60px;
  width: 260px; height: 130px;
  background: radial-gradient(ellipse, var(--c-emerald-glow) 0%, transparent 70%);
}
.hero-inner { position: relative; z-index: 1; }
.hero-badge {
  display: inline-block; padding: 4px 16px; border-radius: 20px;
  background: var(--primary-soft); color: var(--primary);
  font-size: 12px; font-weight: 650; letter-spacing: 1px; margin-bottom: 16px;
}
.hero-title {
  font-size: 30px; font-weight: 800; color: var(--text);
  letter-spacing: -0.5px; margin-bottom: 8px;
}
.hero-desc {
  font-size: 15px; color: var(--text-secondary); margin-bottom: 28px;
}
.hero-stats {
  display: flex; align-items: center; justify-content: center; gap: 0;
  margin-bottom: 28px;
}
.stat-item { text-align: center; padding: 0 28px; }
.stat-value {
  display: block; font-size: 26px; font-weight: 800;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.stat-divider {
  width: 1px; height: 32px; background: var(--border); flex-shrink: 0;
}
.hero-cta {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 11px 28px; border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #f43f5e);
  color: #fff; font-size: 15px; font-weight: 650;
  box-shadow: 0 4px 18px rgba(99,102,241,0.32);
  transition: all var(--transition);
}
.hero-cta:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99,102,241,0.40); }
.hero-cta span { font-size: 18px; font-weight: 700; }

/* ========== Notice ========== */
.notice-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 18px; margin-bottom: 8px;
  background: linear-gradient(135deg, #fef3c7, #fef9c3, #fffbeb);
  border: 1px solid rgba(245, 158, 11, 0.2); border-radius: var(--radius);
  box-shadow: 0 1px 4px rgba(245, 158, 11, 0.08);
}
.notice-content { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.notice-icon { font-size: 18px; flex-shrink: 0; }
.notice-text { font-size: 14px; color: #92400e; line-height: 1.5; }
.notice-edit-btn { flex-shrink: 0; margin-left: 12px; }

/* ========== Section ========== */
.section { margin-top: 36px; }
.section-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.section-title { font-size: 17px; font-weight: 700; color: var(--text); }

/* ========== Category Cards ========== */
.category-grid {
  display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px;
  align-items: stretch;
}
.cat-card {
  background: var(--bg-card); border-radius: var(--radius); padding: 20px;
  cursor: pointer; display: flex; align-items: center; gap: 14px;
  width: 100%; min-width: 0; min-height: 126px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.cat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px var(--cat-soft), var(--shadow-md);
  border-color: var(--cat-color);
}
.cat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: grid; place-items: center; font-size: 20px; flex-shrink: 0;
}
.cat-body { flex: 1; min-width: 0; }
.cat-name { font-size: 15px; font-weight: 650; color: var(--text); margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cat-desc { font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.cat-arrow {
  color: var(--text-muted); font-size: 16px; flex: 0 0 16px;
  opacity: 0; transform: translateX(-6px); transition: all var(--transition);
}
.cat-card:hover .cat-arrow { opacity: 1; transform: translateX(0); color: var(--cat-color); }

/* ========== Layout ========== */
.content-layout { display: flex; gap: 24px; align-items: flex-start; }
.content-main { flex: 1; min-width: 0; }

/* ========== Post List ========== */
.post-list { display: flex; flex-direction: column; gap: 10px; }
.post-list-motion-enter-active,
.post-list-motion-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}
.post-list-motion-enter-from,
.post-list-motion-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
.post-card {
  background: var(--bg-card); border-radius: var(--radius);
  padding: 18px 22px; cursor: pointer; display: flex; gap: 20px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.post-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}
.post-card-left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.post-tag {
  font-size: 11px; font-weight: 650; padding: 2px 10px; border-radius: 6px;
  align-self: flex-start;
}
.post-title {
  font-size: 15px; font-weight: 650; color: var(--text); line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
}
.post-excerpt {
  font-size: 13px; color: var(--text-muted); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.post-card-right {
  display: flex; flex-direction: column; align-items: flex-end; gap: 8px;
  flex-shrink: 0; min-width: 90px;
}
.post-author { display: flex; align-items: center; gap: 6px; }
.author-avatar-sm {
  width: 20px; height: 20px; border-radius: 6px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-size: 10px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden; flex-shrink: 0;
  cursor: pointer;
}
.author-avatar-sm img { width: 100%; height: 100%; object-fit: cover; display: block; }
.author-name { font-size: 12px; color: var(--text-secondary); font-weight: 500; cursor: pointer; }
.author-name:hover { color: var(--primary); }
.title-badge {
  max-width: 92px; padding: 1px 6px; border-radius: 5px;
  font-size: 10px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-admin { color: #be123c; background: #ffe4ec; border: 1px solid #fecdd3; }
.title-supervisor { color: #5b21b6; background: #ede9fe; border: 1px solid #ddd6fe; }
.post-nums { display: flex; gap: 12px; }
.num-item { font-size: 16px; font-weight: 700; color: var(--text); line-height: 1; }
.num-item small { display: block; font-size: 10px; color: var(--text-muted); font-weight: 400; margin-top: 1px; }
.post-time { font-size: 11px; color: var(--text-muted); }
.home-delete-btn { font-size: 11px; padding: 2px 8px; border-radius: 6px; }

/* ========== Sidebar ========== */
.sidebar { width: 280px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; }
.side-card {
  background: var(--bg-card); border-radius: var(--radius);
  padding: 20px; border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}
.side-card-title { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 10px; }
.side-card-text { font-size: 13px; color: var(--text-secondary); line-height: 1.65; }
.side-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.side-tag {
  padding: 5px 14px; border-radius: 8px;
  font-size: 13px; color: var(--text-secondary); background: var(--bg);
  border: 1px solid var(--border-light); transition: all var(--transition);
}
.side-tag:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.side-cta-card { background: linear-gradient(135deg, #f5f3ff, #fffbeb, #ecfdf5); border-color: rgba(99,102,241,0.10); }
.side-cta-btn {
  display: block; text-align: center; padding: 9px 18px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-size: 13px; font-weight: 650;
  box-shadow: 0 2px 8px rgba(99,102,241,0.22);
  transition: all var(--transition); margin-top: 12px;
}
.side-cta-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(99,102,241,0.30); }
.side-cta-row { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; align-items: center; }
.side-link { font-size: 12px; color: var(--text-muted); }
.side-link:hover { color: var(--primary); }

/* ========== Empty ========== */
.empty-state {
  text-align: center; padding: 40px 20px;
  background: var(--bg-card); border-radius: var(--radius);
  border: 1px dashed var(--border);
}
.empty-icon { font-size: 40px; margin-bottom: 12px; }
.empty-state h3 { font-size: 15px; font-weight: 650; color: var(--text); margin-bottom: 4px; }
.empty-state p { font-size: 13px; color: var(--text-muted); }

/* ========== Mobile ========== */
@media (max-width: 768px) {
  .notice-bar { align-items: flex-start; gap: 10px; padding: 12px 14px; }
  .notice-edit-btn { margin-left: 0; }
  .hero { padding: 36px 16px 32px; border-radius: var(--radius); }
  .hero-title { font-size: 22px; }
  .hero-desc { font-size: 13px; margin-bottom: 20px; }
  .hero-stats {
    width: min(100%, 320px);
    margin: 0 auto 22px;
  }
  .stat-item {
    flex: 1;
    padding: 0 10px;
  }
  .stat-value { font-size: 22px; }
  .stat-divider { height: 24px; }
  .category-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
  .cat-card { padding: 14px; }
  .cat-arrow { opacity: 0.5; transform: none; }
  .content-layout { flex-direction: column; }
  .sidebar { width: 100%; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .side-card { padding: 16px; }
  .side-cta-card { grid-column: 1 / -1; }
  .post-card { flex-direction: column; padding: 16px; }
  .post-card-right { flex-direction: row; align-items: center; gap: 14px; width: 100%; }
  .post-nums { gap: 14px; }
  .section-title { font-size: 15px; }
}

@media (max-width: 520px) {
  .notice-bar { flex-direction: column; }
  .notice-edit-btn { width: 100%; }
  .hero-stats { max-width: 290px; }
  .stat-item { padding: 0 8px; }
  .category-grid { grid-template-columns: 1fr; }
  .cat-card { min-height: 96px; }
  .sidebar { grid-template-columns: 1fr; }
  .post-card-right {
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 10px;
  }
  .post-author { width: 100%; }
  .post-nums { width: 100%; justify-content: space-between; }
}
</style>
