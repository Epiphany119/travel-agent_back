<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publishSocialNote, USER_ID } from '@/api/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 三种模式：view(查看) / edit(编辑) / create(新建)
const mode = ref<'view' | 'edit' | 'create'>('view')

const title = ref('')
const content = ref('')
const coverUrl = ref('')
const author = ref('')
const city = ref('')
const tags = ref<string[]>([])
const tagsInput = ref('')
watch(tags, (val) => { tagsInput.value = val.join(', ') }, { immediate: true })

const publishing = ref(false)
const editing = ref(false)

// 支持 query: title / content / image / author / city / tags(逗号分隔)
onMounted(() => {
  title.value = (route.query.title as string) || ''
  content.value = (route.query.content as string) || ''
  coverUrl.value = (route.query.image as string) || ''
  author.value = (route.query.author as string) || ''
  city.value = (route.query.city as string) || ''
  const tagStr = (route.query.tags as string) || ''
  tags.value = tagStr ? tagStr.split(',').map(s => s.trim()).filter(Boolean) : []

  // 没数据就新建模式，有数据默认查看模式
  if (!title.value && !content.value) {
    mode.value = 'create'
  } else {
    mode.value = 'view'
  }
})

const modeLabel = computed(() => ({
  view: '查看灵感',
  edit: '编辑灵感',
  create: '发布新笔记'
}[mode.value]))

const heroFallback = computed(() => {
  // 封面没有时，用渐变 + 城市名做占位
  if (coverUrl.value) return coverUrl.value
  const palettes = [
    'linear-gradient(135deg,#2d6b57,#5ba391)',
    'linear-gradient(135deg,#c46d3f,#e8a97a)',
    'linear-gradient(135deg,#3a5f8a,#6f93c4)',
    'linear-gradient(135deg,#7a5d8a,#b39ac4)'
  ]
  const idx = (city.value?.length || 0) % palettes.length
  return palettes[idx]
})

const isEditing = computed(() => mode.value === 'edit' || mode.value === 'create')

function toggleEdit() {
  mode.value = mode.value === 'view' ? 'edit' : (mode.value === 'edit' ? 'view' : mode.value)
}

async function publish() {
  if (!title.value.trim()) return ElMessage.warning('请填写标题')
  if (!content.value.trim()) return ElMessage.warning('请填写正文内容')
  publishing.value = true
  try {
    await publishSocialNote({
      userId: USER_ID,
      title: title.value,
      content: content.value,
      coverUrl: coverUrl.value
    })
    ElMessage.success(mode.value === 'create' ? '已发布到旅行社区' : '已保存修改')
    router.push('/explore')
  } finally {
    publishing.value = false
  }
}

// 简易 markdown → 段落数组（支持 # 标题、- 列表、数字. 列表、普通段落、空行分段、**加粗**）
const blocks = computed(() => {
  const src = content.value || ''
  const lines = src.split('\n')
  const out: { type: string; text: string; level?: number }[] = []
  for (const raw of lines) {
    const line = raw.trimEnd()
    if (!line.trim()) continue
    const h = /^(#{1,3})\s+(.*)$/.exec(line)
    if (h) { out.push({ type: 'heading', level: h[1].length, text: h[2].trim() }); continue }
    const ul = /^[-*]\s+(.*)$/.exec(line)
    if (ul) { out.push({ type: 'bullet', text: ul[1] }); continue }
    const ol = /^\d+\.\s+(.*)$/.exec(line)
    if (ol) { out.push({ type: 'number', text: ol[1] }); continue }
    out.push({ type: 'paragraph', text: line })
  }
  return out
})

function renderInline(text: string) {
  // 把 **bold** 和 *italic* 转成 HTML
  return text
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}
</script>

<template>
  <main class="publish-page">
    <!-- Hero 封面 -->
    <header class="hero" :style="{
      background: coverUrl ? `url(${coverUrl}) center/cover no-repeat` : heroFallback
    }">
      <div class="hero-mask"></div>
      <div class="hero-inner">
        <p class="eyebrow">{{ modeLabel }}</p>
        <h1 v-if="!isEditing">{{ title }}</h1>
        <input v-else v-model="title" placeholder="标题，例如：杭州 3 日慢旅行" class="hero-title-input" />
        <div class="meta" v-if="author || city">
          <span v-if="author" class="meta-item">✍️ {{ author }}</span>
          <span v-if="city" class="meta-item">📍 {{ city }}</span>
          <span v-for="t in tags" :key="t" class="meta-item tag">#{{ t }}</span>
        </div>
      </div>
    </header>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="mode-switch">
        <button
          :class="{ active: mode === 'view' }"
          @click="mode = 'view'"
          :disabled="mode === 'create'"
        >👁 查看</button>
        <button
          :class="{ active: isEditing }"
          @click="mode = mode === 'view' ? 'edit' : 'create'"
        >✏️ 编辑</button>
      </div>
      <div class="actions" v-if="mode === 'view'">
        <button class="btn-ghost" @click="router.back()">← 返回</button>
      </div>
      <div class="actions" v-else>
        <button class="btn-ghost" @click="mode = 'view'; title = (route.query.title as string) || ''; content = (route.query.content as string) || ''">← 取消</button>
        <button class="btn-primary" :disabled="publishing" @click="publish">
          {{ publishing ? '保存中…' : mode === 'create' ? '发布笔记' : '保存修改' }}
        </button>
      </div>
    </div>

    <!-- 内容区 -->
    <section class="content">
      <!-- 查看模式：渲染排版 -->
      <article v-if="mode === 'view'" class="note-body">
        <div v-if="coverUrl" class="cover-preview">
          <img :src="coverUrl" alt="封面" />
        </div>

        <template v-for="(b, i) in blocks" :key="i">
          <h1 v-if="b.type === 'heading' && b.level === 1" class="h1" v-html="renderInline(b.text)"></h1>
          <h2 v-else-if="b.type === 'heading' && b.level === 2" class="h2" v-html="renderInline(b.text)"></h2>
          <h3 v-else-if="b.type === 'heading' && b.level === 3" class="h3" v-html="renderInline(b.text)"></h3>
          <p v-else-if="b.type === 'paragraph'" class="p" v-html="renderInline(b.text)"></p>
          <ul v-else-if="b.type === 'bullet'" class="list">
            <li v-html="renderInline(b.text)"></li>
          </ul>
          <ol v-else-if="b.type === 'number'" class="list list-numbered">
            <li v-html="renderInline(b.text)"></li>
          </ol>
        </template>

        <div v-if="blocks.length === 0" class="empty-hint">还没有内容，切到编辑模式开始填写吧。</div>
      </article>

      <!-- 编辑模式：结构化表单 -->
      <form v-else class="note-form" @submit.prevent="publish">
        <div class="form-block">
          <label>封面图片 URL</label>
          <input v-model="coverUrl" placeholder="https://…（可选）" />
          <div v-if="coverUrl" class="form-preview">
            <img :src="coverUrl" alt="预览" />
          </div>
        </div>

        <div class="form-block">
          <label>标题</label>
          <input v-model="title" placeholder="为这段旅行起个标题" />
        </div>

        <div class="form-block">
          <label>作者 / 城市</label>
          <div class="two-col">
            <input v-model="author" placeholder="作者" />
            <input v-model="city" placeholder="城市 / 目的地" />
          </div>
        </div>

        <div class="form-block">
          <label>标签（用逗号分隔）</label>
          <input
            v-model="tagsInput"
            placeholder="如：轻松漫游, 人文, 海边"
            @input="tags = tagsInput.split(',').map(s => s.trim()).filter(Boolean)"
          />
          <div class="tag-chips" v-if="tags.length">
            <span v-for="t in tags" :key="t" class="chip">#{{ t }}</span>
          </div>
        </div>

        <div class="form-block">
          <label>正文内容（支持 # 标题、- 列表、数字. 步骤、**加粗**）</label>
          <textarea
            v-model="content"
            rows="14"
            placeholder="Day 1：抵达后入住西湖边的民宿，清晨错峰逛苏堤。&#10;&#10;Day 2：灵隐寺 + 龙井村徒步。&#10;&#10;Day 3：九溪烟树慢走，再喝一杯桂花龙井收尾。"
          ></textarea>
          <div class="preview-note" v-if="content">
            <span class="preview-label">实时预览：</span>
            <div class="preview-render" v-html="renderInline(content)"></div>
          </div>
        </div>

        <button type="submit" class="btn-primary submit-btn" :disabled="publishing">
          {{ publishing ? '保存中…' : mode === 'create' ? '发布笔记' : '保存修改' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.publish-page { min-height: 100vh; padding-bottom: 80px; }

/* Hero */
.hero {
  position: relative;
  height: 340px;
  display: flex;
  align-items: flex-end;
  color: #fff;
  overflow: hidden;
}
.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,.65), rgba(0,0,0,.25) 40%, transparent);
}
.hero-inner {
  position: relative;
  z-index: 2;
  width: min(920px, calc(100% - 60px));
  margin: 0 auto;
  padding-bottom: 32px;
}
.eyebrow { color: var(--sunset, #f27a4f); font-size: 10px; font-weight: 800; letter-spacing: .16em; margin-bottom: 10px; }
.hero h1 { font: 44px 'DM Serif Display', 'Noto Sans SC'; margin: 6px 0 14px; line-height: 1.15; text-shadow: 0 2px 10px rgba(0,0,0,.4); }
.hero-title-input {
  width: 100%;
  background: rgba(255,255,255,.95);
  border: 0;
  border-radius: 12px;
  padding: 14px 18px;
  font: 26px 'DM Serif Display', 'Noto Sans SC';
  color: var(--ink);
  outline: none;
  box-shadow: 0 4px 20px rgba(0,0,0,.15);
}
.meta { display: flex; flex-wrap: wrap; gap: 10px; }
.meta-item {
  background: rgba(255,255,255,.22);
  backdrop-filter: blur(6px);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

/* Toolbar */
.toolbar {
  position: sticky;
  top: 0;
  z-index: 5;
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line, #e6e2dd);
  padding: 14px 0;
  margin-bottom: 36px;
}
.toolbar { display: flex; justify-content: flex-start; align-items: center; gap: 12px; width: min(920px, calc(100% - 60px)); margin: 0 auto; }
.mode-switch { display: flex; gap: 6px; background: #f3f5f4; padding: 4px; border-radius: 10px; }
.mode-switch button {
  border: 0;
  background: transparent;
  padding: 7px 16px;
  border-radius: 7px;
  font-weight: 700;
  cursor: pointer;
  color: #687873;
  transition: all .18s;
}
.mode-switch button.active { background: var(--forest); color: #fff; box-shadow: 0 2px 8px rgba(22,78,66,.25); }
.mode-switch button:disabled { opacity: .5; cursor: not-allowed; }

.actions { display: flex; gap: 10px; }
.btn-ghost {
  border: 1px solid var(--line);
  background: #fff;
  padding: 9px 18px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  color: var(--ink);
  transition: all .18s;
}
.btn-ghost:hover { background: #f3f5f4; }
.btn-primary {
  border: 0;
  background: var(--forest);
  color: #fff;
  padding: 10px 22px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(22,78,66,.25);
  transition: all .18s;
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(22,78,66,.32); }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }

/* Content */
.content { width: min(920px, calc(100% - 60px)); margin: 0 auto; }
.note-body { background: var(--card, #fff); border: 1px solid var(--line); border-radius: 20px; padding: 36px 44px; }

.cover-preview { margin-bottom: 28px; border-radius: 14px; overflow: hidden; max-height: 420px; }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }

.note-body h1 { font: 30px 'DM Serif Display', 'Noto Sans SC'; margin: 28px 0 16px; color: var(--ink); }
.note-body h2 { font: 24px 'DM Serif Display', 'Noto Sans SC'; margin: 24px 0 14px; color: var(--ink); }
.note-body h3 { font: 19px 'DM Serif Display', 'Noto Sans SC'; margin: 20px 0 10px; color: var(--ink); }
.note-body .p { font-size: 16px; line-height: 1.85; color: #2f3a35; margin: 0 0 18px; }
.note-body .list { margin: 0 0 18px; padding-left: 24px; font-size: 15px; line-height: 1.9; color: #2f3a35; }
.note-body .list-numbered { list-style: decimal; }
.note-body strong { color: var(--forest); }
.empty-hint { text-align: center; color: #8a9792; padding: 40px 0; }

/* Form */
.note-form { background: var(--card, #fff); border: 1px solid var(--line); border-radius: 20px; padding: 36px 44px; }
.form-block { margin-bottom: 22px; }
.form-block label { display: block; font-weight: 700; font-size: 13px; color: var(--ink); margin-bottom: 8px; }
.form-block input, .form-block textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 11px 14px;
  font: 14px inherit;
  color: var(--ink);
  background: #fff;
  outline: none;
  transition: border-color .18s;
}
.form-block input:focus, .form-block textarea:focus { border-color: var(--forest); }
.form-block textarea { resize: vertical; line-height: 1.7; font-family: inherit; }
.two-col { display: flex; gap: 12px; }
.two-col input { flex: 1; }
.form-preview img { width: 100%; max-height: 200px; object-fit: cover; border-radius: 10px; margin-top: 10px; border: 1px solid var(--line); }
.tag-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.chip { background: var(--roam-soft, #e6f0ec); color: var(--forest); padding: 3px 10px; border-radius: 16px; font-size: 12px; font-weight: 600; }
.preview-note { margin-top: 14px; padding: 14px; background: #f7f9f8; border-radius: 10px; border: 1px dashed var(--line); }
.preview-label { font-size: 12px; color: #687873; font-weight: 700; display: block; margin-bottom: 8px; }
.preview-render { font-size: 13px; line-height: 1.8; color: #2f3a35; white-space: pre-wrap; }
.submit-btn { width: 100%; margin-top: 8px; padding: 14px; font-size: 15px; }

@media (max-width: 700px) {
  .hero { height: 260px; }
  .hero h1 { font-size: 30px; }
  .toolbar { flex-wrap: wrap; gap: 12px; }
  .note-body, .note-form { padding: 22px 20px; border-radius: 14px; }
  .two-col { flex-direction: column; }
}
</style>
