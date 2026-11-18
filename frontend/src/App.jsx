import { BrowserRouter } from 'react-router'
import AppRoutes from './routes/AppRoutes'
import './App.css'
import { AuthProvider } from './features/auth/AuthContext'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
