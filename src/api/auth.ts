import request from './index'

export interface AuthResult {
  token: string
  userId: string
  username: string
  expiresIn: number
}

type ApiEnvelope<T> = {
  data: T
  message?: string
  code?: number
}

/** 密码登录 */
export async function login(data: { username: string; password: string }) {
  const r = await request.post<any, ApiEnvelope<AuthResult>>('/auth/login', data)
  return r.data
}

/** 用户注册 */
export async function register(data: {
  username: string
  password: string
  confirmPassword: string
  email?: string
  emailCode?: string
}) {
  const r = await request.post<any, ApiEnvelope<AuthResult>>('/auth/register', data)
  return r.data
}

/** 发送邮箱验证码 */
export function sendEmailCode(email: string) {
  return request.post('/auth/email/send-code', { email })
}

/** 邮箱验证码登录 */
export async function emailLogin(data: { email: string; code: string }) {
  const r = await request.post<any, ApiEnvelope<AuthResult>>('/auth/email/login', data)
  return r.data
}

/** 校验当前 Token 是否有效 */
export function verifyAuth() {
  return request.get<any, { authenticated: boolean; userId?: string }>('/auth/verify')
}

/** 登出 */
export function logout() {
  return request.post('/auth/logout')
}

/** 绑定邮箱 */
export function bindEmail(data: { email: string; code: string }) {
  return request.post('/auth/bind-email', data)
}

/** 解绑邮箱 */
export function unbindEmail() {
  return request.post('/auth/unbind-email')
}
