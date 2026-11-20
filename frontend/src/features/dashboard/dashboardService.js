import { api } from '../../api/client.js'

const MINUTES_KEY = 'focustask.focusMinutes'

// O backend não guarda a duração: ela fica só como preferência deste navegador.
export function loadMinutes(fallback) {
  try {
    const value = Number(localStorage.getItem(MINUTES_KEY))
    return Number.isInteger(value) && value > 0 ? value : fallback
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
