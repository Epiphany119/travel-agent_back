<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { subscribeA2AStream, type TravelPlan } from '@/api/agent'
import { useStreamStore } from '@/stores/stream'
import { useUserStore } from '@/stores/user'
import DayPlanCard from '@/components/DayPlanCard.vue'

const streamStore = useStreamStore()
const userStore = useUserStore()

const destination = ref('杭州')
const days = ref(3)
const budget = ref(3000)
const travelers = ref(2)
const travelStyle = ref('轻松漫游')
const interests = ref(['美食', '人文'])
const travelPlan = ref<TravelPlan | null>(null)
const activeDay = ref(1)
const styles = ['轻松漫游', '深度人文', '美食优先', '亲子友好']
const interestOptions = ['美食', '人文', '自然', '摄影', '购物', '夜生活']

const dayBudget = computed(() => (travelPlan.value?.dayPlans || [])[activeDay.value - 1]?.dayBudget)
const currentDayPlan = computed(() => (travelPlan.value?.dayPlans || [])[activeDay.value - 1])

const formatContent = (content: string) => marked(content)

let cancelStream: (() => void) | null = null

// ─── 流式生成（通过 A2A 后端 SSE）──────────────────────────────────────

async function generateStream() {
  if (!destination.value.trim()) return ElMessage.warning('先告诉我想去哪里')
  if (streamStore.isStreaming) return

  streamStore.reset()
  streamStore.isStreaming = true
  travelPlan.value = null
  activeDay.value = 1

  // 连接 A2A 后端 SSE 流
  cancelStream = subscribeA2AStream({
    destination: destination.value,
    days: days.value,
    budget: budget.value,
    travelers: travelers.value,
    travelStyle: travelStyle.value,
    interests: interests.value
  }, (event) => {
    switch (event.name) {
      case 'task_update':
        // 任务状态更新，可忽略或展示
        break
      case 'tool_call':
        streamStore.toolCalls.push({
          name: event.data?.source || '未知工具',
          args: event.data
        })
        break
      case 'tool_result':
        streamStore.currentToolResult = event.data?.message || event.data?.type || '完成'
        break
      case 'token':
        if (typeof event.data === 'string') {
          streamStore.fullText += event.data
        }
        break
      case 'task_done':
        streamStore.planId = event.data?.planId || null
        streamStore.isStreaming = false
        ElMessage.success('行程规划完成！')
        break
      case 'error':
        streamStore.error = event.data?.message || '未知错误'
        streamStore.isStreaming = false
        break
      default:
        break
    }
  })
}

function cancelStreaming() {
  if (cancelStream) {
    cancelStream()
    cancelStream = null
  }
  streamStore.reset()
}

</script>

<template>
  <main class="app-shell">
    <nav class="topbar">
      <a class="brand"><span>✦</span> roamly</a>
      <div class="nav-links">
        <a @click="$router.push('/inspirations')">灵感目的地</a>
        <a @click="$router.push('/journeys')">我的旅程</a>
        <button class="avatar" @click="$router.push('/profile')">
          <img v-if="userStore.avatar" :src="userStore.avatar" alt="头像" />
          <span v-else>{{ userStore.nickname }}</span>
        </button>
      </div>
    </nav>

    <section class="hero">
      <p class="eyebrow">TRAVEL, THOUGHTFULLY</p>
      <h1>把期待，变成一趟<br><em>刚刚好的旅行。</em></h1>
      <p class="sub">告诉我目的地、时间和预算。Roamly 会把复杂的功课，整理成可以立刻出发的每一天。</p>
      <div class="trust">
        <span>✦ 路线按区域串联</span>
        <span>◌ 预算清晰可控</span>
        <span>⌁ 出发前避坑提醒</span>
      </div>
    </section>

    <section class="planner-card">
      <div class="planner-title">
        <div>
          <p class="eyebrow">START PLANNING</p>
          <h2>你的旅行偏好</h2>
        </div>
        <span>01 / 02</span>
      </div>

      <div class="form-grid">
        <label class="field destination">
          <span>目的地</span>
          <input v-model="destination" maxlength="30" placeholder="例如：杭州" />
        </label>
        <label class="field">
          <span>旅行天数</span>
          <div class="number-control">
            <button @click="days = Math.max(1, days - 1)">−</button>
            <b>{{ days }} 天</b>
            <button @click="days = Math.min(14, days + 1)">+</button>
          </div>
        </label>
        <label class="field">
          <span>总预算（元）</span>
          <input v-model.number="budget" type="number" min="300" max="200000" />
        </label>
        <label class="field">
          <span>同行人数</span>
          <div class="number-control">
            <button @click="travelers = Math.max(1, travelers - 1)">−</button>
            <b>{{ travelers }} 人</b>
            <button @click="travelers = Math.min(12, travelers + 1)">+</button>
          </div>
        </label>
      </div>

      <div class="choice-row">
        <div>
          <span>旅行节奏</span>
          <div class="chips">
            <button
              v-for="item in styles"
              :key="item"
              :class="{ selected: travelStyle === item }"
              @click="travelStyle = item"
            >{{ item }}</button>
          </div>
        </div>
        <div>
          <span>这次更想要</span>
          <div class="chips">
            <button
              v-for="item in interestOptions"
              :key="item"
              :class="{ selected: interests.includes(item) }"
              @click="interests.includes(item) ? interests = interests.filter(x => x !== item) : interests = [...interests, item]"
            >{{ item }}</button>
          </div>
        </div>
      </div>

      <div class="generate-row">
        <button
          class="generate generate--stream"
          :disabled="streamStore.isStreaming"
          @click="generateStream"
        >
          <span>{{ streamStore.isStreaming ? 'AI 正在为你规划行程…' : '✨ 生成专属旅行计划' }}</span>
          <i v-if="!streamStore.isStreaming">→</i>
          <i v-else class="spinner">⟳</i>
        </button>
        <button
          v-if="streamStore.isStreaming"
          class="generate generate--cancel"
          @click="cancelStreaming"
        >
          <span>取消</span>
        </button>
        <button
          v-if="streamStore.error"
          class="generate generate--retry"
          @click="generateStream"
        >
          <span>重新生成</span>
          <i>↻</i>
        </button>
      </div>
    </section>

    <!-- ─── 流式输出区域 ─────────────────────────────────────────────────── -->
    <section
      v-if="streamStore.isStreaming || streamStore.fullText || streamStore.dayPlans.length > 0"
      class="result stream-result"
    >
      <div class="result-head">
        <div>
          <p class="eyebrow">STREAMING OUTPUT</p>
          <h2>{{ destination }} · {{ days }} 天旅程</h2>
          <p>AI 实时生成中，请稍候…</p>
        </div>
        <div class="budget">
          <span>预算</span>
          <b>¥{{ budget.toLocaleString() }}</b>
          <small>{{ budget >= 5000 ? '高端享受' : budget >= 2000 ? '舒适经济' : '经济实惠' }}</small>
        </div>
      </div>

      <!-- 打字机效果文本 -->
      <div v-if="streamStore.fullText" class="stream-container">
        <div class="stream-text" v-html="formatContent(streamStore.fullText)"></div>

        <!-- 工具调用进度 -->
        <div v-if="streamStore.toolCalls.length > 0" class="tool-progress">
          <div
            v-for="(tool, idx) in streamStore.toolCalls"
            :key="idx"
            class="tool-calling"
          >
            <span class="tool-badge tool-badge--calling">
              <span class="loading-dot"></span>
              🔧 调用中: {{ tool.name }}
            </span>
            <span
              v-if="streamStore.currentToolResult && idx === streamStore.toolCalls.length - 1"
              class="tool-badge tool-badge--done"
            >
              ✅ {{ streamStore.currentToolResult }}
            </span>
          </div>
        </div>

        <!-- DayPlan 卡片（实时追加） -->
        <div v-if="streamStore.dayPlans.length > 0" class="day-plans">
          <DayPlanCard
            v-for="plan in streamStore.dayPlans"
            :key="plan.dayNumber"
            :plan="plan"
          />
        </div>
      </div>

      <!-- 流式错误 -->
      <div v-if="streamStore.error" class="stream-error">
        <strong>⚠️ 出错了：</strong>{{ streamStore.error }}
      </div>
    </section>

    <!-- ─── 传统同步结果（保持兼容）────────────────────────────────────── -->
    <section v-if="travelPlan && !streamStore.isStreaming" class="result">
      <div class="result-head">
        <div>
          <p class="eyebrow">YOUR ITINERARY</p>
          <h2>{{ destination }} · {{ days }} 天旅程</h2>
          <p>AI 生成的个性化旅行方案</p>
        </div>
        <div class="budget">
          <span>预算</span>
          <b>¥{{ budget.toLocaleString() }}</b>
          <small>{{ budget >= 5000 ? '高端享受' : budget >= 2000 ? '舒适经济' : '经济实惠' }}</small>
        </div>
      </div>

      <template v-if="travelPlan">
        <div class="days">
          <button
            v-for="day in days"
            :key="day"
            :class="{ active: activeDay === day }"
            @click="activeDay = day"
          >
            <small>DAY</small><b>{{ String(day).padStart(2, '0') }}</b>
          </button>
        </div>

        <div v-if="currentDayPlan" class="day-card">
          <div class="day-title">
            <div>
              <p>DAY {{ String(currentDayPlan.dayNumber).padStart(2, '0') }}</p>
              <h3>{{ currentDayPlan.theme }}</h3>
            </div>
            <b>预算 ¥{{ currentDayPlan.dayBudget?.toLocaleString() }}</b>
          </div>

          <div class="timeline">
            <div v-for="(attraction, idx) in currentDayPlan.attractions" :key="idx" class="timeline-item">
              <span>{{ idx + 1 }}</span>
              <div>
                <h4>{{ attraction.name }}</h4>
                <p>{{ attraction.description }}</p>
                <small>游览约 {{ attraction.duration }} 小时 | 门票 ¥{{ attraction.ticketPrice }}</small>
              </div>
            </div>
          </div>

          <div v-if="currentDayPlan.meals?.length" class="meal-grid">
            <div v-for="meal in currentDayPlan.meals" :key="meal.mealType" class="meal">
              <span>{{ meal.mealType }}</span>
              <h4>{{ meal.restaurantName }}</h4>
              <p>{{ meal.cuisine }} · 人均 ¥{{ meal.avgPrice }}</p>
              <small>{{ meal.reason }}</small>
            </div>
          </div>

          <div v-if="currentDayPlan.transportation" class="notice">
            <strong>交通：</strong>{{ currentDayPlan.transportation }}
          </div>
        </div>

        <div v-if="travelPlan.overview" class="ai-content">
          <h3>📋 行程概览</h3>
          <p>{{ travelPlan.overview }}</p>
        </div>

        <div class="tips">
          <div v-if="travelPlan.travelTips?.length">
            <h3>✦ 旅行贴士</h3>
            <p v-for="tip in travelPlan.travelTips" :key="tip">{{ tip }}</p>
          </div>
          <div v-if="travelPlan.packingList?.length">
            <h3>⌁ 打包清单</h3>
            <div class="packing">
              <span v-for="item in travelPlan.packingList" :key="item">{{ item }}</span>
            </div>
          </div>
        </div>
      </template>
    </section>
  </main>

</template>

<style scoped lang="scss">
@import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=Manrope:wght@400;500;600;700;800&family=Noto+Sans+SC:wght@400;500;600;700;800&display=swap');

:global(*) { box-sizing: border-box; }
:global(body) { margin: 0; background: #f4f1ea; color: #172c29; font-family: Manrope, "Noto Sans SC", sans-serif; }

.app-shell {
  min-height: 100vh;
  padding-bottom: 80px;
  background: radial-gradient(circle at 86% 5%, #d8ede2 0, transparent 23rem),
              radial-gradient(circle at 5% 30%, #fff7e4 0, transparent 24rem);
}

.topbar {
  height: 74px;
  width: min(1160px, calc(100% - 40px));
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand { font: 32px "DM Serif Display"; letter-spacing: -1px; }
.brand span { color: #ee7348; font-size: 27px; }

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
  font-size: 13px;
  font-weight: 700;
  color: #61716b;
}

.avatar {
  border: 0;
  background: #193d35;
  color: #fff;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-weight: 800;
  cursor: pointer;
  padding: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.05);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span {
    line-height: 1;
  }
}

.hero {
  width: min(860px, calc(100% - 40px));
  text-align: center;
  padding: 70px 0 45px;
}

.eyebrow {
  color: #d86a43;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
  margin: 0 0 10px;
}

.hero h1 {
  font: 54px/1.08 "DM Serif Display", "Noto Sans SC";
  letter-spacing: -1.6px;
  margin: 0;
}

.hero h1 em { font-style: normal; color: #4b8a73; }

.sub {
  max-width: 560px;
  margin: 19px auto;
  color: #687873;
  line-height: 1.8;
  font-size: 14px;
}

.trust {
  display: flex;
  justify-content: center;
  gap: 22px;
  color: #587368;
  font-size: 11px;
  font-weight: 700;
  margin-top: 24px;
}

.planner-card, .result {
  width: min(1020px, calc(100% - 40px));
  margin: auto;
  background: #fffefa;
  border: 1px solid #e7e1d5;
  border-radius: 27px;
  box-shadow: 0 20px 55px #293d3210;
}

.planner-card { padding: 34px 38px; }

.planner-title, .result-head, .day-title {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.planner-title h2, .result-head h2 { font-size: 25px; margin: 0; }
.planner-title > span { font-size: 12px; color: #98a59f; }

.form-grid {
  display: grid;
  grid-template-columns: 1.5fr repeat(3, 1fr);
  gap: 12px;
  margin: 27px 0;
}

.field {
  border: 1px solid #e4e4dd;
  border-radius: 13px;
  padding: 10px 14px;
  display: block;
}

.field span, .choice-row > div > span {
  font-size: 11px;
  color: #7c8a84;
  font-weight: 700;
  display: block;
  margin-bottom: 6px;
}

.field input {
  width: 100%;
  border: 0;
  background: transparent;
  font: 700 16px Manrope;
  color: #193d35;
  outline: 0;
}

.number-control {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.number-control button {
  border: 0;
  background: #edf3ec;
  border-radius: 7px;
  width: 22px;
  height: 22px;
  color: #37705d;
  font-size: 16px;
  cursor: pointer;
}

.number-control b { font-size: 14px; }

.choice-row {
  border-top: 1px solid #eee9df;
  padding-top: 21px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 26px;
}

.chips { display: flex; gap: 7px; flex-wrap: wrap; }

.chips button {
  border: 1px solid #e4e4dd;
  background: white;
  padding: 7px 11px;
  border-radius: 20px;
  color: #66756f;
  font-size: 12px;
  cursor: pointer;
}

.chips button.selected {
  background: #183d34;
  color: white;
  border-color: #183d34;
}

.generate-row {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.generate {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 0;
  border-radius: 13px;
  padding: 15px 18px 15px 21px;
  font-weight: 800;
  font-size: 14px;
  cursor: pointer;
  flex: 1;
}

.generate:disabled { opacity: 0.65; cursor: not-allowed; }

.generate i { font-size: 24px; font-style: normal; }

.generate--stream {
  background: linear-gradient(135deg, #4b8a73, #2d6a4f);
  color: white;
  box-shadow: 0 10px 20px #2d6a4f40;
}

.generate--cancel {
  background: #909399;
  color: white;
  flex: 0;
  min-width: 80px;
}

.generate--retry {
  background: #ec7249;
  color: white;
  box-shadow: 0 10px 20px #ec724940;
  flex: 0;
  min-width: 120px;
}

.spinner {
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.result { margin-top: 25px; padding: 35px 38px; }

.stream-result {
  border: 2px dashed #4b8a73;
  background: #f0faf6;
}

.result-head > div > p:not(.eyebrow) {
  color: #687873;
  font-size: 13px;
  max-width: 600px;
  line-height: 1.7;
}

.budget {
  min-width: 190px;
  background: #eef6ed;
  padding: 17px;
  border-radius: 15px;
}

.budget span, .budget small { display: block; font-size: 11px; color: #6f847b; }
.budget b { display: block; font: 27px "DM Serif Display"; color: #1e5d48; margin: 4px 0; }

.days { display: flex; gap: 8px; margin: 28px 0 16px; }

.days button {
  border: 0;
  background: #f1f0ea;
  border-radius: 11px;
  padding: 9px 17px;
  color: #788780;
  cursor: pointer;
}

.days small, .days b { display: block; }
.days small { font-size: 8px; letter-spacing: 0.1em; }
.days b { font-size: 18px; }
.days button.active { background: #1c473c; color: white; }

.day-card {
  border: 1px solid #e8e6dc;
  border-radius: 18px;
  padding: 25px;
}

.day-title p { color: #d36a45; font-size: 11px; font-weight: 800; margin: 0; }
.day-title h3 { margin: 7px 0; font-size: 21px; }
.day-title > b { font-size: 13px; color: #41836a; }

.timeline { padding: 20px 0 7px; }

.timeline-item {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 12px;
  padding-bottom: 18px;
  position: relative;
}

.timeline-item > span {
  width: 25px;
  height: 25px;
  background: #dcece0;
  color: #287055;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
}

.timeline-item h4, .meal h4 { margin: 0 0 5px; font-size: 14px; }
.timeline-item p, .meal p { margin: 0; color: #6e7d77; font-size: 12px; line-height: 1.6; }
.timeline-item small, .meal small { color: #a06445; font-size: 11px; }

.meal-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.meal {
  background: #fbf4ea;
  border-radius: 12px;
  padding: 15px;
}

.meal > span { font-size: 10px; font-weight: 800; color: #d36a45; }

.notice {
  font-size: 11px;
  line-height: 1.7;
  color: #62746d;
  margin: 20px 0 0;
  background: #f4f8f5;
  padding: 12px;
  border-radius: 9px;
}

.tips { display: grid; grid-template-columns: 1.2fr 1fr; gap: 40px; margin-top: 28px; }
.tips h3 { font-size: 16px; margin: 0 0 10px; }
.tips p { font-size: 12px; color: #65756f; line-height: 1.8; margin: 3px 0; }

.packing { display: flex; flex-wrap: wrap; gap: 8px; }
.packing span {
  font-size: 11px;
  background: #eef6ed;
  color: #34745b;
  border-radius: 15px;
  padding: 6px 9px;
}

/* ─── 流式输出样式 ─────────────────────────────────────────────────────────── */

.stream-container {
  padding: 20px 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8faff, #f0f4ff);
  margin-top: 20px;
  border: 1px solid #d0e8ff;
}

.stream-text {
  line-height: 1.8;
  color: #303133;
  font-size: 15px;
}

.tool-progress {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-calling {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
}

.tool-badge--calling {
  background: #fff7e6;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.tool-badge--done {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #c2e7b0;
}

.loading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e6a23c;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.day-plans {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
}

.stream-error {
  margin-top: 16px;
  padding: 14px 18px;
  background: #fef0f0;
  border: 1px solid #f5c6cb;
  border-radius: 8px;
  color: #c0392b;
  font-size: 14px;
}

/* AI 内容样式 */
.ai-content {
  margin-top: 20px;
  padding: 25px;
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e8e6dc;
  line-height: 1.8;
  font-size: 14px;
  color: #333;
}

.ai-content :deep(h1),
.ai-content :deep(h2),
.ai-content :deep(h3) {
  color: #1c473c;
  margin: 1em 0 0.5em;
  font-weight: 700;
}

.ai-content :deep(h1) { font-size: 24px; }
.ai-content :deep(h2) { font-size: 20px; }
.ai-content :deep(h3) { font-size: 16px; }

.ai-content :deep(p) { margin: 0.8em 0; }

.ai-content :deep(ul),
.ai-content :deep(ol) { padding-left: 24px; margin: 0.8em 0; }

.ai-content :deep(li) { margin: 0.4em 0; }

.ai-content :deep(strong) { color: #d36a45; font-weight: 600; }

.ai-content :deep(code) {
  background: #f4f8f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.ai-content :deep(pre) {
  background: #2d3748;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
}

.ai-content :deep(blockquote) {
  border-left: 4px solid #ec7249;
  margin: 1em 0;
  padding: 10px 16px;
  background: #fff7f4;
  color: #66756f;
}

@media (max-width: 700px) {
  .topbar { width: calc(100% - 26px); }
  .nav-links a { display: none; }
  .hero { padding: 45px 0 30px; }
  .hero h1 { font-size: 39px; }
  .trust { gap: 9px; font-size: 9px; }
  .planner-card, .result { width: calc(100% - 26px); padding: 23px 18px; }
  .form-grid, .choice-row, .tips, .generate-row { grid-template-columns: 1fr; }
  .result-head { display: block; }
  .budget { margin-top: 15px; }
  .meal-grid { grid-template-columns: 1fr; }
  .days { overflow: auto; }
  .days button { flex-shrink: 0; }
}
</style>
