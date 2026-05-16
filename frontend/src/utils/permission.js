/**
 * 判断当前用户是否可以删除目标作者的内容。
 * @param {object|null} currentUser - 当前登录用户 { id, role }
 * @param {object} targetAuthor - 目标内容作者 { id, role, isBot }
 * @returns {boolean}
 */
export function canDeleteContent(currentUser, targetAuthor) {
  if (!currentUser || !targetAuthor) return false

  // 作者本人可以删除自己的内容
  if (currentUser.id === targetAuthor.id) return true

  // ADMIN 可以删除所有内容
  if (currentUser.role === 'ADMIN') return true

  // SUPERVISOR 可以删除普通 USER 和 AI 用户的内容
  if (currentUser.role === 'SUPERVISOR') {
    if (targetAuthor.role === 'ADMIN') return false
    if (targetAuthor.role === 'SUPERVISOR') return false
    return true
  }

  return false
}
