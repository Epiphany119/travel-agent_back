<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadNoteImage } from '@/api/note'
import { renderMarkdown, sanitizeRichHtml } from '@/utils/markdown'

const props = withDefaults(defineProps<{
  modelValue: string
  editable?: boolean
  placeholder?: string
  compact?: boolean
  showToolbar?: boolean
}>(), {
  editable: true,
  placeholder: '写下这段旅程……试试输入 / 插入一个区块',
  compact: false,
  showToolbar: true
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'focus'): void
  (event: 'blur', value: string): void
}>()

const editor = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const focused = ref(false)
const activeColor = ref('#164E42')
const activeHighlight = ref('#FFF0E8')
const activeFontSize = ref('16px')

const textColors = ['#164E42', '#F27A4F', '#5378FF', '#8A5A9B', '#C24A62', '#1D2B27']
const highlightColors = ['#FFF0E8', '#FFF7C7', '#E5F4EC', '#E8EEFF', '#F1E7F7', 'transparent']

function looksLikeHtml(value: string) {
  return /<[a-z][\s\S]*>/i.test(value)
}

function setEditorValue(value: string) {
  if (!editor.value) return
  const source = String(value || '')
  editor.value.innerHTML = source ? sanitizeRichHtml(looksLikeHtml(source) ? source : renderMarkdown(source)) : ''
}

watch(() => props.modelValue, (value) => {
  // 输入过程中不要重置 DOM，否则光标会跳到末尾；父组件保存/切换文档时再同步。
  if (!editor.value || focused.value) return
  const next = String(value || '')
  if (editor.value.innerHTML !== next) setEditorValue(next)
})

onMounted(() => setEditorValue(props.modelValue))

function emitContent() {
  if (!editor.value) return
  emit('update:modelValue', sanitizeRichHtml(editor.value.innerHTML))
}

function normalizeFontTags() {
  if (!editor.value) return
  const sizeMap: Record<string, string> = { '1': '12px', '2': '14px', '3': '16px', '4': '20px', '5': '26px', '6': '32px', '7': '40px' }
  editor.value.querySelectorAll('font[size]').forEach((node) => {
    const font = node as HTMLElement
    const span = document.createElement('span')
    span.style.fontSize = sizeMap[font.getAttribute('size') || '3'] || activeFontSize.value
    while (font.firstChild) span.appendChild(font.firstChild)
    font.replaceWith(span)
  })
}

function run(command: string, value?: string) {
  if (!props.editable || !editor.value) return
  editor.value.focus()
  // execCommand 仍是浏览器原生 contenteditable 最稳定的跨 Chromium 能力；
  // 结果会立即归一化为安全 HTML，再交给统一笔记模型保存。
  try { document.execCommand('styleWithCSS', false, 'true') } catch { /* ignore */ }
  try { document.execCommand(command, false, value) } catch { /* ignore */ }
  normalizeFontTags()
  emitContent()
}

function formatBlock(tag: string) {
  run('formatBlock', tag)
}

function setColor(color: string) {
  activeColor.value = color
  run('foreColor', color)
}

function setHighlight(color: string) {
  activeHighlight.value = color
  if (color === 'transparent') {
    run('removeFormat')
    return
  }
  run('hiliteColor', color)
}

function setFontSize(size: string) {
  activeFontSize.value = size
  const commandSize = ({ '12px': '1', '14px': '2', '16px': '3', '20px': '4', '26px': '5', '32px': '6', '40px': '7' } as Record<string, string>)[size] || '3'
  run('fontSize', commandSize)
}

function insertHtml(html: string) {
  if (!props.editable || !editor.value) return
  editor.value.focus()
  try {
    document.execCommand('insertHTML', false, html)
  } catch {
    editor.value.insertAdjacentHTML('beforeend', html)
  }
  emitContent()
}

function insertDivider() {
  insertHtml('<hr><p><br></p>')
}

function insertCallout() {
  insertHtml('<blockquote><strong>旅行提示</strong><br>把值得记住的细节写在这里。</blockquote><p><br></p>')
}

function insertLink() {
  const url = window.prompt('粘贴链接地址（https://…）')?.trim()
  if (!url || !/^https?:\/\//i.test(url)) {
    if (url) ElMessage.warning('请输入以 http:// 或 https:// 开头的链接')
    return
  }
  const label = window.getSelection()?.toString().trim() || url
  insertHtml(`<a href="${url.replace(/"/g, '&quot;')}" target="_blank" rel="noreferrer">${label.replace(/[<&>]/g, '')}</a>`)
}

function insertImageUrl() {
  const url = window.prompt('粘贴图片地址（https://…）')?.trim()
  if (!url || !/^https?:\/\//i.test(url)) {
    if (url) ElMessage.warning('请输入以 http:// 或 https:// 开头的图片地址')
    return
  }
  insertHtml(`<p><img src="${url.replace(/"/g, '&quot;')}" alt="旅行插图" loading="lazy"><br></p>`)
}

function chooseImage() {
  if (!props.editable) return
  fileInput.value?.click()
}

async function uploadImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return ElMessage.warning('请选择图片文件')
  if (file.size > 10 * 1024 * 1024) return ElMessage.warning('图片不能超过 10MB')
  uploading.value = true
  try {
    const url = await uploadNoteImage(file)
    insertHtml(`<p><img src="${url.replace(/"/g, '&quot;')}" alt="旅行插图" loading="lazy"><br></p>`)
    ElMessage.success('插图已加入笔记')
  } catch (error: any) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function onInput() { emitContent() }
function onFocus() { focused.value = true; emit('focus') }
function onBlur() {
  focused.value = false
  const value = sanitizeRichHtml(editor.value?.innerHTML || '')
  emit('update:modelValue', value)
  emit('blur', value)
}

function onPaste(event: ClipboardEvent) {
  const item = Array.from(event.clipboardData?.items || []).find(entry => entry.type.startsWith('image/'))
  if (!item) return
  event.preventDefault()
  const file = item.getAsFile()
  if (file) void uploadImage({ target: { files: [file], value: '' } } as unknown as Event)
}

function onDrop(event: DragEvent) {
  const file = Array.from(event.dataTransfer?.files || []).find(item => item.type.startsWith('image/'))
  if (!file) return
  event.preventDefault()
  void uploadImage({ target: { files: [file], value: '' } } as unknown as Event)
}

function focus() { editor.value?.focus() }
function getHtml() { return sanitizeRichHtml(editor.value?.innerHTML || '') }

defineExpose({ editor, focus, getHtml })

</script>

<template>
  <section class="rich-editor" :class="{ compact, 'is-readonly': !editable }">
    <div v-if="showToolbar && editable" class="rich-toolbar" role="toolbar" aria-label="笔记格式工具栏">
      <div class="toolbar-cluster">
        <select class="format-select" aria-label="段落样式" @change="formatBlock(($event.target as HTMLSelectElement).value)">
          <option value="p">正文</option>
          <option value="h1">标题 1</option>
          <option value="h2">标题 2</option>
          <option value="h3">标题 3</option>
        </select>
        <select class="format-select size-select" aria-label="字号" :value="activeFontSize" @change="setFontSize(($event.target as HTMLSelectElement).value)">
          <option value="12px">小</option>
          <option value="14px">较小</option>
          <option value="16px">标准</option>
          <option value="20px">大</option>
          <option value="26px">特大</option>
          <option value="32px">标题</option>
        </select>
      </div>

      <span class="toolbar-divider"></span>
      <div class="toolbar-cluster emphasis-tools">
        <button type="button" class="format-btn format-letter" title="加粗" @mousedown.prevent="run('bold')">B</button>
        <button type="button" class="format-btn format-letter italic" title="斜体" @mousedown.prevent="run('italic')">I</button>
        <button type="button" class="format-btn format-letter underline" title="下划线" @mousedown.prevent="run('underline')">U</button>
        <button type="button" class="format-btn" title="删除线" @mousedown.prevent="run('strikeThrough')">S̶</button>
      </div>

      <span class="toolbar-divider"></span>
      <div class="swatch-group" title="文字颜色">
        <span class="swatch-label">A</span>
        <button v-for="color in textColors" :key="color" type="button" class="swatch" :style="{ background: color }" :class="{ selected: activeColor === color }" :title="`文字颜色 ${color}`" @mousedown.prevent="setColor(color)"></button>
      </div>
      <div class="swatch-group" title="高亮颜色">
        <span class="swatch-label marker">▰</span>
        <button v-for="color in highlightColors" :key="color" type="button" class="swatch highlight-swatch" :style="{ background: color === 'transparent' ? 'linear-gradient(135deg, transparent 45%, #c9c2b7 46%, #c9c2b7 54%, transparent 55%)' : color }" :class="{ selected: activeHighlight === color }" :title="`高亮 ${color}`" @mousedown.prevent="setHighlight(color)"></button>
      </div>

      <span class="toolbar-divider"></span>
      <div class="toolbar-cluster">
        <button type="button" class="format-btn" title="无序列表" @mousedown.prevent="run('insertUnorderedList')">☷</button>
        <button type="button" class="format-btn" title="有序列表" @mousedown.prevent="run('insertOrderedList')">≣</button>
        <button type="button" class="format-btn" title="引用" @mousedown.prevent="run('formatBlock', 'blockquote')">❝</button>
        <button type="button" class="format-btn" title="插入分割线" @mousedown.prevent="insertDivider">—</button>
        <button type="button" class="format-btn" title="插入提示卡片" @mousedown.prevent="insertCallout">✦</button>
        <button type="button" class="format-btn" title="插入链接" @mousedown.prevent="insertLink">↗</button>
      </div>

      <div class="toolbar-spacer"></div>
      <input ref="fileInput" type="file" accept="image/*" hidden @change="uploadImage" />
      <button type="button" class="insert-image-btn" :disabled="uploading" title="上传插图" @mousedown.prevent="chooseImage">{{ uploading ? '上传中…' : '＋ 插图' }}</button>
      <button type="button" class="image-url-btn" title="通过 URL 插入图片" @mousedown.prevent="insertImageUrl">URL</button>
    </div>

    <div
      ref="editor"
      class="rich-content"
      :contenteditable="editable"
      :data-placeholder="placeholder"
      spellcheck="false"
      role="textbox"
      aria-multiline="true"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
      @paste="onPaste"
      @drop="onDrop"
      @dragover.prevent
    ></div>
    <div v-if="editable" class="editor-hint"><span>支持图片、颜色、字号与提示卡片</span><span>⌘ / Ctrl + S 保存</span></div>
  </section>
</template>

<style scoped lang="scss">
.rich-editor {
  width: 100%;
  border: 1px solid var(--line, #e7e0d2);
  border-radius: 18px;
  background: var(--card, #fffdf8);
  box-shadow: 0 8px 24px color-mix(in srgb, var(--forest, #164e42) 7%, transparent);
  overflow: hidden;
}

.rich-toolbar {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--line, #e7e0d2);
  background: color-mix(in srgb, var(--paper, #f7f3ea) 72%, var(--card, #fffdf8));
  flex-wrap: wrap;
}

.toolbar-cluster, .swatch-group { display: inline-flex; align-items: center; gap: 3px; }
.toolbar-divider { width: 1px; height: 22px; background: var(--line, #e7e0d2); margin: 0 3px; }
.toolbar-spacer { flex: 1; min-width: 8px; }

.format-select {
  height: 30px;
  border: 1px solid transparent;
  border-radius: 7px;
  padding: 0 7px;
  background: transparent;
  color: var(--ink, #1d2b27);
  font-size: 12px;
  outline: 0;
  cursor: pointer;
  &:hover, &:focus { background: var(--card, #fffdf8); border-color: var(--line, #e7e0d2); }
}
.size-select { width: 58px; }
.format-btn {
  width: 29px; height: 29px; display: grid; place-items: center;
  border: 0; border-radius: 7px; background: transparent; color: var(--ink-2, #5c6b65);
  font-size: 14px; cursor: pointer; transition: background .15s, color .15s, transform .15s;
  &:hover { background: var(--roam-soft, #e9f1ec); color: var(--forest, #164e42); transform: translateY(-1px); }
}
.format-letter { font-weight: 800; font-family: Georgia, serif; }
.italic { font-style: italic; }
.underline { text-decoration: underline; }
.swatch-label { width: 18px; text-align: center; color: var(--ink-2); font-weight: 800; font-size: 14px; }
.swatch-label.marker { font-size: 12px; color: var(--sunset, #f27a4f); }
.swatch { width: 14px; height: 14px; padding: 0; border-radius: 50%; border: 2px solid var(--card); box-shadow: 0 0 0 1px var(--line); cursor: pointer; transition: transform .15s, box-shadow .15s; &:hover, &.selected { transform: scale(1.18); box-shadow: 0 0 0 2px var(--forest); } }
.highlight-swatch { border-radius: 4px; }
.insert-image-btn, .image-url-btn { height: 30px; border: 1px solid var(--forest, #164e42); border-radius: 8px; padding: 0 9px; background: var(--forest, #164e42); color: #fff; font-size: 11px; font-weight: 800; cursor: pointer; &:hover { background: var(--forest-deep, #0e382e); } &:disabled { opacity: .55; cursor: wait; } }
.image-url-btn { background: transparent; color: var(--forest, #164e42); border-color: var(--line); }

.rich-content {
  min-height: 360px;
  padding: 34px clamp(20px, 6vw, 72px) 24px;
  color: var(--ink, #1d2b27);
  font-size: 16px;
  line-height: 1.85;
  outline: 0;
  word-break: break-word;
  &:empty::before { content: attr(data-placeholder); color: var(--ink-3, #8c9993); pointer-events: none; }
  &:focus { background: color-mix(in srgb, var(--card, #fffdf8) 94%, var(--forest, #164e42)); }
  :deep(p) { margin: 0 0 1em; min-height: 1.65em; }
  :deep(h1), :deep(h2), :deep(h3) { color: var(--ink); line-height: 1.3; margin: 1.15em 0 .55em; letter-spacing: -.02em; }
  :deep(h1) { font-size: 32px; }
  :deep(h2) { font-size: 24px; }
  :deep(h3) { font-size: 19px; }
  :deep(strong) { color: var(--forest); }
  :deep(a) { color: var(--forest); text-decoration: underline; text-underline-offset: 3px; }
  :deep(blockquote) { margin: 20px 0; padding: 14px 18px; border-left: 4px solid var(--sunset); border-radius: 0 12px 12px 0; background: var(--sunset-soft); color: var(--ink-2); }
  :deep(ul), :deep(ol) { padding-left: 25px; margin: .5em 0 1em; }
  :deep(li) { margin: .35em 0; }
  :deep(hr) { border: 0; border-top: 2px solid var(--line); margin: 28px 0; }
  :deep(img) { display: block; max-width: 100%; height: auto; border-radius: 14px; margin: 14px 0; box-shadow: 0 8px 24px color-mix(in srgb, var(--forest) 12%, transparent); }
}
.editor-hint { display: flex; justify-content: space-between; gap: 12px; padding: 8px 16px 10px; color: var(--ink-3, #8c9993); font-size: 10px; border-top: 1px solid color-mix(in srgb, var(--line) 60%, transparent); }
.is-readonly .rich-content { min-height: 0; padding: 0; &:focus { background: transparent; } }
.compact .rich-content { min-height: 220px; padding: 20px 28px; font-size: 14px; }

@media (max-width: 760px) {
  .rich-toolbar { gap: 4px; }
  .swatch-group { display: none; }
  .rich-content { padding: 24px 20px; min-height: 300px; }
  .editor-hint { font-size: 9px; }
}
</style>
