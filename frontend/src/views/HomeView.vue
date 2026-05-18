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

    <!-- Community header -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-copy">
          <div class="hero-badge">DeepHaha 论坛</div>
          <h1 class="hero-title">今天大家在聊什么？</h1>
          <p class="hero-desc">技术交流、资源分享、闲聊灌水和公告反馈，都从这里进。</p>
        </div>

        <div class="hero-panel">
          <router-link to="/post/new" v-if="store.isLoggedIn()" class="hero-cta">
            <span>+</span> 发布新帖
          </router-link>
          <router-link to="/register" v-else class="hero-cta">加入社区</router-link>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-value">{{ totalPosts }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ categories.length }}</span>
              <span class="stat-label">板块</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ myPostsCount }}</span>
              <span class="stat-label">我的</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 板块 -->
    <section class="section board-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">板块</h2>
          <p class="section-sub">先挑个地方看看，最近的帖子会露在这里。</p>
        </div>
      </div>
      <div class="category-grid">
        <div
          v-for="board in categorySummaries"
          :key="board.id"
          class="cat-card"
          :style="{ '--cat-color': catColor(board.id), '--cat-soft': catSoft(board.id) }"
          @click="$router.push(`/category/${board.id}`)"
        >
          <div class="cat-icon">
            {{ catEmoji(board.name) }}
          </div>
          <div class="cat-body">
            <div class="cat-topline">
              <h3 class="cat-name">{{ board.name }}</h3>
              <span class="cat-count">{{ board.postCount }} 帖</span>
            </div>
            <p class="cat-desc">{{ board.description }}</p>
            <p class="cat-latest">
              <span>最新</span>
              {{ board.latestPost ? board.latestPost.title : '暂无最新动态' }}
            </p>
          </div>
          <div class="cat-meta">
            <span>{{ board.replyCount }} 回复</span>
            <el-icon class="cat-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新帖子 + 侧边栏 -->
    <section class="section feed-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">最新帖子</h2>
          <p class="section-sub">看看刚刚发生的讨论。</p>
        </div>
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
              <div class="post-card-main">
                <h3 class="post-title">{{ post.title }}</h3>
                <p class="post-excerpt">{{ post.content }}</p>
                <div class="post-meta-line">
                  <span class="author-avatar-sm" @click.stop="openUserCard(post.authorId)">
                    <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
                    <span v-else>{{ post.authorName?.charAt(0) }}</span>
                  </span>
                  <span class="author-name" @click.stop="openUserCard(post.authorId)">{{ post.authorName }}</span>
                  <span v-if="post.authorDisplayTitle" class="title-badge" :class="titleClass(post.authorRole)">{{ post.authorDisplayTitle }}</span>
                  <span class="meta-dot">·</span>
                  <span class="post-tag" :style="{ color: catColor(post.categoryId), background: catSoft(post.categoryId) }">{{ post.categoryName }}</span>
                  <span class="meta-dot">·</span>
                  <span class="post-time">{{ formatTime(post.createdAt) }}</span>
                  <span class="meta-spacer"></span>
                  <span class="post-stat">{{ post.likeCount }} 点赞</span>
                  <span class="post-stat">{{ post.replyCount }} 回复</span>
                  <span class="post-stat">{{ post.viewCount }} 阅读</span>
                </div>
              </div>
              <el-button v-if="canDelete(post)" type="danger" size="small" plain @click.stop="handleDelete(post)" class="home-delete-btn">
                <el-icon><Delete /></el-icon>
              </el-button>
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
          <!-- 社区提示 -->
          <div class="side-card">
            <h4 class="side-card-title">📌 社区提示</h4>
            <ul class="side-note-list">
              <li>技术帖写清环境和报错。</li>
              <li>资源帖补上来源和适用场景。</li>
              <li>闲聊可以松弛一点，别刷屏。</li>
            </ul>
          </div>

          <!-- 热门板块 -->
          <div class="side-card">
            <h4 class="side-card-title">🔥 热门板块</h4>
            <div class="side-board-list">
              <router-link
                v-for="board in categorySummaries"
                :key="board.id"
                :to="`/category/${board.id}`"
                class="side-board"
                :style="{ '--cat-color': catColor(board.id) }"
              >
                <span class="side-board-main">
                  <strong>{{ board.name }}</strong>
                  <small>{{ board.latestPost ? board.latestPost.title : '暂无新帖' }}</small>
                </span>
                <span class="side-board-heat">{{ board.postCount + board.replyCount }}</span>
              </router-link>
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

const categorySummaries = computed(() => {
  return categories.value.map(cat => {
    const catPosts = posts.value.filter(post => post.categoryId === cat.id)
    return {
      ...cat,
      postCount: catPosts.length,
      replyCount: catPosts.reduce((sum, post) => sum + (post.replyCount || 0), 0),
      latestPost: catPosts[0] || null
    }
  })
})

const catIcons = { '技术讨论': '💻', '资源共享': '📦', '闲聊灌水': '☕', '公告反馈': '📢' }
// 四个板块各用一个独立色系
const catColors = [
  { color: 'var(--c-indigo)', soft: 'var(--c-indigo-soft)' },
  { color: 'var(--c-amber)',  soft: 'var(--c-amber-soft)' },
  { color: 'var(--c-emerald)', soft: 'var(--c-emerald-soft)' },
  { color: 'var(--c-rose)',   soft: 'var(--c-rose-soft)' },
]

function catEmoji(name) { return catIcons[name] || '📌' }
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
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--bg-card);
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  margin-bottom: 10px;
}
.hero-inner {
  position: relative; z-index: 1;
  display: flex; align-items: center; justify-content: space-between; gap: 18px;
}
.hero-copy { min-width: 0; }
.hero-badge {
  display: inline-flex; align-items: center; gap: 6px;
  color: var(--primary);
  font-size: 13px; font-weight: 750; margin-bottom: 5px;
}
.hero-title {
  font-size: 22px; font-weight: 800; color: var(--text);
  letter-spacing: 0; margin-bottom: 5px;
}
.hero-desc {
  font-size: 14px; color: #475569; line-height: 1.55;
}
.hero-panel {
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
  padding-left: 18px; border-left: 1px solid #edf0f4;
}
.hero-stats {
  display: flex; align-items: center; gap: 4px;
  padding: 5px;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #f8fafc;
}
.stat-item {
  display: inline-flex; align-items: baseline; gap: 4px;
  min-width: 0; padding: 4px 7px; border-radius: 7px;
}
.stat-item + .stat-item { border-left: 1px solid #e8edf3; border-radius: 0; }
.stat-value {
  display: inline; font-size: 15px; font-weight: 800; color: #202637;
}
.stat-label { display: inline; font-size: 12px; color: #64748b; margin-top: 0; white-space: nowrap; }
.hero-cta {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 14px; border-radius: 9px;
  background: var(--primary);
  color: #fff; font-size: 14px; font-weight: 700;
  box-shadow: none;
  transition: all var(--transition);
  white-space: nowrap;
}
.hero-cta:hover { background: var(--primary-hover); transform: translateY(-1px); }
.hero-cta span { font-size: 18px; font-weight: 700; }

/* ========== Notice ========== */
.notice-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 13px; margin-bottom: 10px;
  background: #fffaf0;
  border: 1px solid #f3e3bd; border-radius: 10px;
  box-shadow: none;
}
.notice-content { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.notice-icon { font-size: 14px; flex-shrink: 0; opacity: 0.72; }
.notice-text { font-size: 13px; color: #6f4f16; line-height: 1.45; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-edit-btn { flex-shrink: 0; margin-left: 10px; opacity: 0.74; }
.notice-edit-btn:hover { opacity: 1; }

/* ========== Section ========== */
.section { margin-top: 24px; }
.section-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.section-title { font-size: 17px; font-weight: 700; color: var(--text); }
.section-sub { margin-top: 2px; font-size: 12px; color: #64748b; }

/* ========== Category Cards ========== */
.category-grid {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px;
  align-items: stretch;
}
.cat-card {
  background: var(--bg-card); border-radius: 12px; padding: 13px;
  cursor: pointer; display: flex; align-items: center; gap: 14px;
  width: 100%; min-width: 0; min-height: 112px;
  border: 1px solid #dbe3ee;
  border-left: 3px solid var(--cat-color);
  box-shadow: none;
  transition: all var(--transition);
}
.cat-card:nth-child(2n) { background: #fffdfa; }
.cat-card:nth-child(3n) { background: #fbfffd; }
.cat-card:nth-child(4n) { background: #fffafb; }
.cat-card:hover {
  transform: translateY(-1px);
  background: #fbfcff;
  border-color: color-mix(in srgb, var(--cat-color) 44%, #dbe3ee);
}
.cat-icon {
  width: 34px; height: 34px; border-radius: 9px;
  display: grid; place-items: center; font-size: 16px; flex-shrink: 0;
  color: var(--cat-color); background: var(--cat-soft);
  border: 1px solid color-mix(in srgb, var(--cat-color) 18%, transparent);
}
.cat-body { flex: 1; min-width: 0; }
.cat-topline { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 2px; }
.cat-name { font-size: 15px; font-weight: 700; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cat-count { flex-shrink: 0; font-size: 11px; color: #64748b; }
.cat-desc { font-size: 12px; color: #4f5f72; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.cat-latest {
  margin-top: 8px; font-size: 12px; color: #334155;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.cat-latest span { color: #64748b; margin-right: 5px; }
.cat-meta {
  align-self: stretch; display: flex; flex-direction: column; justify-content: space-between; align-items: flex-end;
  flex: 0 0 auto; min-width: 48px; color: #64748b; font-size: 11px;
}
.cat-arrow {
  color: var(--text-muted); font-size: 16px;
  opacity: 0.48; transform: translateX(-2px); transition: all var(--transition);
}
.cat-card:hover .cat-arrow { opacity: 1; transform: translateX(0); color: var(--cat-color); }

/* ========== Layout ========== */
.content-layout { display: flex; gap: 18px; align-items: flex-start; }
.content-main { flex: 1; min-width: 0; }

/* ========== Post List ========== */
.post-list {
  display: flex; flex-direction: column; gap: 12px;
}
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
  background: var(--bg-card); border-radius: 12px;
  padding: 16px 18px; cursor: pointer; display: flex; gap: 12px;
  border: 1px solid #e8edf3;
  box-shadow: none;
  transition: background-color var(--transition), border-color var(--transition), transform var(--transition);
}
.post-card:hover {
  transform: translateY(-1px);
  box-shadow: none;
  background: #fbfcff;
  border-color: #dbe5f0;
}
.post-card-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 7px; }
.post-tag {
  font-size: 11px; font-weight: 650; padding: 1px 7px; border-radius: 999px;
  flex-shrink: 0;
  border: 1px solid rgba(91, 167, 255, 0.14);
}
.post-title {
  font-size: 16px; font-weight: 750; color: #172033; line-height: 1.38;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
}
.post-card:hover .post-title { color: var(--primary-hover); }
.post-excerpt {
  font-size: 13px; color: #5f6f83; line-height: 1.45;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.post-meta-line {
  display: flex; align-items: center; gap: 7px; flex-wrap: wrap;
  color: #64748b; font-size: 12px; line-height: 1.5;
}
.meta-dot { color: #c4ccd7; }
.meta-spacer { flex: 1 1 auto; min-width: 12px; }
.post-stat { color: #475569; white-space: nowrap; }
.post-stat:nth-last-child(2) { font-weight: 650; color: #334155; }
.author-avatar-sm {
  width: 20px; height: 20px; border-radius: 50%;
  background: var(--primary);
  color: #fff; font-size: 10px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden; flex-shrink: 0;
  cursor: pointer;
}
.author-avatar-sm img { width: 100%; height: 100%; object-fit: cover; object-position: center; display: block; }
.author-name { font-size: 12px; color: #475569; font-weight: 600; cursor: pointer; }
.author-name:hover { color: var(--primary); }
.title-badge {
  max-width: 92px; padding: 1px 6px; border-radius: 5px;
  font-size: 10px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-admin { color: #9f1239; background: #fff1f2; border: 1px solid #fecdd3; }
.title-supervisor { color: #6f8f00; background: #f4ffd8; border: 1px solid #ddf99a; }
.post-time { font-size: 12px; color: #64748b; }
.home-delete-btn { align-self: center; font-size: 11px; padding: 2px 8px; border-radius: 6px; opacity: 0.58; flex-shrink: 0; }
.home-delete-btn:hover { opacity: 1; }

/* ========== Sidebar ========== */
.sidebar { width: 260px; flex-shrink: 0; display: flex; flex-direction: column; gap: 10px; }
.side-card {
  background: var(--bg-card); border-radius: 12px;
  padding: 15px; border: 1px solid #e8edf3;
  box-shadow: none;
}
.side-card-title { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 10px; }
.side-card-text { font-size: 13px; color: #536173; line-height: 1.58; }
.side-note-list { list-style: none; display: flex; flex-direction: column; gap: 7px; }
.side-note-list li {
  position: relative; padding-left: 12px;
  color: #536173; font-size: 13px; line-height: 1.5;
}
.side-note-list li::before {
  content: ''; position: absolute; left: 0; top: 0.72em;
  width: 4px; height: 4px; border-radius: 50%; background: #94a3b8;
}
.side-board-list { display: flex; flex-direction: column; gap: 2px; }
.side-board {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0; border-top: 1px solid #eef2f7;
}
.side-board:first-child { border-top: none; padding-top: 0; }
.side-board:hover strong { color: var(--cat-color); }
.side-board-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.side-board-main strong { font-size: 13px; color: #243044; transition: color var(--transition); }
.side-board-main small { font-size: 12px; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.side-board-heat {
  min-width: 28px; padding: 2px 6px; border-radius: 999px;
  color: var(--cat-color); background: color-mix(in srgb, var(--cat-color) 9%, #fff);
  font-size: 11px; font-weight: 750; text-align: center;
}
.side-cta-card { background: #f8fafc; border-color: #e3e8ef; }
.side-cta-btn {
  display: block; text-align: center; padding: 8px 14px; border-radius: 9px;
  background: var(--primary);
  color: #fff; font-size: 13px; font-weight: 650;
  box-shadow: none;
  transition: all var(--transition); margin-top: 12px;
}
.side-cta-btn:hover { transform: translateY(-1px); background: var(--primary-hover); }
.side-cta-row { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; align-items: center; }
.side-link { font-size: 12px; color: var(--text-muted); }
.side-link:hover { color: var(--primary); }

/* ========== Empty ========== */
.empty-state {
  text-align: center; padding: 40px 20px;
  background: var(--bg-card); border-radius: 12px;
  border: 1px dashed var(--border);
}
.empty-icon { font-size: 40px; margin-bottom: 12px; }
.empty-state h3 { font-size: 15px; font-weight: 650; color: var(--text); margin-bottom: 4px; }
.empty-state p { font-size: 13px; color: var(--text-muted); }

/* ========== Mobile ========== */
@media (max-width: 768px) {
  .notice-bar { align-items: flex-start; gap: 10px; padding: 12px 14px; }
  .notice-edit-btn { margin-left: 0; }
  .hero { padding: 15px; border-radius: 12px; }
  .hero-inner { flex-direction: column; align-items: stretch; gap: 14px; }
  .hero-title { font-size: 20px; }
  .hero-desc { font-size: 13px; }
  .hero-panel {
    padding-left: 0; padding-top: 12px;
    border-left: none; border-top: 1px solid #edf0f4;
    justify-content: space-between;
  }
  .hero-stats { gap: 2px; }
  .stat-item {
    flex: 0 0 auto;
    padding: 4px 6px;
  }
  .stat-value { font-size: 14px; }
  .category-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
  .cat-card { padding: 12px; min-height: 120px; align-items: flex-start; }
  .cat-meta { display: none; }
  .cat-arrow { opacity: 0.5; transform: none; }
  .content-layout { flex-direction: column; }
  .sidebar { width: 100%; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .side-card { padding: 14px; }
  .side-cta-card { grid-column: 1 / -1; }
  .post-card { flex-direction: column; padding: 14px; gap: 12px; }
  .home-delete-btn { align-self: flex-end; }
  .section-title { font-size: 15px; }
}

@media (max-width: 520px) {
  .notice-bar { flex-direction: column; }
  .notice-edit-btn { width: 100%; }
  .notice-text { white-space: normal; }
  .hero-panel { flex-direction: column; align-items: stretch; }
  .hero-cta { justify-content: center; }
  .hero-stats { justify-content: center; flex-wrap: wrap; }
  .category-grid { grid-template-columns: 1fr; }
  .cat-card { min-height: 100px; }
  .sidebar { grid-template-columns: 1fr; }
  .post-meta-line { gap: 6px; }
  .meta-spacer { display: none; }
  .post-stat { font-size: 11px; }
}
</style>
