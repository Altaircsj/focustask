import { useEffect, useRef } from 'react'

export default function Modal({ title, subtitle, onClose, children }) {
  const box = useRef(null)
  const close = useRef(onClose)

  useEffect(() => { close.current = onClose })
  useEffect(() => {
    const previous = document.activeElement
    box.current?.focus()
    const onKey = (event) => { if (event.key === 'Escape') close.current() }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      previous?.focus?.()
    }
  }, [])

  return (
    <div className="modal-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="modal-title" tabIndex={-1} ref={box}>
        <h2 id="modal-title">{title}</h2>
        {subtitle && <p className="modal-subtitle">{subtitle}</p>}
        {children}
      </div>
    </div>
  )
}
