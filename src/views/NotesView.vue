<script setup lang="ts">
import { ref, computed, reactive, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadImage } from '@/api/user'
import { marked } from 'marked'
import { listNotes, getNote, createNote, updateNote, deleteNote,
          getNoteUserId, type NoteDocument, type NoteBlock } from '@/api/note'

// ─── 状态 ───────────────────────────────────────────────
const docs = ref<NoteDocument[]>([])
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const currentId = ref<number | null>(null)
const curDoc = reactive<NoteDocument>({ title: '', blocks: [] })

// 鼠标悬停的块索引 - 用于控制工具条显示
const hoveredBlockIndex = ref<number | null>(null)
// 当前聚焦的块索引
const focusedBlockIndex = ref<number | null>(null)
const formatToolbarVisible = ref(false)

// 块 DOM 元素引用
const blockRefs = new Map<number, HTMLElement>()
const mdInput = ref<HTMLInputElement | null>(null)

// ─── 列表加载 ───────────────────────────────────────────
async function load() {
  loading.value = true
  try {
    docs.value = await listNotes()
    if (currentId.value == null && docs.value.length > 0) {
      await openDoc(docs.value[0].id!)
    }
  } catch (e) { console.error(e) } finally { loading.value = false }
}

onMounted(() => load())

function openMarkdownPicker() { mdInput.value?.click() }
function normalizeInlineMarkdown(value: string) { return value.replace(/\*\*([^*]+)\*\*/g, '$1').replace(/__([^_]+)__/g, '$1').replace(/\[([^\]]+)\]\([^)]*\)/g, '$1') }
async function importMarkdown(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const blocks: NoteBlock[] = []
  for (const line of (await file.text()).split(/\r?\n/)) {
    if (!line.trim()) continue
    const image = line.match(/^!\[(.*?)\]\((.*?)\)$/)
    const heading = line.match(/^(#{1,3})\s+(.*)$/)
    const todo = line.match(/^[-*]\s+\[[ xX]\]\s+(.*)$/)
    const list = line.match(/^[-*+]\s+(.*)$/)
    if (image) blocks.push({ type: 'image', text: image[2], attrsJson: JSON.stringify({ alt: image[1] }) })
    else if (heading) blocks.push({ type: heading[1].length === 1 ? 'h1' : 'h2', text: heading[2] })
    else if (todo) blocks.push({ type: 'todo', text: todo[1] })
    else if (list) blocks.push({ type: 'list', text: list[1] })
    else blocks.push({ type: 'p', text: normalizeInlineMarkdown(line.replace(/\[\[([^|\]]+)(?:\|([^\]]+))?\]\]/g, '$2 ($1)')) })
  }
  curDoc.title = file.name.replace(/\.md$/i, '')
  curDoc.blocks = blocks.length ? blocks : [emptyBlock()]
  if (currentId.value == null) {
    try {
      const created = await createNote({ title: curDoc.title, blocks: curDoc.blocks, visibility: 'private' })
      currentId.value = created.id || null
      curDoc.id = created.id
      curDoc.userId = created.userId
      docs.value.unshift(created)
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '导入保存失败')
      return
    }
  } else {
    try {
      await updateNote(currentId.value, { ...curDoc, blocks: curDoc.blocks })
      docs.value = await listNotes()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '导入保存失败')
      return
    }
  }
  await nextTick(); setAllBlockContents(); ElMessage.success('Markdown 已导入并保存')
  ;(event.target as HTMLInputElement).value = ''
}
async function insertImage(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]
  if (!file) return
  try { const result = await uploadImage(file, 'note'); const url = result.data?.url; if (!url) throw new Error('上传未返回图片地址'); (curDoc.blocks as NoteBlock[]).push({ type: 'image', text: url, attrsJson: JSON.stringify({ alt: file.name }) }); await nextTick(); setAllBlockContents(); ElMessage.success('图片已插入') } catch (e: any) { ElMessage.error(e?.response?.data?.message || e?.message || '图片上传失败') } finally { input.value = '' }
}
function handleEditorClick(event: MouseEvent) { const link = (event.target as HTMLElement).closest('[data-note-id]') as HTMLElement | null; if (link) { event.preventDefault(); router.push('/notes/' + link.dataset.noteId) } }

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
    curDoc.blocks = (d.blocks && d.blocks.length ? d.blocks : [emptyBlock()])
    // 下一轮设置 DOM 内容
    nextTick(() => {
      setAllBlockContents()
    })
  } catch (e: any) { ElMessage.error(e?.message || '打开失败') }
}

// 初始化时设置所有块的 DOM 内容
function setAllBlockContents() {
  curDoc.blocks?.forEach((b, i) => {
    const el = blockRefs.get(i)
    if (el && el.querySelector('.editable')) {
      ;(el.querySelector('.editable') as HTMLElement).innerHTML = renderMarkdownInline(b.text || '')
    }
  })
}

// 设置单个块的 DOM 内容
function setBlockContent(index: number, text: string) {
  const el = blockRefs.get(index)
  if (el) {
    const editable = el.querySelector('.editable') as HTMLElement
    if (editable && editable.innerText !== text) {
      editable.innerHTML = renderMarkdownInline(text)
    }
  }
}

function renderMarkdownInline(source: string) {
  const links: string[] = []
  const withWikiLinks = source.replace(/\[\[([^|\]]+)(?:\|([^\]]+))?\]\]/g, (_match, id, label) => {
    const index = links.push('<a class="note-link" data-note-id="' + String(id).replace(/"/g, '') + '">' + (label || id) + '</a>') - 1
    return '@@WIKILINK' + index + '@@'
  })
  let html = String(marked.parseInline(withWikiLinks, { async: false }))
  links.forEach((link, index) => { html = html.replace('@@WIKILINK' + index + '@@', link) })
  return html
}

// ─── 新增笔记 ───────────────────────────────────────────
async function addDoc() {
  try {
    const d = await createNote({ title: '新笔记', blocks: [emptyBlock()], visibility: 'private' })
    docs.value.unshift(d)
    await openDoc(d.id!)
  } catch (e: any) { ElMessage.error(e?.message || '创建失败') }
}

// ─── 保存 ───────────────────────────────────────────────
async function saveDoc() {
  if (currentId.value == null) { await addDoc(); return }
  saving.value = true
  try {
    // 从 DOM 读取最新文本
    const blocksFromDom = curDoc.blocks?.map((b, idx) => {
      const el = blockRefs.get(idx)
      let text = b.text
      if (el) {
        const editable = el.querySelector('.editable') as HTMLElement
        if (editable) {
          text = editable.innerText
        }
      }
      return { ...b, text }
    }) || []

    const cleaned = { 
      ...curDoc, 
      blocks: blocksFromDom
        .filter(b => !(b.type === 'p' && !b.text?.trim()))
    }

    const d = await updateNote(currentId.value, cleaned)
    ElMessage.success('已保存')
    // 更新本地数据
    curDoc.blocks = cleaned.blocks
    // 刷新列表
    docs.value = (await listNotes())
  } catch (e: any) { 
    console.error('Save failed:', e)
    ElMessage.error(e?.message || '保存失败') 
  } finally { saving.value = false }
}

// ─── 删除 ───────────────────────────────────────────────
async function removeDoc() {
  if (currentId.value == null) return
  try {
    await ElMessageBox.confirm('确定删除这篇笔记吗？', '删除笔记', { type: 'warning' })
    await deleteNote(currentId.value)
    ElMessage.success('已删除')
    currentId.value = null
    curDoc.blocks = []
    curDoc.title = ''
    await load()
  } catch (e) {}
}

// ─── 块操作 ─────────────────────────────────────────────
function emptyBlock(type = 'p'): NoteBlock { return { type: type as any, text: '' } }

function addBlockAfter(index: number, type = 'p') {
  // 先从 DOM 保存当前块内容
  saveBlockFromDom(index)
  
  const list = curDoc.blocks as NoteBlock[]
  list.splice(index + 1, 0, emptyBlock(type))
  
  // DOM 更新后设置新块内容并聚焦
  nextTick(() => {
    setAllBlockContents()
    const newEl = blockRefs.get(index + 1)
    const editable = newEl?.querySelector('.editable') as HTMLElement
    editable?.focus()
  })
}

function addBlockAtTop(type = 'h1') {
  const list = curDoc.blocks as NoteBlock[]
  list.unshift(emptyBlock(type))
}

function removeBlock(index: number) {
  const list = curDoc.blocks as NoteBlock[]
  if (list.length <= 1) { 
    list[0] = emptyBlock()
    nextTick(() => setAllBlockContents())
    return 
  }
  list.splice(index, 1)
  nextTick(() => {
    setAllBlockContents()
    const prevIdx = Math.max(0, index - 1)
    const prevEl = blockRefs.get(prevIdx)
    const editable = prevEl?.querySelector('.editable') as HTMLElement
    editable?.focus()
  })
}

function moveBlock(index: number, dir: -1 | 1) {
  saveBlockFromDom(index)
  const list = curDoc.blocks as NoteBlock[]
  const to = index + dir
  if (to < 0 || to >= list.length) return
  ;[list[index], list[to]] = [list[to], list[index]]
  nextTick(() => setAllBlockContents())
}

function changeBlockType(index: number, type: string) {
  saveBlockFromDom(index)
  const list = curDoc.blocks as NoteBlock[]
  if (list[index]) {
    list[index] = { ...list[index], type: type as any }
  }
  nextTick(() => {
    setAllBlockContents()
    // 保持聚焦
    const el = blockRefs.get(index)
    const editable = el?.querySelector('.editable') as HTMLElement
    if (editable) {
      editable.focus()
      // 恢复光标到末尾
      const range = document.createRange()
      range.selectNodeContents(editable)
      range.collapse(false)
      const sel = window.getSelection()
      sel?.removeAllRanges()
      sel?.addRange(range)
    }
  })
}

// 从 DOM 保存块文本到响应式数据
function saveBlockFromDom(index: number) {
  const el = blockRefs.get(index)
  if (el && curDoc.blocks?.[index]) {
    const editable = el.querySelector('.editable') as HTMLElement
    if (editable) {
      curDoc.blocks[index].text = editable.innerText
    }
  }
}

// 处理键盘事件
function handleKeydown(index: number, e: KeyboardEvent) {
  // Enter 键 - 创建新块
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    saveBlockFromDom(index)
    
    const block = curDoc.blocks?.[index]
    // 列表/待办且为空时转为段落
    if (block && (block.type === 'list' || block.type === 'todo') && !block.text?.trim()) {
      changeBlockType(index, 'p')
      return
    }
    // 继承列表/待办类型
    const newType = (block?.type === 'list' || block?.type === 'todo') ? block.type : 'p'
    addBlockAfter(index, newType)
    return
  }
  
  // Backspace 在空块上
  if (e.key === 'Backspace') {
    const el = blockRefs.get(index)
    if (!el) return
    const editable = el.querySelector('.editable') as HTMLElement
    if (editable && !editable.innerText) {
      const block = curDoc.blocks?.[index]
      if (block && block.type !== 'p') {
        e.preventDefault()
        changeBlockType(index, 'p')
      } else if (curDoc.blocks && curDoc.blocks.length > 1) {
        e.preventDefault()
        removeBlock(index)
      }
    }
  }
}

// 处理 input 事件 - 只保存到内部状态，不触发重渲染
function handleInput(index: number, e: Event) {
  const el = e.target as HTMLElement
  if (curDoc.blocks?.[index]) {
    curDoc.blocks[index].text = el.innerText
  }
}

// 处理 focus 事件
function handleFocus(index: number) {
  focusedBlockIndex.value = index
}

// 处理 blur 事件
function handleBlur(index: number) {
  saveBlockFromDom(index)
  focusedBlockIndex.value = null
}

// 鼠标进入块 - 显示工具条
function handleMouseEnter(index: number) {
  hoveredBlockIndex.value = index
}

// 鼠标离开块 - 隐藏工具条（如果没有聚焦）
function handleMouseLeave(index: number) {
  if (focusedBlockIndex.value !== index) {
    hoveredBlockIndex.value = null
  }
}

// 注册块 DOM 引用
function registerBlockRef(index: number, el: HTMLElement | null) {
  if (el) {
    blockRefs.set(index, el)
  } else {
    blockRefs.delete(index)
  }
}

// 监听 blocks 变化，清理旧引用
watch(() => curDoc.blocks?.length, () => {
  // 重新注册所有 ref
  nextTick(() => {
    setAllBlockContents()
  })
})

onBeforeUnmount(() => {
  blockRefs.clear()
})
</script>

<template>
  <main class="notes-page">
    <!-- 左：笔记列表 -->
    <aside class="list-pane">
      <div class="workspace-head"><div class="workspace-logo">R</div><div><b>Roamly 工作台</b><small>个人空间</small></div><span class="workspace-more">⌄</span></div>
      <div class="list-head">
        <div class="quick-row"><span class="eyebrow">旅行笔记</span><button class="icon-btn" title="新建笔记" @click="addDoc">＋</button></div>
        <button class="new-btn" @click="addDoc">＋ 新建笔记</button>
      </div>
      <nav class="side-nav"><span class="side-nav-item active">▤ <span>我的笔记</span></span><span class="side-nav-item">☆ <span>收藏的笔记</span></span><span class="side-nav-item">⌁ <span>与我共享</span></span></nav>
      <div class="note-ul" v-loading="loading">
        <button
          v-for="d in docs"
          :key="d.id"
          class="note-item"
          :class="{ active: currentId === d.id }"
          @click="openDoc(d.id!)"
        >
          <b>{{ d.title || '未命名笔记' }}</b>
          <small v-if="d.destination">📍 {{ d.destination }}</small>
        </button>
        <p v-if="!loading && docs.length === 0" class="empty-list">还没有笔记，点右上角新建。</p>
      </div>
      <div class="list-foot"><span class="user-dot">{{ getNoteUserId().slice(-1) }}</span><span>{{ getNoteUserId() }}</span><span class="foot-more">···</span></div>
    </aside>

    <!-- 右：编辑器 -->
    <section class="editor-pane">
      <!-- 顶部工具条 -->
      <header class="tb" v-if="curDoc.blocks?.length">
        <div class="breadcrumbs"><span>我的笔记</span><i>/</i><b>{{ curDoc.title || '未命名笔记' }}</b></div>
        <div class="tb-actions">
          <input ref="mdInput" type="file" accept=".md,text/markdown" hidden @change="importMarkdown" />
          <input id="note-image-input" type="file" accept="image/*" hidden @change="insertImage" />
          <button class="toolbar-btn" title="显示格式工具" :class="{ selected: formatToolbarVisible }" @click="formatToolbarVisible = !formatToolbarVisible">格式</button>
          <button class="toolbar-btn" title="导入 Markdown" @click="openMarkdownPicker">导入 MD</button>
          <label class="toolbar-btn" for="note-image-input" title="上传图片">图片</label>
          <span class="save-state">{{ saving ? '保存中…' : '已保存' }}</span><button class="ghost" title="删除笔记" @click="removeDoc">⌫</button>
          <button class="share-btn">分享</button><button class="primary" :disabled="saving" @click="saveDoc">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </header>

      <div class="editor-scroll" v-if="curDoc.blocks?.length">
        <div class="doc" @click="handleEditorClick">
          <div class="doc-meta"><span class="doc-badge">旅行笔记</span><span>最后编辑于刚刚</span></div>
          <!-- 标题 -->
          <input
            v-model="curDoc.title"
            class="doc-title"
            placeholder="标题"
          />

          <!-- 内容块 -->
          <div
            v-for="(b, i) in curDoc.blocks"
            :key="'blk' + i"
            :data-block-index="i"
            class="block-wrapper"
            :ref="(el) => registerBlockRef(i, el as HTMLElement | null)"
          >
            <!-- 左侧工具条 -->
            <div 
              class="block-tools" 
              :class="{ visible: formatToolbarVisible && focusedBlockIndex === i }"
            >
              <button class="tool-btn" title="转为标题1" @click="changeBlockType(i, 'h1')">H1</button>
              <button class="tool-btn" title="转为标题2" @click="changeBlockType(i, 'h2')">H2</button>
              <button class="tool-btn" title="转为正文" @click="changeBlockType(i, 'p')">P</button>
              <button class="tool-btn" title="转为清单" @click="changeBlockType(i, 'list')">•</button>
              <button class="tool-btn" title="转为待办" @click="changeBlockType(i, 'todo')">☐</button>
              <div class="tool-divider"></div>
              <button class="tool-btn arrow" title="上移" @click="moveBlock(i, -1)">▲</button>
              <button class="tool-btn arrow" title="下移" @click="moveBlock(i, 1)">▼</button>
              <button class="tool-btn danger" title="删除" @click="removeBlock(i)">✕</button>
            </div>

            <!-- 块内容 - 不使用 Vue 文本插值，用 DOM 直接管理 -->
            <div class="block-content">
              <!-- 标题1 -->
              <template v-if="b.type === 'h1'">
                <h1
                  class="editable h1"
                  contenteditable="true"
                  @input="handleInput(i, $event)"
                  @keydown="handleKeydown(i, $event)"
                  @focus="handleFocus(i)"
                  @blur="handleBlur(i)"
                ></h1>
              </template>

              <!-- 标题2 -->
              <template v-else-if="b.type === 'h2'">
                <h2
                  class="editable h2"
                  contenteditable="true"
                  @input="handleInput(i, $event)"
                  @keydown="handleKeydown(i, $event)"
                  @focus="handleFocus(i)"
                  @blur="handleBlur(i)"
                ></h2>
              </template>

              <!-- 清单 -->
              <template v-else-if="b.type === 'list'">
                <div class="list-item">
                  <span class="list-bullet">•</span>
                  <div
                    class="editable list-text"
                    contenteditable="true"
                    @input="handleInput(i, $event)"
                    @keydown="handleKeydown(i, $event)"
                    @focus="handleFocus(i)"
                    @blur="handleBlur(i)"
                  ></div>
                </div>
              </template>

              <!-- 待办 -->
              <template v-else-if="b.type === 'todo'">
                <div class="todo-row">
                  <input type="checkbox" class="todo-check" />
                  <div
                    class="editable todo-text"
                    contenteditable="true"
                    @input="handleInput(i, $event)"
                    @keydown="handleKeydown(i, $event)"
                    @focus="handleFocus(i)"
                    @blur="handleBlur(i)"
                  ></div>
                </div>
              </template>

              <template v-else-if="b.type === 'image'">
                <figure class="image-block"><img :src="b.text" :alt="b.attrsJson || '旅行图片'" /><figcaption contenteditable="true">点击图片说明</figcaption></figure>
              </template>

              <!-- 正文 -->
              <template v-else>
                <p
                  class="editable p"
                  contenteditable="true"
                  @input="handleInput(i, $event)"
                  @keydown="handleKeydown(i, $event)"
                  @focus="handleFocus(i)"
                  @blur="handleBlur(i)"
                ></p>
              </template>
            </div>

          </div>
        </div>
      </div>
      <p v-else class="placeholder-xl">选择或创建一篇笔记开始编辑</p>
    </section>
  </main>
</template>

<style scoped lang="scss">
.notes-page {
  display: flex;
  height: 100vh;
  min-height: 0;
  background: #fff;
}

/* 左列表 */
.list-pane {
  width: 270px;
  flex-shrink: 0;
  border-right: 1px solid #e8e4de;
  display: flex;
  flex-direction: column;
  background: #faf8f4;
}
.list-head {
  padding: 22px 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.eyebrow {
  color: #f27a4f;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .16em;
}
.new-btn {
  border: 0;
  background: #1d4d40;
  color: #fff;
  padding: 10px 12px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: background .15s;
}
.new-btn:hover { background: #163d33; }
.note-ul {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.note-item {
  text-align: left;
  background: #fff;
  border: 1px solid transparent;
  padding: 11px 13px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 3px;
  transition: all .15s;
}
.note-item:hover { border-color: #e8e4de; }
.note-item.active {
  background: #fff;
  border-color: #f27a4f;
  box-shadow: 0 2px 8px rgba(0,0,0,.05);
}
.note-item b { font-size: 13px; color: #2f3a35; }
.note-item small { font-size: 11px; color: #999; }
.empty-list { color: #aaa; font-size: 13px; padding: 20px 4px; }
.list-foot {
  padding: 12px 18px;
  border-top: 1px solid #e8e4de;
  color: #aaa;
  font-size: 11px;
}

/* 右编辑器 */
.editor-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部工具栏 */
.tb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 34px;
  border-bottom: 1px solid #e8e4de;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 5;
}
.tb-title {
  font-weight: 700;
  color: #2f3a35;
  max-width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-actions { display: flex; gap: 10px; }
.ghost {
  border: 1px solid #e8e4de;
  background: #fff;
  padding: 8px 14px;
  border-radius: 9px;
  cursor: pointer;
  font-weight: 600;
  color: #2f3a35;
  transition: all .15s;
}
.ghost:hover { background: #f5f5f5; }
.primary {
  border: 0;
  background: #1d4d40;
  color: #fff;
  padding: 9px 18px;
  border-radius: 9px;
  font-weight: 700;
  cursor: pointer;
  transition: background .15s;
}
.primary:hover:not(:disabled) { background: #163d33; }
.primary:disabled { opacity: .6; cursor: not-allowed; }

/* 编辑器滚动区域 - 关键：允许水平溢出显示工具条 */
.editor-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: visible;
  padding: 40px 160px 120px 160px;
}
.doc {
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
}
.doc-title {
  width: 100%;
  border: 0;
  outline: none;
  font: 40px 'DM Serif Display', 'Noto Sans SC', serif;
  color: #2f3a35;
  border-bottom: 1px solid transparent;
  padding: 6px 0 16px;
  margin-bottom: 16px;
  background: transparent;
}
.doc-title:focus { border-bottom-color: #e8e4de; }

/* 块包装器 */
.block-wrapper {
  position: relative;
  margin-bottom: 4px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

/* 左侧工具条 - 悬停或聚焦时显示 */
.block-tools {
  position: absolute;
  left: -130px;
  top: 4px;
  display: flex;
  gap: 2px;
  opacity: 0;
  transform: translateX(-8px);
  transition: opacity .15s ease, transform .15s ease;
  background: #fff;
  border: 1px solid #e8e4de;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 4px 14px rgba(0,0,0,.08);
  z-index: 10;
  pointer-events: none;
}
.block-tools.visible {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}
.tool-btn {
  border: 0;
  background: transparent;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 7px;
  border-radius: 5px;
  cursor: pointer;
  color: #666;
  white-space: nowrap;
  transition: all .12s;
}
.tool-btn:hover {
  background: #f0f0ee;
  color: #2f3a35;
}
.tool-btn.arrow { color: #999; }
.tool-btn.danger:hover { background: #fee; color: #c33; }
.tool-divider {
  width: 1px;
  height: 14px;
  background: #e8e4de;
  margin: 0 2px;
  align-self: center;
}

/* 块内容区 */
.block-content {
  flex: 1;
  min-width: 0;
}

/* 添加块按钮 */
.add-block-btn {
  position: absolute;
  right: -28px;
  top: 6px;
  border: 0;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  padding: 4px;
  opacity: 0;
  transition: all .15s;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}
.add-block-btn.visible { opacity: 1; }
.add-block-btn:hover { color: #1d4d40; background: #f0f0ee; }

/* 可编辑区域基础样式 */
.editable {
  outline: none;
  min-height: 1.6em;
}

/* 标题样式 */
.h1 {
  font: 34px 'DM Serif Display', 'Noto Sans SC', serif;
  color: #2f3a35;
  font-weight: 800;
  margin: 22px 0 6px;
  line-height: 1.3;
}
.h2 {
  font: 24px 'DM Serif Display', 'Noto Sans SC', serif;
  color: #2f3a35;
  font-weight: 700;
  margin: 18px 0 4px;
  line-height: 1.4;
}

/* 正文样式 */
.p {
  font-size: 15px;
  line-height: 1.8;
  color: #2f3a35;
  margin: 2px 0;
  min-height: 1.6em;
}

/* 清单样式 */
.list-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 2px 0;
}
.list-bullet {
  color: #f27a4f;
  font-weight: 800;
  font-size: 18px;
  line-height: 1.6;
  flex-shrink: 0;
}
.list-text {
  font-size: 15px;
  line-height: 1.8;
  color: #2f3a35;
  flex: 1;
  min-height: 1.6em;
}

/* 待办样式 */
.todo-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 2px 0;
  font-size: 15px;
  color: #2f3a35;
}
.todo-check {
  margin-top: 5px;
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1d4d40;
}
.todo-text {
  line-height: 1.8;
  flex: 1;
  min-height: 1.6em;
}

/* 空状态 */
.placeholder-xl {
  color: #bbb;
  text-align: center;
  padding-top: 80px;
  font-size: 15px;
}

/* 滚动条美化 */
.editor-scroll::-webkit-scrollbar {
  width: 6px;
}
.editor-scroll::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}
.editor-scroll::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}

.note-ul::-webkit-scrollbar {
  width: 4px;
}
.note-ul::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

/* Feishu-inspired workspace chrome */
.notes-page { background: #f5f6f7; color: #1f2329; }
.list-pane { width: 264px; background: #f7f8fa; border-right-color: #e5e6e8; }
.workspace-head { height: 62px; display:flex; align-items:center; gap:10px; padding:0 16px; border-bottom:1px solid #e5e6e8; color:#1f2329; }
.workspace-head b { display:block; font-size:14px; font-weight:650; }.workspace-head small { display:block; color:#8f959e; font-size:11px; margin-top:3px; }.workspace-logo { width:30px; height:30px; border-radius:7px; display:grid; place-items:center; background:#3370ff; color:#fff; font-weight:800; }.workspace-more { margin-left:auto; color:#8f959e; }
.list-head { padding:18px 14px 10px; gap:10px; }.quick-row { display:flex; align-items:center; justify-content:space-between; padding:0 4px; }.icon-btn { border:0; background:transparent; color:#646a73; font-size:20px; cursor:pointer; }.new-btn { border-radius:6px; background:#3370ff; padding:9px 12px; font-size:13px; }.new-btn:hover { background:#2864e5; }
.side-nav { padding:0 10px 12px; border-bottom:1px solid #e5e6e8; }.side-nav-item { display:flex; gap:9px; align-items:center; padding:8px 10px; color:#646a73; font-size:13px; border-radius:5px; }.side-nav-item.active { background:#e8f0ff; color:#245bdb; font-weight:600; }
.note-ul { padding:12px 10px; gap:2px; }.note-item { border:0; border-radius:6px; padding:10px 11px; background:transparent; }.note-item:hover { background:#eef0f2; border:0; }.note-item.active { background:#e8f0ff; border:0; box-shadow:none; }.note-item b { color:#1f2329; font-size:13px; font-weight:550; }.note-item small { color:#8f959e; }
.list-foot { padding:12px 14px; border-top-color:#e5e6e8; display:flex; align-items:center; gap:8px; }.user-dot { width:22px; height:22px; border-radius:50%; display:grid; place-items:center; background:#d8e5ff; color:#245bdb; font-size:11px; }.foot-more { margin-left:auto; }
.editor-pane { background:#fff; }.tb { height:52px; box-sizing:border-box; padding:0 24px; border-bottom-color:#e5e6e8; }.breadcrumbs { display:flex; align-items:center; gap:10px; color:#8f959e; font-size:13px; }.breadcrumbs i { font-style:normal; color:#c5c7ca; }.breadcrumbs b { color:#1f2329; font-weight:550; }.tb-actions { align-items:center; }.save-state { color:#8f959e; font-size:12px; }.ghost,.share-btn,.primary { padding:7px 12px; border-radius:6px; font-size:13px; }.ghost { border:0; background:transparent; color:#646a73; }.ghost:hover { background:#f0f1f2; }.share-btn { border:1px solid #d8dade; background:#fff; color:#1f2329; }.primary { background:#3370ff; }.primary:hover:not(:disabled) { background:#2864e5; }
.toolbar-btn { border:1px solid #d8dade; border-radius:6px; padding:6px 10px; color:#646a73; background:#fff; font-size:12px; cursor:pointer; }.toolbar-btn:hover,.toolbar-btn.selected { color:#245bdb; border-color:#9bb8ff; background:#f4f7ff; }
.editor-scroll { padding:42px 120px 120px; background:#fff; }.doc { max-width:860px; }.doc-meta { display:flex; align-items:center; gap:10px; color:#8f959e; font-size:12px; margin-bottom:18px; }.doc-badge { padding:3px 8px; border-radius:4px; background:#e8f0ff; color:#245bdb; }.doc-title { font-family:'Noto Sans SC',sans-serif; font-size:36px; font-weight:700; color:#1f2329; padding-bottom:14px; margin-bottom:22px; }.h1 { font-family:'Noto Sans SC',sans-serif; color:#1f2329; font-size:27px; font-weight:700; }.h2 { font-family:'Noto Sans SC',sans-serif; color:#1f2329; font-size:20px; font-weight:650; }.p,.list-text,.todo-text { color:#4e5969; font-size:15px; line-height:1.85; }.block-wrapper { margin-bottom:6px; }.block-tools { left:-154px; border-color:#d8dade; box-shadow:0 4px 16px rgba(31,35,41,.12); }.tool-btn:hover { background:#e8f0ff; color:#245bdb; }.add-block-btn:hover { color:#3370ff; background:#e8f0ff; }
.add-block-btn { display:none !important; }.image-block { margin:14px 0; }.image-block img { display:block; max-width:100%; max-height:460px; border-radius:8px; object-fit:contain; background:#f5f6f7; }.image-block figcaption { color:#8f959e; font-size:12px; margin-top:7px; outline:none; }
.p,.list-text,.todo-text { color:#27313d; }
.editable strong { color:#17202b; font-weight:750; }.editable em { font-style:italic; }.editable code { padding:2px 5px; border-radius:4px; background:#f0f2f4; color:#b42318; font-family:ui-monospace,monospace; font-size:.92em; }.editable a,.note-link { color:#245bdb; text-decoration:underline; cursor:pointer; }
@media (max-width: 900px) { .list-pane { width:220px; }.editor-scroll { padding:32px 56px 90px; } .block-tools { left:-44px; top:-30px; } }
@media (max-width: 640px) { .list-pane { width:68px; }.workspace-head { padding:0 18px; }.workspace-head>div:not(.workspace-logo),.workspace-more,.list-head .eyebrow,.new-btn,.side-nav-item span,.note-item b,.note-item small,.list-foot span:not(.user-dot) { display:none; }.list-head { align-items:center; }.note-ul { padding:10px 8px; }.note-item { min-height:38px; overflow:hidden; }.editor-scroll { padding:24px 22px 80px; }.tb { padding:0 14px; }.breadcrumbs span,.breadcrumbs i { display:none; }.save-state,.share-btn { display:none; }.doc-title { font-size:30px; } }
</style>
