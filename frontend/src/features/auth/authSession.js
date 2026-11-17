import { tokenStorage } from '../../api/client.js'
import { authService } from './authService.js'

// A generation guards against responses arriving after logout or effect cleanup.
export function createAuthSession({ service = authService, storage = tokenStorage } = {}) {
  let state = { user: null, status: 'checking', error: '', notice: '' }
  let generation = 0
  let pending
  const listeners = new Set()
  const publish = (next) => { state = { ...state, ...next }; listeners.forEach((listener) => listener()) }
  function cancelPending() { generation += 1; pending?.abort() }
  function begin() {
    cancelPending()
    pending = new AbortController()
    return { current: generation, signal: pending.signal }
  }
  function logout(notice = '') {
    cancelPending()
    storage.remove()
    publish({ user: null, status: 'anonymous', error: '', notice })
  }
  function invalidate(token) {
    // A delayed 401 from an older session must not clear a newer login.
    if (token === storage.get()) logout('Sua sessão expirou ou é inválida. Entre novamente.')
  }
  async function restore() {
    const { current, signal } = begin()
    publish({ user: null, status: 'checking', error: '' })
    try {
      const token = storage.get()
      if (!token) { publish({ status: 'anonymous' }); return }
      const user = await service.me(token, signal)
      if (current === generation) publish({ user, status: 'authenticated', notice: '' })
    } catch (error) {
      if (current !== generation) return
      if (error.status === 401) logout('Sua sessão expirou ou é inválida. Entre novamente.')
      else publish({ status: 'error', error: error.message })
    }
  }
  async function login(credentials) {
    const { current, signal } = begin()
    try {
      const token = await service.login(credentials, signal)
      if (current !== generation) return false
      const user = await service.me(token, signal)
      if (current !== generation) return false
      // Persist only after /me validates the token and returns the user.
      storage.set(token)
      publish({ user, status: 'authenticated', error: '', notice: '' })
      return true
    } catch (error) {
      if (current !== generation) return false
      throw error
    }
  }
  return {
    getSnapshot: () => state,
    subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener) },
    restore, login, logout, invalidate, cancelPending,
  }
}
