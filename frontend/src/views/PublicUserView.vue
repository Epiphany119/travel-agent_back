<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createNoteRevision, getCurrentUserId, getPublicUserProfile, listPublicNotes, type PublicUserProfile, type SocialNote } from '@/api/user'
import { createNote } from '@/api/note'
import { useContentTabsStore } from '@/stores/contentTabs'

const route = useRoute()
const router = useRouter()
const tabs = useContentTabsStore()
const userId = computed(() => String(route.params.id || ''))
const isSelf = computed(() => String(profile.value?.user_id || profile.value?.public_id || userId.value) === getCurrentUserId())
const profile = ref<PublicUserProfile | null>(null)
const notes = ref<SocialNote[]>([])
const loading = ref(true)

function imageUrl(value?: string) {
  if (!value) return ''
  if (/^(https?:|data:|blob:)/i.test(value)) return value
  return `${String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')}${value.startsWith('/') ? value : `/${value}`}`
}
function firstImage(content?: string) {
  return content?.match(/<img[^>]+src=["']([^"']+)["']/i)?.[1] || content?.match(/!\[[^\]]*\]\(([^)\s]+)[^)]*\)/i)?.[1] || ''
}
function cardImage(note: SocialNote) { return imageUrl(note.cover_url || note.coverUrl || firstImage(note.content)) }
function authorName(note: SocialNote) { return note.author || note.authorName || note.author_name || '旅行者' }
function openNote(note: SocialNote) {
  tabs.open({ kind: 'explore-note', title: `发现灵感-${note.destination || note.title.slice(0, 6)}`, data: { keyId: note.id, id: note.id, socialNoteId: note.id, sourceType: 'social', title: note.title, content: note.content, image: cardImage(note), destination: note.destination, tags: note.tags, userId: note.user_id, author: authorName(note), authorAvatar: note.author_avatar || note.authorAvatar } })
  router.push({ path: '/card-detail', query: { noteId: String(note.id) } })
}
async function copyNote(note: SocialNote) {
  if (!note.id) return
  try {
    const copied = await createNote({ title: `${note.title} · 副本`, content: note.content, destination: note.destination, coverUrl: note.cover_url || note.coverUrl || '', visibility: 'private', sourceSocialNoteId: note.id })
    await createNoteRevision(note.id, { sourceType: 'copy', privateNoteId: copied.id, title: note.title, content: note.content, coverUrl: note.cover_url || note.coverUrl || '', destination: note.destination || '', tags: Array.isArray(note.tags) ? note.tags : [] })
    ElMessage.success('已复制到我的笔记，发布时会进行版权检测')
  } catch (error: any) { ElMessage.error(error?.message || '复制失败') }
}
onMounted(async () => {
  if (!userId.value) return
  loading.value = true
  try {
    const [profileResult, notesResult] = await Promise.all([getPublicUserProfile(userId.value), listPublicNotes(0, 50, undefined, undefined, userId.value)])
    profile.value = profileResult.data || null
    notes.value = notesResult.data || []
  } catch (error: any) { ElMessage.error(error?.message || '用户主页加载失败') }
  finally { loading.value = false }
})
</script>

<template>
  <main class="public-user-page" v-loading="loading">
    <header class="public-user-head">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <div class="identity">
        <div class="public-avatar"><img v-if="imageUrl(profile?.avatar)" :src="imageUrl(profile?.avatar)" alt="" /><span v-else>{{ (profile?.nickname || userId || '旅').charAt(0) }}</span></div>
        <div><p class="eyebrow">ROAMLY CREATOR</p><h1>{{ profile?.nickname || '旅行者' }}</h1><p class="user-id">Roamly ID · {{ profile?.public_id || profile?.user_id || userId }}</p><p v-if="profile?.bio" class="bio">{{ profile.bio }}</p></div>
      </div>
      <div class="note-count"><b>{{ notes.length }}</b><span>公开笔记</span></div>
    </header>
    <section class="posts-head"><div><p class="eyebrow">PUBLIC NOTES</p><h2>这个用户走过的路</h2></div><button class="profile-settings" v-if="isSelf" @click="router.push('/profile')">编辑我的资料</button></section>
    <section class="post-grid">
      <article v-for="note in notes" :key="note.id" class="post-card" @click="openNote(note)">
        <div class="post-cover"><img v-if="cardImage(note)" :src="cardImage(note)" :alt="note.title" loading="lazy" /><span v-else>{{ note.destination || 'ROAMLY' }}</span></div>
        <div class="post-body"><div class="post-meta"><span>{{ authorName(note) }}</span><span v-if="note.destination">· {{ note.destination }}</span></div><h3>{{ note.title }}</h3><div class="post-actions"><button @click.stop="copyNote(note)">复制到我的笔记</button><span>查看详情 →</span></div></div>
      </article>
      <p v-if="!loading && !notes.length" class="empty">这个用户还没有公开笔记。</p>
    </section>
  </main>
</template>

<style scoped lang="scss">
.public-user-page { width: min(1180px, calc(100% - 48px)); margin: auto; padding: 28px 0 80px; color: var(--ink); }.public-user-head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 22px; padding: 24px; border: 1px solid var(--line); border-radius: 22px; background: var(--card); box-shadow: var(--shadow-soft); }.back-btn,.profile-settings { border: 1px solid var(--line); border-radius: 10px; padding: 9px 12px; background: transparent; color: var(--forest); font-weight: 800; cursor: pointer; }.identity { display:flex; align-items:center; gap:16px; }.public-avatar { width:76px; height:76px; display:grid; place-items:center; overflow:hidden; border-radius:24px; background:var(--roam-soft); color:var(--forest); font-size:28px; font-weight:900; }.public-avatar img { width:100%; height:100%; object-fit:cover; }.eyebrow { margin:0 0 7px; color:var(--sunset); font-size:10px; font-weight:900; letter-spacing:.18em; }.identity h1 { margin:0 0 5px; font:32px 'DM Serif Display','Noto Sans SC'; }.user-id,.bio { margin:0; color:var(--ink-2); font-size:12px; }.bio { margin-top:7px; }.note-count { display:grid; gap:4px; text-align:right; }.note-count b { color:var(--forest); font-size:25px; }.note-count span { color:var(--ink-3); font-size:11px; }.posts-head { display:flex; align-items:end; justify-content:space-between; margin:42px 0 17px; }.posts-head h2 { margin:0; font-size:24px; }.post-grid { columns:3 260px; column-gap:18px; }.post-card { break-inside:avoid; margin-bottom:18px; overflow:hidden; border:1px solid var(--line); border-radius:17px; background:var(--card); cursor:pointer; transition:.2s; }.post-card:hover { transform:translateY(-3px); box-shadow:var(--shadow-lift); }.post-cover { aspect-ratio:4/3; display:grid; place-items:center; overflow:hidden; background:linear-gradient(135deg,var(--forest),var(--roam)); color:#fff; font:24px 'DM Serif Display'; }.post-cover img { width:100%; height:100%; object-fit:cover; }.post-body { padding:14px; }.post-meta { color:var(--ink-2); font-size:11px; }.post-body h3 { margin:9px 0 14px; font-size:16px; line-height:1.45; }.post-actions { display:flex; justify-content:space-between; align-items:center; gap:8px; color:var(--forest); font-size:11px; }.post-actions button { border:0; padding:0; background:transparent; color:var(--sunset); font-weight:800; cursor:pointer; }.empty { column-span:all; padding:60px; color:var(--ink-3); text-align:center; }
@media (max-width:700px) { .public-user-page { width:calc(100% - 28px); }.public-user-head { grid-template-columns:1fr; }.note-count { text-align:left; }.posts-head { align-items:start; gap:12px; }.profile-settings { display:none; }.post-grid { columns:2 150px; gap:10px; } }
</style>
