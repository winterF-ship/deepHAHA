import request from './request'

export function getRepliesToMe() {
  return request.get('/messages/replies')
}
