import { useEffect, useState, useSyncExternalStore } from 'react'
import { api } from '../../api/client'
import { createAuthSession } from './authSession'

import { AuthContext, useAuth } from './useAuth'

export function AuthProvider({ children }) {
  const [session] = useState(() => createAuthSession())
  const state = useSyncExternalStore(session.subscribe, session.getSnapshot)
  useEffect(() => {
    const unsubscribe = api.onUnauthorized(session.invalidate)
    session.restore()
    return () => { unsubscribe(); session.cancelPending() }
  }, [session])
  return <AuthContext.Provider value={{ ...state, login: session.login, retry: session.restore, logout: session.logout, cancelPending: session.cancelPending }}>{children}</AuthContext.Provider>
}

export function SessionGate({ children }) {
  const { status, error, retry, logout } = useAuth()
  if (status === 'checking') return <main className="auth-card" role="status">Verificando sua sessão…</main>
  if (status === 'error') return (
    <main className="auth-card">
      <h1>Não foi possível verificar sua sessão</h1>
      <p role="alert">{error}</p>
      <p>Seu acesso foi preservado. Tente novamente quando a API estiver disponível.</p>
      <button onClick={retry}>Tentar novamente</button>
      <button className="secondary" onClick={() => logout()}>Sair da sessão</button>
    </main>
  )
  return children
}
