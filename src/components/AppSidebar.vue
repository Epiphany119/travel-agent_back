<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useRightPanelStore } from '@/stores/rightPanel'
import { useUserStore } from '@/stores/user'
import roamlySymbol from '@/assets/brand/logo-app-icon.png'

const router = useRouter()
const route = useRoute()
const rightPanel = useRightPanelStore()
const userStore = useUserStore()

interface NavItem {
  path: string
  label: string
  icon: string
  description: string
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
  { path: '/explore', label: '发现灵感', icon: 'compass', description: '浏览全球旅行灵感，发现新目的地与热门路线。' },
  { path: '/', label: 'AI 旅行规划', icon: 'chat', description: '与 AI 助手对话，智能规划个性化旅行行程。' },
  { path: '/inspirations', label: '灵感目的地', icon: 'star', description: '收藏与管理你心仪的旅行目的地卡片。' },
  { path: '/notes', label: '旅行笔记', icon: 'note', description: '类飞书的 Markdown 在线笔记编辑器，支持导入、预览与主题定制。' },
  { path: '/journeys', label: '我的旅程', icon: 'map', description: '查看和管理你创建的旅行行程记录。' }
]

const navMore: NavItem[] = [
  { path: '/users/search', label: '寻找同好', icon: 'star', description: '搜索并发现与你品味相投的旅行同好。' },
  { path: '/journey-map', label: '足迹地图', icon: 'flag', description: '在地图上查看你去过的地方，留下足迹标记。' },
  { path: '/agent-panel', label: '互动式规划', icon: 'chat', description: '使用互动式智能体深度规划你的旅程。' }
]

function isActive(item: NavItem) {
  if (item.path === '/') return route.path === '/chat'
  return route.path.startsWith(item.path)
}

/**
 * 页面优先级：
 *  高优先级（中间工作区）：旅行笔记 /notes、个人信息 /profile、卡片预览 /card-detail
 *  低优先级（三栏时进右侧辅助面板）：发现灵感、灵感目的地、我的旅程、足迹地图、AI 规划等
 * 规则：
 *  - 右侧栏收起时：所有入口都在中间工作区自由切换路由
 *  - 右侧栏打开时：高优先级留在中间切路由；低优先级在右侧辅助面板显示，
 *    绝不让中间和右侧显示同一份内容
 */
const primaryNav = new Set(['/notes', '/profile'])

/** 侧边栏入口 -> 右侧辅助面板视图 key 映射 */
const viewKeyMap: Record<string, string> = {
  '/explore': 'explore',
  '/chat': 'chat',
  '/inspirations': 'inspirations',
  '/journeys': 'journeys',
  '/users/search': 'search',
  '/journey-map': 'map',
  '/agent-panel': 'agent'
}

function navPath(item: NavItem) {
  return item.path === '/' ? '/chat' : item.path
}

function navigate(item: NavItem) {
  const path = navPath(item)
  const viewKey = viewKeyMap[path]

  // 特殊：互动式规划 — 三栏时中间进路由（显示结果）+ 右栏开对话
  if (path === '/agent-panel') {
    if (rightPanel.show && viewKey) {
      rightPanel.openView(viewKey, item.label)
    }
    router.push(path)
    return
  }

  if (rightPanel.show && !primaryNav.has(item.path) && viewKey) {
    // 三栏模式 + 低优先级 -> 右侧辅助面板（中间保持笔记/预览）
    rightPanel.openView(viewKey, item.label)
    return
  }
  // 高优先级或右栏收起 -> 中间工作区切路由
  router.push(path)
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
        :title="item.label"
        @click="navigate(item)"
      >
        <span class="ico" :aria-label="item.label" v-html="icons[item.icon]"></span>
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
        :title="item.label"
        @click="navigate(item)"
      >
        <span class="ico" :aria-label="item.label" v-html="icons[item.icon]"></span>
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
  width: 56px;
  flex-shrink: 0;
  height: calc(100% - 16px);    /* 让父 flex 控制高度，减去顶部/底部 8px 外边距 */
  margin: 8px 8px 8px 0;        /* 只留右边距，左边由 .app-main-row padding 8px 顶开 */
  border-radius: 14px;
  position: sticky;
  z-index: 10000;
  top: 0;
  display: flex;
  flex-direction: column;
  background: var(--forest);
  padding: 14px 8px;
  overflow: visible;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 2px 0 22px;
  cursor: pointer;
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: block;
  flex-shrink: 0;
}

.brand-name {
  display: none;
}

.group-label {
  display: none;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
}

.menu-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 10px;
  color: #A9C4B9;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;

  &::before {
    content: '';
    position: absolute;
    left: -8px;
    width: 3px;
    height: 18px;
    border-radius: 0 3px 3px 0;
    background: var(--sunset, #F27A4F);
    opacity: 0;
    transition: opacity .2s;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.07);
    color: #FFFDF8;
  }

  &.active {
    background: rgba(255, 255, 255, 0.14);
    color: #ffffff;

    &::before { opacity: 1; }
  }
}

.menu-item .label { display: none; }
.ico { 
  position: relative;
  z-index: 2;
}

/* 全局 tooltip 样式 - 使用 fixed 定位，确保不被任何容器裁剪 */
.ico::after {
  content: attr(aria-label);
  position: absolute;
  left: calc(100% + 10px);
  top: 50%;
  transform: translateY(-50%);
  white-space: nowrap;
  z-index: 99999;
  padding: 6px 10px;
  border-radius: 6px;
  background: #1f2329;
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0,0,0,.2);
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transition: opacity .15s ease, visibility .15s ease;
}

.menu-item:hover .ico::after,
.ico:hover::after {
  opacity: 1 !important;
  visibility: visible !important;
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
  justify-content: center;
  gap: 0;
  padding: 8px 0;
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
.user-meta, .goto { display: none; }

.goto {
  width: 15px;
  height: 15px;
  color: #7FA294;
  flex-shrink: 0;
}

@media (max-width: 860px) {
  .sidebar { width: 56px; padding: 14px 8px; }
  .brand { justify-content: center; padding: 4px 0 18px; }
  .brand-name { display: none; }
  .group-label { display: none; }
  .menu-item { justify-content: center; padding: 12px 0; }
  .menu-item .label { display: none; }
  .user { justify-content: center; padding: 8px 0; }
  .user-meta, .goto { display: none; }
}
</style>
