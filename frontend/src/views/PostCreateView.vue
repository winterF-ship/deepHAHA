<template>
  <div class="post-create">
    <!-- 顶部导航 -->
    <div class="page-top">
      <button class="back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div>
        <h1 class="page-title">发布新帖</h1>
        <p class="page-sub">分享你的想法、经验或问题</p>
      </div>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="选择板块">
          <el-select
            v-model="form.categoryId"
            placeholder="请选择帖子所属板块"
            size="large"
            class="full-width"
          >
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="帖子标题">
          <el-input
            v-model="form.title"
            placeholder="起一个吸引人的标题吧"
            maxlength="100"
            show-word-limit
            size="large"
          />
        </el-form-item>

        <el-form-item label="帖子内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="在这里写下你的内容…支持 Markdown 语法"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <div class="form-actions">
          <el-button size="large" @click="$router.back()" class="cancel-btn">取消</el-button>
          <el-button
            type="primary"
            size="large"
            @click="submit"
            :loading="submitting"
            class="submit-btn"
          >
            <el-icon v-if="!submitting"><Upload /></el-icon>
            {{ submitting ? '发布中…' : '发布帖子' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { getCategories } from '../api/category'
import { createPost } from '../api/post'
import { ElMessage } from 'element-plus'

const router = useRouter()
const categories = ref([])
const form = ref({ title: '', content: '', categoryId: null })
const submitting = ref(false)

onMounted(async () => {
  const res = await getCategories()
  categories.value = res.data
})

async function submit() {
  if (!form.value.categoryId) { ElMessage.warning('请选择板块'); return }
  if (!form.value.title.trim()) { ElMessage.warning('标题不能为空'); return }
  if (!form.value.content.trim()) { ElMessage.warning('内容不能为空'); return }
  submitting.value = true
  try {
    const res = await createPost(form.value.title.trim(), form.value.content.trim(), form.value.categoryId)
    ElMessage.success('发帖成功')
    router.push(`/post/${res.data.id}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.post-create { max-width: 760px; margin: 0 auto; }

/* Top bar */
.page-top { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.back-btn {
  width: 40px; height: 40px; border-radius: 12px; border: 1.5px solid var(--border);
  background: var(--bg-card); cursor: pointer; display: grid; place-items: center;
  font-size: 18px; color: var(--text-secondary); transition: all var(--transition);
  flex-shrink: 0;
}
.back-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-soft); }
.page-title { font-size: 22px; font-weight: 750; color: var(--text); letter-spacing: -0.3px; }
.page-sub { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

/* Form card */
.form-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 32px; border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
}
.form-card :deep(.el-form-item__label) {
  font-size: 14px; font-weight: 600; color: var(--text); padding-bottom: 6px;
}
.form-card :deep(.el-input__wrapper) {
  border-radius: 10px; box-shadow: none; border: 1.5px solid var(--border);
  transition: all var(--transition);
}
.form-card :deep(.el-input__wrapper:hover) { border-color: #c0c8d6; }
.form-card :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow);
}
.form-card :deep(.el-textarea__inner) {
  border-radius: 10px; border: 1.5px solid var(--border);
  font-size: 15px; line-height: 1.7; padding: 14px 16px;
  transition: all var(--transition); font-family: inherit;
}
.form-card :deep(.el-textarea__inner:focus) {
  border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow);
}
.form-card :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}
.form-card :deep(.el-form-item) { margin-bottom: 22px; }

.full-width { width: 100%; }

/* Actions */
.form-actions {
  display: flex; justify-content: flex-end; gap: 12px;
  margin-top: 4px; padding-top: 24px; border-top: 1px solid var(--border-light);
}
.cancel-btn {
  border-radius: 10px; font-weight: 500; border: 1.5px solid var(--border);
  color: var(--text-secondary); background: transparent; padding: 10px 24px;
  transition: all var(--transition);
}
.cancel-btn:hover { border-color: #c0c8d6; color: var(--text); }
.submit-btn {
  border-radius: 10px; font-weight: 650; padding: 10px 28px;
  background: linear-gradient(135deg, #5b6eeb, #7c5ce7) !important;
  border: none !important; box-shadow: 0 4px 14px rgba(91, 110, 235, 0.28);
  transition: all var(--transition); display: flex; align-items: center; gap: 6px;
}
.submit-btn:hover {
  transform: translateY(-1px); box-shadow: 0 6px 20px rgba(91, 110, 235, 0.36);
}

@media (max-width: 768px) {
  .page-top { gap: 12px; margin-bottom: 18px; }
  .form-card { padding: 20px 16px; border-radius: var(--radius); }
  .page-title { font-size: 19px; }
  .page-sub { font-size: 12px; }
  .form-card :deep(.el-form-item) { margin-bottom: 18px; }
  .form-card :deep(.el-textarea__inner) { min-height: 220px !important; }
  .form-actions { flex-direction: column-reverse; }
  .form-actions .el-button { width: 100%; justify-content: center; }
}

@media (max-width: 520px) {
  .page-top { align-items: flex-start; }
  .back-btn { width: 36px; height: 36px; }
  .form-card { padding: 16px 12px; }
}
</style>
