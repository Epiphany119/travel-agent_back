<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createNote } from '@/api/note'
import { getCurrentUserId, publishSocialNote } from '@/api/user'
import { renderMarkdown, sanitizeRichHtml } from '@/utils/markdown'
import RichNoteEditor from '@/components/RichNoteEditor.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = ref('')
const content = ref('')
const coverUrl = ref('')
const destination = ref('')
const tagsInput = ref('')
const mode = ref<'edit' | 'preview'>('edit')
const publishing = ref(false)
const savedLocally = ref(false)

const tags = computed(() => tagsInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean))
const preview = computed(() => sanitizeRichHtml(content.value || '<p class="empty-copy">还没有写下内容。</p>'))
const fallbackCover = computed(() => `linear-gradient(135deg, var(--forest), var(--roam))`)

function normalizeContent(value: string) {
  if (!value) return ''
  return /<[a-z][\s\S]*>/i.test(value) ? sanitizeRichHtml(value) : renderMarkdown(value)
}

onMounted(() => {
  title.value = String(route.query.title || '')
  content.value = normalizeContent(String(route.query.content || ''))
  coverUrl.value = String(route.query.image || '')
  destination.value = String(route.query.city || route.query.destination || '')
  tagsInput.value = String(route.query.tags || '')
})

async function publish() {
  if (!title.value.trim()) return ElMessage.warning('请填写标题')
  if (!content.value.trim()) return ElMessage.warning('请写一点正文')
  publishing.value = true
  try {
    // 先落一份私有文档，再发布社区快照，保证内容随时可以迁移回笔记。
    await createNote({ title: title.value.trim(), content: content.value, destination: destination.value.trim(), coverUrl: coverUrl.value.trim(), visibility: 'private' })
    await publishSocialNote({ userId: getCurrentUserId(), title: title.value.trim(), content: content.value, coverUrl: coverUrl.value.trim(), destination: destination.value.trim(), tags: tags.value, authorName: userStore.nickname || '旅行者' })
    savedLocally.value = true
    ElMessage.success('已发布到我的圈子')
    router.push('/explore')
  } catch (error: any) {
    ElMessage.error(error?.message || '发布失败，请稍后重试')
  } finally { publishing.value = false }
}
</script>

<template>
  <main class="publish-page">
    <header class="publish-head">
      <div class="head-copy"><span class="eyebrow">MAKE IT YOURS</span><h1>发布一篇旅行笔记</h1><p>把路线、照片和那些只有你会记得的细节，分享给自己的圈子。</p></div>
      <div class="head-actions"><button class="quiet-btn" @click="router.back()">← 返回</button><button class="preview-btn" :class="{ active: mode === 'preview' }" @click="mode = mode === 'preview' ? 'edit' : 'preview'">{{ mode === 'preview' ? '继续编辑' : '预览' }}</button><button class="publish-btn" :disabled="publishing" @click="publish">{{ publishing ? '发布中…' : '发布到圈子' }}</button></div>
    </header>

    <section class="publish-layout">
      <article class="composer-card">
        <div class="cover-editor" :style="coverUrl ? { backgroundImage: `url(${coverUrl})` } : { background: fallbackCover }"><div class="cover-overlay"></div><div class="cover-content"><span class="cover-kicker">TRAVEL NOTE</span><input v-model="title" class="title-input" placeholder="给这段旅程起个名字" /><div class="cover-meta"><span>⌖ <input v-model="destination" placeholder="添加目的地" /></span><span v-for="tag in tags" :key="tag">#{{ tag }}</span></div></div></div>

        <div v-if="mode === 'edit'" class="meta-form"><label>封面图片 URL<input v-model="coverUrl" placeholder="https://…（可选）" /></label><label>标签<input v-model="tagsInput" placeholder="轻松漫游, 美食, 人文" /></label></div>
        <div v-if="mode === 'edit'" class="editor-wrap"><RichNoteEditor v-model="content" placeholder="从抵达的那一刻开始，写下你的旅行……" /></div>
        <article v-else class="preview-content markdown-body" v-html="preview"></article>

        <footer class="composer-footer"><span>{{ savedLocally ? '已创建可迁移笔记' : '发布前会自动保存一份私有笔记' }}</span><button class="publish-main" :disabled="publishing" @click="publish">{{ publishing ? '保存中…' : '发布这篇笔记 →' }}</button></footer>
      </article>

      <aside class="publish-aside"><section class="aside-card"><span class="aside-kicker">PUBLISHING NOTES</span><h2>一份内容，三种去处</h2><p>你的笔记会保留在个人空间，也可以成为圈子里的帖子，之后再收藏回灵感目的地。</p><div class="flow"><div><b>01</b><span>个人笔记</span><small>随时继续编辑</small></div><i>→</i><div><b>02</b><span>我的圈子</span><small>公开交流</small></div><i>→</i><div><b>03</b><span>灵感目的地</span><small>留给下一次出发</small></div></div></section><section class="aside-card tips-card"><span class="aside-kicker">EDITOR SHORTCUTS</span><div><kbd>⌘ / Ctrl</kbd><span>+ S</span><small>保存当前内容</small></div><div><kbd>⌘ / Ctrl</kbd><span>+ B</span><small>加粗选中文字</small></div><div><kbd>拖入图片</kbd><small>直接生成旅行插图</small></div></section></aside>
    </section>
  </main>
</template>

<style scoped lang="scss">
.publish-page { min-height: 100%; padding: 30px clamp(18px, 4vw, 54px) 72px; background: radial-gradient(circle at 85% 2%, var(--sunset-soft), transparent 25rem); color: var(--ink); }.publish-head { width: min(1180px, 100%); margin: 0 auto 25px; display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; }.eyebrow, .cover-kicker, .aside-kicker { color: var(--sunset); font-size: 10px; font-weight: 900; letter-spacing: .18em; }.head-copy h1 { margin: 8px 0 7px; font: 38px/1.15 'DM Serif Display', 'Noto Sans SC', serif; }.head-copy p { color: var(--ink-2); font-size: 13px; }.head-actions { display: flex; gap: 8px; flex-wrap: wrap; }.quiet-btn, .preview-btn, .publish-btn { border: 1px solid var(--line); border-radius: 9px; padding: 10px 13px; background: var(--card); color: var(--ink-2); font-size: 12px; font-weight: 800; cursor: pointer; }.preview-btn.active { color: var(--forest); background: var(--roam-soft); border-color: var(--forest); }.publish-btn { border-color: var(--forest); background: var(--forest); color: #fff; }.publish-btn:disabled, .publish-main:disabled { opacity: .55; cursor: wait; }.publish-layout { width: min(1180px, 100%); margin: 0 auto; display: grid; grid-template-columns: minmax(0, 1fr) 285px; gap: 22px; align-items: start; }.composer-card, .aside-card { border: 1px solid var(--line); border-radius: 20px; background: var(--card); box-shadow: var(--shadow-soft); overflow: hidden; }.cover-editor { position: relative; min-height: 255px; background-size: cover; background-position: center; }.cover-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(12,40,32,.82), rgba(12,40,32,.08) 75%); }.cover-content { position: absolute; inset: auto 28px 25px; color: #fff; }.cover-kicker { color: rgba(255,255,255,.72); }.title-input { display: block; width: 100%; margin: 9px 0 13px; border: 0; border-bottom: 1px solid rgba(255,255,255,.45); outline: 0; background: transparent; color: #fff; font: 36px/1.18 'DM Serif Display', 'Noto Sans SC', serif; }.title-input::placeholder { color: rgba(255,255,255,.65); }.cover-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 11px; }.cover-meta > span { padding: 5px 9px; border-radius: 999px; background: rgba(255,255,255,.16); backdrop-filter: blur(5px); }.cover-meta input { width: 125px; border: 0; outline: 0; background: transparent; color: #fff; font-size: 11px; }.cover-meta input::placeholder { color: rgba(255,255,255,.7); }.meta-form { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 17px 22px 0; }.meta-form label { display: grid; gap: 6px; color: var(--ink-2); font-size: 11px; font-weight: 800; }.meta-form input { height: 35px; border: 1px solid var(--line); border-radius: 8px; padding: 0 10px; background: var(--paper); color: var(--ink); outline: 0; }.meta-form input:focus { border-color: var(--forest); }.editor-wrap { padding: 18px 22px 0; }.editor-wrap :deep(.rich-content) { min-height: 420px; }.preview-content { padding: 34px 48px 42px; min-height: 420px; color: var(--ink); font-size: 16px; line-height: 1.9; }.preview-content :deep(h1), .preview-content :deep(h2), .preview-content :deep(h3) { color: var(--ink); margin: 1.2em 0 .55em; }.preview-content :deep(strong) { color: var(--forest); }.preview-content :deep(img) { max-width: 100%; border-radius: 14px; }.composer-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 14px 22px; border-top: 1px solid var(--line); color: var(--ink-3); font-size: 11px; }.publish-main { border: 0; border-radius: 9px; padding: 10px 14px; background: var(--sunset); color: #fff; font-size: 12px; font-weight: 800; cursor: pointer; }.publish-aside { display: grid; gap: 13px; }.aside-card { padding: 19px; }.aside-card h2 { margin: 8px 0; font-size: 18px; }.aside-card p { color: var(--ink-2); font-size: 12px; line-height: 1.7; }.flow { display: grid; grid-template-columns: 1fr auto 1fr auto 1fr; align-items: center; gap: 5px; margin-top: 19px; }.flow > div { display: grid; gap: 3px; }.flow b { color: var(--sunset); font-size: 10px; }.flow span { color: var(--ink); font-size: 11px; font-weight: 800; }.flow small { color: var(--ink-3); font-size: 9px; }.flow i { color: var(--forest); font-style: normal; }.tips-card { display: grid; gap: 12px; }.tips-card > div { display: grid; grid-template-columns: auto auto 1fr; align-items: center; gap: 5px; color: var(--ink-2); font-size: 11px; }.tips-card small { grid-column: 1 / -1; color: var(--ink-3); }.tips-card kbd { padding: 3px 6px; border: 1px solid var(--line); border-radius: 5px; background: var(--paper); color: var(--forest); font: 10px ui-monospace, monospace; }.empty-copy { color: var(--ink-3); }
@media (max-width: 850px) { .publish-head { align-items: flex-start; flex-direction: column; }.publish-layout { grid-template-columns: 1fr; }.publish-aside { grid-template-columns: 1fr 1fr; }.title-input { font-size: 31px; } }
@media (max-width: 580px) { .publish-page { padding: 22px 12px 55px; }.publish-aside, .meta-form { grid-template-columns: 1fr; }.cover-content { left: 18px; right: 18px; }.preview-content { padding: 25px 20px; }.composer-footer { align-items: flex-start; flex-direction: column; }.publish-main { width: 100%; } }
</style>
