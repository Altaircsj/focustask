import { useState } from 'react'
import Modal from '../../dashboard/components/Modal'
import { PRIORITY_LABEL, STATUS_LABEL } from '../format'

export default function EditTaskModal({ task, busy, error, onSave, onClose }) {
  const [title, setTitle] = useState(task.title)
  const [description, setDescription] = useState(task.description ?? '')
  const [status, setStatus] = useState(task.status)
  const [priority, setPriority] = useState(task.priority ?? 'MEDIUM')
  const [dueDate, setDueDate] = useState(task.dueDate ?? '')
  const valid = title.trim().length > 0

  function submit(event) {
    event.preventDefault()
    if (!valid || busy) return
    const details = { title: title.trim(), description: description.trim(), status, priority }
    if (dueDate) details.dueDate = dueDate
    onSave(details)
  }

  return (
    <Modal title="Editar Tarefa" subtitle="Atualize os dados da tarefa." onClose={onClose}>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="task-title">Título</label>
            <input id="task-title" className="chip-input" maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="task-description">Descrição (opcional)</label>
            <textarea id="task-description" className="chip-input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="task-status">Status</label>
            <select id="task-status" value={status} onChange={(e) => setStatus(e.target.value)}>
              {Object.entries(STATUS_LABEL).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="task-priority">Prioridade</label>
            <select id="task-priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
              {Object.entries(PRIORITY_LABEL).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="task-due">Prazo de entrega</label>
            <input id="task-due" type="date" className="chip-input" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>
        </fieldset>
        {error && <p role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="dash-btn" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!valid || busy}>{busy ? 'Salvando…' : 'Salvar alterações'}</button>
        </div>
      </form>
    </Modal>
  )
}
