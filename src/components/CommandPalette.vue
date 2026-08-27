<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRightPanelStore } from '@/stores/rightPanel'

interface Command { id: string; label: string; hint: string; icon: string; run: () => void }
const router = useRouter()
const rightPanel = useRightPanelStore()
const open = ref(false)
const query = ref('')
const activeIndex = ref(0)
const searchInput = ref<HTMLInputElement | null>(null)

function close() { open.value = false; query.value = ''; activeIndex.value = 0 }
function go(path: string) { router.push(path); close() }
const commands = computed<Command[]>(() => [
  { id: 'notes', label: '打开旅行笔记', hint: '⌘ 1', icon: '▤', run: () => go('/notes') },
  { id: 'explore', label: '探索旅行灵感', hint: '⌘ 2', icon: '✦', run: () => go('/explore') },
  { id: 'plan', label: '开始 AI 旅行规划', hint: '⌘ 3', icon: '◈', run: () => go('/chat') },
  { id: 'journeys', label: '查看我的旅程', hint: '⌘ 4', icon: '⌁', run: () => go('/journeys') },
  { id: 'profile', label: '打开个人主页', hint: '⌘ 5', icon: '◎', run: () => go('/profile') },
  { id: 'panel', label: rightPanel.show ? '收起右侧面板' : '打开右侧面板', hint: '⌘ B', icon: '◫', run: () => { rightPanel.show ? rightPanel.close() : rightPanel.openView('explore', '发现灵感'); close() } }
])
const filteredCommands = computed(() => {
  const q = query.value.trim().toLowerCase()
  return q ? commands.value.filter(c => `${c.label} ${c.hint}`.toLowerCase().includes(q)) : commands.value
})
function toggle() {
  if (open.value) close()
  else { open.value = true; void nextTick(() => searchInput.value?.focus()) }
}
function onKeydown(event: KeyboardEvent) {
  const modifier = event.metaKey || event.ctrlKey
  if (modifier && event.key.toLowerCase() === 'k') { event.preventDefault(); toggle(); return }
  if (!open.value && modifier && ['1', '2', '3', '4', '5'].includes(event.key)) {
    event.preventDefault()
    const paths = ['/notes', '/explore', '/chat', '/journeys', '/profile']
    go(paths[Number(event.key) - 1])
    return
  }
  if (!open.value) return
  if (event.key === 'Escape') close()
  else if (event.key === 'ArrowDown') { event.preventDefault(); activeIndex.value = Math.min(activeIndex.value + 1, Math.max(filteredCommands.value.length - 1, 0)) }
  else if (event.key === 'ArrowUp') { event.preventDefault(); activeIndex.value = Math.max(activeIndex.value - 1, 0) }
  else if (event.key === 'Enter') { event.preventDefault(); filteredCommands.value[activeIndex.value]?.run() }
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
defineExpose({ toggle })
</script>

<template>
  <Transition name="command-fade">
    <div v-if="open" class="command-backdrop" @mousedown.self="close">
      <section class="command-palette" role="dialog" aria-modal="true" aria-label="命令面板">
        <div class="command-search-row"><span class="command-search-icon">⌕</span><input ref="searchInput" v-model="query" placeholder="输入命令或搜索页面…" @input="activeIndex = 0" /><kbd>ESC</kbd></div>
        <div v-if="filteredCommands.length" class="command-list" role="listbox">
          <button v-for="(command, index) in filteredCommands" :key="command.id" class="command-item" :class="{ active: index === activeIndex }" role="option" :aria-selected="index === activeIndex" @mouseenter="activeIndex = index" @click="command.run"><span class="command-icon">{{ command.icon }}</span><span class="command-label">{{ command.label }}</span><kbd>{{ command.hint }}</kbd></button>
        </div>
        <p v-else class="command-empty">没有找到匹配的操作</p>
        <footer class="command-footer"><span>↑↓ 选择</span><span>↵ 执行</span><span>esc 关闭</span></footer>
      </section>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.command-backdrop { position: fixed; inset: 0; z-index: 100000; display: flex; align-items: flex-start; justify-content: center; padding-top: min(15vh, 140px); background: color-mix(in srgb, var(--ink) 32%, transparent); backdrop-filter: blur(5px); }
.command-palette { width: min(600px, calc(100vw - 32px)); overflow: hidden; border: 1px solid var(--line); border-radius: 14px; background: var(--card); color: var(--ink); box-shadow: 0 24px 70px color-mix(in srgb, var(--ink) 28%, transparent); }
.command-search-row { display: flex; align-items: center; gap: 11px; padding: 15px 17px; border-bottom: 1px solid var(--line); }.command-search-icon { color: var(--ink-3); font-size: 23px; line-height: 1; }.command-search-row input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; color: var(--ink); font: 15px inherit; }.command-search-row input::placeholder { color: var(--ink-3); }
kbd { border: 1px solid var(--line); border-radius: 5px; padding: 3px 6px; color: var(--ink-3); font: 10px ui-monospace, monospace; }.command-list { padding: 7px; max-height: 410px; overflow: auto; }.command-item { display: flex; align-items: center; gap: 11px; width: 100%; padding: 11px 12px; border: 0; border-radius: 8px; background: transparent; color: var(--ink); text-align: left; cursor: pointer; }.command-item:hover, .command-item.active { background: var(--roam-soft); }.command-icon { display: grid; place-items: center; width: 24px; height: 24px; color: var(--forest); font-size: 17px; }.command-label { flex: 1; font-size: 13px; font-weight: 650; }.command-empty { padding: 30px 20px; color: var(--ink-3); text-align: center; font-size: 13px; }.command-footer { display: flex; gap: 18px; padding: 10px 15px; border-top: 1px solid var(--line); color: var(--ink-3); font-size: 11px; }
.command-fade-enter-active, .command-fade-leave-active { transition: opacity .16s ease; }.command-fade-enter-active .command-palette, .command-fade-leave-active .command-palette { transition: transform .16s ease, opacity .16s ease; }.command-fade-enter-from, .command-fade-leave-to { opacity: 0; }.command-fade-enter-from .command-palette, .command-fade-leave-to .command-palette { transform: translateY(-8px) scale(.98); opacity: 0; }
</style>
