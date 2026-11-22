import { useState } from 'react'
import Modal from '../../dashboard/components/Modal'

export default function ProjectFormModal({ project, busy, error, onSave, onClose }) {
  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const valid = name.trim().length > 0 && name.length <= 255

  function submit(event) {
    event.preventDefault()
    if (valid && !busy) onSave({ name: name.trim(), description: description.trim() })
  }

  return (
    <Modal title={project ? 'Editar Projeto' : 'Criar Novo Projeto'} subtitle="Defina o nome e a descrição do espaço de trabalho." onClose={onClose}>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="project-name">Nome do Projeto</label>
            <input id="project-name" className="chip-input" maxLength={255} placeholder="Ex: Desenvolvimento Web App" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="project-description">Descrição / Objetivos</label>
            <textarea id="project-description" className="chip-input" rows={4} placeholder="Escreva brevemente os marcos importantes, tecnologias ou contexto." value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
        </fieldset>
        {error && <p role="alert">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="dash-btn" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="submit" className="dash-btn dash-btn-primary" disabled={!valid || busy}>{busy ? 'Salvando…' : 'Salvar Projeto'}</button>
        </div>
      </form>
    </Modal>
  )
}
