import { useState } from 'react'
import { activeSeconds, formatDuration, formatStartedAt } from '../../focusSessions/format'
import Modal from './Modal'

export default function CompletedSessionModal({ session, tasks, projects, busy, error, onSave, onClose }) {
  const task = tasks.find((item) => item.id === session.taskId)
  const [title, setTitle] = useState(session.title ?? '')
  const [mode, setMode] = useState(session.taskId != null ? 'Tarefa' : session.projectId != null ? 'Projeto' : 'Avulsa')
  const [projectId, setProjectId] = useState(String(task?.projectId ?? session.projectId ?? ''))
  const [taskId, setTaskId] = useState(String(session.taskId ?? ''))
  const projectTasks = tasks.filter((item) => String(item.projectId) === projectId)
  const valid = mode === 'Avulsa' || (Boolean(projectId) && (mode === 'Projeto' || Boolean(taskId)))

  function save(selectedMode) {
    let association = {}
    if (selectedMode === 'Tarefa') association = { taskId: Number(taskId) }
    if (selectedMode === 'Projeto') association = { projectId: Number(projectId) }
    onSave({ title: title.trim(), association })
  }

  return (
    <Modal title="Sessão concluída" subtitle="Ótimo trabalho! Revise os dados antes de salvar." onClose={onClose}>
      <form onSubmit={(event) => { event.preventDefault(); if (valid && !busy) save(mode) }} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="review-title">Título</label>
            <input id="review-title" className="chip-input" maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <dl className="modal-summary"><div><dt>Duração e horário</dt><dd>{formatDuration(activeSeconds(session))} · {formatStartedAt(session.startedAt)}</dd></div></dl>
          <div className="form-field">
            <span id="review-mode" className="form-label">Como deseja registrar?</span>
            <div className="chip-row" role="group" aria-labelledby="review-mode">
              {['Tarefa', 'Projeto', 'Avulsa'].map((item) => <button type="button" key={item} className="chip" aria-pressed={mode === item} onClick={() => setMode(item)}>{item}</button>)}
            </div>
          </div>
          {mode !== 'Avulsa' && <div className="form-field">
            <label htmlFor="review-project">Projeto</label>
            <select id="review-project" value={projectId} disabled={!projects.length} onChange={(e) => { setProjectId(e.target.value); setTaskId('') }}>
              <option value="">{projects.length ? 'Selecione um projeto' : 'Nenhum projeto disponível'}</option>
              {projects.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>}
          {mode === 'Tarefa' && <div className="form-field">
            <label htmlFor="review-task">Tarefa</label>
            <select id="review-task" value={taskId} disabled={!projectTasks.length} onChange={(e) => setTaskId(e.target.value)}>
              <option value="">{!projectId ? 'Selecione um projeto primeiro' : projectTasks.length ? 'Selecione uma tarefa' : 'Este projeto não tem tarefas'}</option>
              {projectTasks.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
            </select>
          </div>}
        </fieldset>
        {error && <p role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="dash-btn" disabled={busy} onClick={() => save('Avulsa')}>Manter avulsa</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!valid || busy}>{busy ? 'Salvando…' : 'Salvar sessão'}</button>
        </div>
      </form>
    </Modal>
  )
}
