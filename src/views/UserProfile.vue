<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getPreferences, savePreferences, uploadAvatar, getAvatar, updateNickname, type UserPreference } from '@/api/user'
const userStore = useUserStore()
const saving = ref(false)
const loaded = ref(false)

// 头像
const avatar = ref<string>('')
const uploadPreview = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const isUploading = ref(false)
const uploadRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)

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

// 多选字段选项
const interestOptions = ['美食','人文','自然','摄影','购物','夜生活','历史','艺术','冒险','户外','亲子','养生','音乐','建筑']
const styleOptions = ['轻松漫游','深度人文','美食优先','亲子友好','户外探险','奢华度假','都市探索','自然风光']
const attractionOptions = ['自然','人文','历史遗迹','博物馆','主题公园','古镇','海滩','雪山','动物园','水族馆']
const dietOptions = ['无特殊要求','清真','素食','纯素','无麸质','忌辣','忌海鲜','儿童餐']
const cuisineOptions = ['川菜','粤菜','湘菜','江浙菜','本帮菜','日料','韩餐','东南亚菜','西餐','火锅','烧烤','家常菜']
const accommodationOptions = ['酒店','民宿','青旅','度假村','精品酒店','公寓']
const transportOptions = ['高铁','自驾','飞机','地铁','公交','骑行','徒步','包车']

// JSON / 逗号串 <-> 数组 相互转换
function toList(v: unknown): string[] {
  if (!v) return []
  const s = String(v).trim()
  if (!s) return []
  if (s.startsWith('[')) { try { return JSON.parse(s) } catch { return [] } }
  return s.split(/[,，]/).map(x => x.trim()).filter(Boolean)
}
function joinList(v: string[]): string { return (v || []).join(',') }

const interests = computed({ get: () => toList(form.interests), set: (v) => { form.interests = joinList(v) } })
const attractionTypes = computed({ get: () => toList(form.attractionTypes), set: (v) => { form.attractionTypes = joinList(v) } })
const dietaryRequirements = computed({ get: () => toList(form.dietaryRequirements), set: (v) => { form.dietaryRequirements = joinList(v) } })
const preferredCuisines = computed({ get: () => toList(form.preferredCuisines), set: (v) => { form.preferredCuisines = joinList(v) } })
const accommodationType = computed({ get: () => toList(form.accommodationType), set: (v) => { form.accommodationType = joinList(v) } })
const transportationPreference = computed({ get: () => toList(form.transportationPreference), set: (v) => { form.transportationPreference = joinList(v) } })

function resolveUrl(url: string) {
  if (!url) return ''
  return url.startsWith('http') ? url : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
}

async function load() {
  // 分别请求，单个失败不影响另一个
  try {
    const prefRes = await getPreferences()
    const p = prefRes.data || {}
    Object.assign(form, p)
    form.favoriteDestinations = p.favoriteDestinations || ''
    // 同步到全局 store
    if (form.name) userStore.setNickname(form.name)
    console.log('[Profile] 昵称已加载:', form.name)
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

function openPicker() { uploadRef.value?.click() }
function handleDragOver(e: DragEvent) { e.preventDefault(); dragOver.value = true }
function handleDragLeave() { dragOver.value = false }
function handleDrop(e: DragEvent) {
  e.preventDefault(); dragOver.value = false
  const f = e.dataTransfer?.files[0]; if (f) pickFile(f)
}
function handleChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]; if (f) pickFile(f)
}
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
  } catch (e) {
    console.error(e); ElMessage.error('头像上传失败')
  } finally {
    isUploading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await savePreferences({ ...form, userId: 'user_001' })
    // 同步昵称到 user_travel_preference（侧边栏持久化）
    if (form.name) {
      await updateNickname(form.name)
      userStore.setNickname(form.name)
    }
    if (avatar.value) userStore.setAvatar(avatar.value)
    // 保存后强制刷新 store，确保侧边栏同步
    await userStore.fetchProfile()
    ElMessage.success('资料已保存')
  } catch (e) {
    console.error(e); ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
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
          <p class="email">{{ form.email || '未填写邮箱' }}</p>

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
            <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="you@example.com" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="选填" /></el-form-item>
          </el-form>
        </div>
      </aside>

      <!-- 右：旅行偏好 -->
      <section class="panel prefs">
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
.eyebrow { color: var(--sunset); font-size: 10px; font-weight: 800; letter-spacing: 0.18em; margin: 0 0 8px; }
.profile-head h1 { font: 34px "DM Serif Display", "Noto Sans SC"; color: var(--ink); margin: 0; }
.sub { color: #687873; font-size: 14px; margin: 10px 0 0; }

.layout { width: min(1160px, calc(100% - 40px)); margin: auto; display: grid; grid-template-columns: 320px 1fr; gap: 24px; align-items: start; }

/* ── 左侧资料卡片 ── */
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
.preview-box { text-align: center; margin: -6px 0 12px; }
.preview-box img { width: 84px; height: 84px; border-radius: 50%; object-fit: cover; border: 2px solid var(--roam); }
.row-btn { margin-top: 8px; display: flex; justify-content: center; gap: 8px; }

.stat-row { display: flex; border-top: 1px solid var(--line); padding-top: 18px; }
.stat { flex: 1; text-align: center; }
.stat + .stat { border-left: 1px solid var(--line); }
.stat b { display: block; color: var(--ink); font-size: 18px; font-weight: 800; }
.stat span { color: #8a9792; font-size: 11px; margin-top: 2px; display: block; }

.card-form { padding: 20px 24px 24px; }
.sec-label { color: var(--sunset); font-size: 11px; font-weight: 800; letter-spacing: 0.12em; margin: 0 0 10px; text-transform: uppercase; }
.basic { text-align: left; }

/* ── 右侧偏好 ── */
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
