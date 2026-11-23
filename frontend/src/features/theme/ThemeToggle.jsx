import { Monitor, Moon, Sun } from 'lucide-react'
import { useTheme } from './useTheme'
import './theme.css'

const OPTIONS = [
  { value: 'system', label: 'Tema automático (segue o sistema)', icon: Monitor },
  { value: 'light', label: 'Tema claro', icon: Sun },
  { value: 'dark', label: 'Tema escuro', icon: Moon },
]

export default function ThemeToggle() {
  const [choice, setChoice] = useTheme()
  return (
    <div className="theme-toggle" role="group" aria-label="Tema">
      {OPTIONS.map(({ value, label, icon: Icon }) => (
        <button key={value} type="button" className="theme-option" aria-pressed={choice === value} aria-label={label} title={label} onClick={() => setChoice(value)}>
          <Icon size={16} aria-hidden="true" />
        </button>
      ))}
    </div>
  )
}
