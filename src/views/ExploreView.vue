<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useContentTabsStore } from '@/stores/contentTabs'
import { createNoteRevision, listPublicNotes } from '@/api/user'
import { createNote } from '@/api/note'
import { ElMessage } from 'element-plus'
const router = useRouter(); const query = ref(''); const active = ref('为你推荐'); const tabs = ['为你推荐','城市灵感','AI 路线','避坑经验']; const notes = ref<any[]>([]); const loading = ref(false)
// 点击卡片 → 打开浏览器式卡片标签（覆盖中间查看，底层页面不受影响）
const contentTabs = useContentTabsStore()
function openNote(note: any) {
  contentTabs.open({
    kind: 'explore-note',
    title: `发现灵感-${note.city || note.title.slice(0, 6)}`,
    data: {
      // 帖子标题不是唯一键，必须使用数据库帖子 ID，避免同标题或旧标签串到别的帖子。
      keyId: note.id,
      id: note.id,
      socialNoteId: note.id ? Number(note.id) : undefined,
      travelNoteId: note.travel_note_id || note.travelNoteId,
      sourceType: 'social',
      title: note.title,
      content: note.content || note.title,
      image: note.image || '',
      author: note.author || '',
      userId: note.user_id || note.userId || '',
      authorAvatar: note.author_avatar || note.authorAvatar || '',
      city: note.city || '',
      tags: note.tags || [],
      likes: note.likes || ''
    }
  })
  router.push({ path: '/card-detail', query: { noteId: String(note.id) } })
}
function openUserProfile(userId?: string) {
  if (userId) router.push(`/users/${encodeURIComponent(userId)}`)
}
async function saveFeedNote(note: any) {
  try {
    if (note.id) {
      const copied = await createNote({
        title: `${note.title || '旅行笔记'} · 副本`, content: note.content || '',
        destination: note.destination || note.city || '', coverUrl: note.cover_url || note.coverUrl || note.image || '',
        visibility: 'private', sourceSocialNoteId: Number(note.id)
      })
      await createNoteRevision(Number(note.id), {
        sourceType: 'copy', privateNoteId: copied.id, title: note.title, content: note.content || '',
        coverUrl: note.cover_url || note.coverUrl || note.image || '',
        destination: note.destination || note.city || '', tags: note.tags || []
      })
      ElMessage.success('已复制到我的笔记，发布时会进行版权检测')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败，请稍后重试')
  }
}
function normalizeImageUrl(value: unknown): string {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^(https?:|data:|blob:)/i.test(raw)) return raw
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
}
function extractFirstImage(content: unknown): string {
  const source = String(content || '')
  const html = source.match(/<img[^>]+src=["']([^"']+)["']/i)?.[1]
  if (html) return html
  const markdown = source.match(/!\[[^\]]*\]\(([^)\s]+)(?:\s+[^)]*)?\)/i)?.[1]
  return markdown || ''
}
function normalizeNote(note: any) {
  const tags = Array.isArray(note.tags) ? note.tags : typeof note.tags === 'string' ? (() => { try { return JSON.parse(note.tags) } catch { return note.tags.split(',').filter(Boolean) } })() : []
  const image = normalizeImageUrl(note.coverUrl || note.cover_url || extractFirstImage(note.content))
  return { ...note, author: note.author || note.authorName || note.author_name || '旅行者', city: note.destination || '', image, imageFailed: false, likes: note.likeCount ?? note.like_count ?? 0, tags }
}
const visibleNotes = computed(() => {
  const q = query.value.trim().toLowerCase()
  return notes.value.filter(note => {
    const text = [note.title, note.author, note.city, note.content, ...(note.tags || [])].join(' ').toLowerCase()
    const matchesQuery = !q || text.includes(q)
    const matchesTab = active.value === '为你推荐' || active.value === '城市灵感' || (active.value === 'AI 路线' && /路线|自驾|规划|行程/.test(text)) || (active.value === '避坑经验' && /避坑|建议|注意|经验/.test(text))
    return matchesQuery && matchesTab
  })
})
onMounted(async()=>{
  loading.value = true
  try { notes.value = ((await listPublicNotes()).data || []).map(normalizeNote) } catch { notes.value = [] }
  finally { loading.value = false }
})
</script>
<template>
 <main class="explore-page"><header class="explore-head"><div><p class="eyebrow">DISCOVER YOUR NEXT ROAM</p><h1>发现下一段旅程</h1><p class="sub">真实经验、可执行路线，以及可以直接交给 Roamly 的灵感。</p></div><div class="head-actions"><button class="profile-pill" @click="router.push('/profile')">我的主页 <span>→</span></button><button class="create-note-btn" @click="router.push('/publish')">＋ 发布笔记</button></div></header>
 <div class="search-bar"><span>⌕</span><input v-model="query" placeholder="搜索目的地、旅行笔记或路线"/><kbd>⌘ K</kbd></div>
 <nav class="feed-tabs"><button v-for="tab in tabs" :key="tab" :class="{active:active===tab}" @click="active=tab">{{tab}}</button></nav>
 <section class="ai-callout"><div class="ai-symbol">✦</div><div><span class="eyebrow">ROAMLY AI PLANNER</span><h2>不知道去哪玩？把一句想法变成完整行程。</h2><p>“8 月，想去海边，预算 5000，两个人，节奏别太赶。”</p></div><button @click="router.push('/')">开始规划 <span>→</span></button></section>
 <section class="section-head"><div><span class="eyebrow">TRAVEL NOTES</span><h2>今日热门旅行</h2></div><button class="text-btn">查看全部 →</button></section>
 <section class="feed-grid" v-loading="loading"><article v-for="note in visibleNotes" :key="note.id" class="note-card" @click="openNote(note)"><div class="note-cover"><img v-if="note.image && !note.imageFailed" :src="note.image" :alt="note.city" loading="lazy" @error="note.imageFailed = true"/><span v-else class="note-cover-placeholder">{{ note.city || 'ROAMLY' }}</span><button class="save" title="复制到我的笔记" @click.stop="saveFeedNote(note)">☆</button><div class="hover-open">查看并编辑 →</div></div><div class="note-body"><div class="note-author"><button class="author-link" @click.stop="openUserProfile(note.user_id || note.userId)"><span class="avatar"><img v-if="normalizeImageUrl(note.author_avatar || note.authorAvatar)" :src="normalizeImageUrl(note.author_avatar || note.authorAvatar)" alt="" /><span v-else>{{note.author.charAt(0)}}</span></span><span>{{note.author}}</span></button><span v-if="note.city" class="dot">·</span><span>{{note.city}}</span></div><h3>{{note.title}}</h3><div class="tag-row"><span v-for="tag in note.tags" :key="tag">{{tag}}</span></div><div class="note-meta"><span>♡ {{note.likes}}</span><span>◌ 可编辑笔记</span></div></div></article><p v-if="!loading && !visibleNotes.length" class="empty-feed">暂时还没有已发布的旅行笔记，发布第一篇吧。</p></section>
 <section class="creator-strip"><div><span class="eyebrow">MAKE IT YOURS</span><h2>你的旅行，值得被认真记录。</h2><p>生成一份计划，出发后变成一篇可分享的旅行笔记。</p></div><button @click="router.push({ path: '/chat', query: { mode: 'agent' } })">和 Agent 一起开始 <span>→</span></button></section></main>
</template>
<style scoped>
.explore-page{width:min(1180px,calc(100% - 48px));margin:auto;padding:38px 0 80px}.explore-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:28px}.eyebrow{color:var(--sunset);font-size:10px;font-weight:800;letter-spacing:.16em}h1{margin:8px 0;font:38px 'DM Serif Display','Noto Sans SC';color:var(--ink)}.sub{color:var(--ink-2);font-size:14px}.profile-pill,.text-btn{border:1px solid var(--line);background:var(--card);color:var(--ink);padding:10px 15px;border-radius:12px;font-weight:700;cursor:pointer}.search-bar{display:flex;align-items:center;gap:12px;padding:15px 18px;border:1px solid var(--line);border-radius:16px;background:var(--card);box-shadow:var(--shadow-soft)}.search-bar span{font-size:23px;color:var(--ink-3)}.search-bar input{flex:1;border:0;outline:0;background:transparent;font:14px inherit;color:var(--ink)}.search-bar kbd{color:var(--ink-3);font-size:11px;border:1px solid var(--line);padding:3px 7px;border-radius:6px}.feed-tabs{display:flex;gap:24px;border-bottom:1px solid var(--line);margin:22px 0 26px}.feed-tabs button{padding:0 0 12px;border:0;background:none;color:var(--ink-2);font-weight:700;cursor:pointer}.feed-tabs button.active{color:var(--forest);border-bottom:2px solid var(--sunset)}.ai-callout,.creator-strip{display:flex;align-items:center;gap:16px;padding:22px 24px;border:1px solid #f2c9b6;border-radius:18px;background:linear-gradient(110deg,#fff8f2,#f7fbf5);margin-bottom:38px}.ai-symbol{display:grid;place-items:center;width:44px;height:44px;border-radius:13px;background:var(--sunset);color:#fff;font-size:22px}.ai-callout h2,.creator-strip h2{margin:5px 0;font-size:19px}.ai-callout p,.creator-strip p{margin:0;color:var(--ink-2);font-size:12px}.ai-callout button,.creator-strip button{margin-left:auto;flex:none;border:0;border-radius:10px;padding:11px 15px;background:var(--forest);color:#fff;font-weight:800;cursor:pointer}.section-head{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:16px}.section-head h2{margin-top:5px;font-size:23px}.feed-grid{columns:3 260px;column-gap:18px}.note-card{break-inside:avoid;margin:0 0 18px;border:1px solid var(--line);border-radius:17px;overflow:hidden;background:var(--card);transition:transform .2s,box-shadow .2s;cursor:pointer}.note-card:hover{transform:translateY(-3px);box-shadow:var(--shadow-lift)}.note-cover{position:relative;aspect-ratio:4/3;overflow:hidden;background:var(--wash)}.note-cover img{width:100%;height:100%;object-fit:cover;display:block}.note-cover--fallback{display:grid;place-items:center;background:linear-gradient(135deg,var(--forest),var(--roam));color:#fff;font:24px 'DM Serif Display'}.note-cover--fallback .hover-open{position:absolute;left:0;right:0;bottom:0}.save{position:absolute;right:12px;top:10px;width:30px;height:30px;display:grid;place-items:center;border-radius:50%;background:#ffffffd9;color:var(--ink);font-size:20px;cursor:pointer}.hover-open{position:absolute;left:0;right:0;bottom:0;padding:18px 14px 12px;background:linear-gradient(to top,rgba(0,0,0,.55),transparent);color:#fff;font-size:13px;font-weight:700;opacity:0;transition:opacity .2s;text-align:center}.note-card:hover .hover-open{opacity:1}.note-body{padding:13px 14px 15px}.note-author,.note-meta{display:flex;align-items:center;gap:7px;color:var(--ink-2);font-size:11px}.avatar{display:grid;place-items:center;width:23px;height:23px;border-radius:50%;background:var(--roam-soft);color:var(--forest);font-weight:800}.dot{color:var(--ink-3)}.note-body h3{margin:10px 0;font-size:15px;line-height:1.45}.tag-row{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:12px}.tag-row span{padding:4px 8px;border-radius:6px;background:var(--roam-soft);color:var(--forest);font-size:10px;font-weight:700}.note-meta{justify-content:space-between;padding-top:10px;border-top:1px solid var(--line)}.creator-strip{margin-top:42px;margin-bottom:0}@media(max-width:700px){.explore-page{width:calc(100% - 28px);padding-top:24px}.explore-head{align-items:flex-start}.profile-pill{display:none}h1{font-size:31px}.ai-callout,.creator-strip{align-items:flex-start;flex-wrap:wrap}.ai-callout button,.creator-strip button{margin-left:60px}.feed-grid{columns:2 150px;column-gap:10px}.note-body{padding:10px}.note-body h3{font-size:13px}}
 </style>
<style scoped>
.empty-feed { column-span: all; padding: 48px 0; color: var(--ink-3); text-align: center; }
.search-bar:focus-within { border-color: var(--forest); box-shadow: 0 0 0 3px color-mix(in srgb, var(--forest) 12%, transparent), var(--shadow-soft); }
.feed-tabs button { transition: color .16s, transform .16s; }
.feed-tabs button:hover { color: var(--forest); transform: translateY(-1px); }
.note-card { box-shadow: 0 1px 0 color-mix(in srgb, var(--ink) 3%, transparent); }
.head-actions { display: flex; align-items: center; gap: 8px; }
.create-note-btn { border: 1px solid var(--forest); border-radius: 12px; padding: 10px 14px; background: var(--forest); color: #fff; font-weight: 800; cursor: pointer; transition: transform .16s, box-shadow .16s; }
.create-note-btn:hover { transform: translateY(-1px); box-shadow: var(--shadow-lift); }
.note-cover-placeholder { width: 100%; height: 100%; display: grid; place-items: center; padding: 20px; background: linear-gradient(135deg, var(--forest), var(--roam)); color: #fff; font: 20px 'DM Serif Display', serif; text-align: center; }
.save { border: 0; }
.author-link { display: inline-flex; align-items: center; gap: 7px; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; cursor: pointer; }
.author-link:hover { color: var(--forest); }
.author-link .avatar { overflow: hidden; }
.author-link .avatar img { width: 100%; height: 100%; object-fit: cover; }
</style>
