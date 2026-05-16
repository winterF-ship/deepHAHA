<template>
  <div class="admin-users">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
    </div>

    <el-table :data="users" stripe class="users-table" v-loading="loading" empty-text="暂无用户">
      <el-table-column label="头像" width="80" align="center">
        <template #default="{ row }">
          <el-avatar :size="40" :src="row.avatar || undefined">
            {{ row.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="180" />
      <el-table-column label="角色" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="公开头衔" width="150" align="center">
        <template #default="{ row }">
          <span v-if="displayTitle(row)" class="title-badge" :class="titleClass(row.role)">{{ displayTitle(row) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="AI" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isBot" type="warning" size="small">AI</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="140" align="center">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-cell">
            <el-button
              v-if="canEditTitle(row)"
              size="small"
              :loading="operatingId === row.id"
              @click="handleEditTitle(row)"
            >头衔</el-button>
            <el-button
              v-if="canAppoint(row)"
              size="small"
              type="primary"
              :loading="operatingId === row.id"
              @click="handleAppoint(row)"
            >设为监督</el-button>
            <el-button
              v-if="canRemove(row)"
              size="small"
              type="warning"
              :loading="operatingId === row.id"
              @click="handleRemove(row)"
            >解除监督</el-button>
            <el-button
              v-if="canDelete(row)"
              size="small"
              type="danger"
              :loading="operatingId === row.id"
              @click="handleDelete(row)"
            >删除用户</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, appointSupervisor, removeSupervisor, deleteUser, updateDisplayTitle } from '../api/auth'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const users = ref([])
const loading = ref(false)
const operatingId = ref(null)

onMounted(() => { loadUsers() })

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
  } finally {
    loading.value = false
  }
}

function roleLabel(role) {
  if (!role || role === 'USER') return '普通用户'
  if (role === 'ADMIN') return '管理员'
  if (role === 'SUPERVISOR') return '监督'
  return role
}

function roleTagType(role) {
  if (role === 'ADMIN') return 'danger'
  if (role === 'SUPERVISOR') return ''
  return 'info'
}

function displayTitle(row) {
  const custom = row.displayTitle?.trim()
  if (custom) return custom
  if (row.role === 'ADMIN') return '管理员'
  if (row.role === 'SUPERVISOR') return '监督'
  return ''
}

function titleClass(role) {
  if (role === 'ADMIN') return 'title-admin'
  if (role === 'SUPERVISOR') return 'title-supervisor'
  return ''
}

function canEditTitle(row) {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  return row.role === 'ADMIN' || row.role === 'SUPERVISOR'
}

function canAppoint(row) {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  if (row.id === store.userInfo.id) return false
  if (row.role === 'ADMIN') return false
  if (row.role === 'SUPERVISOR') return false
  if (row.isBot) return false
  return true
}

function canRemove(row) {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  if (row.id === store.userInfo.id) return false
  if (row.role !== 'SUPERVISOR') return false
  return true
}

function canDelete(row) {
  if (!store.userInfo || store.userInfo.role !== 'ADMIN') return false
  if (row.id === store.userInfo.id) return false
  if (row.role === 'ADMIN') return false
  if (row.role === 'SUPERVISOR') return false
  if (row.isBot) return false
  return true
}

async function handleAppoint(row) {
  try {
    await ElMessageBox.confirm(`确定将 ${row.username} 设为监督吗？`, '确认操作', { type: 'warning' })
  } catch { return }
  operatingId.value = row.id
  try {
    await appointSupervisor(row.id)
    ElMessage.success(`已将 ${row.username} 设为监督`)
    await loadUsers()
  } catch { /* handled by interceptor */ }
  finally { operatingId.value = null }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(`确定解除 ${row.username} 的监督权限吗？`, '确认操作', { type: 'warning' })
  } catch { return }
  operatingId.value = row.id
  try {
    await removeSupervisor(row.id)
    ElMessage.success(`已解除 ${row.username} 的监督权限`)
    await loadUsers()
  } catch { /* handled by interceptor */ }
  finally { operatingId.value = null }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？此操作不可恢复。`, '确认删除', { type: 'warning' })
  } catch { return }
  operatingId.value = row.id
  try {
    await deleteUser(row.id)
    ElMessage.success(`已删除用户 ${row.username}`)
    await loadUsers()
  } catch { /* handled by interceptor */ }
  finally { operatingId.value = null }
}

async function handleEditTitle(row) {
  let value
  try {
    const res = await ElMessageBox.prompt('留空将使用默认头衔', `编辑 ${row.username} 的公开头衔`, {
      inputValue: row.displayTitle || '',
      inputPlaceholder: row.role === 'ADMIN' ? '管理员' : '监督',
      inputValidator: (val) => !val || val.trim().length <= 50 || '公开头衔不能超过50个字符',
    })
    value = res.value
  } catch { return }
  operatingId.value = row.id
  try {
    await updateDisplayTitle(row.id, value)
    ElMessage.success('公开头衔已更新')
    await loadUsers()
  } catch { /* handled by interceptor */ }
  finally { operatingId.value = null }
}

function formatDate(t) {
  if (!t) return ''
  return t.split('T')[0]
}
</script>

<style scoped>
.admin-users { max-width: 980px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text); }
.users-table { width: 100%; }
.users-table :deep(.el-table__cell) { vertical-align: middle; }
.users-table :deep(.cell) { display: flex; align-items: center; min-height: 52px; }
.users-table :deep(th .cell) { min-height: auto; }
.users-table :deep(.el-table__row),
.users-table :deep(.el-table__row > td) {
  transition: background-color 200ms ease;
}
.users-table :deep(.el-table__row:hover > td) {
  background-color: rgba(99, 102, 241, 0.04) !important;
}
.action-cell { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 52px; }
.action-cell :deep(.el-button + .el-button) { margin-left: 0; }
.text-muted { color: var(--text-muted); }
.title-badge {
  display: inline-flex; align-items: center; max-width: 120px; padding: 2px 8px;
  border-radius: 6px; font-size: 12px; font-weight: 700; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-admin { color: #be123c; background: #ffe4ec; border: 1px solid #fecdd3; }
.title-supervisor { color: #5b21b6; background: #ede9fe; border: 1px solid #ddd6fe; }

@media (max-width: 768px) {
  .admin-users {
    max-width: none;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  .page-header { margin-bottom: 14px; }
  .page-title { font-size: 19px; }
  .users-table {
    min-width: 820px;
  }
  .action-cell {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
