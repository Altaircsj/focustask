import { Outlet } from 'react-router'
import ThemeToggle from '../features/theme/ThemeToggle'

// Páginas sem menu lateral (login, cadastro): só o seletor de tema no canto.
export default function PublicLayout() {
  return (
    <>
      <div className="public-theme"><ThemeToggle /></div>
      <Outlet />
    </>
  )
}
