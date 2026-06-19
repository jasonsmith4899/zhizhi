import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../stores/auth'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const request = axios.create({
  baseURL,
  timeout: 30000,
})

// 无拦截器的独立实例，仅用于刷新token
const refreshRequest = axios.create({ baseURL, timeout: 30000 })

// 请求拦截器
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 刷新token相关状态
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (err: any) => void
}> = []

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // blob 响应直接返回 data（Blob），不做 code 检查
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const { data } = response
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      if (data.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        router.push('/login')
      }
      return Promise.reject(data)
    }
    return data
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('refreshToken')

      if (!refreshToken) {
        localStorage.removeItem('token')
        router.push('/login')
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingRequests.push({
            resolve: (newToken: string) => {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(request(originalRequest))
            },
            reject,
          })
        })
      }

      isRefreshing = true
      originalRequest._retry = true

      try {
        const res = await refreshRequest.post('/auth/refresh', { refreshToken })

        if (res.data.code === 200) {
          const newToken = res.data.data.token
          const authStore = useAuthStore()
          authStore.setToken(newToken)

          // 执行等待中的请求
          pendingRequests.forEach(({ resolve }) => resolve(newToken))
          pendingRequests = []

          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        }
      } catch (refreshError) {
        // F5: 刷新失败时 reject 所有挂起的请求
        pendingRequests.forEach(({ reject }) => reject(refreshError))
        pendingRequests = []

        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        router.push('/login')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    ElMessage.error(error.response?.data?.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
