// 一次性批量把旧配色替换为 Roamly 品牌设计令牌
const fs = require('fs')
const path = require('path')

const files = [
  'src/components/AppHeader.vue',
  'src/components/DayPlanCard.vue',
  'src/views/ChatView.vue',
  'src/views/InspirationView.vue',
  'src/views/JourneyView.vue',
  'src/views/UserProfile.vue',
  'src/views/AgentPanel.vue',
  'src/views/JourneyMapView.vue'
]

// 顺序敏感：长串/带透明度的先替换
const replaces = [
  // 渐变按钮 → 纯色 Forest（克制、SaaS 感）
  ['linear-gradient(135deg, #4b8a73, #2d6a4f)', 'var(--forest)'],
  ['linear-gradient(135deg,#2d6a4f,#4b8a73)', 'linear-gradient(135deg, var(--forest), var(--roam))'],
  // 8 位 hex（带 alpha）
  ['#2d6a4f30', 'rgba(22,78,66,.18)'],
  ['#2d6a4f40', 'rgba(22,78,66,.25)'],
  ['#ec724940', 'rgba(242,122,79,.25)'],
  ['#293d3210', 'rgba(22,78,66,.06)'],
  // rgba() 旧色 → 新色
  ['rgba(45,106,79,', 'rgba(22,78,66,'],
  ['rgba(238,115,72,', 'rgba(242,122,79,'],
  ['rgba(25,61,53,', 'rgba(22,78,66,'],
  // 深绿系 → Forest / Roam
  ['#1c473c', 'var(--forest)'],
  ['#183d34', 'var(--forest)'],
  ['#1e5d48', 'var(--forest)'],
  ['#2d6a4f', 'var(--forest)'],
  ['#287055', 'var(--roam)'],
  ['#34745b', 'var(--roam)'],
  ['#37705d', 'var(--roam)'],
  ['#41836a', 'var(--roam)'],
  ['#4b8a73', 'var(--roam)'],
  // 橙色系 → Sunset
  ['#ee7348', 'var(--sunset)'],
  ['#d86a43', 'var(--sunset)'],
  ['#d36a45', 'var(--sunset)'],
  ['#ec7249', 'var(--sunset)'],
  ['#a06445', 'var(--sunset)'],
  // 文字墨色
  ['#193d35', 'var(--ink)'],
  ['#172c29', 'var(--ink)'],
  ['#33443f', 'var(--ink)'],
  // 背景 / 卡片
  ['#f4f1ea', 'var(--paper)'],
  ['#fffefa', 'var(--card)'],
  ['#fff7f4', 'var(--sunset-soft)'],
  ['#fef0f0', 'var(--sunset-soft)'],
  ['#f5c6cb', '#F2C4B4'],
  // 边线
  ['#e7e1d5', 'var(--line)'],
  ['#e4e4dd', 'var(--line)'],
  ['#e8e6dc', 'var(--line)'],
  ['#d8d5ce', 'var(--line)'],
  ['#e0dcd2', 'var(--line)'],
  ['#eee9df', 'var(--line)'],
  ['#ece7dc', 'var(--line)'],
  ['#eae4d6', 'var(--line)'],
  ['#e5c0bb', '#EAC7BC'],
  // 柔和绿面
  ['#eef6ed', 'var(--roam-soft)'],
  ['#eef3f0', 'var(--roam-soft)'],
  ['#f4f8f5', 'var(--roam-soft)'],
  ['#dcece0', 'var(--roam-soft)'],
  ['#cfe0d6', '#D5E4DA'],
  ['#d0e8ff', '#DCE8E2'],
  ['#f0f4ff', 'var(--roam-soft)'],
  ['#f8faff', 'var(--card)'],
  ['#f0faf6', 'var(--roam-soft)'],
  // 沉浸浅底
  ['#f1f0ea', 'var(--wash)'],
  ['#f7f5ef', 'var(--wash)'],
  ['#f6f3ec', 'var(--wash)'],
  ['#fbf4ea', 'var(--sunset-soft)'],
  // 卡片圆角统一 24px
  ['border-radius: 27px', 'border-radius: 24px'],
  ['border-radius: 26px', 'border-radius: 24px'],
  ['border-radius: 22px', 'border-radius: 24px'],
  ['border-radius: 20px', 'border-radius: 24px'],
  // 主阴影柔化（Claude card style）
  ['box-shadow: 0 20px 55px rgba(22,78,66,.06)', 'box-shadow: var(--shadow-soft)'],
  ['box-shadow: 0 10px 20px rgba(22,78,66,.25)', 'box-shadow: 0 8px 18px rgba(22,78,66,.22)'],
  ['box-shadow: 0 10px 20px rgba(242,122,79,.25)', 'box-shadow: 0 8px 18px rgba(242,122,79,.28)']
]

const root = '/Users/epiphany/IdeaProjects/travel-agent-front'
for (const f of files) {
  const p = path.join(root, f)
  let s = fs.readFileSync(p, 'utf8')
  let changed = 0
  for (const [from, to] of replaces) {
    if (s.includes(from)) {
      s = s.split(from).join(to)
      changed++
    }
  }
  fs.writeFileSync(p, s)
  console.log(f, '→', changed, 'rules applied')
}
console.log('done')
