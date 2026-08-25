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
  height: 100vh;
  background: var(--paper, #f7f3ea);
  border-left: 1px solid var(--line, #e5e6e8);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.04);
  isolation: isolate;
}

.panel-drag-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -4px;
  width: 10px;
  cursor: col-resize;
  z-index: 100;
  background: transparent;
  transition: background .15s;

  &:hover { background: rgba(51, 112, 255, 0.06); }
  &:hover::after {
    content: '';
    position: absolute;
    left: 4px;
    top: 50%;
    transform: translateY(-50%);
    width: 2px;
    height: 48px;
    border-radius: 1px;
    background: #c5c7ca;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e6e8;
  background: var(--card, #fffdf8);
  flex-shrink: 0;
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
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--paper, #f7f3ea);
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
