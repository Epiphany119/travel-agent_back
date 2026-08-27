<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useContentTabsStore, type ContentTab } from '@/stores/contentTabs'

const router = useRouter()
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
/** 副标题（作者/城市/目的地） */
const subtitle = computed(() => {
  const d = tab.value?.data ?? {}
  const parts: string[] = []
  if (d.author) parts.push(d.author)
  if (d.city) parts.push(d.city)
  if (d.destination) parts.push(d.destination)
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

/** 返回：关闭当前预览标签并回来源页面（替换式预览，不覆盖） */
function goBack() {
  if (tab.value) tabs.close(tab.value.id)
  router.push(tabs.lastRoute || '/notes')
}
</script>

<template>
  <section class="card-detail-page">
    <!-- 圆弧框预览页：与 app-main 卡片风格一致 -->
    <div class="cdp-bar">
      <button class="cdp-back" @click="goBack">← 返回</button>
      <span class="cdp-count" v-if="tabs.tabs.length">预览标签 {{ tabs.tabs.length }} 个</span>
    </div>

    <article v-if="tab" class="detail-wrap">
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

    <div v-else class="empty-state">
      <p>没有正在预览的卡片。</p>
      <p>请点击右侧面板中的旅程 / 灵感 / 发现卡片。</p>
    </div>
  </section>
</template>

<style>
/* 预览页：独立路由页面（中间工作区被替换成它），圆弧框风格与 app-main 卡片一致 */
.card-detail-page {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--paper, #F7F3EA);
  color: var(--ink, #1D2B27);
  border: 1px solid var(--line, #E7E0D2);
  border-radius: 16px;
  box-shadow: var(--shadow-soft, 0 2px 12px rgba(22, 78, 66, 0.06));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.cdp-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--line, #E7E0D2);
}

.cdp-back {
  border: 0;
  background: var(--roam-soft, #E9F1EC);
  color: var(--forest, #164E42);
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s, transform .15s;
}

.cdp-back:hover {
  background: color-mix(in srgb, var(--roam, #4F8F78) 18%, var(--paper, #F7F3EA));
  transform: translateY(-1px);
}

.cdp-count {
  font-size: 12px;
  color: var(--ink-3, #8C9993);
}

.detail-wrap {
  flex: 1;
  width: min(860px, calc(100% - 48px));
  margin: 0 auto;
  padding: 32px 0 60px;
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
}

.detail-cover img {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  display: block;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 0 22px;
}

.tag-row span {
  padding: 5px 12px;
  border-radius: 20px;
  background: var(--roam-soft, #e9f1ec);
  color: var(--forest, #164e42);
  font-size: 12px;
  font-weight: 700;
}

.detail-body p {
  font-size: 16px;
  line-height: 1.85;
  color: var(--ink, #1d2b27);
  margin: 0 0 18px;
}

.empty {
  color: var(--ink-3, #8c9993);
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--ink-3, #8C9993);
  font-size: 14px;
}
</style>
