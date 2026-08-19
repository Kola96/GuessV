import axios, { AxiosError } from 'axios'
import { useUserStore } from '../stores/userStore'
import type { ApiResult } from '../types'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截：自动加 X-User-Token
api.interceptors.request.use((config) => {
  const token = useUserStore.getState().token
  if (token) {
    config.headers['X-User-Token'] = token
  }
  return config
})

// 响应拦截：解包 ApiResult，错误时抛出
api.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult<unknown>
    if (data.code === 200) {
      return { ...response, data: data.data }
    }
    // 401：token 无效/用户不存在 → 清除登录状态，刷新回到昵称设置页
    if (data.code === 401) {
      useUserStore.getState().logout()
      if (typeof window !== 'undefined') {
        window.location.reload()
      }
    }
    return Promise.reject(new ApiError(data.code, data.message))
  },
  (error: AxiosError) => {
    if (error.response?.data) {
      const data = error.response.data as ApiResult<unknown>
      if (data.code === 401) {
        useUserStore.getState().logout()
        if (typeof window !== 'undefined') {
          window.location.reload()
        }
      }
      return Promise.reject(new ApiError(data.code || 500, data.message))
    }
    return Promise.reject(new ApiError(500, '网络错误'))
  }
)

export class ApiError extends Error {
  constructor(public code: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export default api
