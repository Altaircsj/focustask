import { Link } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import '../dashboard.css'

export default function DashboardPage() {
  const { user, logout } = useAuth()
  return (
    <main className="auth-card dashboard">
      <h1>Olá, {user.name}!</h1>
      <p>Você está conectado ao FocusTask.</p>
      <dl><dt>Nome</dt><dd>{user.name}</dd><dt>E-mail</dt><dd>{user.email}</dd></dl>
      <nav aria-label="Navegação principal">
        <Link to="/projects">Projetos</Link><Link to="/tasks">Tarefas</Link><Link to="/focus-sessions">Sessões de foco</Link><Link to="/profile">Perfil</Link>
      </nav>
      <button onClick={() => logout()}>Sair</button>
    </main>
  )
}
