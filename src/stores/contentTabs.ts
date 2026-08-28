import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'

/** 卡片类型 */
export type CardKind =
  | 'explore-note'     // 发现灵感 · 旅行笔记
  | 'explore-spot'     // 发现灵感 · 灵感目的地
  | 'inspiration'      // 灵感目的地
  | 'journey'          // 我的旅程

/** 打开的卡片（浏览器标签式） */
export interface ContentTab {
  /** 唯一 id：`${kind}#${keyId}` */
  id: string
  kind: CardKind
  /** 标签名，如「发现灵感-珠海」 */
  title: string
  /** 卡片数据 */
  data: Record<string, any>
}

const MAX_TABS = 10
const STORAGE_KEY = 'roamly_content_tabs_v1'

function restoreState() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    const saved = raw ? JSON.parse(raw) : null
    if (!saved || !Array.isArray(saved.tabs)) return { tabs: [], activeId: null, lastRoute: '/notes' }
    const restoredTabs = saved.tabs.filter((tab: any) => tab && tab.id && tab.kind && tab.data)
    return {
      tabs: restoredTabs.slice(-MAX_TABS) as ContentTab[],
      activeId: restoredTabs.some((tab: any) => tab.id === saved.activeId) ? saved.activeId : (restoredTabs.at(-1)?.id ?? null),
      lastRoute: typeof saved.lastRoute === 'string' && saved.lastRoute !== '/card-detail' ? saved.lastRoute : '/notes'
    }
  } catch {
    return { tabs: [], activeId: null, lastRoute: '/notes' }
  }
}

/** 全局卡片多标签导航：覆盖中间主界面查看，关闭不影响底层页面（如旅行笔记） */
export const useContentTabsStore = defineStore('contentTabs', () => {
  const restored = restoreState()
  const tabs = ref<ContentTab[]>(restored.tabs)
  const activeId = ref<string | null>(restored.activeId)
  /** 预览来源路由：关闭预览/全部标签后返回的位置（由 App.vue 在路由跳转时记录） */
  const lastRoute = ref<string>(restored.lastRoute)

  watch([tabs, activeId, lastRoute], () => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ tabs: tabs.value, activeId: activeId.value, lastRoute: lastRoute.value }))
    } catch { /* 存储被禁用时仍保持内存内交互 */ }
  }, { deep: true })

  const activeTab = computed<ContentTab | null>(
    () => tabs.value.find(t => t.id === activeId.value) ?? null
  )

  /** 打开/聚焦卡片 */
  function open(tab: Omit<ContentTab, 'id'> & { id?: string }) {
    const id = tab.id ?? `${tab.kind}#${tab.data?.keyId ?? tab.title}`
    const exists = tabs.value.find(t => t.id === id)
    if (exists) {
      // 同一张卡片再次打开时合并列表页传入的最新快照，避免详情页显示旧内容。
      exists.title = tab.title
      exists.kind = tab.kind
      exists.data = { ...exists.data, ...tab.data }
      activeId.value = id
      return
    }
    // 最多同时打开 MAX_TABS 个：超出时空出最早打开的那个
    if (tabs.value.length >= MAX_TABS) {
      const victim = tabs.value.find(t => t.id !== activeId.value)
      close((victim ?? tabs.value[0]).id)
    }
    tabs.value.push({ ...tab, id })
    activeId.value = id
  }

  /** 关闭卡片：若关闭的是当前激活项，自动切到相邻（右优先） */
  function close(id: string) {
    const idx = tabs.value.findIndex(t => t.id === id)
    if (idx === -1) return
    tabs.value.splice(idx, 1)
    if (activeId.value === id) {
      activeId.value = tabs.value[idx]?.id ?? tabs.value[idx - 1]?.id ?? null
    }
  }

  /** 切换激活的卡片 */
  function activate(id: string) {
    if (tabs.value.some(t => t.id === id)) activeId.value = id
  }

  /** 关闭全部 */
  function closeAll() {
    tabs.value = []
    activeId.value = null
  }

  return { tabs, activeId, activeTab, lastRoute, MAX_TABS, open, close, activate, closeAll }
})
