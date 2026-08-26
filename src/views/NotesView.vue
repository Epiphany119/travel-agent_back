<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { listNotes, getNote, createNote, updateNote, deleteNote,
          getNoteUserId, type NoteDocument } from '@/api/note'
import { useRightPanelStore } from '@/stores/rightPanel'
import { getPreferences } from '@/api/user'

// ─── Markdown 引擎 ──────────────────────────────────────
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  typographer: true,
  table: true
  ,highlight: (code: string, lang: string) => highlightCode(code, lang)
} as any)

function escapeHtml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

// Lightweight highlighting for the languages most commonly used in travel notes.
// Markdown remains valid even when a language is unknown.
function highlightCode(code: string, lang: string): string {
  const source = escapeHtml(code)
  if (!/^(java|javascript|typescript|js|ts|json|css|html|xml|sql|bash|sh|python|py)?$/i.test(lang || '')) return source
  let highlighted = source
    .replace(/(\/\/[^\n]*|#[^\n]*)/g, '<span class="code-comment">$1</span>')
    .replace(/(&quot;.*?&quot;|&#39;.*?&#39;)/g, '<span class="code-string">$1</span>')
    .replace(/\b(abstract|boolean|break|case|catch|class|const|continue|def|else|extends|final|for|from|function|if|implements|import|in|interface|let|new|null|package|private|protected|public|return|static|this|throw|try|var|void|while|async|await|true|false)\b/g, '<span class="code-keyword">$1</span>')
  return highlighted
}

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('class', 'md-link')
  token.attrSet('data-link', 'true')
  return self.renderToken(tokens, idx, options)
}

function renderMarkdown(source: string): string {
  if (!source) return ''
  try {
    const raw = md.render(source)
    // 为代码块提取语言，标注到 <pre> 上（用于显示语言角标 + 代码框）
    let html = raw.replace(
      /<pre><code[^>]*class="language-([\w+-]+)"[^>]*>/g,
      (m: string, lang: string) => `<pre data-lang="${lang}"><code class="language-${lang}">`
    )
    return DOMPurify.sanitize(html, {
      ADD_TAGS: ['img', 'hr', 'input', 'figure', 'figcaption'],
      ADD_ATTR: ['contenteditable', 'draggable', 'data-note-id', 'data-link']
    })
  } catch {
    return source
  }
}

// ─── 状态 ───────────────────────────────────────────────
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const currentId = ref<number | null>(null)
const docs = ref<NoteDocument[]>([])
const curDoc = reactive<NoteDocument>({ title: '', content: '', updatedAt: '' })

const editorContent = ref('')
const editorRef = ref<HTMLTextAreaElement | null>(null)
const liveEditorRef = ref<HTMLElement | null>(null)
const themePopoverVisible = ref(false)
const showPreview = ref(true)
const isEditorMode = ref(false)

// 全局右侧面板
const rightPanel = useRightPanelStore()

// 右侧信息栏折叠状态
const selectionThemeVisible = ref(true)
const outlineVisible = ref(true)
const infoVisible = ref(false)
const shortcutsVisible = ref(true)
const outlinePanelHeight = ref(360)
let outlineResizeCleanup: (() => void) | null = null

const currentUserId = computed(() => getNoteUserId())

// ─── 主题 ────────────────────────────────────────────────
const theme = reactive({
  bg: '#ffffff',
  fg: '#1f2329',
  accent: '#3370ff'
})
const useSystemTheme = ref(false)
const systemTheme = reactive({ bg: '#F7F3EA', fg: '#1D2B27', accent: '#164E42' })
const effectiveTheme = computed(() => useSystemTheme.value ? systemTheme : theme)

const themePresets = [
  { name: '明亮', bg: '#ffffff', fg: '#1f2329', accent: '#3370ff' },
  { name: '护眼', bg: '#f5f0e1', fg: '#3d3527', accent: '#c4702b' },
  { name: '夜蓝', bg: '#1a1a2e', fg: '#e0e0e0', accent: '#5378ff' },
  { name: '森林', bg: '#f0f7f1', fg: '#2d3e35', accent: '#4a9b6e' },
  { name: '樱花', bg: '#fff5f7', fg: '#3d2937', accent: '#e8758e' },
  { name: '石墨', bg: '#2c2c2c', fg: '#e8e8e8', accent: '#888888' },
]
const themeRows: { key: 'bg' | 'fg' | 'accent'; label: string }[] = [
  { key: 'bg', label: '背景色' },
  { key: 'fg', label: '文字色' },
  { key: 'accent', label: '强调色' }
]

function applyTheme() {
  const root = document.documentElement
  root.style.setProperty('--notes-bg', effectiveTheme.value.bg)
  root.style.setProperty('--notes-fg', effectiveTheme.value.fg)
  root.style.setProperty('--notes-accent', effectiveTheme.value.accent)
}
watch([theme, systemTheme, useSystemTheme], applyTheme, { deep: true })

function themeToJson(): string {
  return JSON.stringify({ bg: theme.bg, fg: theme.fg, accent: theme.accent, useSystemTheme: useSystemTheme.value })
}

async function toggleSystemTheme() {
  applyTheme()
  // The checkbox represents an explicit preference, so persist it immediately.
  await saveDoc()
}

function loadThemeFromJson(jsonStr?: string) {
  if (!jsonStr) return
  try {
    const saved = JSON.parse(jsonStr)
    if (saved.bg) theme.bg = saved.bg
    if (saved.fg) theme.fg = saved.fg
    if (saved.accent) theme.accent = saved.accent
    useSystemTheme.value = saved.useSystemTheme === true
  } catch {}
}

async function loadSystemTheme() {
  try {
    const result = await getPreferences()
    const saved = result.data?.systemThemeJson ? JSON.parse(result.data.systemThemeJson) : {}
    Object.assign(systemTheme, { bg: saved.bg || systemTheme.bg, fg: saved.fg || systemTheme.fg, accent: saved.accent || systemTheme.accent })
    applyTheme()
  } catch {}
}

// ─── 时间格式化 ──────────────────────────────────────────
function formatTime(dateStr?: string): string {
  if (!dateStr) return '刚刚'
  try {
    let parsed: Date
    if (dateStr.includes('T')) {
      const [datePart, timePart] = dateStr.split('T')
      parsed = new Date(datePart + 'T' + (timePart || '00:00:00') + '.000')
    } else {
      parsed = new Date(dateStr)
    }
    if (isNaN(parsed.getTime())) return '刚刚'
    const now = new Date()
    const diffMs = now.getTime() - parsed.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    if (diffMin < 1) return '刚刚'
    if (diffMin < 60) return `${diffMin} 分钟前`
    const y = parsed.getFullYear()
    const m = String(parsed.getMonth() + 1).padStart(2, '0')
    const d = String(parsed.getDate()).padStart(2, '0')
    const h = String(parsed.getHours()).padStart(2, '0')
    const mi = String(parsed.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${mi}`
  } catch { return '刚刚' }
}

const displayTime = computed(() => formatTime(curDoc.updatedAt))
const renderedPreview = computed(() => renderMarkdown(editorContent.value))

function syncLiveEditor() {
  const el = liveEditorRef.value
  if (!el) return
  editorContent.value = htmlToMarkdown(el)
}

function htmlToMarkdown(root: HTMLElement): string {
  const walk = (node: Node): string => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent || ""
    if (!(node instanceof HTMLElement)) return Array.from(node.childNodes).map(walk).join("")
    const inner = Array.from(node.childNodes).map(walk).join("")
    const tag = node.tagName.toLowerCase()
    if (/^h[1-6]$/.test(tag)) return "#".repeat(Number(tag[1])) + " " + inner.trim() + "\n\n"
    if (tag === "p") return inner.trim() + "\n\n"
    if (tag === "br") return "\n"
    // contenteditable browsers represent Enter-separated lines as div blocks.
    // A single newline keeps normal lines tightly spaced in Markdown; adding
    // a blank line would turn every line into a separate paragraph.
    if (tag === "div") return "\n" + inner
    if (tag === "strong" || tag === "b") return "**" + inner + "**"
    if (tag === "em" || tag === "i") return "*" + inner + "*"
    if (tag === "code" && node.parentElement?.tagName.toLowerCase() !== "pre") return "`" + inner + "`"
    if (tag === "pre") return "\n\n```\n" + node.innerText.trim() + "\n```\n\n"
    if (tag === "img") return "![" + (node.getAttribute("alt") || "") + "](" + (node.getAttribute("src") || "") + ")"
    if (tag === "table") {
      const rows = Array.from(node.querySelectorAll("tr")).map(row => Array.from(row.children).map(cell => String(cell.textContent || "").trim()))
      if (!rows.length) return ""
      const head = "| " + rows[0].join(" | ") + " |"
      const divider = "| " + rows[0].map(() => "---").join(" | ") + " |"
      const body = rows.slice(1).map(row => "| " + row.join(" | ") + " |").join("\n")
      return head + "\n" + divider + (body ? "\n" + body : "") + "\n\n"
    }
    if (tag === "li") return "- " + inner.trim() + "\n"
    if (tag === "ul" || tag === "ol") return inner + "\n"
    if (tag === "blockquote") return inner.split("\n").filter(Boolean).map(line => "> " + line).join("\n") + "\n\n"
    return inner
  }
  return walk(root).replace(/\n{3,}/g, "\n\n").trim()
}

// ─── 列表加载 ────────────────────────────────────────────
async function load() {
  loading.value = true
  try {
    docs.value = await listNotes()
    if (currentId.value == null && docs.value.length > 0) {
      await openDoc(docs.value[0].id!)
    }
  } catch (e) { console.error(e) } finally { loading.value = false }
}

onMounted(() => { 
  load()
  loadSystemTheme()
  applyTheme()
  document.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
})

// ─── 打开笔记 ───────────────────────────────────────────
async function openDoc(id: number) {
  try {
    const d = await getNote(id)
    currentId.value = id
    curDoc.id = d.id
    curDoc.title = d.title || ''
    curDoc.destination = d.destination || ''
    curDoc.coverUrl = d.coverUrl || ''
    curDoc.visibility = d.visibility || 'private'
    curDoc.updatedAt = d.updatedAt || ''
    curDoc.themeJson = d.themeJson || ''
    curDoc.content = d.content || ''
    
    editorContent.value = d.content || ''
    
    if (d.themeJson) loadThemeFromJson(d.themeJson)
  } catch (e: any) { ElMessage.error(e?.message || '打开失败') }
}

// ─── 保存笔记 ────────────────────────────────────────────
async function saveDoc() {
  syncLiveEditor()
  if (currentId.value == null) { await addDoc(); return }
  saving.value = true
  try {
    const payload = {
      title: curDoc.title || '未命名笔记',
      content: editorContent.value,
      destination: curDoc.destination || '',
      coverUrl: curDoc.coverUrl || '',
      visibility: curDoc.visibility || 'private',
      themeJson: themeToJson()
    }
    const d = await updateNote(currentId.value, payload as NoteDocument)
    ElMessage.success('已保存')
    curDoc.updatedAt = d.updatedAt || new Date().toISOString().replace('Z', '')
    curDoc.content = editorContent.value
    curDoc.themeJson = d.themeJson || themeToJson()
    docs.value = await listNotes()
  } catch (e: any) {
    console.error('Save failed:', e)
    ElMessage.error(e?.message || '保存失败')
  } finally { saving.value = false }
}

async function addDoc(initialContent = '') {
  // Initialize the editor state before the request completes so the toolbar
  // and title input remain present while the new document is being created.
  curDoc.title = '新笔记'
  curDoc.content = initialContent
  editorContent.value = initialContent
  creating.value = true
  try {
    const d = await createNote({
      title: '新笔记',
      content: initialContent,
      visibility: 'private',
      themeJson: themeToJson()
    })
    docs.value.unshift(d)
    await openDoc(d.id!)
  } catch (e: any) { ElMessage.error(e?.message || '创建失败') }
  finally { creating.value = false }
}

async function removeDoc() {
  if (currentId.value == null) return
  try {
    await ElMessageBox.confirm('确定删除这篇笔记吗？', '删除笔记', { type: 'warning' })
    await deleteNote(currentId.value)
    ElMessage.success('已删除')
    currentId.value = null
    curDoc.title = ''
    curDoc.content = ''
    curDoc.updatedAt = ''
    editorContent.value = ''
    await load()
  } catch {}
}

// ─── 链接点击处理 ───────────────────────────────────────
function handlePreviewClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  const linkEl = target.closest('a') as HTMLAnchorElement | null
  if (!linkEl) return
  
  const url = linkEl.getAttribute('href') || linkEl.getAttribute('data-href') || ''
  if (!url) return
  
  e.preventDefault()
  e.stopPropagation()
  
  if (url.startsWith('mailto:')) {
    window.open(url, '_blank')
    return
  }
  if (url.startsWith('tel:')) {
    window.open(url, '_blank')
    return
  }
  
  const title = linkEl.textContent?.trim() || url
  // 在全局右侧面板打开链接预览
  rightPanel.openLink({ url, title, })
}

// ─── Markdown 导入 ──────────────────────────────────────
const mdInput = ref<HTMLInputElement | null>(null)

function openMdPicker() { mdInput.value?.click() }

async function importMarkdown(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const title = file.name.replace(/\.md$/i, '')
    
    if (currentId.value != null) {
      // 更新现有文档
      editorContent.value = text
      curDoc.title = title
      const updated = await updateNote(currentId.value, {
        title,
        content: text,
        destination: curDoc.destination || '',
        coverUrl: curDoc.coverUrl || '',
        visibility: curDoc.visibility || 'private',
        themeJson: themeToJson()
      } as NoteDocument)
      curDoc.updatedAt = updated.updatedAt || new Date().toISOString()
      curDoc.content = text
    } else {
      // 直接创建完整文档，避免先创建空文档再更新导致导入状态不一致
      const created = await createNote({
        title,
        content: text,
        visibility: 'private',
        themeJson: themeToJson()
      })
      docs.value.unshift(created)
      await openDoc(created.id!)
    }
    await load()
    ElMessage.success('Markdown 已导入')
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  }
  ;(event.target as HTMLInputElement).value = ''
}

// ─── 拖拽调整面板宽度（修复版） ────────────────────────────
const LEFT_MIN = 176
const LEFT_MAX = 420
const RIGHT_MIN = 264
const RIGHT_MAX = 640
const CENTER_MIN = 360
const leftPanelWidth = ref(Number(localStorage.getItem('roamly-notes-left-width')) || 220)
const rightPanelWidth = ref(Number(localStorage.getItem('roamly-notes-right-width')) || 320)
const showRightPanel = ref(localStorage.getItem('roamly-notes-right-visible') !== '0')
const showLeftPanel = ref(localStorage.getItem('roamly-notes-left-visible') !== '0')

function notesBounds() {
  const el = document.querySelector('.notes-app') as HTMLElement | null
  return el?.getBoundingClientRect() || { left: 0, right: window.innerWidth, width: window.innerWidth }
}

function clampPanelWidth(width: number, side: 'left' | 'right') {
  const bounds = notesBounds()
  const available = Math.max(0, bounds.width - CENTER_MIN - 12)
  if (side === 'left') return Math.round(Math.min(LEFT_MAX, Math.max(LEFT_MIN, Math.min(width, available - (showRightPanel.value ? rightPanelWidth.value : 0)))))
  return Math.round(Math.min(RIGHT_MAX, Math.max(RIGHT_MIN, Math.min(width, available - (showLeftPanel.value ? leftPanelWidth.value : 0)))))
}

watch(leftPanelWidth, value => localStorage.setItem('roamly-notes-left-width', String(value)))
watch(rightPanelWidth, value => localStorage.setItem('roamly-notes-right-width', String(value)))
watch(showLeftPanel, value => localStorage.setItem('roamly-notes-left-visible', value ? '1' : '0'))
watch(showRightPanel, value => localStorage.setItem('roamly-notes-right-visible', value ? '1' : '0'))

function onLeftHandleMouseDown(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  const bounds = notesBounds()
  leftPanelWidth.value = clampPanelWidth(e.clientX - bounds.left, 'left')

  const handleMove = (ev: MouseEvent) => {
    leftPanelWidth.value = clampPanelWidth(ev.clientX - bounds.left, 'left')
  }

  const handleUp = () => {
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleMove)
  document.addEventListener('mouseup', handleUp)
}

function onRightHandleMouseDown(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  const bounds = notesBounds()
  rightPanelWidth.value = clampPanelWidth(bounds.right - e.clientX, 'right')

  const handleMove = (ev: MouseEvent) => {
    rightPanelWidth.value = clampPanelWidth(bounds.right - ev.clientX, 'right')
  }

  const handleUp = () => {
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleMove)
  document.addEventListener('mouseup', handleUp)
}

// 快捷键
function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    saveDoc()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    showPreview.value = !showPreview.value
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
    e.preventDefault()
    showRightPanel.value = !showRightPanel.value
  }
}

// ─── 字数统计 ───────────────────────────────────────────
const wordCount = computed(() => {
  const text = editorContent.value
  if (!text) return 0
  const chinese = (text.match(/[\u4e00-\u9fa5]/g) || []).length
  const english = (text.match(/[a-zA-Z]+/g) || []).length
  return chinese + english
})

// ─── 大纲提取 ───────────────────────────────────────────
const outline = computed(() => {
  const lines = editorContent.value.split('\n')
  const result: { level: number; text: string; line: number }[] = []
  lines.forEach((line, idx) => {
    const match = line.match(/^(#{1,3})\s+(.+)$/)
    if (match) {
      result.push({ level: match[1].length, text: match[2], line: idx })
    }
  })
  return result
})

function scrollToHeading(line: number) {
  const item = outline.value.find(entry => entry.line === line)
  const container = liveEditorRef.value
  if (!container || !item) return
  const headings = container.querySelectorAll('h1, h2, h3')
  const heading = Array.from(headings).find(node => node.textContent?.trim() === item.text.trim()) as HTMLElement | undefined
  if (heading) {
    const cRect = container.getBoundingClientRect()
    const hRect = heading.getBoundingClientRect()
    // 精准定位：标题行刚好对齐容器顶部（不差行、不偏移）
    const delta = hRect.top - cRect.top
    container.scrollTo({ top: container.scrollTop + delta, behavior: 'smooth' })
  }
}

function startOutlineResize(e: PointerEvent) {
  e.preventDefault()
  const startY = e.clientY
  const startHeight = outlinePanelHeight.value
  const move = (ev: PointerEvent) => {
    outlinePanelHeight.value = Math.max(180, Math.min(window.innerHeight * .78, startHeight + ev.clientY - startY))
  }
  const up = () => { document.removeEventListener('pointermove', move); document.removeEventListener('pointerup', up); outlineResizeCleanup = null; document.body.style.cursor = '' }
  document.body.style.cursor = 'row-resize'
  document.addEventListener('pointermove', move)
  document.addEventListener('pointerup', up)
  outlineResizeCleanup = up
}
</script>

<template>
  <div class="notes-app" :style="{ background: effectiveTheme.bg, color: effectiveTheme.fg }">
    <!-- ===== 左侧笔记列表面板（与右侧面板一致） ===== -->
    <Transition name="sidebar-slide-left">
    <aside 
      v-if="showLeftPanel"
      class="left-panel" 
      :style="{ width: leftPanelWidth + 'px', background: effectiveTheme.bg, color: effectiveTheme.fg }"
    >
      <!-- 面板工具条 -->
      <div class="left-toolbar">
        <span class="left-toolbar-title">🗂 笔记列表</span>
        <button 
          class="panel-collapse-btn"
          title="收起左侧面板"
          @click="showLeftPanel = false"
        ><span class="panel-chevron" aria-hidden="true">‹</span></button>
      </div>

      <div class="panel-toolbar">
        <input 
          ref="mdInput" 
          type="file" 
          accept=".md,text/markdown" 
          hidden 
          @change="importMarkdown" 
        />
        <button class="toolbar-btn" @click="openMdPicker" title="导入 Markdown">
          📥 导入 MD
        </button>
        <button 
          class="new-note-btn" 
          :style="{ background: effectiveTheme.accent }"
          @click="addDoc('')"
        >
          ＋ 新建笔记
        </button>
      </div>

      <div class="workspace-mini">
        <span class="workspace-name">
          <span class="workspace-dot" :style="{ background: effectiveTheme.accent }">R</span>
          Roamly 工作台 · {{ currentUserId }}
        </span>
      </div>

      <div class="notes-list" v-loading="loading">
        <button
          v-for="d in docs"
          :key="d.id"
          class="note-item"
          :class="{ active: currentId === d.id }"
          :style="currentId === d.id ? { background: effectiveTheme.accent + '18', color: effectiveTheme.accent } : {}"
          @click="openDoc(d.id!)"
        >
          <div class="note-title">{{ d.title || '未命名笔记' }}</div>
          <div class="note-time">{{ formatTime(d.updatedAt) }}</div>
        </button>
        <p v-if="!loading && docs.length === 0" class="empty-hint">
          还没有笔记，点上方新建。
        </p>
      </div>

<!--      <div class="panel-footer">-->
<!--        <span>{{ currentUserId }}</span>-->
<!--      </div>-->
    </aside>
    </Transition>

    <!-- ===== 左侧拖拽手柄 ===== -->
    <div 
      v-if="showLeftPanel"
      class="drag-handle left-handle" 
      @mousedown="onLeftHandleMouseDown"
    ></div>

    <!-- ===== 左栏收起后的展开按钮 ===== -->
    <button
      v-if="!showLeftPanel"
      class="left-restore-btn"
      title="展开笔记列表"
      @click="showLeftPanel = true"
    ><span class="panel-chevron" aria-hidden="true">›</span></button>

    <!-- ===== 中间编辑区 ===== -->
    <main class="center-panel">
      <header class="editor-toolbar" v-if="curDoc.id || creating">
        <div class="breadcrumb">
          <span>我的笔记</span>
          <i>/</i>
          <b>{{ curDoc.title || '未命名笔记' }}</b>
        </div>
        
        <div class="toolbar-right">
          <input 
            v-model="curDoc.title" 
            class="title-input" 
            placeholder="标题"
          />
          
          <div class="tool-group">
          </div>
          
          <div class="tool-group">
            <div class="theme-popover-wrap">
              <button class="tool-btn" :class="{ active: showRightPanel && selectionThemeVisible }" @click="showRightPanel = true; selectionThemeVisible = true" title="打开主题设置">🎨</button>
              <div v-if="themePopoverVisible" class="theme-popover">
                <div class="popover-title">主题颜色</div>
                <label v-for="row in themeRows" :key="row.key" class="popover-color-row">
                  <span>{{ row.label }}</span>
                  <input type="color" v-model="theme[row.key]" />
                  <input class="hex-input" v-model="theme[row.key]" maxlength="7" spellcheck="false" />
                </label>
                <div class="theme-presets compact-presets"><button v-for="p in themePresets" :key="p.name" class="preset-btn" :style="{ background: p.bg, color: p.fg, borderColor: p.accent }" @click="Object.assign(theme, p)">{{ p.name }}</button></div>
                <label class="system-theme-check"><input type="checkbox" v-model="useSystemTheme" @change="toggleSystemTheme" /> 应用系统主题</label>
                <button class="theme-save-btn" :style="{ background: effectiveTheme.accent }" @click="saveDoc(); themePopoverVisible = false">保存主题</button>
              </div>
            </div>
            <span class="save-indicator" :class="{ saving }">
              {{ saving ? '保存中…' : '已保存' }}
            </span>
            <button 
              class="save-btn" 
              :style="{ background: effectiveTheme.accent }"
              :disabled="saving"
              @click="saveDoc"
            >
              {{ saving ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </header>

      <div class="editor-body" v-if="curDoc.id || creating">
        <div class="editor-area">
          <div class="editor-main">
            
            <div ref="liveEditorRef" class="preview-content markdown-body live-editor" contenteditable="true" spellcheck="false" v-html="renderedPreview" @blur="syncLiveEditor" @click="handlePreviewClick"></div>
          </div>
          
        </div>
      </div>
      
      <p v-else class="empty-state">选择或创建一篇笔记开始编辑</p>
    </main>

    <!-- ===== 右侧拖拽手柄 ===== -->
    <div 
      v-if="showRightPanel" 
      class="drag-handle right-handle" 
      @mousedown="onRightHandleMouseDown"
    ></div>

    <!-- ===== 右侧信息面板 ===== -->
    <Transition name="sidebar-slide-right">
    <aside 
      v-if="showRightPanel"
      class="right-panel"
      :style="{ width: rightPanelWidth + 'px', background: effectiveTheme.bg }"
    >
      <!-- 面板收起条 -->
      <div class="right-toolbar">
        <span class="right-toolbar-title">📄 面板</span>
        <button 
          class="panel-collapse-btn"
          title="收起右侧面板 (Ctrl+B)"
          @click="showRightPanel = false"
        ><span class="panel-chevron" aria-hidden="true">›</span></button>
      </div>

      <!-- 主题设置 -->
      <div class="panel-section">
        <div class="section-header" @click="selectionThemeVisible = !selectionThemeVisible">
          <span>🎨 主题设置</span>
          <span class="collapse-icon">{{ selectionThemeVisible ? '▼' : '▶' }}</span>
        </div>
        <div class="section-body" v-if="selectionThemeVisible">
          <div class="theme-row">
            <label>背景色</label>
            <input type="color" v-model="theme.bg" />
            <input class="hex-input" v-model="theme.bg" maxlength="7" spellcheck="false" />
          </div>
          <div class="theme-row">
            <label>文字色</label>
            <input type="color" v-model="theme.fg" />
            <input class="hex-input" v-model="theme.fg" maxlength="7" spellcheck="false" />
          </div>
          <div class="theme-row">
            <label>强调色</label>
            <input type="color" v-model="theme.accent" />
            <input class="hex-input" v-model="theme.accent" maxlength="7" spellcheck="false" />
          </div>
          <div class="theme-presets">
            <button 
              v-for="p in themePresets" 
              :key="p.name"
              class="preset-btn"
              :style="{ background: p.bg, color: p.fg, borderColor: p.accent }"
              @click="Object.assign(theme, p)"
            >{{ p.name }}</button>
          </div>
          <label class="system-theme-check"><input type="checkbox" v-model="useSystemTheme" @change="toggleSystemTheme" /> 应用系统主题</label>
          <button 
            class="theme-save-btn"
            :style="{ background: effectiveTheme.accent }"
            @click="saveDoc"
          >💾 保存主题</button>
        </div>
      </div>

      <!-- 大纲 -->
      <div class="panel-section" v-if="outline.length > 0">
        <div class="section-header" @click="outlineVisible = !outlineVisible">
          <span>📑 大纲目录</span>
          <span class="collapse-icon">{{ outlineVisible ? '▼' : '▶' }}</span>
        </div>
        <div class="section-body outline" v-if="outlineVisible" :style="{ height: outlinePanelHeight + 'px' }">
          <div 
            v-for="(item, idx) in outline" 
            :key="idx"
            class="outline-item"
            :style="{ paddingLeft: (item.level - 1) * 12 + 12 + 'px' }"
            @click="scrollToHeading(item.line)"
          >
            {{ item.text }}
          </div>
          <div class="section-resize-handle" title="拖动调整大纲高度" @pointerdown="startOutlineResize"><span></span></div>
        </div>
      </div>

      <!-- 文档信息 -->
      <div class="panel-section" v-if="curDoc.id">
        <div class="section-header" @click="infoVisible = !infoVisible">
          <span>📄 文档信息</span>
          <span class="collapse-icon">{{ infoVisible ? '▼' : '▶' }}</span>
        </div>
        <div class="section-body info" v-if="infoVisible">
          <div class="info-row">
            <span>字数</span>
            <b>{{ wordCount }}</b>
          </div>
          <div class="info-row">
            <span>最后编辑</span>
            <b>{{ displayTime }}</b>
          </div>
          <div class="info-row">
            <span>状态</span>
            <b>草稿</b>
          </div>
          <button class="danger-btn" @click="removeDoc">🗑 删除笔记</button>
        </div>
      </div>

      <!-- 快捷键提示 -->
      <div class="panel-section">
        <div class="section-header" @click="shortcutsVisible = !shortcutsVisible">
          <span>⌨️ 快捷键</span>
          <span class="collapse-icon">{{ shortcutsVisible ? '▼' : '▶' }}</span>
        </div>
        <div class="section-body shortcuts" v-if="shortcutsVisible">
          <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>S</kbd> 保存</div>
          <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>E</kbd> 切换预览</div>
          <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>B</kbd> 显示/隐藏右侧</div>
          <div class="shortcut-row"><kbd>Ctrl</kbd>+<kbd>A</kbd> 全选</div>

        </div>
      </div>
    </aside>
    </Transition>

    <!-- 右栏收起后的展开按钮 -->
    <button
      v-if="!showRightPanel"
      class="right-restore-btn"
      title="展开右侧面板 (Ctrl+B)"
      @click="showRightPanel = true"
    ><span class="panel-chevron" aria-hidden="true">‹</span></button>
  </div>
</template>

<style scoped lang="scss">
.notes-app {
  /* Keep the editor's design tokens local; the surrounding app follows the
     user's system palette independently. */
  --forest: #164E42;
  --forest-deep: #0E382E;
  --roam: #4F8F78;
  --roam-soft: #E9F1EC;
  --sunset: #F27A4F;
  --sunset-soft: #FDEEE6;
  --paper: #F7F3EA;
  --card: #FFFDF8;
  --wash: #F2EDE1;
  --ink: #1D2B27;
  --ink-2: #5C6B65;
  --ink-3: #8C9993;
  --line: #E7E0D2;
  position: relative;
  display: flex;
  height: 100vh;
  overflow: hidden;
  min-width: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background: var(--notes-bg, #ffffff);
  color: var(--notes-fg, #1f2329);
}

.system-theme-check {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 10px 0;
  color: var(--notes-fg, #1f2329);
  font-size: 12px;
  cursor: pointer;
}

/* ─── 左侧面板 ─────────────────────────────────── */
.left-panel {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  border-radius: 18px;
  margin: 8px 0 8px 8px;
  box-shadow: 4px 0 18px rgba(31,35,41,.10);
  overflow: hidden;
  will-change: transform, opacity;
}

.sidebar-slide-left-enter-active,
.sidebar-slide-left-leave-active,
.sidebar-slide-right-enter-active,
.sidebar-slide-right-leave-active { transition: transform .22s ease, opacity .18s ease; }
.sidebar-slide-left-enter-from,
.sidebar-slide-left-leave-to { transform: translateX(-18px); opacity: 0; }
.sidebar-slide-right-enter-from,
.sidebar-slide-right-leave-to { transform: translateX(18px); opacity: 0; }

/* 收起/展开按钮 */
.panel-collapse-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #d8dade;
  border-radius: 6px;
  background: #fff;
  color: #646a73;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;

  &:hover {
    color: #245bdb;
    border-color: #9bb8ff;
    background: #f4f7ff;
  }
}

/* 左侧面板工具条（与右侧 right-toolbar 一致） */
.left-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e6e8;
  background: rgba(0, 0, 0, 0.02);
  flex-shrink: 0;

  .left-toolbar-title {
    font-size: 12px;
    font-weight: 600;
    color: #8f959e;
  }

  .panel-collapse-btn { width: 26px; height: 26px; }
}

/* 工作台标识条 */
.workspace-mini {
  padding: 10px 14px;
  border-bottom: 1px solid #e5e6e8;
  flex-shrink: 0;
}

.workspace-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-dot {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 12px;
  flex-shrink: 0;
}

/* 左栏收起后的展开按钮 */
.left-restore-btn {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 44px;
  border: 1px solid #e5e6e8;
  border-left: 0;
  border-radius: 0 12px 12px 0;
  background: var(--card, #fffdf8);
  color: #646a73;
  font-size: 24px;
  cursor: pointer;
  z-index: 50;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.06);
  transition: all .15s;

  &:hover {
    color: #245bdb;
    background: #f4f7ff;
    border-color: #9bb8ff;
  }
}

/* 右侧面板收起工具条 */
.right-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e6e8;
  background: rgba(0, 0, 0, 0.02);
  flex-shrink: 0;

  .right-toolbar-title {
    font-size: 12px;
    font-weight: 600;
    color: #8f959e;
  }

  .panel-collapse-btn { width: 26px; height: 26px; }
}

/* 右栏收起后的展开按钮 */
.right-restore-btn {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 44px;
  border: 1px solid #e5e6e8;
  border-right: 0;
  border-radius: 12px 0 0 12px;
  background: var(--card, #fffdf8);
  color: #646a73;
  font-size: 24px;
  cursor: pointer;
  z-index: 50;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.06);
  transition: all .15s;

  &:hover {
    color: #245bdb;
    background: #f4f7ff;
    border-color: #9bb8ff;
  }
}

.panel-chevron { display: block; line-height: 1; transform: translateY(-1px); }

.panel-toolbar {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid #e5e6e8;
}

.toolbar-btn {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d8dade;
  border-radius: 6px;
  background: #fff;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
  text-align: center;
  transition: all .15s;

  &:hover {
    border-color: #9bb8ff;
    color: #245bdb;
    background: #f4f7ff;
  }
}

.new-note-btn {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: opacity .15s;

  &:hover { opacity: .9; }
}

.notes-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.note-item {
  width: 100%;
  text-align: left;
  padding: 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background .15s;

  &:hover { background: #f0f1f2; }

  .note-title {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .note-time {
    font-size: 12px;
    color: #8f959e;
  }
}

.empty-hint {
  text-align: center;
  color: #aaa;
  font-size: 13px;
  padding: 40px 16px;
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid #e5e6e8;
  font-size: 12px;
  color: #8f959e;
}

/* ─── 拖拽手柄（修复版） ─────────────────────────────────── */
.drag-handle {
  width: 12px;
  flex-shrink: 0;
  cursor: col-resize;
  background: transparent;
  position: relative;
  z-index: 10;
  pointer-events: auto;
  user-select: none;
  touch-action: none;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: -8px;
    right: -8px;
  }

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 4px;
    height: 72px;
    border-radius: 999px;
    background: rgba(31,35,41,.18);
    transition: background .15s;
  }

  &:hover::after { background: var(--notes-accent, #3370ff); box-shadow: 0 0 0 4px color-mix(in srgb, var(--notes-accent, #3370ff) 14%, transparent); }
  &:active { background: rgba(51, 112, 255, 0.05); }
  &:active::after { background: #3370ff; }
}

.left-handle { margin-left: -1px; }
.right-handle { margin-right: -1px; }

/* ─── 中间编辑区 ─────────────────────────────────── */
.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: var(--notes-bg, #fff);
  border: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  border-radius: 18px;
  margin: 8px 0;
  box-shadow: 0 8px 24px rgba(31,35,41,.10);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  border-radius: 18px 18px 0 0;
  gap: 16px;
  flex-shrink: 0;
  background: var(--notes-bg, #fff);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8f959e;

  i { font-style: normal; color: #c5c7ca; }
  b { font-weight: 500; color: inherit; }
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-input {
  border: 0;
  outline: none;
  font-size: 16px;
  font-weight: 500;
  padding: 6px 8px;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  min-width: 200px;

  &:focus { background: #f5f6f7; }
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.theme-popover-wrap { position: relative; }
.theme-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 100;
  width: 286px;
  padding: 14px;
  border: 1px solid #dfe2e6;
  border-radius: 10px;
  background: var(--card, #fffdf8);
  box-shadow: 0 12px 30px rgba(0,0,0,.14);
}
.popover-title { font-size: 13px; font-weight: 700; margin-bottom: 10px; }
.popover-color-row { display: grid; grid-template-columns: 62px 32px 1fr; align-items: center; gap: 8px; margin: 8px 0; font-size: 12px; color: #646a73; }
.popover-color-row input[type='color'] { width: 28px; height: 28px; border: 1px solid #d8dade; border-radius: 5px; padding: 2px; background: #fff; }
.hex-input { min-width: 0; width: 100%; border: 1px solid #d8dade; border-radius: 5px; padding: 6px 7px; font: 12px ui-monospace, monospace; color: #1f2329; background: #fff; }
.theme-row .hex-input { width: 96px; }
.compact-presets { display: flex; flex-wrap: wrap; gap: 5px; margin: 12px 0; }
.compact-presets .preset-btn { padding: 4px 7px; font-size: 11px; }

.tool-btn {
  padding: 6px 10px;
  border: 1px solid #d8dade;
  border-radius: 6px;
  background: #fff;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
  transition: all .15s;

  &.active, &:hover {
    color: #245bdb;
    border-color: #9bb8ff;
    background: #f4f7ff;
  }
}

.save-indicator {
  font-size: 12px;
  color: #8f959e;

  &.saving { color: #f0a020; }
}

.save-btn {
  padding: 7px 16px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: opacity .15s;

  &:hover { opacity: .9; }
  &:disabled { opacity: .5; cursor: not-allowed; }
}

.editor-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.half { flex: 0.5; }
}

.md-textarea {
  flex: 1;
  width: 100%;
  padding: 48px 72px 120px;
  border: 0;
  outline: none;
  resize: none;
  font-family: 'SF Mono', 'Menlo', 'Consolas', 'Liberation Mono', 'Courier New', monospace;
  font-size: 15px;
  line-height: 1.8;
  color: inherit;
  background: transparent;
  tab-size: 2;
  overflow-y: auto;

  &::placeholder {
    color: #c5c7ca;
    white-space: pre;
  }
}

.editor-preview {
  flex: 0.5;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e5e6e8;
  overflow: hidden;
  background: var(--notes-bg, #fff);

  .preview-label {
    padding: 8px 16px;
    font-size: 12px;
    color: #8f959e;
    border-bottom: 1px solid #e5e6e8;
    background: #fafafa;
  }
}

.preview-content {
  flex: 1;
  width: min(100%, 1080px);
  margin: 0 auto;
  padding: 52px 64px 140px;
  overflow-y: auto;
  line-height: 1.8;
}

.live-editor {
  outline: none;
  cursor: text;
  min-height: 100%;
}
.live-editor:focus { box-shadow: inset 0 0 0 1px rgba(51,112,255,.12); }

.preview-content.scrollable { overflow-y: auto; }

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c5c7ca;
  font-size: 14px;
}

/* ─── 右侧面板 ─────────────────────────────────── */
.right-panel {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid color-mix(in srgb, var(--notes-fg, #1f2329) 10%, transparent);
  border-radius: 18px;
  margin: 8px 8px 8px 0;
  box-shadow: -4px 0 18px rgba(31,35,41,.10);
  overflow-y: auto;
  background: var(--notes-bg, #fafafa);
}

.panel-section {
  border-bottom: 1px solid #e5e6e8;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: inherit;

  &:hover { background: #f5f6f7; }
}

.collapse-icon { font-size: 10px; color: #8f959e; }

.section-body {
  position: relative;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 42px;
  max-height: 52vh;
  overflow: auto;
  resize: vertical;
}
.section-body.outline { min-height: 180px; resize: vertical; }
.section-resize-handle {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -7px;
  height: 14px;
  z-index: 5;
  cursor: row-resize;
  display: flex;
  align-items: center;
  justify-content: center;
}
.section-resize-handle span { width: 42px; height: 3px; border-radius: 3px; background: transparent; transition: background .15s; }
.section-resize-handle:hover span, .section-resize-handle:active span { background: var(--notes-accent, #3370ff); }

/* 主题设置 */
.theme-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;

  label {
    width: 56px;
    color: #646a73;
  }

  input[type="color"] {
    width: 32px;
    height: 28px;
    border: 1px solid #d8dade;
    border-radius: 4px;
    cursor: pointer;
    padding: 2px;
  }

  .hex {
    font-family: monospace;
    font-size: 12px;
    color: #8f959e;
  }
}

.theme-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preset-btn {
  padding: 6px 12px;
  border: 2px solid;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: transform .1s;

  &:hover { transform: translateY(-2px); }
}

.theme-save-btn {
  padding: 8px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: opacity .15s;

  &:hover { opacity: .9; }
}

/* 大纲 */
.outline {
  max-height: 200px;
  overflow-y: auto;
}

.outline-item {
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: background .1s;

  &:hover { background: #f5f6f7; }
}

/* 文档信息 */
.info {
  .info-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    padding: 6px 0;

    span { color: #646a73; }
    b { color: inherit; font-weight: 500; }
  }
}

.danger-btn {
  width: 100%;
  padding: 8px;
  border: 1px solid #f5c6cb;
  border-radius: 6px;
  background: #fff;
  color: #c33;
  font-size: 13px;
  cursor: pointer;
  margin-top: 8px;
  transition: all .15s;

  &:hover { background: #fde8e8; }
}

/* 快捷键 */
.shortcuts {
  .shortcut-row {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #646a73;
    padding: 2px 0;
  }

  kbd {
    display: inline-block;
    padding: 2px 6px;
    border: 1px solid #d8dade;
    border-radius: 3px;
    background: #f5f6f7;
    font-family: monospace;
    font-size: 11px;
    color: #646a73;
  }
}

/* ─── Markdown 预览样式（Cursor/GitHub 宽松排版） ────── */
/* 必须用 :deep()：v-html 注入的内容不带 scoped 属性，普通选择器无法命中 */
:deep(.markdown-body) {
  font-size: 16px;
  line-height: 1.75;
  letter-spacing: .01em;
  color: var(--notes-fg, #1f2329);
  word-break: break-word;

  h1, h2, h3, h4, h5, h6 {
    font-weight: 650;
    line-height: 1.35;
    margin-top: 2.1em;
    margin-bottom: .7em;
  }

  h1 {
    font-size: 2em;
    border-bottom: 1px solid #e5e6e8;
    padding-bottom: 10px;
    margin-top: 0;
  }

  h2 {
    font-size: 1.5em;
    border-bottom: 1px solid #e5e6e8;
    padding-bottom: 8px;
  }

  h3 { font-size: 1.25em; }
  h4 { font-size: 1em; }
  h5 { font-size: 0.9em; }
  h6 { font-size: 0.85em; color: #646a73; }

  p { margin: 1em 0; line-height: 1.8; }

  ul, ol {
    padding-left: 1.8em;
    margin: 1em 0;

    li {
      margin: .35em 0;
      line-height: 1.7;
    }

    ul, ol { margin: 8px 0; }
  }

  blockquote {
    border-left: 4px solid var(--notes-accent, #3370ff);
    padding: 4px 16px;
    color: #646a73;
    margin: 18px 0;
    background: rgba(51, 112, 255, 0.04);
    border-radius: 0 6px 6px 0;

    p { margin: 12px 0; }
  }

  code {
    padding: 3px 6px;
    border-radius: 5px;
    background: #f0f2f4;
    color: #b42318;
    font-family: 'SF Mono', ui-monospace, 'Menlo', monospace;
    font-size: 0.88em;
  }

  pre {
    position: relative;
    padding: 42px 20px 18px;
    border-radius: 10px;
    background: #1e1f26;
    border: 1px solid #3a3d45;
    box-shadow: 0 6px 18px rgba(16, 18, 24, 0.28);
    color: #d6d9e0;
    overflow-x: auto;
    margin: 20px 0;
    font-size: 13.5px;
    line-height: 1.7;
    /* 顶部工具条（代码框头）：圆点 + 语言标签 */
    background-image:
      radial-gradient(circle at 16px 18px, #ff5f57 0 8px, transparent 9px),
      radial-gradient(circle at 36px 18px, #febc2e 0 8px, transparent 9px),
      radial-gradient(circle at 56px 18px, #28c840 0 8px, transparent 9px);

    code {
      padding: 0;
      background: transparent;
      color: inherit;
      font-size: inherit;
      font-family: 'SF Mono', ui-monospace, 'Menlo', 'Consolas', monospace;
    }

    .code-comment { color: #8b949e; font-style: italic; }
    .code-string { color: #a5d6ff; }
    .code-keyword { color: #ff7b72; }

    /* 语言角标 */
    &::after {
      content: attr(data-lang);
      position: absolute;
      top: 22px;
      right: 16px;
      font-size: 10.5px;
      font-weight: 600;
      letter-spacing: .06em;
      text-transform: uppercase;
      color: #9aa0aa;
      background: rgba(255,255,255,.06);
      border: 1px solid rgba(255,255,255,.1);
      padding: 2px 9px;
      border-radius: 4px;
      font-family: 'SF Mono', ui-monospace, 'Menlo', monospace;
    }
  }

  hr {
    border: 0;
    border-top: 1px solid #e5e6e8;
    margin: 32px 0;
  }

  /* 链接样式 - 指针光标 + hover 效果 */
  a, .md-link {
    color: var(--notes-accent, #3370ff);
    text-decoration: none;
    cursor: pointer;
    transition: color .15s, text-decoration .15s;

    &:hover {
      color: var(--notes-accent, #3370ff);
      text-decoration: underline;
    }

    &:active {
      opacity: 0.7;
    }
  }

  img {
    max-width: 100%;
    border-radius: 10px;
    margin: 12px 0;
  }

  table {
    border-collapse: collapse;
    width: 100%;
    margin: 20px 0;
    border: 1px solid var(--notes-border, #cdd3dc);
    border-radius: 10px;
    overflow: hidden;
    background: var(--notes-tbl-bg, #ffffff);
    box-shadow: 0 1px 3px rgba(20, 24, 30, 0.06);

    th, td {
      border: 1px solid var(--notes-border, #d9dde3);
      padding: 11px 15px;
      text-align: left;
      min-width: 110px;
      line-height: 1.65;
    }

    th {
      background: var(--notes-tbl-head, #f3f5f8);
      font-weight: 700;
      color: var(--notes-fg, #1f2329);
    }

    /* 斑马纹 + hover 行高亮（Office Viewer 观感） */
    tbody tr:nth-child(even) { background: rgba(0, 0, 0, 0.025); }
    tbody tr:hover { background: rgba(51, 112, 255, 0.05); }
  }

  input[type="checkbox"] {
    margin-right: 6px;
    cursor: pointer;
  }
}

/* ─── 响应式 ─────────────────────────────────── */
@media (max-width: 900px) {
  .preview-content,
  .md-textarea {
    padding: 24px 20px;
  }
}
</style>
