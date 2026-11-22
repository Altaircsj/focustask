import { useEffect, useRef, useState } from 'react'
import ProjectBadge from '../../projects/components/ProjectBadge'
import { Link, useLocation, useNavigate } from 'react-router'
import { AlarmClock, Award, CircleCheck, ListTodo, Pause, Play, RefreshCw, SquareCheck, Zap } from 'lucide-react'
import { useAuth } from '../../auth/useAuth'
import { catalogService, focusSessionService } from '../../focusSessions/focusSessionService'
import { dashboardService, loadMinutes, saveMinutes } from '../dashboardService'
import CompletedSessionModal from '../components/CompletedSessionModal'
import NewFocusModal from '../components/NewFocusModal'
import EditFocusModal from '../components/EditFocusModal'
import TimerMenu from '../components/TimerMenu'
import Modal from '../components/Modal'
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
  const autoFinishAttempt = useRef(null)
  const location = useLocation()
  const navigate = useNavigate()
  const enterNewFocus = useRef(location.state?.newFocus)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    Promise.all([focusSessionService.list(signal), catalogService.tasks(signal), catalogService.projects(signal)]).then(
      (lists) => {
        if (signal.aborted) return
        if (!lists.every(Array.isArray)) throw new Error('A API retornou uma resposta inválida.')
        const [sessions, tasks, projects] = lists
        setData({ status: 'ready', sessions, tasks, projects })
        if (enterNewFocus.current) {
          enterNewFocus.current = false
          if (currentSession(sessions)) setActionError('Você já tem uma sessão em andamento.')
          else setModal({ type: 'new' })
        }
      },
    ).catch((error) => { if (!signal.aborted) setData({ status: 'error', message: error.message }) })
    return () => controller.abort()
  }, [attempt])

  const ready = data.status === 'ready'
  const current = ready ? currentSession(data.sessions) : null
  const planned = current?.plannedMinutes ?? minutes
  const remaining = current ? remainingSeconds(current, now, planned * 60) : minutes * 60
  const running = current?.status === 'RUNNING'

  useEffect(() => {
    if (ready && location.state?.newFocus) navigate('/dashboard', { replace: true, state: null })
  }, [ready, location.state, navigate])

  function openModal(next) {
    setActionError('')
    setModal(next)
  }

  function closeModal() {
    if (!working.current) openModal(null)
  }

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

  // Uma falha de conclusão permite nova tentativa pelo botão Finalizar sessão.
  useEffect(() => {
    if (!running) return undefined
    const timer = setInterval(() => {
      const tick = Date.now()
      setNow(tick)
      if (remainingSeconds(current, tick, planned * 60) > 0) {
        autoFinishAttempt.current = null
      } else if (!working.current && autoFinishAttempt.current !== current.id) {
        autoFinishAttempt.current = current.id
        perform(() => focusSessionService.setStatus(current.id, 'COMPLETED'), finishSession)
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [running, current, planned])

  function toggleTimer() {
    if (!current) return startFocus({ taskId: null, plannedMinutes: minutes })
    return perform(() => focusSessionService.setStatus(current.id, running ? 'PAUSED' : 'RUNNING'), saveSession)
  }

  function rememberMinutes(value) {
    setMinutes(value)
    saveMinutes(value)
  }

  function startFocus(details) {
    perform(() => focusSessionService.create(details), (session) => {
      rememberMinutes(details.plannedMinutes)
      saveSession(session)
      setModal(null)
    })
  }

  function finishNow() {
    perform(() => focusSessionService.setStatus(current.id, 'COMPLETED'), finishSession)
  }

  function discardSession() {
    const { id } = modal.session
    perform(async () => {
      try {
        await focusSessionService.remove(id)
      } catch (error) {
        // A previous DELETE may have succeeded before its response was lost.
        if (error.status !== 404) throw error
      }
    }, () => {
      rememberMinutes(planned)
      setData((state) => ({ ...state, sessions: state.sessions.filter((item) => item.id !== id) }))
      setModal(null)
    })
  }

  function updateSession(details) {
    const { id } = modal.session
    perform(async () => {
      try {
        return await focusSessionService.update(id, details)
      } catch (error) {
        if (modal.type === 'edit' && error.status === 422) {
          const latest = await focusSessionService.get(id)
          if (latest.status === 'COMPLETED') {
            finishSession(latest)
            throw new Error('Esta sessão já terminou. Revise os dados da conclusão.')
          }
        }
        throw error
      }
    }, (updated) => {
      if (details.plannedMinutes != null) rememberMinutes(updated.plannedMinutes)
      saveSession(updated)
      setModal(null)
    })
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
                <button type="button" className="timer-round" aria-label="Descartar sessão" disabled={!current || busy} onClick={() => openModal({ type: 'discard', session: current })}>
                <RefreshCw size={18} aria-hidden="true" />
              </button>
              <button type="button" className="timer-play" aria-label={timerLabel} disabled={busy} onClick={toggleTimer}>
                {running ? <Pause size={24} aria-hidden="true" /> : <Play size={24} aria-hidden="true" />}
              </button>
              <TimerMenu current={current} onEdit={(session) => openModal({ type: 'edit', session })} />
            </div>
            <div className="timer-actions">
              <button type="button" className="dash-btn" disabled={Boolean(current) || busy} onClick={toggleTimer}>
                <Zap size={16} aria-hidden="true" />Foco avulso
              </button>
              {current && (
                <button type="button" className="dash-btn" disabled={busy} onClick={finishNow}>
                  <CircleCheck size={16} aria-hidden="true" />Finalizar sessão
                </button>
              )}
              <button type="button" className="dash-btn dash-btn-primary" disabled={Boolean(current) || busy} onClick={() => openModal({ type: 'new' })}>
                <SquareCheck size={16} aria-hidden="true" />Foco com tarefa
              </button>
            </div>
            {!modal && actionError && <p role="alert">{actionError}</p>}
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
                    <Link to={`/tasks/${task.id}`} className="task-title">{task.title}</Link>
                    <span className="task-meta">
                      <ProjectBadge project={projects.get(task.projectId)} />
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
        <NewFocusModal projects={data.projects} tasks={data.tasks} minutes={minutes} busy={busy} error={actionError} onStart={startFocus} onClose={closeModal} />
      )}
      {ready && modal?.type === 'edit' && <EditFocusModal session={modal.session} projects={data.projects} tasks={data.tasks}
        minutes={minutes} busy={busy} error={actionError} onSave={updateSession} onClose={closeModal} />}
      {ready && modal?.type === 'done' && <CompletedSessionModal key={modal.session.id} session={modal.session} projects={data.projects}
        tasks={data.tasks} busy={busy} error={actionError} onSave={updateSession} onClose={closeModal} />}
      {modal?.type === 'discard' && (
        <Modal title="Descartar esta sessão de foco?" subtitle="Esta sessão será excluída do histórico." onClose={closeModal}>
          {actionError && <p role="alert">{actionError}</p>}
          <div className="modal-actions">
            <button type="button" className="dash-btn" disabled={busy} onClick={closeModal}>Manter sessão</button>
            <button type="button" className="dash-btn dash-btn-primary" disabled={busy} onClick={discardSession}>
              {busy ? 'Descartando…' : 'Descartar sessão'}
            </button>
          </div>
        </Modal>
      )}
    </main>
  )
}
