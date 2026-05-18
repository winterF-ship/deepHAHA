<template>
  <div class="post-detail" v-if="post" :class="{ 'reply-bar-open': replyExpanded }">
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
      <div ref="bottomSentinelRef" class="bottom-sentinel"></div>
    </section>

    <!-- 底部固定回复条 -->
    <div class="reply-bar" :class="{ expanded: replyExpanded }">
      <template v-if="store.isLoggedIn()">
        <div class="reply-bar-collapsed" @click="expandReply">
          <span class="reply-bar-hint">
            {{ replyingTo ? '回复 @' + replyingTo.authorName + '…' : '写回复…' }}
          </span>
          <el-icon class="reply-bar-icon-btn"><EditPen /></el-icon>
        </div>
        <div class="reply-bar-expanded">
          <div v-if="replyingTo" class="reply-bar-replying">
            <span>正在回复 @{{ replyingTo.authorName }}</span>
            <el-button size="small" text @click="cancelReplyingTo">取消回复</el-button>
          </div>
          <el-input
            ref="replyTextareaRef"
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="写下你的回复…支持 Markdown 语法"
          />
          <div class="reply-bar-actions">
            <el-button size="small" text @click="collapseReply" :disabled="submitting">收起</el-button>
            <el-button type="primary" size="small" @click="submitReply" :loading="submitting">
              {{ submitting ? '提交中…' : '提交回复' }}
            </el-button>
          </div>
        </div>
      </template>
      <div v-else class="reply-bar-collapsed">
        <span class="reply-bar-hint">请先登录后再回复</span>
        <router-link to="/login" class="reply-bar-login-link">去登录</router-link>
      </div>
    </div>

    <UserCard v-model="showUserCard" :user="cardUser" @refresh="loadPost" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete, EditPen } from '@element-plus/icons-vue'
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
const replyExpanded = ref(false)
const replyTextareaRef = ref(null)
const bottomSentinelRef = ref(null)
const newReplyIds = ref(new Set())
const deletingReplyIds = ref(new Set())
let knownReplyIds = new Set()
let repliesReady = false
let pollTimer = null
let sentinelObserver = null
let observerReady = false
let userManuallyCollapsed = false

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
  if (sentinelObserver) {
    sentinelObserver.disconnect()
    sentinelObserver = null
  }
})

function setupSentinelObserver() {
  if (!bottomSentinelRef.value) return
  if (sentinelObserver) sentinelObserver.disconnect()
  sentinelObserver = new IntersectionObserver((entries) => {
    const visible = entries[0].isIntersecting
    const textarea = replyTextareaRef.value?.$el?.querySelector('textarea')
    const focused = document.activeElement === textarea
    if (visible && observerReady && !userManuallyCollapsed) {
      // 滚到底部 → 展开（但不强制 focus）
      if (!replyContent.value.trim() && !replyingTo.value && !focused) {
        replyExpanded.value = true
      }
    } else if (visible) {
      // 初始加载完成前或用户手动收起后不自动展开
    } else {
      // 离开底部 → 无内容、无目标、无 focus 时自动收起，并重置手动收起标记
      userManuallyCollapsed = false
      if (!replyContent.value.trim() && !replyingTo.value && !focused) {
        replyExpanded.value = false
      }
    }
  }, { threshold: 0 })
  sentinelObserver.observe(bottomSentinelRef.value)
}

watch(post, (val) => {
  if (val) {
    nextTick(() => {
      setupSentinelObserver()
      // 延迟激活，避免短页面初始加载就自动展开
      setTimeout(() => { observerReady = true }, 800)
    })
  }
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
  replyExpanded.value = true
  userManuallyCollapsed = false
  nextTick(() => {
    replyTextareaRef.value?.focus()
  })
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
    replyExpanded.value = false
    userManuallyCollapsed = false
    loadPost()
  } catch {
    // error handled by interceptor, keep content intact
  } finally {
    submitting.value = false
  }
}

function expandReply() {
  replyExpanded.value = true
  userManuallyCollapsed = false
  nextTick(() => {
    replyTextareaRef.value?.focus()
  })
}

function collapseReply() {
  if (replyContent.value.trim()) return
  replyExpanded.value = false
  userManuallyCollapsed = true
}

function cancelReplyingTo() {
  replyingTo.value = null
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
  const colors = ['#5BA7FF', '#f59e0b', '#8fdc18', '#F56565']
  const softs = ['#EEF6FF', 'rgba(245,158,11,0.09)', '#F4FFD8', 'rgba(245,101,101,0.10)']
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
.post-detail {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 92px;
}
.post-detail.reply-bar-open { padding-bottom: 220px; }

/* Breadcrumb */
.breadcrumb { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.breadcrumb a { color: var(--text-secondary); transition: color var(--transition); }
.breadcrumb a:hover { color: var(--primary); }
.bc-sep { color: #c0c8d6; }
.bc-current { color: var(--text); font-weight: 500; }

/* Content card */
.content-card {
  background: var(--bg-card); border-radius: 14px;
  padding: 22px 26px; border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-head-right { display: flex; align-items: center; gap: 10px; }
.delete-btn {
  border-radius: 7px; font-size: 12px;
  opacity: 0.72;
}
.delete-btn:hover { opacity: 1; }
.author-row { display: flex; align-items: center; gap: 10px; }
.author-avatar {
  width: 38px; height: 38px; border-radius: 50%;
  background: var(--primary);
  color: #fff; font-size: 15px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden; flex-shrink: 0;
}
.author-avatar img,
.ra-avatar img {
  width: 100%; height: 100%; object-fit: cover; object-position: center; display: block;
}
.author-name-line { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.author-name { font-size: 14px; font-weight: 650; color: var(--text); }
.post-time { font-size: 12px; color: var(--text-muted); margin-top: 1px; }
.tag {
  font-size: 11px; font-weight: 650; padding: 3px 9px; border-radius: 6px;
  border: 1px solid rgba(91, 167, 255, 0.14);
}
.post-title { font-size: 23px; font-weight: 750; color: var(--text); margin-bottom: 12px; line-height: 1.36; letter-spacing: 0; }
.post-body {
  font-size: 15px; line-height: 1.78; color: #243044;
  white-space: pre-wrap; word-break: break-word;
}
.card-foot { margin-top: 18px; padding-top: 12px; border-top: 1px solid #edf0f4; }
.stat { font-size: 13px; color: var(--text-muted); }
.stat-sep { margin: 0 8px; color: #c0c8d6; }
.action-stat {
  cursor: pointer; user-select: none; transition: all var(--transition);
  display: inline-flex; align-items: center; gap: 4px;
}
.action-stat:hover { color: var(--text); }
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
.section { margin-top: 22px; }
.section-title { font-size: 16px; font-weight: 700; color: var(--text); margin-bottom: 10px; }

/* Reply list */
.reply-list {
  display: flex; flex-direction: column; gap: 0;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid #e8edf3;
  border-radius: 14px;
  overflow: hidden;
}
.floor-group {
  margin-bottom: 0;
  transition: opacity 220ms ease, transform 220ms ease, background-color 260ms ease;
}
.floor-group + .floor-group { border-top: 1px solid #eef2f7; }
.reply-card {
  background: rgba(255, 255, 255, 0.86); border-radius: 0; padding: 15px 18px;
  border: none;
  box-shadow: none;
  transition: border-color 200ms ease, box-shadow 200ms ease, background-color 260ms ease;
}
.reply-head-row { display: flex; align-items: center; gap: 9px; }
.floor-no {
  font-size: 11px; color: #9aa5b5; font-weight: 650;
  background: #f6f8fb; padding: 2px 7px; border-radius: 999px;
  flex-shrink: 0;
}
.reply-head { display: flex; align-items: center; gap: 10px; flex: 1; cursor: pointer; }
.reply-actions { display: flex; gap: 2px; flex-shrink: 0; opacity: 0.72; transition: opacity 160ms ease; }
.reply-card:hover .reply-actions,
.sub-reply-card:hover .reply-actions { opacity: 1; }
.reply-btn { font-size: 12px; padding: 2px 7px; border-radius: 6px; color: var(--text-secondary); }
.reply-btn:hover { background: #f3f5f8; color: var(--text); }
.reply-btn.el-button--danger { color: #b45353; }
.reply-btn.el-button--danger:hover { background: #fff1f2; color: #dc2626; }
.reply-to-tag {
  font-size: 12px; color: var(--primary-hover); margin: 7px 0 3px 40px;
  font-weight: 550;
}

/* Sub replies */
.sub-reply-list {
  margin-left: 58px; margin-right: 18px; padding: 0 0 10px 12px;
  border-left: 2px solid #e8edf3;
}
.sub-reply-card {
  padding: 10px 12px; margin: 4px 0;
  background: #f8fafc; border-radius: 10px;
  border: 1px solid #edf1f6;
  transition: opacity 220ms ease, transform 220ms ease, border-color 200ms ease, box-shadow 200ms ease, background-color 260ms ease;
}
.reply-card:hover,
.sub-reply-card:hover {
  background-color: #fbfcff;
  border-color: #dde5ef;
  box-shadow: none;
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
  0% { background-color: rgba(91, 167, 255, 0.14); }
  100% { background-color: var(--bg-card); }
}
.ra-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--primary);
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
.title-admin { color: #9f1239; background: #fff1f2; border: 1px solid #fecdd3; }
.title-supervisor { color: #6f8f00; background: #f4ffd8; border: 1px solid #ddf99a; }
.ra-time { font-size: 12px; color: var(--text-muted); margin-left: 10px; }
.reply-content { font-size: 14px; color: #273449; line-height: 1.68; white-space: pre-wrap; word-break: break-word; margin-top: 9px; }

/* Empty replies */
.empty-replies {
  text-align: center; padding: 28px; background: rgba(255, 255, 255, 0.72);
  border-radius: 14px; border: 1px dashed #d8e0ea;
}
.empty-replies .empty-icon { font-size: 32px; margin-bottom: 8px; }
.empty-replies p { font-size: 14px; color: var(--text-muted); }

/* Bottom sentinel (invisible trigger) */
.bottom-sentinel {
  height: 1px;
  width: 100%;
}

/* Fixed bottom reply bar */
.reply-bar {
  position: fixed;
  left: 50%;
  bottom: 18px;
  z-index: 999;
  width: min(760px, calc(100vw - 32px));
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: 16px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.10), 0 1px 2px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px) saturate(160%);
  -webkit-backdrop-filter: blur(14px) saturate(160%);
  max-height: 58px;
  overflow: hidden;
  transition: max-height 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.35s ease,
              border-color 0.35s ease;
  padding: 0;
}

.reply-bar.expanded {
  max-height: 320px;
  border-color: rgba(148, 163, 184, 0.86);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.13), 0 1px 2px rgba(15, 23, 42, 0.06);
}

.reply-bar-collapsed {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 58px;
  cursor: text;
  border-radius: 16px;
  padding: 7px 8px 7px 16px;
  background: transparent;
  border: none;
  opacity: 1;
  transition: opacity 0.2s ease, height 0.3s ease, background-color var(--transition);
}
.reply-bar.expanded .reply-bar-collapsed {
  height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}
.reply-bar-collapsed:hover {
  background: rgba(248, 250, 252, 0.74);
}

.reply-bar-hint {
  font-size: 14px;
  color: #7b8798;
  user-select: none;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-bar-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  color: var(--primary-hover);
  background: var(--primary-soft);
  border: 1px solid rgba(91, 167, 255, 0.18);
  transition: background-color var(--transition), transform var(--transition);
  flex-shrink: 0;
}
.reply-bar-collapsed:hover .reply-bar-icon-btn {
  background: #ffe3f0;
}
.reply-bar-collapsed:active .reply-bar-icon-btn {
  transform: scale(0.96);
}

.reply-bar-login-link {
  font-size: 13px;
  color: var(--primary-hover);
  font-weight: 700;
  flex-shrink: 0;
  padding: 8px 13px;
  border-radius: 9px;
  background: var(--primary-soft);
  border: 1px solid rgba(91, 167, 255, 0.18);
}
.reply-bar-login-link:hover { background: #ffe3f0; }

/* Expanded */
.reply-bar-expanded {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 12px 12px;
  opacity: 0;
  transition: opacity 0.25s ease 0.05s;
}
.reply-bar.expanded .reply-bar-expanded {
  opacity: 1;
  transition: opacity 0.25s ease 0.15s;
}

.reply-bar-replying {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: var(--primary-hover);
  background: transparent;
  padding: 0 0 0 9px;
  border-left: 3px solid rgba(91, 167, 255, 0.35);
  border-radius: 0;
  font-weight: 600;
}
.reply-bar-replying :deep(.el-button) {
  color: var(--text-secondary);
  font-weight: 500;
}

.reply-bar :deep(.el-textarea__inner) {
  min-height: 92px !important;
  border-radius: 12px;
  border: 1px solid #d7dee8;
  color: #273449;
  font-size: 14px;
  line-height: 1.65;
  padding: 10px 12px;
  transition: border-color var(--transition), box-shadow var(--transition);
  font-family: inherit;
  resize: vertical;
  box-shadow: none;
}
.reply-bar :deep(.el-textarea__inner:focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(91, 167, 255, 0.12);
}

.reply-bar-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}
.reply-bar-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
}
.reply-bar-actions :deep(.el-button--primary) {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: none;
}
.reply-bar-actions :deep(.el-button--primary:hover) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

/* Post detail padding for fixed bar */
.post-detail {
  transition: padding-bottom 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@media (max-width: 768px) {
  .content-card { padding: 18px 16px; border-radius: 12px; }
  .card-head { align-items: flex-start; gap: 12px; }
  .card-head-right { flex-direction: column-reverse; align-items: flex-end; gap: 8px; }
  .post-title { font-size: 20px; }
  .post-body { font-size: 14px; }
  .card-foot { display: flex; flex-wrap: wrap; gap: 8px; }
  .stat-sep { display: none; }
  .reply-card { padding: 13px 14px; }
  .reply-head-row { align-items: flex-start; flex-wrap: wrap; }
  .reply-head { min-width: 0; }
  .reply-actions { width: 100%; justify-content: flex-end; }
  .sub-reply-list { margin-left: 22px; margin-right: 10px; }
  .sub-reply-card { padding: 10px 12px; }
  .reply-bar {
    bottom: calc(var(--mobile-tabbar-height) + 10px + env(safe-area-inset-bottom));
    width: calc(100vw - 20px);
    border-radius: 14px;
  }
  .post-detail {
    padding-bottom: 134px;
  }
  .post-detail.reply-bar-open {
    padding-bottom: 268px;
  }
}

@media (max-width: 520px) {
  .breadcrumb { flex-wrap: wrap; }
  .card-head { flex-direction: column; }
  .card-head-right { width: 100%; flex-direction: row; justify-content: space-between; align-items: center; }
  .author-avatar { width: 34px; height: 34px; border-radius: 50%; }
  .ra-time { display: block; margin-left: 0; margin-top: 2px; }
  .reply-list { border-radius: 12px; }
  .sub-reply-list { margin-left: 12px; margin-right: 8px; }
  .reply-bar-replying { flex-wrap: wrap; gap: 4px; }
  .reply-bar-collapsed { height: 52px; }
}
</style>
