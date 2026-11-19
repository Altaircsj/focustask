import { Outlet } from 'react-router'
import SideBar from './components/SideBar'
import './app-layout.css'

export default function AppLayout() {
  return (
    <div className="app-shell">
      <SideBar />
      <div className="app-content"><Outlet /></div>
    </div>
  )
}
