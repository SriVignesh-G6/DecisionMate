const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

async function request(path, options = {}) {
  const response = await fetch(`${BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  })
  if (response.status === 204) return null
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.error || data.message || `Request failed (${response.status})`)
  return data
}
const json = (method, body) => ({ method, body: JSON.stringify(body) })

export const api = {
  health: () => request('/health'),
  decisions: () => request('/decisions'),
  createDecision: (body) => request('/decisions', json('POST', body)),
  updateDecision: (id, body) => request(`/decisions/${id}`, json('PUT', body)),
  deleteDecision: (id) => request(`/decisions/${id}`, { method: 'DELETE' }),
  decision: (id) => request(`/decisions/${id}`),
  addCriterion: (id, body) => request(`/decisions/${id}/criteria`, json('POST', body)),
  deleteCriterion: (id) => request(`/criteria/${id}`, { method: 'DELETE' }),
  addAlternative: (id, body) => request(`/decisions/${id}/alternatives`, json('POST', body)),
  deleteAlternative: (id) => request(`/alternatives/${id}`, { method: 'DELETE' }),
  saveScore: (id, body) => request(`/decisions/${id}/scores`, json('PUT', body)),
  results: (id) => request(`/decisions/${id}/results`),
}
