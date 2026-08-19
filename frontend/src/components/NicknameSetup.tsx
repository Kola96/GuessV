import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { userApi } from '../services/user'
import { useUserStore } from '../stores/userStore'
import { ApiError } from '../services/api'

export default function NicknameSetup() {
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [shake, setShake] = useState(false)
  const { setAuth } = useUserStore()

  const handleSubmit = async (useRandom: boolean) => {
    setLoading(true)
    setError('')
    try {
      const fp = navigator.userAgent.slice(0, 64)
      const resp = await userApi.init(useRandom ? null : nickname.trim(), useRandom, fp)
      const profile = await userApi.profile().catch(() => null)
      setAuth(resp.token, profile ?? {
        userId: resp.userId, nickname: resp.nickname, gameId: resp.gameId,
        displayName: resp.displayName, isAnonymous: true,
      })
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '初始化失败'
      setError(msg)
      setShake(true)
      setTimeout(() => setShake(false), 500)
    } finally {
      setLoading(false)
    }
  }

  const getRandom = async () => {
    try {
      const names = await userApi.randomNicknames(1)
      if (names.length > 0) setNickname(names[0])
    } catch {}
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-8 max-w-md w-full"
      >
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
            GuessV
          </h1>
          <p className="text-text-secondary mt-1">V一把 · 猜VTuber游戏</p>
        </div>

        <p className="text-text-muted text-sm mb-4 text-center">
          设置你的昵称即可开始游玩
        </p>

        <motion.div animate={shake ? { x: [0, -8, 8, -5, 5, 0] } : {}}>
          <input
            className="input-field mb-2"
            placeholder="输入昵称（2-16 字符）"
            value={nickname}
            maxLength={16}
            onChange={(e) => setNickname(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && nickname.trim().length >= 2 && handleSubmit(false)}
          />
        </motion.div>

        <AnimatePresence>
          {error && (
            <motion.p
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="text-red-400 text-sm mb-2"
            >
              {error}
            </motion.p>
          )}
        </AnimatePresence>

        <div className="flex gap-2">
          <button
            className="btn-primary flex-1"
            disabled={loading || nickname.trim().length < 2}
            onClick={() => handleSubmit(false)}
          >
            {loading ? '加载中...' : '开始游戏'}
          </button>
          <button
            className="btn-secondary"
            disabled={loading}
            onClick={getRandom}
            title="随机昵称"
          >
            🎲
          </button>
        </div>

        <button
          className="w-full mt-3 text-text-muted hover:text-secondary text-sm transition-colors"
          disabled={loading}
          onClick={() => handleSubmit(true)}
        >
          直接随机进入 →
        </button>
      </motion.div>
    </div>
  )
}
