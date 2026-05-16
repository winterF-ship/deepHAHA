<template>
  <div class="category-page">
    <!-- 顶部 -->
    <div class="page-top">
      <button class="back-btn" @click="$router.push('/')" :style="{ '--cat-color': catStyle(route.params.id).color }">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div>
        <h1 class="page-title">{{ categoryName || '板块' }}</h1>
        <p class="page-sub">{{ totalElements }} 篇帖子</p>
      </div>
      <router-link to="/post/new" v-if="store.isLoggedIn()" class="top-post-btn" :style="{ background: catStyle(route.params.id).gradient }">
        <el-icon><Plus /></el-icon> 发帖
      </router-link>
    </div>

    <!-- 帖子列表 -->
    <TransitionGroup v-if="posts.length" name="post-list-motion" tag="div" class="post-list">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        @click="$router.push(`/post/${post.id}`)"
      >
        <div class="post-card-body">
          <h3 class="post-title">{{ post.title }}</h3>
          <div class="post-meta">
            <span class="meta-author" @click.stop="openUserCard(post.authorId)">
              <span class="meta-avatar">
                <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
                <span v-else>{{ post.authorName?.charAt(0) }}</span>
              </span>
              {{ post.authorName }}
              <span v-if="post.authorDisplayTitle" class="title-badge" :class="titleClass(post.authorRole)">{{ post.authorDisplayTitle }}</span>
            </span>
            <span class="meta-sep">·</span>
            <span>{{ post.likeCount }} 赞</span>
            <span class="meta-sep">·</span>
            <span>{{ post.replyCount }} 回复</span>
            <span class="meta-sep">·</span>
            <span>{{ post.viewCount }} 阅读</span>
            <span class="meta-sep">·</span>
            <span>{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <el-button v-if="canDelete(post)" type="danger" size="small" plain @click.stop="handleDelete(post)" class="cat-delete-btn">
          <el-icon><Delete /></el-icon>
        </el-button>
        <el-icon class="post-arrow"><ArrowRight /></el-icon>
      </div>
    </TransitionGroup>

    <div v-else class="empty-state">
      <div class="empty-icon">📭</div>
      <h3>本板块暂无帖子</h3>
      <p>成为第一个打破沉默的人</p>
      <router-link to="/post/new" v-if="store.isLoggedIn()" class="empty-cta">发布新帖</router-link>
      <router-link to="/login" v-else class="empty-cta">登录发帖</router-link>
    </div>

    <div class="pagination" v-if="totalPages > 1">
      <el-pagination
        layout="prev, pager, next"
        :total="totalElements"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="loadPosts" />
    </div>

    <UserCard v-model="showUserCard" :user="cardUser" @refresh="loadPosts" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Plus, Delete } from '@element-plus/icons-vue'
import { getPosts, deletePost } from '../api/post'
import { getCategories } from '../api/category'
import { getUsers } from '../api/auth'
import { useUserStore } from '../stores/user'
import { canDeleteContent } from '../utils/permission'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserCard from '../components/UserCard.vue'

const store = useUserStore()
const route = useRoute()
const posts = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 10
const currentPage = ref(1)
const categoryName = ref('')

const catColorMap = [
  { color: '#6366f1', soft: 'rgba(99,102,241,0.08)', gradient: 'linear-gradient(135deg, #6366f1, #7c3aed)' },
  { color: '#f59e0b', soft: 'rgba(245,158,11,0.08)', gradient: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  { color: '#10b981', soft: 'rgba(16,185,129,0.08)', gradient: 'linear-gradient(135deg, #10b981, #059669)' },
  { color: '#f43f5e', soft: 'rgba(244,63,94,0.08)', gradient: 'linear-gradient(135deg, #f43f5e, #e11d48)' },
]

onMounted(() => loadCategory())
watch(() => route.params.id, () => { currentPage.value = 1; loadCategory() })

async function loadCategory() {
  const catId = route.params.id
  const cats = await getCategories()
  const cat = cats.data.find(c => c.id === Number(catId))
  categoryName.value = cat ? cat.name : ''
  loadPosts()
}

function catStyle(id) { return catColorMap[(id - 1) % 4] }

const showUserCard = ref(false)
const cardUser = ref(null)

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

function canDelete(post) {
  return canDeleteContent(store.userInfo, {
    id: post.authorId,
    role: post.authorRole,
    isBot: post.authorIsBot
  })
}

async function handleDelete(post) {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？', '确认删除', { type: 'warning' })
    await deletePost(post.id)
    ElMessage.success('删除成功')
    loadPosts()
  } catch { /* cancelled or error */ }
}

async function loadPosts() {
  const res = await getPosts(route.params.id, currentPage.value - 1, pageSize)
  posts.value = res.data.content
  totalPages.value = res.data.totalPages
  totalElements.value = res.data.totalElements
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' 天前'
  return t.split('T')[0]
}
</script>

<style scoped>
.category-page { max-width: 760px; margin: 0 auto; }

/* Top */
.page-top { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.back-btn {
  width: 40px; height: 40px; border-radius: 12px; border: 1.5px solid var(--border);
  background: var(--bg-card); cursor: pointer; display: grid; place-items: center;
  font-size: 18px; color: var(--text-secondary); transition: all var(--transition);
  flex-shrink: 0;
}
.back-btn:hover { border-color: var(--cat-color); color: var(--cat-color); background: var(--cat-soft, rgba(99,102,241,0.08)); }
.page-title { font-size: 22px; font-weight: 750; color: var(--text); letter-spacing: -0.3px; }
.page-sub { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.top-post-btn {
  margin-left: auto; flex-shrink: 0;
  display: flex; align-items: center; gap: 4px;
  padding: 9px 18px; border-radius: 10px;
  color: #fff; font-size: 13px; font-weight: 600;
  box-shadow: 0 2px 8px rgba(99,102,241,0.24);
  transition: all var(--transition);
}
.top-post-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(99,102,241,0.32); }

/* Post list */
.post-list { display: flex; flex-direction: column; gap: 8px; }
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
  background: var(--bg-card); border-radius: var(--radius); padding: 18px 22px;
  cursor: pointer; display: flex; align-items: center; gap: 14px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.post-card:hover {
  transform: translateY(-1px); box-shadow: var(--shadow-md);
  border-color: transparent;
}
.post-card-body { flex: 1; min-width: 0; }
.post-title { font-size: 15px; font-weight: 600; color: var(--text); line-height: 1.4; margin-bottom: 6px; }
.post-meta { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.meta-author { color: var(--text-secondary); font-weight: 500; display: inline-flex; align-items: center; gap: 5px; cursor: pointer; }
.meta-author:hover { color: var(--primary); }
.title-badge {
  max-width: 90px; padding: 1px 6px; border-radius: 5px;
  font-size: 10px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-admin { color: #be123c; background: #ffe4ec; border: 1px solid #fecdd3; }
.title-supervisor { color: #5b21b6; background: #ede9fe; border: 1px solid #ddd6fe; }
.meta-avatar {
  width: 18px; height: 18px; border-radius: 5px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-size: 10px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden; flex-shrink: 0;
}
.meta-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.meta-sep { font-size: 8px; margin: 0 2px; }
.cat-delete-btn { font-size: 11px; padding: 2px 8px; border-radius: 6px; flex-shrink: 0; }
.post-arrow { color: var(--text-muted); font-size: 18px; opacity: 0; transform: translateX(-4px); transition: all var(--transition); flex-shrink: 0; }
.post-card:hover .post-arrow { opacity: 1; transform: translateX(0); }

/* Empty */
.empty-state {
  text-align: center; padding: 48px 20px;
  background: var(--bg-card); border-radius: var(--radius-lg);
  border: 1px dashed var(--border);
}
.empty-icon { font-size: 44px; margin-bottom: 14px; }
.empty-state h3 { font-size: 16px; font-weight: 650; color: var(--text); margin-bottom: 4px; }
.empty-state p { font-size: 14px; color: var(--text-muted); margin-bottom: 18px; }
.empty-cta {
  display: inline-block; padding: 9px 22px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #f43f5e);
  color: #fff; font-size: 14px; font-weight: 600;
  box-shadow: 0 4px 12px rgba(99,102,241,0.26);
}
.empty-cta:hover { transform: translateY(-1px); }

.pagination { display: flex; justify-content: center; margin-top: 28px; }

@media (max-width: 768px) {
  .page-top { gap: 10px; margin-bottom: 18px; }
  .post-card { padding: 14px 16px; border-radius: var(--radius-sm); }
  .post-title { font-size: 14px; }
  .page-title { font-size: 19px; }
  .top-post-btn { padding: 8px 14px; font-size: 12px; }
  .post-meta { gap: 3px; }
  .cat-delete-btn { padding: 2px 7px; }
}

@media (max-width: 520px) {
  .page-top { flex-wrap: wrap; }
  .top-post-btn { margin-left: 50px; }
  .post-card { align-items: flex-start; }
  .post-arrow { display: none; }
  .meta-author { max-width: 100%; }
}
</style>
