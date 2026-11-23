import { useEffect, useRef, useState } from 'react'
import ProjectBadge from '../../projects/components/ProjectBadge'
import { Link, useNavigate, useParams } from 'react-router'
import { Pencil, Play, Trash2 } from 'lucide-react'
import { catalogService, focusSessionService } from '../../focusSessions/focusSessionService'
import { activeSeconds, formatDuration, formatStartedAt } from '../../focusSessions/format'
import { sessionLink } from '../../focusSessions/sessionInfo'
import { DEFAULT_MINUTES, formatClock } from '../../dashboard/summary'
import { loadMinutes } from '../../dashboard/dashboardService'
import Modal from '../../dashboard/components/Modal'
import TaskFormModal from '../components/TaskFormModal'
import { formatDueDate, formatFocusedSeconds, formatMinutes, PRIORITY_LABEL, STATUS_LABEL } from '../format'
import { taskService } from '../taskService'
import '../../dashboard/dashboard.css'
import '../../focusSessions/focus-sessions.css'
import '../tasks.css'

export default function TaskDetailPage() {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [data, setData] = useState({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState('')
  const working = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    Promise.all([taskService.get(taskId, signal), catalogService.projects(signal), focusSessionService.list(signal)]).then(
      ([task, projects, sessions]) => {
        if (signal.aborted) return
        if (!task || !Array.isArray(projects) || !Array.isArray(sessions)) throw new Error('A API retornou uma resposta inválida.')
        setData({ status: 'ready', task, projects, sessions })
      },
    ).catch((error) => { if (!signal.aborted) setData({ status: 'error', code: error.status, message: error.message }) })
    return () => controller.abort()
  }, [taskId, attempt])

  async function perform(request, onDone) {
    if (working.current) return
    working.current = true
    setBusy(true)
    setActionError('')
    try {
      onDone(await request())
    } catch (error) {
      setActionError(error.message)
    } finally {
      working.current = false
      setBusy(false)
    }
  }

  const ready = data.status === 'ready'
  const task = ready ? data.task : null
  const project = ready ? data.projects.find((item) => item.id === task.projectId) : null
  const history = ready ? data.sessions.filter((session) => session.taskId === task.id).sort((a, b) => Date.parse(b.startedAt) - Date.parse(a.startedAt)) : []
  const focusedSeconds = history.reduce((total, session) => total + activeSeconds(session), 0)
  const minutes = loadMinutes(DEFAULT_MINUTES)
  const closeModal = () => { if (!working.current) { setModal(null); setActionError('') } }

  function startSession() {
    perform(() => focusSessionService.create({ taskId: task.id, plannedMinutes: minutes }), (session) => navigate(sessionLink(session.id)))
  }

  function saveTask(details) {
    perform(() => taskService.update(task.id, details), (updated) => {
      setData((state) => ({ ...state, task: updated }))
      setModal(null)
    })
  }

  function deleteTask() {
    perform(() => taskService.remove(task.id), () => navigate('/tasks'))
  }

  if (data.status === 'loading') return <main className="dashboard-page task-page"><p role="status">Carregando tarefa…</p></main>
  if (data.status === 'error') {
    return (
      <main className="dashboard-page task-page">
        <p role="alert">{data.code === 404 ? 'Tarefa não encontrada.' : data.message}</p>
        <div className="task-page-actions">
          {data.code !== 404 && <button type="button" className="dash-btn" onClick={() => { setData({ status: 'loading' }); setAttempt((value) => value + 1) }}>Tentar novamente</button>}
          <Link to="/tasks" className="dash-btn">Voltar às tarefas</Link>
        </div>
      </main>
    )
  }

  const cards = [
    { label: 'Projeto', value: <ProjectBadge project={project} /> },
    { label: 'Prioridade', value: PRIORITY_LABEL[task.priority] ? <span className="priority"><span className="priority-dot" data-priority={task.priority} aria-hidden="true" />{PRIORITY_LABEL[task.priority]}</span> : '—' },
    { label: 'Prazo de entrega', value: formatDueDate(task.dueDate) },
    { label: 'Tempo estimado', value: formatMinutes(task.estimatedMinutes, 'Sem estimativa') },
    { label: 'Tempo focado', value: formatFocusedSeconds(focusedSeconds) },
  ]

  return (
    <main className="dashboard-page task-page">
      <header className="task-header">
        <div>
          <nav className="breadcrumb" aria-label="Você está em"><Link to="/tasks">Tarefas</Link><span aria-hidden="true">/</span><span aria-current="page">Detalhes</span></nav>
          <h1>{task.title}</h1>
          <p>{STATUS_LABEL[task.status]}{task.description ? ` · ${task.description}` : ''}</p>
        </div>
        <div className="task-page-actions">
          <button type="button" className="dash-btn" onClick={() => setModal('edit')}><Pencil size={16} aria-hidden="true" />Editar</button>
          <button type="button" className="dash-btn dash-btn-danger" onClick={() => setModal('delete')}><Trash2 size={16} aria-hidden="true" />Excluir</button>
        </div>
      </header>

      <dl className="stats-grid task-facts">
        {cards.map(({ label, value }) => (
          <div key={label} className="stat-card">
            <div><dt>{label}</dt><dd>{value}</dd></div>
          </div>
        ))}
      </dl>

      <section className="timer-card task-timer" aria-label="Sessão desta tarefa" aria-busy={busy}>
        <div className="timer-ring">
          <p className="timer-clock">{formatClock(minutes * 60)}</p>
          <span className="timer-badge">Foco #{history.length + 1}</span>
        </div>
        <button type="button" className="timer-play" aria-label="Iniciar sessão para esta tarefa" disabled={busy} onClick={startSession}><Play size={24} aria-hidden="true" /></button>
        <button type="button" className="dash-btn dash-btn-primary" disabled={busy} onClick={startSession}>Iniciar sessão para esta tarefa</button>
        {actionError && !modal && <p role="alert">{actionError}</p>}
      </section>

      <section className="tasks-section" aria-labelledby="task-history">
        <h2 id="task-history">Histórico de Sessões</h2>
        {history.length === 0 ? (
          <p className="tasks-empty">Nenhuma sessão registrada para esta tarefa.</p>
        ) : (
          <table className="sessions-table">
            <thead><tr><th scope="col">Data &amp; Hora</th><th scope="col">Duração</th><th scope="col" className="col-type">Tipo</th></tr></thead>
            <tbody>
              {history.map((session) => (
                <tr key={session.id}>
                  <td>{formatStartedAt(session.startedAt)}</td>
                  <td>{formatDuration(activeSeconds(session))}{session.status !== 'COMPLETED' && <span className="session-status">{session.status === 'RUNNING' ? 'Em andamento' : 'Pausada'}</span>}</td>
                  <td><span className="badge badge-task">Tarefa</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {modal === 'edit' && <TaskFormModal task={task} projects={data.projects} busy={busy} error={actionError} onSave={saveTask} onClose={closeModal} />}
      {modal === 'delete' && (
        <Modal title="Excluir esta tarefa?" subtitle="A tarefa será removida e esta ação não pode ser desfeita." onClose={closeModal}>
          {actionError && <p role="alert">{actionError}</p>}
          <div className="modal-actions">
            <button type="button" className="dash-btn" disabled={busy} onClick={closeModal}>Cancelar</button>
            <button type="button" className="dash-btn dash-btn-danger" disabled={busy} onClick={deleteTask}>{busy ? 'Excluindo…' : 'Excluir tarefa'}</button>
          </div>
        </Modal>
      )}
    </main>
  )
}
