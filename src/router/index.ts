import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/auth', name: 'Auth', component: () => import('@/views/AuthView.vue') },
    { path: '/explore', name: 'Explore', component: () => import('@/views/ExploreView.vue') },
    { path: '/chat', name: 'Chat', component: () => import('@/views/ChatView.vue') },
    { path: '/users/search', name: 'UserSearch', component: () => import('@/views/UserSearchView.vue') },
    { path: '/users/:id', name: 'PublicUser', component: () => import('@/views/PublicUserView.vue') },
    { path: '/notes/:id', name: 'NoteDetail', component: () => import('@/views/NoteDetailView.vue') },
    { path: '/publish', name: 'PublishNote', component: () => import('@/views/PublishNoteView.vue') },
    { path: '/', redirect: '/notes' },
    { path: '/profile', name: 'Profile', component: () => import('@/views/UserProfile.vue') },
    { path: '/inspirations', name: 'Inspirations', component: () => import('@/views/InspirationView.vue') },
    { path: '/notes', name: 'Notes', component: () => import('@/views/NotesView.vue') },
    { path: '/journeys', name: 'Journeys', component: () => import('@/views/JourneyView.vue') },
    { path: '/journey-map', name: 'JourneyMap', component: () => import('@/views/JourneyMapView.vue') },
    { path: '/card-detail', name: 'CardDetail', component: () => import('@/views/CardDetailView.vue') },
    { path: '/agent-panel', name: 'AgentPanel', component: () => import('@/views/AgentPanel.vue') }
  ]
})

/**
 * 全局路由守卫
 * - 已登录用户访问 /auth → 跳转首页
 * - 未登录用户访问受保护页面 → 跳转 /auth 并记录来源，登录后回跳
 */
router.beforeEach((to) => {
  const token = localStorage.getItem('roamly_token')

  // 已登录，访问认证页面 → 跳转首页
  if (to.path === '/auth' && token) {
    return '/'
  }

  // 未登录，访问非认证页面 → 跳转登录并记录 redirect
  if (to.path !== '/auth' && !token) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
