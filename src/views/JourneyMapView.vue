<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { listJourneys, type JourneyDetail } from '@/api/user'

const router = useRouter()
const route = useRoute()
const mapEl = ref<HTMLDivElement | null>(null)

const loading = ref(true)
const journeys = ref<JourneyDetail[]>([])
let map: L.Map | null = null
let markers: L.Marker[] = []

// 每个旅程一个颜色，旗标按旅程着色
const palette = ['var(--sunset)', 'var(--forest)', '#3a5f8a', '#b96a3d', '#7a5d8a', '#b39ac4', '#c0392b', '#5a8a4a']

function flagIcon(color: string, label: string) {
  return L.divIcon({
    className: '',
    html: `
      <div class="jflag" style="--c:${color}">
        <span class="jflag-head">${label}</span>
        <span class="jflag-tip"></span>
      </div>`,
    iconSize: [28, 38],
    iconAnchor: [14, 38],
    popupAnchor: [0, -34]
  })
}

function popupHtml(p: JourneyDetail['points'][number], j: JourneyDetail) {
  const dest = j.journey.destination || ''
  const date = p.visitDate || [j.journey.startDate, j.journey.endDate].filter(Boolean).join(' ~ ') || ''
  const desc = p.description || j.journey.summary || '—'
  return `
    <div class="jpop">
      <div class="jpop-tag">旅程 · ${dest}</div>
      <h4>${p.name}</h4>
      <div class="jpop-date">${date ? '📅 ' + date : ''}</div>
      ${p.latitude ? `<div class="jpop-coord">📍 ${p.latitude}, ${p.longitude}</div>` : ''}
      <p>${desc}</p>
      <div class="jpop-actions">
        <a class="jpop-btn" href="/journeys?focus=${j.journey.id}">📋 查看详情</a>
      </div>
    </div>`
}

function escapeHtml(s: string) {
  return String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string))
}

async function initMap() {
  if (!mapEl.value) return

  const focusId = route.query.journeyId as string | undefined

  map = L.map(mapEl.value, { zoomControl: true, attributionControl: false })
  // 高德街道瓦片（无需 Key）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3
  }).addTo(map)

  try {
    const res = await listJourneys()
    journeys.value = res.data || []
  } catch (e) {
    console.error('加载旅程失败', e)
  } finally {
    loading.value = false
  }

  const latlngs: L.LatLngExpression[] = []
  const focusLatlngs: L.LatLngExpression[] = []
  journeys.value.forEach((j, jIdx) => {
    const color = palette[jIdx % palette.length]
    const isFocus = focusId && String(j.journey.id) === focusId
    ;(j.points || []).forEach((p, i) => {
      const lat = Number(p.latitude)
      const lng = Number(p.longitude)
      if (isNaN(lat) || isNaN(lng)) return
      const ll: L.LatLngExpression = [lat, lng]
      latlngs.push(ll)
      if (isFocus) focusLatlngs.push(ll)
      const label = escapeHtml(p.name.slice(0, 1))
      const m = L.marker(ll, { icon: flagIcon(color, label), title: p.name, opacity: isFocus || !focusId ? 1 : 0.25 })
        .bindPopup(popupHtml(p, j), { maxWidth: 300, closeButton: false, className: 'jpopup' })
      m.addTo(map!)
      markers.push(m)
    })
  })

  if (focusLatlngs.length > 0) {
    map.fitBounds(L.latLngBounds(focusLatlngs).pad(0.15))
  } else if (latlngs.length > 0) {
    map.fitBounds(L.latLngBounds(latlngs).pad(0.15))
  } else {
    // 默认视野：中国
    map.setView([31.2304, 121.4737], 5)
  }
}

onMounted(initMap)
onBeforeUnmount(() => {
  markers.forEach((m) => m.remove())
  if (map) { map.remove(); map = null }
})
</script>

<template>
  <div class="map-shell">
    <div ref="mapEl" class="map" v-loading="loading"></div>

    <!-- 左上角标题 -->
    <div class="map-title">
      <p class="eyebrow">TRAVEL MEMORIES</p>
      <h1>去过的地方</h1>
      <p class="sub">把每一段足迹，插上回忆的旗。</p>
    </div>

    <!-- 右下角图例 -->
    <div class="legend">
      <div class="legend-head">
        <b>{{ markers.length }}</b>
        <span>个地点 · {{ journeys.length }} 段旅程</span>
      </div>
      <div class="legend-list">
        <div v-for="(j, i) in journeys" :key="j.journey.id" class="legend-item">
          <i class="dot" :style="{ background: palette[i % palette.length] }"></i>
          <span>{{ j.journey.destination }}</span>
          <em v-if="(j.points || []).length">· {{ j.points.length }} 处</em>
        </div>
      </div>
    </div>

    <div class="hint">点击地图上的旗子，再点击「查看详情」打开行程卡片</div>
  </div>
</template>

<style scoped lang="scss">
/* 填满内容区剩余高度（侧边栏布局下） */
.map-shell { position: relative; flex: 1; min-height: 460px; overflow: hidden; }
.map { position: absolute; inset: 0; z-index: 0; }

/* 左上标题 */
.map-title {
  position: absolute; top: 24px; left: 24px; z-index: 1000;
  background: rgba(255,254,250,.9); border: 1px solid var(--line); border-radius: 18px; padding: 16px 20px;
  box-shadow: 0 8px 24px rgba(22,78,66,.10); backdrop-filter: blur(6px);
}
.map-title .eyebrow { color: var(--sunset); font-size: 10px; font-weight: 800; letter-spacing: .18em; margin: 0 0 4px; }
.map-title h1 { font: 26px "DM Serif Display","Noto Sans SC"; color: var(--ink); margin: 0; }
.map-title .sub { color: #687873; font-size: 12px; margin: 6px 0 0; }

/* 右下图例 */
.legend {
  position: absolute; right: 20px; bottom: 24px; z-index: 1000;
  width: 210px; background: rgba(255,254,250,.94); border: 1px solid var(--line); border-radius: 16px; padding: 14px 16px;
  box-shadow: 0 10px 30px rgba(22,78,66,.14); backdrop-filter: blur(6px);
}
.legend-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 10px; border-bottom: 1px solid var(--line); padding-bottom: 10px; }
.legend-head b { color: var(--sunset); font-size: 24px; font-weight: 800; }
.legend-head span { color: #8a9792; font-size: 12px; }
.legend-list { display: flex; flex-direction: column; gap: 7px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--ink); }
.legend-item .dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-item em { color: #8a9792; font-style: normal; }

/* 底部提示 */
.hint {
  position: absolute; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 1000;
  background: rgba(255,254,250,.92); color: #4a5f58; font-size: 12px; padding: 8px 16px; border-radius: 24px;
  border: 1px solid var(--line); box-shadow: 0 4px 14px rgba(22,78,66,.10); white-space: nowrap;
}

/* 旗标（图钉） */
:deep(.jflag) { position: relative; width: 28px; height: 38px; filter: drop-shadow(0 3px 4px rgba(0,0,0,.35)); cursor: pointer; }
:deep(.jflag-head) {
  position: absolute; top: 0; left: 0; width: 28px; height: 28px; border-radius: 50% 50% 50% 0;
  background: var(--c); transform: rotate(-45deg); border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center;
}
:deep(.jflag-head)::after {
  content: ''; position: absolute; inset: 0; border-radius: inherit; transform: rotate(45deg);
  display: flex; align-items: center; justify-content: center;
}
:deep(.jflag-head) { color: #fff; font-weight: 800; font-size: 12px; }
</style>

<style>
/* 弹窗样式（非 scoped，因为挂在 Leaflet 的 overlay 上） */
.jpop { font-family: "Noto Sans SC","Manrope",sans-serif; }
.jpop-tag { display: inline-block; background: var(--roam-soft); color: var(--forest); font-size: 11px; font-weight: 700; padding: 2px 10px; border-radius: 24px; margin-bottom: 8px; }
.jpop h4 { margin: 0 0 6px; color: var(--ink); font-size: 18px; font-family: "DM Serif Display","Noto Sans SC",serif; }
.jpop-date, .jpop-coord { color: #687873; font-size: 12px; margin-bottom: 4px; }
.jpop p { color: #4a5f58; font-size: 13px; line-height: 1.6; margin: 8px 0 0; }
.jpop-actions { margin-top: 12px; }
.jpop-btn {
  display: inline-block;
  background: var(--forest);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 8px 18px;
  border-radius: 24px;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.2s;
}
.jpop-btn:hover { background: var(--roam); }
.leaflet-popup-content-wrapper { border-radius: 16px; box-shadow: 0 12px 32px rgba(22,78,66,.18); }
.leaflet-popup-content { margin: 14px 16px; }
</style>
