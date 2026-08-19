import { useState } from 'react'
import { motion } from 'framer-motion'
import { useAdminStore } from '../../stores/adminStore'
import adminApi from '../../services/adminApi'

export default function AdminLogin() {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setToken } = useAdminStore()

  const handleLogin = async () => {
    setLoading(true)
    setError('')
    try {
      const resp = await adminApi.post<{ token: string }>('/login', { username, password })
      setToken(resp.data.token)
    } catch (e) {
      setError(e instanceof Error ? e.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-8 max-w-sm w-full"
      >
        <h1 className="text-xl font-bold text-text-primary mb-1">GuessV 后台</h1>
        <p className="text-text-muted text-sm mb-6">管理员登录</p>

        <input
          className="input-field mb-3"
          placeholder="用户名"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          className="input-field mb-3"
          type="password"
          placeholder="密码"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
        />

        {error && <p className="text-red-400 text-sm mb-3">{error}</p>}

        <button
          className="btn-primary w-full"
          disabled={loading || !password}
          onClick={handleLogin}
        >
          {loading ? '登录中...' : '登录'}
        </button>
      </motion.div>
    </div>
  )
}
