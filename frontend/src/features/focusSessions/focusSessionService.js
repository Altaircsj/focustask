import { api } from '../../api/client.js'

export const focusSessionService = {
  list: (signal) => api.request('/focus-sessions', { signal }),
  create: (taskId, signal) => api.request('/focus-sessions', { method: 'POST', body: { taskId }, signal }),
  setStatus: (id, status, signal) => api.request(`/focus-sessions/${id}`, { method: 'PATCH', body: { status }, signal }),
  remove: (id, signal) => api.request(`/focus-sessions/${id}`, { method: 'DELETE', signal }),
}

// Leitura de projetos e tarefas, usada para nomear e vincular sessões.
export const catalogService = {
  projects: (signal) => api.request('/projects', { signal }),
  tasks: (signal) => api.request('/tasks', { signal }),
}
