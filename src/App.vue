<template>
  <router-view v-if="isAuthPage" />
  <div v-else class="app-layout">
    <AppSidebar />
    <div class="app-main">
      <AppHeader />
      <div class="app-body">
        <!-- 中间工作区 -->
        <div class="workspace">
          <!-- 底层页面（如旅行笔记）保持存活 -->
          <router-view />
        </div>

        <!-- 右侧查看栏：嵌入 app-body，与 header 共享同一张卡片背景 -->
        <GlobalRightPanel />

        <!-- 独立 sibling 拖拽手柄，绝对定位在 GlobalRightPanel 左边缘 -->
        <div
          v-if="rightPanel.show"
          class="app-resize-handle"
          :style="{'--rpw': rightPanel.width + 'px'}"
          @mousedown="onRightPanelResizeStart"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import GlobalRightPanel from '@/components/GlobalRightPanel.vue'
import { useRightPanelStore } from '@/stores/rightPanel'
import { getPreferences } from '@/api/user'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/auth' || route.path.startsWith('/auth/'))
const rightPanel = useRightPanelStore()

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
  height: 100vh;
  min-height: 0;
  overflow: visible;
  padding: 0;
  background: var(--paper, #F7F3EA);
}

/* 顶层两列里的 app-main：撑满整个剩余宽度，右上、右下都留 8px 外边距 */
.app-main {
  flex: 1;
  height: calc(100vh - 16px);
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: visible;
  margin: 8px 8px 8px 8px;
  border-radius: 18px;
  border: 1px solid var(--line, #e5e6e8);
  background: var(--card, #FFFDF8);
}

/* header 在 app-main 里，顶部圆角贴齐卡片 */
.app-main > .app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-radius: 18px 18px 0 0;
  background: var(--forest);
  color: #fff;
  flex-shrink: 0;
}

.app-main > .app-header .search input { background: transparent; color: #fff; }
.app-main > .app-header .search input::placeholder { color: rgba(255,255,255,0.5); }
.app-main > .app-header .search svg { color: rgba(255,255,255,0.7); }
.app-main > .app-header .icon-btn { color: #fff; }
.app-main > .app-header .icon-btn:hover { background: rgba(255,255,255,0.1); }
.app-main > .app-header .new-btn { background: #fff; color: var(--forest); }

/* app-body：横向排布 workspace | GlobalRightPanel */
.app-body {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  overflow: hidden;
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
}

/* ─── 独立 sibling 拖拽手柄 ──────────────────────────
   挂在 app-body 里、GlobalRightPanel 的旁边，绝对定位到面板左边缘。
   它是 workspace 和 right-panel 的 sibling，不嵌在 panel 里面，
   所以不受 GlobalRightPanel 的 overflow / isolation / z-index 影响。 */
.app-resize-handle {
  position: absolute;
  top: 0;           /* 视觉竖线从 app-body 顶部贯穿到底 */
  bottom: 0;
  width: 10px;
  right: var(--rpw, 360px);
  /* -50% 时竖线正好落在面板左边缘；改成 -30% 让手柄中心往右偏 ~2px，
     竖线跨过两个面板的边界一点，看起来在缝隙正中间 */
  /* -30% 让竖线跨过两个面板的边界一点，看起来在缝隙正中间 */
  transform: translateX(-0%);
  cursor: col-resize;
  z-index: 500;
  background: transparent;

  /* 视觉竖线贯穿整个 app-body */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 2px;
    background: #d5d8dc;
    box-shadow: 1px 0 2px rgba(0,0,0,.04);
    transition: background .15s, width .15s, box-shadow .15s;
  }
  &:hover::before {
    width: 3px;
    background: var(--forest, #164E42);
    box-shadow: 0 0 6px rgba(22,78,66,.25);
  }
}

/* Notes page 自己管自己的满屏布局 */
.notes-app {
  border-radius: 0 !important;
  border: none !important;
  margin: 0 !important;
  height: 100vh !important;
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
