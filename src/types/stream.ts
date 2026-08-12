// 独立定义 DayPlan（不同于 agent.ts 中 TravelPlan.dayPlans 里的子类型）
export interface DayPlanSlot {
  plan: string
  duration: string
  tips?: string
  budget: number
}

export interface DayPlan {
  dayNumber: number
  theme: string
  date: string
  morning?: DayPlanSlot
  afternoon?: DayPlanSlot
  evening?: DayPlanSlot
  tips?: string
}

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
  data: DayPlan
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
