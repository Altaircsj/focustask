import { api } from '../../api/client.js'

export const projectService = {
  get: (id, signal) => api.request(`/projects/${id}`, { signal }),
  create: (body, signal) => api.request('/projects', { method: 'POST', body, signal }),
  update: (id, body, signal) => api.request(`/projects/${id}`, { method: 'PATCH', body, signal }),
  remove: (id, signal) => api.request(`/projects/${id}`, { method: 'DELETE', signal }),
}
