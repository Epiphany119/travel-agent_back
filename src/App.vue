<template>
  <router-view v-if="isAuthPage" />
  <div v-else class="app-layout">
    <div class="app-header-row">
      <!-- 顶栏横跨整个系统宽度：左栏 + app-main（右侧） -->
      <div class="app-header-left-spacer"></div>
      <AppHeader @toggle-panel="onPanelToggle" />
    </div>

    
    <div class="app-main-row">
      <AppSidebar />

      <!-- app-main：一张白色圆角大卡片，里面横向布局 header + workspace + right-panel -->
      <div class="app-main">
        <div class="app-body">
          <!-- 中间工作区 -->
          <div class="workspace">
            <!-- 底层路由页面保持存活（如 NotesView） -->
            <router-view v-slot="{ Component, route: r }">
              <component :is="Component" :key="r.fullPath" class="workspace-route-view" />
            </router-view>

          </div>

          <!-- 右侧查看栏 -->
          <GlobalRightPanel />

          <!-- 独立 sibling 拖拽手柄 -->
          <div
            v-if="rightPanel.show"
            class="app-resize-handle"
            :style="{'--rpw': rightPanel.width + 'px'}"
            @mousedown="onRightPanelResizeStart"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import GlobalRightPanel from '@/components/GlobalRightPanel.vue'
import { useContentTabsStore } from '@/stores/contentTabs'
import { useRightPanelStore } from '@/stores/rightPanel'
import { getPreferences } from '@/api/user'

const route = useRoute()
const router = useRouter()
const isAuthPage = computed(() => route.path === '/auth' || route.path.startsWith('/auth/'))
const rightPanel = useRightPanelStore()
const contentTabs = useContentTabsStore()

/* 替换式预览联动：点卡片 open → activeId 变化 → 路由切到 /card-detail（不覆盖） */
watch(
  () => contentTabs.activeId,
  (id) => {
    if (id && route.path !== '/card-detail') {
      router.push('/card-detail')
    }
  }
)

/* 全部标签关闭后，若停留在预览页则返回来源页 */
watch(
  () => contentTabs.tabs.length,
  (n) => {
    if (n === 0 && route.path === '/card-detail') {
      router.push(contentTabs.lastRoute || '/notes')
    }
  }
)

/* 记录来源路由：离开预览页时记录位置，供关闭预览后返回 */
watch(
  () => route.fullPath,
  (fp) => {
    if (fp && fp !== '/card-detail' && fp !== '/auth') {
      contentTabs.lastRoute = fp
    }
  },
  { immediate: true }
)

/**
 * 右侧面板只显示低优先级辅助 tab（由侧边栏 navigate 送入右栏），
 * 不按中间路由联动，避免出现「中间和右边显示同一份内容」。
 * 笔记界面自带面板，无需额外辅助。
 */
const exploreAssist = { key: 'explore', label: '发现灵感' }

/**
 * 三栏模式下，低优先级页面只在右侧辅助面板停留，中间主区域固定为高优先级页
 * （笔记 /notes、个人信息 /profile、预览 /card-detail）。
 * 打开右栏时若中间停在低优先级路由，先把它送回 /notes，再在右栏显示该内容。
 */
const defaultAssist: Record<string, { key: string; label: string }> = {
  '/explore': exploreAssist,
  '/inspirations': { key: 'inspirations', label: '灵感目的地' },
  '/journeys': { key: 'journeys', label: '我的旅程' },
  '/chat': { key: 'chat', label: 'AI 旅行规划' },
  '/journey-map': { key: 'map', label: '足迹地图' },
  '/users/search': { key: 'search', label: '寻找同好' },
  '/agent-panel': { key: 'agent', label: '互动式规划' }
}

/** 右上角固定开关：隐藏 -> 打开右栏（按当前页面给对应辅助，低优先级页面移入右栏）；显示 -> 关闭 */
function onPanelToggle() {
  if (rightPanel.show) {
    rightPanel.close()
    return
  }
  const assist = defaultAssist[route.path]
  if (assist) {
    rightPanel.openView(assist.key, assist.label)
    // 中间不是高优先级页时，送回 /notes，避免中间和右栏重复
    if (route.path !== '/notes') {
      router.push('/notes')
    }
    return
  }
  // /profile、/card-detail 等：保持中间，右栏给一个通用辅助
  if (!rightPanel.type || rightPanel.type === 'empty' || !rightPanel.viewKey) {
    rightPanel.openView(exploreAssist.key, exploreAssist.label)
  } else {
    rightPanel.show = true
  }
}

function onRightPanelResizeStart(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'

  const handleMove = (ev: MouseEvent) => {
    const panel = document.querySelector('.global-right-panel') as HTMLElement | null
    if (!panel) return
    // panel 右边界减去鼠标 X = 新宽度（鼠标越往左，面板越宽）
    const right = panel.getBoundingClientRect().right
    const w = Math.min(860, Math.max(260, right - ev.clientX))
    rightPanel.setWidth(w)
  }
  const handleUp = () => {
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.addEventListener('mousemove', handleMove)
  document.addEventListener('mouseup', handleUp)
}
const systemDefaults = { fg: '#1D2B27', bg: '#F7F3EA', accent: '#164E42' }

async function applySystemPalette() {
  const root = document.documentElement
  let palette = systemDefaults
  try {
    const result = await getPreferences()
    const p = result.data || {}
    const saved = p.systemThemeJson ? JSON.parse(p.systemThemeJson) : {}
    palette = { fg: saved.fg || systemDefaults.fg, bg: saved.bg || systemDefaults.bg, accent: saved.accent || systemDefaults.accent }
  } catch { /* keep defaults when the profile cannot be loaded */ }
  root.style.setProperty('--ink', palette.fg)
  root.style.setProperty('--paper', palette.bg)
  root.style.setProperty('--forest', palette.accent)
}

watch(() => route.path, () => { void applySystemPalette() }, { immediate: true })
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=Manrope:wght@400;500;600;700;800&family=Noto+Sans+SC:wght@400;500;600;700;800&display=swap');

/* ─── Roamly Design Tokens ─────────────────────────────────────── */
:root {
  --forest: #164E42;        /* Primary · Forest Green   */
  --forest-deep: #0E382E;   /* Primary deep             */
  --roam: #4F8F78;          /* Secondary · Roam Green   */
  --roam-soft: #E9F1EC;     /* Roam 8% surface          */
  --sunset: #F27A4F;        /* Accent · Sunset Orange   */
  --sunset-soft: #FDEEE6;   /* Sunset surface           */
  --paper: #F7F3EA;         /* Background · Paper       */
  --card: #FFFDF8;          /* Card surface             */
  --wash: #F2EDE1;          /* Sunken wash              */
  --ink: #1D2B27;           /* Primary text             */
  --ink-2: #5C6B65;         /* Secondary text           */
  --ink-3: #8C9993;         /* Tertiary text            */
  --line: #E7E0D2;          /* Hairline border          */
  --shadow-soft: 0 2px 12px rgba(22, 78, 66, 0.06);
  --shadow-lift: 0 12px 30px rgba(22, 78, 66, 0.10);
  --radius-card: 24px;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

html, body, #app { height: 100%; }

body {
  background: var(--paper);
  color: var(--ink);
  font-family: Manrope, "Noto Sans SC", -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ─── 顶层两列布局：AppSidebar | app-main ─────────────────────────
   AppHeader + app-body(workspace + GlobalRightPanel) 都在 app-main 里。
   Header 是一整条横跨 app-main 剩余宽度的绿色栏，不再被右侧面板切断。
*/
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  overflow: visible;
  padding: 0;
  background: var(--paper, #F7F3EA);
}

/* 顶部深绿色导航栏：横跨整个系统宽度（左栏 + 右栏） */
.app-header-row {
  flex-shrink: 0;
  display: flex;
  align-items: stretch;
  padding: 0 8px;                    /* 和下方主体的 8px 外边距对齐 */
  padding-top: 8px;                   /* 顶上加 8px，和 AppSidebar 顶部圆角对齐 */
  box-sizing: border-box;
  background: transparent;
}

.app-header-left-spacer {
  flex-shrink: 0;
  width: 56px;                       /* 和 AppSidebar 同宽 */
  height: 48px;
  margin-right: 8px;                 /* 和 .app-main-row 的 gap 对齐 */
}

/* AppHeader 本身在 AppHeader.vue 里 height:48px */
.app-header-row > .app-header {
  flex: 1;
  min-width: 0;
  height: 48px;
  background: var(--forest);
  color: #fff;
  flex-shrink: 0;
  border-radius: 18px;               /* 整个顶栏独立圆角（只有上半部分能看到完整圆角） */
  margin-right: 8px;                 /* 和右栏右边 8px 外边距对齐 */
}

/* 底部主体：左栏 + 中间大卡片 */
.app-main-row {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  padding: 0 8px 8px 8px;   /* 右下左 8px，顶部 0 交给 header row */
  gap: 0;
  overflow: hidden;
  align-items: stretch;
}

/* 顶层两列里的 app-main：撑满整个剩余宽度，右上、右下都留 8px 外边距 */
.app-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: visible;
  margin: 0 0 0 0;
  height: auto;                      /* 让 flex row 自己算高度 */
  align-self: stretch;
}

/* app-body：横向排布 workspace | GlobalRightPanel */
.app-body {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  background: inherit;       /* 显式继承，防止 isolation/contain 切断 */
}

/* 中间工作区 */
.workspace {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: inherit;       /* 显式继承主题 paper 色 */
  isolation: auto;
}

/* 底层路由页面容器：让 NotesView / JourneyView 真正填满 workspace */
.workspace-route-view {
  position: relative;
  width: 100%;
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: inherit;
}

/* ─── 独立 sibling 拖拽手柄 ──────────────────────────
   挂在 app-body 里、GlobalRightPanel 的旁边，绝对定位到面板左边缘。
   它是 workspace 和 right-panel 的 sibling，不嵌在 panel 里面，
   所以不受 GlobalRightPanel 的 overflow / isolation / z-index 影响。
   注意：本 style 是原生 CSS（无 lang="scss"），必须展开成纯 CSS 选择器，
   不能使用 & 嵌套语法。 */
.app-resize-handle {
  position: absolute;
  top: 0;           /* 视觉竖线从 app-body 顶部贯穿到底 */
  bottom: 0;
  width: 14px;      /* 加宽热区，面板窄时也好命中 */
  right: var(--rpw, 360px);
  transform: translateX(0);
  cursor: col-resize;
  z-index: 999999;  /* 压过 NotesView / 面板内任何高 z-index 元素 */
  background: transparent;
  pointer-events: auto;
}

/* 视觉竖线贯穿整个 app-body */
.app-resize-handle::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  transform: translateX(-45%);   /* 让竖线跨在 workspace 与右侧面板的缝隙正中间 */
  width: 2px;
  background: #d5d8dc;
  box-shadow: 1px 0 2px rgba(0,0,0,.04);
  transition: background .15s, width .15s, box-shadow .15s;
}

.app-resize-handle:hover::before {
  width: 3px;
  background: var(--forest, #164E42);
  box-shadow: 0 0 6px rgba(22,78,66,.25);
}

/* Notes page 自己管自己的满屏布局 */
.notes-app {
  border-radius: 0 !important;
  border: none !important;
  margin: 0 !important;
  height: 100% !important;
  min-height: 100%;
  width: 100%;
  position: relative;
}

/* Element Plus 主色对齐品牌色 */
.el-button--primary {
  --el-button-bg-color: var(--forest);
  --el-button-border-color: var(--forest);
  --el-button-hover-bg-color: var(--roam);
  --el-button-hover-border-color: var(--roam);
  --el-button-active-bg-color: var(--forest-deep);
  --el-button-active-border-color: var(--forest-deep);
}

/* 滚动条统一风格 */
::-webkit-scrollbar { width: 9px; height: 9px; }
::-webkit-scrollbar-thumb { background: #D8D1C0; border-radius: 8px; }
::-webkit-scrollbar-thumb:hover { background: #C4BCA8; }
::-webkit-scrollbar-track { background: transparent; }
</style>
