import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

function getErrorMessage(error) {
  if (error.code === 'ECONNABORTED') return '请求超时，请检查后端服务是否正常启动'
  if (!error.response) return '无法连接后端服务，请先启动后端'
  const status = error.response.status
  if (status === 404) return '接口不存在，请检查前后端路径是否一致'
  if (status >= 500) return '服务器内部错误，请查看后端控制台'
  return error.response.data?.message || '请求失败，请稍后重试'
}

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      const err = new Error(data.message || '请求失败')
      err._handled = true
      return Promise.reject(err)
    }
    return data
  },
  error => {
    if (error._handled) {
      return Promise.reject(error)
    }
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(getErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

export default request