import request from './request'

export function getPosts(categoryId, page = 0, size = 10) {
  return request.get('/posts', { params: { categoryId, page, size } })
}

export function getPostDetail(id) {
  return request.get(`/posts/${id}`)
}

export function createPost(title, content, categoryId) {
  return request.post('/posts', { title, content, categoryId })
}

export function createReply(postId, content, parentReplyId) {
  return request.post(`/posts/${postId}/replies`, { content, parentReplyId })
}

export function deleteReply(postId, replyId) {
  return request.delete(`/posts/${postId}/replies/${replyId}`)
}

export function toggleLike(postId) {
  return request.post(`/posts/${postId}/like`)
}

export function deletePost(id) {
  return request.delete(`/posts/${id}`)
}

export function toggleFavorite(postId) {
  return request.post(`/posts/${postId}/favorite`)
}

