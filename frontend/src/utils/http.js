import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'

const httpInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080',
  timeout: 10000,
  withCredentials: true
})

httpInstance.interceptors.request.use(config => {
  // The backend stores this value as a Redis session id. Keep the legacy
  // token fallback so an existing browser session is not logged out abruptly.
  const sessionId = localStorage.getItem('sessionId') || localStorage.getItem('token')
  if (sessionId) {
    config.headers.SessionId = sessionId
  }

  // Legacy API modules still add Authorization themselves. Remove it so all
  // requests use the SessionId contract.
  delete config.headers.Authorization
  return config
}, error => Promise.reject(error))

httpInstance.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('sessionId')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/')
      ElMessage.error('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)

export default httpInstance
