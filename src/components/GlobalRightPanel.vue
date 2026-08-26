<script setup lang="ts">
import { ref, computed, defineAsyncComponent, type Component } from 'vue'
import { useRightPanelStore } from '@/stores/rightPanel'

const panel = useRightPanelStore()

const linkLoading = ref(false)

// 视图组件映射：侧边栏 tab -> 右侧面板直接渲染的组件
const viewComponents: Record<string, Component> = {
  explore: defineAsyncComponent(() => import('@/views/ExploreView.vue')),
  chat: defineAsyncComponent(() => import('@/views/ChatView.vue')),
  inspirations: defineAsyncComponent(() => import('@/views/InspirationView.vue')),
  journeys: defineAsyncComponent(() => import('@/views/JourneyView.vue')),
  search: defineAsyncComponent(() => import('@/views/UserSearchView.vue')),
  map: defineAsyncComponent(() => import('@/views/JourneyMapView.vue')),
  agent: defineAsyncComponent(() => import('@/views/AgentPanel.vue'))
}

const activeComponent = computed(() => {
  if (panel.type !== 'view' || !panel.viewKey) return null
  return viewComponents[panel.viewKey] || null
})

// 拖拽调整宽度
function onDragStart(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'

  const handleMove = (ev: MouseEvent) => {
    const windowWidth = window.innerWidth
    const newWidth = Math.min(860, Math.max(300, windowWidth - ev.clientX))
    panel.setWidth(newWidth)
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

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener')
}

function copyUrl(url: string) {
  navigator.clipboard?.writeText(url).then(() => {}).catch(() => {})
}
</script>

<template>
  <Transition name="panel-slide">
    <aside
      v-if="panel.show"
      class="global-right-panel"
      :style="{ width: panel.width + 'px' }"
    >
      <!-- 拖拽手柄 -->
      <div class="panel-drag-handle" @mousedown="onDragStart"></div>

      <!-- 面板标题栏 -->
      <div class="panel-header">
        <div class="panel-title">{{ panel.title }}</div>
        <div class="panel-actions">
          <button
            v-if="panel.type === 'link' && panel.linkData"
            class="panel-btn"
            title="在新标签页打开"
            @click="openExternal(panel.linkData!.url)"
          >↗</button>
          <button
            v-if="panel.type === 'link' && panel.linkData"
            class="panel-btn"
            title="复制链接"
            @click="copyUrl(panel.linkData!.url)"
          >📋</button>
          <button class="panel-btn close" title="收起面板" @click="panel.close()">✕</button>
        </div>
      </div>

      <!-- 面板内容 -->
      <div class="panel-body">
        <!-- 直接渲染对应功能页面 -->
        <div v-if="panel.type === 'view' && activeComponent" class="view-wrapper">
          <component :is="activeComponent" />
        </div>

        <!-- 链接预览 -->
        <div v-else-if="panel.type === 'link' && panel.linkData" class="link-panel-content">
          <div class="link-meta">
            <span class="link-dot"></span>
            <span class="link-url" :title="panel.linkData.url">{{ panel.linkData.title }}</span>
          </div>
          <iframe
            :src="panel.linkData.url"
            class="link-iframe"
            frameborder="0"
            @load="linkLoading = false"
          ></iframe>
          <div v-if="linkLoading" class="link-loading">内容加载中…</div>
          <div class="link-fallback" @click="openExternal(panel.linkData!.url)">
            无法内嵌预览？点击这里在新标签页打开 →
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-panel">
          <p>点击左侧侧边栏的功能，在右侧打开对应内容。</p>
        </div>
      </div>
    </aside>
  </Transition>
</template>

<style scoped lang="scss">
.global-right-panel {
  position: relative;
  flex-shrink: 0;
  height: calc(100vh - 16px);
  margin: 8px 8px 8px 0;
  border: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  border-radius: 18px;
  padding: 12px;
  box-shadow: -4px 0 18px rgba(31,35,41,.10);
  background: var(--notes-bg, #fafafa);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  isolation: isolate;
  box-sizing: border-box;

  /* 左侧强调线（NotesView section-drag-handle::after 风格）*/
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: -1px;
    transform: translateY(-50%);
    width: 28px;
    height: 3px;
    border-radius: 2px;
    background: var(--notes-accent, #3370ff);
    opacity: 0.5;
    transition: width .2s, opacity .2s, box-shadow .2s;
    pointer-events: none;
  }

  &:hover::before {
    width: 36px;
    opacity: 1;
    box-shadow: 0 0 8px rgba(51,112,255,.25);
  }
}





.panel-drag-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -7px;
  width: 14px;
  cursor: col-resize;
  z-index: 100;
  background: transparent;
  pointer-events: auto;
  user-select: none;
  touch-action: none;

  /* 热区：向上向下各扩展 8px，整条可拖拽 */
  &::before {
    content: '';
    position: absolute;
    top: -8px;
    bottom: -8px;
    left: 0;
    right: 0;
    background: transparent;
  }

  /* 分割线：整条垂直，NotesView 风格 */
  &::after {
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

  &:hover::after {
    width: 3px;
    background: var(--notes-accent, #3370ff);
    box-shadow: 0 0 6px rgba(51,112,255,.25);
  }
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 12px;
  background: var(--notes-bg, #fafafa);
  border-bottom: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  margin-bottom: 4px;
  z-index: 10;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f2329;
}

/* 右侧面板滚动条 */
.view-wrapper::-webkit-scrollbar {
  width: 6px;
}

.view-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,.2);
  border-radius: 3px;
}

.view-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

/* 内部视图包装 */
.view-wrapper {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;

  /* 重置内部页面组件的滚动行为，避免双滚动条 */
  :deep(.page),
  :deep([class$='-page']) {
    overflow: visible !important;
    overflow-y: visible !important;
    min-height: 0 !important;
    height: auto !important;
    max-width: none !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 12px !important;
    box-sizing: border-box;
  }
}

.view-wrapper > * {
  flex-shrink: 0;
}

/* 链接预览 */
.link-panel-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 空状态 */
.empty-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--ink-3, #8C9993);
  padding: 24px;
}

.panel-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.panel-btn {
  width: 28px;
  height: 28px;
  border: 0;
  background: #f0f1f2;
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #646a73;
  transition: background .15s, color .15s;

  &:hover { background: #e5e6e8; color: #245bdb; }
  &.close:hover { background: #fee; color: #c33; }
}

.panel-body {
  flex: 1 1 auto;
  overflow: hidden;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: transparent;
}

/* 嵌入页面容器：可滚动、占满、不强制白色。
   contain + isolation 将组件内部 fixed/溢出元素约束在面板内，
   防止右侧页面透出覆盖中间笔记编辑区 */
.view-wrapper {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: auto;
  background: transparent;
  position: relative;
  isolation: isolate;
  contain: layout paint style;
  pointer-events: auto;
}

.view-wrapper > :deep(*) {
  min-height: 100%;
  max-width: 100%;
  background: transparent;
}

/* 链接面板 */
.link-panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.link-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e6e8;
  flex-shrink: 0;

  .link-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4caf50;
    flex-shrink: 0;
  }

  .link-url {
    font-size: 12px;
    color: #646a73;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.link-iframe {
  flex: 1;
  width: 100%;
  border: 0;
  background: #fff;
  min-height: 0;
}

.link-loading {
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #8f959e;
  font-size: 13px;
}

.link-fallback {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: #8f959e;
  background: rgba(255, 255, 255, 0.92);
  padding: 5px 12px;
  border-radius: 14px;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &:hover { color: #245bdb; }
}

/* 空状态 */
.empty-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #8f959e;
  font-size: 13px;
  line-height: 1.8;
  text-align: center;
}

/* 动画 */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: width .2s ease, opacity .2s ease;
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  width: 0 !important;
  opacity: 0;
  overflow: hidden;
}
</style>
