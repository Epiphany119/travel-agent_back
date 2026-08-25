<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPreferences, savePreferences, uploadAvatar, getAvatar, updateNickname, type UserPreference } from '@/api/user'
import { logout as apiLogout, sendEmailCode, bindEmail as apiBindEmail, unbindEmail as apiUnbindEmail } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)
const loaded = ref(false)
const paletteOpen = ref(false)
const systemPalette = reactive({
  fg: '#1D2B27', bg: '#F7F3EA', accent: '#164E42'
})
const palettePresets = [
  { name: 'Roamly', fg: '#1D2B27', bg: '#F7F3EA', accent: '#164E42' },
  { name: '海岸', fg: '#17324D', bg: '#EEF7FA', accent: '#147D92' },
  { name: '日落', fg: '#43251C', bg: '#FFF5ED', accent: '#C85A36' },
  { name: '墨绿', fg: '#E8F1EC', bg: '#10241E', accent: '#4FBE91' }
]

function applySystemPalette() {
  const root = document.documentElement
  root.style.setProperty('--ink', systemPalette.fg)
  root.style.setProperty('--paper', systemPalette.bg)
  root.style.setProperty('--forest', systemPalette.accent)
}

async function saveSystemPalette() {
  saving.value = true
  try {
    await savePreferences({
      ...form,
      systemThemeJson: JSON.stringify(systemPalette),
      userId: localStorage.getItem('roamly_user_id') || 'user_001'
    })
    form.systemThemeJson = JSON.stringify(systemPalette)
    applySystemPalette()
    paletteOpen.value = false
    ElMessage.success('系统主题已保存')
  } catch (e) {
    console.error(e)
    ElMessage.error('系统主题保存失败')
  } finally { saving.value = false }
}

function closePalette() {
  // Discard unsaved edits and restore the last database-backed palette.
  loadSystemPalette()
  paletteOpen.value = false
}

function loadSystemPalette() {
  try {
    const saved = form.systemThemeJson ? JSON.parse(form.systemThemeJson) : {}
    Object.assign(systemPalette, {
      fg: saved.fg || systemPalette.fg, bg: saved.bg || systemPalette.bg, accent: saved.accent || systemPalette.accent
    })
  } catch {}
  applySystemPalette()
}

// 头像
const avatar = ref<string>('')
const uploadPreview = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const isUploading = ref(false)
const uploadRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)

// 绑定邮箱相关
const bindingFormOpen = ref(false)
const bindingEmail = ref('')
const bindingCode = ref('')
const bindingLoading = ref(false)
const bindingSending = ref(false)
const bindingCountdown = ref(0)
let bindingTimer: ReturnType<typeof setInterval> | undefined

// 解绑相关
const unbindingLoading = ref(false)

// 个人资料 + 偏好
const form = reactive<UserPreference>({
  name: '', email: '', phone: '',
  favoriteDestinations: '', defaultDepartureCity: '',
  defaultDays: 5, defaultBudget: 8000, defaultTravelers: 2,
  preferredSeason: '不限', preferredTripType: '深度体验', budgetLevel: '舒适',
  travelStyle: '', interests: '', attractionTypes: '',
  preferFreeAttractions: false,
  dietaryRequirements: '', preferredCuisines: '', spicyLevel: 1,
  accommodationType: '', hotelStarMin: 3,
  transportationPreference: '',
  notifyBeforeTripDays: 3, notifyWeatherAlert: true, notifyPriceChange: true
})

const interestOptions = ['美食','人文','自然','摄影','购物','夜生活','历史','艺术','冒险','户外','亲子','养生','音乐','建筑']
const styleOptions = ['轻松漫游','深度人文','美食优先','亲子友好','户外探险','奢华度假','都市探索','自然风光']
const attractionOptions = ['自然','人文','历史遗迹','博物馆','主题公园','古镇','海滩','雪山','动物园','水族馆']
const dietOptions = ['无特殊要求','清真','素食','纯素','无麸质','忌辣','忌海鲜','儿童餐']
const cuisineOptions = ['川菜','粤菜','湘菜','江浙菜','本帮菜','日料','韩餐','东南亚菜','西餐','火锅','烧烤','家常菜']
const accommodationOptions = ['酒店','民宿','青旅','度假村','精品酒店','公寓']
const transportOptions = ['高铁','自驾','飞机','地铁','公交','骑行','徒步','包车']

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function toList(v: unknown): string[] {
  if (!v) return []
  const s = String(v).trim()
  if (!s) return []
  if (s.startsWith('[')) { try { return JSON.parse(s) } catch { return [] } }
  return s.split(/[,，]/).map(x => x.trim()).filter(Boolean)
}

const interests = computed({ get: () => toList(form.interests), set: (v) => { form.interests = v.join(',') } })
const attractionTypes = computed({ get: () => toList(form.attractionTypes), set: (v) => { form.attractionTypes = v.join(',') } })
const dietaryRequirements = computed({ get: () => toList(form.dietaryRequirements), set: (v) => { form.dietaryRequirements = v.join(',') } })
const preferredCuisines = computed({ get: () => toList(form.preferredCuisines), set: (v) => { form.preferredCuisines = v.join(',') } })
const accommodationType = computed({ get: () => toList(form.accommodationType), set: (v) => { form.accommodationType = v.join(',') } })
const transportationPreference = computed({ get: () => toList(form.transportationPreference), set: (v) => { form.transportationPreference = v.join(',') } })

function resolveUrl(url: string) {
  if (!url) return ''
  return url.startsWith('http') ? url : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
}

async function load() {
  try {
    const prefRes = await getPreferences()
    const p = prefRes.data || {}
    Object.assign(form, p)
    loadSystemPalette()
    // 关键：从后端合并的 auth_account 数据中获取邮箱
    // 后端 getPreferences 接口现在会返回 email 字段（来自 auth_account 表）
    if (form.email) {
      console.log('加载到绑定邮箱:', form.email)
    } else {
      console.log('当前用户未绑定邮箱')
    }
    if (form.name) {
      userStore.setNickname(form.name)
    } else if (p.username) {
      // 如果 nickname 为空，使用 auth_account 的 username
      userStore.setNickname(p.username as string)
      form.name = p.username as string
    }
  } catch (e) {
    console.error('加载偏好失败', e)
  }

  try {
    const avatarRes = await getAvatar()
    avatar.value = resolveUrl(avatarRes.data?.avatar || '')
    if (avatar.value) userStore.setAvatar(avatar.value)
  } catch (e) {
    console.error('加载头像失败', e)
  }

  loaded.value = true
}

// 头像处理
function openPicker() { uploadRef.value?.click() }
function handleDragOver(e: DragEvent) { e.preventDefault(); dragOver.value = true }
function handleDragLeave() { dragOver.value = false }
function handleDrop(e: DragEvent) { e.preventDefault(); dragOver.value = false; const f = e.dataTransfer?.files[0]; if (f) pickFile(f) }
function handleChange(e: Event) { const f = (e.target as HTMLInputElement).files?.[0]; if (f) pickFile(f) }
function pickFile(file: File) {
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片'); return }
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片不能超过 5MB'); return }
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { uploadPreview.value = ev.target?.result as string }
  reader.readAsDataURL(file)
}
async function confirmAvatar() {
  if (!selectedFile.value) { ElMessage.warning('请先选择图片'); return }
  isUploading.value = true
  try {
    const res = await uploadAvatar(selectedFile.value)
    const url = res.data?.avatar
    avatar.value = resolveUrl(url || uploadPreview.value || '')
    userStore.setAvatar(avatar.value)
    uploadPreview.value = null; selectedFile.value = null
    ElMessage.success('头像已更新')
  } catch (e) { console.error(e); ElMessage.error('头像上传失败') }
  finally { isUploading.value = false }
}

// 保存资料
async function save() {
  saving.value = true
  try {
    await savePreferences({ ...form, systemThemeJson: JSON.stringify(systemPalette), userId: localStorage.getItem('roamly_user_id') || 'user_001' })
    if (form.name) { await updateNickname(form.name); userStore.setNickname(form.name) }
    if (avatar.value) userStore.setAvatar(avatar.value)
    await userStore.fetchProfile()
    ElMessage.success('资料已保存')
  } catch (e) { console.error(e); ElMessage.error('保存失败') }
  finally { saving.value = false }
}

// 打开绑定表单
function openBindForm() {
  bindingFormOpen.value = true
  bindingEmail.value = form.email || ''
  bindingCode.value = ''
}

// 关闭绑定表单
function closeBindForm() {
  bindingFormOpen.value = false
  bindingEmail.value = ''
  bindingCode.value = ''
  if (bindingTimer) clearInterval(bindingTimer)
  bindingCountdown.value = 0
}

// 绑定邮箱 - 发送验证码
async function handleBindingSendCode() {
  if (!bindingEmail.value || !EMAIL_REGEX.test(bindingEmail.value)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  if (bindingCountdown.value > 0) return

  bindingSending.value = true
  try {
    await sendEmailCode(bindingEmail.value.trim())
    ElMessage.success('验证码已发送，请查收邮件')
    bindingCountdown.value = 60
    bindingTimer = setInterval(() => {
      bindingCountdown.value--
      if (bindingCountdown.value <= 0) {
        if (bindingTimer) clearInterval(bindingTimer)
        bindingCountdown.value = 0
      }
    }, 1000)
  } catch (err: any) {
    const msg = err?.response?.data?.message || '验证码发送失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    bindingSending.value = false
  }
}

// 绑定邮箱 - 提交
async function handleBindEmail() {
  if (!bindingEmail.value || !EMAIL_REGEX.test(bindingEmail.value)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  if (!bindingCode.value || bindingCode.value.length !== 6) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }

  bindingLoading.value = true
  try {
    await apiBindEmail({ email: bindingEmail.value.trim(), code: bindingCode.value.trim() })
    form.email = bindingEmail.value.trim()
    ElMessage.success('邮箱绑定成功')
    closeBindForm()
  } catch (err: any) {
    const msg = err?.response?.data?.message
      || err?.message
      || '绑定失败，请检查邮箱和验证码是否正确'
    ElMessage.error(msg)
  } finally {
    bindingLoading.value = false
  }
}

// 解绑邮箱
async function handleUnbindEmail() {
  if (!form.email) return

  try {
    await ElMessageBox.confirm(
      `确定要解绑邮箱 ${form.email} 吗？解绑后将无法使用邮箱验证码登录。`,
      '解绑邮箱',
      { confirmButtonText: '解绑', cancelButtonText: '取消', type: 'warning' }
    )
    unbindingLoading.value = true
    try {
      await apiUnbindEmail()
      form.email = ''
      ElMessage.success('邮箱已解绑')
    } catch (err: any) {
      const msg = err?.response?.data?.message || '解绑失败，请稍后重试'
      ElMessage.error(msg)
    } finally {
      unbindingLoading.value = false
    }
  } catch { /* 用户取消 */ }
}

// 退出登录
async function handleLogout() {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出登录',
      { confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning' }
    )
    try { await apiLogout() } catch { /* 忽略 */ }
    localStorage.removeItem('roamly_token')
    localStorage.removeItem('roamly_user_id')
    localStorage.removeItem('roamly_username')
    userStore.setAvatar('')
    userStore.setNickname('旅人')
    ElMessage.success('已退出登录')
    router.replace('/auth')
  } catch { /* 用户取消 */ }
}

onMounted(() => { loadSystemPalette(); load() })
</script>

<template>
  <main class="shell">
    <section class="profile-head">
      <p class="eyebrow">YOUR PROFILE</p>
      <h1>个人主页</h1>
      <p class="sub">管理头像、个人信息与旅行偏好，让 Roamly 更懂你。</p>
    </section>

    <div class="layout" v-loading="!loaded">
      <!-- 左：资料卡片 -->
      <aside class="profile-card">
        <div class="card-hero">
          <div class="avatar-ring">
            <div
              class="avatar-uploader" :class="{ 'drag-over': dragOver }"
              @dragover="handleDragOver" @dragleave="handleDragLeave" @drop="handleDrop"
              @click="openPicker"
            >
              <img v-if="avatar" :src="avatar" alt="头像" />
              <span v-else class="ph">{{ (form.name || '旅').slice(0, 1) }}</span>
              <div class="mask"><span>更换头像</span></div>
              <input ref="uploadRef" type="file" accept="image/*" style="display:none" @change="handleChange" />
            </div>
          </div>

          <div v-if="uploadPreview" class="preview-box">
            <img :src="uploadPreview" alt="预览" />
            <div class="row-btn">
              <el-button size="small" @click="uploadPreview = null; selectedFile = null">取消</el-button>
              <el-button size="small" type="primary" :loading="isUploading" @click="confirmAvatar">确认</el-button>
            </div>
          </div>

          <h2>{{ form.name || '旅人' }}</h2>
          <p class="email">
            <span v-if="form.email">{{ form.email }}</span>
            <span v-else class="email-unbound">未绑定邮箱</span>
          </p>

          <div class="stat-row">
            <div class="stat">
              <b>{{ form.defaultDays || 0 }}</b>
              <span>默认天数</span>
            </div>
            <div class="stat">
              <b>¥{{ (form.defaultBudget || 0).toLocaleString() }}</b>
              <span>默认预算</span>
            </div>
            <div class="stat">
              <b>{{ form.defaultTravelers || 0 }}</b>
              <span>同行人数</span>
            </div>
          </div>
        </div>

        <div class="card-form">
          <h5 class="sec-label">基本信息</h5>
          <el-form label-position="top" class="basic">
            <el-form-item label="昵称"><el-input v-model="form.name" placeholder="你的昵称" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="选填" /></el-form-item>
          </el-form>
        </div>

        <!-- 账号安全区域 -->
        <div class="card-security">
          <h5 class="sec-label">账号安全</h5>

          <!-- 邮箱绑定状态 -->
          <div class="bind-status">
            <div class="bind-info">
              <span class="bind-label">邮箱</span>
              <span v-if="form.email" class="bind-value bound">{{ form.email }}</span>
              <span v-else class="bind-value unbound">未绑定</span>
            </div>
          </div>

          <!-- 操作按钮行 -->
          <div class="bind-actions-row">
            <button
              v-if="!form.email"
              class="action-btn primary"
              @click="openBindForm"
              type="button"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              绑定邮箱
            </button>
            <template v-else>
              <button class="action-btn" @click="openBindForm" type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                更换邮箱
              </button>
              <button
                class="action-btn danger"
                @click="handleUnbindEmail"
                :disabled="unbindingLoading"
                type="button"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
                {{ unbindingLoading ? '解绑中' : '解绑' }}
              </button>
            </template>
          </div>

          <!-- 绑定邮箱表单 -->
          <div v-if="bindingFormOpen" class="bind-form">
            <div class="bind-form-header">
              <span class="bind-form-title">{{ form.email ? '更换邮箱' : '绑定新邮箱' }}</span>
              <button class="bind-form-close" @click="closeBindForm" type="button">✕</button>
            </div>
            <input
              v-model="bindingEmail"
              type="email"
              placeholder="输入邮箱地址"
              class="form-input-sm"
            />
            <div class="bind-code-row">
              <input
                v-model="bindingCode"
                type="text"
                maxlength="6"
                placeholder="6 位验证码"
                class="form-input-sm code"
              />
              <button
                class="code-btn-sm"
                :disabled="bindingCountdown > 0 || bindingSending"
                @click="handleBindingSendCode"
                type="button"
              >
                {{ bindingSending ? '发送中' : bindingCountdown > 0 ? `${bindingCountdown}s` : '获取验证码' }}
              </button>
            </div>
            <button
              class="bind-submit"
              :disabled="bindingLoading"
              @click="handleBindEmail"
              type="button"
            >
              {{ bindingLoading ? '提交中…' : '确认' }}
            </button>
            <p class="bind-hint">
              绑定后可用邮箱验证码快速登录
            </p>
          </div>

          <!-- 退出登录 -->
          <button class="logout-btn" @click="handleLogout" type="button">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </button>
        </div>
      </aside>

      <!-- 右：旅行偏好 -->
      <section class="panel prefs">
        <section class="palette-panel">
          <button class="palette-trigger" type="button" @click="paletteOpen ? closePalette() : paletteOpen = true">🎨 系统调色板 <span>{{ paletteOpen ? '收起' : '编辑' }}</span></button>
          <div v-if="paletteOpen" class="palette-popover">
            <div class="palette-popover-head"><div><h3>系统调色板</h3><p class="palette-hint">调整会即时预览，保存后写入数据库。</p></div><button type="button" class="palette-close" @click="closePalette">×</button></div>
            <div class="palette-fields">
              <label>前景色<div class="palette-input"><input type="color" v-model="systemPalette.fg" @input="applySystemPalette" /><input v-model="systemPalette.fg" maxlength="7" @input="applySystemPalette" /></div></label>
              <label>背景色<div class="palette-input"><input type="color" v-model="systemPalette.bg" @input="applySystemPalette" /><input v-model="systemPalette.bg" maxlength="7" @input="applySystemPalette" /></div></label>
              <label>强调色<div class="palette-input"><input type="color" v-model="systemPalette.accent" @input="applySystemPalette" /><input v-model="systemPalette.accent" maxlength="7" @input="applySystemPalette" /></div></label>
            </div>
            <div class="palette-presets"><button v-for="p in palettePresets" :key="p.name" type="button" :style="{ background: p.bg, color: p.fg, borderColor: p.accent }" @click="Object.assign(systemPalette, p); applySystemPalette()">{{ p.name }}</button></div>
            <button class="palette-save" type="button" :disabled="saving" @click="saveSystemPalette">{{ saving ? '保存中…' : '保存主题' }}</button>
          </div>
        </section>
        <div class="prefs-head">
          <h3>旅行偏好</h3>
          <span class="badge">为 {{ form.name || '旅人' }} 定制</span>
        </div>
        <el-form label-position="top">
          <h5 class="sec-label">行程</h5>
          <div class="grid2">
            <el-form-item label="想去的目的地（逗号分隔）">
              <el-input v-model="form.favoriteDestinations" placeholder="如：冰岛,日本,新疆" />
            </el-form-item>
            <el-form-item label="常驻出发城市"><el-input v-model="form.defaultDepartureCity" placeholder="如：上海" /></el-form-item>
          </div>
          <div class="grid3">
            <el-form-item label="默认天数"><el-input-number v-model="form.defaultDays" :min="1" :max="30" /></el-form-item>
            <el-form-item label="默认预算"><el-input-number v-model="form.defaultBudget" :min="0" :step="1000" /></el-form-item>
            <el-form-item label="同行人数"><el-input-number v-model="form.defaultTravelers" :min="1" /></el-form-item>
          </div>
          <div class="grid3">
            <el-form-item label="偏好季节">
              <el-select v-model="form.preferredSeason">
                <el-option v-for="s in ['不限','春季','夏季','秋季','冬季']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="出行类型">
              <el-select v-model="form.preferredTripType">
                <el-option v-for="s in ['深度体验','休闲度假','户外探险','亲子游','文化之旅','美食之旅']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="预算档位">
              <el-select v-model="form.budgetLevel">
                <el-option v-for="s in ['经济','舒适','豪华','奢享']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </div>
          <div class="grid2">
            <el-form-item label="旅行风格">
              <el-select v-model="form.travelStyle" placeholder="选择旅行风格">
                <el-option v-for="s in styleOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="兴趣标签（可多选）">
              <el-select v-model="interests" multiple collapse-tags placeholder="选择兴趣">
                <el-option v-for="s in interestOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="偏好的景点类型（可多选）">
            <el-select v-model="attractionTypes" multiple collapse-tags placeholder="选择景点类型">
              <el-option v-for="s in attractionOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.preferFreeAttractions">优先免费景点</el-checkbox>
          </el-form-item>

          <h5 class="sec-label">餐饮</h5>
          <div class="grid3">
            <el-form-item label="饮食要求（可多选）">
              <el-select v-model="dietaryRequirements" multiple collapse-tags placeholder="选择饮食要求">
                <el-option v-for="s in dietOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="偏好的菜系（可多选）">
              <el-select v-model="preferredCuisines" multiple collapse-tags placeholder="选择菜系">
                <el-option v-for="s in cuisineOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="辣度">
              <el-select v-model="form.spicyLevel">
                <el-option v-for="n in [0,1,2,3]" :key="n" :label="['不辣','微辣','中辣','重辣'][n]" :value="n" />
              </el-select>
            </el-form-item>
          </div>

          <h5 class="sec-label">住宿与交通</h5>
          <div class="grid3">
            <el-form-item label="住宿类型（可多选）">
              <el-select v-model="accommodationType" multiple collapse-tags placeholder="选择住宿类型">
                <el-option v-for="s in accommodationOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="最低星级"><el-input-number v-model="form.hotelStarMin" :min="1" :max="5" /></el-form-item>
            <el-form-item label="交通偏好（可多选）">
              <el-select v-model="transportationPreference" multiple collapse-tags placeholder="选择交通方式">
                <el-option v-for="s in transportOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </div>

          <h5 class="sec-label">通知</h5>
          <div class="grid3">
            <el-form-item label="出行前提醒（天）"><el-input-number v-model="form.notifyBeforeTripDays" :min="0" :max="30" /></el-form-item>
            <el-form-item><el-checkbox v-model="form.notifyWeatherAlert">天气预警提醒</el-checkbox></el-form-item>
            <el-form-item><el-checkbox v-model="form.notifyPriceChange">价格变动提醒</el-checkbox></el-form-item>
          </div>
        </el-form>
      </section>
    </div>

    <div class="savebar">
      <button class="save-btn" :disabled="saving" @click="save">
        {{ saving ? '保存中…' : '保存资料' }}
      </button>
    </div>
  </main>
</template>

<style scoped lang="scss">
.shell { min-height: 100vh; padding-bottom: 90px; }
.profile-head { width: min(1160px, calc(100% - 40px)); margin: 24px auto 26px; }
.palette-panel { position: relative; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--line); }
.palette-trigger { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 12px 14px; border: 1px solid var(--line); border-radius: 10px; background: var(--card); color: var(--ink); font-weight: 800; cursor: pointer; }
.palette-trigger span { color: var(--ink-2); font-size: 12px; font-weight: 600; }
.palette-popover { margin-top: 10px; padding: 16px; border: 1px solid var(--line); border-radius: 12px; background: var(--card); box-shadow: var(--shadow-soft); }
.palette-popover-head { display: flex; align-items: flex-start; justify-content: space-between; }
.palette-popover-head h3 { margin: 0; font-size: 16px; }
.palette-close { border: 0; background: transparent; color: var(--ink-2); font-size: 22px; line-height: 1; cursor: pointer; }
.palette-hint { margin-top: 5px; color: var(--ink-2); font-size: 12px; }
.palette-fields { display: flex; gap: 14px; flex-wrap: wrap; margin: 16px 0 12px; }
.palette-fields label { display: grid; gap: 7px; color: var(--ink-2); font-size: 12px; font-weight: 700; }
.palette-input { display: flex; align-items: center; gap: 7px; }
.palette-input input[type='color'] { width: 34px; height: 28px; padding: 0; border: 1px solid var(--line); border-radius: 6px; cursor: pointer; }
.palette-input input[type='text'], .palette-input input:not([type]) { width: 92px; height: 28px; padding: 0 7px; border: 1px solid var(--line); border-radius: 6px; background: var(--card); color: var(--ink); font: 12px ui-monospace, monospace; }
.palette-presets { display: flex; gap: 8px; flex-wrap: wrap; }
.palette-presets button { padding: 7px 12px; border: 1px solid; border-radius: 8px; font-size: 12px; font-weight: 700; cursor: pointer; }
.palette-save { width: 100%; margin-top: 14px; padding: 10px; border: 0; border-radius: 8px; background: var(--forest); color: #fff; font-weight: 800; cursor: pointer; }
.palette-save:disabled { opacity: .6; cursor: wait; }
.eyebrow { color: var(--sunset); font-size: 10px; font-weight: 800; letter-spacing: 0.18em; margin: 0 0 8px; }
.profile-head h1 { font: 34px "DM Serif Display", "Noto Sans SC"; color: var(--ink); margin: 0; }
.sub { color: #687873; font-size: 14px; margin: 10px 0 0; }

.layout { width: min(1160px, calc(100% - 40px)); margin: auto; display: grid; grid-template-columns: 320px 1fr; gap: 24px; align-items: start; }

.profile-card { background: var(--card); border: 1px solid var(--line); border-radius: 24px; overflow: hidden; box-shadow: 0 2px 4px rgba(22,78,66,.05); }
.card-hero { padding: 30px 26px 22px; text-align: center; background: radial-gradient(160% 120% at 50% 10%, var(--roam-soft) 0%, var(--card) 55%); }
.avatar-ring { width: 130px; height: 130px; margin: 0 auto 14px; border-radius: 50%; padding: 5px; background: var(--forest); box-shadow: 0 8px 28px rgba(22,78,66,.22), 0 0 0 10px rgba(79,143,120,.12); }
.avatar-uploader { position: relative; width: 100%; height: 100%; border-radius: 50%; overflow: hidden; background: #fff; cursor: pointer; transition: .3s; }
.avatar-uploader img { width: 100%; height: 100%; object-fit: cover; }
.avatar-uploader .ph { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; background: var(--ink); color: #fff; font-weight: 800; font-size: 42px; }
.mask { position: absolute; inset: auto 0 0 0; background: rgba(0,0,0,.5); color: #fff; font-size: 12px; padding: 8px 0; opacity: 0; transition: .2s; text-align: center; }
.avatar-uploader:hover .mask, .avatar-uploader.drag-over .mask { opacity: 1; }
.card-hero h2 { color: var(--ink); margin: 4px 0 2px; font-size: 22px; }
.card-hero .email { color: #8a9792; font-size: 13px; margin: 0 0 18px; }
.email-unbound { color: var(--sunset); font-weight: 600; }
.preview-box { text-align: center; margin: -6px 0 12px; }
.preview-box img { width: 84px; height: 84px; border-radius: 50%; object-fit: cover; border: 2px solid var(--roam); }
.row-btn { margin-top: 8px; display: flex; justify-content: center; gap: 8px; }

.stat-row { display: flex; border-top: 1px solid var(--line); padding-top: 18px; }
.stat { flex: 1; text-align: center; }
.stat + .stat { border-left: 1px solid var(--line); }
.stat b { display: block; color: var(--ink); font-size: 18px; font-weight: 800; }
.stat span { color: #8a9792; font-size: 11px; margin-top: 2px; display: block; }

.card-form { padding: 20px 24px 24px; }
.card-security { padding: 0 24px 20px; border-top: 1px solid var(--line); }
.sec-label { color: var(--sunset); font-size: 11px; font-weight: 800; letter-spacing: 0.12em; margin: 0 0 10px; text-transform: uppercase; }
.basic { text-align: left; }

/* 绑定状态 */
.bind-status { display: flex; align-items: center; justify-content: space-between; padding: 10px 0; }
.bind-info { display: flex; flex-direction: column; }
.bind-label { font-size: 11px; color: var(--ink-3); font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.bind-value { font-size: 14px; font-weight: 600; margin-top: 2px; }
.bind-value.bound { color: var(--ink); }
.bind-value.unbound { color: var(--sunset); }

/* 操作按钮行 */
.bind-actions-row { display: flex; gap: 8px; margin-bottom: 12px; }

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 13px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}
.action-btn:hover {
  border-color: var(--forest);
  color: var(--forest);
  background: var(--roam-soft);
}
.action-btn.primary {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.action-btn.primary:hover {
  background: var(--forest-deep);
  border-color: var(--forest-deep);
  color: #fff;
}
.action-btn.danger {
  border-color: var(--line);
  color: var(--ink-3);
}
.action-btn.danger:hover {
  border-color: #e74c3c;
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.05);
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 绑定表单 */
.bind-form { padding: 14px; background: var(--wash); border-radius: 12px; position: relative; }
.bind-form-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.bind-form-title { font-size: 13px; font-weight: 700; color: var(--ink); }
.bind-form-close { background: none; border: none; color: var(--ink-3); font-size: 14px; cursor: pointer; padding: 4px 8px; border-radius: 6px; }
.bind-form-close:hover { background: var(--line); color: var(--ink); }
.form-input-sm { display: block; width: 100%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 8px; background: #fff; font-size: 13px; box-sizing: border-box; margin-bottom: 8px; }
.form-input-sm:focus { outline: none; border-color: var(--forest); }
.bind-code-row { display: flex; gap: 8px; margin-bottom: 8px; }
.form-input-sm.code { flex: 1; }
.code-btn-sm { padding: 10px 12px; border: 1px solid var(--forest); border-radius: 8px; background: transparent; color: var(--forest); font-size: 12px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.code-btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.bind-submit { width: 100%; padding: 10px; border: none; border-radius: 8px; background: var(--forest); color: #fff; font-weight: 700; font-size: 13px; cursor: pointer; }
.bind-submit:disabled { opacity: 0.6; cursor: not-allowed; }
.bind-hint { font-size: 11px; color: var(--ink-3); margin-top: 8px; text-align: center; }

/* 退出按钮 */
.logout-btn { width: 100%; margin-top: 16px; padding: 10px; border: 1px solid var(--line); border-radius: 10px; background: transparent; color: var(--ink-2); font-size: 13px; font-weight: 500; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; transition: all 0.15s; }
.logout-btn:hover { background: var(--wash); border-color: var(--ink-3); color: var(--ink); }

.panel { background: var(--card); border: 1px solid var(--line); border-radius: 24px; padding: 26px 28px; box-shadow: 0 2px 4px rgba(22,78,66,.05); }
.prefs-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.prefs-head h3 { color: var(--ink); margin: 0; font-size: 20px; }
.prefs .badge { background: var(--roam-soft); color: var(--forest); font-size: 11px; font-weight: 700; padding: 4px 12px; border-radius: 24px; }
.prefs .sec-label { margin: 20px 0 4px; }
.prefs .sec-label:first-of-type { margin-top: 6px; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.grid3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }

.savebar { width: min(1160px, calc(100% - 40px)); margin: 26px auto 0; display: flex; justify-content: flex-end; }
.save-btn { border: 0; background: var(--forest); color: #fff; font-weight: 800; padding: 14px 34px; border-radius: 13px; cursor: pointer; box-shadow: 0 10px 20px rgba(22,78,66,.18); }
.save-btn:disabled { opacity: .65; cursor: not-allowed; }

@media (max-width: 800px) {
  .layout { grid-template-columns: 1fr; }
  .grid2, .grid3 { grid-template-columns: 1fr; }
}
</style>
