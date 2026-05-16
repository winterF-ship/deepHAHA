import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  function setLogin(tokenVal, user) {
    token.value = tokenVal
    userInfo.value = user
    localStorage.setItem('token', tokenVal)
    localStorage.setItem('userInfo', JSON.stringify(user))
  }

  function updateUser(user) {
    userInfo.value = user
    localStorage.setItem('userInfo', JSON.stringify(user))
  }

  async function refreshUserInfo() {
    if (!token.value) return
    try {
      const res = await getUserInfo()
      if (res.data) {
        userInfo.value = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
    } catch { /* ignore */ }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isLoggedIn = () => !!token.value

  return { token, userInfo, setLogin, updateUser, refreshUserInfo, logout, isLoggedIn }
})
