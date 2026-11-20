import { useState } from 'react'
import { LockKeyhole } from 'lucide-react'
import { activeSeconds, formatStartedAt } from '../../focusSessions/format'
import { MAX_MINUTES, MINUTE_OPTIONS } from '../summary'
import Modal from './Modal'

export default function FocusSessionForm({ session, projects, tasks, minutes, busy, error, onSave, onClose }) {
  const editing = Boolean(session)
  const lockedTask = session?.taskId != null
  const task = tasks.find((item) => item.id === session?.taskId)
  const initialMinutes = session?.plannedMinutes ?? minutes
  const [title, setTitle] = useState(session?.title ?? '')
  const [projectId, setProjectId] = useState(String(task?.projectId ?? session?.projectId ?? ''))
  const [taskId, setTaskId] = useState(String(session?.taskId ?? ''))
  const [value, setValue] = useState(initialMinutes)
  const [custom, setCustom] = useState(!MINUTE_OPTIONS.includes(initialMinutes))

  const options = tasks.filter((item) => item.status !== 'DONE' && String(item.projectId) === projectId)
  const chosen = Number(value)
  const validMinutes = Number.isInteger(chosen) && chosen >= 1 && chosen <= MAX_MINUTES
  const tooShort = editing && chosen * 60 <= activeSeconds(session)
  const allowed = validMinutes && !tooShort && !busy

  function submit(event) {
    event.preventDefault()
    if (!allowed) return
    const details = { title: title.trim(), plannedMinutes: chosen }
    if (editing) {
      if (!lockedTask) details.association = { projectId: Number(projectId) || null }
    } else {
      details.taskId = Number(taskId) || null
      details.projectId = taskId ? null : Number(projectId) || null
    }
    onSave(details)
  }

  return (
    <Modal title={editing ? 'Editar Sessão de Foco' : 'Nova Sessão de Foco'}
      subtitle={editing ? 'Atualize os dados da sessão selecionada.' : 'Inicie um cronômetro de foco imediato.'} onClose={onClose}>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="focus-title">Título da sessão (opcional)</label>
            <input id="focus-title" className="chip-input" maxLength={255} value={title} placeholder="Ex.: Revisão de código" onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="focus-project">Projeto (opcional)</label>
            <select id="focus-project" value={projectId} disabled={lockedTask || !projects.length}
              onChange={(e) => { setProjectId(e.target.value); setTaskId('') }}>
              <option value="">Nenhum</option>
              {projects.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="focus-task">{editing ? 'Tarefa associada' : 'Tarefa (opcional)'}</label>
            {editing ? (
              <div className="locked-field"><input id="focus-task" value={task?.title ?? 'Nenhuma'} disabled /><LockKeyhole size={16} aria-hidden="true" /></div>
            ) : (
              <select id="focus-task" value={taskId} onChange={(e) => setTaskId(e.target.value)} disabled={!options.length}>
                <option value="">{!projectId ? 'Selecione um projeto primeiro' : options.length ? 'Nenhuma' : 'Este projeto não tem tarefas pendentes'}</option>
                {options.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
              </select>
            )}
          </div>
          <div className="form-field">
            <span id="focus-duration" className="form-label">Duração</span>
            <div className="chip-row" role="group" aria-labelledby="focus-duration">
              {MINUTE_OPTIONS.map((option) => (
                <button key={option} type="button" className="chip" aria-pressed={!custom && chosen === option}
                  onClick={() => { setCustom(false); setValue(option) }}>{option}m</button>
              ))}
              <button type="button" className="chip" aria-pressed={custom} onClick={() => setCustom(true)}>Personalizado…</button>
            </div>
            {custom && <input type="number" className="chip-input" aria-label="Minutos" min="1" max={MAX_MINUTES} value={value} onChange={(e) => setValue(e.target.value)} />}
            {!validMinutes && <p role="alert">Informe de 1 a {MAX_MINUTES} minutos.</p>}
            {validMinutes && tooShort && <p role="alert">A duração precisa superar o tempo já trabalhado. Para encerrar agora, use Finalizar sessão no Painel.</p>}
          </div>
        </fieldset>
        <p className="modal-note">{editing
          ? `${formatStartedAt(session.startedAt)} · ${session.plannedMinutes == null ? 'Duração planejada não registrada' : `Duração original: ${session.plannedMinutes} min`}`
          : 'Você poderá associar esta sessão a uma tarefa após concluí-la.'}</p>
        {error && <p role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="dash-btn" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!allowed}>
            {editing ? (busy ? 'Salvando…' : 'Salvar alterações') : (busy ? 'Iniciando…' : 'Iniciar Foco')}
          </button>
        </div>
      </form>
    </Modal>
  )
}
