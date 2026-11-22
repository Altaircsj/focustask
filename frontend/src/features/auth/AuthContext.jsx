import { useCallback, useEffect, useRef, useState } from 'react'
import { api, tokenStorage } from '../../api/client'
import { authService } from './authService'
import { AuthContext } from './useAuth'

async function loadSavedUser(signal) {
  const token = tokenStorage.get()
  return token ? authService.me(token, signal) : null
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState({ checking: true })
  const pending = useRef(null)

  const cancelPending = useCallback(() => {
    pending.current?.abort()
  }, [])

  const logout = useCallback((notice = '') => {
    cancelPending()
    tokenStorage.remove()
    setSession({ user: null, notice })
  }, [cancelPending])

  const restore = useCallback(() => {
    cancelPending()
    const controller = new AbortController()
    pending.current = controller

    loadSavedUser(controller.signal).then(
      (user) => {
        if (!controller.signal.aborted) setSession({ user })
      },
      (error) => {
        if (!controller.signal.aborted) setSession({ error: error.message })
      },
    )
  }, [cancelPending])

  useEffect(() => {
    const unsubscribe = api.onUnauthorized((token) => {
      if (token === tokenStorage.get()) logout('Sua sessão expirou ou é inválida. Entre novamente.')
    })
    restore()
    return () => {
      unsubscribe()
      cancelPending()
    }
  }, [cancelPending, logout, restore])

  const updateUser = useCallback((user) => setSession((current) => (current.user ? { ...current, user } : current)), [])

  async function login(credentials) {
    cancelPending()
    const controller = new AbortController()
    pending.current = controller
    const { signal } = controller
    const token = await authService.login(credentials, signal)
    signal.throwIfAborted()
    const user = await authService.me(token, signal)
    signal.throwIfAborted()
    tokenStorage.set(token)
    setSession({ user })
  }

  function retry() {
    setSession({ checking: true })
    restore()
  }

  if (session.checking) return <main className="auth-card" role="status">Verificando sua sessão…</main>

  if (session.error) return (
    <main className="auth-card">
      <h1>Não foi possível verificar sua sessão</h1>
      <p role="alert">{session.error}</p>
      <p>Seu acesso foi preservado. Tente novamente quando a API estiver disponível.</p>
      <button onClick={retry}>Tentar novamente</button>
      <button className="secondary" onClick={() => logout()}>Sair da sessão</button>
    </main>
  )

  return (
    <AuthContext.Provider value={{ user: session.user, notice: session.notice, login, logout, updateUser, cancelPending }}>
      {children}
    </AuthContext.Provider>
  )
}
