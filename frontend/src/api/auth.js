import request from './request'

export function login(username, password, captchaId, captchaCode) {
  return request.post('/auth/login', { username, password, captchaId, captchaCode })
}

export function getCaptcha() {
  return request.get('/captcha')
}

export function register(username, password, email) {
  return request.post('/auth/register', { username, password, email })
}

export function getUserInfo() {
  return request.get('/user/me')
}

export function updateProfile(username) {
  return request.put('/user/profile', { username })
}

export function updatePassword(oldPassword, newPassword) {
  return request.put('/user/password', { oldPassword, newPassword })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData)
}

export function getNotice() {
  return request.get('/notice')
}

export function updateNotice(content) {
  return request.put('/admin/notice', { content })
}

export function getUsers() {
  return request.get('/admin/users')
}

export function muteUser(userId, days, reason) {
  return request.put(`/admin/users/${userId}/mute`, { days, reason })
}

export function unmuteUser(userId) {
  return request.put(`/admin/users/${userId}/unmute`)
}

export function appointSupervisor(userId) {
  return request.post(`/admin/users/${userId}/supervisor`)
}

export function removeSupervisor(userId) {
  return request.delete(`/admin/users/${userId}/supervisor`)
}

export function updateDisplayTitle(userId, displayTitle) {
  return request.put(`/admin/users/${userId}/display-title`, { displayTitle })
}

export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}
