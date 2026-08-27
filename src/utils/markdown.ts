import DOMPurify from 'dompurify'
import { marked } from 'marked'

const RICH_HTML_CONFIG = {
  ADD_TAGS: ['img', 'hr', 'figure', 'figcaption', 'mark', 'u', 's', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
  ADD_ATTR: ['style', 'width', 'height', 'loading', 'draggable', 'target', 'rel', 'data-note-id', 'data-link'],
  ALLOW_DATA_ATTR: true
}

/** 统一的富文本安全边界，编辑器预览、社区帖子和复制内容共用。 */
export function sanitizeRichHtml(value: string | undefined | null): string {
  if (!value) return ''
  return DOMPurify.sanitize(String(value), RICH_HTML_CONFIG)
}

export function renderMarkdown(value: string | undefined | null): string {
  if (!value) return ''
  return sanitizeRichHtml(marked.parse(value) as string)
}
