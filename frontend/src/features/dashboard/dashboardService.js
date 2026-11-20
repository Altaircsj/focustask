import { api } from '../../api/client.js'
import { MAX_MINUTES } from './summary.js'

const MINUTES_KEY = 'focustask.focusMinutes'

// Preferência para o próximo foco e referência para sessões anteriores à migração.
export function loadMinutes(fallback) {
  try {
    const value = Number(localStorage.getItem(MINUTES_KEY))
    return Number.isInteger(value) && value > 0 && value <= MAX_MINUTES ? value : fallback
  } catch {
    return fallback
  }
}

export function saveMinutes(minutes) {
  try { localStorage.setItem(MINUTES_KEY, String(minutes)) } catch { /* armazenamento indisponível */ }
}

export const dashboardService = {
  completeTask: (id, signal) => api.request(`/tasks/${id}`, { method: 'PATCH', body: { status: 'DONE' }, signal }),
}
