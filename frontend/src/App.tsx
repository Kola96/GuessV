import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useUserStore } from './stores/userStore'
import NicknameSetup from './components/NicknameSetup'
import Header from './components/Header'
import DailyGame from './features/daily/DailyGame'
import SingleGame from './features/single/SingleGame'

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

export default function App() {
  const { token } = useUserStore()

  // 页面加载时不需要做额外初始化，token 有则进入游戏
  useEffect(() => {}, [])

  if (!token) {
    return <NicknameSetup />
  }

  return (
    <BrowserRouter>
      <GameLayout />
    </BrowserRouter>
  )
}
