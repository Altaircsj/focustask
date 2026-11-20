import { api } from '../../api/client.js'

export const dashboardService = {
  completeTask: (id, signal) => api.request(`/tasks/${id}`, { method: 'PATCH', body: { status: 'DONE' }, signal }),
}
