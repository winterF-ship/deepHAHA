<template>
  <el-dialog v-model="visible" width="360px" :close-on-click-modal="true" class="user-card-dialog">
    <template #header>
      <span class="dialog-title">用户信息</span>
    </template>
    <div v-if="user" class="user-card-body">
      <div class="uc-avatar">
        <img v-if="user.avatar" :src="user.avatar" :alt="user.username" />
        <span v-else>{{ user.username?.charAt(0) }}</span>
      </div>
      <div class="uc-name">{{ user.username }}</div>
      <div class="uc-info">
        <span class="uc-label">ID</span>
        <span>{{ user.id }}</span>
      </div>
      <div class="uc-info">
        <span class="uc-label">角色</span>
        <span :class="roleClass(user.role)">{{ roleLabel(user.role) }}</span>
      </div>
      <div v-if="displayTitle" class="uc-info">
        <span class="uc-label">公开头衔</span>
        <span class="title-badge" :class="titleClass(user.role)">{{ displayTitle }}</span>
      </div>
      <div class="uc-info">
        <span class="uc-label">状态</span>
        <span :class="isMutedNow ? 'muted-yes' : 'muted-no'">{{ isMutedNow ? '禁言中' : '正常' }}</span>
      </div>
      <div v-if="isMutedNow && user.muteReason" class="uc-info">
        <span class="uc-label">原因</span>
        <span>{{ user.muteReason }}</span>
      </div>
      <div v-if="isMutedNow" class="uc-info">
        <span class="uc-label">剩余时间</span>
        <span>{{ muteRemainingText }}</span>
      </div>

      <div v-if="showMutePanel" class="mute-panel">
        <div class="mute-title">禁言设置</div>
        <div class="mute-days">
          <el-button
            v-for="d in muteOptions"
            :key="d.value"
            :type="muteDays === d.value ? 'primary' : ''"
            size="small"
            @click="muteDays = d.value"
            class="mute-day-btn"
          >{{ d.label }}</el-button>
        </div>
        <el-input
          v-model="muteReason"
          size="small"
          placeholder="禁言原因（选填）"
          class="mute-reason"
        />
        <el-button type="primary" size="small" @click="doMute" :loading="muting" class="mute-submit">
          确认禁言
        </el-button>
      </div>

      <div class="uc-actions">
        <el-button
          v-if="showMuteBtn"
          type="warning"
          size="small"
          @click="showMutePanel = !showMutePanel"
        >{{ showMutePanel ? '取消' : '禁言' }}</el-button>
        <el-button
          v-if="showUnmuteBtn"
          type="success"
          size="small"
          @click="doUnmute"
          :loading="unmuting"
        >解除禁言</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { muteUser, unmuteUser } from '../api/auth'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: Boolean, user: Object })
const emit = defineEmits(['update:modelValue', 'refresh'])

const store = useUserStore()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const showMutePanel = ref(false)
const muteDays = ref(null)
const muteReason = ref('')
const muting = ref(false)
const unmuting = ref(false)

const muteOptions = [
  { label: '1天', value: 1 },
  { label: '3天', value: 3 },
  { label: '7天', value: 7 },
  { label: '30天', value: 30 },
  { label: '永久', value: null },
]

const isMutedNow = computed(() => {
  if (!props.user?.muted) return false
  if (!props.user.muteEndTime) return true
  return new Date(props.user.muteEndTime).getTime() > Date.now()
})

const muteRemainingText = computed(() => {
  if (!isMutedNow.value) return ''
  if (!props.user.muteEndTime) return '永久'
  const diff = new Date(props.user.muteEndTime).getTime() - Date.now()
  if (diff <= 0) return '0分钟'
  const totalMinutes = Math.ceil(diff / 60000)
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60
  const parts = []
  if (days) parts.push(`${days}天`)
  if (hours) parts.push(`${hours}小时`)
  if (minutes || !parts.length) parts.push(`${minutes}分钟`)
  return parts.join('')
})

const displayTitle = computed(() => {
  const custom = props.user?.displayTitle?.trim()
  if (custom) return custom
  if (props.user?.role === 'ADMIN') return '管理员'
  if (props.user?.role === 'SUPERVISOR') return '监督'
  return ''
})

const showMuteBtn = computed(() => {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  if (!props.user) return false
  if (props.user.id === store.userInfo.id) return false
  if (props.user.role === 'ADMIN') return false
  return !isMutedNow.value
})

const showUnmuteBtn = computed(() => {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  if (!props.user) return false
  if (props.user.id === store.userInfo.id) return false
  if (props.user.role === 'ADMIN') return false
  return isMutedNow.value
})

function roleLabel(role) {
  if (!role) return '普通用户'
  if (role === 'ADMIN') return '管理员'
  if (role === 'SUPERVISOR') return '监督'
  return '普通用户'
}

function roleClass(role) {
  if (role === 'ADMIN') return 'role-admin'
  if (role === 'SUPERVISOR') return 'role-supervisor'
  return 'role-user'
}

function titleClass(role) {
  if (role === 'ADMIN') return 'title-admin'
  if (role === 'SUPERVISOR') return 'title-supervisor'
  return ''
}

async function doMute() {
  if (muteDays.value === undefined) { ElMessage.warning('请选择禁言时长'); return }
  muting.value = true
  try {
    await muteUser(props.user.id, muteDays.value, muteReason.value || null)
    syncCurrentUserMuteState({
      muted: 1,
      muteReason: muteReason.value || null,
      muteEndTime: buildMuteEndTime(muteDays.value)
    })
    ElMessage.success('禁言成功')
    showMutePanel.value = false
    emit('refresh')
  } finally { muting.value = false }
}

async function doUnmute() {
  unmuting.value = true
  try {
    await unmuteUser(props.user.id)
    syncCurrentUserMuteState({
      muted: 0,
      muteReason: null,
      muteEndTime: null
    })
    ElMessage.success('已解除禁言')
    emit('refresh')
  } finally { unmuting.value = false }
}

function buildMuteEndTime(days) {
  if (days === null) return null
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString()
}

function syncCurrentUserMuteState(nextState) {
  if (!props.user) return
  Object.assign(props.user, nextState)
}
</script>

<style scoped>
.user-card-dialog :deep(.el-dialog__header) { padding: 20px 24px 0; }
.user-card-dialog :deep(.el-dialog__body) { padding: 16px 24px 24px; }
.dialog-title { font-size: 16px; font-weight: 700; color: var(--text); }
.user-card-body { text-align: center; }
.uc-avatar {
  width: 64px; height: 64px; border-radius: 50%; margin: 0 auto 12px;
  background: linear-gradient(135deg, #5BA7FF, #3B8BEA 62%, #C8FF3D);
  color: #fff; font-size: 24px; font-weight: 700;
  display: grid; place-items: center; overflow: hidden; flex-shrink: 0;
}
.uc-avatar img { width: 100%; height: 100%; object-fit: cover; object-position: center; display: block; }
.uc-name { font-size: 18px; font-weight: 700; color: var(--text); margin-bottom: 12px; }
.uc-info {
  display: flex; justify-content: space-between; padding: 6px 0;
  font-size: 13px; color: var(--text-secondary); border-bottom: 1px solid var(--border-light);
}
.uc-label { color: var(--text-muted); }
.role-admin { color: #f59e0b; font-weight: 600; }
.role-supervisor { color: #6f8f00; font-weight: 600; }
.role-user { color: var(--text-secondary); }
.title-badge {
  max-width: 180px; padding: 2px 8px; border-radius: 6px;
  font-size: 12px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-admin { color: #be123c; background: #ffe4ec; border: 1px solid #fecdd3; }
.title-supervisor { color: #6f8f00; background: #f4ffd8; border: 1px solid #ddf99a; }
.muted-yes { color: #ef4444; font-weight: 600; }
.muted-no { color: #7fb600; }
.uc-actions { margin-top: 16px; display: flex; justify-content: center; gap: 10px; }
.mute-panel { margin-top: 16px; padding: 14px; background: var(--bg); border-radius: 10px; border: 1px solid var(--border-light); }
.mute-title { font-size: 13px; font-weight: 600; color: var(--text); margin-bottom: 10px; }
.mute-days { display: flex; gap: 6px; flex-wrap: wrap; justify-content: center; margin-bottom: 10px; }
.mute-day-btn { font-size: 12px; padding: 4px 10px; border-radius: 6px; }
.mute-reason { margin-bottom: 10px; }
.mute-submit { width: 100%; }

@media (max-width: 520px) {
  .user-card-dialog :deep(.el-dialog__header) { padding: 18px 18px 0; }
  .user-card-dialog :deep(.el-dialog__body) { padding: 14px 18px 20px; }
  .uc-avatar { width: 56px; height: 56px; border-radius: 50%; }
  .uc-name { font-size: 16px; }
  .uc-actions { flex-direction: column; }
  .uc-actions :deep(.el-button) { width: 100%; margin-left: 0; }
  .mute-days { display: grid; grid-template-columns: repeat(2, 1fr); }
  .mute-day-btn { width: 100%; }
}
</style>
