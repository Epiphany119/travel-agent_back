import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StreamEvent } from '@/types/stream'
import type { DayPlan } from '@/types/stream'

export const useStreamStore = defineStore('stream', () => {
  const isStreaming = ref(false)
  const fullText = ref('')
  const toolCalls = ref<{ name: string; args: Record<string, unknown> }[]>([])
  const currentToolResult = ref<string | null>(null)
  const dayPlans = ref<DayPlan[]>([])
  const error = ref<string | null>(null)
  const planId = ref<string | null>(null)

  function reset() {
    isStreaming.value = false
    fullText.value = ''
    toolCalls.value = []
    currentToolResult.value = null
    dayPlans.value = []
    error.value = null
    planId.value = null
  }

  function handleEvent(event: StreamEvent) {
    switch (event.name) {
      case 'token':
        fullText.value += event.data.delta
        break
      case 'tool_call':
        toolCalls.value.push(event.data)
        currentToolResult.value = null
        break
      case 'tool_result':
        currentToolResult.value = event.data.summary || event.data.error || null
        break
      case 'dayplan':
        dayPlans.value.push(event.data)
        break
      case 'done':
        planId.value = event.data.planId
        isStreaming.value = false
        break
      case 'error':
        error.value = event.data.message
        isStreaming.value = false
        break
    }
  }

  return { isStreaming, fullText, toolCalls, currentToolResult, dayPlans, error, planId, reset, handleEvent }
})
