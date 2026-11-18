import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../useAuth'
import AuthField from '../components/AuthField'
import { authErrorMessage, validateAuth } from '../validation'
import '../auth.css'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [message, setMessage] = useState('')
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const busy = useRef(false)
  const mounted = useRef(false)
  const { user, login, notice, cancelPending } = useAuth()
  const location = useLocation()

  useEffect(() => {
    mounted.current = true
    return () => { mounted.current = false; cancelPending() }
  }, [cancelPending])
  if (user) return <Navigate to="/dashboard" replace />

  async function handleSubmit(e) {
    e.preventDefault()
    if (busy.current) return
    const credentials = { email: email.trim(), password: senha }
    const validation = validateAuth(credentials)
    setErrors(validation)
    setMessage('')
    if (Object.keys(validation).length) return
    busy.current = true
    setSubmitting(true)
    try {
      await login(credentials)
    } catch (error) {
      if (mounted.current) { setMessage(authErrorMessage(error)); setErrors(error.errors || {}) }
    } finally {
      busy.current = false
      if (mounted.current) setSubmitting(false)
    }
  }
  
  return (
    <main className="auth-card">
      <h1>Entrar no FocusTask</h1>
      {location.state?.registered && <p role="status">Conta criada com sucesso. Entre para continuar.</p>}
      {notice && <p role="status">{notice}</p>}
      <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
        <AuthField name="email" label="E-mail" type="email" autoComplete="username" maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} required />
        <AuthField name="password" label="Senha" type="password" autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} error={errors.password} required />
        <button type="submit" disabled={submitting}>{submitting ? 'Entrando…' : 'Entrar'}</button>
        {message && <p role="alert">{message}</p>}
      </form>
      <p>Ainda não tem conta? <Link to="/register">Criar conta</Link></p>
    </main>
  )
}
