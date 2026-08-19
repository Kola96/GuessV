import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAdminStore } from '../../stores/adminStore'
import adminApi from '../../services/adminApi'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { logout } = useAdminStore()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  const navItems = [
    { to: '/admin', label: '数据看板', icon: '📊' },
    { to: '/admin/vtuber', label: 'VTuber 管理', icon: '🎤' },
  ]

  return (
    <div className="min-h-screen flex">
      {/* 侧边栏 */}
      <div className={`bg-surface border-r border-white/5 transition-all ${collapsed ? 'w-16' : 'w-48'}`}>
        <div className="p-3 flex items-center justify-between">
          {!collapsed && <span className="text-sm font-bold text-primary">GuessV 后台</span>}
          <button onClick={() => setCollapsed(!collapsed)} className="text-text-muted text-xs">
            {collapsed ? '▶' : '◀'}
          </button>
        </div>
        <nav className="px-2">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors mb-1 ${
                location.pathname === item.to
                  ? 'bg-primary/20 text-primary'
                  : 'text-text-secondary hover:text-text-primary hover:bg-elevated/50'
              }`}
            >
              <span>{item.icon}</span>
              {!collapsed && <span>{item.label}</span>}
            </Link>
          ))}
        </nav>
        <div className="absolute bottom-4 left-2 right-2">
          <button
            onClick={() => { logout(); window.location.href = '/admin' }}
            className="w-full text-left px-3 py-2 rounded-lg text-sm text-text-muted hover:text-red-400 transition-colors"
          >
            {collapsed ? '🚪' : '🚪 退出登录'}
          </button>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-auto p-6">
        {children}
      </div>
    </div>
  )
}
