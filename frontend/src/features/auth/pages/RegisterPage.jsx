import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { useAuth } from '../useAuth'
import { authService } from '../authService'
import AuthField from '../components/AuthField'
import { authErrorMessage, validateAuth } from '../validation'
import '../auth.css'

export default function RegisterPage() {
  const [values, setValues] = useState({ name: '', email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const pending = useRef(null)
  const busy = useRef(false)
  const { user } = useAuth()
  const navigate = useNavigate()

  useEffect(() => () => pending.current?.abort(), [])
  if (user) return <Navigate to="/dashboard" replace />

  async function handleSubmit(e) {
    e.preventDefault()
    if (busy.current) return
    const data = { ...values, name: values.name.trim(), email: values.email.trim() }
    const validation = validateAuth(data, true)
    setErrors(validation)
    setMessage('')
    if (Object.keys(validation).length) return
    busy.current = true
    setSubmitting(true)
    const controller = new AbortController()
    pending.current = controller
    try {
      await authService.register(data, controller.signal)
      if (!controller.signal.aborted) navigate('/login', { replace: true, state: { registered: true } })
    } catch (error) {
      if (!controller.signal.aborted) { setMessage(authErrorMessage(error, true)); setErrors(error.errors || {}) }
    } finally {
      busy.current = false
      if (!controller.signal.aborted) setSubmitting(false)
    }
  }

  const change = (event) => setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  
  return (
    <main className="auth-card">
      <h1>Criar conta no FocusTask</h1>
      <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
        <AuthField name="name" label="Nome" autoComplete="name" maxLength={255} value={values.name} onChange={change} error={errors.name} required />
        <AuthField name="email" label="E-mail" type="email" autoComplete="email" maxLength={254} value={values.email} onChange={change} error={errors.email} required />
        <AuthField name="password" label="Senha" type="password" autoComplete="new-password" value={values.password} onChange={change} error={errors.password} minLength={6} required />
        <p className="form-hint">Use pelo menos 6 caracteres e até 72 bytes UTF-8 na senha.</p>
        <button type="submit" disabled={submitting}>{submitting ? 'Criando conta…' : 'Criar conta'}</button>
        {message && <p role="alert">{message}</p>}
      </form>
      <p>Já tem conta? <Link to="/login">Entrar</Link></p>
    </main>
  )
}
