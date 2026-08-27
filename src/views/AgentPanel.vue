<script setup lang="ts">
import { computed, nextTick, onMounted, onBeforeUnmount, onUnmounted, watch, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRightPanelStore } from '@/stores/rightPanel'
import { useAgentSessionStore } from '@/stores/agentSession'
import PlanPane from './PlanPane.vue'

/** 由 GlobalRightPanel 传入：告诉子组件它被渲染在右栏宿主里 */
const props = defineProps<{ fromRightPanel?: boolean }>()

const route = useRoute()
const router = useRouter()
const rightPanel = useRightPanelStore()
const agent = useAgentSessionStore()

// ── layout mode 自感知 ───────────────────────────────────────
// 两栏（右栏关）: 中间路由 -> split 左右分栏
// 三栏中间（右栏开但 viewKey !== agent）: 中间 /agent-panel -> results-only
// 三栏右栏（viewKey === agent）: GlobalRightPanel -> chat-only
// 兜底: chat-only
type LayoutMode = 'split' | 'results-only' | 'chat-only'
const layoutMode = computed<LayoutMode>(() => {
  // ★ 关键：GlobalRightPanel 通过 prop 显式告知宿主身份，
  // 因为两个实例共享同一个 Vue Router，route.path 都一样，无法靠路由区分。
  const isRightPanelHost = props.fromRightPanel === true
  const inRoute = route.path === '/agent-panel'

  // 被 GlobalRightPanel 渲染：显示对话 + 输入（不管中间路由是什么）
  if (isRightPanelHost) return 'chat-only'
  // 主路由 + 右栏关：两栏左右分栏
  if (inRoute && !rightPanel.show) return 'split'
  // 主路由 + 右栏开：三栏中间态，只展示结果（对话已挪到右栏）
  if (inRoute && rightPanel.show) return 'results-only'
  return 'chat-only'
})

// 容器内滚动用的 ref
const scrollBox = ref<HTMLElement | null>(null)
const inputBox = ref<HTMLInputElement | null>(null)

function scrollBottom() {
  nextTick(() => {
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  })
}
watch(
  () => agent.messages.length,
  () => {
    // 仅在对话/分栏模式下自动滚到底部
    if (layoutMode.value !== 'results-only') scrollBottom()
  },
)
// 计划生成后切到 Day 0（store 已自动设好）
watch(
  () => agent.messages.length,
  () => { if (agent.done) scrollBottom() },
)

// ── 生命周期 ─────────────────────────────────────────────────
onMounted(() => {
  // 三栏中间 results-only 模式：不触发问卷初始化，store 为空就保持空
  if (layoutMode.value === 'results-only') return
  // 仅当会话还没启动（没问候消息）时才初始化，避免右栏/中间各自 mount 时重复 greet
  if (agent.messages.length === 0 && !agent.done) {
    agent.startQuestionnaire()
  }
})

// ★ 关键：chat-only 宿主（GlobalRightPanel）下，
// 一旦 done=true 自动把中间路由切到 /agent-panel（结果以预览身份覆盖笔记界面）。
// 注意：useRouter() 只能在 setup 顶层调用，watcher 里直接用顶层的 router 常量。
watch(
  [layoutMode, () => agent.done],
  ([mode, done]) => {
    if (mode === 'chat-only' && done) {
      router.push('/agent-panel')
    }
  },
  { immediate: true, flush: 'post' },
)

onUnmounted(() => {
  // 实例卸载时不 dispose store，让两栏/三栏切换时数据保留
  // 只在整个页面关闭时再 reset（通过侧边栏显式调用 startQuestionnaire 或 router-leave）
})

</script>

<template>
  <main
    class="agent-page"
    :class="[`mode-${layoutMode}`, `plan-state-${agent.done ? 'done' : 'chatting'}`]"
  >
    <!-- ═══════════ split 模式（两栏 /agent-panel + 右栏关）═══════════ -->
    <template v-if="layoutMode === 'split'">
      <!-- 页头 -->
      <section class="page-head">
        <div>
          <p class="eyebrow">PLAN AGENT</p>
          <h1>Agent 规划</h1>
          <p class="sub">流式问答 · 实时整合天气与景点数据 · 逐步生成计划</p>
        </div>
        <div class="status">
          <span class="dot" :class="{ on: !agent.done }"></span>
          {{ agent.done ? '已生成' : agent.sending ? '生成中' : agent.loading ? '启动中' : '就绪' }}
        </div>
      </section>

      <!-- 左右分栏主体 -->
      <div class="split-body">
        <!-- 左栏：对话 + 输入 -->
        <section class="chat-pane">
          <div class="chat-card">
            <div ref="scrollBox" class="log">
              <div v-for="(m, i) in agent.messages" :key="i" class="msg" :class="m.role">
                <span v-if="m.meta" class="meta">{{ m.meta }}</span>
                <p>{{ m.content }}</p>
              </div>
              <div v-if="agent.sending && !agent.done" class="typing"><span></span><span></span><span></span></div>
            </div>

            <div v-if="agent.optionChips.length && !agent.done" class="chips">
              <button
                v-for="opt in agent.optionChips"
                :key="opt"
                :disabled="agent.sending"
                @click="agent.selectOption(opt)"
              >{{ opt }}</button>
            </div>

            <footer v-if="!agent.done" class="composer">
              <div class="row">
                <input
                  ref="inputBox"
                  v-model="agent.input"
                  :disabled="agent.sending"
                  placeholder="Write a message… 输入你的回答"
                  @keyup.enter="agent.send"
                />
                <button class="send" :disabled="agent.sending" @click="agent.send" title="发送">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="22" y1="2" x2="11" y2="13" />
                    <polygon points="22 2 15 22 11 13 2 9 22 2" />
                  </svg>
                </button>
                <button class="ghost" :disabled="agent.loading || agent.sending" @click="agent.startQuestionnaire">重新开始</button>
              </div>
            </footer>

            <footer v-else class="composer done-footer">
              <button class="ghost" @click="agent.startQuestionnaire">🔄 重新规划</button>
            </footer>
          </div>
        </section>

        <!-- 分隔竖线 -->
        <div class="split-divider"></div>

        <!-- 右栏：结果展示 -->
        <section class="plan-pane">
          <PlanPane v-if="agent.done && agent.plan" />
          <div v-else class="plan-placeholder">
            <div class="placeholder-illustration">🗺️</div>
            <h3>旅行计划将在这里呈现</h3>
            <p>在左侧回答完 5 个问题后，Agent 会整合天气与景点数据，自动生成可执行的行程。</p>
          </div>
        </section>
      </div>
    </template>

    <!-- ═══════════ results-only 模式（三栏中间）═══════════ -->
    <template v-else-if="layoutMode === 'results-only'">
      <section class="page-head slim">
        <div>
          <p class="eyebrow">PLAN AGENT</p>
          <h1>Agent 规划</h1>
          <p class="sub">结果预览 · 对话在右侧辅助面板中</p>
        </div>
        <div class="status">
          <span class="dot" :class="{ on: !agent.done }"></span>
          {{ agent.done ? '已生成' : agent.sending ? '生成中' : agent.loading ? '启动中' : '就绪' }}
        </div>
      </section>
      <PlanPane v-if="agent.done && agent.plan" />
      <div v-else class="plan-placeholder standalone">
        <div class="placeholder-illustration">🗺️</div>
        <h3>旅行计划将在这里呈现</h3>
        <p>打开右侧辅助面板即可开始与 Agent 对话，完成问卷后结果会自动显示在中央。</p>
      </div>
    </template>

    <!-- ═══════════ chat-only 模式（三栏右栏）═══════════ -->
    <template v-else>
      <div class="chat-only-head">
        <span class="dot" :class="{ on: !agent.done }"></span>
        <b>{{ agent.done ? '已生成' : agent.sending ? '生成中' : agent.loading ? '启动中' : '就绪' }}</b>
      </div>
      <div class="chat-only-body">
        <div ref="scrollBox" class="log">
          <div v-for="(m, i) in agent.messages" :key="i" class="msg" :class="m.role">
            <span v-if="m.meta" class="meta">{{ m.meta }}</span>
            <p>{{ m.content }}</p>
          </div>
          <div v-if="agent.sending && !agent.done" class="typing"><span></span><span></span><span></span></div>
        </div>

        <div v-if="agent.optionChips.length && !agent.done" class="chips">
          <button v-for="opt in agent.optionChips" :key="opt" :disabled="agent.sending" @click="agent.selectOption(opt)">{{ opt }}</button>
        </div>

        <footer v-if="!agent.done" class="composer">
          <div class="row">
            <input ref="inputBox" v-model="agent.input" :disabled="agent.sending" placeholder="输入回答…" @keyup.enter="agent.send" />
            <button class="send" :disabled="agent.sending" @click="agent.send" title="发送">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
          <button class="ghost ghost-full" :disabled="agent.loading || agent.sending" @click="agent.startQuestionnaire">重新开始</button>
        </footer>

        <footer v-else class="composer done-footer">
          <button class="ghost ghost-full" @click="agent.startQuestionnaire">🔄 重新规划</button>
        </footer>
      </div>
    </template>
  </main>
</template>

<style scoped lang="scss">
:global(*) { box-sizing: border-box; }

.agent-page {
  /* fill the workspace route-view (which has height 100%) */
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;                  /* ★ critical flex child height constraint */
  overflow: hidden;               /* page itself never scrolls */
}

/* ── 共用：页头 ─────────────────────────────────────────────── */
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;

  &.slim { margin-bottom: 0; }
}
.eyebrow { color: var(--sunset); font-size: 10px; font-weight: 800; letter-spacing: 0.18em; margin: 0 0 8px; }
.page-head h1 { font: 32px "DM Serif Display", "Noto Sans SC"; color: var(--ink); margin: 0; }
.sub { color: #687873; font-size: 13px; margin: 8px 0 0; }
.status {
  display: inline-flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 700;
  color: #587368; border: 1px solid var(--line); padding: 7px 14px; border-radius: 24px;
  background: var(--card); flex-shrink: 0;
}
.status .dot { width: 8px; height: 8px; border-radius: 50%; background: #b9c4be; }
.status .dot.on { background: var(--forest); box-shadow: 0 0 0 4px rgba(45, 106, 79, 0.15); }

/* ── split 模式：左右分栏 ───────────────────────────────────── */
.mode-split .agent-page, &.mode-split { }  // no-op
.agent-page.mode-split { max-width: 1240px; }

.split-body {
  flex: 1 1 auto;        /* ★ fill remaining height of .agent-page */
  min-height: 0;         /* ★ allow flex child to shrink */
  display: flex;
  flex-direction: row;
  gap: 20px;
  overflow: hidden;      /* prevent outer scrollbar */
}
.chat-pane {
  flex: 0 0 46%;
  min-width: 360px;
  max-width: 520px;
  min-height: 0;         /* ★ allow child .chat-card to flex */
  display: flex;
  flex-direction: column;
}
.plan-pane {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;         /* ★ allow internal content to scroll */
  overflow-y: auto;      /* ★ plan pane IS the scroll container */
  padding-right: 4px;
  /* smooth scroll + nicer scrollbar */
  scroll-behavior: smooth;
}
.split-divider {
  width: 1px;
  background: var(--line);
  flex-shrink: 0;
  margin: 8px 0;
}

/* ── chat-only 模式：右栏窄适配 ─────────────────────────────── */
.mode-chat-only {
  padding: 14px 12px 18px;
  gap: 0;
}
.chat-only-head {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: var(--forest); font-weight: 700;
  padding: 6px 2px 10px;
}
.chat-only-head .dot { width: 8px; height: 8px; border-radius: 50%; background: #b9c4be; flex-shrink: 0; }
.chat-only-head .dot.on { background: var(--forest); box-shadow: 0 0 0 3px rgba(45, 106, 79, 0.15); }
.chat-only-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
  overflow: hidden;
}

/* ── results-only 模式：顶部页头紧凑一些 ───────────────────── */
.mode-results-only {
  max-width: 1100px;
  height: 100%;
  overflow: hidden;          /* 页面本身不滚 */
}
.mode-results-only .page-head.slim h1 { font-size: 28px; }
/* results-only 模式下 PlanPane 是唯一内容，让它填满并内部滚 */
.mode-results-only :deep(.plan-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;          /* ★ PlanPane 自己滚 */
  padding-right: 4px;
  scroll-behavior: smooth;
}

/* ── 对话卡片（split + chat-only 共用）─────────────────────── */
.chat-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 20px;
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1 1 auto;       /* ★ fill chat-pane height */
  overflow: hidden;
}
.log {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scroll-behavior: smooth;
}
/* chat-only 模式下也让 log 撑满（chat-card 高度由 flex 控制） */
.mode-chat-only .log {
  padding: 16px;
}
/* split 模式下不再需要 max-height，让 flex 自己算 */

.msg {
  max-width: 80%;
  border-radius: 16px;
  padding: 10px 14px;
  line-height: 1.6;
  font-size: 13px;
}
.msg p { margin: 0; white-space: pre-wrap; word-break: break-word; }
.msg .meta { display: block; font-size: 10px; font-weight: 700; letter-spacing: 0.06em; color: #b08968; margin-bottom: 4px; }
.msg.agent { align-self: flex-start; background: var(--wash); border: 1px solid var(--line); border-top-left-radius: 6px; color: var(--ink); }
.msg.user { align-self: flex-end; background: var(--forest); color: var(--card); border-top-right-radius: 6px; }
.msg.tool { align-self: flex-start; background: #f2f7f4; border: 1px dashed #D5E4DA; color: #6e7d77; font-size: 12px; }
.msg.info { align-self: center; background: transparent; border: 0; color: #8a9792; font-size: 12px; text-align: center; max-width: 90%; }
.typing { display: flex; gap: 5px; padding: 6px 2px; align-self: flex-start; }
.typing span { width: 7px; height: 7px; border-radius: 50%; background: var(--roam); animation: blink 1s infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%,100%{opacity:.2} 50%{opacity:1} }

.chips { display: flex; flex-wrap: wrap; gap: 8px; padding: 4px 20px 14px; }
.chips button {
  border: 1px solid #dfe7e1; background: #fff; color: var(--forest);
  padding: 6px 14px; border-radius: 22px; cursor: pointer; font-size: 12px; font-weight: 700;
  transition: background 0.2s, border-color 0.2s;
}
.chips button:hover:not(:disabled) { background: var(--roam-soft); border-color: var(--roam); }
.chips button:disabled { opacity: 0.5; cursor: default; }

/* chat-only 模式下 chips 内边距收紧 */
.mode-chat-only .chips { padding: 4px 14px 12px; }

/* ── 输入区（三种模式共用） ──────────────────────────────────── */
.composer { border-top: 1px solid var(--line); padding: 12px 18px 16px; background: var(--card); }
.row { display: flex; gap: 8px; align-items: center; }
.row input {
  flex: 1; border: 1px solid var(--line); background: var(--wash); color: var(--ink);
  padding: 10px 16px; border-radius: 999px; font-size: 13px;
  outline: none; transition: border-color 0.2s, background 0.2s;
}
.row input:focus { border-color: var(--roam); background: #fff; }
.row input:disabled { opacity: 0.5; }
.row input::placeholder { color: #98a59f; }
.send {
  width: 38px; height: 38px; flex-shrink: 0;
  border: 0; border-radius: 50%;
  background: var(--forest); color: #fff;
  display: grid; place-items: center;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(45, 106, 79, 0.28);
  transition: transform 0.2s;
}
.send svg { width: 16px; height: 16px; }
.send:hover:not(:disabled) { transform: translateY(-1px); }
.send:disabled { opacity: 0.5; cursor: default; }
.ghost {
  border: 1px solid var(--line); background: var(--card); color: var(--ink);
  padding: 10px 16px; border-radius: 999px; font-size: 12px; font-weight: 700;
  cursor: pointer; flex-shrink: 0; transition: background 0.2s;
}
.ghost:hover:not(:disabled) { background: var(--paper); }
.ghost:disabled { opacity: 0.5; cursor: default; }
.ghost-full { width: 100%; margin-top: 10px; }
.done-footer {
  display: flex; justify-content: flex-start;
}

/* ── 结果面板（PlanPane） ───────────────────────────────────── */
.plan-wrapper { }
.plan-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.plan-head h2 { margin: 0; font-size: 20px; color: var(--ink); }
.plan-head p { margin: 6px 0 0; color: #687873; font-size: 12px; }
.weather-badge { display: flex; align-items: center; gap: 10px; background: var(--roam-soft); padding: 10px 14px; border-radius: 14px; flex-shrink: 0; }
.weather-badge .weather-icon { font-size: 28px; line-height: 1; }
.weather-badge b { display: block; color: var(--forest); font-size: 15px; font-weight: 700; }
.weather-badge span { font-size: 12px; color: #6f847b; }

.day-tabs { margin-top: 4px; }
.tab-bar { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 12px; border-bottom: 2px solid var(--line); scrollbar-width: none; }
.tab-bar::-webkit-scrollbar { display: none; }
.tab-btn {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 8px 16px; border-radius: 12px; border: 1.5px solid transparent;
  background: transparent; cursor: pointer; transition: all 0.2s;
  flex-shrink: 0; min-width: 56px;
}
.tab-btn .tab-label { font-size: 13px; font-weight: 700; color: #98a59f; transition: color 0.2s; }
.tab-btn .tab-temp { font-size: 11px; color: #c0cac4; transition: color 0.2s; }
.tab-btn.active { border-color: var(--forest); background: var(--roam-soft); }
.tab-btn.active .tab-label { color: var(--forest); }
.tab-btn.active .tab-temp { color: var(--forest); }

.tab-content { padding-top: 16px; }
.day-weather-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.weather-chip { display: inline-flex; align-items: center; gap: 5px; background: #f0f7f4; border: 1px solid #d5e4da; color: #3d6e5a; font-size: 12px; padding: 5px 10px; border-radius: 20px; }
.budget-chip { background: #fef9ec; border-color: #f0dfb5; color: #a0712e; }

.plan-md {
  background: #fafaf8; border: 1px solid var(--line); border-radius: 14px;
  padding: 18px 20px; font-size: 13.5px; line-height: 1.75; color: var(--ink);
  :deep(h2) { font-size: 17px; font-weight: 700; color: var(--ink); margin: 0 0 10px; border-bottom: 1.5px solid var(--line); padding-bottom: 6px; }
  :deep(h3) { font-size: 14px; font-weight: 700; color: var(--ink-2); margin: 14px 0 6px; }
  :deep(p) { margin: 0 0 8px; }
  :deep(strong) { color: var(--forest); }
  :deep(ul), :deep(ol) { margin: 6px 0 8px; padding-left: 20px; }
  :deep(li) { margin-bottom: 4px; }
}

.activity-cards { display:flex; flex-direction:column; gap:10px; margin-bottom:14px; }
.activity-card { display:flex; overflow:hidden; border:1px solid var(--line); border-radius:12px; background:#fff; }
.activity-card img { width:112px; min-height:92px; object-fit:cover; }
.activity-card-body { padding:10px 12px; min-width:0; }
.activity-card-body > span { color:var(--forest); font-size:11px; font-weight:800; }
.activity-card h4 { margin:3px 0; color:var(--sunset); font-size:15px; }
.activity-card p { margin:0 0 5px; color:#687873; font-size:12px; }
.activity-card small { color:#87938e; font-size:11px; }

.plan-detail {
  margin-top: 20px; background: var(--card); border: 1px solid var(--line); border-radius: 16px; overflow: hidden;
}
.plan-detail-inner {
  padding: 22px 24px; line-height: 1.8; font-size: 14px; color: #333;
  :deep(h1), :deep(h2), :deep(h3) { color: var(--forest); margin: 1em 0 0.5em; font-weight: 700; }
  :deep(h1) { font-size: 22px; }
  :deep(h2) { font-size: 18px; border-bottom: 1.5px solid var(--line); padding-bottom: 6px; }
  :deep(h3) { font-size: 15px; }
  :deep(p) { margin: 0.7em 0; }
  :deep(ul), :deep(ol) { padding-left: 22px; margin: 0.7em 0; }
  :deep(li) { margin: 0.3em 0; }
  :deep(strong) { color: var(--sunset); font-weight: 600; }
  :deep(em) { color: var(--sunset); font-style: italic; }
  :deep(code) { background: var(--roam-soft); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
  :deep(blockquote) { border-left: 4px solid var(--sunset); margin: 1em 0; padding: 10px 16px; background: var(--sunset-soft); color: #66756f; }
}
.plan .pois { margin-top: 16px; }
.plan .pois .lab { color: var(--forest); font-size: 12px; font-weight: 700; margin: 0 0 8px; }
.plan .pois span { display: inline-block; margin: 0 6px 6px 0; padding: 5px 11px; background: var(--roam-soft); color: var(--roam); border-radius: 16px; font-size: 12px; }

/* ── 占位提示（split 右栏 & results-only） ───────────────────── */
.plan-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 32px;
  background: var(--card);
  border: 1px dashed var(--line);
  border-radius: 20px;
  min-height: 300px;
  gap: 14px;

  &.standalone {
    padding: 80px 32px;
    min-height: 400px;
  }
}
.plan-placeholder .placeholder-illustration { font-size: 64px; line-height: 1; opacity: 0.6; }
.plan-placeholder h3 { margin: 0; color: var(--ink); font-size: 18px; }
.plan-placeholder p { margin: 0; color: #8a9792; font-size: 13px; max-width: 360px; }

/* ── 响应式：窄屏回叠 ───────────────────────────────────────── */
@media (max-width: 980px) {
  .split-body { flex-direction: column; }
  .chat-pane, .plan-pane { flex: none; max-width: none; width: 100%; }
  .split-divider { width: 100%; height: 1px; margin: 6px 0; }
}
@media (max-width: 640px) {
  .chat-pane { min-width: 0; }
  .page-head { flex-direction: column; align-items: flex-start; gap: 10px; }
}
</style>
