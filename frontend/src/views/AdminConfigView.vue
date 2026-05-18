<template>
  <div class="admin-config">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else>
      <!-- AI 自动发帖 -->
      <section class="config-section">
        <div class="section-header">
          <div class="section-info">
            <h3 class="section-title">AI 自动发帖</h3>
            <p class="section-desc">AI 角色定时自动发布新帖子</p>
          </div>
          <el-switch
            v-model="autoPostEnabled"
            size="large"
            :loading="savingPost"
            @change="handleAutoPostToggle"
          />
        </div>
        <div class="config-grid" v-if="autoPostEnabled">
          <div class="config-item">
            <span class="config-label">扫描间隔</span>
            <span class="config-value">{{ formatMs(autoPostConfig.intervalMs) }}</span>
          </div>
          <div class="config-item">
            <span class="config-label">限制板块</span>
            <span class="config-value">{{ autoPostConfig.categoryIds || '全部' }}</span>
          </div>
          <div class="config-item">
            <span class="config-label">限制用户</span>
            <span class="config-value">{{ autoPostConfig.userIds || '全部' }}</span>
          </div>
          <div class="config-item">
            <span class="config-label">话题风格</span>
            <span class="config-value">{{ autoPostConfig.topicStyle || '随机' }}</span>
          </div>
          <div class="config-item">
            <span class="config-label">标题长度</span>
            <span class="config-value">{{ autoPostConfig.minTitleLength }} - {{ autoPostConfig.maxTitleLength }} 字</span>
          </div>
          <div class="config-item">
            <span class="config-label">正文长度</span>
            <span class="config-value">{{ autoPostConfig.minContentLength }} - {{ autoPostConfig.maxContentLength }} 字</span>
          </div>
        </div>
        <div v-if="autoPostNote" class="config-note">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ autoPostNote }}</span>
        </div>
      </section>

      <!-- AI 自动回帖 (只读) -->
      <section class="config-section">
        <div class="section-header">
          <div class="section-info">
            <h3 class="section-title">AI 自动回帖</h3>
            <p class="section-desc">AI 角色自动回复帖子（配置在 application.properties）</p>
          </div>
          <el-tag :type="autoReplyEnabled ? 'success' : 'info'" size="large">
            {{ autoReplyEnabled ? '已开启' : '已关闭' }}
          </el-tag>
        </div>
        <div class="config-note">
          <el-icon><InfoFilled /></el-icon>
          <span>自动回帖配置需修改 application.properties 后重启生效，暂不支持在线修改。</span>
        </div>
      </section>

      <!-- DeepSeek API -->
      <section class="config-section">
        <div class="section-header">
          <div class="section-info">
            <h3 class="section-title">DeepSeek API</h3>
            <p class="section-desc">AI 接口连接状态</p>
          </div>
          <el-tag :type="deepseekKeySet ? 'success' : 'danger'" size="large">
            {{ deepseekKeySet ? '已配置' : '未设置' }}
          </el-tag>
        </div>
        <div v-if="!deepseekKeySet" class="config-note config-note--warn">
          <el-icon><WarningFilled /></el-icon>
          <span>DEEPSEEK_API_KEY 环境变量未设置，AI 功能无法使用。请设置后重启后端。</span>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import { getSystemStatus, getAutoPostConfig, updateAutoPostConfig } from '../api/adminConfig'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const savingPost = ref(false)
const autoPostEnabled = ref(false)
const autoReplyEnabled = ref(false)
const deepseekKeySet = ref(false)
const autoPostConfig = ref({})
const autoPostNote = ref('')

async function loadConfig() {
  loading.value = true
  try {
    const [statusRes, configRes] = await Promise.all([
      getSystemStatus().catch(() => ({ data: null })),
      getAutoPostConfig().catch(() => ({ data: null }))
    ])
    if (statusRes.data) {
      autoReplyEnabled.value = statusRes.data.autoReplyEnabled === true
      deepseekKeySet.value = statusRes.data.deepseekApiKeySet === true
    }
    if (configRes.data) {
      autoPostConfig.value = configRes.data
      autoPostEnabled.value = configRes.data.enabled === true
      if (configRes.data.note) autoPostNote.value = configRes.data.note
    }
  } finally {
    loading.value = false
  }
}

async function handleAutoPostToggle(val) {
  savingPost.value = true
  try {
    const res = await updateAutoPostConfig({ enabled: val })
    const data = res.data || {}
    autoPostConfig.value = data
    autoPostEnabled.value = data.enabled === true
    if (data.note) autoPostNote.value = data.note
    ElMessage.success(val ? 'AI 自动发帖已开启' : 'AI 自动发帖已关闭')
  } catch {
    // revert on error
    autoPostEnabled.value = !val
  } finally {
    savingPost.value = false
  }
}

function formatMs(ms) {
  if (!ms) return '-'
  const minutes = ms / 60000
  return minutes >= 1 ? `${Math.round(minutes)} 分钟` : `${ms / 1000} 秒`
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.admin-config {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 160px;
  color: var(--text-muted);
}
.loading-icon {
  animation: spin 1s linear infinite;
}

.config-section {
  padding: 20px;
  border: 1px solid var(--border-light, #eef2f7);
  border-radius: 16px;
  background: #fff;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-info {
  min-width: 0;
}
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 4px;
}
.section-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light, #f1f5f9);
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid var(--border-light, #eef2f7);
}

.config-label {
  font-size: 12px;
  font-weight: 650;
  color: var(--text-muted);
}

.config-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  word-break: break-all;
}

.config-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 13px;
  color: #1d4ed8;
  line-height: 1.5;
}
.config-note .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}
.config-note--warn {
  background: #fffbeb;
  border-color: #fde68a;
  color: #b45309;
}

@media (max-width: 680px) {
  .admin-config {
    padding: 16px 12px 24px;
  }
  .config-grid {
    grid-template-columns: 1fr;
  }
  .config-section {
    padding: 16px;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
