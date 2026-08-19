import axios from 'axios'
import { useAdminStore } from '../stores/adminStore'
import type { ApiResult } from '../types'

const adminApi = axios.create({
  baseURL: '/api/admin',
  timeout: 10000,
})

adminApi.interceptors.request.use((config) => {
  const token = useAdminStore.getState().token
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

adminApi.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult<unknown>
    if (data.code === 200) {
      return { ...response, data: data.data }
    }
    if (data.code === 401) {
      useAdminStore.getState().logout()
      if (typeof window !== 'undefined') window.location.href = '/admin'
    }
    return Promise.reject(new Error(data.message))
  },
  (error) => {
    if (error.response?.status === 401) {
      useAdminStore.getState().logout()
      if (typeof window !== 'undefined') window.location.href = '/admin'
    }
    const data = error.response?.data as ApiResult<unknown>
    return Promise.reject(new Error(data?.message || '网络错误'))
  }
)

export default adminApi
