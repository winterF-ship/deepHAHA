<template>
  <div class="post-detail" v-if="post">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <router-link to="/">首页</router-link>
      <span class="bc-sep">/</span>
      <router-link :to="`/category/${post.categoryId}`">{{ post.categoryName }}</router-link>
      <span class="bc-sep">/</span>
      <span class="bc-current">帖子详情</span>
    </div>

    <!-- 帖子内容 -->
    <div class="content-card">
      <div class="card-head">
        <div class="author-row" style="cursor:pointer" @click="openUserCard(post.authorId)">
          <span class="author-avatar">
            <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
            <span v-else>{{ post.authorName?.charAt(0) }}</span>
          </span>
          <div>
            <div class="author-name-line">
              <span class="author-name">{{ post.authorName }}</span>
              <span v-if="post.authorDisplayTitle" class="title-badge" :class="titleClass(post.authorRole)">{{ post.authorDisplayTitle }}</span>
            </div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <div class="card-head-right">
          <el-button v-if="canDelete" type="danger" size="small" plain @click.stop="handleDelete" class="delete-btn">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
          <span class="tag" :style="{ color: catColor(post.categoryId).color, background: catColor(post.categoryId).soft }">{{ post.categoryName }}</span>
        </div>
      </div>

      <h1 class="post-title">{{ post.title }}</h1>
      <div class="post-body">{{ post.content }}</div>

      <div class="card-foot">
        <span class="stat action-stat" :class="{ liked: post.liked }" @click="handleLike">
          <span class="stat-icon">{{ post.liked ? '❤️' : '🤍' }}</span>
          {{ post.likeCount }} 点赞
        </span>
        <span class="stat-sep">·</span>
        <span class="stat action-stat" :class="{ favorited: post.favorited }" @click="handleFavorite">
          <span class="stat-icon">{{ post.favorited ? '⭐' : '☆' }}</span>
          {{ post.favoriteCount }} 收藏
        </span>
        <span class="stat-sep">·</span>
        <span class="stat">{{ post.viewCount }} 阅读</span>
        <span class="stat-sep">·</span>
        <span class="stat">{{ post.replyCount }} 回复</span>
      </div>
    </div>

    <!-- 回复区 -->
    <section class="section">
      <h3 class="section-title">回复 ({{ post.replyCount }})</h3>

      <TransitionGroup v-if="post.replies && post.replies.length" name="reply-motion" tag="div" class="reply-list">
        <div
          v-for="floor in post.replies"
          :key="floor.id"
          class="floor-group"
          :class="{ 'is-new-reply': newReplyIds.has(floor.id), 'is-deleting': deletingReplyIds.has(floor.id) }"
        >
          <!-- 主楼 -->
          <div class="reply-card">
            <div class="reply-head-row">
              <span class="floor-no" v-if="floor.floorNo">#{{ floor.floorNo }}</span>
              <div class="reply-head" @click="openUserCard(floor.authorId)">
                <span class="ra-avatar">
                  <img v-if="floor.authorAvatar" :src="floor.authorAvatar" :alt="floor.authorName" />
                  <span v-else>{{ floor.authorName?.charAt(0) }}</span>
                </span>
                  <div>
                    <span class="ra-name">{{ floor.authorName }}</span>
                    <span v-if="floor.authorDisplayTitle" class="title-badge" :class="titleClass(floor.authorRole)">{{ floor.authorDisplayTitle }}</span>
                    <span class="ra-time">{{ formatTime(floor.createdAt) }}</span>
                </div>
              </div>
              <div class="reply-actions">
                <el-button v-if="store.isLoggedIn()" size="small" text class="reply-btn" @click.stop="handleReplyTo(floor)">回复</el-button>
                <el-button v-if="canDeleteReply(floor)" size="small" text type="danger" class="reply-btn" @click.stop="handleDeleteReply(floor)">删除</el-button>
              </div>
            </div>
            <p class="reply-content">{{ floor.content }}</p>
          </div>

          <!-- 楼中楼 -->
          <TransitionGroup v-if="floor.subReplies && floor.subReplies.length" name="reply-motion" tag="div" class="sub-reply-list">
            <div
              v-for="sub in floor.subReplies"
              :key="sub.id"
              class="sub-reply-card"
              :class="{ 'is-new-reply': newReplyIds.has(sub.id), 'is-deleting': deletingReplyIds.has(sub.id) }"
            >
              <div class="reply-head-row">
                <div class="reply-head" @click="openUserCard(sub.authorId)">
                  <span class="ra-avatar">
                    <img v-if="sub.authorAvatar" :src="sub.authorAvatar" :alt="sub.authorName" />
                    <span v-else>{{ sub.authorName?.charAt(0) }}</span>
                  </span>
                  <div>
                    <span class="ra-name">{{ sub.authorName }}</span>
                    <span v-if="sub.authorDisplayTitle" class="title-badge" :class="titleClass(sub.authorRole)">{{ sub.authorDisplayTitle }}</span>
                    <span class="ra-time">{{ formatTime(sub.createdAt) }}</span>
                  </div>
                </div>
                <div class="reply-actions">
                  <el-button v-if="store.isLoggedIn()" size="small" text class="reply-btn" @click.stop="handleReplyTo(sub)">回复</el-button>
                  <el-button v-if="canDeleteReply(sub)" size="small" text type="danger" class="reply-btn" @click.stop="handleDeleteReply(sub)">删除</el-button>
                </div>
              </div>
              <div class="reply-to-tag">回复 @{{ sub.parentAuthorName }}</div>
              <p class="reply-content">{{ sub.content }}</p>
            </div>
          </TransitionGroup>
        </div>
      </TransitionGroup>
      <div v-else class="empty-replies">
        <div class="empty-icon">💬</div>
        <p>暂无回复，来说两句吧</p>
      </div>
    </section>

    <!-- 回复表单 -->
    <div class="reply-form" v-if="store.isLoggedIn()">
      <h4 class="form-title">写下你的回复</h4>
      <div v-if="replyingTo" class="replying-indicator">
        正在回复 @{{ replyingTo.authorName }}
        <el-button size="small" text @click="replyingTo = null">取消</el-button>
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        placeholder="写下你的回复…支持 Markdown 语法"
      />
      <div class="form-actions">
        <el-button
          type="primary"
          @click="submitReply"
          :loading="submitting"
          class="reply-submit"
        >
          {{ submitting ? '提交中…' : '提交回复' }}
        </el-button>
      </div>
    </div>
    <div v-else class="login-prompt">
      <div class="prompt-card">
        <el-icon class="prompt-icon"><Lock /></el-icon>
        <span>请先登录后再回复</span>
        <router-link to="/login" class="prompt-link">去登录</router-link>
      </div>
    </div>

    <UserCard v-model="showUserCard" :user="cardUser" @refresh="loadPost" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Delete } from '@element-plus/icons-vue'
import { getPostDetail, createReply, deleteReply, toggleLike, toggleFavorite, deletePost } from '../api/post'
import { getUsers } from '../api/auth'
import { useUserStore } from '../stores/user'
import { canDeleteContent } from '../utils/permission'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserCard from '../components/UserCard.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const post = ref(null)
const replyContent = ref('')
const submitting = ref(false)
const showUserCard = ref(false)
const cardUser = ref(null)
const replyingTo = ref(null)
const newReplyIds = ref(new Set())
const deletingReplyIds = ref(new Set())
let knownReplyIds = new Set()
let repliesReady = false
let pollTimer = null

async function loadPost() {
  const res = await getPostDetail(route.params.id)
  const nextIds = collectReplyIds(res.data?.replies || [])
  if (repliesReady) {
    const freshIds = nextIds.filter(id => !knownReplyIds.has(id))
    if (freshIds.length) {
      newReplyIds.value = new Set(freshIds)
      window.setTimeout(() => {
        newReplyIds.value = new Set()
      }, 900)
    }
  }
  knownReplyIds = new Set(nextIds)
  repliesReady = true
  post.value = res.data
}
onMounted(() => {
  loadPost()
  pollTimer = setInterval(loadPost, 30000)
})
onUnmounted(() => {
  clearInterval(pollTimer)
})

function findReplyByUserId(userId) {
  const floors = post.value?.replies
  if (!floors) return null
  for (const f of floors) {
    if (f.authorId === userId) return f
    if (f.subReplies) {
      const sub = f.subReplies.find(s => s.authorId === userId)
      if (sub) return sub
    }
  }
  return null
}

async function openUserCard(userId) {
  const base = post.value?.authorId === userId
    ? {
        id: post.value.authorId,
        username: post.value.authorName,
        avatar: post.value.authorAvatar,
        role: post.value.authorRole,
        displayTitle: post.value.authorDisplayTitle,
        muted: post.value.authorMuted,
        muteEndTime: post.value.authorMuteEndTime,
        muteReason: post.value.authorMuteReason
      }
    : (() => {
        const r = findReplyByUserId(userId)
        return r ? {
          id: r.authorId,
          username: r.authorName,
          avatar: r.authorAvatar,
          role: r.authorRole,
          displayTitle: r.authorDisplayTitle,
          muted: r.authorMuted,
          muteEndTime: r.authorMuteEndTime,
          muteReason: r.authorMuteReason
        } : { id: userId }
      })()
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

const canDelete = computed(() => {
  if (!post.value) return false
  return canDeleteContent(store.userInfo, {
    id: post.value.authorId,
    role: post.value.authorRole,
    isBot: post.value.authorIsBot
  })
})

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？', '确认删除', { type: 'warning' })
    await deletePost(post.value.id)
    ElMessage.success('删除成功')
    router.push('/')
  } catch { /* cancelled or error handled by interceptor */ }
}

function canDeleteReply(reply) {
  return canDeleteContent(store.userInfo, {
    id: reply.authorId,
    role: reply.authorRole,
    isBot: reply.authorIsBot
  })
}

async function handleDeleteReply(reply) {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？', '确认删除', { type: 'warning' })
    await deleteReply(post.value.id, reply.id)
    ElMessage.success('删除成功')
    deletingReplyIds.value = new Set([...deletingReplyIds.value, reply.id])
    window.setTimeout(() => {
      deletingReplyIds.value = new Set([...deletingReplyIds.value].filter(id => id !== reply.id))
      loadPost()
    }, 180)
  } catch { /* cancelled or error handled by interceptor */ }
}

function handleReplyTo(reply) {
  replyingTo.value = reply
}

async function handleLike() {
  if (!store.isLoggedIn()) { ElMessage.warning('请先登录'); return }
  const res = await toggleLike(post.value.id)
  post.value = res.data
}

async function handleFavorite() {
  if (!store.isLoggedIn()) { ElMessage.warning('请先登录'); return }
  const res = await toggleFavorite(post.value.id)
  post.value = res.data
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  submitting.value = true
  try {
    await createReply(post.value.id, replyContent.value, replyingTo.value?.id)
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyingTo.value = null
    loadPost()
  } finally {
    submitting.value = false
  }
}

function collectReplyIds(floors) {
  const ids = []
  for (const floor of floors || []) {
    ids.push(floor.id)
    for (const sub of floor.subReplies || []) ids.push(sub.id)
  }
  return ids
}

function titleClass(role) {
  if (role === 'ADMIN') return 'title-admin'
  if (role === 'SUPERVISOR') return 'title-supervisor'
  return ''
}

function catColor(id) {
  const colors = ['#6366f1', '#f59e0b', '#10b981', '#f43f5e']
  const softs = ['rgba(99,102,241,0.09)', 'rgba(245,158,11,0.09)', 'rgba(16,185,129,0.09)', 'rgba(244,63,94,0.09)']
  return { color: colors[(id - 1) % 4], soft: softs[(id - 1) % 4] }
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
.post-detail { max-width: 760px; margin: 0 auto; }

/* Breadcrumb */
.breadcrumb { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.breadcrumb a { color: var(--text-secondary); transition: color var(--transition); }
.breadcrumb a:hover { color: var(--primary); }
.bc-sep { color: #c0c8d6; }
.bc-current { color: var(--text); font-weight: 500; }

/* Content card */
.content-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 28px 32px; border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
}
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.card-head-right { display: flex; align-items: center; gap: 10px; }
.delete-btn { border-radius: 8px; font-size: 12px; }
.author-row { display: flex; align-items: center; gap: 10px; }
.author-avatar {
  width: 38px; height: 38px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-size: 15px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden;
}
.author-avatar img,
.ra-avatar img {
  width: 100%; height: 100%; object-fit: cover; display: block;
}
.author-name-line { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.author-name { font-size: 14px; font-weight: 650; color: var(--text); }
.post-time { font-size: 12px; color: var(--text-muted); margin-top: 1px; }
.tag {
  font-size: 11px; font-weight: 600; padding: 4px 12px; border-radius: 6px;
}
.post-title { font-size: 24px; font-weight: 750; color: var(--text); margin-bottom: 16px; line-height: 1.35; letter-spacing: -0.3px; }
.post-body {
  font-size: 15px; line-height: 1.85; color: var(--text);
  white-space: pre-wrap; word-break: break-word;
}
.card-foot { margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-light); }
.stat { font-size: 13px; color: var(--text-muted); }
.stat-sep { margin: 0 8px; color: #c0c8d6; }
.action-stat {
  cursor: pointer; user-select: none; transition: all var(--transition);
  display: inline-flex; align-items: center; gap: 4px;
}
.action-stat:hover { color: var(--text-secondary); transform: scale(1.04); }
.action-stat .stat-icon { transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); display: inline-block; }
.action-stat.liked { color: #ef4444; font-weight: 600; }
.action-stat.liked:hover { color: #dc2626; }
.action-stat.liked .stat-icon { animation: heartBeat 0.4s ease; }
.action-stat.favorited { color: #f59e0b; font-weight: 600; }
.action-stat.favorited:hover { color: #d97706; }
.action-stat.favorited .stat-icon { animation: starPop 0.4s ease; }

@keyframes heartBeat {
  0% { transform: scale(1); }
  30% { transform: scale(1.35); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}
@keyframes starPop {
  0% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.4) rotate(15deg); }
  100% { transform: scale(1) rotate(0deg); }
}

/* Section */
.section { margin-top: 32px; }
.section-title { font-size: 17px; font-weight: 700; color: var(--text); margin-bottom: 14px; }

/* Reply list */
.reply-list { display: flex; flex-direction: column; gap: 8px; }
.floor-group {
  margin-bottom: 4px;
  transition: opacity 220ms ease, transform 220ms ease, background-color 260ms ease;
}
.reply-card {
  background: var(--bg-card); border-radius: var(--radius); padding: 18px 22px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: border-color 200ms ease, box-shadow 200ms ease, background-color 260ms ease;
}
.reply-head-row { display: flex; align-items: center; gap: 10px; }
.floor-no {
  font-size: 12px; color: var(--primary); font-weight: 700;
  background: var(--primary-soft); padding: 2px 8px; border-radius: 4px;
  flex-shrink: 0;
}
.reply-head { display: flex; align-items: center; gap: 10px; flex: 1; cursor: pointer; }
.reply-actions { display: flex; gap: 2px; flex-shrink: 0; }
.reply-btn { font-size: 12px; padding: 2px 6px; }
.reply-to-tag {
  font-size: 12px; color: var(--primary); margin: 8px 0 4px;
  font-weight: 500;
}

/* Sub replies */
.sub-reply-list {
  margin-left: 48px; padding: 4px 0;
  border-left: 2px solid var(--border-light);
}
.sub-reply-card {
  padding: 12px 16px; margin: 2px 0;
  background: var(--bg-card); border-radius: var(--radius);
  border: 1px solid var(--border-light);
  transition: opacity 220ms ease, transform 220ms ease, border-color 200ms ease, box-shadow 200ms ease, background-color 260ms ease;
}
.reply-card:hover,
.sub-reply-card:hover {
  border-color: rgba(99, 102, 241, 0.18);
  box-shadow: var(--shadow-md);
}
.reply-motion-enter-active,
.reply-motion-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}
.reply-motion-enter-from,
.reply-motion-leave-to,
.floor-group.is-deleting,
.sub-reply-card.is-deleting {
  opacity: 0;
  transform: translateY(6px);
}
.floor-group.is-new-reply .reply-card,
.sub-reply-card.is-new-reply {
  animation: replyHighlight 900ms ease-out;
}

@keyframes replyHighlight {
  0% { background-color: rgba(99, 102, 241, 0.16); }
  100% { background-color: var(--bg-card); }
}
.ra-avatar {
  width: 30px; height: 30px; border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-size: 12px; font-weight: 700;
  display: grid; place-items: center; flex-shrink: 0; overflow: hidden;
}
.ra-name { font-size: 13px; font-weight: 650; color: var(--text); }
.title-badge {
  display: inline-flex; align-items: center; max-width: 100px; padding: 1px 6px;
  border-radius: 5px; font-size: 10px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-left: 6px;
}
.author-name-line .title-badge { margin-left: 0; }
.title-admin { color: #be123c; background: #ffe4ec; border: 1px solid #fecdd3; }
.title-supervisor { color: #5b21b6; background: #ede9fe; border: 1px solid #ddd6fe; }
.ra-time { font-size: 12px; color: var(--text-muted); margin-left: 10px; }
.reply-content { font-size: 14px; color: var(--text); line-height: 1.7; white-space: pre-wrap; word-break: break-word; }

/* Empty replies */
.empty-replies {
  text-align: center; padding: 32px; background: var(--bg-card);
  border-radius: var(--radius); border: 1px dashed var(--border);
}
.empty-replies .empty-icon { font-size: 32px; margin-bottom: 8px; }
.empty-replies p { font-size: 14px; color: var(--text-muted); }

/* Reply form */
.reply-form {
  margin-top: 24px; background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 24px 28px; border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
}
.form-title { font-size: 15px; font-weight: 650; color: var(--text); margin-bottom: 12px; }
.replying-indicator {
  font-size: 13px; color: var(--primary); margin-bottom: 10px;
  display: flex; align-items: center; gap: 8px;
  background: var(--primary-soft); padding: 6px 12px; border-radius: 6px;
}
.reply-form :deep(.el-textarea__inner) {
  border-radius: 10px; border: 1.5px solid var(--border);
  font-size: 14px; line-height: 1.7; padding: 12px 16px;
  transition: all var(--transition); font-family: inherit;
}
.reply-form :deep(.el-textarea__inner:focus) {
  border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow);
}
.form-actions { display: flex; justify-content: flex-end; margin-top: 14px; }
.reply-submit {
  border-radius: 10px; font-weight: 600; padding: 9px 24px;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #f43f5e) !important;
  border: none !important; box-shadow: 0 2px 10px rgba(99,102,241,0.24);
  transition: all var(--transition);
}
.reply-submit:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(99,102,241,0.32); }

/* Login prompt */
.login-prompt { margin-top: 24px; }
.prompt-card {
  background: var(--primary-soft); border-radius: var(--radius);
  padding: 16px 22px; display: flex; align-items: center; gap: 10px;
  font-size: 14px; color: var(--text-secondary);
}
.prompt-icon { font-size: 18px; color: var(--primary); }
.prompt-link { color: var(--primary); font-weight: 700; margin-left: auto; }

@media (max-width: 768px) {
  .content-card { padding: 20px 18px; border-radius: var(--radius); }
  .card-head { align-items: flex-start; gap: 12px; }
  .card-head-right { flex-direction: column-reverse; align-items: flex-end; gap: 8px; }
  .post-title { font-size: 20px; }
  .post-body { font-size: 14px; }
  .card-foot { display: flex; flex-wrap: wrap; gap: 8px; }
  .stat-sep { display: none; }
  .reply-card { padding: 14px 16px; }
  .reply-head-row { align-items: flex-start; flex-wrap: wrap; }
  .reply-head { min-width: 0; }
  .reply-actions { width: 100%; justify-content: flex-end; }
  .sub-reply-list { margin-left: 24px; }
  .sub-reply-card { padding: 10px 12px; }
  .reply-form { padding: 18px 16px; }
  .prompt-card { align-items: flex-start; flex-wrap: wrap; }
  .prompt-link { margin-left: 0; }
}

@media (max-width: 520px) {
  .breadcrumb { flex-wrap: wrap; }
  .card-head { flex-direction: column; }
  .card-head-right { width: 100%; flex-direction: row; justify-content: space-between; align-items: center; }
  .author-avatar { width: 34px; height: 34px; border-radius: 9px; }
  .ra-time { display: block; margin-left: 0; margin-top: 2px; }
  .sub-reply-list { margin-left: 12px; }
  .replying-indicator { flex-wrap: wrap; }
}
</style>
