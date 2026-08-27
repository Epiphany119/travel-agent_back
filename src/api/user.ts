import request from './index'

export interface ApiResult<T> {
  code: number
  message?: string
  data: T
}

// 获取当前登录用户 ID
function getCurrentUserId(): string {
  return localStorage.getItem('roamly_user_id') || 'user_001'
}

export interface Inspiration {
  id?: number; userId?: string; name: string; imageUrl?: string
  quote?: string; description?: string; tags?: string; priority?: number
  estimatedBudget?: number; bestSeason?: string; status?: number; sortOrder?: number
  createdAt?: string
}

export function listInspirations() {
  return request.get<unknown, ApiResult<Inspiration[]>>('/user/inspirations', { params: { userId: getCurrentUserId() } })
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

export interface JourneyPoint {
  id?: number; journeyId?: number; name: string
  latitude?: number | string; longitude?: number | string
  visitDate?: string; description?: string; sortOrder?: number
}
export interface JourneyImage {
  id?: number; journeyId?: number; imageUrl: string
  caption?: string; sortOrder?: number
}
export interface Journey {
  id?: number; userId?: string; destination: string; departureCity?: string
  startDate?: string; endDate?: string; totalDays?: number; summary?: string
  totalCost?: number; rating?: number; travelType?: string; companions?: string
  weatherInfo?: string; highlight?: string; tips?: string; status?: number; createdAt?: string
}
export interface JourneyDetail { journey: Journey; points: JourneyPoint[]; images: JourneyImage[] }

export function listJourneys() {
  return request.get<unknown, ApiResult<JourneyDetail[]>>('/user/journeys', { params: { userId: getCurrentUserId() } })
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

export interface TravelNote {
  id?: number; userId?: string; title: string; destination: string
  noteType?: 'inspiration'|'journey'; sourceType?: string; sourceId?: number
  templateVersion?: number; status?: string; visibility?: 'private'|'link'
  shareToken?: string; coverUrl?: string; startDate?: string; endDate?: string
  totalDays?: number; travelers?: number; budget?: number; contentJson: string
}
export function listTravelNotes() { return request.get<unknown, ApiResult<TravelNote[]>>('/user/travel-notes', { params: { userId: getCurrentUserId() } }) }
export function getTravelNote(id: number) { return request.get<unknown, ApiResult<TravelNote>>('/user/travel-notes/' + id) }
export function saveTravelNote(data: TravelNote) { return request.post<unknown, ApiResult<TravelNote>>('/user/travel-notes', data) }
export function copyTravelNote(id: number) { return request.post<unknown, ApiResult<TravelNote>>('/user/travel-notes/' + id + '/copy', null, { params: { userId: getCurrentUserId() } }) }
export function getSharedTravelNote(token: string) { return request.get<unknown, ApiResult<TravelNote>>('/user/travel-notes/share/' + token) }
export function deleteTravelNote(id: number) { return request.delete<unknown, ApiResult<unknown>>('/user/travel-notes/' + id) }
export function searchUsers(q: string) { return request.get<unknown, ApiResult<any[]>>('/user/users/search', { params: { q } }) }
export function listPublicNotes(page=0, size=20, q?: string, tag?: string) { return request.get<unknown, ApiResult<any[]>>('/user/social/notes', { params: { page, size, q, tag } }) }
export function getPublicNote(id: number) { return request.get<unknown, ApiResult<any>>('/user/social/notes/' + id) }
export function reactNote(id: number, type: 'like'|'favorite') { return request.post('/user/social/notes/' + id + '/reaction', null, { params: { type, userId: getCurrentUserId() } }) }
export function listComments(id: number) { return request.get<unknown, ApiResult<any[]>>('/user/social/notes/' + id + '/comments') }
export function addComment(id: number, content: string) { return request.post('/user/social/notes/' + id + '/comments', { content }, { params: { userId: getCurrentUserId() } }) }
export function requestFriend(id: string, message='') { return request.post('/user/users/' + id + '/friend-request', { message }, { params: { from: getCurrentUserId() } }) }
export function publishSocialNote(data: { userId: string; travelNoteId?: number; title: string; content: string; coverUrl?: string; destination?: string; tags?: string[]; authorName?: string; authorAvatar?: string }) { return request.post('/user/social/notes', data) }
export function saveJourneyPoints(journeyId: number, points: JourneyPoint[]) {
  return request.post<unknown, ApiResult<unknown>>(`/user/journeys/${journeyId}/points`, points)
}
export function saveJourneyImages(journeyId: number, images: JourneyImage[]) {
  return request.post<unknown, ApiResult<unknown>>(`/user/journeys/${journeyId}/images`, images)
}

export function geocodeAddress(address: string) {
  return request.get<unknown, ApiResult<{ latitude: number; longitude: number }>>('/user/geocode', { params: { address } })
}

// ─── 用户偏好 / 个人资料 ─────────────────────────────────────────────
export interface UserPreference {
  systemThemeJson?: string
  id?: number; userId?: string; name?: string; email?: string; phone?: string
  preferenceType?: string; preferenceName?: string
  favoriteDestinations?: string; defaultDepartureCity?: string
  defaultDays?: number; defaultBudget?: number; defaultTravelers?: number
  preferredSeason?: string; preferredSeasonDetail?: string; preferredMonth?: string
  preferredTripType?: string; budgetLevel?: string
  dailyBudgetMin?: number; dailyBudgetMax?: number
  travelStyle?: string; interests?: string; attractionTypes?: string
  maxAttractionsPerDay?: number; preferFreeAttractions?: boolean
  dietaryRequirements?: string; preferredCuisines?: string; cuisinePreferences?: string
  mealBudgetPerPerson?: number; spicyLevel?: number
  accommodationType?: string; accommodationRequirements?: string
  hotelStarMin?: number; hotelBudgetPerNightMin?: number; hotelBudgetPerNightMax?: number
  preferredHotelType?: string; transportationPreference?: string
  seatPreference?: string; maxTransitDuration?: number
  travelCompanion?: string; hasChildren?: boolean; childrenAges?: string
  hasElderly?: boolean; hasDisability?: boolean; activityLevel?: string
  pacePreference?: string; mobilityRequirements?: string
  shoppingPreference?: string; shoppingBudget?: number; specialRequests?: string
  notifyBeforeTripDays?: number; notifyWeatherAlert?: boolean; notifyPriceChange?: boolean
  preferredLanguage?: string
}

export function getPreferences() {
  return request.get<unknown, ApiResult<UserPreference & { email?: string; username?: string }>>('/user/preferences', { params: { userId: getCurrentUserId() } })
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
    params: { userId: getCurrentUserId() }
  })
}
export function getAvatar() {
  return request.get<unknown, ApiResult<{ avatar: string }>>('/user/avatar', { params: { userId: getCurrentUserId() } })
}

// ─── 用户昵称 ───────────────────────────────────────────────────────
export function getNickname() {
  return request.get<unknown, ApiResult<{ nickname: string }>>('/user/nickname', { params: { userId: getCurrentUserId() } })
}
export function updateNickname(nickname: string) {
  return request.put<unknown, ApiResult<unknown>>('/user/nickname', { nickname }, { params: { userId: getCurrentUserId() } })
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

// ─── 向后兼容导出（不推荐，逐步迁移到 getCurrentUserId()）──
export const USER_ID = 'user_001'
