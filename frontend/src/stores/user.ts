import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAvatar, getNickname, updateNickname } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const avatar = ref<string | null>(null)
  const nickname = ref<string>('旅人')

  async function fetchAvatar() {
    try {
      const res = await getAvatar()
      const url = res?.data?.avatar
      if (url) {
        avatar.value = url.startsWith('http')
          ? url
          : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
      }
    } catch (e) {
      console.warn('获取头像失败:', e)
    }
  }

  async function fetchProfile() {
    // 分别请求，单个失败不影响另一个
    try {
      const nicknameRes = await getNickname()
      const name = nicknameRes?.data?.nickname
      if (name && name.trim()) {
        nickname.value = name
        console.log('[userStore] 昵称已加载:', name)
      } else {
        console.warn('[userStore] 昵称接口返回空值:', nicknameRes)
      }
    } catch (e) {
      console.warn('[userStore] 获取昵称失败:', e)
    }

    try {
      const avatarRes = await getAvatar()
      const url = avatarRes?.data?.avatar
      if (url) {
        avatar.value = url.startsWith('http')
          ? url
          : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
      }
    } catch (e) {
      console.warn('[userStore] 获取头像失败:', e)
    }
  }

  function setAvatar(url: string) {
    avatar.value = url
  }

  function setNickname(name: string) {
    nickname.value = name
  }

  async function syncNickname(name: string) {
    setNickname(name)
    try {
      await updateNickname(name)
    } catch (e) {
      console.warn('保存昵称失败（非阻塞）：', e)
    }
  }

  function clearAvatar() {
    avatar.value = null
  }

  fetchProfile()

  return { avatar, nickname, fetchAvatar, fetchProfile, setAvatar, setNickname, syncNickname, clearAvatar }
})
