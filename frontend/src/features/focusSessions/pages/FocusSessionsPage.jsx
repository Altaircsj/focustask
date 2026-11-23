import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router'
import { Menu, Pause, Play, SquareCheck } from 'lucide-react'
import { catalogService, focusSessionService } from '../focusSessionService'
import { activeSeconds, formatDuration, formatStartedAt } from '../format'
import { applyActivation, listTitle, sessionLink } from '../sessionInfo'
import { DEFAULT_MINUTES } from '../../dashboard/summary'
import { loadMinutes } from '../../dashboard/dashboardService'
import CompletedSessionModal from '../../dashboard/components/CompletedSessionModal'
import EditFocusModal from '../../dashboard/components/EditFocusModal'
import '../../dashboard/dashboard.css'
import '../focus-sessions.css'

const FILTERS = [
  { id: 'all', label: 'Todas' },
  { id: 'standalone', label: 'Avulsa' },
  { id: 'project', label: 'Projeto' },
  { id: 'task', label: 'Tarefa' },
]
const STATUS_LABEL = { RUNNING: 'Em andamento', PAUSED: 'Pausada' }

const MENU_ACTIONS = [{ id: 'edit', label: 'Editar' }, { id: 'delete', label: 'Excluir' }]

// Controles diretos da linha: pausar/retomar e concluir; sessões concluídas só mostram o estado.
function rowControls(session) {
  if (session.status === 'RUNNING') return [{ status: 'PAUSED', label: 'Pausar', icon: Pause }, { status: 'COMPLETED', label: 'Concluir', icon: SquareCheck }]
  if (session.status === 'PAUSED') return [{ status: 'RUNNING', label: 'Retomar', icon: Play }, { status: 'COMPLETED', label: 'Concluir', icon: SquareCheck }]
  return []
}

export default function FocusSessionsPage() {
  const [data, setData] = useState({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [filter, setFilter] = useState('all')
  const [menuId, setMenuId] = useState(null)
  const [actionError, setActionError] = useState('')
  const [now, setNow] = useState(() => Date.now())
  const [editing, setEditing] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const working = useRef(false)
  const [saving, setSaving] = useState(false)
  const [editError, setEditError] = useState('')

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

  // Uma ação por vez: cliques repetidos são ignorados e as respostas não se ultrapassam.
  async function guarded(session, request, onDone) {
    if (working.current) return
    working.current = true
    setBusyId(session.id)
    setActionError('')
    try {
      onDone(await request())
    } catch (error) {
      setActionError(error.message)
      // O estado local pode estar defasado (sessão concluída ou excluída em outra aba): relê do servidor.
      try {
        const fresh = await focusSessionService.list()
        if (Array.isArray(fresh)) setData((current) => ({ ...current, sessions: fresh }))
      } catch { /* mantém o que já está na tela */ }
    } finally {
      working.current = false
      setBusyId(null)
    }
  }

  // A resposta traz a sessão alterada; ao retomar, a que rodava passa a pausada na própria lista.
  function changeStatus(session, status) {
    return guarded(session, () => focusSessionService.setStatus(session.id, status),
      (updated) => setData((current) => ({ ...current, sessions: applyActivation(current.sessions, updated) })))
  }

  function runAction(session, action) {
    setMenuId(null)
    setActionError('')
    if (action === 'edit') {
      setEditError('')
      setEditing(session)
      return undefined
    }
    if (!window.confirm('Excluir esta sessão? Essa ação não pode ser desfeita.')) return undefined
    return guarded(session, () => focusSessionService.remove(session.id),
      () => setData((current) => ({ ...current, sessions: current.sessions.filter((item) => item.id !== session.id) })))
  }

  async function saveEdit(details) {
    if (saving) return
    setSaving(true)
    setEditError('')
    try {
      const updated = await focusSessionService.update(editing.id, details)
      setData((current) => ({ ...current, sessions: current.sessions.map((item) => (item.id === updated.id ? updated : item)) }))
      setEditing(null)
    } catch (error) {
      setEditError(error.message)
    } finally {
      setSaving(false)
    }
  }

  const closeEdit = () => { if (!saving) setEditing(null) }

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
                    <td><Link className="session-link" to={sessionLink(session.id)}>{listTitle(session, task, project)}</Link></td>
                    <td className={task ? undefined : 'muted'}>{task?.title ?? '—'}</td>
                    <td className={`col-project${project ? '' : ' muted'}`}>{project?.name ?? '—'}</td>
                    <td>
                      <span className={`badge badge-${type}`}>
                        {FILTERS.find((item) => item.id === type).label}
                      </span>
                    </td>
                    <td className="col-actions">
                      <div className="row-actions">
                      {session.status === 'COMPLETED' && <span className="row-done">Concluída</span>}
                      {rowControls(session).map(({ status, label, icon: Icon }) => (
                        <button key={status} type="button" className="row-control" disabled={busyId !== null}
                          aria-label={`${label} sessão: ${listTitle(session, task, project)}`} onClick={() => changeStatus(session, status)}>
                          <Icon size={12} aria-hidden="true" />{label}
                        </button>
                      ))}
                      <div className="row-menu">
                        <button type="button" className="row-menu-trigger" aria-label="Ações da sessão" aria-haspopup="true"
                          aria-expanded={menuId === session.id} onClick={() => setMenuId(menuId === session.id ? null : session.id)}>
                          <Menu size={16} aria-hidden="true" />
                        </button>
                        {menuId === session.id && (
                          <ul className="row-menu-list">
                            {MENU_ACTIONS.map((action) => (
                              <li key={action.id}><button type="button" disabled={busyId !== null} onClick={() => runAction(session, action.id)}>{action.label}</button></li>
                            ))}
                          </ul>
                        )}
                      </div>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
      {data.status === 'ready' && editing && (
        <div className="dashboard-page modal-host">
          {editing.status === 'COMPLETED' ? (
            <CompletedSessionModal key={editing.id} session={editing} projects={data.projects} tasks={data.tasks} busy={saving} error={editError} onSave={saveEdit} onClose={closeEdit}
              heading="Editar Sessão de Foco" subtitle="Atualize o título e a associação da sessão." />
          ) : (
            <EditFocusModal session={editing} projects={data.projects} tasks={data.tasks} minutes={loadMinutes(DEFAULT_MINUTES)} busy={saving} error={editError} onSave={saveEdit} onClose={closeEdit} />
          )}
        </div>
      )}
    </main>
  )
}
