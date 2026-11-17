import { api, ApiError } from '../../api/client.js'

export const authService = {
  register: (data, signal) => api.request('/auth/register', { method: 'POST', body: data, authenticated: false, signal }),
  async login(data, signal) {
    const result = await api.request('/auth/login', { method: 'POST', body: data, authenticated: false, signal })
    if (typeof result?.token !== 'string' || !result.token.trim()) throw new ApiError('A API não retornou um token válido. Tente entrar novamente.')
    return result.token
  },
  async me(token, signal) {
    const user = await api.request('/me', { token, signal })
    if (user?.id == null || typeof user.name !== 'string' || typeof user.email !== 'string') {
      throw new ApiError('Não foi possível confirmar os dados da sua conta. Tente novamente.')
    }
    return { id: user.id, name: user.name, email: user.email }
  },
}
