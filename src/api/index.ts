import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000
})

instance.interceptors.response.use(
  (response) => response.data,
  (error) => {
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
