import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { Menu } from 'lucide-react'
import { catalogService, focusSessionService } from '../focusSessionService'
import { activeSeconds, formatDuration, formatStartedAt } from '../format'
import '../focus-sessions.css'

const FILTERS = [
  { id: 'all', label: 'Todas' },
  { id: 'standalone', label: 'Avulsa' },
  { id: 'task', label: 'Tarefa' },
  { id: 'project', label: 'Projeto' },
]
const STATUS_LABEL = { RUNNING: 'Em andamento', PAUSED: 'Pausada' }

function rowActions(session) {
  const actions = []
  if (session.status === 'RUNNING') actions.push({ id: 'PAUSED', label: 'Pausar' })
  if (session.status === 'PAUSED') actions.push({ id: 'RUNNING', label: 'Retomar' })
  if (session.status !== 'COMPLETED') actions.push({ id: 'COMPLETED', label: 'Concluir' })
  actions.push({ id: 'delete', label: 'Excluir' })
  return actions
}

export default function FocusSessionsPage() {
  const [data, setData] = useState({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [filter, setFilter] = useState('all')
  const [menuId, setMenuId] = useState(null)
  const [actionError, setActionError] = useState('')
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    Promise.all([focusSessionService.list(signal), catalogService.tasks(signal), catalogService.projects(signal)]).then(
      ([sessions, tasks, projects]) => { if (!signal.aborted) setData({ status: 'ready', sessions, tasks, projects }) },
      (error) => { if (!signal.aborted) setData({ status: 'error', message: error.message }) },
    )
    return () => controller.abort()
  }, [attempt])

  const sessions = data.sessions
  const hasRunning = sessions?.some((session) => session.status === 'RUNNING')
  useEffect(() => {
    if (!hasRunning) return undefined
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [hasRunning])

  useEffect(() => {
    if (menuId === null) return undefined
    const close = (event) => { if (event.type === 'keydown' ? event.key === 'Escape' : !event.target.closest('.row-menu')) setMenuId(null) }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', close)
    }
  }, [menuId])

  const rows = useMemo(() => {
    if (!sessions) return []
    const tasks = new Map(data.tasks.map((task) => [task.id, task]))
    const projects = new Map(data.projects.map((project) => [project.id, project]))
    return sessions
      .map((session) => {
        const task = session.taskId == null ? null : tasks.get(session.taskId) ?? null
        const type = session.taskId != null ? 'task' : session.projectId != null ? 'project' : 'standalone'
        const project = projects.get(task?.projectId ?? session.projectId)
        return { session, task, project, type }
      })
      .filter(({ type }) => filter === 'all' || filter === type)
      .sort((a, b) => Date.parse(b.session.startedAt) - Date.parse(a.session.startedAt))
  }, [data.projects, data.tasks, filter, sessions])

  function retry() {
    setData({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  async function runAction(session, action) {
    setMenuId(null)
    setActionError('')
    if (action === 'delete' && !window.confirm('Excluir esta sessão? Essa ação não pode ser desfeita.')) return
    try {
      if (action === 'delete') {
        await focusSessionService.remove(session.id)
        setData((current) => ({ ...current, sessions: current.sessions.filter((item) => item.id !== session.id) }))
      } else {
        const updated = await focusSessionService.setStatus(session.id, action)
        setData((current) => ({ ...current, sessions: current.sessions.map((item) => (item.id === updated.id ? updated : item)) }))
      }
    } catch (error) {
      setActionError(error.message)
    }
  }

  return (
    <main className="sessions-page">
      <header className="sessions-header">
        <div>
          <h1>Sessões de Foco</h1>
          <p>Consulte seu histórico de trabalho focado e acompanhe as sessões em andamento.</p>
        </div>
        <Link to="/focus-sessions/new" className="fs-btn fs-btn-primary">Nova sessão</Link>
      </header>

      {data.status === 'loading' && <p role="status">Carregando sessões…</p>}
      {data.status === 'error' && (
        <div>
          <p role="alert">{data.message}</p>
          <button type="button" className="fs-btn fs-btn-secondary" onClick={retry}>Tentar novamente</button>
        </div>
      )}

      {data.status === 'ready' && (
        <>
          <div className="filter-chips" role="group" aria-label="Filtrar por tipo">
            {FILTERS.map(({ id, label }) => (
              <button key={id} type="button" className="filter-chip" aria-pressed={filter === id} onClick={() => setFilter(id)}>{label}</button>
            ))}
          </div>
          {actionError && <p role="alert">{actionError}</p>}
          {rows.length === 0 ? (
            <p className="sessions-empty">
              {sessions.length === 0 ? 'Você ainda não tem sessões. ' : 'Nenhuma sessão neste filtro. '}
              <Link to="/focus-sessions/new">Iniciar uma sessão</Link>
            </p>
          ) : (
            <table className="sessions-table">
              <thead>
                <tr>
                  <th scope="col" className="col-date">Data &amp; Hora</th>
                  <th scope="col" className="col-duration">Duração</th>
                  <th scope="col">Título</th>
                  <th scope="col">Tarefa</th>
                  <th scope="col" className="col-project">Projeto</th>
                  <th scope="col" className="col-type">Tipo</th>
                  <th scope="col" className="col-actions"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {rows.map(({ session, task, project, type }) => (
                  <tr key={session.id}>
                    <td>{formatStartedAt(session.startedAt, new Date(now))}</td>
                    <td>
                      {formatDuration(activeSeconds(session, now))}
                      {session.status !== 'COMPLETED' && <span className="session-status">{STATUS_LABEL[session.status]}</span>}
                    </td>
                    <td>{session.title || '—'}</td>
                    <td className={task ? undefined : 'muted'}>{task?.title ?? '—'}</td>
                    <td className={`col-project${project ? '' : ' muted'}`}>{project?.name ?? '—'}</td>
                    <td>
                      <span className={`badge badge-${type}`}>
                        {FILTERS.find((item) => item.id === type).label}
                      </span>
                    </td>
                    <td className="col-actions">
                      <div className="row-menu">
                        <button type="button" className="row-menu-trigger" aria-label="Ações da sessão" aria-haspopup="true"
                          aria-expanded={menuId === session.id} onClick={() => setMenuId(menuId === session.id ? null : session.id)}>
                          <Menu size={16} aria-hidden="true" />
                        </button>
                        {menuId === session.id && (
                          <ul className="row-menu-list">
                            {rowActions(session).map((action) => (
                              <li key={action.id}><button type="button" onClick={() => runAction(session, action.id)}>{action.label}</button></li>
                            ))}
                          </ul>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </main>
  )
}
