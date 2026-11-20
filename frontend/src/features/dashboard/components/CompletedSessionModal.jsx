import { Link } from 'react-router'
import { activeSeconds, formatStartedAt } from '../../focusSessions/format'
import Modal from './Modal'

export default function CompletedSessionModal({ session, task, project, onClose }) {
  const minutes = Math.max(1, Math.round(activeSeconds(session) / 60))
  const linked = task ? `${task.title}${project ? ` · ${project.name}` : ''}` : 'Sessão avulsa (sem tarefa)'

  return (
    <Modal title="Sessão concluída" subtitle="Ótimo trabalho! Sua sessão foi registrada no histórico." onClose={onClose}>
      <dl className="modal-summary">
        <div><dt>Duração e horário</dt><dd>{minutes} {minutes === 1 ? 'minuto' : 'minutos'} • {formatStartedAt(session.startedAt)}</dd></div>
        <div><dt>Associada a</dt><dd>{linked}</dd></div>
      </dl>
      <div className="modal-actions">
        <Link to="/focus-sessions" className="dash-btn" onClick={onClose}>Ver histórico</Link>
        <button type="button" className="dash-btn dash-btn-primary" onClick={onClose}>Concluir</button>
      </div>
    </Modal>
  )
}
