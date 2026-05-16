import request from './request'

export function getBots() {
  return request.get('/admin/bots')
}

export function createBot(data) {
  return request.post('/admin/bots', data)
}

export function updateBot(id, data) {
  return request.put(`/admin/bots/${id}`, data)
}

export function generatePostPreview(id, data) {
  return request.post(`/admin/bots/${id}/generate-post-preview`, data, { timeout: 60000 })
}

export function publishBotPost(id, data) {
  return request.post(`/admin/bots/${id}/publish-post`, data)
}

export function uploadBotAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/bots/upload-avatar', formData)
}
