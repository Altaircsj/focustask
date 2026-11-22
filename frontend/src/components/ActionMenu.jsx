import { useEffect, useState } from 'react'
import { EllipsisVertical } from 'lucide-react'
import '../features/focusSessions/focus-sessions.css'

export default function ActionMenu({ label, actions, onSelect, disabled = false }) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) return undefined
    const close = (event) => { if (event.type === 'keydown' ? event.key === 'Escape' : !event.target.closest('.row-menu')) setOpen(false) }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', close)
    }
  }, [open])

  return (
    <div className="row-menu">
      <button type="button" className="row-menu-trigger" aria-label={label} aria-haspopup="true" aria-expanded={open} disabled={disabled} onClick={() => setOpen((value) => !value)}>
        <EllipsisVertical size={16} aria-hidden="true" />
      </button>
      {open && (
        <ul className="row-menu-list">
          {actions.map((action) => (
            <li key={action.id}><button type="button" onClick={() => { setOpen(false); onSelect(action.id) }}>{action.label}</button></li>
          ))}
        </ul>
      )}
    </div>
  )
}
