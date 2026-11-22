import { api } from '../../api/client.js'

export const taskService = {
  get: (id, signal) => api.request(`/tasks/${id}`, { signal }),
  update: (id, body, signal) => api.request(`/tasks/${id}`, { method: 'PATCH', body, signal }),
  remove: (id, signal) => api.request(`/tasks/${id}`, { method: 'DELETE', signal }),
}
