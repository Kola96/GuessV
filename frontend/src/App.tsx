import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useUserStore } from './stores/userStore'
import { useAdminStore } from './stores/adminStore'
import NicknameSetup from './components/NicknameSetup'
import Header from './components/Header'
import DailyGame from './features/daily/DailyGame'
import SingleGame from './features/single/SingleGame'
import AdminLogin from './features/admin/AdminLogin'
import AdminLayout from './features/admin/AdminLayout'
import AdminDashboard from './features/admin/AdminDashboard'
import AdminVtuberList from './features/admin/AdminVtuberList'
import AdminVtuberEdit from './features/admin/AdminVtuberEdit'
import AdminPoolList from './features/admin/AdminPoolList'

function GameLayout() {
  return (
    <div className="min-h-screen">
      <Header />
      <Routes>
        <Route path="/" element={<DailyGame />} />
        <Route path="/single" element={<SingleGame />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

function AdminRoutes() {
  const { token } = useAdminStore()
  if (!token) return <AdminLogin />
  return (
    <AdminLayout>
      <Routes>
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/vtuber" element={<AdminVtuberList />} />
        <Route path="/admin/vtuber/:id" element={<AdminVtuberEdit />} />
        <Route path="/admin/pool" element={<AdminPoolList />} />
        <Route path="/admin/*" element={<Navigate to="/admin" replace />} />
      </Routes>
    </AdminLayout>
  )
}

export default function App() {
  const { token } = useUserStore()

  return (
    <BrowserRouter>
      <Routes>
        {/* 后台路由 */}
        <Route path="/admin/*" element={<AdminRoutes />} />
        {/* 游戏路由 */}
        <Route path="/*" element={!token ? <NicknameSetup /> : <GameLayout />} />
      </Routes>
    </BrowserRouter>
  )
}
