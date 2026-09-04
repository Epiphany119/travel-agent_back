<script setup lang="ts">
import { computed, ref, watch, KeepAlive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatView from './ChatView.vue'
import AgentPanel from './AgentPanel.vue'

const props = defineProps<{ fromRightPanel?: boolean }>()
const route = useRoute()
const router = useRouter()
type PlannerMode = 'form' | 'agent'

const mode = ref<PlannerMode>(route.query.mode === 'agent' ? 'agent' : 'form')
const activeComponent = computed(() => mode.value === 'form' ? ChatView : AgentPanel)

watch(() => route.query.mode, (value) => {
  mode.value = value === 'agent' ? 'agent' : 'form'
})

function selectMode(next: PlannerMode) {
  mode.value = next
  router.replace({
    path: '/chat',
    query: next === 'agent' ? { mode: 'agent' } : {}
  })
}
</script>

<template>
  <main class="planner-hub" :class="{ 'is-embedded': props.fromRightPanel }">
    <header class="planner-hub-head">
      <div>
        <p class="eyebrow">ROAMLY PLANNER</p>
        <h1>Agent 旅行规划</h1>
        <p class="sub">同一份旅行目标，既可以快速填写，也可以交给 Agent 深聊。</p>
      </div>
      <div class="planner-status"><span></span> 工作区已就绪</div>
    </header>

    <div class="planner-body">
      <nav class="planner-tabs" role="tablist" aria-label="旅行规划方式">
        <button type="button" role="tab" :aria-selected="mode === 'form'" :class="{ active: mode === 'form' }" @click="selectMode('form')">
          <span>▦</span><b>快速规划</b><small>固定表单</small>
        </button>
        <button type="button" role="tab" :aria-selected="mode === 'agent'" :class="{ active: mode === 'agent' }" @click="selectMode('agent')">
          <span>✦</span><b>对话 Agent</b><small>逐步协作</small>
        </button>
      </nav>

      <section class="planner-canvas">
        <KeepAlive>
          <component
            :is="activeComponent"
            :key="mode"
            :from-right-panel="props.fromRightPanel"
            :embedded="mode === 'agent' && !props.fromRightPanel"
          />
        </KeepAlive>
      </section>
    </div>
  </main>
</template>

<style scoped lang="scss">
.planner-hub { width: 100%; height: 100%; min-height: 0; display: flex; flex-direction: column; overflow: hidden; background: radial-gradient(circle at 90% 0, color-mix(in srgb, var(--roam-soft) 74%, transparent), transparent 28rem); color: var(--ink); }
.planner-hub-head { flex: 0 0 auto; display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; padding: 26px 34px 15px; }
.eyebrow { margin: 0 0 8px; color: var(--sunset); font-size: 10px; font-weight: 900; letter-spacing: .18em; }
.planner-hub-head h1 { margin: 0; font: 31px 'DM Serif Display', 'Noto Sans SC'; }
.sub { margin: 8px 0 0; color: var(--ink-2); font-size: 13px; }
.planner-status { display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; border: 1px solid var(--line); border-radius: 999px; background: color-mix(in srgb, var(--card) 80%, transparent); color: var(--ink-2); font-size: 11px; font-weight: 800; white-space: nowrap; }
.planner-status span { width: 8px; height: 8px; border-radius: 50%; background: var(--forest); box-shadow: 0 0 0 4px color-mix(in srgb, var(--forest) 12%, transparent); }
.planner-body { flex: 1 1 auto; min-height: 0; display: flex; overflow: hidden; }
.planner-tabs { flex: 0 0 188px; align-self: stretch; display: flex; flex-direction: column; gap: 6px; margin: 0 0 0 34px; padding: 6px; border: 1px solid var(--line); border-radius: 15px; background: color-mix(in srgb, var(--card) 72%, transparent); overflow: auto; }
.planner-tabs button { display: grid; grid-template-columns: 26px 1fr; grid-template-rows: 1fr 1fr; column-gap: 8px; align-items: center; width: 100%; flex: 0 0 auto; min-width: 0; padding: 10px 12px; border: 0; border-radius: 11px; background: transparent; color: var(--ink-2); text-align: left; cursor: pointer; transition: background .18s, color .18s, transform .18s; }
.planner-tabs button > span { grid-row: 1 / 3; display: grid; place-items: center; width: 26px; height: 26px; border-radius: 8px; background: var(--roam-soft); color: var(--forest); font-size: 15px; }
.planner-tabs button b { font-size: 13px; }
.planner-tabs button small { font-size: 10px; color: var(--ink-3); }
.planner-tabs button:hover { transform: translateY(-1px); color: var(--forest); }
.planner-tabs button.active { background: var(--forest); color: #fff; box-shadow: var(--shadow-soft); }
.planner-tabs button.active > span { background: color-mix(in srgb, #fff 18%, transparent); color: #fff; }
.planner-tabs button.active small { color: color-mix(in srgb, #fff 70%, transparent); }
.planner-canvas { flex: 1 1 auto; min-width: 0; min-height: 0; margin: 0 34px 0 12px; padding-left: 18px; border-left: 1px solid color-mix(in srgb, var(--line) 78%, transparent); overflow: auto; }
.planner-canvas :deep(> *) { min-height: 100%; }
.planner-hub.is-embedded .planner-tabs { flex-basis: 142px; margin-left: 14px; }
.planner-hub.is-embedded .planner-canvas { margin-right: 14px; padding-left: 12px; }
@media (max-width: 700px) {
  .planner-hub-head { align-items: flex-start; padding: 20px 16px 12px; }
  .planner-hub-head h1 { font-size: 27px; }
  .planner-status { display: none; }
  .planner-body { flex-direction: column; overflow: hidden; }
  .planner-tabs,
  .planner-hub.is-embedded .planner-tabs { flex: 0 0 auto; flex-direction: row; align-self: auto; margin: 0 16px 8px; }
  .planner-tabs button { min-width: 0; flex: 1 1 0; padding: 8px 10px; }
  .planner-canvas,
  .planner-hub.is-embedded .planner-canvas { margin: 0; padding: 0 16px; border-left: 0; overflow: auto; }
}
</style>

<style scoped>
.planner-hub {
  position: relative;
  isolation: isolate;
  background:
    radial-gradient(circle at 80% 2%, color-mix(in srgb, var(--roam) 15%, transparent), transparent 22rem),
    linear-gradient(135deg, color-mix(in srgb, var(--sunset) 4%, transparent), transparent 42%),
    var(--paper);
}

.planner-hub::before {
  content: 'PLAN / BUILD / ROAM';
  position: absolute;
  top: 22px;
  right: 34px;
  color: color-mix(in srgb, var(--forest) 28%, transparent);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: .18em;
  pointer-events: none;
}

.planner-hub-head {
  position: relative;
  margin: 20px 34px 0;
  padding: 24px 26px 20px;
  border: 1px solid color-mix(in srgb, var(--forest) 12%, var(--line));
  border-radius: 22px;
  background: linear-gradient(115deg, color-mix(in srgb, var(--card) 90%, var(--sunset) 10%), color-mix(in srgb, var(--card) 92%, var(--roam) 8%));
  box-shadow: var(--shadow-soft), inset 0 1px 0 rgba(255,255,255,.7);
}

.planner-hub-head h1 { letter-spacing: -.025em; }
.planner-status { background: color-mix(in srgb, var(--card) 82%, var(--roam-soft)); }
.planner-body { padding-top: 18px; }
.planner-tabs { background: color-mix(in srgb, var(--card) 82%, transparent); box-shadow: inset 0 1px 0 rgba(255,255,255,.65); }
.planner-tabs button { min-height: 64px; }
.planner-tabs button.active { background: linear-gradient(145deg, var(--forest), var(--forest-deep)); box-shadow: 0 10px 20px color-mix(in srgb, var(--forest) 18%, transparent); }
.planner-canvas { padding-top: 4px; }
.planner-canvas::before { content: ''; display: block; height: 1px; margin: 0 0 12px; background: linear-gradient(90deg, color-mix(in srgb, var(--sunset) 50%, var(--line)), transparent 70%); }

@media (max-width: 700px) {
  .planner-hub::before { display: none; }
  .planner-hub-head { margin: 12px 16px 0; padding: 20px 18px 17px; }
  .planner-body { padding-top: 10px; }
}
</style>
