<script setup lang="ts">
import { useContentTabsStore } from '@/stores/contentTabs'

const tabs = useContentTabsStore()
</script>

<template>
  <div class="card-tab-bar">
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
    <button v-if="tabs.tabs.length" class="tab-close-all" title="关闭全部" @click="tabs.closeAll()">✕</button>
  </div>
</template>

<style scoped lang="scss">
.card-tab-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 10px 0;
  background: var(--card, #fffdf8);
  border-bottom: 1px solid var(--line, #e7e0d2);
  flex-shrink: 0;
}

.tabs-scroll {
  display: flex;
  align-items: flex-end;
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
  max-width: 170px;
  padding: 0 6px 1px 9px;
  border: 1px solid var(--line, #e7e0d2);
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  background: var(--wash, #f2ede1);
  color: var(--ink-2, #5c6b65);
  font-size: 10px;
  line-height: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: background .15s, color .15s;
  flex-shrink: 0;

  &:hover { color: var(--ink, #1d2b27); }

  &.active {
    background: var(--card, #fffdf8);
    color: var(--forest, #164e42);
    font-weight: 700;
    box-shadow: 0 -2px 8px rgba(22, 78, 66, .06);
  }
}

.tab-label {
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.tab-close {
  font-size: 12px;
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
  font-size: 10px;
  line-height: 14px;
  cursor: pointer;
  padding: 4px 6px;
  flex-shrink: 0;

  &:hover { color: var(--sunset, #f27a4f); }
}
</style>
