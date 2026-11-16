import { Navigate, Route, Routes } from 'react-router'

import LoginPage from '../features/auth/LoginPage'

// Substitua cada uso por sua página real conforme for implementando.
function PlaceholderPage({ titulo }) {
  return (
    <main>
      <h1>{titulo}</h1>
    </main>
  )
}

//pra quando houver usuario logado
/*
<Route
  path="/"
  element={
    usuarioLogado
      ? <DashboardPage />
      : <Navigate to="/login" replace />
  }
/>
 */


export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route path="/login" element={<LoginPage />} />

      <Route
        path="/register"
        element={<PlaceholderPage titulo="Criar conta" />}
      />

      <Route
        path="/dashboard"
        element={<PlaceholderPage titulo="Painel" />}
      />

      <Route
        path="/projects"
        element={<PlaceholderPage titulo="Projetos" />}
      />

      <Route
        path="/projects/:projectId"
        element={<PlaceholderPage titulo="Detalhes do projeto" />}
      />

      <Route
        path="/tasks"
        element={<PlaceholderPage titulo="Tarefas" />}
      />

      <Route
        path="/tasks/:taskId"
        element={<PlaceholderPage titulo="Detalhes da tarefa" />}
      />

      <Route
        path="/focus-sessions"
        element={<PlaceholderPage titulo="Sessões de foco" />}
      />

      <Route
        path="/profile"
        element={<PlaceholderPage titulo="Perfil" />}
      />

      <Route
        path="*"
        element={<PlaceholderPage titulo="Página não encontrada" />}
      />
    </Routes>
  )
}