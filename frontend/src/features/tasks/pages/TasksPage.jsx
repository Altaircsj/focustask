import { useMemo, useState } from 'react'
import ProjectBadge from '../../projects/components/ProjectBadge'
import { Link } from 'react-router'
import { Plus } from 'lucide-react'
import ActionMenu from '../../../components/ActionMenu'
import ConfirmModal from '../../catalog/components/ConfirmModal'
import { useAction, useCatalog } from '../../catalog/useCatalog'
import { formatFocusedTime } from '../../dashboard/summary'
import { focusedByTask } from '../../projects/summary'
import TaskFormModal from '../components/TaskFormModal'
import { formatDueDate, PRIORITY_LABEL, STATUS_LABEL } from '../format'
import { createTask, taskService } from '../taskService'
import '../../dashboard/dashboard.css'
import '../../focusSessions/focus-sessions.css'
import '../tasks.css'

const FILTERS = [
  { id: 'ALL', label: 'Todas' },
  { id: 'TODO', label: 'Pendentes' },
  { id: 'IN_PROGRESS', label: 'Em Andamento' },
  { id: 'DONE', label: 'Concluídas' },
]
const MENU = [{ id: 'edit', label: 'Editar' }, { id: 'delete', label: 'Excluir' }]

export default function TasksPage() {
  const { data, setData, retry } = useCatalog()
  const [filter, setFilter] = useState('ALL')
  const [modal, setModal] = useState(null)
  const [notice, setNotice] = useState('')
  const { busy, error, run, reset } = useAction()

  const ready = data.status === 'ready'
  const projects = useMemo(() => (ready ? new Map(data.projects.map((project) => [project.id, project])) : null), [ready, data.projects])
  const focused = useMemo(() => (ready ? focusedByTask(data.sessions) : null), [ready, data.sessions])
  const rows = ready ? data.tasks.filter((task) => filter === 'ALL' || task.status === filter) : []
  const closeModal = () => { if (!busy) { setModal(null); reset() } }
  const open = (next) => { reset(); setNotice(''); setModal(next) }
  const replace = (task) => setData((state) => ({ ...state, tasks: state.tasks.some((item) => item.id === task.id) ? state.tasks.map((item) => (item.id === task.id ? task : item)) : [...state.tasks, task] }))

  function toggle(task) {
    run(() => taskService.update(task.id, { status: task.status === 'DONE' ? 'TODO' : 'DONE' }), replace)
  }

  function saveTask(details) {
    const request = modal.task ? () => taskService.update(modal.task.id, details).then((task) => ({ task })) : () => createTask(details)
    run(request, ({ task, warning }) => {
      replace(task)
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

  return (
    <main className="dashboard-page tasks-page">
      <header className="page-header">
        <div>
          <h1>Tarefas</h1>
          <p>Crie, gerencie e acompanhe a execução das suas tarefas diárias.</p>
        </div>
        <button type="button" className="dash-btn dash-btn-primary" disabled={!ready} onClick={() => open({ type: 'task' })}><Plus size={16} aria-hidden="true" />Nova Tarefa</button>
      </header>

      {data.status === 'loading' && <p role="status">Carregando tarefas…</p>}
      {data.status === 'error' && (
        <div>
          <p role="alert">{data.message}</p>
          <button type="button" className="dash-btn" onClick={retry}>Tentar novamente</button>
        </div>
      )}
      {notice && <p role="status">{notice}</p>}
      {ready && !modal && error && <p role="alert">{error}</p>}

      {ready && (
        <>
          <div className="filter-chips" role="group" aria-label="Filtrar por status">
            {FILTERS.map(({ id, label }) => (
              <button key={id} type="button" className="filter-chip" aria-pressed={filter === id} onClick={() => setFilter(id)}>{label}</button>
            ))}
          </div>

          {rows.length === 0 ? (
            <p className="tasks-empty">{data.tasks.length === 0 ? 'Você ainda não tem tarefas. Crie a primeira em "Nova Tarefa".' : 'Nenhuma tarefa neste filtro.'}</p>
          ) : (
            <table className="sessions-table tasks-table">
              <thead>
                <tr>
                  <th scope="col" className="col-name">Nome da Tarefa</th>
                  <th scope="col" className="col-project">Projeto</th>
                  <th scope="col" className="col-priority">Prioridade</th>
                  <th scope="col" className="col-status">Status</th>
                  <th scope="col" className="col-due">Prazo</th>
                  <th scope="col" className="col-focused">Focado</th>
                  <th scope="col" className="col-actions"><span className="visually-hidden">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((task) => (
                  <tr key={task.id}>
                    <td className="task-name-cell">
                      <input type="checkbox" className="task-check" checked={task.status === 'DONE'} disabled={busy} aria-label={`${task.status === 'DONE' ? 'Reabrir' : 'Concluir'} tarefa: ${task.title}`} onChange={() => toggle(task)} />
                      <Link to={`/tasks/${task.id}`}>{task.title}</Link>
                    </td>
                    <td><ProjectBadge project={projects.get(task.projectId)} /></td>
                    <td>{PRIORITY_LABEL[task.priority] ? <span className="priority"><span className="priority-dot" data-priority={task.priority} aria-hidden="true" />{PRIORITY_LABEL[task.priority]}</span> : '—'}</td>
                    <td><span className="status-badge" data-status={task.status}>{STATUS_LABEL[task.status]}</span></td>
                    <td>{formatDueDate(task.dueDate)}</td>
                    <td>{formatFocusedTime(focused.get(task.id) ?? 0)}</td>
                    <td className="col-actions">
                      <ActionMenu label={`Ações da tarefa ${task.title}`} actions={MENU} onSelect={(action) => open({ type: action === 'edit' ? 'task' : 'delete', task })} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {ready && modal?.type === 'task' && <TaskFormModal task={modal.task} projects={data.projects} busy={busy} error={error} onSave={saveTask} onClose={closeModal} />}
      {ready && modal?.type === 'delete' && (
        <ConfirmModal title="Excluir esta tarefa?" subtitle="A tarefa será removida e esta ação não pode ser desfeita." confirmLabel="Excluir tarefa" busy={busy} error={error} onConfirm={deleteTask} onClose={closeModal} />
      )}
    </main>
  )
}
