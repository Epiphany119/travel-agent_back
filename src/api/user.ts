import request from './index'

// 统一响应结构（axios 拦截器已返回 response.data，即该结构）
export interface ApiResult<T> {
  code: number
  message?: string
  data: T
}

// ─── 灵感目的地 ──────────────────────────────────────────────────────
export interface Inspiration {
  id?: number
  userId?: string
  name: string
  imageUrl?: string
  quote?: string
  description?: string
  tags?: string
  priority?: number
  estimatedBudget?: number
  bestSeason?: string
  status?: number
  sortOrder?: number
  createdAt?: string
}

export const USER_ID = 'user_001'

export function listInspirations() {
  return request.get<unknown, ApiResult<Inspiration[]>>('/user/inspirations', { params: { userId: USER_ID } })
}
export function addInspiration(data: Inspiration) {
  return request.post<unknown, ApiResult<Inspiration>>('/user/inspirations', data)
}
export function updateInspiration(id: number, data: Inspiration) {
  return request.put<unknown, ApiResult<unknown>>(`/user/inspirations/${id}`, data)
}
export function deleteInspiration(id: number) {
  return request.delete<unknown, ApiResult<unknown>>(`/user/inspirations/${id}`)
}

// ─── 我的旅程 ────────────────────────────────────────────────────────
export interface JourneyPoint {
  id?: number
  journeyId?: number
  name: string
  latitude?: number | string
  longitude?: number | string
  visitDate?: string
  description?: string
  sortOrder?: number
}
export interface JourneyImage {
  id?: number
  journeyId?: number
  imageUrl: string
  caption?: string
  sortOrder?: number
}
export interface Journey {
  id?: number
  userId?: string
  destination: string
  departureCity?: string
  startDate?: string
  endDate?: string
  totalDays?: number
  summary?: string
  totalCost?: number
  rating?: number
  travelType?: string
  companions?: string
  weatherInfo?: string
  highlight?: string
  tips?: string
  status?: number
  createdAt?: string
}
export interface JourneyDetail {
  journey: Journey
  points: JourneyPoint[]
  images: JourneyImage[]
}

export function listJourneys() {
  return request.get<unknown, ApiResult<JourneyDetail[]>>('/user/journeys', { params: { userId: USER_ID } })
}
export function getJourney(id: number) {
  return request.get<unknown, ApiResult<JourneyDetail>>(`/user/journeys/${id}`)
}
export function addJourney(data: Journey) {
  return request.post<unknown, ApiResult<Journey>>('/user/journeys', data)
}
export function updateJourney(id: number, data: Journey) {
  return request.put<unknown, ApiResult<unknown>>(`/user/journeys/${id}`, data)
}
export function deleteJourney(id: number) {
  return request.delete<unknown, ApiResult<unknown>>(`/user/journeys/${id}`)
}
export function saveJourneyPoints(journeyId: number, points: JourneyPoint[]) {
  return request.post<unknown, ApiResult<unknown>>(`/user/journeys/${journeyId}/points`, points)
}
export function saveJourneyImages(journeyId: number, images: JourneyImage[]) {
  return request.post<unknown, ApiResult<unknown>>(`/user/journeys/${journeyId}/images`, images)
}

// ─── 地理编码（景点名称 → 经纬度）────────────────────────────────────
export function geocodeAddress(address: string) {
  return request.get<unknown, ApiResult<{ latitude: number; longitude: number }>>('/user/geocode', { params: { address } })
}

// ─── 用户偏好 / 个人资料 ─────────────────────────────────────────────
export interface UserPreference {
  id?: number
  userId?: string
  name?: string
  email?: string
  phone?: string
  preferenceType?: string
  preferenceName?: string
  favoriteDestinations?: string
  defaultDepartureCity?: string
  defaultDays?: number
  defaultBudget?: number
  defaultTravelers?: number
  preferredSeason?: string
  preferredSeasonDetail?: string
  preferredMonth?: string
  preferredTripType?: string
  budgetLevel?: string
  dailyBudgetMin?: number
  dailyBudgetMax?: number
  travelStyle?: string
  interests?: string
  attractionTypes?: string
  maxAttractionsPerDay?: number
  preferFreeAttractions?: boolean
  dietaryRequirements?: string
  preferredCuisines?: string
  cuisinePreferences?: string
  mealBudgetPerPerson?: number
  spicyLevel?: number
  accommodationType?: string
  accommodationRequirements?: string
  hotelStarMin?: number
  hotelBudgetPerNightMin?: number
  hotelBudgetPerNightMax?: number
  preferredHotelType?: string
  transportationPreference?: string
  seatPreference?: string
  maxTransitDuration?: number
  travelCompanion?: string
  hasChildren?: boolean
  childrenAges?: string
  hasElderly?: boolean
  hasDisability?: boolean
  activityLevel?: string
  pacePreference?: string
  mobilityRequirements?: string
  shoppingPreference?: string
  shoppingBudget?: number
  specialRequests?: string
  notifyBeforeTripDays?: number
  notifyWeatherAlert?: boolean
  notifyPriceChange?: boolean
  preferredLanguage?: string
}

export function getPreferences() {
  return request.get<unknown, ApiResult<UserPreference>>('/user/preferences', { params: { userId: USER_ID } })
}
export function savePreferences(data: UserPreference) {
  return request.put<unknown, ApiResult<unknown>>('/user/preferences', data)
}

// ─── 头像 ────────────────────────────────────────────────────────────
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ApiResult<{ avatar: string }>>('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: { userId: USER_ID }
  })
}
export function getAvatar() {
  return request.get<unknown, ApiResult<{ avatar: string }>>('/user/avatar', { params: { userId: USER_ID } })
}

// ─── 用户昵称 ───────────────────────────────────────────────────────
export function getNickname() {
  return request.get<unknown, ApiResult<{ nickname: string }>>('/user/nickname', { params: { userId: USER_ID } })
}
export function updateNickname(nickname: string) {
  return request.put<unknown, ApiResult<unknown>>('/user/nickname', { nickname }, { params: { userId: USER_ID } })
}

// ─── 通用图片上传 ─────────────────────────────────────────────────
export function uploadImage(file: File, category: string = 'general') {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ApiResult<{ url: string }>>('/user/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: { category }
  })
}
