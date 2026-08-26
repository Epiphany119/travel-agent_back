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
      <button class="new-btn" @click="router.push('/journeys')">
        <span class="plus">＋</span> 新旅程
      </button>
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
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
  margin: 0 auto;
  height: 54px;
  padding: 8px 18px;
  border-bottom: 1px solid var(--line);
  background: var(--paper);
}

/* 搜索胶囊（模板中的深绿圆角搜索条） */
.search {
  flex: 1;
  max-width: 480px;
  display: flex;
  align-items: center;
  gap: 11px;
  background: var(--forest);
  border-radius: 8px;
  padding: 8px 12px;

  svg {
    width: 16px;
    height: 16px;
    color: #9db8ad;
    flex-shrink: 0;
  }

  input {
    flex: 1;
    border: 0;
    background: transparent;
    outline: 0;
    color: var(--card);
    font: 600 13px Manrope, "Noto Sans SC", sans-serif;

    &::placeholder { color: #8fa89e; }
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.new-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 0;
  background: var(--forest);
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  padding: 11px 22px;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(45, 106, 79, 0.28);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(45, 106, 79, 0.34);
  }

  .plus { font-size: 15px; line-height: 1; }
}

.icon-btn {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--card);
  color: var(--forest);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.2s;

  svg { width: 18px; height: 18px; }

  &:hover { background: var(--paper); }
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
