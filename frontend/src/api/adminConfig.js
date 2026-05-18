import request from './request'

export function getSystemStatus() {
  return request.get('/admin/system-status')
}

export function getAutoPostConfig() {
  return request.get('/admin/auto-post-config')
}

export function updateAutoPostConfig(data) {
  return request.put('/admin/auto-post-config', data)
}
