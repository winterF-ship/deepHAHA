<template>
  <el-dialog
    v-model="visible"
    title="裁剪头像"
    width="720px"
    :close-on-click-modal="false"
    class="avatar-cropper-dialog"
    @closed="cleanup"
  >
    <div class="cropper-layout">
      <div class="cropper-stage">
        <img ref="imageRef" :src="imageUrl" alt="头像裁剪" @error="handleImageError" />
      </div>
      <div class="preview-panel">
        <div class="preview-title">预览</div>
        <div class="avatar-preview-circle"></div>
        <el-slider v-model="zoomValue" :min="0.5" :max="3" :step="0.05" @input="handleZoom" />
      </div>
    </div>
    <template #footer>
      <el-button size="large" @click="cancel">取消</el-button>
      <el-button size="large" type="primary" :loading="confirming" @click="confirm">确认上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  file: File
})
const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(false)
const imageRef = ref(null)
const imageUrl = ref('')
const cropper = ref(null)
const zoomValue = ref(1)
const confirming = ref(false)

watch(() => props.modelValue, async val => {
  visible.value = val
  if (val && props.file) {
    imageUrl.value = URL.createObjectURL(props.file)
    await nextTick()
    initCropper()
  }
})

watch(visible, val => emit('update:modelValue', val))

function initCropper() {
  if (!imageRef.value) return
  cropper.value?.destroy()
  cropper.value = new Cropper(imageRef.value, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.86,
    background: false,
    responsive: true,
    restore: false,
    guides: false,
    center: true,
    highlight: false,
    cropBoxMovable: true,
    cropBoxResizable: false,
    toggleDragModeOnDblclick: false,
    preview: '.avatar-preview-circle',
    ready() {
      zoomValue.value = 1
    }
  })
}

function handleZoom(value) {
  if (!cropper.value) return
  const canvas = cropper.value.getCanvasData()
  if (!canvas?.naturalWidth) return
  cropper.value.zoomTo(value)
}

async function confirm() {
  if (!cropper.value || !props.file) return
  confirming.value = true
  try {
    const canvas = cropper.value.getCroppedCanvas({
      width: 512,
      height: 512,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high',
      fillColor: '#fff'
    })
    if (!canvas) throw new Error('crop failed')
    canvas.toBlob(blob => {
      confirming.value = false
      if (!blob) {
        ElMessage.error('头像裁剪失败，请换一张图片')
        return
      }
      const ext = props.file.type === 'image/png' ? 'png' : 'jpg'
      const type = ext === 'png' ? 'image/png' : 'image/jpeg'
      const cropped = new File([blob], `avatar_${Date.now()}.${ext}`, { type })
      emit('confirm', cropped)
      visible.value = false
    }, props.file.type === 'image/png' ? 'image/png' : 'image/jpeg', 0.92)
  } catch {
    confirming.value = false
    ElMessage.error('头像裁剪失败，请换一张图片')
  }
}

function cancel() {
  emit('cancel')
  visible.value = false
}

function handleImageError() {
  ElMessage.error('图片加载失败，请换一张图片')
  cancel()
}

function cleanup() {
  cropper.value?.destroy()
  cropper.value = null
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
  confirming.value = false
}
</script>

<style scoped>
.cropper-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px;
  gap: 18px;
  align-items: start;
}
.cropper-stage {
  width: 100%;
  height: min(52vh, 420px);
  min-height: 300px;
  background: #f8fafc;
  border: 1px solid #e5eaf2;
  border-radius: 12px;
  overflow: hidden;
}
.cropper-stage img {
  display: block;
  max-width: 100%;
}
.preview-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.preview-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}
.avatar-preview-circle {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
}

:deep(.cropper-view-box),
:deep(.cropper-face) {
  border-radius: 50%;
}

@media (max-width: 680px) {
  :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin: 12px auto;
  }
  :deep(.el-dialog__body) {
    padding: 12px;
  }
  :deep(.el-dialog__footer) {
    padding: 12px;
  }
  .cropper-layout {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .cropper-stage {
    height: min(58vh, 360px);
    min-height: 260px;
  }
  .preview-panel {
    gap: 10px;
  }
  .avatar-preview-circle {
    width: 88px;
    height: 88px;
  }
}
</style>
