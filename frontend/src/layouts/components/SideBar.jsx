import { NavLink } from 'react-router'
import { AlarmClock, Folder, LayoutDashboard, ListCheck, Timer, User } from 'lucide-react'
import { useAuth } from '../../features/auth/useAuth'
import { getInitials } from '../initials'
import './sidebar.css'

const navItems = [
  { to: '/dashboard', label: 'Painel', icon: LayoutDashboard },
  { to: '/projects', label: 'Projetos', icon: Folder },
  { to: '/tasks', label: 'Tarefas', icon: ListCheck },
  { to: '/focus-sessions', label: 'Sessões', icon: Timer },
  { to: '/profile', label: 'Perfil', icon: User },
]

export default function SideBar() {
  const { user } = useAuth()

  return (
    <aside className="sidebar">
      <div className="sidebar-top">
        <div className="sidebar-logo">
          <span className="sidebar-logo-mark" aria-hidden="true"><AlarmClock size={20} /></span>
          <span className="sidebar-logo-text">FocusTask</span>
        </div>
        <nav className="sidebar-nav" aria-label="Menu lateral">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className="sidebar-link">
              <Icon size={18} aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
      </div>
      <div className="sidebar-user">
        <span className="sidebar-avatar" aria-hidden="true">{getInitials(user.name)}</span>
        <div className="sidebar-user-details">
          <p className="sidebar-user-name">{user.name}</p>
          <p className="sidebar-user-email" title={user.email}>{user.email}</p>
        </div>
      </div>
    </aside>
  )
}
