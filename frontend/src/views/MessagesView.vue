<template>
  <div class="messages-page">
    <div class="page-header">
      <h1 class="page-title">回复我的</h1>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon" :size="28"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="messages.length === 0" class="empty-state">
      <el-icon class="empty-icon" :size="48"><ChatDotSquare /></el-icon>
      <p class="empty-text">还没有人回复你</p>
      <p class="empty-hint">当你发布的帖子或评论收到回复时，会显示在这里</p>
    </div>

    <div v-else class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.replyId"
        class="message-card"
        @click="goToPost(msg)"
      >
        <el-avatar :size="40" class="msg-avatar" :src="msg.fromAvatar || undefined">
          {{ msg.fromUsername?.charAt(0)?.toUpperCase() }}
        </el-avatar>
        <div class="msg-body">
          <div class="msg-header">
            <span class="msg-from">
              {{ msg.fromUsername }}
              <el-tag v-if="msg.fromIsBot === 1" size="small" type="warning" class="bot-tag">AI</el-tag>
            </span>
            <span class="msg-type-label">{{ msg.replyType === 'FLOOR' ? '回复了你的帖子' : '回复了你的评论' }}</span>
            <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
          </div>
          <p class="msg-content">{{ truncate(msg.content, 200) }}</p>
          <div class="msg-footer">
            <el-icon :size="14"><Document /></el-icon>
            <span class="msg-post-title">{{ msg.postTitle }}</span>
          </div>
        </div>
        <el-icon class="msg-arrow" :size="16"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, ChatDotSquare, Document, ArrowRight } from '@element-plus/icons-vue'
import { getRepliesToMe } from '../api/message'

const router = useRouter()
const messages = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getRepliesToMe()
    messages.value = res.data || []
  } finally {
    loading.value = false
  }
})

function goToPost(msg) {
  router.push({ path: `/post/${msg.postId}` })
}

function truncate(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function formatTime(t) {
  if (!t) return ''
  const date = new Date(t)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 30) return `${days} 天前`
  return t.split('T')[0]
}
</script>

<style scoped>
.messages-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: var(--primary);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-muted);
}

.empty-icon {
  color: var(--border);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0 0 8px;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all var(--transition);
}

.message-card:hover {
  border-color: var(--primary);
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.08);
  transform: translateY(-1px);
}

.msg-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff;
  font-weight: 700;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.msg-from {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
}

.bot-tag {
  margin-left: 2px;
  font-size: 11px;
}

.msg-type-label {
  font-size: 12px;
  color: var(--text-muted);
}

.msg-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}

.msg-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 8px;
  word-break: break-all;
}

.msg-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.msg-post-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-arrow {
  flex-shrink: 0;
  color: var(--text-muted);
  margin-top: 10px;
}

@media (max-width: 768px) {
  .messages-page {
    max-width: none;
    padding: 16px 0 24px;
  }

  .page-header {
    padding: 0 12px;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 18px;
  }

  .loading-state {
    padding: 48px 12px;
  }

  .empty-state {
    min-height: 48vh;
    padding: 56px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  .empty-icon {
    margin-bottom: 12px;
  }

  .empty-hint {
    max-width: 260px;
    line-height: 1.6;
  }

  .message-list {
    gap: 10px;
    padding: 0 12px;
  }

  .message-card {
    padding: 12px;
    gap: 10px;
    border-radius: var(--radius);
  }

  .msg-avatar {
    width: 36px !important;
    height: 36px !important;
  }

  .msg-header {
    gap: 4px 8px;
  }

  .msg-from {
    max-width: 100%;
  }

  .msg-time {
    width: 100%;
    margin-left: 0;
  }

  .msg-content {
    font-size: 13px;
    line-height: 1.65;
    word-break: break-word;
  }

  .msg-footer {
    min-width: 0;
  }

  .msg-arrow {
    display: none;
  }
}

@media (max-width: 420px) {
  .message-card {
    align-items: flex-start;
  }

  .msg-type-label {
    display: block;
    width: 100%;
  }
}
</style>
