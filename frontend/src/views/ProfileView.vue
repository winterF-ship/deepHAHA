<template>
  <div class="profile-page">
    <!-- 顶部 -->
    <div class="page-top">
      <button class="back-btn" @click="$router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="page-title">个人中心</h1>
    </div>

    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-card-top">
        <!-- 头像 -->
        <div class="avatar-wrap" @click="triggerUpload" title="点击更换头像">
          <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" />
          <span v-else class="avatar-placeholder">{{ store.userInfo?.username?.charAt(0)?.toUpperCase() }}</span>
          <div class="avatar-overlay">
            <el-icon><Camera /></el-icon>
          </div>
        </div>
        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFile" />

        <div class="user-main">
          <h2 class="user-name">{{ store.userInfo?.username }}</h2>
          <p class="user-email">{{ store.userInfo?.email || '未设置邮箱' }}</p>
          <p class="user-date">注册于 {{ formatDate(store.userInfo?.createdAt) }}</p>
        </div>
        <div class="user-stat">
          <span class="stat-num">{{ myPosts.length }}</span>
          <span class="stat-label">帖子</span>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <button :class="['tab', { active: activeTab === 'posts' }]" @click="activeTab = 'posts'">我的帖子</button>
      <button :class="['tab', { active: activeTab === 'info' }]" @click="activeTab = 'info'">账号信息</button>
      <button :class="['tab', { active: activeTab === 'security' }]" @click="activeTab = 'security'">安全设置</button>
    </div>

    <!-- 我的帖子 -->
    <div v-if="activeTab === 'posts'" class="tab-content">
      <div v-if="myPosts.length" class="post-list">
        <div v-for="post in pagedPosts" :key="post.id" class="post-card" @click="$router.push(`/post/${post.id}`)">
          <div class="post-body">
            <span class="post-tag">{{ post.categoryName }}</span>
            <h3 class="post-title">{{ post.title }}</h3>
            <div class="post-meta">
              <span>{{ post.replyCount }} 回复</span><span class="sep">·</span>
              <span>{{ post.viewCount }} 阅读</span><span class="sep">·</span>
              <span>{{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
          <el-icon class="post-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="pagination" v-if="totalPages > 1">
          <el-pagination layout="prev, pager, next" :total="myPosts.length" :page-size="pageSize" v-model:current-page="currentPage" background small />
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">📝</div>
        <h3>还没有帖子</h3>
        <p>你还没有发布过任何帖子</p>
        <router-link to="/post/new" class="empty-cta">去发帖</router-link>
      </div>
    </div>

    <!-- 账号信息 -->
    <div v-if="activeTab === 'info'" class="tab-content">
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">用户名</span>
          <div class="info-value-row">
            <template v-if="editingName">
              <input v-model="editName" class="inline-input" maxlength="20" @keyup.enter="saveName" />
              <button class="inline-btn save" @click="saveName" :disabled="savingName">保存</button>
              <button class="inline-btn cancel" @click="cancelEditName">取消</button>
            </template>
            <template v-else>
              <span class="info-value">{{ store.userInfo?.username }}</span>
              <button class="info-edit-btn" @click="startEditName">编辑</button>
            </template>
          </div>
        </div>
        <div class="info-row">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ store.userInfo?.email || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">用户 ID</span>
          <span class="info-value">#{{ store.userInfo?.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">注册时间</span>
          <span class="info-value">{{ formatDate(store.userInfo?.createdAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 安全设置 -->
    <div v-if="activeTab === 'security'" class="tab-content">
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">旧密码</span>
          <input v-model="pwForm.oldPassword" type="password" class="inline-input" placeholder="输入旧密码" />
        </div>
        <div class="info-row">
          <span class="info-label">新密码</span>
          <input v-model="pwForm.newPassword" type="password" class="inline-input" placeholder="至少4位" />
        </div>
        <div class="info-row">
          <span class="info-label"></span>
          <button class="inline-btn save" @click="savePassword" :disabled="savingPw">{{ savingPw ? '修改中…' : '修改密码' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Camera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPosts } from '../api/post'
import { updateProfile, updatePassword, uploadAvatar } from '../api/auth'
import { useUserStore } from '../stores/user'

const route = useRoute()
const store = useUserStore()
const activeTab = ref(route.query.tab === 'info' ? 'info' : route.query.tab === 'security' ? 'security' : 'posts')
const myPosts = ref([])
const currentPage = ref(1)
const pageSize = 6

const totalPages = computed(() => Math.ceil(myPosts.value.length / pageSize))
const pagedPosts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return myPosts.value.slice(start, start + pageSize)
})

// 头像
const fileInput = ref(null)
const avatarPreview = ref(store.userInfo?.avatar || '')

function triggerUpload() { fileInput.value?.click() }

async function handleFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片不能超过2MB'); return }
  if (!file.type.startsWith('image/')) { ElMessage.warning('只支持图片文件'); return }
  try {
    const res = await uploadAvatar(file)
    avatarPreview.value = res.data.avatar
    store.updateUser(res.data)
    ElMessage.success('头像更新成功')
  } catch (err) {
    // error already handled by interceptor
  }
}

// 用户名
const editingName = ref(false)
const editName = ref('')
const savingName = ref(false)

function startEditName() {
  editName.value = store.userInfo?.username || ''
  editingName.value = true
}
function cancelEditName() {
  editingName.value = false
  editName.value = ''
}
async function saveName() {
  const name = editName.value.trim()
  if (!name) { ElMessage.warning('用户名不能为空'); return }
  if (name.length < 2) { ElMessage.warning('用户名至少2个字符'); return }
  if (name === store.userInfo?.username) { editingName.value = false; return }
  savingName.value = true
  try {
    const res = await updateProfile(name)
    store.updateUser(res.data)
    editingName.value = false
    ElMessage.success('用户名修改成功')
  } finally {
    savingName.value = false
  }
}

// 密码
const pwForm = ref({ oldPassword: '', newPassword: '' })
const savingPw = ref(false)

async function savePassword() {
  if (!pwForm.value.oldPassword) { ElMessage.warning('请输入旧密码'); return }
  if (!pwForm.value.newPassword || pwForm.value.newPassword.length < 4) { ElMessage.warning('新密码至少4位'); return }
  savingPw.value = true
  try {
    await updatePassword(pwForm.value.oldPassword, pwForm.value.newPassword)
    pwForm.value = { oldPassword: '', newPassword: '' }
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => {
      store.logout()
      window.location.href = '/login'
    }, 1000)
  } finally {
    savingPw.value = false
  }
}

// 加载帖子
onMounted(async () => {
  try {
    let allPosts = []
    for (let page = 0; page < 10; page++) {
      const res = await getPosts(null, page, 20)
      const content = res.data.content || []
      const mine = content.filter(p => p.authorId === store.userInfo?.id)
      allPosts.push(...mine)
      if (content.length < 20) break
    }
    myPosts.value = allPosts
  } catch (e) {
    myPosts.value = []
  }
})

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

function formatDate(t) {
  if (!t) return ''
  return t.split('T')[0]
}
</script>

<style scoped>
.profile-page { max-width: 760px; margin: 0 auto; }

/* Top */
.page-top { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
.back-btn {
  width: 40px; height: 40px; border-radius: 12px; border: 1.5px solid var(--border);
  background: var(--bg-card); cursor: pointer; display: grid; place-items: center;
  font-size: 18px; color: var(--text-secondary); transition: all var(--transition); flex-shrink: 0;
}
.back-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.page-title { font-size: 22px; font-weight: 750; color: var(--text); letter-spacing: -0.3px; }

/* User card */
.user-card {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: 28px 30px;
  border: 1px solid var(--border-light); box-shadow: var(--shadow-md); margin-bottom: 24px;
}
.user-card-top { display: flex; align-items: center; gap: 18px; }

/* Avatar */
.avatar-wrap {
  width: 72px; height: 72px; border-radius: 18px; flex-shrink: 0; cursor: pointer;
  position: relative; overflow: hidden;
  background: linear-gradient(135deg, #5b6eeb, #8b5cf6);
  box-shadow: 0 4px 14px rgba(91,110,235,0.28);
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%; display: grid; place-items: center;
  color: #fff; font-size: 28px; font-weight: 800;
}
.avatar-overlay {
  position: absolute; inset: 0; display: grid; place-items: center;
  background: rgba(0,0,0,0.35); color: #fff; font-size: 22px;
  opacity: 0; transition: opacity var(--transition);
}
.avatar-wrap:hover .avatar-overlay { opacity: 1; }

.user-main { flex: 1; }
.user-name { font-size: 20px; font-weight: 750; color: var(--text); letter-spacing: -0.3px; }
.user-email { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.user-date { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.user-stat { text-align: center; flex-shrink: 0; }
.stat-num { display: block; font-size: 28px; font-weight: 800; color: var(--primary); line-height: 1; }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

/* Tabs */
.tabs { display: flex; gap: 0; margin-bottom: 16px; background: var(--bg-card); border-radius: var(--radius); padding: 4px; border: 1px solid var(--border-light); }
.tab {
  flex: 1; padding: 9px 16px; border-radius: 10px; border: none;
  background: transparent; cursor: pointer; font-size: 14px; font-weight: 500;
  color: var(--text-secondary); transition: all var(--transition);
}
.tab.active { background: var(--primary-soft); color: var(--primary); font-weight: 650; box-shadow: var(--shadow-sm); }
.tab:hover:not(.active) { color: var(--text); }

/* Post list */
.post-list { display: flex; flex-direction: column; gap: 8px; }
.post-card {
  background: var(--bg-card); border-radius: var(--radius); padding: 16px 20px;
  cursor: pointer; display: flex; align-items: center; gap: 12px;
  border: 1px solid var(--border-light); box-shadow: var(--shadow-sm); transition: all var(--transition);
}
.post-card:hover { transform: translateY(-1px); box-shadow: var(--shadow-md); border-color: transparent; }
.post-body { flex: 1; min-width: 0; }
.post-tag { font-size: 11px; font-weight: 600; color: var(--primary); background: var(--primary-soft); padding: 2px 8px; border-radius: 5px; }
.post-title { font-size: 14px; font-weight: 600; color: var(--text); margin-top: 6px; line-height: 1.4; }
.post-meta { font-size: 12px; color: var(--text-muted); margin-top: 4px; display: flex; align-items: center; gap: 4px; }
.sep { font-size: 8px; margin: 0 2px; }
.post-arrow { color: var(--text-muted); font-size: 16px; opacity: 0; transform: translateX(-4px); transition: all var(--transition); flex-shrink: 0; }
.post-card:hover .post-arrow { opacity: 1; transform: translateX(0); }
.pagination { display: flex; justify-content: center; margin-top: 20px; }

/* Info card */
.info-card {
  background: var(--bg-card); border-radius: var(--radius); padding: 4px 0;
  border: 1px solid var(--border-light); box-shadow: var(--shadow-sm); overflow: hidden;
}
.info-row {
  display: flex; align-items: center; padding: 15px 22px;
  border-bottom: 1px solid var(--border-light);
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 13px; color: var(--text-muted); width: 80px; flex-shrink: 0; }
.info-value { font-size: 14px; color: var(--text); font-weight: 500; }
.info-value-row { display: flex; align-items: center; gap: 10px; }
.info-edit-btn {
  font-size: 12px; color: var(--primary); background: none; border: none;
  cursor: pointer; font-weight: 600; padding: 2px 6px;
}
.info-edit-btn:hover { text-decoration: underline; }

/* Inline inputs */
.inline-input {
  padding: 7px 12px; border: 1.5px solid var(--border); border-radius: 8px;
  font-size: 14px; outline: none; width: 200px; color: var(--text);
  transition: border-color var(--transition); font-family: inherit;
}
.inline-input:focus { border-color: var(--primary); }
.inline-btn {
  padding: 7px 16px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all var(--transition);
}
.inline-btn.save {
  background: linear-gradient(135deg, #5b6eeb, #7c5ce7); color: #fff;
  box-shadow: 0 2px 6px rgba(91,110,235,0.22);
}
.inline-btn.save:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(91,110,235,0.30); }
.inline-btn.save:disabled { opacity: 0.5; cursor: not-allowed; }
.inline-btn.cancel { background: transparent; color: var(--text-muted); }
.inline-btn.cancel:hover { color: var(--text); }

/* Empty */
.empty-state {
  text-align: center; padding: 44px 20px; background: var(--bg-card);
  border-radius: var(--radius-lg); border: 1px dashed var(--border);
}
.empty-icon { font-size: 40px; margin-bottom: 12px; }
.empty-state h3 { font-size: 15px; font-weight: 650; color: var(--text); margin-bottom: 4px; }
.empty-state p { font-size: 13px; color: var(--text-muted); margin-bottom: 18px; }
.empty-cta {
  display: inline-block; padding: 8px 22px; border-radius: 10px;
  background: linear-gradient(135deg, #5b6eeb, #7c5ce7);
  color: #fff; font-size: 13px; font-weight: 600;
  box-shadow: 0 4px 12px rgba(91,110,235,0.24);
}
.empty-cta:hover { transform: translateY(-1px); }

.tab-content { min-height: 200px; }

@media (max-width: 768px) {
  .page-top { margin-bottom: 18px; }
  .user-card { padding: 20px 16px; }
  .user-card-top { align-items: flex-start; gap: 12px; }
  .avatar-wrap { width: 56px; height: 56px; border-radius: 14px; }
  .avatar-placeholder { font-size: 22px; }
  .user-name { font-size: 17px; }
  .user-stat { margin-left: auto; }
  .tabs { overflow-x: auto; scrollbar-width: none; }
  .tabs::-webkit-scrollbar { display: none; }
  .tab { min-width: 96px; padding: 8px 12px; white-space: nowrap; }
  .post-card { padding: 14px 16px; }
  .post-meta { flex-wrap: wrap; }
  .info-row { padding: 12px 16px; flex-wrap: wrap; }
  .info-label { width: 65px; font-size: 12px; }
  .info-value-row { flex: 1; min-width: 0; flex-wrap: wrap; }
  .inline-input { width: min(180px, 100%); }
}

@media (max-width: 520px) {
  .user-card-top { flex-wrap: wrap; }
  .user-main { min-width: 0; }
  .user-email,
  .user-date { word-break: break-word; }
  .user-stat { width: 100%; text-align: left; display: flex; align-items: baseline; gap: 6px; }
  .info-row { flex-direction: column; align-items: flex-start; gap: 8px; }
  .info-label { width: auto; }
  .inline-btn { width: 100%; }
  .post-arrow { display: none; }
}
</style>
