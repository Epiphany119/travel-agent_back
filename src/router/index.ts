import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Chat', component: () => import('@/views/ChatView.vue') },
    { path: '/profile', name: 'Profile', component: () => import('@/views/UserProfile.vue') },
    { path: '/inspirations', name: 'Inspirations', component: () => import('@/views/InspirationView.vue') },
    { path: '/journeys', name: 'Journeys', component: () => import('@/views/JourneyView.vue') },
    { path: '/journey-map', name: 'JourneyMap', component: () => import('@/views/JourneyMapView.vue') }
  ]
})

export default router
