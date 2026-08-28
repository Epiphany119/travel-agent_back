import request from './index'

// ─── 类型定义 ────────────────────────────────────────────────────────────

/** 主题设置 */
export interface ThemeSettings {
  bg: string
  fg: string
  accent: string
  fontSize?: number
}

/** 笔记文档 */
export interface NoteDocument {
  id?: number
  userId?: string
  title: string
  destination?: string
  coverUrl?: string
  visibility?: string
  shareToken?: string
  status?: string
  themeJson?: string
  /** 复制来源的社区帖子 ID */
  sourceSocialNoteId?: number
  /** 完整 Markdown 内容 */
  content?: string
  createdAt?: string
  updatedAt?: string
}

type ApiEnvelope<T> = { data: T; message?: string; code?: number }

/** 当前登录用户 ID */
export function getNoteUserId(): string {
  return localStorage.getItem('roamly_user_id') || 'user_001'
}

/** 查询用户笔记列表 */
export async function listNotes(): Promise<NoteDocument[]> {
  const r = await request.get<unknown, ApiEnvelope<NoteDocument[]>>(
    '/notes', { params: { userId: getNoteUserId() } }
  )
  return r.data || []
}

/** 获取单篇笔记完整内容 */
export async function getNote(id: number): Promise<NoteDocument> {
  return (await request.get<unknown, ApiEnvelope<NoteDocument>>(
    `/notes/${id}`, { params: { userId: getNoteUserId() } }
  )).data
}

/** 通过分享 token 查看笔记 */
export async function getSharedNote(token: string): Promise<NoteDocument> {
  return (await request.get<unknown, ApiEnvelope<NoteDocument>>(`/notes/share/${token}`)).data
}

/** 创建笔记 */
export async function createNote(data: NoteDocument): Promise<NoteDocument> {
  const payload = {
    title: data.title,
    destination: data.destination,
    coverUrl: data.coverUrl,
    visibility: data.visibility,
    themeJson: data.themeJson,
    sourceSocialNoteId: data.sourceSocialNoteId,
    content: data.content
  }
  return (await request.post<unknown, ApiEnvelope<NoteDocument>>(
    '/notes', payload, { params: { userId: getNoteUserId() } }
  )).data
}

/** 更新笔记 */
export async function updateNote(id: number, data: NoteDocument): Promise<NoteDocument> {
  const payload = {
    title: data.title,
    destination: data.destination,
    coverUrl: data.coverUrl,
    visibility: data.visibility,
    themeJson: data.themeJson,
    sourceSocialNoteId: data.sourceSocialNoteId,
    content: data.content
  }
  return (await request.put<unknown, ApiEnvelope<NoteDocument>>(
    `/notes/${id}`, payload, { params: { userId: getNoteUserId() } }
  )).data
}

/** 删除笔记 */
export async function deleteNote(id: number): Promise<void> {
  await request.delete(`/notes/${id}`, { params: { userId: getNoteUserId() } })
}

/**
 * 上传笔记图片（jpg/jpeg/png/gif）。
 *
 * <p>图片保存到后端用户本地目录，数据库只记录访问路径；
 * 前端将返回的 URL 以 Markdown 图片语法写入笔记内容。</p>
 *
 * @param file 图片文件
 * @returns 可访问路径，如 /uploads/note/user_001/xxx.jpg
 */
export async function uploadNoteImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const r = await request.post<unknown, ApiEnvelope<{ url: string }>>(
    '/notes/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: { userId: getNoteUserId() }
    }
  )
  return r.data.url
}
