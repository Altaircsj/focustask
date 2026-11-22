import { useState } from 'react'
import { Link } from 'react-router'
import { CircleCheck, Clock, FolderPlus, Plus } from 'lucide-react'
import ActionMenu from '../../../components/ActionMenu'
import ConfirmModal from '../../catalog/components/ConfirmModal'
import { useAction, useCatalog } from '../../catalog/useCatalog'
import { formatFocusedTime } from '../../dashboard/summary'
import ProjectFormModal from '../components/ProjectFormModal'
import { DEFAULT_COLOR } from '../appearance'
import { projectService } from '../projectService'
import { projectStats } from '../summary'
import '../../dashboard/dashboard.css'
import '../projects.css'

const MENU = [{ id: 'edit', label: 'Editar' }, { id: 'delete', label: 'Excluir' }]

export default function ProjectsPage() {
  const { data, setData, retry } = useCatalog()
  const [modal, setModal] = useState(null)
  const { busy, error, run, reset } = useAction()

  const ready = data.status === 'ready'
  const closeModal = () => { if (!busy) { setModal(null); reset() } }

  function saveProject(details) {
    if (modal.project) {
      run(() => projectService.update(modal.project.id, details), (updated) => {
        setData((state) => ({ ...state, projects: state.projects.map((item) => (item.id === updated.id ? updated : item)) }))
        setModal(null)
      })
    } else {
      run(() => projectService.create(details), (created) => {
        setData((state) => ({ ...state, projects: [...state.projects, created] }))
        setModal(null)
      })
    }
  }

  function deleteProject() {
    const { id } = modal.project
    run(() => projectService.remove(id), () => {
      setData((state) => ({ ...state, projects: state.projects.filter((item) => item.id !== id), tasks: state.tasks.filter((task) => task.projectId !== id) }))
      setModal(null)
    })
  }

  return (
    <main className="dashboard-page projects-page">
      <header className="page-header">
        <div>
          <h1>Projetos</h1>
          <p>Gerencie seus espaços de trabalho e acompanhe o progresso.</p>
        </div>
        <button type="button" className="dash-btn dash-btn-primary" disabled={!ready} onClick={() => { reset(); setModal({ type: 'form' }) }}><Plus size={16} aria-hidden="true" />Novo Projeto</button>
      </header>

      {data.status === 'loading' && <p role="status">Carregando projetos…</p>}
      {data.status === 'error' && (
        <div>
          <p role="alert">{data.message}</p>
          <button type="button" className="dash-btn" onClick={retry}>Tentar novamente</button>
        </div>
      )}

      {ready && (
        <ul className="projects-grid">
          {data.projects.map((project) => {
            const stats = projectStats(project, data.tasks, data.sessions)
            return (
              <li key={project.id} className="project-card project-with-color" style={{ '--project-color': project.color ?? DEFAULT_COLOR }}>
                <div className="project-card-header">
                  <Link to={`/projects/${project.id}`} className="project-card-title">{project.name}</Link>
                  <ActionMenu label={`Ações do projeto ${project.name}`} actions={MENU} onSelect={(action) => { reset(); setModal({ type: action === 'edit' ? 'form' : 'delete', project }) }} />
                </div>
                <div className="progress-area">
                  <div className="progress-label"><span>Progresso</span><span>{stats.percent}%</span></div>
                  <div className="progress" role="progressbar" aria-label={`Progresso de ${project.name}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={stats.percent}><span style={{ width: `${stats.percent}%` }} /></div>
                </div>
                <div className="meta-stats">
                  <span><CircleCheck size={14} aria-hidden="true" />{stats.done}/{stats.total} tarefas</span>
                  <span><Clock size={14} aria-hidden="true" />{formatFocusedTime(stats.focusedSeconds)}</span>
                </div>
              </li>
            )
          })}
          <li className="project-card project-card-empty">
            <button type="button" onClick={() => { reset(); setModal({ type: 'form' }) }}>
              <FolderPlus size={32} aria-hidden="true" />
              <strong>Criar Novo Projeto</strong>
              <span>Organize suas tarefas em categorias dedicadas.</span>
            </button>
          </li>
        </ul>
      )}

      {ready && modal?.type === 'form' && <ProjectFormModal project={modal.project} busy={busy} error={error} onSave={saveProject} onClose={closeModal} />}
      {ready && modal?.type === 'delete' && (
        <ConfirmModal title="Excluir este projeto?" subtitle="O projeto será removido e esta ação não pode ser desfeita." confirmLabel="Excluir projeto" busy={busy} error={error} onConfirm={deleteProject} onClose={closeModal} />
      )}
    </main>
  )
}
