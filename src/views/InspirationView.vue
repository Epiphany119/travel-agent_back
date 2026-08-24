<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInspirations, addInspiration, updateInspiration, deleteInspiration, uploadImage, type Inspiration } from '@/api/user'

const router = useRouter()

function openView(item: Inspiration) {
  // 跳转到 PublishNoteView 查看模式，预填灵感数据
  const content = [
    item.quote ? `“${item.quote}”` : '',
    item.description ? `\n\n${item.description}` : '',
    item.estimatedBudget ? `\n\n预算：¥${item.estimatedBudget.toLocaleString()}` : '',
    item.bestSeason ? `\n最佳季节：${item.bestSeason}` : '',
  ].filter(Boolean).join('')
  router.push({
    path: '/publish',
    query: {
      title: item.name,
      content: content || `想去 ${item.name} 看看，${item.quote || item.description || '把这里加入下一段旅行的计划。'}`,
      image: item.imageUrl || '',
      tags: item.tags || '',
      city: '',
    }
  })
}

const loading = ref(false)
const list = ref<Inspiration[]>([])

const dialogVisible = ref(false)
const editing = ref<Inspiration | null>(null)
const form = ref<Inspiration>({ name: '' })

// 图片上传相关状态
const imageInputMode = ref<'url' | 'upload'>('url')
const imageUploading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listInspirations()
    list.value = res.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载灵感目的地失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editing.value = null
  form.value = { name: '', priority: 3, status: 1, sortOrder: list.value.length + 1 }
  imageInputMode.value = 'url'
  imageUploading.value = false
  dialogVisible.value = true
}

function openEdit(item: Inspiration) {
  editing.value = item
  form.value = { ...item }
  // 如果已有本地图片路径，切换到 URL 模式显示
  imageInputMode.value = item.imageUrl?.startsWith('/uploads') ? 'url' : 'url'
  imageUploading.value = false
  dialogVisible.value = true
}

async function handleImageFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  imageUploading.value = true
  try {
    const res = await uploadImage(file, 'inspiration')
    form.value.imageUrl = res.data.url
    ElMessage.success('图片上传成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  } finally {
    imageUploading.value = false
    input.value = ''
  }
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('请填写目的地名称')
    return
  }
  try {
    if (editing.value?.id) {
      await updateInspiration(editing.value.id, form.value)
      ElMessage.success('已更新')
    } else {
      await addInspiration(form.value)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  }
}

async function remove(item: Inspiration) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.name}」吗？`, '删除灵感目的地', { type: 'warning' })
    if (item.id) await deleteInspiration(item.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const seasonOptions = ['不限', '春季', '夏季', '秋季', '冬季']
const statusText = (s?: number) => (s === 0 ? '待规划' : '已完成')
const budgetText = (b?: number) => (b ? `¥${b.toLocaleString()}` : '—')

// 无图片时生成柔和的渐变占位背景，避免一片死灰
const palettes = [
  'linear-gradient(135deg, var(--forest), var(--roam))',
  'linear-gradient(135deg,#3a5f8a,#6f93c4)',
  'linear-gradient(135deg,#b96a3d,#d89a6a)',
  'linear-gradient(135deg,#7a5d8a,#b39ac4)'
]
function thumbStyle(item: Inspiration, i: number) {
  if (item.imageUrl) return { backgroundImage: `url(${item.imageUrl})` }
  return { backgroundImage: palettes[i % palettes.length] }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <section class="head">
      <p class="eyebrow">WISHLIST</p>
      <h1>灵感目的地</h1>
      <p class="sub">收藏你向往的远方，为下一次出发攒下灵感。</p>
      <button class="add-btn" @click="openAdd">＋ 添加目的地</button>
    </section>

    <section class="grid" v-loading="loading">
      <article v-for="(item, i) in list" :key="item.id" class="card clickable" @click="openView(item)">
        <div class="thumb" :style="thumbStyle(item, i)" @click.stop>
          <span class="status" :class="item.status === 0 ? 'pending' : 'done'">
            <i class="dot"></i>{{ statusText(item.status) }}
          </span>
        </div>
        <div class="body">
          <div class="topline">
            <h3>{{ item.name }}</h3>
            <span v-if="item.priority" class="priority">★ {{ item.priority }}</span>
          </div>
          <p class="quote" v-if="item.quote">“{{ item.quote }}”</p>
          <p class="desc" v-if="item.description">{{ item.description }}</p>
          <div class="meta">
            <span v-if="item.bestSeason">🍂 {{ item.bestSeason }}</span>
            <span v-if="item.estimatedBudget">{{ budgetText(item.estimatedBudget) }}</span>
          </div>
          <div class="tags" v-if="item.tags">
            <span v-for="t in item.tags.split(',')" :key="t" class="chip">{{ t }}</span>
          </div>
          <div class="actions">
            <button class="ghost" @click.stop="openView(item)">查看详情</button>
            <button class="ghost" @click.stop="openEdit(item)">编辑</button>
            <button class="ghost danger" @click.stop="remove(item)">删除</button>
          </div>
        </div>
      </article>
      <div v-if="!loading && list.length === 0" class="empty">
        还没有灵感目的地，点击上方「添加目的地」开始吧。
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing?.id ? '编辑目的地' : '添加目的地'" width="520px">
      <el-form label-position="top">
        <el-form-item label="名称 *">
          <el-input v-model="form.name" placeholder="如：冰岛极光之旅" />
        </el-form-item>
        <el-form-item label="一句话文案">
          <el-input v-model="form.quote" placeholder="如：在世界尽头，等一场极光" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="为什么想去这里…" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="image-input-row">
            <el-radio-group v-model="imageInputMode" size="small" style="margin-bottom:6px">
              <el-radio-button value="url">粘贴链接</el-radio-button>
              <el-radio-button value="upload">上传图片</el-radio-button>
            </el-radio-group>
            <!-- URL 模式 -->
            <el-input
              v-if="imageInputMode === 'url'"
              v-model="form.imageUrl"
              placeholder="https://…（可选）"
              clearable
            />
            <!-- 上传模式 -->
            <div v-else class="upload-row">
              <input
                ref="imageFileRef"
                type="file"
                accept="image/*"
                style="display:none"
                @change="handleImageFile"
              />
              <el-button
                size="small"
                :loading="imageUploading"
                @click="($refs.imageFileRef as HTMLInputElement).click()"
              >
                选择图片
              </el-button>
              <span v-if="form.imageUrl" class="upload-preview">{{ form.imageUrl }}</span>
            </div>
          </div>
          <!-- 图片预览 -->
          <div v-if="form.imageUrl" class="image-preview">
            <img :src="form.imageUrl" alt="预览" />
          </div>
        </el-form-item>
        <div class="row">
          <el-form-item label="标签（逗号分隔）">
            <el-input v-model="form.tags" placeholder="摄影,美食,自驾" />
          </el-form-item>
          <el-form-item label="优先级">
            <el-input-number v-model="form.priority" :min="1" :max="5" />
          </el-form-item>
        </div>
        <div class="row">
          <el-form-item label="预估预算">
            <el-input-number v-model="form.estimatedBudget" :min="0" :step="1000" />
          </el-form-item>
          <el-form-item label="最佳季节">
            <el-select v-model="form.bestSeason" placeholder="选择季节">
              <el-option v-for="s in seasonOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped lang="scss">
.page { min-height: 100vh; padding-bottom: 80px; }
.head { width: min(1160px, calc(100% - 40px)); margin: 24px auto 30px; }
.eyebrow { color: var(--sunset); font-size: 10px; font-weight: 800; letter-spacing: 0.18em; margin: 0 0 8px; }
.head h1 { font: 34px "DM Serif Display", "Noto Sans SC"; color: var(--ink); margin: 0; }
.sub { color: #687873; font-size: 14px; margin: 10px 0 20px; }
.add-btn {
  border: 0; background: var(--forest); color: #fff; font-weight: 800;
  padding: 12px 22px; border-radius: 12px; cursor: pointer; box-shadow: 0 10px 20px rgba(22,78,66,.18);
}

.grid {
  width: min(1160px, calc(100% - 40px)); margin: auto;
  display: grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap: 22px;
}
.card { background: var(--card); border: 1px solid var(--line); border-radius: 24px; overflow: hidden; box-shadow: 0 1px 2px rgba(22,78,66,.04); transition: transform .25s, box-shadow .25s; }
.card.clickable { cursor: pointer; }
.card:hover { transform: translateY(-3px); box-shadow: 0 16px 34px rgba(22,78,66,.10); }
.thumb { height: 150px; position: relative; background-size: cover; background-position: center; }
.thumb::after { content: ''; position: absolute; inset: 0; background: linear-gradient(to bottom, rgba(0,0,0,.18), transparent 45%); }
.status { position: absolute; top: 12px; left: 12px; z-index: 1; display: inline-flex; align-items: center; gap: 6px; background: rgba(255,255,255,.92); color: var(--ink); font-size: 11px; font-weight: 700; padding: 4px 11px; border-radius: 24px; box-shadow: 0 2px 8px rgba(0,0,0,.10); backdrop-filter: blur(4px); }
.status .dot { width: 7px; height: 7px; border-radius: 50%; }
.status.done .dot { background: var(--forest); box-shadow: 0 0 0 3px rgba(22,78,66,.18); }
.status.pending .dot { background: var(--sunset); box-shadow: 0 0 0 3px rgba(242,122,79,.18); }
.body { padding: 16px 18px 18px; }
.topline { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 6px; }
.topline h3 { margin: 0; color: var(--ink); font-size: 18px; }
.priority { color: var(--sunset); font-weight: 800; font-size: 12px; flex-shrink: 0; }
.quote { color: var(--sunset); font-style: italic; font-size: 13px; margin: 0 0 8px; }
.desc { color: #687873; font-size: 13px; line-height: 1.6; margin: 0 0 10px; }
.meta { display: flex; gap: 14px; color: #687873; font-size: 12px; margin-bottom: 8px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.chip { background: var(--roam-soft); color: var(--forest); font-size: 11px; padding: 3px 9px; border-radius: 24px; }
.actions { display: flex; gap: 10px; }
.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink); font-weight: 700; font-size: 12px; padding: 7px 14px; border-radius: 9px; cursor: pointer; }
.ghost.danger { color: #c0392b; border-color: #EAC7BC; }
.empty { grid-column: 1 / -1; text-align: center; color: #8a9792; padding: 60px 0; font-size: 14px; }
.row { display: flex; gap: 14px; }
.row .el-form-item { flex: 1; }
.image-input-row { width: 100%; }
.upload-row { display: flex; align-items: center; gap: 8px; }
.upload-preview { font-size: 12px; color: #687873; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 260px; }
.image-preview { margin-top: 8px; }
.image-preview img { width: 100%; max-height: 180px; object-fit: cover; border-radius: 8px; border: 1px solid var(--line); }
</style>
