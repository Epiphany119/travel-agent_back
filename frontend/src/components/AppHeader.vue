<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useContentTabsStore } from '@/stores/contentTabs'
import { useRightPanelStore } from '@/stores/rightPanel'
import { searchUsers } from '@/api/user'
import PanelOpenIcon from '@/assets/右边栏按钮-开.png'
import PanelCloseIcon from '@/assets/右边栏按钮-关.png'

const router = useRouter()
const keyword = ref('')
const userMatches = ref<any[]>([])
const searchLoading = ref(false)
const searchOpen = ref(false)
const searchRoot = ref<HTMLElement | null>(null)
const tabs = useContentTabsStore()
const rightPanel = useRightPanelStore()
const emit = defineEmits<{ (e: 'toggle-panel'): void }>()
let searchTimer: ReturnType<typeof setTimeout> | undefined
let searchRequestId = 0

function userId(user: any) {
  return String(user?.public_id || user?.publicId || user?.user_id || user?.userId || '').trim()
}

function userName(user: any) {
  return String(user?.nickname || user?.name || user?.username || userId(user) || '旅行者')
}

function userAvatar(value?: string) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^(https?:|data:|blob:)/i.test(raw)) return raw
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
}

function invalidateSearch() {
  searchRequestId += 1
  userMatches.value = []
  searchLoading.value = false
}

async function loadUserMatches(value: string) {
  const requestId = ++searchRequestId
  searchLoading.value = true
  try {
    const result = await searchUsers(value)
    if (requestId === searchRequestId) userMatches.value = result.data || []
  } catch {
    if (requestId === searchRequestId) userMatches.value = []
  } finally {
    if (requestId === searchRequestId) searchLoading.value = false
  }
}

watch(keyword, (value) => {
  if (searchTimer) clearTimeout(searchTimer)
  const q = value.trim()
  searchOpen.value = Boolean(q)
  if (!q) {
    invalidateSearch()
    return
  }
  searchTimer = setTimeout(() => { void loadUserMatches(q) }, 180)
})

function openUser(user: any) {
  const id = userId(user)
  if (!id) return
  searchOpen.value = false
  keyword.value = ''
  router.push(`/users/${encodeURIComponent(id)}`)
}

function search() {
  const q = keyword.value.trim()
  searchOpen.value = false
  if (q) router.push({ path: '/users/search', query: { q } })
  else router.push('/users/search')
}

function handleSearchFocus() {
  searchOpen.value = Boolean(keyword.value.trim())
}

function handleOutsideClick(event: MouseEvent) {
  if (!searchRoot.value?.contains(event.target as Node)) searchOpen.value = false
}

onMounted(() => document.addEventListener('click', handleOutsideClick))
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <header class="app-header">
    <div ref="searchRoot" class="search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        v-model="keyword"
        placeholder="搜索用户、昵称或 ID…"
        aria-label="搜索用户、昵称或 ID"
        @focus="handleSearchFocus"
        @keyup.enter="search"
      />
      <div v-if="searchOpen && keyword.trim()" class="search-popover" @mousedown.prevent>
        <div class="search-popover-head">
          <span>旅行同好</span>
          <small>输入即匹配</small>
        </div>
        <div v-if="searchLoading" class="search-state">正在匹配用户…</div>
        <button
          v-for="user in userMatches"
          :key="userId(user)"
          type="button"
          class="search-user"
          @click="openUser(user)"
        >
          <span class="search-avatar">
            <img v-if="userAvatar(user.avatar || user.avatar_url || user.avatarUrl)" :src="userAvatar(user.avatar || user.avatar_url || user.avatarUrl)" alt="" />
            <span v-else>{{ userName(user).charAt(0) }}</span>
          </span>
          <span class="search-user-copy">
            <b>{{ userName(user) }}</b>
            <small>ID · {{ userId(user) }}</small>
          </span>
          <span class="search-user-arrow">↗</span>
        </button>
        <div v-if="!searchLoading && !userMatches.length" class="search-state">没有找到匹配用户</div>
        <button type="button" class="search-more" @click="search">查看全部用户 →</button>
      </div>
    </div>

    <!-- 嵌入式标签栏：仿 Chrome 标签风格，始终占位固定宽度 -->
    <div class="inline-tabs">
      <div v-if="tabs.tabs.length" class="tabs-scroll">
        <button
          v-for="t in tabs.tabs"
          :key="t.id"
          class="tab-item"
          :class="{ active: tabs.activeId === t.id }"
          @click="tabs.activate(t.id)"
        >
          <span class="tab-label">{{ t.title }}</span>
          <span class="tab-close" title="关闭" @click.stop="tabs.close(t.id)">×</span>
        </button>
      </div>
      <button v-if="tabs.tabs.length > 1" class="tab-close-all" title="关闭全部" @click="tabs.closeAll()">✕</button>
    </div>

    <div class="actions">
      <!-- 右侧栏开关：固定在右上角同一位置，点一下开 / 再点一下关 -->
      <button
        class="icon-btn panel-toggle-btn"
        :title="rightPanel.show ? '收起右侧栏' : '打开右侧栏'"
        @click="emit('toggle-panel')"
      >
        <img :src="rightPanel.show ? PanelCloseIcon : PanelOpenIcon" alt="右侧栏开关" />
      </button>
      <button class="icon-btn" title="通知">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <i class="badge-dot"></i>
      </button>
      <button class="icon-btn" title="收藏" @click="router.push('/inspirations')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
      </button>
      <button class="new-btn" @click="router.push('/journeys')">
        <span class="plus">＋</span> 新旅程
      </button>
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 48px;
  padding: 4px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.12);
  background: linear-gradient(90deg, rgba(255,255,255,.05), transparent 38%, rgba(242,122,79,.08));
  flex-shrink: 0;
}

/* 搜索框：保留顶栏的轻量感，但给输入区明确的层级 */
.search {
  position: relative;
  flex: 1 1 260px;
  min-width: 180px;
  max-width: 420px;
  display: flex;
  align-items: center;
  gap: 9px;
  background: rgba(5, 35, 29, .34);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  padding: 7px 11px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.04);
  transition: border-color .15s, background .15s, box-shadow .15s;

  &:hover,
  &:focus-within {
    border-color: rgba(255,255,255,0.42);
    background: rgba(5, 35, 29, .48);
    box-shadow: 0 0 0 3px rgba(255,255,255,.06), inset 0 1px 0 rgba(255,255,255,.06);
  }

  svg {
    width: 15px;
    height: 15px;
    color: rgba(255,255,255,0.7);
    flex-shrink: 0;
  }

  input {
    width: 100%;
    min-width: 0;
    border: 0;
    background: transparent;
    outline: 0;
    color: #fff;
    font: 600 13px Manrope, "Noto Sans SC", sans-serif;

    &::placeholder { color: rgba(255,255,255,0.5); }
  }
}

.search-popover {
  position: absolute;
  top: calc(100% + 9px);
  left: 0;
  width: min(360px, calc(100vw - 32px));
  padding: 8px;
  border: 1px solid var(--line, rgba(255,255,255,.2));
  border-radius: 14px;
  background: var(--card, #fffdf8);
  color: var(--ink, #1d2b27);
  box-shadow: 0 16px 34px rgba(8, 44, 37, .22);
  z-index: 10020;
}

.search-popover-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 8px 8px;
  color: var(--ink-2, #66756e);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .04em;

  small { color: var(--ink-3, #9aa9a2); font-size: 10px; font-weight: 600; }
}

.search-user {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 8px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;

  &:hover { background: var(--roam-soft, #e5f0ea); }
}

.search-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--roam-soft, #e5f0ea);
  color: var(--forest, #164e42);
  font-size: 12px;
  font-weight: 900;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.search-user-copy {
  min-width: 0;
  flex: 1;

  b, small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  b { font-size: 12px; }
  small { margin-top: 2px; color: var(--ink-3, #9aa9a2); font-size: 10px; }
}

.search-user-arrow { color: var(--ink-3, #9aa9a2); font-size: 16px; }
.search-state { padding: 16px 8px; color: var(--ink-3, #9aa9a2); font-size: 12px; text-align: center; }
.search-more { width: 100%; margin-top: 4px; padding: 8px; border: 0; border-top: 1px solid var(--line, #e7e0d2); background: transparent; color: var(--forest, #164e42); font-size: 11px; font-weight: 800; text-align: left; cursor: pointer; }

/* 嵌入在 header 里的 Chrome 风格标签栏 */
.inline-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1 1 auto;       /* 填满 app-header 剩余空间 */
  min-width: 180px;
  height: 30px;
  padding: 0 6px;
  background: rgba(8, 44, 37, 0.28);
  border: 1px solid rgba(255,255,255,0.13);
  border-radius: 11px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.04);
  overflow: hidden;
}

.tabs-scroll {
  display: flex;
  align-items: stretch;
  gap: 2px;
  overflow-x: auto;
  scrollbar-width: none;
  flex: 1;
  min-width: 0;

  &::-webkit-scrollbar { display: none; }
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 150px;
  padding: 0 8px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: rgba(255,255,255,0.62);
  font-size: 12px;
  line-height: 1;
  height: 100%;
  cursor: pointer;
  white-space: nowrap;
  transition: color .15s;
  flex-shrink: 0;
  position: relative;
  border-bottom: 2px solid transparent;

  &:hover { color: #fff; background: rgba(255,255,255,.07); }

  &.active {
    background: rgba(255,255,255,.12);
    color: #fff;
    font-weight: 700;
    border-bottom-color: var(--sunset, #f27a4f);  /* 当前页指示线 */
    border-radius: 8px 8px 5px 5px;
  }
}

.tab-label {
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}

.tab-close {
  font-size: 13px;
  line-height: 1;
  padding: 0 2px;
  border-radius: 50%;
  opacity: .55;

  &:hover { opacity: 1; background: rgba(0, 0, 0, .08); }
}

.tab-close-all {
  border: 0;
  background: transparent;
  color: rgba(255,255,255,0.62);
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 4px;
  flex-shrink: 0;

  &:hover { color: #fff; background: rgba(255,255,255,.08); border-radius: 4px; }
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.new-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 0;
  background: #fff;
  color: var(--forest);
  font-weight: 800;
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
  }

  .plus { font-size: 15px; line-height: 1; }
}

.icon-btn {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,.08);
  background: rgba(255,255,255,0.12);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;

  svg { width: 15px; height: 15px; }

  img {
    width: 18px;
    height: 18px;
    display: block;
    pointer-events: none;
  }

  &:hover { background: rgba(255,255,255,0.25); }
}

/* 右侧栏开关按钮：固定右上角，强调色描边区分于普通图标按钮 */
.panel-toggle-btn {
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.35);
}

.badge-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--sunset);
  box-shadow: 0 0 0 2px var(--forest);
}

@media (max-width: 640px) {
  .app-header { padding: 6px 12px; }
  .icon-btn { display: none; }
}
</style>
