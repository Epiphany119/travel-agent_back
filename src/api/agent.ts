import request from './index'

export interface CreateSessionRequest {
  destination: string
  startDate?: string
  endDate?: string
  budgetLevel?: string
  travelType?: string
  travelers?: number
}

export interface SendMessageRequest {
  sessionId: string
  content: string
}

export interface SessionResponse {
  sessionId: string
  title: string
  status: string
  createdAt: string
}

export interface MessageResponse {
  messageId: string
  role: string
  content: string
  needsToolCall?: boolean
}

export interface TravelPlanRequest {
  destination: string
  days: number
  budget: number
  travelers?: number
  travelStyle?: string
  interests?: string[]
}

export interface TravelPlan {
  planId: string; destination: string; days: number; totalBudget: number; estimatedCost: number
  budgetStatus: string; overview: string; travelTips: string[]; packingList: string[]
  dayPlans: Array<{ dayNumber: number; date: string; theme: string; dayBudget: number; transportation: string; notes: string
    attractions: Array<{ name: string; description: string; duration: number; ticketPrice: number }>
    meals: Array<{ mealType: string; restaurantName: string; cuisine: string; avgPrice: number; reason: string }>
  }>
}

export function createSession(data: CreateSessionRequest) {
  return request.post<any, { code: number; data: SessionResponse }>('/agent/sessions', data)
}

export function sendMessage(data: SendMessageRequest) {
  return request.post<any, { code: number; data: MessageResponse }>('/agent/messages', data)
}

export function getMessages(sessionId: string) {
  return request.get<any, { code: number; data: MessageResponse[] }>(`/agent/sessions/${sessionId}/messages`)
}

export function getUserSessions(userId: number = 1) {
  return request.get<any, { code: number; data: SessionResponse[] }>('/agent/sessions', { params: { userId } })
}

export function deleteSession(sessionId: string) {
  return request.delete(`/agent/sessions/${sessionId}`)
}

export function generateTravelPlan(data: TravelPlanRequest) {
  return request.post<any, { code: number; data: TravelPlan }>('/travel-plans/generate', data)
}

// ─── SSE 流式订阅 ────────────────────────────────────────────────────────────

export interface StreamTokenEvent {
  name: 'token'
  data: { delta: string; full?: string }
}

export interface StreamToolCallEvent {
  name: 'tool_call'
  data: { name: string; args: Record<string, unknown> }
}

export interface StreamToolResultEvent {
  name: 'tool_result'
  data: { name: string; ok: boolean; summary?: string; error?: string }
}

export interface StreamDayPlanEvent {
  name: 'dayplan'
  data: {
    dayNumber: number
    theme: string
    date: string
    morning?: { plan: string; duration: string; tips?: string; budget: number }
    afternoon?: { plan: string; duration: string; tips?: string; budget: number }
    evening?: { plan: string; duration: string; tips?: string; budget: number }
    tips?: string
  }
}

export interface StreamDoneEvent {
  name: 'done'
  data: { planId: string }
}

export interface StreamErrorEvent {
  name: 'error'
  data: { code: string; message: string }
}

export type StreamEvent =
  | StreamTokenEvent
  | StreamToolCallEvent
  | StreamToolResultEvent
  | StreamDayPlanEvent
  | StreamDoneEvent
  | StreamErrorEvent

/**
 * SSE 流式订阅行程规划
 * 使用 fetch + ReadableStream 解析 SSE
 * 返回取消函数，调用后终止请求
 */
export function subscribePlanStream(
  planId: string,
  onEvent: (event: StreamEvent) => void
): () => void {
  const controller = new AbortController()

  fetch(`/api/travel-plans/${planId}/stream`, {
    method: 'GET',
    headers: { Accept: 'text/event-stream' },
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok || !response.body) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEventName = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEventName = line.slice(7).trim()
          } else if (line.startsWith('data: ')) {
            const rawData = line.slice(6).trim()
            if (!rawData) continue
            try {
              const parsed = JSON.parse(rawData)
              const eventName = currentEventName || parsed.type || 'unknown'
              currentEventName = ''
              onEvent({ name: eventName as StreamEvent['name'], data: parsed } as StreamEvent)
            } catch {
              // ignore parse error
            }
          } else if (line === '') {
            currentEventName = ''
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onEvent({ name: 'error', data: { code: 'FETCH_ERROR', message: err.message } } as StreamErrorEvent)
      }
    })

  return () => controller.abort()
}
