<template>
  <router-view v-if="isAuthPage" />
  <div v-else class="app-layout">
    <AppSidebar />
    <div class="app-main">
      <AppHeader />
      <router-view />
    </div>
    <GlobalRightPanel />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import GlobalRightPanel from '@/components/GlobalRightPanel.vue'
import { getPreferences } from '@/api/user'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/auth' || route.path.startsWith('/auth/'))
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

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
}

body {
  background: var(--paper);
  color: var(--ink);
  font-family: Manrope, "Noto Sans SC", -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 系统级三层布局：左侧边栏 | 中间主体 | 右侧面板 */
.app-layout {
  display: flex;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
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
