<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, sendEmailCode, emailLogin } from '@/api/auth'

const router = useRouter()
const route = useRoute()

// 模式切换
const mode = ref<'login' | 'register'>('login')
const method = ref<'password' | 'email'>('password')

// 表单数据
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const code = ref('')

// 密码可见性
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// 状态
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | undefined

// 邮箱正则
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const USERNAME_REGEX = /^[A-Za-z0-9_]{3,32}$/

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

function clearCountdown() {
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
  countdown.value = 0
}

onBeforeUnmount(() => {
  clearCountdown()
})

async function handleSendCode() {
  if (!email.value || !EMAIL_REGEX.test(email.value)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  if (countdown.value > 0) return

  sendingCode.value = true
  try {
    await sendEmailCode(email.value.trim())
    ElMessage.success('验证码已发送，请查收邮件')
    startCountdown()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '验证码发送失败'
    ElMessage.error(msg)
  } finally {
    sendingCode.value = false
  }
}

function validateForm(): boolean {
  if (method.value === 'password') {
    if (!username.value || !USERNAME_REGEX.test(username.value)) {
      ElMessage.warning('用户名需为 3-32 位字母、数字或下划线')
      return false
    }
    if (!password.value || password.value.length < 8) {
      ElMessage.warning('密码至少 8 位')
      return false
    }
    if (!/[A-Z]/.test(password.value) || !/[a-z]/.test(password.value) || !/\d/.test(password.value)) {
      ElMessage.warning('密码必须包含大小写字母和数字')
      return false
    }
    // 注册时校验确认密码
    if (mode.value === 'register') {
      if (!confirmPassword.value) {
        ElMessage.warning('请再次输入密码')
        return false
      }
      if (password.value !== confirmPassword.value) {
        ElMessage.warning('两次输入的密码不一致')
        return false
      }
    }
  }

  // 邮箱方式或注册填邮箱时，需要验证码
  const needsCode =
    method.value === 'email' ||
    (mode.value === 'register' && email.value.trim() !== '')

  if (needsCode) {
    if (!email.value || !EMAIL_REGEX.test(email.value)) {
      ElMessage.warning('请输入正确的邮箱地址')
      return false
    }
    if (!code.value || code.value.length !== 6) {
      ElMessage.warning('请输入 6 位验证码')
      return false
    }
  }

  return true
}

async function handleSubmit() {
  if (!validateForm()) return

  loading.value = true
  try {
    let result: { token: string; userId: string; username: string }

    if (method.value === 'email') {
      // 邮箱登录：如果用户不存在，自动创建（邮箱即用户名）
      result = await emailLogin({
        email: email.value.trim(),
        code: code.value.trim()
      })
    } else if (mode.value === 'login') {
      result = await login({
        username: username.value.trim(),
        password: password.value
      })
    } else {
      // 注册
      result = await register({
        username: username.value.trim(),
        password: password.value,
        confirmPassword: confirmPassword.value,
        email: email.value.trim() || undefined,
        emailCode: email.value.trim() ? code.value.trim() : undefined
      })
    }

    localStorage.setItem('roamly_token', result.token)
    localStorage.setItem('roamly_user_id', result.userId)
    localStorage.setItem('roamly_username', result.username)

    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')

    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '操作失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function switchMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  code.value = ''
  password.value = ''
  confirmPassword.value = ''
  clearCountdown()
}

function switchMethod() {
  method.value = method.value === 'password' ? 'email' : 'password'
  code.value = ''
  clearCountdown()
}

// 注册模式下，填写了邮箱才显示验证码
const showCodeField = () => {
  if (method.value === 'email') return true
  if (mode.value === 'register' && email.value.trim() !== '') return true
  return false
}

// 密码眼睛 SVG
const eyeOpen = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`
const eyeClosed = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <!-- Logo -->
      <div class="brand-mark">✦</div>
      <p class="brand-eyebrow">ROAMLY</p>

      <!-- 标题 -->
      <h1 class="auth-title">
        {{ mode === 'login' ? '欢迎回来' : '创建你的 Roamly 账号' }}
      </h1>
      <p class="auth-sub">
        {{ mode === 'login'
          ? '登录后保存旅行计划，发布你的真实路线与灵感。'
          : '注册后即可开启你的旅行规划之旅。'
        }}
      </p>

      <!-- 登录方式切换 -->
      <div class="method-tabs">
        <button
          :class="{ active: method === 'password' }"
          @click="switchMethod"
          type="button"
        >
          密码{{ mode === 'login' ? '登录' : '注册' }}
        </button>
        <button
          :class="{ active: method === 'email' }"
          @click="switchMethod"
          type="button"
        >
          邮箱验证码
        </button>
      </div>

      <!-- 密码方式表单 -->
      <template v-if="method === 'password'">
        <template v-if="mode === 'login' || mode === 'register'">
          <label class="form-label">
            用户名
            <input
              v-model="username"
              type="text"
              autocomplete="username"
              placeholder="3-32 位字母、数字或下划线"
              class="form-input"
            />
          </label>
        </template>

        <label class="form-label">
          密码
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="至少 8 位，包含大小写字母和数字"
              class="form-input password-input"
            />
            <button
              type="button"
              class="password-toggle"
              @click="showPassword = !showPassword"
              :title="showPassword ? '隐藏密码' : '显示密码'"
            >
              <span v-html="showPassword ? eyeClosed : eyeOpen"></span>
            </button>
          </div>
        </label>

        <!-- 注册模式：确认密码 -->
        <label v-if="mode === 'register'" class="form-label">
          确认密码
          <div class="password-wrapper">
            <input
              v-model="confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="再次输入密码"
              class="form-input password-input"
              @input="() => { if (confirmPassword && confirmPassword !== password) {} }"
            />
            <button
              type="button"
              class="password-toggle"
              @click="showConfirmPassword = !showConfirmPassword"
              :title="showConfirmPassword ? '隐藏密码' : '显示密码'"
            >
              <span v-html="showConfirmPassword ? eyeClosed : eyeOpen"></span>
            </button>
          </div>
          <span v-if="confirmPassword && confirmPassword !== password" class="form-error">
            两次密码输入不一致
          </span>
        </label>

        <!-- 注册模式：邮箱（选填） -->
        <label v-if="mode === 'register'" class="form-label">
          邮箱
          <input
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="选填，用于接收验证码"
            class="form-input"
            @blur="() => { if (!email) { code = ''; clearCountdown() } }"
          />
          <span class="form-hint">填写邮箱后需完成验证，便于找回账号</span>
        </label>
      </template>

      <!-- 邮箱验证码方式 -->
      <template v-if="method === 'email'">
        <label class="form-label">
          邮箱
          <input
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="请输入邮箱地址"
            class="form-input"
          />
        </label>
        <p class="form-hint">
          {{ mode === 'login'
            ? '未注册的邮箱将自动创建新账号（用户名即邮箱）'
            : '注册新账号'
          }}
        </p>
      </template>

      <!-- 验证码输入框 -->
      <template v-if="showCodeField()">
        <label class="form-label">
          验证码
          <div class="code-row">
            <input
              v-model="code"
              type="text"
              maxlength="6"
              placeholder="6 位验证码"
              class="form-input code-input"
            />
            <button
              class="code-btn"
              :disabled="countdown > 0 || sendingCode"
              @click="handleSendCode"
              type="button"
            >
              {{ sendingCode ? '发送中…' : countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </div>
        </label>
      </template>

      <!-- 提交按钮 -->
      <button
        class="submit-btn"
        :disabled="loading"
        @click="handleSubmit"
        type="button"
      >
        {{ loading ? '处理中…' : (
          method === 'email'
            ? (mode === 'login' ? '邮箱登录 / 注册' : '邮箱注册')
            : (mode === 'login' ? '登录' : '注册并登录')
        ) }}
      </button>

      <!-- 切换登录/注册 -->
      <button class="switch-btn" @click="switchMode" type="button">
        {{ mode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: var(--paper);
  padding: 24px;
}

.auth-card {
  width: min(430px, 100%);
  padding: 40px 36px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--card);
  box-shadow: var(--shadow-lift);
}

/* Brand */
.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--forest);
  color: #fff;
  font-size: 24px;
  margin-bottom: 4px;
}

.brand-eyebrow {
  color: var(--sunset);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
  margin: 14px 0 6px;
}

.auth-title {
  font-family: 'DM Serif Display', serif;
  font-size: 28px;
  font-weight: 400;
  margin: 0 0 6px;
  color: var(--ink);
}

.auth-sub {
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 22px;
}

/* Tabs */
.method-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.method-tabs button {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--line);
  background: transparent;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.15s ease;
}

.method-tabs button:hover {
  border-color: var(--forest);
  color: var(--forest);
}

.method-tabs button.active {
  background: var(--forest);
  color: #fff;
  border-color: var(--forest);
}

/* Form */
.form-label {
  display: block;
  color: var(--ink-2);
  font-size: 12px;
  font-weight: 700;
  margin: 14px 0 4px;
}

.form-input {
  display: block;
  width: 100%;
  margin-top: 6px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: var(--ink);
  box-sizing: border-box;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
}

.form-input::placeholder {
  color: var(--ink-3);
}

.form-input:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(22, 78, 66, 0.08);
}

/* Password wrapper with eye toggle */
.password-wrapper {
  position: relative;
  margin-top: 6px;
}

.password-input {
  padding-right: 44px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  display: grid;
  place-items: center;
  border-radius: 8px;
  transition: all 0.15s ease;
}

.password-toggle:hover {
  color: var(--forest);
  background: rgba(22, 78, 66, 0.06);
}

.form-hint {
  display: block;
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
  font-weight: 500;
}

.form-error {
  display: block;
  font-size: 11px;
  color: #e74c3c;
  margin-top: 4px;
  font-weight: 600;
}

/* Code row */
.code-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-top: 6px;
}

.code-input {
  flex: 1;
  letter-spacing: 0.3em;
  text-align: center;
}

.code-btn {
  flex-shrink: 0;
  margin-top: 0;
  padding: 12px 16px;
  border: 1px solid var(--forest);
  border-radius: 10px;
  background: transparent;
  color: var(--forest);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  height: 44px;
  line-height: 1;
}

.code-btn:hover:not(:disabled) {
  background: var(--forest);
  color: #fff;
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: var(--line);
  color: var(--ink-3);
}

/* Submit */
.submit-btn {
  width: 100%;
  margin-top: 24px;
  padding: 14px;
  border: none;
  border-radius: 10px;
  background: var(--forest);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.15s ease;
  font-family: inherit;
}

.submit-btn:hover:not(:disabled) {
  background: var(--forest-deep);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Switch */
.switch-btn {
  width: 100%;
  margin-top: 16px;
  border: none;
  background: transparent;
  color: var(--forest);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease;
}

.switch-btn:hover {
  color: var(--forest-deep);
  text-decoration: underline;
}
</style>
