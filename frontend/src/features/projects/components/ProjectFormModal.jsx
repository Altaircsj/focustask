import { useState } from 'react'
import { Check, Plus } from 'lucide-react'
import { DEFAULT_COLOR, PROJECT_COLORS, TAG_SUGGESTIONS, tagError } from '../appearance'
import Modal from '../../dashboard/components/Modal'

export default function ProjectFormModal({ project, busy, error, onSave, onClose }) {
  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const [color, setColor] = useState(project?.color ?? DEFAULT_COLOR)
  const [tags, setTags] = useState(() => [...(project?.tags ?? [])])
  const [adding, setAdding] = useState(false)
  const [draft, setDraft] = useState('')
  const [tagMessage, setTagMessage] = useState('')
  const valid = !draft.trim() && name.trim().length > 0 && name.length <= 255

  function addTag(value) {
    const tag = value.trim()
    const message = tagError(tags, tag)
    setTagMessage(message)
    if (message) return
    setTags([...tags, tag])
    setDraft('')
    setAdding(false)
  }

  function submit(event) {
    event.preventDefault()
    if (valid && !busy) onSave({ name: name.trim(), description: description.trim(), color, tags })
  }

  return (
    <Modal title={project ? 'Editar Projeto' : 'Criar Novo Projeto'} subtitle="Organize seu espaço de trabalho." onClose={onClose}>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="focus-fields" disabled={busy}>
          <div className="form-field">
            <label htmlFor="project-name">Nome do Projeto</label>
            <input id="project-name" className="chip-input" maxLength={255} placeholder="Ex: Desenvolvimento Web App" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <fieldset className="form-field project-palette">
            <legend>Cor do projeto</legend>
            <div className="color-options">
              {PROJECT_COLORS.map(([label, hex]) => (
                <label key={hex} style={{ '--project-color': hex }}>
                  <input type="radio" name="project-color" aria-label={label} value={hex} checked={color === hex} onChange={() => setColor(hex)} />
                  {color === hex && <Check size={16} strokeWidth={3} aria-hidden="true" />}
                </label>
              ))}
            </div>
          </fieldset>
          <div className="form-field">
            <label htmlFor="project-description">Descrição / Objetivos</label>
            <textarea id="project-description" className="chip-input" rows={4} placeholder="Escreva brevemente os marcos importantes, tecnologias ou contexto." value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="form-field">
            <span className="form-label" id="project-tags-label">Tags do projeto</span>
            <div className="project-chips" role="group" aria-labelledby="project-tags-label">
              {tags.map((tag) => <button key={tag} type="button" className="chip selected-tag" aria-label={`Remover ${tag}`}
                onClick={() => { setTags(tags.filter((item) => item !== tag)); setTagMessage('') }}>
                {tag} <span aria-hidden="true">×</span>
              </button>)}
              {TAG_SUGGESTIONS.filter((tag) => !tags.some((item) => item.toLowerCase() === tag.toLowerCase())).map((tag) => (
                <button key={tag} type="button" className="chip" disabled={tags.length >= 10} onClick={() => addTag(tag)}>{tag}</button>
              ))}
              {!adding && <button type="button" className="chip add-tag" onClick={() => { setAdding(true); setTagMessage('') }}>
                <Plus size={13} aria-hidden="true" />Nova Tag
              </button>}
            </div>
            {adding && <div className="tag-entry">
              <input id="project-tag-input" aria-label="Texto da nova tag" className="chip-input" autoFocus value={draft}
                aria-invalid={Boolean(tagMessage)} aria-describedby={tagMessage ? 'project-tag-error' : undefined}
                onChange={(event) => { setDraft(event.target.value); setTagMessage('') }}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.nativeEvent.isComposing) { event.preventDefault(); addTag(draft) }
                }} />
              <button type="button" className="dash-btn" onClick={() => addTag(draft)}>Adicionar</button>
            </div>}
            {tagMessage && <p id="project-tag-error" role="alert">{tagMessage}</p>}
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
