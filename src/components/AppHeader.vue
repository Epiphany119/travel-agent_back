<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useContentTabsStore } from '@/stores/contentTabs'

const router = useRouter()
const keyword = ref('')
const tabs = useContentTabsStore()

function search() {
  const q = keyword.value.trim()
  if (q) router.push({ path: '/inspirations', query: { q } })
  else router.push('/inspirations')
}
</script>

<template>
  <header class="app-header">
    <div class="search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        v-model="keyword"
        placeholder="搜索目的地、旅程…"
        @keyup.enter="search"
      />
    </div>

    <!-- 嵌入式标签栏：仿 Chrome 标签风格 -->
    <div v-if="tabs.tabs.length" class="inline-tabs">
      <div class="tabs-scroll">
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
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 48px;
  padding: 4px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  background: transparent;
  flex-shrink: 0;
}

/* 搜索框：透明、无胶囊背景，仅底部一条细线 */
.search {
  flex: 0 0 auto;
  max-width: 320px;
  display: flex;
  align-items: center;
  gap: 9px;
  background: transparent;
  border: 0;
  border-bottom: 1px solid rgba(255,255,255,0.35);
  padding: 6px 2px;
  transition: border-color .15s;

  &:hover,
  &:focus-within {
    border-bottom-color: rgba(255,255,255,0.8);
  }

  svg {
    width: 15px;
    height: 15px;
    color: rgba(255,255,255,0.7);
    flex-shrink: 0;
  }

  input {
    width: 180px;
    border: 0;
    background: transparent;
    outline: 0;
    color: #fff;
    font: 600 13px Manrope, "Noto Sans SC", sans-serif;

    &::placeholder { color: rgba(255,255,255,0.5); }
  }
}

/* 嵌入在 header 里的 Chrome 风格标签栏 */
.inline-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1 1 auto;
  min-width: 0;
  height: 30px;   /* ← 整个白色标签板块高度压到 30px */
  padding: 0 6px;
  background: rgba(255,255,255,0.92);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.08);
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
  color: var(--ink-3, #8c9993);
  font-size: 12px;
  line-height: 1;
  height: 100%;
  cursor: pointer;
  white-space: nowrap;
  transition: color .15s;
  flex-shrink: 0;
  position: relative;
  border-bottom: 2px solid transparent;

  &:hover { color: var(--ink, #1d2b27); }

  &.active {
    background: transparent;  /* 不做白色胶囊，保持跟外层白膜一体 */
    color: var(--forest, #164e42);
    font-weight: 700;
    border-bottom-color: var(--forest, #164e42);  /* 绿色下划线 */
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
  color: var(--ink-3, #8c9993);
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 4px;
  flex-shrink: 0;

  &:hover { color: var(--sunset, #f27a4f); }
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
  border-radius: 8px;
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
  border-radius: 8px;
  border: 0;
  background: rgba(255,255,255,0.15);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;

  svg { width: 15px; height: 15px; }

  &:hover { background: rgba(255,255,255,0.25); }
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
