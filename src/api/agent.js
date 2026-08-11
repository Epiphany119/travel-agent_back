import request from './index';
export function createSession(data) {
    return request.post('/agent/sessions', data);
}
export function sendMessage(data) {
    return request.post('/agent/messages', data);
}
export function getMessages(sessionId) {
    return request.get(`/agent/sessions/${sessionId}/messages`);
}
export function getUserSessions(userId = 1) {
    return request.get('/agent/sessions', { params: { userId } });
}
export function deleteSession(sessionId) {
    return request.delete(`/agent/sessions/${sessionId}`);
}
export function generateTravelPlan(data) {
    return request.post('/travel-plans/generate', data);
}
//# sourceMappingURL=agent.js.map