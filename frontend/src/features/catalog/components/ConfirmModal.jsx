import Modal from '../../dashboard/components/Modal'

export default function ConfirmModal({ title, subtitle, confirmLabel, busy, error, onConfirm, onClose }) {
  return (
    <Modal title={title} subtitle={subtitle} onClose={onClose}>
      {error && <p role="alert">{error}</p>}
      <div className="modal-actions">
        <button type="button" className="dash-btn" disabled={busy} onClick={onClose}>Cancelar</button>
        <button type="button" className="dash-btn dash-btn-solid-danger" disabled={busy} onClick={onConfirm}>{busy ? 'Excluindo…' : confirmLabel}</button>
      </div>
    </Modal>
  )
}
