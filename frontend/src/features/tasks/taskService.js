import { api } from '../../api/client.js'

export const taskService = {
  create: (projectId, body, signal) => api.request(`/projects/${projectId}/tasks`, { method: 'POST', body, signal }),
  get: (id, signal) => api.request(`/tasks/${id}`, { signal }),
  update: (id, body, signal) => api.request(`/tasks/${id}`, { method: 'PATCH', body, signal }),
  remove: (id, signal) => api.request(`/tasks/${id}`, { method: 'DELETE', signal }),
}

// A criação só aceita título, descrição e prazo: status e prioridade são aplicados em seguida.
export async function createTask(details) {
  const { projectId, status, priority, ...base } = details
  const created = await taskService.create(projectId, base)
  try {
    return { task: await taskService.update(created.id, { status, priority }) }
  } catch {
    return { task: created, warning: 'Tarefa criada, mas o status e a prioridade não puderam ser aplicados. Edite a tarefa para ajustá-los.' }
  }
}
