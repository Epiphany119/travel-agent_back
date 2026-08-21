<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { subscribeA2AStream, type TravelPlan } from '@/api/agent'
import { useStreamStore } from '@/stores/stream'

interface ChatMsg {
  role: 'agent' | 'user' | 'tool' | 'info'
  content: string
  meta?: string
}

const loading = ref(false)
const sending = ref(false)
const current = ref<any>(null)
const input = ref('')
const messages = ref<ChatMsg[]>([])
const done = ref(false)
const plan = ref<any>(null)
const activeDay = ref(0)
const scrollBox = ref<HTMLElement | null>(null)
const inputBox = ref<HTMLInputElement | null>(null)
const streamStore = useStreamStore()

let cancelStream: (() => void) | null = null

// ─── 问卷进度 ────────────────────────────────────────────────────────────────
const TOTAL_STEPS = 5
const stepIndex = ref(0)
const answers = ref<Record<string, string>>({})

const questions: Array<{ field: string; prompt: string; options?: string[] }> = [
  { field: 'destination', prompt: '你想去哪里旅行？比如：杭州、成都、上海…' },
  { field: 'madeAt',     prompt: '这次你更想要什么旅行节奏？', options: ['轻松漫游', '紧凑高效', '慢节奏深度'] },
  { field: 'days',       prompt: '打算玩几天？', options: ['1', '2', '3', '4', '5'] },
  { field: 'interests',  prompt: '对什么主题感兴趣？比如：美食、人文、自然、摄影、购物…（可多个，逗号分隔）' },
  { field: 'budget',     prompt: '这次出行总预算大概是多少元？' },
]

const currentQuestion = computed(() => questions[stepIndex.value])
const meta = computed(() => `Q${stepIndex.value + 1} / ${TOTAL_STEPS}`)

// ─── 计划数据（复用 ChatView 同款 dayTabs + overview 逻辑） ─────────────────
const dayTabs = computed(() => {
  if (!plan.value) return []
  if (plan.value.dayPlans && plan.value.dayPlans.length) {
    return plan.value.dayPlans.map((d: any) => {
      const activities = d.activities || []
      const withTime = (a: any) => a.time

      const morning = activities.filter((a: any) => withTime(a) && a.time < '12:00')
      const afternoon = activities.filter((a: any) => withTime(a) && a.time >= '12:00' && a.time < '18:00')
      const evening = activities.filter((a: any) => withTime(a) && a.time >= '18:00')
      if (!morning.length && !afternoon.length && !evening.length && activities.length) {
        const third = Math.ceil(activities.length / 3)
        activities.slice(0, third).forEach(a => morning.push(a))
        activities.slice(third, third * 2).forEach(a => afternoon.push(a))
        activities.slice(third * 2).forEach(a => evening.push(a))
      }

      const fmt = (a: any) =>
        `- **${a.name}**${a.location ? `（${a.location}）` : ''}${a.time ? ` · ${a.time}` : ''}${a.notes ? `\n  ${a.notes}` : ''}`

      const md = [
        `## ${d.theme || ('第' + d.day + '天')}`,
        d.date ? `**日期：** ${d.date}` : '',
        d.dailyBudget ? `**预算：** ¥${d.dailyBudget}` : '',
        morning.length ? `\n### 🌅 上午\n${morning.map(fmt).join('\n')}` : '',
        afternoon.length ? `\n### ☀️ 下午\n${afternoon.map(fmt).join('\n')}` : '',
        evening.length ? `\n### 🌙 晚上\n${evening.map(fmt).join('\n')}` : '',
      ].filter(Boolean).join('\n')

      return {
        label: `Day ${d.day || 1}`,
        subLabel: d.date || '',
        html: marked.parse(md) as string,
        weather: null,
        budget: d.dailyBudget || null
      }
    })
  }
  const days = plan.value.days || 1
  return Array.from({ length: days }, (_, i) => ({
    label: `Day ${i + 1}`, subLabel: '', html: '', weather: null, budget: null
  }))
})

const overview = computed(() => {
  if (!plan.value?.dayPlans?.length) return ''
  return plan.value.dayPlans.map((d: any) => {
    const fmt = (a: any) =>
      `- **${a.name}**${a.location ? `（${a.location}）` : ''}${a.time ? ` · ${a.time}` : ''}${a.notes ? `\n  ${a.notes}` : ''}`
    const acts = d.activities || []
    return [
      `## 第 ${d.day || '?'} 天 · ${d.theme || ''}`,
      d.date ? `**日期：** ${d.date}` : '',
      d.dailyBudget ? `**预算：** ¥${d.dailyBudget}` : '',
      acts.length ? `\n${acts.map(fmt).join('\n')}` : '',
    ].filter(Boolean).join('\n')
  }).join('\n\n')
})

const globalWeather = computed(() => {
  if (!plan.value) return null
  if (plan.value.weather) return plan.value.weather
  if (plan.value.dayPlans?.[0]?.weather) return plan.value.dayPlans[0].weather
  return null
})

function renderMarkdown(md: string) {
  return marked.parse(md) as string
}

// ─── 对话 UI ─────────────────────────────────────────────────────────────────
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

// ─── 问卷核心 ────────────────────────────────────────────────────────────────
onMounted(() => {
  startQuestionnaire()
})

onUnmounted(() => {
  cancelStream?.()
  streamStore.reset()
})

function startQuestionnaire() {
  loading.value = false
  sending.value = false
  messages.value = []
  plan.value = null
  done.value = false
  stepIndex.value = 0
  answers.value = {}
  streamStore.reset()
  greet()
  push({ role: 'agent', content: currentQuestion.value.prompt, meta: meta.value })
}

function selectOption(opt: string) {
  input.value = opt
  send()
}

function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  sending.value = true
  push({ role: 'user', content: text })
  input.value = ''

  const field = currentQuestion.value.field
  const value = text
  answers.value[field] = value

  stepIndex.value++

  if (stepIndex.value < TOTAL_STEPS) {
    // 问下一个问题
    sending.value = false
    push({ role: 'agent', content: currentQuestion.value.prompt, meta: meta.value })
  } else {
    // 全部答完 → 调 A2A 编排生成真实计划
    push({ role: 'tool', content: '🔧 正在获取天气与景点数据，整合编排中…' })
    runA2APlan()
  }
}

function runA2APlan() {
  const dest = answers.value['destination'] || '未知'
  const days = parseInt(answers.value['days'] || '3')
  const budget = parseInt(answers.value['budget'] || '3000')
  const travelStyle = answers.value['madeAt'] || '轻松漫游'
  const interests = (answers.value['interests'] || '美食,人文').split(/[,，]/).map(s => s.trim()).filter(Boolean)

  cancelStream?.()
  cancelStream = subscribeA2AStream({
    destination: dest,
    days,
    budget,
    travelers: 1,
    travelStyle,
    interests
  }, (event) => {
    console.log('[AgentPanel A2A]', event.name, event.data)
    switch (event.name) {
      case 'tool_call':
        push({ role: 'tool', content: '🔧 ' + (event.data?.source === 'weather' ? '天气数据获取中…' : '景点数据获取中…') })
        break
      case 'tool_result':
        push({ role: 'tool', content: '✅ ' + (event.data?.summary || '数据已缓存') })
        break
      case 'token':
        // 忽略 token，不做流式打字（问卷流程不需要）
        break
      case 'task_done': {
        const result = event.data
        if (!result) {
          push({ role: 'info', content: '⚠️ 计划生成失败，请重试。' })
          sending.value = false
          return
        }
        // 映射成前端 plan 结构
        plan.value = {
          destination: result.destination || dest,
          days: result.dayPlans?.length || days,
          budget: budget,
          weather: result.weather || null,
          dayPlans: (result.dayPlans || []).map((dp: any) => ({
            day: dp.day || 1,
            date: dp.date || '',
            theme: dp.theme || '',
            dailyBudget: dp.dailyBudget || 0,
            activities: dp.activities || []
          }))
        }
        done.value = true
        sending.value = false
        push({ role: 'info', content: '🎉 旅行计划已生成：' })
        activeDay.value = 0
        break
      }
      case 'error':
        push({ role: 'info', content: '⚠️ ' + (event.data?.message || '出现错误') })
        sending.value = false
        break
    }
  })
}

const optionChips = computed(() => currentQuestion.value.options)
</script>

<template>
  <main class="agent-page">
    <!-- 页头 -->
    <section class="page-head">
      <div>
        <p class="eyebrow">PLAN AGENT</p>
        <h1>Agent 规划</h1>
        <p class="sub">流式问答 · 实时整合天气与景点数据 · 逐步生成计划</p>
      </div>
      <div class="status">
        <span class="dot" :class="{ on: !done }"></span>
        {{ done ? '已生成' : sending ? '生成中' : loading ? '启动中' : '就绪' }}
      </div>
    </section>

    <!-- 对话卡片 -->
    <section class="chat-card">
      <div ref="scrollBox" class="log">
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <span v-if="m.meta" class="meta">{{ m.meta }}</span>
          <p>{{ m.content }}</p>
        </div>
        <div v-if="sending && !done" class="typing"><span></span><span></span><span></span></div>
      </div>

      <!-- 选项快捷 chips -->
      <div v-if="optionChips && !done" class="chips">
        <button v-for="opt in optionChips" :key="opt" :disabled="sending"
                @click="selectOption(opt)">{{ opt }}</button>
      </div>

      <!-- 计划结果 -->
      <div v-if="plan" class="plan">
        <div class="plan-head">
          <div>
            <h2>{{ plan.destination }} · {{ dayTabs.length }} 天</h2>
            <p v-if="plan.budget">预算 ¥{{ plan.budget?.toLocaleString() }}</p>
          </div>
          <div v-if="globalWeather" class="weather-badge">
            <span class="weather-icon">{{ globalWeather.icon || '☀️' }}</span>
            <div>
              <b>{{ globalWeather.text || '天气' }}</b>
              <span>{{ globalWeather.tempMin || '--' }} ~ {{ globalWeather.tempMax || '--' }}℃</span>
            </div>
          </div>
        </div>

        <!-- 按天 Tabs -->
        <div v-if="dayTabs.length > 0" class="day-tabs">
          <div class="tab-bar">
            <button
              v-for="(tab, i) in dayTabs"
              :key="i"
              class="tab-btn"
              :class="{ active: activeDay === i }"
              @click="activeDay = i"
            >
              <span class="tab-label">{{ tab.label }}</span>
              <span v-if="tab.weather" class="tab-temp">{{ tab.weather.temp }}°</span>
            </button>
          </div>

          <div class="tab-content">
            <div v-if="dayTabs[activeDay]" class="day-detail">
              <!-- 天气指标 -->
              <div v-if="dayTabs[activeDay].weather" class="day-weather-row">
                <div class="weather-chip">
                  <span>{{ dayTabs[activeDay].weather.icon }}</span>
                  <span>{{ dayTabs[activeDay].weather.text }}</span>
                </div>
                <div class="weather-chip">
                  <span>🌡️</span>
                  <span>{{ dayTabs[activeDay].weather.tempMin }} ~ {{ dayTabs[activeDay].weather.tempMax }}℃</span>
                </div>
                <div v-if="dayTabs[activeDay].budget" class="weather-chip budget-chip">
                  <span>💰</span>
                  <span>¥{{ dayTabs[activeDay].budget }}</span>
                </div>
              </div>

              <!-- Markdown 渲染 -->
              <div v-if="dayTabs[activeDay].html" class="plan-md" v-html="dayTabs[activeDay].html"></div>
              <div v-else-if="overview" class="plan-empty">
                <p>暂无详细计划数据。</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 完整行程正文 -->
        <div v-if="overview" class="plan-detail">
          <div class="plan-detail-inner" v-html="renderMarkdown(overview)"></div>
        </div>

        <!-- 景点缓存标签 -->
        <div v-if="plan.dataCache?.pois?.length" class="pois">
          <p class="lab">✓ 已整合景点数据</p>
          <span v-for="p in plan.dataCache.pois" :key="p.name">{{ p.name }}</span>
        </div>
      </div>

      <!-- 输入区（仅问卷阶段显示） -->
      <footer v-if="!done" class="composer">
        <div class="row">
          <input ref="inputBox" v-model="input" :disabled="sending"
                 placeholder="Write a message… 输入你的回答" @keyup.enter="send" />
          <button class="send" :disabled="sending" @click="send" title="发送">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
          <button class="ghost" :disabled="loading || sending" @click="startQuestionnaire">重新开始</button>
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

/* 计划结果 */
.plan { margin: 0 26px 22px; border-top: 1px solid var(--line); padding-top: 20px; }
.plan-head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
  margin-bottom: 18px;
}
.plan-head h2 { margin: 0; font-size: 20px; color: var(--ink); }
.plan-head p { margin: 6px 0 0; color: #687873; font-size: 12px; }

/* 全局天气徽章 */
.weather-badge {
  display: flex; align-items: center; gap: 10px;
  background: var(--roam-soft); padding: 10px 14px; border-radius: 14px;
  flex-shrink: 0;
}
.weather-badge .weather-icon { font-size: 28px; line-height: 1; }
.weather-badge b { display: block; color: var(--forest); font-size: 15px; font-weight: 700; }
.weather-badge span { font-size: 12px; color: #6f847b; }

/* 按天 Tabs */
.day-tabs { margin-top: 4px; }
.tab-bar {
  display: flex; gap: 6px; overflow-x: auto; padding-bottom: 12px;
  border-bottom: 2px solid var(--line);
  scrollbar-width: none;
}
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
.day-weather-row {
  display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px;
}
.weather-chip {
  display: inline-flex; align-items: center; gap: 5px;
  background: #f0f7f4; border: 1px solid #d5e4da;
  color: #3d6e5a; font-size: 12px; padding: 5px 10px; border-radius: 20px;
}
.budget-chip { background: #fef9ec; border-color: #f0dfb5; color: #a0712e; }

/* Markdown 内容 */
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

.plan-empty { text-align: center; padding: 24px; color: #98a59f; font-size: 13px; }

/* ─── 完整行程正文 ─────────────────────────────────────── */
.plan-detail {
  margin-top: 20px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
  overflow: hidden;
}

.plan-detail-inner {
  padding: 22px 24px;
  line-height: 1.8;
  font-size: 14px;
  color: #333;

  :deep(h1), :deep(h2), :deep(h3) {
    color: var(--forest);
    margin: 1em 0 0.5em;
    font-weight: 700;
  }
  :deep(h1) { font-size: 22px; }
  :deep(h2) { font-size: 18px; border-bottom: 1.5px solid var(--line); padding-bottom: 6px; }
  :deep(h3) { font-size: 15px; }
  :deep(p) { margin: 0.7em 0; }
  :deep(ul), :deep(ol) { padding-left: 22px; margin: 0.7em 0; }
  :deep(li) { margin: 0.3em 0; }
  :deep(strong) { color: var(--sunset); font-weight: 600; }
  :deep(em) { color: var(--sunset); font-style: italic; }
  :deep(code) {
    background: var(--roam-soft);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
  }
  :deep(blockquote) {
    border-left: 4px solid var(--sunset);
    margin: 1em 0;
    padding: 10px 16px;
    background: var(--sunset-soft);
    color: #66756f;
  }
  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 1em 0;
    font-size: 13px;
  }
  :deep(th) {
    background: var(--roam-soft);
    color: var(--forest);
    padding: 8px 12px;
    text-align: left;
    border-bottom: 2px solid var(--line);
  }
  :deep(td) {
    padding: 8px 12px;
    border-bottom: 1px solid var(--line);
  }
  :deep(tr:last-child td) { border-bottom: none; }
}

.plan .pois { margin-top: 16px; }
.plan .pois .lab { color: var(--forest); font-size: 12px; font-weight: 700; margin: 0 0 8px; }
.plan .pois span {
  display: inline-block; margin: 0 6px 6px 0; padding: 5px 11px;
  background: var(--roam-soft); color: var(--roam); border-radius: 16px; font-size: 12px;
}

/* 底部输入区 */
.composer { border-top: 1px solid var(--line); padding: 16px 22px 20px; background: var(--card); }

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
