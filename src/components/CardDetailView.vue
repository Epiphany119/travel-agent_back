<script setup lang="ts">
import { computed } from 'vue'
import { useContentTabsStore, type ContentTab } from '@/stores/contentTabs'

const tabs = useContentTabsStore()
const tab = computed<ContentTab | null>(() => tabs.activeTab)

/** 封面图地址 */
const cover = computed(() => {
  const d = tab.value?.data ?? {}
  return d.imageUrl || d.image || ''
})
/** 主标题 */
const heading = computed(() => {
  const d = tab.value?.data ?? {}
  return d.title || d.name || tab.value?.title || '卡片详情'
})
/** 副标题（作者/城市） */
const subtitle = computed(() => {
  const d = tab.value?.data ?? {}
  const parts: string[] = []
  if (d.author) parts.push(d.author)
  if (d.city) parts.push(d.city)
  return parts.join(' · ')
})
/** 标签 */
const tags = computed<string[]>(() => {
  const d = tab.value?.data ?? {}
  if (Array.isArray(d.tags)) return d.tags
  const t = d.bestSeason || d.season || ''
  return t ? [t] : []
})
/** 正文段落 */
const paragraphs = computed<string[]>(() => {
  const d = tab.value?.data ?? {}
  const raw = String(d.content || d.description || d.quote || '').trim()
  if (!raw) return []
  return raw.split(/\n+/).map(s => s.trim()).filter(Boolean)
})
</script>

<template>
  <div class="card-detail" v-if="tab">
    <article class="detail-wrap">
      <header class="detail-head">
        <p class="eyebrow">{{ tab.kind === 'inspiration' ? 'INSPIRATION' : 'TRAVEL NOTE' }}</p>
        <h1>{{ heading }}</h1>
        <p v-if="subtitle" class="sub">{{ subtitle }}</p>
      </header>

      <div v-if="cover" class="detail-cover">
        <img :src="cover" :alt="heading" />
      </div>

      <div v-if="tags.length" class="tag-row">
        <span v-for="t in tags" :key="t">{{ t }}</span>
      </div>

      <div class="detail-body">
        <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
        <p v-if="!paragraphs.length" class="empty">该卡片暂无更多内容。</p>
      </div>
    </article>
  </div>
</template>

<style scoped lang="scss">
.card-detail {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background: var(--paper, #f7f3ea);
}

.detail-wrap {
  width: min(860px, calc(100% - 48px));
  margin: 0 auto;
  padding: 40px 0 80px;
}

.eyebrow {
  color: var(--sunset, #f27a4f);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .16em;
  margin: 0 0 8px;
}

.detail-head h1 {
  margin: 0 0 10px;
  font: 34px 'DM Serif Display', 'Noto Sans SC';
  color: var(--ink, #1d2b27);
}

.sub {
  color: var(--ink-2, #5c6b65);
  font-size: 14px;
  margin: 0;
}

.detail-cover {
  margin: 22px 0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 26px rgba(22, 78, 66, .12);

  img {
    width: 100%;
    max-height: 420px;
    object-fit: cover;
    display: block;
  }
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 0 22px;

  span {
    padding: 5px 12px;
    border-radius: 20px;
    background: var(--roam-soft, #e9f1ec);
    color: var(--forest, #164e42);
    font-size: 12px;
    font-weight: 700;
  }
}

.detail-body p {
  font-size: 16px;
  line-height: 1.85;
  color: var(--ink, #1d2b27);
  margin: 0 0 18px;
}

.empty { color: var(--ink-3, #8c9993); }
</style>
