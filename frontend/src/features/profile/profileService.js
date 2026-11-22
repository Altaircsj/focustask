import { api, ApiError } from '../../api/client.js'

export const profileService = {
  async update(body, signal) {
    const user = await api.request('/me', { method: 'PATCH', body, signal })
    if (user?.id == null || typeof user.name !== 'string' || typeof user.email !== 'string') {
      throw new ApiError('A API retornou uma resposta inválida. Atualize a página e confira seus dados.')
    }
    return { id: user.id, name: user.name, email: user.email }
  },
  changePassword: ({ currentPassword, newPassword }, signal) =>
    api.request('/me/password', { method: 'PUT', body: { currentPassword, newPassword }, signal }),
  remove: (signal) => api.request('/me', { method: 'DELETE', signal }),
}
