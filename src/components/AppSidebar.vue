<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import roamlySymbol from '@/assets/brand/logo-app-icon.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

interface NavItem {
  path: string
  label: string
  icon: string
}

// 线性图标（内联 SVG，描边跟随 currentColor）
const icons: Record<string, string> = {
  compass: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="3 11 22 2 13 21 11 13 3 11"/></svg>`,
  star: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  map: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="1 6 8 3 16 6 23 3 23 18 16 21 8 18 1 21 1 6"/><line x1="8" y1="3" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="21"/></svg>`,
  flag: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>`,
  chat: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
  note: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>`
}

const navMain: NavItem[] = [
  { path: '/explore', label: '发现灵感', icon: 'compass' },
  { path: '/', label: 'AI 旅行规划', icon: 'chat' },
  { path: '/inspirations', label: '灵感目的地', icon: 'star' },
  { path: '/notes', label: '旅行笔记', icon: 'note' },
  { path: '/journeys', label: '我的旅程', icon: 'map' }
]

const navMore: NavItem[] = [
  { path: '/users/search', label: '寻找同好', icon: 'star' },
  { path: '/journey-map', label: '足迹地图', icon: 'flag' },
  { path: '/agent-panel', label: '互动式规划', icon: 'chat' }
]

function isActive(item: NavItem) {
  if (item.path === '/') return route.path === '/'
  return route.path.startsWith(item.path)
}

const avatarStyle = computed(() => {
  if (userStore.avatar) {
    return { backgroundImage: `url(${userStore.avatar})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
})

const avatarFallback = computed(() => (userStore.nickname || '旅人').charAt(0))
</script>

<template>
  <aside class="sidebar">
    <a class="brand" @click="router.push('/')">
      <img class="brand-mark" :src="roamlySymbol" alt="Roamly" />
      <span class="brand-name">Roamly</span>
    </a>

    <p class="group-label">菜单</p>
    <nav class="menu">
      <a
        v-for="item in navMain"
        :key="item.path"
        class="menu-item"
        :class="{ active: isActive(item) }"
        @click="router.push(item.path)"
      >
        <span class="ico" v-html="icons[item.icon]"></span>
        <span class="label">{{ item.label }}</span>
      </a>
    </nav>

    <p class="group-label">更多</p>
    <nav class="menu">
      <a
        v-for="item in navMore"
        :key="item.path"
        class="menu-item"
        :class="{ active: isActive(item) }"
        @click="router.push(item.path)"
      >
        <span class="ico" v-html="icons[item.icon]"></span>
        <span class="label">{{ item.label }}</span>
      </a>
    </nav>

    <div class="side-foot">
      <!-- 用户卡片 -->
      <a class="user" @click="router.push('/profile')">
        <span class="user-avatar" :style="avatarStyle">
          <span v-if="!userStore.avatar" class="fallback">{{ avatarFallback }}</span>
        </span>
        <span class="user-meta">
          <b>{{ userStore.nickname || '旅人' }}</b>
          <small>个人主页</small>
        </span>
        <svg class="goto" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </a>
    </div>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  width: 248px;
  flex-shrink: 0;
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  background: var(--forest);
  padding: 24px 16px;
  overflow-y: auto;
  overflow-x: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 10px 22px;
  cursor: pointer;
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: block;
  flex-shrink: 0;
}

.brand-name {
  font: 25px "DM Serif Display", serif;
  letter-spacing: -0.3px;
  color: #FFFDF8;
}

.group-label {
  color: #7FA294;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  padding: 0 13px;
  margin: 6px 0 8px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 11px 13px;
  border-radius: 13px;
  color: #A9C4B9;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.07);
    color: #FFFDF8;
  }

  &.active {
    background: rgba(255, 255, 255, 0.14);
    color: #ffffff;
  }
}

.ico {
  width: 19px;
  height: 19px;
  display: grid;
  place-items: center;
  flex-shrink: 0;

  :deep(svg) { width: 19px; height: 19px; }
}

.side-foot {
  margin-top: auto;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: rgba(255, 255, 255, 0.12); }
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #FFFDF8;
  color: var(--forest);
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 14px;
  overflow: hidden;
  flex-shrink: 0;

  .fallback { line-height: 1; }
}

.user-meta {
  flex: 1;
  min-width: 0;

  b {
    display: block;
    color: #FFFDF8;
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  small {
    display: block;
    color: #7FA294;
    font-size: 11px;
  }
}

.goto {
  width: 15px;
  height: 15px;
  color: #7FA294;
  flex-shrink: 0;
}

@media (max-width: 860px) {
  .sidebar { width: 68px; padding: 18px 10px; }
  .brand { justify-content: center; padding: 4px 0 18px; }
  .brand-name { display: none; }
  .group-label { display: none; }
  .menu-item { justify-content: center; padding: 12px 0; }
  .menu-item .label { display: none; }
  .user { justify-content: center; padding: 8px 0; }
  .user-meta, .goto { display: none; }
}
</style>
