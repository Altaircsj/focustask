import { useState } from 'react'
import { MAX_MINUTES, MINUTE_OPTIONS } from '../summary'
import Modal from './Modal'

export default function NewFocusModal({ projects, tasks, minutes, busy, onStart, onClose }) {
  const [projectId, setProjectId] = useState('')
  const [taskId, setTaskId] = useState('')
  const [custom, setCustom] = useState(!MINUTE_OPTIONS.includes(minutes))
  const [value, setValue] = useState(minutes)

  const projectTasks = tasks.filter((task) => task.status !== 'DONE' && String(task.projectId) === projectId)
  const taskPlaceholder = !projectId ? 'Nenhuma (selecione um projeto primeiro)' : projectTasks.length === 0 ? 'Este projeto não tem tarefas pendentes' : 'Nenhuma'
  const chosen = Number(value)
  const valid = Number.isInteger(chosen) && chosen >= 1 && chosen <= MAX_MINUTES

  function submit(event) {
    event.preventDefault()
    if (valid) onStart({ taskId: taskId ? Number(taskId) : null, minutes: chosen })
  }

  return (
    <Modal title="Nova Sessão de Foco" subtitle="Inicie um cronômetro de foco imediato." onClose={onClose}>
      <form onSubmit={submit}>
        <div className="form-field">
          <label htmlFor="focus-project">Projeto (opcional)</label>
          <select id="focus-project" value={projectId} onChange={(event) => { setProjectId(event.target.value); setTaskId('') }} disabled={projects.length === 0}>
            <option value="">Nenhum</option>
            {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
          </select>
        </div>
        <div className="form-field">
          <label htmlFor="focus-task">Tarefa (opcional)</label>
          <select id="focus-task" value={taskId} onChange={(event) => setTaskId(event.target.value)} disabled={projectTasks.length === 0}>
            <option value="">{taskPlaceholder}</option>
            {projectTasks.map((task) => <option key={task.id} value={task.id}>{task.title}</option>)}
          </select>
        </div>
        <div className="form-field">
          <span id="focus-duration" className="form-label">Duração</span>
          <div className="chip-row" role="group" aria-labelledby="focus-duration">
            {MINUTE_OPTIONS.map((option) => (
              <button key={option} type="button" className="chip" aria-pressed={!custom && chosen === option} onClick={() => { setCustom(false); setValue(option) }}>{option}m</button>
            ))}
            <button type="button" className="chip" aria-pressed={custom} onClick={() => setCustom(true)}>Personalizado…</button>
          </div>
          {custom && (
            <input type="number" className="chip-input" aria-label="Minutos" min="1" max={MAX_MINUTES} value={value} onChange={(event) => setValue(event.target.value)} />
          )}
          {!valid && <p role="alert" className="modal-error">Informe de 1 a {MAX_MINUTES} minutos.</p>}
        </div>
        <p className="modal-note">A duração fica salva só neste navegador. A sessão começa agora e vai para o histórico.</p>
        <div className="modal-actions">
          <button type="button" className="dash-btn" onClick={onClose}>Cancelar</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!valid || busy}>Iniciar Foco</button>
        </div>
      </form>
    </Modal>
  )
}
