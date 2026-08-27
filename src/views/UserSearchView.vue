<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchUsers, requestFriend } from '@/api/user'
const q=ref(''); const users=ref<any[]>([]); const loading=ref(false); const searched=ref(false)
async function search(){
  const keyword = q.value.trim()
  if (!keyword) { users.value = []; searched.value = false; return }
  loading.value=true
  try { users.value=(await searchUsers(keyword)).data||[]; searched.value = true }
  catch { ElMessage.error('搜索失败，请稍后重试') }
  finally { loading.value=false }
}
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
      <article v-for="u in users" :key="u.public_id" class="user-card"><span class="avatar">{{(u.nickname||u.public_id).charAt(0)}}</span><div><b>{{u.nickname||'Roamly 用户'}}</b><small>ID · {{u.public_id}}</small></div><button @click="add(u)">加好友</button></article>
      <p v-if="!loading&&!users.length" class="empty">{{ searched ? '没有找到匹配用户，换个关键词试试。' : '搜索一个用户，看看他走过的路。' }}</p>
    </section>
  </main>
</template>
<style scoped>.search-page{width:min(760px,calc(100% - 40px));margin:auto;padding:54px 0}.eyebrow{color:var(--sunset);font-size:10px;font-weight:800;letter-spacing:.16em}h1{font:38px 'DM Serif Display';margin:8px 0}.sub{color:var(--ink-2);font-size:14px}.search-box{display:flex;margin:28px 0 20px;border:1px solid var(--line);border-radius:14px;background:var(--card);padding:6px;transition:border-color .16s,box-shadow .16s}.search-box:focus-within{border-color:var(--forest);box-shadow:0 0 0 3px color-mix(in srgb,var(--forest) 12%,transparent)}.search-box input{flex:1;border:0;outline:0;padding:12px;font-size:14px;background:transparent;color:var(--ink)}.search-box button,.user-card button{border:0;background:var(--forest);color:#fff;border-radius:9px;padding:0 18px;font-weight:800;cursor:pointer}.search-box .clear{background:transparent;color:var(--ink-3);font-size:18px;padding:0 10px}.results{display:grid;gap:10px}.user-card{display:flex;align-items:center;gap:12px;padding:15px;border:1px solid var(--line);border-radius:14px;background:var(--card);transition:transform .18s,box-shadow .18s}.user-card:hover{transform:translateY(-2px);box-shadow:var(--shadow-soft)}.user-card div{flex:1}.user-card b,.user-card small{display:block}.user-card small{color:var(--ink-3);font-size:11px;margin-top:3px}.avatar{display:grid;place-items:center;width:42px;height:42px;border-radius:50%;background:var(--roam-soft);color:var(--forest);font-weight:800}.empty{color:var(--ink-3);text-align:center;padding:40px}</style>
