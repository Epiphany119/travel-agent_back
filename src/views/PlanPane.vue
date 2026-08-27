<script setup lang="ts">
import { useAgentSessionStore } from '@/stores/agentSession'
const agent = useAgentSessionStore()
</script>

<template>
  <div class="plan-wrapper">
    <div class="plan-head">
      <div>
        <h2>{{ agent.plan.destination }} · {{ agent.dayTabs.length }} 天</h2>
        <p v-if="agent.plan.budget">预算 ¥{{ agent.plan.budget?.toLocaleString() }}</p>
      </div>
      <div v-if="agent.globalWeather" class="weather-badge">
        <span class="weather-icon">{{ agent.globalWeather.icon || '☀️' }}</span>
        <div>
          <b>{{ agent.globalWeather.text || '天气' }}</b>
          <span>{{ agent.globalWeather.tempMin || '--' }} ~ {{ agent.globalWeather.tempMax || '--' }}℃</span>
        </div>
      </div>
    </div>

    <div v-if="agent.dayTabs.length > 0" class="day-tabs">
      <div class="tab-bar">
        <button
          v-for="(tab, i) in agent.dayTabs"
          :key="i"
          class="tab-btn"
          :class="{ active: agent.activeDay === i }"
          @click="agent.activeDay = i"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="tab.weather" class="tab-temp">{{ tab.weather.temp }}°</span>
        </button>
      </div>
      <div class="tab-content">
        <div v-if="agent.dayTabs[agent.activeDay]" class="day-detail">
          <div v-if="agent.dayTabs[agent.activeDay].weather" class="day-weather-row">
            <div class="weather-chip"><span>{{ agent.dayTabs[agent.activeDay].weather.icon }}</span><span>{{ agent.dayTabs[agent.activeDay].weather.text }}</span></div>
            <div class="weather-chip"><span>🌡️</span><span>{{ agent.dayTabs[agent.activeDay].weather.tempMin }} ~ {{ agent.dayTabs[agent.activeDay].weather.tempMax }}℃</span></div>
            <div v-if="agent.dayTabs[agent.activeDay].budget" class="weather-chip budget-chip"><span>💰</span><span>¥{{ agent.dayTabs[agent.activeDay].budget }}</span></div>
          </div>
          <div v-if="agent.currentActivities.length" class="activity-cards">
            <article v-for="(activity, ai) in agent.currentActivities" :key="ai" class="activity-card">
              <img v-if="agent.imageMap[activity.name]" :src="agent.imageMap[activity.name]" :alt="activity.name" loading="lazy" />
              <div class="activity-card-body"><span>{{ activity.time || '行程' }}</span><h4>{{ activity.name }}</h4><p>{{ activity.location || activity.notes }}</p><small>⏱ {{ activity.duration ? Math.round(activity.duration / 60) : 1 }} 小时 · ¥{{ activity.cost || 0 }}</small></div>
            </article>
          </div>
          <div v-if="agent.dayTabs[agent.activeDay].html" class="plan-md" v-html="agent.dayTabs[agent.activeDay].html"></div>
        </div>
      </div>
    </div>

    <div v-if="agent.overview" class="plan-detail">
      <div class="plan-detail-inner" v-html="agent.renderMarkdown(agent.overview)"></div>
    </div>

    <div v-if="agent.plan.dataCache?.pois?.length" class="pois">
      <p class="lab">✓ 已整合景点数据</p>
      <span v-for="p in agent.plan.dataCache.pois" :key="p.name">{{ p.name }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.plan-wrapper { min-height: 0; height: 100%; display: flex; flex-direction: column; gap: 4px; }
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
.pois { margin-top: 16px; }
.pois .lab { color: var(--forest); font-size: 12px; font-weight: 700; margin: 0 0 8px; }
.pois span { display: inline-block; margin: 0 6px 6px 0; padding: 5px 11px; background: var(--roam-soft); color: var(--roam); border-radius: 16px; font-size: 12px; }
</style>

