<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useContentTabsStore, type ContentTab } from '@/stores/contentTabs'
import {
  addComment,
  addInspiration,
  createNoteRevision,
  copyTravelNote,
  getCurrentUserId,
  getPublicNote,
  getTravelNote,
  listComments,
  listNoteRevisions,
  publishSocialNote,
  reportSocialNote,
  reactNote,
  reviewNoteRevision,
  saveTravelNote,
  submitNoteRevision,
  updateSocialNote,
  type SocialComment,
  type SocialNote
} from '@/api/user'
import { createNote, getNote, updateNote, type NoteDocument } from '@/api/note'
import { renderMarkdown, sanitizeRichHtml } from '@/utils/markdown'
import { contentFromDocument, contentToDocument, plainTextFromHtml, serializeNoteDocument, tagsFromValue } from '@/types/note'
import RichNoteEditor from '@/components/RichNoteEditor.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const tabs = useContentTabsStore()
const userStore = useUserStore()
const tab = computed<ContentTab | null>(() => tabs.activeTab)

const loading = ref(false)
const saving = ref(false)
const copying = ref(false)
const publishing = ref(false)
const editing = ref(false)
const commentsLoading = ref(false)
const commenting = ref(false)
const collaborationLoading = ref(false)
const collaborationRequested = ref(false)

const title = ref('')
const destination = ref('')
const coverUrl = ref('')
const content = ref('')
const tags = ref<string[]>([])
const tagInput = ref('')
const noteDocumentId = ref<number | null>(null)
const travelNoteId = ref<number | null>(null)
const socialNoteId = ref<number | null>(null)
const socialNote = ref<SocialNote | null>(null)
const comments = ref<SocialComment[]>([])
const revisions = ref<any[]>([])
const commentDraft = ref('')
const savedSnapshot = ref('')
const published = ref(false)

const dirty = computed(() => `${title.value}\n${destination.value}\n${coverUrl.value}\n${content.value}\n${tags.value.join(',')}` !== savedSnapshot.value)
const isSocial = computed(() => !!socialNoteId.value)
const ownsSocial = computed(() => {
  const owner = socialNote.value?.user_id || socialNote.value?.userId || tab.value?.data?.userId || tab.value?.data?.user_id
  return !!owner && String(owner) === getCurrentUserId()
})
const sourceLabel = computed(() => {
  if (isSocial.value) return ownsSocial.value ? 'MY CIRCLE NOTE' : 'COMMUNITY NOTE'
  if (tab.value?.kind === 'journey') return 'JOURNEY MEMORY'
  if (tab.value?.kind === 'inspiration' || tab.value?.kind === 'explore-spot') return 'INSPIRATION'
  if (tab.value?.data?.sourceType === 'agent') return 'AI TRAVEL PLAN'
  return 'TRAVEL NOTE'
})
const actionEditLabel = computed(() => {
  if (editing.value) return saving.value ? '保存中…' : '保存笔记'
  if (isSocial.value && !ownsSocial.value && myRevision.value?.status === 'approved') return '开始协作编辑'
  return isSocial.value && !ownsSocial.value ? '复制后编辑' : '编辑笔记'
})
const renderedContent = computed(() => sanitizeRichHtml(content.value || '<p class="empty-copy">还没有正文，开始写下这段旅程吧。</p>'))
const tagList = computed(() => tags.value.filter(Boolean))
const myRevision = computed(() => revisions.value.find((revision) => String(revision.contributor_id || revision.contributorId) === getCurrentUserId() && revision.source_type !== 'copy' && revision.sourceType !== 'copy' && ['requested', 'approved', 'submitted'].includes(String(revision.status))))
const reviewableRevisions = computed(() => revisions.value.filter((revision) => (revision.source_type || revision.sourceType) !== 'copy' && ['requested', 'approved', 'submitted'].includes(String(revision.status))))

function setTabData(values: Record<string, unknown>) {
  if (tab.value) Object.assign(tab.value.data, values)
}

function normalizeEditorContent(raw: unknown): string {
  const value = String(raw ?? '').trim()
  if (!value) return ''
  return /<[a-z][\s\S]*>/i.test(value) ? sanitizeRichHtml(value) : renderMarkdown(value)
}

function legacyTravelNoteContent(raw: unknown): string {
  const rich = contentFromDocument(raw, '')
  if (rich) return normalizeEditorContent(rich)
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw as any
    if (!parsed || typeof parsed !== 'object') return normalizeEditorContent(raw)
    const days = Array.isArray(parsed.days) ? parsed.days : []
    const markdown = [
      parsed.overview?.destination ? `# ${parsed.overview.destination} 旅行计划` : '',
      parsed.overview?.preferences ? `> ${parsed.overview.preferences}` : '',
      ...days.map((day: any) => {
        const items = (day.items || day.activities || []).map((item: any) => `- **${item.time || ''} ${item.name || ''}** ${item.location || ''} ${item.notes || ''}`.trim()).join('\n')
        return `## 第 ${day.day || ''} 天${day.theme ? ` · ${day.theme}` : ''}\n${items}`
      })
    ].filter(Boolean).join('\n\n')
    return normalizeEditorContent(markdown)
  } catch {
    return normalizeEditorContent(raw)
  }
}

function hydrateFromTab() {
  const data = tab.value?.data || {}
  title.value = data.title || data.name || tab.value?.title || '未命名笔记'
  destination.value = data.destination || data.city || ''
  coverUrl.value = data.coverUrl || data.cover_url || data.imageUrl || data.image || ''
  tags.value = tagsFromValue(data.tags || (data.bestSeason ? [data.bestSeason] : []))
  tagInput.value = tags.value.join(', ')
  content.value = normalizeEditorContent(data.content || data.description || data.quote || '')
  noteDocumentId.value = Number(data.noteDocumentId || data.documentId || 0) || null
  travelNoteId.value = Number(data.travelNoteId || data.travel_note_id || 0) || null
  socialNoteId.value = Number(data.socialNoteId || (tab.value?.kind === 'explore-note' ? data.id : 0)) || null
  socialNote.value = null
  comments.value = []
  revisions.value = []
  published.value = false
  savedSnapshot.value = `${title.value}\n${destination.value}\n${coverUrl.value}\n${content.value}\n${tags.value.join(',')}`
}

function applySocial(note: SocialNote) {
  socialNote.value = note
  socialNoteId.value = Number(note.id) || socialNoteId.value
  title.value = note.title || title.value
  destination.value = note.destination || destination.value
  coverUrl.value = note.coverUrl || note.cover_url || coverUrl.value
  tags.value = tagsFromValue(note.tags)
  tagInput.value = tags.value.join(', ')
  content.value = normalizeEditorContent(note.content || content.value)
  editing.value = ownsSocial.value
  setTabData({ socialNoteId: socialNoteId.value, id: note.id, title: title.value, content: note.content, destination: destination.value, image: coverUrl.value, tags: tags.value })
}

async function loadComments() {
  if (!socialNoteId.value) return
  commentsLoading.value = true
  try { comments.value = (await listComments(socialNoteId.value)).data || [] } catch { comments.value = [] }
  finally { commentsLoading.value = false }
}

async function loadRevisions() {
  if (!socialNoteId.value) return
  try { revisions.value = (await listNoteRevisions(socialNoteId.value)).data || [] } catch { revisions.value = [] }
}

async function hydrate() {
  if (!tab.value) return
  hydrateFromTab()
  loading.value = true
  try {
    if (socialNoteId.value) {
      const result = await getPublicNote(socialNoteId.value)
      if (result.data) applySocial(result.data)
      void Promise.all([loadComments(), loadRevisions()])
    } else if (noteDocumentId.value) {
      const note = await getNote(noteDocumentId.value)
      title.value = note.title || title.value
      destination.value = note.destination || destination.value
      coverUrl.value = note.coverUrl || coverUrl.value
      content.value = normalizeEditorContent(note.content || content.value)
      setTabData({ title: title.value, content: note.content, destination: destination.value, image: coverUrl.value, noteDocumentId: note.id })
    } else if (travelNoteId.value) {
      const note: any = (await getTravelNote(travelNoteId.value)).data
      if (note) {
        title.value = note.title || title.value
        destination.value = note.destination || destination.value
        coverUrl.value = note.coverUrl || coverUrl.value
        content.value = legacyTravelNoteContent(note.contentJson)
        setTabData({ title: title.value, content: content.value, destination: destination.value, image: coverUrl.value, travelNoteId: note.id })
      }
    }
  } catch (error) {
    console.warn('[CardDetail] hydrate failed', error)
  } finally {
    loading.value = false
    savedSnapshot.value = `${title.value}\n${destination.value}\n${coverUrl.value}\n${content.value}\n${tags.value.join(',')}`
  }
}

watch(() => tab.value?.id, () => {
  if (!tab.value) return
  hydrate()
  // 社区帖子打开即进入可编辑状态；其他卡片先保持阅读，减少误触。
  editing.value = false
}, { immediate: true })

// 即使会话存储被清空，直接刷新 /card-detail?noteId=123 仍能从数据库恢复帖子。
watch(() => route.query.noteId, (value) => {
  const id = Number(value)
  if (!id || tab.value) return
  tabs.open({
    kind: 'explore-note',
    title: '社区笔记',
    data: { keyId: id, id, socialNoteId: id, sourceType: 'social' }
  })
}, { immediate: true })

function syncTags(value: string) {
  tagInput.value = value
  tags.value = tagsFromValue(value)
}

function snapshot() {
  savedSnapshot.value = `${title.value}\n${destination.value}\n${coverUrl.value}\n${content.value}\n${tags.value.join(',')}`
}

async function saveNote(showMessage = true) {
  if (!title.value.trim()) { ElMessage.warning('请先填写标题'); return false }
  if (!content.value.trim()) { ElMessage.warning('正文不能为空'); return false }
  saving.value = true
  try {
    const unified = contentToDocument(content.value, { title: title.value, destination: destination.value, coverUrl: coverUrl.value, origin: isSocial.value ? 'community' : 'manual' })
    if (socialNoteId.value && !ownsSocial.value && myRevision.value?.id) {
      await submitNoteRevision(myRevision.value.id, { title: title.value, content: content.value, coverUrl: coverUrl.value, destination: destination.value, tags: tagList.value })
      await loadRevisions()
      setTabData({ title: title.value, content: content.value, destination: destination.value, image: coverUrl.value, coverUrl: coverUrl.value, tags: tagList.value })
      snapshot()
      editing.value = false
      if (showMessage) ElMessage.success('协作版本已提交，等待原作者审核')
      return true
    }
    if (travelNoteId.value) {
      const current: any = (await getTravelNote(travelNoteId.value)).data
      await saveTravelNote({ ...(current || {}), id: travelNoteId.value, title: title.value, destination: destination.value, coverUrl: coverUrl.value, contentJson: serializeNoteDocument(unified), status: current?.status || 'draft' } as any)
    }

    if (socialNoteId.value && ownsSocial.value) {
      const updated = await updateSocialNote(socialNoteId.value, { title: title.value, content: content.value, coverUrl: coverUrl.value, destination: destination.value, tags: tagList.value, authorName: userStore.nickname || '旅行者', authorAvatar: userStore.avatar || '' })
      if (updated.data) socialNote.value = updated.data
    }

    if (noteDocumentId.value) {
      const saved = await updateNote(noteDocumentId.value, { title: title.value, content: content.value, destination: destination.value, coverUrl: coverUrl.value, visibility: 'private' } as NoteDocument)
      noteDocumentId.value = saved.id || noteDocumentId.value
    } else if (!socialNoteId.value || !ownsSocial.value) {
      // 社区他人帖子/灵感卡片的“编辑”会生成自己的私有笔记，不覆盖原作者内容。
      const created = await createNote({ title: title.value, content: content.value, destination: destination.value, coverUrl: coverUrl.value, visibility: 'private' })
      noteDocumentId.value = created.id || null
    }

    setTabData({ title: title.value, content: content.value, destination: destination.value, image: coverUrl.value, coverUrl: coverUrl.value, tags: tagList.value, noteDocumentId: noteDocumentId.value })
    snapshot()
    editing.value = false
    if (showMessage) ElMessage.success(socialNoteId.value && !ownsSocial.value ? '已保存为我的笔记' : '笔记已保存')
    return true
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败，请稍后重试')
    return false
  } finally { saving.value = false }
}

async function toggleEdit() {
  if (editing.value) { await saveNote(); return }
  if (isSocial.value && !ownsSocial.value && myRevision.value?.status !== 'approved') { await copyToNotes(); return }
  editing.value = true
}

async function publishToCircle() {
  if (isSocial.value && !ownsSocial.value) return ElMessage.warning('这是他人的帖子，请复制或申请协作后再编辑')
  if (!title.value.trim() || !content.value.trim()) return ElMessage.warning('标题和正文都不能为空')
  publishing.value = true
  try {
    if (dirty.value || !noteDocumentId.value) {
      const saved = await saveNote(false)
      if (!saved) return
    }
    if (socialNoteId.value && ownsSocial.value) {
      const updated = await updateSocialNote(socialNoteId.value, { title: title.value, content: content.value, coverUrl: coverUrl.value, destination: destination.value, tags: tagList.value, authorName: userStore.nickname || '旅行者', authorAvatar: userStore.avatar || '' })
      if (updated.data) socialNote.value = updated.data
    } else {
      const response = await publishSocialNote({ userId: getCurrentUserId(), title: title.value, content: content.value, coverUrl: coverUrl.value, destination: destination.value, tags: tagList.value, authorName: userStore.nickname || '旅行者', authorAvatar: userStore.avatar || '' })
      const result = response.data
      const id = Number(result?.id || 0)
      published.value = result?.published === true
      if (published.value && id) { socialNoteId.value = id; setTabData({ socialNoteId: id }) }
      if (!published.value) {
        ElMessage.warning(result?.reviewRequired ? (result.message || '已提交平台人工审核') : (result?.message || 'Agent 版权检测未通过，暂不允许发布'))
        return
      }
    }
    published.value = true
    ElMessage.success('已发布到我的圈子')
    await loadComments()
  } catch (error: any) { ElMessage.error(error?.message || '发布失败，请稍后重试') }
  finally { publishing.value = false }
}

async function requestCollaboration() {
  if (!socialNoteId.value || ownsSocial.value || collaborationLoading.value || myRevision.value) return
  collaborationLoading.value = true
  try {
    await createNoteRevision(socialNoteId.value, { sourceType: 'invite', message: `希望协作完善《${title.value}》` })
    collaborationRequested.value = true
    ElMessage.success('协作申请已发送，等待创作者审核')
  } catch (error: any) { ElMessage.error(error?.message || '协作申请发送失败') }
  finally { collaborationLoading.value = false }
}

async function reviewRevision(revision: any, status: 'approved'|'rejected'|'merged') {
  try {
    await reviewNoteRevision(Number(revision.id), status, status === 'merged' ? '已合并到公开帖子' : '')
    await loadRevisions()
    if (status === 'merged') await hydrate()
    ElMessage.success(status === 'merged' ? '版本已合并并更新公开帖' : status === 'approved' ? '已开放协作编辑权限' : '已拒绝该版本')
  } catch (error: any) { ElMessage.error(error?.message || '协作审核失败') }
}

async function copyToNotes() {
  if (copying.value) return
  copying.value = true
  try {
    let copied: any = null
    if (socialNoteId.value) {
      copied = await createNote({ title: `${title.value} · 副本`, content: content.value, destination: destination.value, coverUrl: coverUrl.value, visibility: 'private', sourceSocialNoteId: socialNoteId.value })
      await createNoteRevision(socialNoteId.value, { sourceType: 'copy', privateNoteId: copied.id, title: title.value, content: content.value, coverUrl: coverUrl.value, destination: destination.value, tags: tagList.value })
    }
    else if (travelNoteId.value) copied = (await copyTravelNote(travelNoteId.value)).data
    else copied = (await createNote({ title: `${title.value} · 副本`, content: content.value, destination: destination.value, coverUrl: coverUrl.value, visibility: 'private' }))
    ElMessage.success(socialNoteId.value ? '已复制到我的笔记，发布时会进行版权检测' : '已复制到我的笔记')
    if (copied?.id) setTabData({ noteDocumentId: copied.id })
    router.push('/notes')
  } catch (error: any) { ElMessage.error(error?.message || '复制失败') }
  finally { copying.value = false }
}

async function reportNote() {
  if (!socialNoteId.value || ownsSocial.value) return
  try {
    const prompt = await ElMessageBox.prompt('请说明你与该帖子的原创关系或侵权依据', '举报疑似侵权', {
      inputPlaceholder: '例如：这是我发布的原帖，内容被完整复制',
      confirmButtonText: '提交举报',
      cancelButtonText: '取消',
      inputValidator: (value: string) => value.trim().length >= 4 ? true : '请至少填写 4 个字'
    })
    const sourceId = Number(socialNote.value?.source_note_id || socialNote.value?.sourceNoteId || 0) || undefined
    const result = await reportSocialNote(socialNoteId.value, { sourceNoteId: sourceId, reason: prompt.value })
    ElMessage.success(result.data?.message || '举报已提交，等待 Agent 识别')
  } catch (error: any) {
    if (error?.response) ElMessage.error(error.response.data?.message || '举报失败，请稍后重试')
  }
}

async function saveAsInspiration() {
  try {
    const text = plainTextFromHtml(content.value)
    await addInspiration({ userId: getCurrentUserId(), name: destination.value || title.value, imageUrl: coverUrl.value, quote: text.slice(0, 120), description: text, tags: tagList.value.join(','), status: 0, priority: 1 })
    ElMessage.success('已保存到灵感目的地')
  } catch (error: any) { ElMessage.error(error?.message || '保存灵感失败') }
}

async function react(type: 'like' | 'favorite') {
  if (!socialNoteId.value) return ElMessage.info('发布到圈子后即可互动')
  try { await reactNote(socialNoteId.value, type); ElMessage.success(type === 'like' ? '已点赞' : '已收藏') }
  catch (error: any) { ElMessage.error(error?.message || '操作失败') }
}

async function sendComment() {
  if (!socialNoteId.value || !commentDraft.value.trim() || commenting.value) return
  commenting.value = true
  try { await addComment(socialNoteId.value, commentDraft.value.trim()); commentDraft.value = ''; await loadComments(); ElMessage.success('评论已发送') }
  catch (error: any) { ElMessage.error(error?.message || '评论发送失败') }
  finally { commenting.value = false }
}

async function goBack() {
  if (dirty.value) {
    try { await ElMessageBox.confirm('还有未保存的修改，确定离开吗？', '离开编辑', { confirmButtonText: '离开', cancelButtonText: '继续编辑', type: 'warning' }) }
    catch { return }
  }
  if (tab.value) tabs.close(tab.value.id)
  router.push(tabs.lastRoute || '/notes')
}

function formatTime(value?: string) {
  if (!value) return '刚刚'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '刚刚' : date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
function imageUrl(value?: string) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^(https?:|data:|blob:)/i.test(raw)) return raw
  const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
}
function commentUser(comment: SocialComment) { return comment.nickname || '旅行者' }
function commentAvatar(comment: SocialComment) { return imageUrl(comment.avatar) }
function openUserProfile(userId?: string) {
  if (userId) router.push(`/users/${encodeURIComponent(userId)}`)
}
function toggleRevisionPreview(revision: any) {
  revision.previewOpen = !revision.previewOpen
}
</script>

<template>
  <section class="card-detail-page">
    <header class="detail-topbar">
      <div class="topbar-left">
        <button class="back-btn" @click="goBack">← <span>返回</span></button>
        <span class="crumb-divider">/</span><span class="crumb">{{ sourceLabel }}</span>
        <span v-if="dirty" class="dirty-dot" title="有未保存修改"></span>
      </div>
      <div class="topbar-actions" v-if="tab">
        <button class="quiet-btn" :disabled="copying" @click="copyToNotes">{{ copying ? '复制中…' : '⌘ 复制到笔记' }}</button>
        <button v-if="isSocial && !ownsSocial" class="quiet-btn report-btn" @click="reportNote">举报侵权</button>
        <button class="quiet-btn" @click="saveAsInspiration">♡ 存灵感</button>
        <button class="edit-btn" :class="{ active: editing }" :disabled="saving || copying" @click="toggleEdit">{{ actionEditLabel }}</button>
        <button v-if="isSocial && !ownsSocial && myRevision?.status !== 'approved' && myRevision?.status !== 'submitted'" class="quiet-btn" :disabled="collaborationLoading || collaborationRequested" @click="requestCollaboration">{{ collaborationRequested || myRevision?.status === 'requested' ? '已申请协作' : collaborationLoading ? '申请中…' : '申请协作' }}</button>
        <button v-if="!isSocial || ownsSocial" class="publish-btn" :disabled="publishing || saving" @click="publishToCircle">{{ publishing ? '发布中…' : published ? '再次发布' : '发布到圈子' }}</button>
      </div>
    </header>

    <div v-if="loading" class="detail-skeleton"><div class="skeleton-line short"></div><div class="skeleton-line title"></div><div class="skeleton-cover"></div><div class="skeleton-line"></div><div class="skeleton-line"></div></div>

    <div v-else-if="tab" class="detail-grid">
      <article class="story-column">
        <header class="story-head">
          <span class="eyebrow">{{ sourceLabel }}</span>
          <input v-if="editing" v-model="title" class="story-title-input" placeholder="给这段旅程起个标题" @input="published = false" />
          <h1 v-else>{{ title }}</h1>
          <div class="story-meta"><span v-if="destination">⌖ {{ destination }}</span><span v-if="socialNote?.created_at">· {{ formatTime(socialNote.created_at) }}</span><span v-if="socialNote" class="status-pill"><i></i>{{ ownsSocial ? '我的圈子' : '社区公开' }}</span><button v-if="socialNote" class="creator-link" @click="openUserProfile(socialNote.user_id || socialNote.userId)"><span class="mini-avatar"><img v-if="imageUrl(socialNote.author_avatar || socialNote.authorAvatar)" :src="imageUrl(socialNote.author_avatar || socialNote.authorAvatar)" alt="" /><span v-else>{{ (socialNote.author || '旅').charAt(0) }}</span></span>{{ socialNote.author || '旅行者' }}</button></div>
        </header>

        <div v-if="coverUrl" class="story-cover"><img :src="coverUrl" :alt="title" loading="eager" /><span class="cover-caption">TRAVEL, THOUGHTFULLY</span></div>

        <div v-if="editing" class="edit-meta-grid">
          <label>目的地<input v-model="destination" placeholder="例如：杭州" @input="published = false" /></label>
          <label>封面 URL<input v-model="coverUrl" placeholder="https://…（可选）" @input="published = false" /></label>
          <label class="wide">标签（逗号分隔）<input :value="tagInput" placeholder="轻松漫游, 美食, 人文" @input="syncTags(($event.target as HTMLInputElement).value)" /></label>
        </div>

        <div v-if="editing" class="editor-shell" @keydown.meta.enter.prevent="saveNote()" @keydown.ctrl.enter.prevent="saveNote()"><RichNoteEditor v-model="content" placeholder="把这次旅行写成一篇值得保存的笔记……" /></div>
        <article v-else class="rendered-note markdown-body" v-html="renderedContent"></article>
        <div v-if="tagList.length" class="story-tags"><span v-for="tag in tagList" :key="tag">#{{ tag }}</span></div>

        <footer class="story-footer"><div class="footer-hint">{{ editing ? '支持图片、彩色文字、字号和提示卡片' : '这是一份可以继续编辑、复制和分享的旅行笔记' }}</div><div class="footer-actions"><button v-if="editing" class="cancel-btn" @click="hydrateFromTab(); editing = false">取消</button><button v-if="editing" class="save-main-btn" :disabled="saving" @click="saveNote()">{{ saving ? '保存中…' : '保存修改' }}</button><template v-else><button class="reaction-btn" @click="react('like')">♡ {{ socialNote?.like_count ?? socialNote?.likeCount ?? 0 }}</button><button class="reaction-btn" @click="react('favorite')">☆ 收藏</button></template></div></footer>
      </article>

      <aside class="social-column">
        <section class="side-card creator-card"><div class="side-card-kicker">ONE NOTE, MANY WAYS</div><h3>{{ isSocial ? '把别人的灵感，变成你的路线' : '把计划变成可分享的故事' }}</h3><p>{{ isSocial ? '复制后会生成你的私有版本，可以继续编辑，再发布到自己的圈子。' : '保存后可以发布到圈子，也能在 Agent 和灵感目的地之间继续迁移。' }}</p><div class="migration-flow"><span>笔记</span><i>→</i><span>圈子</span><i>→</i><span>灵感</span></div><button class="side-primary" @click="copyToNotes">{{ copying ? '复制中…' : '复制成我的笔记' }} <span>→</span></button></section>

        <section v-if="isSocial" class="side-card comments-card"><div class="comments-head"><div><span class="side-card-kicker">COMMUNITY</span><h3>大家在聊 <small>{{ comments.length }}</small></h3></div><span class="live-mark"><i></i> LIVE</span></div><div v-if="commentsLoading" class="comments-loading">正在加载评论…</div><div v-else-if="!comments.length" class="comments-empty">成为第一个分享想法的人。</div><div v-else class="comment-list"><article v-for="comment in comments" :key="comment.id" class="comment-item"><button class="comment-avatar" @click="openUserProfile(comment.user_id)"><img v-if="commentAvatar(comment)" :src="commentAvatar(comment)" alt="" /><span v-else>{{ commentUser(comment).charAt(0) }}</span></button><div><button class="comment-user" @click="openUserProfile(comment.user_id)">{{ commentUser(comment) }}</button><p>{{ comment.content }}</p><time>{{ formatTime(comment.created_at) }}</time></div></article></div><div class="comment-composer"><input v-model="commentDraft" placeholder="写下你的想法…" maxlength="1000" @keyup.enter="sendComment" /><button :disabled="commenting || !commentDraft.trim()" @click="sendComment">{{ commenting ? '…' : '↗' }}</button></div></section>

        <section v-if="isSocial && ownsSocial && reviewableRevisions.length" class="side-card pr-card"><span class="side-card-kicker">COLLABORATION PR</span><h3>待处理的协作版本</h3><article v-for="revision in reviewableRevisions" :key="revision.id" class="pr-item"><div class="pr-heading"><div class="pr-contributor"><span class="mini-avatar"><img v-if="imageUrl(revision.contributor_avatar || revision.contributorAvatar)" :src="imageUrl(revision.contributor_avatar || revision.contributorAvatar)" alt="" /><span v-else>{{ (revision.contributor_name || '协').charAt(0) }}</span></span><b>{{ revision.contributor_name || revision.contributorName || '协作者' }}</b></div><small>{{ revision.status }} · v{{ revision.revision_no }} · {{ revision.revision_code }}</small></div><div class="pr-actions"><button @click="toggleRevisionPreview(revision)">{{ revision.previewOpen ? '收起预览' : '预览效果' }}</button><button v-if="revision.status === 'requested'" @click="reviewRevision(revision, 'approved')">开放编辑</button><button v-if="revision.status === 'submitted'" @click="reviewRevision(revision, 'rejected')">拒绝</button><button v-if="revision.status === 'submitted'" class="merge" @click="reviewRevision(revision, 'merged')">合并发布</button></div><div v-if="revision.previewOpen" class="pr-preview markdown-body" v-html="sanitizeRichHtml(revision.content || '<p>暂无正文</p>')"></div></article></section>

        <section v-else class="side-card shortcut-card"><span class="side-card-kicker">QUICK ACTIONS</span><button @click="saveAsInspiration"><span>✦</span><div><b>保存到灵感目的地</b><small>下次规划时快速找回</small></div><em>→</em></button><button @click="publishToCircle"><span>◌</span><div><b>发布到我的圈子</b><small>邀请同好一起完善路线</small></div><em>→</em></button></section>
      </aside>
    </div>

    <div v-else class="empty-state"><div class="empty-icon">✦</div><h3>没有正在预览的卡片</h3><p>从发现灵感、我的旅程或灵感目的地打开一篇笔记。</p></div>
  </section>
</template>

<style scoped lang="scss">
.card-detail-page { width: 100%; height: 100%; min-height: 0; overflow: auto; background: var(--paper, #f7f3ea); color: var(--ink, #1d2b27); }
.creator-link { display: inline-flex; align-items: center; gap: 6px; border: 0; padding: 0; background: transparent; color: var(--forest); font: inherit; cursor: pointer; }
.pr-card h3 { margin-bottom: 13px; }.pr-item { display:grid; gap:9px; padding:11px 0; border-top:1px solid var(--line); }.pr-heading { display:flex; justify-content:space-between; align-items:center; gap:8px; }.pr-contributor { display:flex; align-items:center; gap:7px; min-width:0; }.pr-contributor b { overflow:hidden; color:var(--ink); font-size:11px; text-overflow:ellipsis; white-space:nowrap; }.pr-heading small { color:var(--ink-3); font-size:9px; white-space:nowrap; }.pr-actions { display:flex; gap:6px; flex-wrap:wrap; }.pr-actions button { border:1px solid var(--line); border-radius:7px; padding:6px 8px; background:transparent; color:var(--forest); font-size:10px; cursor:pointer; }.pr-actions button.merge { border-color:var(--forest); background:var(--forest); color:#fff; }.pr-preview { max-height:220px; overflow:auto; padding:10px 11px; border:1px solid var(--line); border-radius:9px; background:var(--paper); color:var(--ink-2); font-size:11px; line-height:1.65; }.pr-preview :deep(p) { margin:0 0 7px; }.pr-preview :deep(img) { max-width:100%; border-radius:7px; }
.creator-link:hover, .comment-user:hover { text-decoration: underline; }
.mini-avatar { width: 22px; height: 22px; display: grid; place-items: center; overflow: hidden; border-radius: 50%; background: var(--roam-soft); color: var(--forest); font-size: 10px; font-weight: 900; }
.mini-avatar img, .comment-avatar img { width: 100%; height: 100%; object-fit: cover; }
.comment-avatar { padding: 0; border: 0; cursor: pointer; overflow: hidden; }
.comment-user { padding: 0; border: 0; background: transparent; color: var(--forest); font-size: 11px; font-weight: 800; cursor: pointer; }
.detail-topbar { position: sticky; top: 0; z-index: 5; display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 58px; padding: 10px clamp(16px, 3vw, 34px); border-bottom: 1px solid var(--line); background: color-mix(in srgb, var(--paper) 90%, transparent); backdrop-filter: blur(16px); }
.topbar-left, .topbar-actions, .footer-actions, .story-meta, .migration-flow, .comments-head { display: flex; align-items: center; }.topbar-left { gap: 10px; min-width: 0; }.back-btn, .quiet-btn, .edit-btn, .publish-btn, .cancel-btn, .save-main-btn, .reaction-btn { border: 1px solid var(--line); border-radius: 9px; cursor: pointer; font-weight: 700; transition: .18s ease; }.back-btn { border: 0; padding: 7px 10px; background: var(--roam-soft); color: var(--forest); }.back-btn:hover, .quiet-btn:hover, .reaction-btn:hover { transform: translateY(-1px); border-color: var(--forest); }.crumb-divider { color: var(--ink-3); }.crumb { color: var(--ink-2); font-size: 11px; letter-spacing: .12em; font-weight: 800; white-space: nowrap; }.dirty-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--sunset); box-shadow: 0 0 0 4px var(--sunset-soft); }.topbar-actions { gap: 7px; flex-wrap: wrap; justify-content: flex-end; }.quiet-btn { padding: 8px 10px; background: transparent; color: var(--ink-2); font-size: 11px; }.report-btn { color: var(--sunset); border-color: color-mix(in srgb, var(--sunset) 36%, var(--line)); }.edit-btn { padding: 8px 12px; background: var(--card); color: var(--forest); font-size: 11px; }.edit-btn.active { background: var(--roam-soft); }.publish-btn { padding: 9px 13px; background: var(--sunset); color: #fff; border-color: var(--sunset); font-size: 11px; }.publish-btn:hover:not(:disabled), .save-main-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 7px 16px color-mix(in srgb, var(--sunset) 22%, transparent); }.publish-btn:disabled, .edit-btn:disabled, .save-main-btn:disabled { opacity: .55; cursor: wait; }
.detail-grid { width: min(1240px, calc(100% - 48px)); margin: 0 auto; padding: 34px 0 76px; display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 28px; align-items: start; }.story-column { min-width: 0; }.story-head { padding: 14px 0 18px; }.eyebrow, .side-card-kicker { color: var(--sunset); font-size: 10px; font-weight: 900; letter-spacing: .18em; }.story-head h1, .story-title-input { margin: 9px 0 11px; font: 42px/1.12 'DM Serif Display', 'Noto Sans SC', serif; letter-spacing: -.025em; color: var(--ink); }.story-title-input { width: 100%; border: 0; border-bottom: 1px dashed var(--line); outline: 0; background: transparent; padding: 0 0 8px; }.story-title-input:focus { border-color: var(--forest); }.story-meta { gap: 11px; color: var(--ink-2); font-size: 12px; flex-wrap: wrap; }.status-pill { display: inline-flex; align-items: center; gap: 5px; padding: 4px 8px; border-radius: 999px; background: var(--roam-soft); color: var(--forest); font-size: 10px; font-weight: 800; }.status-pill i, .live-mark i { width: 6px; height: 6px; border-radius: 50%; background: var(--forest); display: inline-block; }
.story-cover { position: relative; overflow: hidden; border-radius: 22px; margin: 4px 0 24px; background: var(--wash); box-shadow: var(--shadow-lift); }.story-cover img { display: block; width: 100%; max-height: 480px; object-fit: cover; }.cover-caption { position: absolute; bottom: 16px; left: 18px; padding: 6px 9px; border: 1px solid rgba(255,255,255,.35); border-radius: 7px; background: rgba(15,45,38,.46); color: #fff; font-size: 9px; letter-spacing: .14em; }.edit-meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 11px; margin: 0 0 17px; }.edit-meta-grid label { display: grid; gap: 6px; color: var(--ink-2); font-size: 11px; font-weight: 800; }.edit-meta-grid .wide { grid-column: 1 / -1; }.edit-meta-grid input { min-width: 0; height: 36px; border: 1px solid var(--line); border-radius: 9px; padding: 0 10px; background: var(--card); color: var(--ink); outline: 0; }.edit-meta-grid input:focus { border-color: var(--forest); box-shadow: 0 0 0 3px color-mix(in srgb, var(--forest) 10%, transparent); }.editor-shell { margin-top: 8px; }.rendered-note { padding: 12px 4px 18px; color: var(--ink); font-size: 17px; line-height: 1.9; }.rendered-note :deep(h1), .rendered-note :deep(h2), .rendered-note :deep(h3) { color: var(--ink); line-height: 1.3; margin: 1.2em 0 .55em; }.rendered-note :deep(h1) { font-size: 30px; }.rendered-note :deep(h2) { font-size: 24px; border-bottom: 1px solid var(--line); padding-bottom: 9px; }.rendered-note :deep(h3) { font-size: 19px; color: var(--forest); }.rendered-note :deep(p) { margin: 0 0 1em; }.rendered-note :deep(strong) { color: var(--forest); }.rendered-note :deep(blockquote) { margin: 22px 0; padding: 14px 18px; border-left: 4px solid var(--sunset); border-radius: 0 12px 12px 0; background: var(--sunset-soft); color: var(--ink-2); }.rendered-note :deep(img) { display: block; max-width: 100%; border-radius: 15px; margin: 18px 0; box-shadow: var(--shadow-soft); }.rendered-note :deep(hr) { border: 0; border-top: 2px solid var(--line); margin: 30px 0; }.empty-copy { color: var(--ink-3); }.story-tags { display: flex; flex-wrap: wrap; gap: 7px; margin: 10px 0 26px; }.story-tags span { padding: 5px 10px; border-radius: 999px; background: var(--roam-soft); color: var(--forest); font-size: 11px; font-weight: 800; }.story-footer { display: flex; justify-content: space-between; align-items: center; gap: 14px; padding-top: 17px; border-top: 1px solid var(--line); }.footer-hint { color: var(--ink-3); font-size: 11px; }.footer-actions { gap: 8px; }.cancel-btn, .reaction-btn { padding: 9px 13px; background: transparent; color: var(--ink-2); font-size: 11px; }.save-main-btn { padding: 10px 16px; background: var(--forest); color: #fff; border-color: var(--forest); font-size: 11px; }
.social-column { display: grid; gap: 14px; position: sticky; top: 78px; }.side-card { padding: 19px; border: 1px solid var(--line); border-radius: 18px; background: var(--card); box-shadow: var(--shadow-soft); }.side-card h3 { margin: 8px 0 7px; font-size: 17px; line-height: 1.35; }.side-card p { color: var(--ink-2); font-size: 12px; line-height: 1.7; }.migration-flow { gap: 5px; margin: 17px 0; color: var(--forest); font-size: 10px; font-weight: 800; }.migration-flow span { padding: 5px 7px; border-radius: 6px; background: var(--roam-soft); }.migration-flow i { color: var(--ink-3); font-style: normal; }.side-primary { width: 100%; display: flex; justify-content: space-between; border: 0; border-radius: 9px; padding: 10px 12px; background: var(--forest); color: #fff; font-size: 11px; font-weight: 800; cursor: pointer; }.comments-head { justify-content: space-between; gap: 8px; }.comments-head h3 { margin-bottom: 0; }.comments-head small { color: var(--ink-3); font-size: 11px; }.live-mark { display: inline-flex; align-items: center; gap: 4px; color: var(--sunset); font-size: 9px; font-weight: 900; letter-spacing: .1em; }.live-mark i { background: var(--sunset); }.comment-list { display: grid; gap: 13px; max-height: 360px; overflow: auto; margin: 16px -3px 12px 0; padding-right: 4px; }.comment-item { display: grid; grid-template-columns: 25px 1fr; gap: 8px; }.comment-avatar { width: 25px; height: 25px; display: grid; place-items: center; border-radius: 50%; background: var(--roam-soft); color: var(--forest); font-size: 10px; font-weight: 900; }.comment-item b { font-size: 11px; color: var(--forest); }.comment-item p { margin: 3px 0 2px; font-size: 11px; line-height: 1.55; color: var(--ink); }.comment-item time { font-size: 9px; color: var(--ink-3); }.comments-empty, .comments-loading { padding: 21px 0 14px; text-align: center; color: var(--ink-3); font-size: 11px; }.comment-composer { display: flex; gap: 6px; padding-top: 11px; border-top: 1px solid var(--line); }.comment-composer input { min-width: 0; flex: 1; height: 34px; border: 1px solid var(--line); border-radius: 9px; padding: 0 9px; background: var(--paper); color: var(--ink); outline: 0; font-size: 11px; }.comment-composer input:focus { border-color: var(--forest); }.comment-composer button { width: 34px; height: 34px; border: 0; border-radius: 9px; background: var(--forest); color: #fff; font-weight: 800; cursor: pointer; }.comment-composer button:disabled { opacity: .45; cursor: not-allowed; }.shortcut-card { display: grid; gap: 9px; }.shortcut-card > button { display: grid; grid-template-columns: 25px 1fr 15px; align-items: center; gap: 8px; width: 100%; padding: 10px 0; border: 0; border-top: 1px solid var(--line); background: transparent; text-align: left; cursor: pointer; }.shortcut-card > button:first-of-type { margin-top: 4px; }.shortcut-card > button > span { color: var(--sunset); font-size: 17px; }.shortcut-card b { display: block; color: var(--ink); font-size: 11px; }.shortcut-card small { display: block; margin-top: 3px; color: var(--ink-3); font-size: 10px; }.shortcut-card em { color: var(--forest); font-style: normal; }.shortcut-card > button:hover b { color: var(--forest); }
.detail-skeleton { width: min(850px, calc(100% - 48px)); margin: 48px auto; }.skeleton-line, .skeleton-cover { background: linear-gradient(90deg, var(--wash), color-mix(in srgb, var(--wash) 55%, #fff), var(--wash)); background-size: 200% 100%; animation: shimmer 1.4s infinite; border-radius: 9px; }.skeleton-line { height: 18px; margin: 15px 0; }.skeleton-line.short { width: 120px; height: 11px; }.skeleton-line.title { width: 58%; height: 42px; }.skeleton-cover { height: 300px; margin: 25px 0; border-radius: 20px; }@keyframes shimmer { to { background-position: -200% 0; } }.empty-state { min-height: 70%; display: grid; place-items: center; align-content: center; gap: 8px; color: var(--ink-3); text-align: center; }.empty-icon { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 17px; background: var(--roam-soft); color: var(--forest); font-size: 25px; }.empty-state h3 { color: var(--ink); font-size: 17px; }.empty-state p { font-size: 12px; }
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; width: min(760px, calc(100% - 32px)); }.social-column { position: static; grid-template-columns: 1fr 1fr; }.story-head h1, .story-title-input { font-size: 36px; } }
@media (max-width: 620px) { .detail-topbar { align-items: flex-start; flex-direction: column; }.topbar-actions { width: 100%; justify-content: flex-start; }.detail-grid { width: calc(100% - 24px); padding-top: 18px; }.story-head h1, .story-title-input { font-size: 31px; }.edit-meta-grid, .social-column { grid-template-columns: 1fr; }.story-footer { align-items: flex-start; flex-direction: column; }.footer-actions { width: 100%; }.footer-actions button { flex: 1; }.story-cover img { max-height: 300px; } }
</style>
