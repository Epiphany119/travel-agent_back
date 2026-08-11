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
