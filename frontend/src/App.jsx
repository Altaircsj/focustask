import { BrowserRouter } from 'react-router'
import AppRoutes from './routes/AppRoutes'
import './App.css'
import { AuthProvider, SessionGate } from './features/auth/AuthContext'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <SessionGate><AppRoutes /></SessionGate>
      </AuthProvider>
    </BrowserRouter>
  )
}
