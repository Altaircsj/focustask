import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { catalogService, focusSessionService } from '../focusSessionService'
import '../focus-sessions.css'

export default function NewFocusSessionPage() {
  const navigate = useNavigate()
  const [catalog, setCatalog] = useState({ status: 'loading', projects: [], tasks: [] })
  const [projectId, setProjectId] = useState('')
  const [taskId, setTaskId] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const busy = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    Promise.all([catalogService.projects(signal), catalogService.tasks(signal)]).then(
      ([projects, tasks]) => { if (!signal.aborted) setCatalog({ status: 'ready', projects, tasks }) },
      (error) => { if (!signal.aborted) setCatalog({ status: 'error', projects: [], tasks: [], message: error.message }) },
    )
    return () => controller.abort()
  }, [])

  const projectTasks = catalog.tasks.filter((task) => String(task.projectId) === projectId)

  function handleProjectChange(event) {
    setProjectId(event.target.value)
    setTaskId('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (busy.current) return
    busy.current = true
    setSubmitting(true)
    setMessage('')
    try {
      await focusSessionService.create(taskId ? Number(taskId) : null)
      navigate('/focus-sessions')
    } catch (error) {
      setMessage(error.message)
      busy.current = false
      setSubmitting(false)
    }
  }

  const taskPlaceholder = !projectId ? 'Nenhuma (selecione um projeto primeiro)' : projectTasks.length === 0 ? 'Este projeto não tem tarefas' : 'Nenhuma'

  return (
    <main className="sessions-page">
      <form className="session-form-card" onSubmit={handleSubmit} aria-busy={submitting}>
        <h1>Nova Sessão de Foco</h1>
        <p className="session-form-subtitle">Inicie um cronômetro de foco imediato.</p>

        {catalog.status === 'error' && <p role="alert">Não foi possível carregar seus projetos e tarefas. Você ainda pode iniciar uma sessão avulsa. ({catalog.message})</p>}

        <div className="form-row">
          <label htmlFor="project">Projeto (opcional)</label>
          <select id="project" value={projectId} onChange={handleProjectChange} disabled={catalog.projects.length === 0}>
            <option value="">Nenhum</option>
            {catalog.projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="task">Tarefa (opcional)</label>
          <select id="task" value={taskId} onChange={(event) => setTaskId(event.target.value)} disabled={projectTasks.length === 0}>
            <option value="">{taskPlaceholder}</option>
            {projectTasks.map((task) => <option key={task.id} value={task.id}>{task.title}</option>)}
          </select>
        </div>

        <p className="session-form-note">A sessão começa agora e aparece no histórico, onde pode ser pausada ou concluída.</p>

        {message && <p role="alert">{message}</p>}

        <div className="session-form-actions">
          <Link to="/focus-sessions" className="fs-btn fs-btn-secondary">Cancelar</Link>
          <button type="submit" className="fs-btn fs-btn-primary" disabled={submitting}>{submitting ? 'Iniciando…' : 'Iniciar Foco'}</button>
        </div>
      </form>
    </main>
  )
}
