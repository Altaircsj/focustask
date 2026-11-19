import { Link, Navigate, Route, Routes } from 'react-router'
import AppLayout from '../layouts/AppLayout'
import LoginPage from '../features/auth/pages/LoginPage'
import RegisterPage from '../features/auth/pages/RegisterPage'
import { useAuth } from '../features/auth/useAuth'
import DashboardPage from '../features/dashboard/pages/DashboardPage'

function PlaceholderPage({ titulo }) {
  return <main className="auth-card"><h1>{titulo}</h1><p>Esta página será implementada em uma próxima etapa.</p><Link to="/dashboard">Voltar ao painel</Link></main>
}

export default function AppRoutes() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={user ? <AppLayout /> : <Navigate to="/login" replace />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/projects" element={<PlaceholderPage titulo="Projetos" />} />
        <Route path="/projects/:projectId" element={<PlaceholderPage titulo="Detalhes do projeto" />} />
        <Route path="/tasks" element={<PlaceholderPage titulo="Tarefas" />} />
        <Route path="/tasks/:taskId" element={<PlaceholderPage titulo="Detalhes da tarefa" />} />
        <Route path="/focus-sessions" element={<PlaceholderPage titulo="Sessões de foco" />} />
        <Route path="/profile" element={<PlaceholderPage titulo="Perfil" />} />
      </Route>
      <Route path="*" element={<main className="auth-card"><h1>Página não encontrada</h1><Link to="/">Ir ao início</Link></main>} />
    </Routes>
  )
}
