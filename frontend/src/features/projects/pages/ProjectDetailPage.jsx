import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Clock, Pencil, Plus, SquareCheck, Trash2 } from 'lucide-react'
import ActionMenu from '../../../components/ActionMenu'
import ConfirmModal from '../../catalog/components/ConfirmModal'
import { useAction, useCatalog } from '../../catalog/useCatalog'
import TaskFormModal from '../../tasks/components/TaskFormModal'
import { formatDueDate, formatFocusedSeconds, formatMinutes } from '../../tasks/format'
import { createTask, taskService } from '../../tasks/taskService'
import ProjectFormModal from '../components/ProjectFormModal'
import { projectService } from '../projectService'
import { focusedByTask, projectStats } from '../summary'
import '../../dashboard/dashboard.css'
import '../projects.css'

const MENU = [{ id: 'edit', label: 'Editar' }, { id: 'delete', label: 'Excluir' }]
const TASK_STATE = { TODO: 'Pendente', IN_PROGRESS: 'Em andamento', DONE: '✓ Concluída' }

function estimateCaption({ estimatedMinutes, estimatedCount, total }) {
  if (!estimatedCount) return 'Sem estimativas'
  const unit = estimatedMinutes < 60
    ? (estimatedMinutes === 1 ? 'estimado' : 'estimados')
    : (estimatedMinutes < 120 ? 'estimada' : 'estimadas')
  return `${formatMinutes(estimatedMinutes)} ${unit} em ${estimatedCount} de ${total} ${total === 1 ? 'tarefa' : 'tarefas'}`
}

export default function ProjectDetailPage() {
  const { projectId } = useParams()
  const navigate = useNavigate()
  const { data, setData, retry } = useCatalog((signal) => projectService.get(projectId, signal), projectId)
  const [modal, setModal] = useState(null)
  const [notice, setNotice] = useState('')
  const { busy, error, run, reset } = useAction()

  const ready = data.status === 'ready'
  const project = ready ? data.extra : null
  const tasks = ready ? data.tasks.filter((task) => task.projectId === project.id) : []
  const focused = ready ? focusedByTask(data.sessions) : null
  const closeModal = () => { if (!busy) { setModal(null); reset() } }
  const open = (next) => { reset(); setNotice(''); setModal(next) }

  function saveProject(details) {
    run(() => projectService.update(project.id, details), (updated) => {
      setData((state) => ({ ...state, extra: updated, projects: state.projects.map((item) => (item.id === updated.id ? updated : item)) }))
      setModal(null)
    })
  }

  function deleteProject() {
    run(() => projectService.remove(project.id), () => navigate('/projects'))
  }

  function saveTask(details) {
    const request = modal.task ? () => taskService.update(modal.task.id, details).then((task) => ({ task })) : () => createTask(details)
    run(request, ({ task, warning }) => {
      setData((state) => ({ ...state, tasks: state.tasks.some((item) => item.id === task.id) ? state.tasks.map((item) => (item.id === task.id ? task : item)) : [...state.tasks, task] }))
      setNotice(warning ?? '')
      setModal(null)
    })
  }

  function deleteTask() {
    const { id } = modal.task
    run(() => taskService.remove(id), () => {
      setData((state) => ({ ...state, tasks: state.tasks.filter((item) => item.id !== id) }))
      setModal(null)
    })
  }

  if (data.status === 'loading') return <main className="dashboard-page projects-page"><p role="status">Carregando projeto…</p></main>
  if (data.status === 'error') {
    return (
      <main className="dashboard-page projects-page">
        <p role="alert">{data.code === 404 ? 'Projeto não encontrado.' : data.message}</p>
        <div className="page-actions">
          {data.code !== 404 && <button type="button" className="dash-btn" onClick={retry}>Tentar novamente</button>}
          <Link to="/projects" className="dash-btn">Voltar aos projetos</Link>
        </div>
      </main>
    )
  }

  const stats = projectStats(project, tasks, data.sessions)
  return (
    <main className="dashboard-page projects-page">
      <header className="page-header">
        <div>
          <nav className="breadcrumb" aria-label="Você está em"><Link to="/projects">Projetos</Link><span aria-hidden="true">/</span><span aria-current="page">Detalhes</span></nav>
          <h1>Detalhes do Projeto - {project.name}</h1>
          <p>{project.description || 'Este projeto ainda não tem descrição.'}</p>
          {project.tags?.length > 0 && <ul className="project-chips detail-tags" aria-label="Tags do projeto">
            {project.tags.map((tag) => <li className="chip selected-tag" key={tag}>{tag}</li>)}
          </ul>}
        </div>
        <div className="page-actions">
          <button type="button" className="dash-btn dash-btn-primary" onClick={() => open({ type: 'task' })}><Plus size={16} aria-hidden="true" />Nova Tarefa</button>
          <button type="button" className="dash-btn" onClick={() => open({ type: 'project' })}><Pencil size={16} aria-hidden="true" />Editar Projeto</button>
          <button type="button" className="dash-btn dash-btn-danger" onClick={() => open({ type: 'delete-project' })}><Trash2 size={16} aria-hidden="true" />Excluir</button>
        </div>
      </header>

      {notice && <p role="status">{notice}</p>}

      <section className="project-summary" aria-labelledby="project-completion">
        <h2 id="project-completion">{stats.done} de {stats.total} {stats.total === 1 ? 'tarefa concluída' : 'tarefas concluídas'} · {stats.percent}%</h2>
        <div className="progress" role="progressbar" aria-label="Progresso do projeto" aria-valuemin={0} aria-valuemax={100} aria-valuenow={stats.percent}>
          <span style={{ width: `${stats.percent}%` }} />
        </div>
        <dl className="project-time-totals">
          <div><dt>Estimativa das tarefas</dt><dd>{estimateCaption(stats)}</dd></div>
          <div><dt>Tempo focado</dt><dd>{formatFocusedSeconds(stats.focusedSeconds)}</dd></div>
        </dl>
      </section>

      <ul className="projects-grid project-task-grid">
        {tasks.map((task) => (
          <li key={task.id} className="project-card">
            <div className="project-card-header">
              <Link to={`/tasks/${task.id}`} className="project-card-title">{task.title}</Link>
              <ActionMenu label={`Ações da tarefa ${task.title}`} actions={MENU} onSelect={(action) => open({ type: action === 'edit' ? 'task' : 'delete-task', task })} />
            </div>
            <div className="progress-area">
              <span className="task-state-label">{TASK_STATE[task.status]}</span>
              <div className="task-state-track" data-status={task.status} aria-hidden="true" />
            </div>
            <div className="meta-stats">
              <span><SquareCheck size={14} aria-hidden="true" />Prazo: {formatDueDate(task.dueDate)}</span>
              {task.estimatedMinutes != null && <span>Estimativa: {formatMinutes(task.estimatedMinutes)}</span>}
              <span><Clock size={14} aria-hidden="true" />Focado: {formatFocusedSeconds(focused.get(task.id) ?? 0)}</span>
            </div>
          </li>
        ))}
        <li className="project-card project-card-empty">
          <button type="button" onClick={() => open({ type: 'task' })}>
            <Plus size={32} aria-hidden="true" />
            <strong>Criar Nova Tarefa</strong>
            <span>Adicione uma tarefa a este projeto.</span>
          </button>
        </li>
      </ul>

      {modal?.type === 'project' && <ProjectFormModal project={project} busy={busy} error={error} onSave={saveProject} onClose={closeModal} />}
      {modal?.type === 'task' && <TaskFormModal task={modal.task} projects={data.projects} defaultProjectId={project.id} busy={busy} error={error} onSave={saveTask} onClose={closeModal} />}
      {modal?.type === 'delete-project' && (
        <ConfirmModal title="Excluir este projeto?" subtitle="O projeto será removido e esta ação não pode ser desfeita." confirmLabel="Excluir projeto" busy={busy} error={error} onConfirm={deleteProject} onClose={closeModal} />
      )}
      {modal?.type === 'delete-task' && (
        <ConfirmModal title="Excluir esta tarefa?" subtitle="A tarefa será removida e esta ação não pode ser desfeita." confirmLabel="Excluir tarefa" busy={busy} error={error} onConfirm={deleteTask} onClose={closeModal} />
      )}
    </main>
  )
}
