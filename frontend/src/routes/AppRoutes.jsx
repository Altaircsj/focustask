import { Link, Navigate, Route, Routes } from 'react-router'
import AppLayout from '../layouts/AppLayout'
import LoginPage from '../features/auth/pages/LoginPage'
import RegisterPage from '../features/auth/pages/RegisterPage'
import { useAuth } from '../features/auth/useAuth'
import DashboardPage from '../features/dashboard/pages/DashboardPage'
import FocusSessionsPage from '../features/focusSessions/pages/FocusSessionsPage'
import ProfilePage from '../features/profile/pages/ProfilePage'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage'
import NewFocusSessionPage from '../features/focusSessions/pages/NewFocusSessionPage'

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
        <Route path="/tasks/:taskId" element={<TaskDetailPage />} />
        <Route path="/focus-sessions" element={<FocusSessionsPage />} />
        <Route path="/focus-sessions/new" element={<NewFocusSessionPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<main className="auth-card"><h1>Página não encontrada</h1><Link to="/">Ir ao início</Link></main>} />
    </Routes>
  )
}
