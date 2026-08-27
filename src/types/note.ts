/**
 * Roamly 的统一笔记模型。
 *
 * 不同来源（手写笔记、社区帖子、Agent 计划、灵感目的地）最终都可以
 * 转换为这个结构。后端目前仍保留各自的表，前端用 version/blocks 做
 * 迁移层，未来切换存储实现时不需要重写编辑器。
 */
export type NoteOrigin = 'manual' | 'community' | 'agent' | 'inspiration' | 'journey' | 'copy'

export interface NoteBlock {
  id: string
  type: 'rich-text' | 'image' | 'divider' | 'callout'
  html?: string
  text?: string
  attrs?: Record<string, string | number | boolean>
}

export interface UnifiedNoteDocument {
  version: 1
  format: 'html'
  title: string
  destination: string
  coverUrl: string
  origin: NoteOrigin
  blocks: NoteBlock[]
  meta?: Record<string, unknown>
}

const DEFAULT_CONTENT = '<p>从这里开始记录你的旅程。</p>'

export function contentToDocument(
  content: string | undefined | null,
  meta: Partial<Pick<UnifiedNoteDocument, 'title' | 'destination' | 'coverUrl' | 'origin'>> = {}
): UnifiedNoteDocument {
  const value = String(content || '').trim() || DEFAULT_CONTENT
  return {
    version: 1,
    format: 'html',
    title: meta.title || '未命名笔记',
    destination: meta.destination || '',
    coverUrl: meta.coverUrl || '',
    origin: meta.origin || 'manual',
    blocks: [{ id: 'body', type: 'rich-text', html: value }]
  }
}

/** 把统一模型编码为 travel_note.content_json。 */
export function serializeNoteDocument(document: UnifiedNoteDocument): string {
  return JSON.stringify(document)
}

/** 兼容旧版 Agent 结构和空/损坏 JSON，返回可编辑的 HTML 内容。 */
export function contentFromDocument(value: unknown, fallback = ''): string {
  if (!value) return fallback
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (parsed && typeof parsed === 'object') {
      const data = parsed as any
      if (Array.isArray(data.blocks)) {
        const html = data.blocks
          .map((block: any) => block?.html || block?.text || '')
          .filter(Boolean)
          .join('')
        if (html) return html
      }
      // 旧版 Agent 计划：保留结构化字段，交给调用方的 markdown fallback。
    }
  } catch { /* 旧数据可能本来就是 Markdown/纯文本 */ }
  return fallback
}

export function tagsFromValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String).map(s => s.trim()).filter(Boolean)
  const text = String(value ?? '').trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) return parsed.map(String).map(s => s.trim()).filter(Boolean)
  } catch { /* 逗号分隔格式 */ }
  return text.split(/[,，]/).map(s => s.trim()).filter(Boolean)
}

export function plainTextFromHtml(value: string | undefined | null): string {
  if (!value) return ''
  const el = document.createElement('div')
  el.innerHTML = value
  return (el.textContent || '').replace(/\s+/g, ' ').trim()
}

