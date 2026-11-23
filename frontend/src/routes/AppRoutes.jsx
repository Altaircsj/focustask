import { Link, Navigate, Route, Routes } from 'react-router'
import AppLayout from '../layouts/AppLayout'
import PublicLayout from '../layouts/PublicLayout'
import LoginPage from '../features/auth/pages/LoginPage'
import RegisterPage from '../features/auth/pages/RegisterPage'
import { useAuth } from '../features/auth/useAuth'
import DashboardPage from '../features/dashboard/pages/DashboardPage'
import FocusSessionsPage from '../features/focusSessions/pages/FocusSessionsPage'
import ProjectDetailPage from '../features/projects/pages/ProjectDetailPage'
import ProjectsPage from '../features/projects/pages/ProjectsPage'
import TasksPage from '../features/tasks/pages/TasksPage'
import ProfilePage from '../features/profile/pages/ProfilePage'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage'
import NewFocusSessionPage from '../features/focusSessions/pages/NewFocusSessionPage'

export default function AppRoutes() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
      <Route element={<PublicLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={user ? <AppLayout /> : <Navigate to="/login" replace />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="/tasks" element={<TasksPage />} />
        <Route path="/tasks/:taskId" element={<TaskDetailPage />} />
        <Route path="/focus-sessions" element={<FocusSessionsPage />} />
        <Route path="/focus-sessions/new" element={<NewFocusSessionPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>
      <Route element={<PublicLayout />}>
        <Route path="*" element={<main className="auth-card"><h1>Página não encontrada</h1><Link to="/">Ir ao início</Link></main>} />
      </Route>
    </Routes>
  )
}
