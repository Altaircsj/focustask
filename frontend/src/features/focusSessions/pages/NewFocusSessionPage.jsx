import { Navigate } from 'react-router'

export default function NewFocusSessionPage() {
  return <Navigate to="/dashboard" replace state={{ newFocus: true }} />
}
