import { Link, useLocation } from 'react-router-dom'
import { useUserStore } from '../stores/userStore'

export default function Header() {
  const { profile } = useUserStore()
  const location = useLocation()

  const navItems = [
    { to: '/', label: '每日', path: '/' },
    { to: '/single', label: '单人', path: '/single' },
  ]

  return (
    <header className="sticky top-0 z-20 backdrop-blur-md bg-base/60 border-b border-white/5">
      <div className="max-w-2xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <span className="text-xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
            GuessV
          </span>
          <span className="text-text-muted text-xs hidden sm:inline">V一把</span>
        </Link>

        <nav className="flex items-center gap-1">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
                location.pathname === item.path
                  ? 'bg-primary/20 text-primary'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        {profile && (
          <div className="text-sm text-text-secondary">
            {profile.displayName}
          </div>
        )}
      </div>
    </header>
  )
}
