import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { AlarmClock, Award, CircleCheck, Ellipsis, ListTodo, Pause, Play, RefreshCw, SquareCheck, Zap } from 'lucide-react'
import { useAuth } from '../../auth/useAuth'
import { catalogService, focusSessionService } from '../../focusSessions/focusSessionService'
import { dashboardService, loadMinutes, saveMinutes } from '../dashboardService'
import CompletedSessionModal from '../components/CompletedSessionModal'
import NewFocusModal from '../components/NewFocusModal'
import {
  DEFAULT_MINUTES, currentSession, dailyTasks, firstName, focusNumber, formatClock, formatFocusedTime, greeting, remainingSeconds, summarize,
} from '../summary'
import '../dashboard.css'

const PRIORITY = { HIGH: 'Alta', MEDIUM: 'Média', LOW: 'Baixa' }

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const [data, setData] = useState({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [now, setNow] = useState(() => Date.now())
  const [busy, setBusy] = useState(false)
  const [minutes, setMinutes] = useState(() => loadMinutes(DEFAULT_MINUTES))
  const [modal, setModal] = useState(null)
  const [actionError, setActionError] = useState('')
  const working = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    Promise.all([focusSessionService.list(signal), catalogService.tasks(signal), catalogService.projects(signal)]).then(
      (lists) => {
        if (signal.aborted) return
        if (!lists.every(Array.isArray)) throw new Error('A API retornou uma resposta inválida.')
        const [sessions, tasks, projects] = lists
        setData({ status: 'ready', sessions, tasks, projects })
      },
    ).catch((error) => { if (!signal.aborted) setData({ status: 'error', message: error.message }) })
    return () => controller.abort()
  }, [attempt])

  const ready = data.status === 'ready'
  const current = ready ? currentSession(data.sessions) : null
  const remaining = current ? remainingSeconds(current, now, minutes * 60) : minutes * 60
  const running = current?.status === 'RUNNING'

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

  function saveSession(session) {
    setNow(Date.now())
    setData((state) => {
      const known = state.sessions.some((item) => item.id === session.id)
      return { ...state, sessions: known ? state.sessions.map((item) => (item.id === session.id ? session : item)) : [...state.sessions, session] }
    })
  }

  function finishSession(session) {
    saveSession(session)
    setModal({ type: 'done', session })
  }

  // O backend não conhece a duração do ciclo: ao zerar, a sessão é concluída aqui.
  useEffect(() => {
    if (!running) return undefined
    const timer = setInterval(() => {
      const tick = Date.now()
      setNow(tick)
      if (remainingSeconds(current, tick, minutes * 60) === 0) perform(() => focusSessionService.setStatus(current.id, 'COMPLETED'), finishSession)
    }, 1000)
    return () => clearInterval(timer)
  }, [running, current, minutes])

  function toggleTimer() {
    if (!current) return perform(() => focusSessionService.create(null), saveSession)
    return perform(() => focusSessionService.setStatus(current.id, running ? 'PAUSED' : 'RUNNING'), saveSession)
  }

  function startFocus({ taskId, minutes: chosen }) {
    setMinutes(chosen)
    saveMinutes(chosen)
    perform(() => focusSessionService.create(taskId), (session) => {
      saveSession(session)
      setModal(null)
    })
  }

  function discardSession() {
    if (!window.confirm('Descartar a sessão em andamento? Ela será excluída do histórico.')) return
    perform(() => focusSessionService.remove(current.id), () => setData((state) => ({ ...state, sessions: state.sessions.filter((item) => item.id !== current.id) })))
  }

  function completeTask(task) {
    perform(() => dashboardService.completeTask(task.id), (updated) => setData((state) => ({ ...state, tasks: state.tasks.map((item) => (item.id === updated.id ? updated : item)) })))
  }

  function retry() {
    setData({ status: 'loading' })
    setAttempt((value) => value + 1)
  }

  const summary = ready ? summarize(data, now) : null
  const projects = ready ? new Map(data.projects.map((project) => [project.id, project])) : null
  const tasks = ready ? dailyTasks(data.tasks, now) : []
  const stats = summary && [
    { label: 'Tempo Focado', value: formatFocusedTime(summary.focusedSeconds), icon: AlarmClock },
    { label: 'Sessões Concluídas', value: summary.completedSessions, icon: Award },
    { label: 'Tarefas Concluídas', value: summary.doneTasks, icon: CircleCheck },
    { label: 'Tarefas Pendentes', value: summary.pendingTasks, icon: ListTodo },
  ]
  const timerLabel = !current ? 'Iniciar foco' : running ? 'Pausar' : 'Retomar'

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <h1>{greeting(new Date(now))}, {firstName(user.name)}</h1>
          <p>Acompanhe seu foco e suas tarefas de hoje.</p>
        </div>
        <button type="button" className="dash-btn" onClick={() => logout()}>Sair</button>
      </header>

      {data.status === 'loading' && <p role="status">Carregando painel…</p>}
      {data.status === 'error' && (
        <div>
          <p role="alert">{data.message}</p>
          <button type="button" className="dash-btn" onClick={retry}>Tentar novamente</button>
        </div>
      )}

      {ready && (
        <>
          <dl className="stats-grid">
            {stats.map(({ label, value, icon: Icon }) => (
              <div key={label} className="stat-card">
                <span className="stat-icon" aria-hidden="true"><Icon size={20} /></span>
                <div>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </div>
              </div>
            ))}
          </dl>

          <section className="timer-card" aria-label="Cronômetro de foco" aria-busy={busy}>
            <div className="timer-ring">
              <p className="timer-clock" role="timer">{formatClock(remaining)}</p>
              <span className="timer-badge">Foco #{focusNumber(data.sessions, current, now)}{current?.status === 'PAUSED' ? ' · Pausado' : ''}</span>
            </div>
            <div className="timer-controls">
              <button type="button" className="timer-round" aria-label="Descartar sessão" disabled={!current || busy} onClick={discardSession}>
                <RefreshCw size={18} aria-hidden="true" />
              </button>
              <button type="button" className="timer-play" aria-label={timerLabel} disabled={busy} onClick={toggleTimer}>
                {running ? <Pause size={24} aria-hidden="true" /> : <Play size={24} aria-hidden="true" />}
              </button>
              <Link to="/focus-sessions" className="timer-round" aria-label="Histórico de sessões"><Ellipsis size={18} aria-hidden="true" /></Link>
            </div>
            <div className="timer-actions">
              <button type="button" className="dash-btn" disabled={Boolean(current) || busy} onClick={toggleTimer}>
                <Zap size={16} aria-hidden="true" />Foco avulso
              </button>
              <button type="button" className="dash-btn dash-btn-primary" disabled={Boolean(current) || busy} onClick={() => setModal({ type: 'new' })}>
                <SquareCheck size={16} aria-hidden="true" />Foco com tarefa
              </button>
            </div>
            {actionError && <p role="alert">{actionError}</p>}
          </section>

          <section className="tasks-section" aria-labelledby="daily-tasks">
            <div className="tasks-heading">
              <h2 id="daily-tasks">Tarefas do dia</h2>
              <Link to="/tasks">Ver todas as tarefas</Link>
            </div>
            {tasks.length === 0 ? (
              <p className="tasks-empty">Nenhuma tarefa pendente para hoje.</p>
            ) : (
              <ul className="tasks-list">
                {tasks.map((task) => (
                  <li key={task.id} className="task-row">
                    <input type="checkbox" className="task-check" checked={false} disabled={busy} aria-label={`Concluir tarefa: ${task.title}`} onChange={() => completeTask(task)} />
                    <span className="task-title">{task.title}</span>
                    <span className="task-meta">
                      <span className="project-tag" data-tone={task.projectId % 4}>{projects.get(task.projectId)?.name ?? 'Geral'}</span>
                      {PRIORITY[task.priority] && (
                        <span className="priority"><span className="priority-dot" data-priority={task.priority} aria-hidden="true" />{PRIORITY[task.priority]}</span>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {ready && modal?.type === 'new' && (
        <NewFocusModal projects={data.projects} tasks={data.tasks} minutes={minutes} busy={busy} onStart={startFocus} onClose={() => setModal(null)} />
      )}
      {ready && modal?.type === 'done' && (() => {
        const task = data.tasks.find((item) => item.id === modal.session.taskId)
        return <CompletedSessionModal session={modal.session} task={task} project={task && projects.get(task.projectId)} onClose={() => setModal(null)} />
      })()}
    </main>
  )
}
