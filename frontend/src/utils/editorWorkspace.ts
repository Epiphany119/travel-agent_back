/**
 * 本地源文件工作区。
 *
 * 这里故意不复用 note_document 的 API：数据库笔记和本地文件编辑是两条
 * 完全不同的数据流。浏览器不能读取真实绝对路径，因此 sourcePath 只保存
 * 浏览器能够提供的路径标签（通常是文件名或相对路径），真实文件句柄单独
 * 放在 IndexedDB 中，快照元数据和文本放在 localStorage 中。
 */

export const DATABASE_WORKSPACE_ID = 'database-notes'
const STORAGE_KEY = 'roamly_editor_workspaces_v1'

export interface EditorWorkspaceFile {
  id: string
  name: string
  /** 浏览器可见的路径标签；不是数据库路径，也不保证是绝对路径。 */
  sourcePath: string
  /** 文件内容的归属：本地源文件只保存快照，数据库文件保存到 note_document。 */
  storage?: 'local' | 'database'
  /** 数据库文件对应的 note_document ID；本地文件没有这个字段。 */
  documentId?: number
  size: number
  lastModified: number
  snapshot: string
  updatedAt: string
}

export interface EditorWorkspace {
  id: string
  name: string
  createdAt: string
  updatedAt: string
  files: EditorWorkspaceFile[]
}

export interface EditorWorkspaceState {
  workspaces: EditorWorkspace[]
  activeWorkspaceId: string
  activeFileId: string | null
  localFileMode: boolean
  expanded: boolean
  /** 当前 Markdown 文件的编辑视图；刷新后恢复，避免视图突然跳回默认模式。 */
  editorMode?: 'rendered' | 'source' | 'collab'
}

function now() {
  return new Date().toISOString()
}

function defaultWorkspace(): EditorWorkspace {
  const timestamp = now()
  return {
    id: DATABASE_WORKSPACE_ID,
    name: '我的笔记',
    createdAt: timestamp,
    updatedAt: timestamp,
    files: []
  }
}

function normalizeFile(value: unknown): EditorWorkspaceFile | null {
  if (!value || typeof value !== 'object') return null
  const item = value as Partial<EditorWorkspaceFile>
  if (!item.id || !item.name) return null
  return {
    id: String(item.id),
    name: String(item.name),
    sourcePath: String(item.sourcePath || item.name),
    storage: item.storage === 'database' ? 'database' : 'local',
    documentId: Number.isFinite(Number(item.documentId)) ? Number(item.documentId) : undefined,
    size: Number.isFinite(Number(item.size)) ? Number(item.size) : 0,
    lastModified: Number.isFinite(Number(item.lastModified)) ? Number(item.lastModified) : 0,
    snapshot: typeof item.snapshot === 'string' ? item.snapshot : '',
    updatedAt: String(item.updatedAt || now())
  }
}

function normalizeWorkspace(value: unknown): EditorWorkspace | null {
  if (!value || typeof value !== 'object') return null
  const item = value as Partial<EditorWorkspace>
  if (!item.id || !item.name) return null
  return {
    id: String(item.id),
    name: String(item.name),
    createdAt: String(item.createdAt || now()),
    updatedAt: String(item.updatedAt || now()),
    files: Array.isArray(item.files)
      ? item.files.map(normalizeFile).filter((file): file is EditorWorkspaceFile => Boolean(file))
      : []
  }
}

/** 读取本地工作区；解析失败时只回退本地状态，不影响数据库笔记。 */
export function loadEditorWorkspaceState(): EditorWorkspaceState {
  const fallback: EditorWorkspaceState = {
    workspaces: [defaultWorkspace()],
    activeWorkspaceId: DATABASE_WORKSPACE_ID,
    activeFileId: null,
    localFileMode: false,
    expanded: true,
    editorMode: 'rendered'
  }

  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return fallback
    const saved = JSON.parse(raw) as Partial<EditorWorkspaceState>
    const workspaces = Array.isArray(saved.workspaces)
      ? saved.workspaces.map(normalizeWorkspace).filter((workspace): workspace is EditorWorkspace => Boolean(workspace))
      : []
    if (!workspaces.some(workspace => workspace.id === DATABASE_WORKSPACE_ID)) {
      workspaces.unshift(defaultWorkspace())
    }
    const activeWorkspaceId = workspaces.some(workspace => workspace.id === saved.activeWorkspaceId)
      ? String(saved.activeWorkspaceId)
      : DATABASE_WORKSPACE_ID
    const activeWorkspace = workspaces.find(workspace => workspace.id === activeWorkspaceId)
    const activeFileId = activeWorkspace?.files.some(file => file.id === saved.activeFileId)
      ? String(saved.activeFileId)
      : null
    return {
      workspaces,
      activeWorkspaceId,
      activeFileId,
      localFileMode: saved.localFileMode === true && Boolean(activeFileId) && activeWorkspaceId !== DATABASE_WORKSPACE_ID,
      expanded: saved.expanded !== false,
      editorMode: saved.editorMode === 'source' || saved.editorMode === 'collab'
        ? saved.editorMode
        : 'rendered'
    }
  } catch {
    return fallback
  }
}

export function persistEditorWorkspaceState(state: EditorWorkspaceState) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      ...state,
      workspaces: state.workspaces.map(workspace => ({
        ...workspace,
        files: workspace.files.map(file => ({ ...file }))
      }))
    }))
  } catch (error) {
    // 大文件快照可能触发浏览器配额限制；调用方仍可继续编辑当前文件。
    console.warn('[editorWorkspace] 本地快照保存失败：', error)
  }
}

export function createWorkspaceId() {
  return `workspace-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}

export function createWorkspaceFileId(workspaceId: string) {
  return `${workspaceId}-file-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}

// FileSystemFileHandle 可以被结构化克隆，适合放入 IndexedDB；localStorage
// 只保留可读的文件记录，避免把浏览器专有句柄混进可迁移的 JSON 快照。
const HANDLE_DB_NAME = 'roamly_editor_file_handles_v1'
const HANDLE_STORE_NAME = 'handles'

type StoredHandle = {
  id: string
  handle: unknown
}

function openHandleDb(): Promise<IDBDatabase | null> {
  if (typeof indexedDB === 'undefined') return Promise.resolve(null)
  return new Promise(resolve => {
    const request = indexedDB.open(HANDLE_DB_NAME, 1)
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(HANDLE_STORE_NAME)) {
        request.result.createObjectStore(HANDLE_STORE_NAME, { keyPath: 'id' })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => resolve(null)
  })
}

export async function saveEditorFileHandle(id: string, handle: unknown) {
  const db = await openHandleDb()
  if (!db) return
  await new Promise<void>(resolve => {
    const tx = db.transaction(HANDLE_STORE_NAME, 'readwrite')
    tx.objectStore(HANDLE_STORE_NAME).put({ id, handle } satisfies StoredHandle)
    tx.oncomplete = () => resolve()
    tx.onerror = () => resolve()
    tx.onabort = () => resolve()
  })
  db.close()
}

export async function loadEditorFileHandle(id: string): Promise<unknown | null> {
  const db = await openHandleDb()
  if (!db) return null
  return new Promise(resolve => {
    const request = db.transaction(HANDLE_STORE_NAME, 'readonly').objectStore(HANDLE_STORE_NAME).get(id)
    request.onsuccess = () => {
      db.close()
      resolve((request.result as StoredHandle | undefined)?.handle || null)
    }
    request.onerror = () => {
      db.close()
      resolve(null)
    }
  })
}
