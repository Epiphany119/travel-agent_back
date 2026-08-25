import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface LinkPanelData {
  url: string
  title: string
}

/** 全局右侧面板 Store */
export const useRightPanelStore = defineStore('rightPanel', () => {
  const show = ref(false)
  const width = ref(360)
  const type = ref<'view' | 'link' | 'empty'>('empty')
  const title = ref('')
  /** 视图 key：对应右侧面板渲染哪个页面组件 */
  const viewKey = ref<string | null>(null)
  const linkData = ref<LinkPanelData | null>(null)

  /** 在右侧直接显示某个功能页面 */
  function openView(key: string, label: string) {
    viewKey.value = key
    linkData.value = null
    type.value = 'view'
    title.value = label
    show.value = true
  }

  /** 打开链接预览面板 */
  function openLink(data: LinkPanelData) {
    linkData.value = data
    type.value = 'link'
    title.value = data.title
    show.value = true
  }

  /** 关闭面板 */
  function close() {
    show.value = false
    viewKey.value = null
    linkData.value = null
    type.value = 'empty'
    title.value = ''
  }

  /** 设置宽度 */
  function setWidth(w: number) {
    width.value = w
  }

  /** 切换显示/隐藏 */
  function toggle() {
    show.value = !show.value
  }

  return { show, width, type, title, viewKey, linkData, openView, openLink, close, setWidth, toggle }
})
