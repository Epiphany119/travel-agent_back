import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 请求拦截器：自动注入 Authorization 头
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('roamly_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理业务错误
instance.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 401 未授权 → 清除 token 并跳转登录页
    if (error?.response?.status === 401) {
      localStorage.removeItem('roamly_token')
      localStorage.removeItem('roamly_user_id')
      localStorage.removeItem('roamly_username')
      if (!window.location.pathname.includes('/auth')) {
        window.location.href = '/auth'
      }
    }
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// 上传头像
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return instance.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取头像
export const getAvatar = () => {
  return instance.get('/user/avatar')
}

export default instance
