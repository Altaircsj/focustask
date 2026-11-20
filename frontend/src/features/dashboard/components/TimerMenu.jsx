import { useEffect, useRef, useState } from 'react'
import { Ellipsis } from 'lucide-react'
import { Link } from 'react-router'

export default function TimerMenu({ current, onEdit }) {
  const [open, setOpen] = useState(false)
  const box = useRef(null)
  const trigger = useRef(null)

  useEffect(() => {
    if (!open) return
    function outside(event) {
      if (!box.current?.contains(event.target)) setOpen(false)
    }
    function escape(event) {
      if (event.key === 'Escape') {
        setOpen(false)
        trigger.current?.focus()
      }
    }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', outside)
      document.removeEventListener('keydown', escape)
    }
  }, [open])

  return <div className="timer-menu" ref={box}>
    <button type="button" className="timer-round" ref={trigger} aria-label="Opções da sessão" aria-expanded={open} onClick={() => setOpen(!open)}>
      <Ellipsis size={18} aria-hidden="true" />
    </button>
    {open && <div className="timer-menu-list">
      <Link to="/focus-sessions">Ver histórico</Link>
      {current && <button type="button" onClick={() => { trigger.current.focus(); setOpen(false); onEdit(current) }}>Editar sessão atual</button>}
    </div>}
  </div>
}
