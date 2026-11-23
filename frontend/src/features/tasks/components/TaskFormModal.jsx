import { useState } from 'react'
import Modal from '../../dashboard/components/Modal'
import { PRIORITY_LABEL, STATUS_LABEL } from '../format'

export default function TaskFormModal({ task, projects, defaultProjectId, busy, error, onSave, onClose }) {
  const [title, setTitle] = useState(task?.title ?? '')
  const [projectId, setProjectId] = useState(String(task?.projectId ?? defaultProjectId ?? projects[0]?.id ?? ''))
  const [status, setStatus] = useState(task?.status ?? 'TODO')
  const [priority, setPriority] = useState(task?.priority ?? 'MEDIUM')
  const [description, setDescription] = useState(task?.description ?? '')
  const [dueDate, setDueDate] = useState(task?.dueDate ?? '')
  const [estimate, setEstimate] = useState(String(task?.estimatedMinutes ?? ''))
  const minutes = estimate === '' ? null : Number(estimate)
  const validEstimate = minutes === null || (/^\d+$/.test(estimate) && minutes > 0 && minutes <= 2147483647)
  const valid = validEstimate && title.trim().length > 0 && title.length <= 255 && projectId !== ''

  function submit(event) {
    event.preventDefault()
    if (!valid || busy) return
    const details = { projectId: Number(projectId), title: title.trim(), description: description.trim(), status, priority }
    if (dueDate) details.dueDate = dueDate
    if (minutes !== (task?.estimatedMinutes ?? null)) details.estimatedMinutes = minutes
    onSave(details)
  }

  return (
    <Modal title={task ? 'Editar Tarefa' : 'Criar Tarefa'} subtitle="Preencha os dados da tarefa. Na edição, os valores atuais são mantidos." onClose={onClose}>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="task-title">Título</label>
            <input id="task-title" className="chip-input" maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="task-project">Projeto</label>
            <select id="task-project" value={projectId} onChange={(e) => setProjectId(e.target.value)} disabled={projects.length === 0}>
              {projects.length === 0 && <option value="">Crie um projeto primeiro</option>}
              {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="task-status">Status</label>
            <select id="task-status" value={status} onChange={(e) => setStatus(e.target.value)}>
              {Object.entries(STATUS_LABEL).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
            </select>
          </div>
          <div className="form-field">
            <span id="task-priority" className="form-label">Prioridade</span>
            <div className="chip-row" role="group" aria-labelledby="task-priority">
              {['LOW', 'MEDIUM', 'HIGH'].map((id) => (
                <button key={id} type="button" className="chip" aria-pressed={priority === id} onClick={() => setPriority(id)}>{PRIORITY_LABEL[id]}</button>
              ))}
            </div>
          </div>
          <div className="form-field">
            <label htmlFor="task-description">Descrição (opcional)</label>
            <textarea id="task-description" className="chip-input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="task-estimate">Tempo estimado (minutos)</label>
            <input id="task-estimate" className="chip-input" inputMode="numeric" placeholder="90" value={estimate}
              aria-invalid={!validEstimate} aria-describedby={!validEstimate ? 'task-estimate-error' : undefined}
              onChange={(e) => setEstimate(e.target.value)} />
            {!validEstimate && <p id="task-estimate-error" role="alert">Informe um número inteiro de 1 a 2147483647 ou deixe em branco.</p>}
          </div>
          <div className="form-field">
            <label htmlFor="task-due">Prazo</label>
            <input id="task-due" type="date" className="chip-input" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>
        </fieldset>
        <p className="modal-note">Todos os campos podem ser ajustados antes de salvar.</p>
        {error && <p role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="dash-btn" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!valid || busy}>{busy ? 'Salvando…' : 'Salvar Tarefa'}</button>
        </div>
      </form>
    </Modal>
  )
}
