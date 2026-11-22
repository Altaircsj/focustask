import { useRef, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import Modal from '../../dashboard/components/Modal'
import { getInitials } from '../../../layouts/initials'
import { profileService } from '../profileService'
import { profileErrorMessage, validateProfile } from '../validation'
import '../../dashboard/dashboard.css'
import '../profile.css'

export default function ProfilePage() {
  const { user, updateUser, logout } = useAuth()
  const [values, setValues] = useState({ name: user.name, email: user.email })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState({ type: '', text: '' })
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const working = useRef(false)

  const changed = values.name.trim() !== user.name || values.email.trim() !== user.email

  function change(field) {
    return (event) => {
      setValues((current) => ({ ...current, [field]: event.target.value }))
      setErrors((current) => ({ ...current, [field]: undefined }))
      setMessage({ type: '', text: '' })
    }
  }

  async function save(event) {
    event.preventDefault()
    if (working.current || !changed) return
    const found = validateProfile(values)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    working.current = true
    setBusy(true)
    setMessage({ type: '', text: '' })
    try {
      const body = {}
      if (values.name.trim() !== user.name) body.name = values.name.trim()
      if (values.email.trim() !== user.email) body.email = values.email.trim()
      const updated = await profileService.update(body)
      updateUser(updated)
      setValues({ name: updated.name, email: updated.email })
      setMessage({ type: 'status', text: 'Alterações salvas.' })
    } catch (error) {
      setMessage({ type: 'alert', text: profileErrorMessage(error) })
    } finally {
      working.current = false
      setBusy(false)
    }
  }

  async function deleteAccount() {
    if (working.current) return
    working.current = true
    setBusy(true)
    setDeleteError('')
    try {
      await profileService.remove()
      logout('Sua conta foi excluída.')
    } catch (error) {
      setDeleteError(profileErrorMessage(error))
      working.current = false
      setBusy(false)
    }
  }

  const closeConfirm = () => { if (!working.current) { setConfirming(false); setDeleteError('') } }

  return (
    <main className="dashboard-page profile-page">
      <header className="profile-header">
        <div>
          <h1>Perfil</h1>
          <p>Edite suas informações pessoais e credenciais de acesso.</p>
        </div>
        <button type="submit" form="profile-form" className="dash-btn dash-btn-primary" disabled={!changed || busy}>{busy ? 'Salvando…' : 'Salvar Alterações'}</button>
      </header>

      <section className="profile-card">
        <div className="profile-identity">
          <span className="profile-avatar" aria-hidden="true">{getInitials(user.name)}</span>
          <div>
            <p className="profile-name">{user.name}</p>
            <p className="profile-email">{user.email}</p>
          </div>
        </div>

        <form id="profile-form" onSubmit={save} noValidate aria-busy={busy}>
          <fieldset className="profile-section" disabled={busy} aria-labelledby="personal-title">
            <h2 id="personal-title">Dados Pessoais</h2>
            <div className="profile-grid">
              <div className="form-field">
                <label htmlFor="profile-name">Nome Completo</label>
                <input id="profile-name" className="chip-input" maxLength={255} autoComplete="name" value={values.name} aria-invalid={Boolean(errors.name)} aria-describedby={errors.name ? 'profile-name-error' : undefined} onChange={change('name')} />
                {errors.name && <p id="profile-name-error" className="modal-error">{errors.name}</p>}
              </div>
              <div className="form-field">
                <label htmlFor="profile-email">E-mail</label>
                <input id="profile-email" type="email" className="chip-input" maxLength={254} autoComplete="email" value={values.email} aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'profile-email-error' : undefined} onChange={change('email')} />
                {errors.email && <p id="profile-email-error" className="modal-error">{errors.email}</p>}
              </div>
            </div>
          </fieldset>
          {message.text && <p role={message.type} className={message.type === 'status' ? 'profile-ok' : undefined}>{message.text}</p>}
        </form>

        <fieldset className="profile-section" disabled aria-labelledby="password-title" aria-describedby="password-note">
          <h2 id="password-title">Alterar Senha</h2>
          <div className="form-field">
            <label htmlFor="current-password">Senha Atual</label>
            <input id="current-password" type="password" className="chip-input" autoComplete="current-password" />
          </div>
          <div className="profile-grid">
            <div className="form-field">
              <label htmlFor="new-password">Nova Senha</label>
              <input id="new-password" type="password" className="chip-input" autoComplete="new-password" />
            </div>
            <div className="form-field">
              <label htmlFor="confirm-password">Confirmar Nova Senha</label>
              <input id="confirm-password" type="password" className="chip-input" autoComplete="new-password" />
            </div>
          </div>
          <p id="password-note" className="modal-note">A troca de senha ainda não está disponível na API.</p>
        </fieldset>

        <section className="profile-danger" aria-labelledby="danger-title">
          <h2 id="danger-title">Zona de Perigo</h2>
          <div className="danger-box">
            <div>
              <p className="danger-title">Excluir permanentemente sua conta</p>
              <p className="danger-text">Todos os seus projetos, tarefas e histórico de foco serão deletados para sempre.</p>
            </div>
            <button type="button" className="dash-btn dash-btn-solid-danger" onClick={() => setConfirming(true)}>Excluir Conta</button>
          </div>
        </section>
      </section>

      {confirming && (
        <Modal title="Excluir sua conta?" subtitle="Seus projetos, tarefas e sessões serão apagados e esta ação não pode ser desfeita." onClose={closeConfirm}>
          {deleteError && <p role="alert">{deleteError}</p>}
          <div className="modal-actions">
            <button type="button" className="dash-btn" disabled={busy} onClick={closeConfirm}>Cancelar</button>
            <button type="button" className="dash-btn dash-btn-solid-danger" disabled={busy} onClick={deleteAccount}>{busy ? 'Excluindo…' : 'Excluir conta'}</button>
          </div>
        </Modal>
      )}
    </main>
  )
}
