<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')

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
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  height: 52px;
  padding: 6px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  background: transparent;
  flex-shrink: 0;
}

/* 搜索框：透明、无胶囊背景，仅底部一条细线 */
.search {
  flex: 1;
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
    flex: 1;
    border: 0;
    background: transparent;
    outline: 0;
    color: #fff;
    font: 600 13px Manrope, "Noto Sans SC", sans-serif;

    &::placeholder { color: rgba(255,255,255,0.5); }
  }
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
  padding: 9px 18px;
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
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 0;
  background: rgba(255,255,255,0.15);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;

  svg { width: 17px; height: 17px; }

  &:hover { background: rgba(255,255,255,0.25); }
}

.badge-dot {
  position: absolute;
  top: 9px;
  right: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--sunset);
  box-shadow: 0 0 0 2px var(--card);
}

@media (max-width: 640px) {
  .app-header { padding: 8px 12px; }
  .icon-btn { display: none; }
}
</style>
