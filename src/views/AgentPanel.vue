<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  startQuestionnaire,
  submitQuestionnaireAnswer,
  type QuestionnaireQuestion
} from '@/api/agent'

interface ChatMsg {
  role: 'agent' | 'user' | 'tool' | 'info'
  content: string
  meta?: string
}

const loading = ref(false)
const sending = ref(false)
const current = ref<QuestionnaireQuestion | null>(null)
const input = ref('')
const messages = ref<ChatMsg[]>([])
const done = ref(false)
const plan = ref<any>(null)
const scrollBox = ref<HTMLElement | null>(null)
const inputBox = ref<HTMLInputElement | null>(null)

let cancelStream: (() => void) | null = null

function scrollBottom() {
  nextTick(() => {
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  })
}

function push(m: ChatMsg) {
  messages.value.push(m)
  scrollBottom()
}

function greet() {
  push({ role: 'info', content: '✦ 我是你的私人旅行规划 Agent。按几个问题，我就能整合天气与景点数据，为你定制一份可执行的计划。' })
}

async function start() {
  if (loading.value || sending.value) return
  loading.value = true
  messages.value = []
  plan.value = null
  done.value = false
  current.value = null
  try {
    greet()
    const q = await startQuestionnaire('user_001')
    current.value = q
    push({ role: 'agent', content: q.question, meta: 'Q' + (q.stepIndex + 1) + ' / ' + q.totalSteps })
  } catch (e) {
    push({ role: 'info', content: '无法连接后端，请确认服务已启动。' })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

function askQuestion(q: QuestionnaireQuestion) {
  current.value = q
  push({ role: 'agent', content: q.question, meta: 'Q' + (q.stepIndex + 1) + ' / ' + q.totalSteps })
}

function handleQuestionnaireEvent(ev: { name: string; data: any }) {
  console.log('[Agent UI] handling event:', ev.name, 'data:', ev.data)
  switch (ev.name) {
    case 'parsed':
      break
    case 'tool_call':
      push({ role: 'tool', content: '🔧 正在获取 ' + (ev.data?.source === 'weather' ? '目的地天气' : '景点数据') + '…' })
      break
    case 'tool_result':
      push({ role: 'tool', content: '✅ ' + (ev.data?.summary || '数据已缓存') })
      break
    case 'next_question':
      // 下一问题到达后立即解锁，让用户可以继续回答，不等待流自然结束
      sending.value = false
      askQuestion(ev.data as QuestionnaireQuestion)
      break
    case 'plan':
      sending.value = false
      plan.value = ev.data
      done.value = true
      current.value = null
      push({ role: 'info', content: '🎉 已回答完所有问题，你的旅行计划已生成：' })
      break
    case 'error':
      sending.value = false
      ElMessage.error(ev.data?.message || '出现错误')
      push({ role: 'info', content: '⚠️ ' + (ev.data?.message || '出现错误') })
      break
    default:
      break
  }
}

function send() {
  const text = input.value.trim()
  if (!text || !current.value || sending.value) return
  if (!current.value.sessionId) {
    ElMessage.warning('请先开始一次规划')
    return
  }
  sending.value = true
  const step = current.value.stepIndex
  const sessionId = current.value.sessionId
  push({ role: 'user', content: text })
  input.value = ''

  cancelStream?.()
  cancelStream = submitQuestionnaireAnswer(
    sessionId, step, text,
    (ev) => {
      handleQuestionnaireEvent(ev)
    },
    () => {
      // 流完全结束后才重置发送状态
      sending.value = false
    }
  )
}

function finishNow() {
  ElMessage.info('MVP：请先回答完当前问题；自动出方案能力将在下一步增强。')
}

onMounted(() => {
  start()
})
onUnmounted(() => {
  cancelStream?.()
})

const optionChips = (q: QuestionnaireQuestion | null) => (q?.type === 'select' ? q.options : undefined)
</script>

<template>
  <main class="agent-page">
    <!-- 页头：标题 + 状态（对应模板 Messages 页头） -->
    <section class="page-head">
      <div>
        <p class="eyebrow">PLAN AGENT</p>
        <h1>Agent 规划</h1>
        <p class="sub">流式问答 · 实时整合天气与景点数据 · 逐步生成计划</p>
      </div>
      <div class="status">
        <span class="dot" :class="{ on: !done }"></span>
        {{ done ? '已生成' : sending ? '对话中' : loading ? '启动中' : '就绪' }}
      </div>
    </section>

    <!-- 对话卡片 -->
    <section class="chat-card">
      <div ref="scrollBox" class="log">
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <span v-if="m.meta" class="meta">{{ m.meta }}</span>
          <p>{{ m.content }}</p>
        </div>
        <div v-if="loading" class="typing"><span></span><span></span><span></span></div>
      </div>

      <div v-if="optionChips(current)" class="chips">
        <button v-for="opt in optionChips(current)" :key="opt" :disabled="sending"
                @click="input = opt; send()">{{ opt }}</button>
      </div>

      <div v-if="plan" class="plan">
        <div class="plan-head">
          <div>
            <h2>{{ plan.destination }} · {{ plan.days }} 天</h2>
            <p>预算 ¥{{ plan.budget?.toLocaleString() }} · 风格：{{ plan.madeAt }}</p>
          </div>
          <div v-if="plan.weather" class="weather">
            <b>{{ plan.weather.text }}</b>
            <span>{{ plan.weather.tempMin }} ~ {{ plan.weather.tempMax }}℃</span>
          </div>
        </div>
        <div class="days">
          <div v-for="d in plan.daysPlan" :key="d.day" class="day">
            <span class="idx">{{ String(d.day).padStart(2, '0') }}</span>
            <div>
              <small>{{ d.date }}</small>
              <b>{{ d.theme }}</b>
              <p>{{ d.attraction }}</p>
            </div>
            <em>¥{{ d.dayBudget }}</em>
          </div>
        </div>
        <div v-if="plan.dataCache?.pois?.length" class="pois">
          <p class="lab">✓ 已整合景点数据</p>
          <span v-for="p in plan.dataCache.pois" :key="p.name">{{ p.name }}</span>
        </div>
      </div>

      <footer class="composer">
        <div v-if="!current && !done" class="empty-hint">点击下方输入或重新开始，进入一轮新的规划。</div>
        <div class="row">
          <input ref="inputBox" v-model="input" :disabled="sending || !current"
                 placeholder="Write a message… 输入你的回答" @keyup.enter="send" />
          <button class="send" :disabled="sending || !current" @click="send" title="发送">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
          <button class="ghost" :disabled="!current" @click="finishNow">直接出方案</button>
          <button class="ghost" :disabled="loading || sending" @click="start">重新开始</button>
        </div>
      </footer>
    </section>
  </main>
</template>

<style scoped lang="scss">
:global(*) { box-sizing: border-box; }

.agent-page {
  width: min(880px, calc(100% - 40px));
  margin: 0 auto;
  padding: 26px 0 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页头 */
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.eyebrow {
  color: var(--sunset);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
  margin: 0 0 8px;
}

.page-head h1 {
  font: 36px "DM Serif Display", "Noto Sans SC";
  color: var(--ink);
  margin: 0;
}

.sub {
  color: #687873;
  font-size: 13px;
  margin: 8px 0 0;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #587368;
  border: 1px solid var(--line);
  padding: 7px 14px;
  border-radius: 24px;
  background: var(--card);
  flex-shrink: 0;
}

.status .dot { width: 8px; height: 8px; border-radius: 50%; background: #b9c4be; }
.status .dot.on { background: var(--forest); box-shadow: 0 0 0 4px rgba(45, 106, 79, 0.15); }

/* 对话卡片 */
.chat-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.log {
  height: 400px;
  overflow-y: auto;
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

/* 消息气泡：模板中 米色来函 / 深绿发出 */
.msg {
  max-width: 76%;
  border-radius: 18px;
  padding: 12px 16px;
  line-height: 1.65;
  font-size: 13.5px;
}

.msg p { margin: 0; white-space: pre-wrap; word-break: break-word; }

.msg .meta {
  display: block;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #b08968;
  margin-bottom: 5px;
}

.msg.agent {
  align-self: flex-start;
  background: var(--wash);
  border: 1px solid var(--line);
  border-top-left-radius: 6px;
  color: var(--ink);
}

.msg.user {
  align-self: flex-end;
  background: var(--forest);
  color: var(--card);
  border-top-right-radius: 6px;
}

.msg.tool {
  align-self: flex-start;
  background: #f2f7f4;
  border: 1px dashed #D5E4DA;
  color: #6e7d77;
  font-size: 12px;
}

.msg.info {
  align-self: center;
  background: transparent;
  border: 0;
  color: #8a9792;
  font-size: 12px;
  text-align: center;
  max-width: 90%;
}

/* 输入中动画 */
.typing { display: flex; gap: 5px; padding: 6px 2px; align-self: flex-start; }
.typing span {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--roam);
  animation: blink 1s infinite;
}
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%,100%{opacity:.2} 50%{opacity:1} }

/* 选项快捷 chips */
.chips { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 26px 18px; }

.chips button {
  border: 1px solid #dfe7e1;
  background: #fff;
  color: var(--forest);
  padding: 8px 16px;
  border-radius: 24px;
  cursor: pointer;
  font-size: 12.5px;
  font-weight: 700;
  transition: background 0.2s, border-color 0.2s;
}

.chips button:hover:not(:disabled) { background: var(--roam-soft); border-color: var(--roam); }
.chips button:disabled { opacity: 0.5; cursor: default; }

/* 计划结果（浅色） */
.plan { margin: 0 26px 22px; border-top: 1px solid var(--line); padding-top: 20px; }
.plan-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.plan-head h2 { margin: 0; font-size: 20px; color: var(--ink); }
.plan-head p { margin: 6px 0 0; color: #687873; font-size: 12px; }
.weather { text-align: right; background: var(--roam-soft); padding: 10px 14px; border-radius: 12px; }
.weather b { display: block; color: var(--forest); font-size: 14px; }
.weather span { font-size: 12px; color: #6f847b; }

.days { margin-top: 16px; display: flex; flex-direction: column; gap: 9px; }
.day {
  display: flex; align-items: center; gap: 14px;
  background: #f8f6f0; border: 1px solid var(--line); border-radius: 14px; padding: 13px 15px;
}
.day .idx {
  width: 36px; height: 36px; border-radius: 11px; background: var(--forest); color: #fff;
  display: grid; place-items: center; font-weight: 800; font-size: 13px; flex-shrink: 0;
}
.day div { flex: 1; min-width: 0; }
.day small { color: #98a59f; font-size: 11px; }
.day b { display: block; font-size: 14px; margin-top: 2px; color: var(--ink); }
.day p { margin: 3px 0 0; font-size: 12px; color: #687873; }
.day em { font-style: normal; color: var(--forest); font-size: 13px; font-weight: 700; flex-shrink: 0; }

.pois { margin-top: 16px; }
.pois .lab { color: var(--forest); font-size: 12px; font-weight: 700; margin: 0 0 8px; }
.pois span {
  display: inline-block; margin: 0 6px 6px 0; padding: 5px 11px;
  background: var(--roam-soft); color: var(--roam); border-radius: 16px; font-size: 12px;
}

/* 底部输入区：模板的胶囊输入 + 圆形发送按钮 */
.composer { border-top: 1px solid var(--line); padding: 16px 22px 20px; background: var(--card); }
.empty-hint { color: #98a59f; font-size: 12px; margin: 0 0 10px; }

.row { display: flex; gap: 10px; align-items: center; }

.row input {
  flex: 1;
  border: 1px solid var(--line);
  background: var(--wash);
  color: var(--ink);
  padding: 13px 20px;
  border-radius: 999px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
}

.row input:focus { border-color: var(--roam); background: #fff; }
.row input:disabled { opacity: 0.5; }
.row input::placeholder { color: #98a59f; }

.send {
  width: 46px; height: 46px; flex-shrink: 0;
  border: 0; border-radius: 50%;
  background: var(--forest);
  color: #fff;
  display: grid; place-items: center;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(45, 106, 79, 0.32);
  transition: transform 0.2s;
}

.send svg { width: 18px; height: 18px; }
.send:hover:not(:disabled) { transform: translateY(-1px); }
.send:disabled { opacity: 0.5; cursor: default; }

.ghost {
  border: 1px solid var(--line);
  background: var(--card);
  color: var(--ink);
  padding: 12px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}

.ghost:hover:not(:disabled) { background: var(--paper); }
.ghost:disabled { opacity: 0.5; cursor: default; }

@media (max-width: 640px) {
  .page-head { flex-direction: column; align-items: flex-start; gap: 10px; }
  .log { padding: 18px; height: 340px; }
  .msg { max-width: 88%; }
  .chips { padding: 0 18px 14px; }
  .plan { margin: 0 18px 18px; }
  .row { flex-wrap: wrap; }
  .row input { min-width: 100%; }
  .send { margin-left: auto; }
}
</style>
