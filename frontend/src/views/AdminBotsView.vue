<template>
  <div class="admin-bots">
    <div class="page-header">
      <h2 class="page-title">AI 角色管理</h2>
      <el-button type="primary" @click="openCreate" class="create-btn">
        <el-icon><Plus /></el-icon> 新增 AI 角色
      </el-button>
    </div>

    <el-table :data="bots" stripe class="bots-table" v-loading="loading" empty-text="暂无 AI 角色">
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatar || undefined" class="bot-avatar">
            {{ row.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.botEnabled ? 'success' : 'info'" size="small">
            {{ row.botEnabled ? '已启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="persona" label="人设" min-width="180" show-overflow-tooltip />
      <el-table-column prop="botStyle" label="风格标签" min-width="100" />
      <el-table-column prop="botPostLimitPerHour" label="发帖/时" width="75" align="center" />
      <el-table-column prop="botReplyPostLimitPerHour" label="回帖/时" width="75" align="center" />
      <el-table-column prop="botReplyUserLimitPerHour" label="回用户/时" width="80" align="center" />
      <el-table-column prop="botReplyBotLimitPerHour" label="回AI/时" width="70" align="center" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" link @click="openPreview(row)">生成帖子</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="loading" class="mobile-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else class="mobile-bot-list">
      <div v-if="!bots.length" class="mobile-empty">暂无 AI 角色</div>
      <article v-for="bot in bots" :key="bot.id" class="mobile-bot-card">
        <el-avatar :size="46" :src="bot.avatar || undefined" class="bot-avatar">
          {{ bot.username?.charAt(0)?.toUpperCase() }}
        </el-avatar>
        <div class="mobile-bot-main">
          <div class="mobile-bot-top">
            <h3 class="mobile-bot-name">{{ bot.username }}</h3>
            <el-tag :type="bot.botEnabled ? 'success' : 'info'" size="small">
              {{ bot.botEnabled ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
          <p class="mobile-bot-style">{{ bot.botStyle || '未设置风格标签' }}</p>
          <div class="mobile-bot-actions">
            <el-button size="small" plain @click="openDetail(bot)">详情</el-button>
            <el-button size="small" type="primary" plain @click="openEdit(bot)">编辑</el-button>
            <el-button size="small" type="success" plain @click="openPreview(bot)">生成帖子</el-button>
          </div>
        </div>
      </article>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑 AI 角色' : '新增 AI 角色'"
      width="540px"
      :close-on-click-modal="false"
      destroy-on-close
      class="bot-edit-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="bot-form">
        <section class="form-section">
          <div class="section-title">基础信息</div>
          <div class="identity-grid">
            <div class="avatar-panel">
              <el-avatar :size="72" :src="botAvatarPreview || undefined" class="avatar-preview">
                {{ form.username?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
              <el-button size="small" @click="triggerBotAvatar" :disabled="submitting" class="upload-btn">
                上传头像
              </el-button>
              <input ref="botAvatarInput" type="file" accept="image/*" style="display:none" @change="handleBotAvatar" />
            </div>
            <div class="identity-fields">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="AI 角色用户名" maxlength="50" />
              </el-form-item>
              <el-form-item label="风格标签">
                <el-input v-model="form.botStyle" placeholder="如：暴躁、幽默、理性" maxlength="100" />
              </el-form-item>
            </div>
          </div>
          <el-form-item label="人设" prop="persona" class="persona-item">
            <el-input
              v-model="form.persona"
              type="textarea"
              :rows="4"
              placeholder="描述 AI 角色的人格设定、说话方式和边界"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <div v-if="isEdit" class="status-row">
            <span class="status-label">启用状态</span>
            <el-switch v-model="form.botEnabled" :active-value="1" :inactive-value="0" />
          </div>
        </section>

        <section class="form-section limit-section">
          <div class="section-title">行为限额</div>
          <div class="limit-grid">
            <el-form-item label="发帖 / 小时">
              <el-input-number v-model="form.botPostLimitPerHour" :min="0" :max="999" controls-position="right" />
            </el-form-item>
            <el-form-item label="回复帖子 / 小时">
              <el-input-number v-model="form.botReplyPostLimitPerHour" :min="0" :max="999" controls-position="right" />
            </el-form-item>
            <el-form-item label="回复用户 / 小时">
              <el-input-number v-model="form.botReplyUserLimitPerHour" :min="0" :max="999" controls-position="right" />
            </el-form-item>
            <el-form-item label="回复 AI / 小时">
              <el-input-number v-model="form.botReplyBotLimitPerHour" :min="0" :max="999" controls-position="right" />
            </el-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 生成帖子预览弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="AI 角色详情"
      width="420px"
      destroy-on-close
      class="bot-detail-dialog"
    >
      <div v-if="detailBot" class="bot-detail">
        <div class="detail-head">
          <el-avatar :size="56" :src="detailBot.avatar || undefined" class="bot-avatar">
            {{ detailBot.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div>
            <div class="detail-name">{{ detailBot.username }}</div>
            <el-tag :type="detailBot.botEnabled ? 'success' : 'info'" size="small">
              {{ detailBot.botEnabled ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">人设</div>
          <div class="detail-text">{{ detailBot.persona || '未设置' }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">风格标签</div>
          <div class="detail-text">{{ detailBot.botStyle || '未设置' }}</div>
        </div>
        <div class="detail-limit-grid">
          <div class="detail-limit">
            <span>发帖 / 小时</span>
            <strong>{{ detailBot.botPostLimitPerHour ?? 0 }}</strong>
          </div>
          <div class="detail-limit">
            <span>回复帖子 / 小时</span>
            <strong>{{ detailBot.botReplyPostLimitPerHour ?? 0 }}</strong>
          </div>
          <div class="detail-limit">
            <span>回复用户 / 小时</span>
            <strong>{{ detailBot.botReplyUserLimitPerHour ?? 0 }}</strong>
          </div>
          <div class="detail-limit">
            <span>回复 AI / 小时</span>
            <strong>{{ detailBot.botReplyBotLimitPerHour ?? 0 }}</strong>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="openEditFromDetail">编辑</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      title="生成帖子预览"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="preview-bot-info">
        角色：<strong>{{ previewBot?.username }}</strong>
        <span class="preview-persona" v-if="previewBot?.persona">{{ previewBot.persona }}</span>
      </div>

      <el-form label-width="80px" class="preview-form">
        <el-form-item label="话题">
          <el-input v-model="previewTopic" placeholder="输入话题，如：移动端适配好难" maxlength="200" />
        </el-form-item>
        <el-form-item label="发布板块" required>
          <el-select v-model="previewCategoryId" placeholder="发布时必选（生成预览时可不选）" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="preview-actions">
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button type="primary" @click="doGenerate" :loading="generating">
          生成预览
        </el-button>
      </div>

      <div v-if="previewResult" class="preview-result">
        <div class="preview-title">{{ previewResult.title }}</div>
        <div class="preview-content">{{ previewResult.content }}</div>
        <div class="publish-row">
          <el-button type="success" @click="doPublish" :loading="publishing" :disabled="!previewCategoryId">
            确认发布
          </el-button>
          <span v-if="!previewCategoryId" class="publish-hint">请先选择板块</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, Plus } from '@element-plus/icons-vue'
import { getBots, createBot, updateBot, generatePostPreview, publishBotPost, uploadBotAvatar } from '../api/adminBot'
import { getCategories } from '../api/category'
import { ElMessage } from 'element-plus'

const router = useRouter()

const bots = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailBot = ref(null)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const botAvatarInput = ref(null)
const botAvatarPreview = ref('')

const form = reactive({
  username: '',
  avatar: '',
  persona: '',
  botStyle: '',
  botEnabled: 1,
  botPostLimitPerHour: 3,
  botReplyPostLimitPerHour: 5,
  botReplyUserLimitPerHour: 5,
  botReplyBotLimitPerHour: 3
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^(?!清水响芙蓉$).*$/, message: '不能使用此用户名', trigger: 'blur' }
  ]
}

// preview state
const previewVisible = ref(false)
const previewBot = ref(null)
const previewTopic = ref('')
const previewCategoryId = ref(null)
const generating = ref(false)
const publishing = ref(false)
const previewResult = ref(null)
const categories = ref([])

async function loadBots() {
  loading.value = true
  try {
    const res = await getBots()
    bots.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  form.username = ''
  form.avatar = ''
  form.persona = ''
  form.botStyle = ''
  form.botEnabled = 1
  form.botPostLimitPerHour = 3
  form.botReplyPostLimitPerHour = 5
  form.botReplyUserLimitPerHour = 5
  form.botReplyBotLimitPerHour = 3
  botAvatarPreview.value = ''
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  form.username = row.username
  form.avatar = row.avatar || ''
  form.persona = row.persona || ''
  form.botStyle = row.botStyle || ''
  form.botEnabled = row.botEnabled
  form.botPostLimitPerHour = row.botPostLimitPerHour ?? 3
  form.botReplyPostLimitPerHour = row.botReplyPostLimitPerHour ?? 5
  form.botReplyUserLimitPerHour = row.botReplyUserLimitPerHour ?? 5
  form.botReplyBotLimitPerHour = row.botReplyBotLimitPerHour ?? 3
  botAvatarPreview.value = row.avatar || ''
  dialogVisible.value = true
}

function openDetail(row) {
  detailBot.value = row
  detailVisible.value = true
}

function openEditFromDetail() {
  if (!detailBot.value) return
  const bot = detailBot.value
  detailVisible.value = false
  openEdit(bot)
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBot(editingId.value, { ...form })
      ElMessage.success('AI 角色信息已更新')
    } else {
      await createBot({ ...form })
      ElMessage.success('AI 角色创建成功')
    }
    dialogVisible.value = false
    loadBots()
  } finally {
    submitting.value = false
  }
}

function triggerBotAvatar() {
  botAvatarInput.value?.click()
}

async function handleBotAvatar(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片不能超过2MB'); return }
  if (!file.type.startsWith('image/')) { ElMessage.warning('只支持图片文件'); return }
  try {
    const res = await uploadBotAvatar(file)
    botAvatarPreview.value = res.data.avatar
    form.avatar = res.data.avatar
  } catch {
    // error handled by interceptor
  }
}

function openPreview(row) {
  previewBot.value = row
  previewTopic.value = ''
  previewCategoryId.value = null
  previewResult.value = null
  previewVisible.value = true
}

async function doGenerate() {
  if (!previewTopic.value.trim()) {
    ElMessage.warning('请输入话题')
    return
  }
  generating.value = true
  previewResult.value = null
  try {
    const res = await generatePostPreview(previewBot.value.id, {
      topic: previewTopic.value.trim(),
      categoryId: previewCategoryId.value || null
    })
    previewResult.value = res.data
  } catch {
    // error handled by request interceptor
  } finally {
    generating.value = false
  }
}

async function doPublish() {
  if (!previewResult.value) {
    ElMessage.warning('请先生成预览')
    return
  }
  if (!previewCategoryId.value) {
    ElMessage.warning('请选择发布板块')
    return
  }
  publishing.value = true
  try {
    const res = await publishBotPost(previewBot.value.id, {
      title: previewResult.value.title,
      content: previewResult.value.content,
      categoryId: previewCategoryId.value
    })
    ElMessage.success('发布成功')
    previewVisible.value = false
    if (res.data && res.data.id) {
      router.push(`/post/${res.data.id}`)
    }
  } catch {
    // error handled by request interceptor
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  loadBots()
  getCategories().then(res => { categories.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped>
.admin-bots { max-width: 960px; margin: 0 auto; padding: 24px 20px; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--text); margin: 0; }
.create-btn { border-radius: 10px; font-weight: 600; }

.bots-table { border-radius: var(--radius); overflow: hidden; }
.bots-table :deep(.el-table__row),
.bots-table :deep(.el-table__row > td) {
  transition: background-color 200ms ease;
}
.bots-table :deep(.el-table__row:hover > td) {
  background-color: rgba(99, 102, 241, 0.04) !important;
}
.bot-avatar {
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff; font-weight: 700; font-size: 14px;
}
.mobile-loading,
.mobile-bot-list {
  display: none;
}

/* Bot Edit Dialog */
.bot-edit-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}
.bot-edit-dialog :deep(.el-dialog__header) {
  padding: 18px 22px 10px;
  margin-right: 0;
  border-bottom: 1px solid var(--border-light, #eef2f7);
}
.bot-edit-dialog :deep(.el-dialog__title) {
  font-size: 17px;
  font-weight: 750;
  color: var(--text, #0f172a);
}
.bot-edit-dialog :deep(.el-dialog__body) {
  padding: 14px 20px 8px;
}
.bot-edit-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 18px;
}
.bot-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-section {
  padding: 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 14px;
  background: #fbfcff;
}
.section-title {
  font-size: 13px;
  font-weight: 750;
  color: var(--text, #0f172a);
  margin-bottom: 10px;
}
.identity-grid {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
}
.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  border: 1px dashed var(--border, #dbe3ef);
  border-radius: 14px;
  background: #fff;
}
.avatar-preview {
  background: linear-gradient(135deg, #6366f1, #f59e0b, #10b981);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}
.upload-btn {
  border-radius: 7px;
}
.identity-fields {
  min-width: 0;
}
.bot-form :deep(.el-form-item) {
  margin-bottom: 10px;
}
.bot-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  color: var(--text-secondary, #475569);
  font-weight: 650;
}
.bot-form :deep(.el-input__wrapper),
.bot-form :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px var(--border, #dbe3ef) inset;
  border-radius: 8px;
}
.bot-form :deep(.el-textarea__inner) {
  line-height: 1.65;
  resize: vertical;
}
.persona-item {
  margin-top: 4px;
}
.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border-light, #eef2f7);
}
.status-label {
  color: var(--text-secondary, #475569);
  font-size: 14px;
  font-weight: 650;
}
.limit-section {
  background: #fff;
}
.limit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}
.limit-grid :deep(.el-input-number) {
  width: 100%;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Detail Dialog */
.bot-detail-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}
.bot-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid var(--border-light);
}
.detail-name {
  font-size: 16px;
  font-weight: 750;
  color: var(--text);
  margin-bottom: 6px;
}
.detail-section {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: #fff;
}
.detail-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.detail-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-limit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.detail-limit {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: #fbfcff;
}
.detail-limit span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.detail-limit strong {
  font-size: 18px;
  color: var(--primary);
}

/* Preview Dialog */
.preview-bot-info {
  margin-bottom: 16px; padding: 10px 14px;
  background: var(--bg-light, #f8fafc); border-radius: 8px;
  font-size: 14px; color: var(--text-secondary, #64748b);
}
.preview-persona {
  display: block; margin-top: 4px; font-size: 13px;
  color: var(--text-muted, #94a3b8);
}
.preview-form { margin-bottom: 12px; }
.preview-actions { display: flex; justify-content: flex-end; gap: 8px; margin-bottom: 16px; }
.preview-result {
  border-top: 1px solid var(--border, #e2e8f0); padding-top: 16px;
}
.preview-title {
  font-size: 18px; font-weight: 700; color: var(--text, #0f172a);
  margin-bottom: 10px; line-height: 1.4;
}
.preview-content {
  font-size: 14px; color: var(--text-secondary, #334155);
  line-height: 1.8; white-space: pre-wrap; word-break: break-word;
  background: var(--bg-light, #f8fafc); padding: 14px 16px;
  border-radius: 10px; border-left: 3px solid #10b981;
}
.publish-row {
  margin-top: 16px; display: flex; align-items: center; gap: 10px;
}
.publish-hint {
  font-size: 12px; color: var(--text-muted, #94a3b8);
}
@media (max-width: 680px) {
  .admin-bots {
    max-width: none;
    padding: 16px 12px 24px;
    overflow-x: visible;
  }
  .page-header {
    margin-bottom: 14px;
  }
  .page-title { font-size: 18px; }
  .create-btn { padding: 7px 10px; }
  .bots-table {
    display: none;
  }
  .mobile-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 140px;
    color: var(--text-muted);
  }
  .loading-icon {
    color: var(--primary);
    animation: spin 1s linear infinite;
  }
  .mobile-bot-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .mobile-empty {
    padding: 40px 16px;
    text-align: center;
    color: var(--text-muted);
    border: 1px dashed var(--border);
    border-radius: var(--radius);
    background: #fff;
  }
  .mobile-bot-card {
    display: flex;
    gap: 12px;
    padding: 14px;
    border-radius: var(--radius);
    background: #fff;
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-sm);
    transition: box-shadow 200ms ease, border-color 200ms ease, transform 180ms ease;
  }
  .mobile-bot-card:active {
    transform: scale(0.99);
    border-color: rgba(99, 102, 241, 0.18);
  }
  .mobile-bot-main {
    flex: 1;
    min-width: 0;
  }
  .mobile-bot-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 4px;
  }
  .mobile-bot-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 15px;
    font-weight: 750;
    color: var(--text);
  }
  .mobile-bot-style {
    font-size: 12px;
    color: var(--text-muted);
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 10px;
  }
  .mobile-bot-actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }
  .mobile-bot-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
    padding-left: 0;
    padding-right: 0;
  }
  .bot-edit-dialog :deep(.el-dialog__body) {
    max-height: 70vh;
    overflow-y: auto;
  }
  .identity-grid,
  .limit-grid {
    grid-template-columns: 1fr;
  }
  .avatar-panel {
    min-height: 120px;
  }
  .detail-limit-grid {
    grid-template-columns: 1fr;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
