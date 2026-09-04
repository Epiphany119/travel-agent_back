<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchUsers, requestFriend } from '@/api/user'
const q=ref(''); const users=ref<any[]>([]); const loading=ref(false); const searched=ref(false)
const router = useRouter()
const route = useRoute()
function openUser(id: string) { router.push(`/users/${encodeURIComponent(id)}`) }
function imageUrl(value?: string) { const raw = String(value || '').trim(); if (!raw) return ''; if (/^(https?:|data:|blob:)/i.test(raw)) return raw; const base = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, ''); return `${base}${raw.startsWith('/') ? raw : `/${raw}`}` }
async function search(){
  const keyword = q.value.trim()
  if (!keyword) { users.value = []; searched.value = false; return }
  loading.value=true
  try { users.value=(await searchUsers(keyword)).data||[]; searched.value = true }
  catch { ElMessage.error('搜索失败，请稍后重试') }
  finally { loading.value=false }
}

// 顶部搜索框的“查看全部用户”会带着关键词进入这里，保持结果页与顶部搜索状态一致。
watch(() => route.query.q, (value) => {
  const next = String(value || '').trim()
  if (next === q.value) return
  q.value = next
  if (next) void search()
  else { users.value = []; searched.value = false }
}, { immediate: true })
async function add(u:any){
  const message=await ElMessageBox.prompt('告诉对方你为什么想认识他','申请加好友',{inputPlaceholder:'例如：也喜欢慢旅行和摄影',confirmButtonText:'发送申请'}).catch(()=>null)
  if(message) {
    try { await requestFriend(u.public_id,message.value); ElMessage.success('申请已发送') }
    catch { ElMessage.error('申请发送失败，请稍后重试') }
  }
}
</script>
<template>
  <main class="search-page">
    <p class="eyebrow">FIND YOUR PEOPLE</p><h1>寻找旅行同好</h1>
    <p class="sub">用 8 位 Roamly ID 或昵称搜索用户。公开笔记无需加好友也能阅读。</p>
    <div class="search-box"><input v-model="q" placeholder="输入 8 位 ID / 昵称" @keyup.enter="search"/><button v-if="q" class="clear" aria-label="清除搜索" @click="q=''; users=[]; searched=false">×</button><button @click="search">搜索</button></div>
    <section v-loading="loading" class="results">
      <article v-for="u in users" :key="u.public_id" class="user-card"><button class="avatar avatar-button" @click="openUser(u.public_id)"><img v-if="imageUrl(u.avatar)" :src="imageUrl(u.avatar)" alt=""/><span v-else>{{(u.nickname||u.public_id).charAt(0)}}</span></button><div @click="openUser(u.public_id)"><b>{{u.nickname||'Roamly 用户'}}</b><small>ID · {{u.public_id}}</small></div><button @click="add(u)">加好友</button></article>
      <p v-if="!loading&&!users.length" class="empty">{{ searched ? '没有找到匹配用户，换个关键词试试。' : '搜索一个用户，看看他走过的路。' }}</p>
    </section>
  </main>
</template>
<style scoped>
.avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
</style>
<style scoped>.search-page{width:min(760px,calc(100% - 40px));margin:auto;padding:54px 0}.eyebrow{color:var(--sunset);font-size:10px;font-weight:800;letter-spacing:.16em}h1{font:38px 'DM Serif Display';margin:8px 0}.sub{color:var(--ink-2);font-size:14px}.search-box{display:flex;margin:28px 0 20px;border:1px solid var(--line);border-radius:14px;background:var(--card);padding:6px;transition:border-color .16s,box-shadow .16s}.search-box:focus-within{border-color:var(--forest);box-shadow:0 0 0 3px color-mix(in srgb,var(--forest) 12%,transparent)}.search-box input{flex:1;border:0;outline:0;padding:12px;font-size:14px;background:transparent;color:var(--ink)}.search-box button,.user-card button{border:0;background:var(--forest);color:#fff;border-radius:9px;padding:0 18px;font-weight:800;cursor:pointer}.search-box .clear{background:transparent;color:var(--ink-3);font-size:18px;padding:0 10px}.results{display:grid;gap:10px}.user-card{display:flex;align-items:center;gap:12px;padding:15px;border:1px solid var(--line);border-radius:14px;background:var(--card);transition:transform .18s,box-shadow .18s}.user-card:hover{transform:translateY(-2px);box-shadow:var(--shadow-soft)}.user-card div{flex:1;cursor:pointer}.user-card b,.user-card small{display:block}.user-card small{color:var(--ink-3);font-size:11px;margin-top:3px}.avatar{display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:var(--roam-soft);color:var(--forest);font-weight:800}.avatar-button{flex:0 0 42px;padding:0;background:var(--roam-soft)!important;color:var(--forest)!important}.empty{color:var(--ink-3);text-align:center;padding:40px}</style>

<style scoped>
.search-page {
  position: relative;
  isolation: isolate;
  width: min(820px, calc(100% - 56px));
  padding: 62px 0 90px;
}

.search-page::before {
  content: '';
  position: absolute;
  z-index: -1;
  width: 420px;
  height: 420px;
  top: 0;
  right: -190px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--roam) 14%, transparent), transparent 70%);
  pointer-events: none;
}

.search-page h1 { letter-spacing: -.025em; }
.search-page .sub { line-height: 1.7; }
.search-box { margin-top: 30px; padding: 7px; border-radius: 17px; box-shadow: var(--shadow-soft), inset 0 1px 0 rgba(255,255,255,.72); }
.search-box button:not(.clear) { min-height: 42px; padding: 0 22px; border-radius: 11px; }
.results { gap: 12px; padding-top: 6px; }
.user-card { padding: 16px 18px; border-radius: 17px; border-color: color-mix(in srgb, var(--forest) 12%, var(--line)); box-shadow: 0 3px 0 color-mix(in srgb, var(--forest) 3%, transparent); }
.user-card:hover { box-shadow: 0 12px 24px color-mix(in srgb, var(--forest) 10%, transparent); }
.user-card > div b { font-size: 14px; }
.user-card > button:last-child { min-height: 32px; }
.empty { margin-top: 10px; padding: 54px 20px; border: 1px dashed color-mix(in srgb, var(--forest) 18%, var(--line)); border-radius: 18px; background: color-mix(in srgb, var(--card) 62%, transparent); }

@media (max-width: 700px) {
  .search-page { width: calc(100% - 28px); padding-top: 34px; }
}
</style>
