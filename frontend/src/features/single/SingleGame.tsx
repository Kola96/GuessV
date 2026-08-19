import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { gameApi } from '../../services/game'
import { ApiError } from '../../services/api'
import type { GuessEntry, GuessResponse, PoolVO, SingleStartResponse, VtuberSearchResult } from '../../types'
import GuessCard, { TABLE_COLUMNS } from '../../components/GuessCard'
import SearchInput from '../../components/SearchInput'
import ResultBanner from '../../components/ResultBanner'

export default function SingleGame() {
  const [pools, setPools] = useState<PoolVO[]>([])
  const [selectedPool, setSelectedPool] = useState<string | null>(null)
  const [session, setSession] = useState<SingleStartResponse | null>(null)
  const [guesses, setGuesses] = useState<GuessEntry[]>([])
  const [gameOver, setGameOver] = useState(false)
  const [win, setWin] = useState(false)
  const [targetName, setTargetName] = useState<string | undefined>()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    gameApi.pools().then(setPools).catch(() => {})
  }, [])

  const maxAttempts = session?.maxAttempts ?? 8
  const attemptsUsed = guesses.length

  const handleStart = async (poolTag: string) => {
    setError('')
    try {
      const resp = await gameApi.singleStart(poolTag)
      setSession(resp)
      setSelectedPool(poolTag)
      setGuesses([])
      setGameOver(false)
      setWin(false)
      setTargetName(undefined)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '开始失败')
    }
  }

  const handleGuess = async (v: VtuberSearchResult) => {
    if (!session) return
    setError('')
    setLoading(true)
    try {
      const resp: GuessResponse = await gameApi.singleGuess(session.sessionId, v.id)
      const newEntry: GuessEntry = {
        vtuberId: v.id,
        vtuberName: v.name,
        attemptNumber: resp.attemptsUsed,
        correct: resp.correct,
        comparison: resp.comparison,
        guessedAt: new Date().toISOString(),
      }
      setGuesses(prev => [...prev, newEntry])
      if (resp.gameOver) {
        setGameOver(true)
        setWin(resp.win)
        setTargetName(resp.targetVtuber?.name)
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '猜测失败')
    } finally {
      setLoading(false)
    }
  }

  const handleRestart = () => {
    setSession(null)
    setSelectedPool(null)
    setGuesses([])
    setGameOver(false)
    setWin(false)
    setTargetName(undefined)
    setDismissed(false)
  }

  // 题库选择页
  if (!session) {
    return (
      <div className="max-w-4xl mx-auto px-6 py-6">
        <h2 className="text-xl font-bold text-text-primary mb-4">选择题库</h2>
        <div className="grid grid-cols-2 gap-3">
          {pools.map((p) => (
            <motion.button
              key={p.tag}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => handleStart(p.tag)}
              disabled={p.vtuberCount === 0}
              className={`card p-4 text-left ${p.vtuberCount === 0 ? 'opacity-40 cursor-not-allowed' : 'hover:border-primary/30'}`}
            >
              <div className="font-bold text-text-primary mb-1">{p.tag}</div>
              <div className="text-text-muted text-xs">{p.description}</div>
              <div className="text-secondary text-sm mt-2">{p.vtuberCount} 位 V</div>
            </motion.button>
          ))}
        </div>
        {error && <p className="text-red-400 text-sm mt-4">{error}</p>}
      </div>
    )
  }

  const remaining = maxAttempts - attemptsUsed

  // 游戏页
  return (
    <div className="max-w-4xl mx-auto px-6 pb-24">
      <div className="flex items-center justify-between py-4">
        <div className="flex items-center gap-2">
          <span className="text-text-muted text-sm">单人模式</span>
          <span className="text-secondary text-xs">{selectedPool}</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex gap-1">
            {Array.from({ length: maxAttempts }).map((_, i) => (
              <div
                key={i}
                className={`w-2 h-2 rounded-full ${
                  i < attemptsUsed
                    ? (win && i === attemptsUsed - 1 ? 'bg-exact' : 'bg-primary/60')
                    : 'bg-white/10'
                }`}
              />
            ))}
          </div>
          <span className="text-text-secondary text-sm">
            {remaining > 0 ? `剩 ${remaining}` : '用完'}
          </span>
        </div>
      </div>

      <ResultBanner
        win={win}
        gameOver={gameOver && !dismissed}
        attemptsUsed={attemptsUsed}
        maxAttempts={maxAttempts}
        targetName={targetName}
        onRestart={handleRestart}
        onDismiss={() => setDismissed(true)}
      />

      {guesses.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-white/5">
          <table className="w-full border-collapse">
            <thead>
              <tr className="bg-elevated/60">
                {TABLE_COLUMNS.map((col) => (
                  <th
                    key={col.key}
                    className={`px-2 py-2 text-xs font-medium text-text-secondary border border-white/5 ${
                      col.key === 'name' ? 'sticky left-0 bg-elevated/60 z-10 text-left' : 'text-center'
                    }`}
                  >
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {guesses.map((g, i) => (
                <GuessCard key={`${g.vtuberId}-${i}`} entry={g} index={i} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {guesses.length === 0 && !gameOver && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="card p-8 text-center text-text-muted"
        >
          <p className="mb-2">🎯 目标已锁定</p>
          <p className="text-sm">输入 VTuber 名字开始推理</p>
        </motion.div>
      )}

      {error && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-red-400 text-sm text-center py-2">
          {error}
        </motion.div>
      )}

      {gameOver ? (
        <button onClick={handleRestart} className="btn-primary w-full mt-4">
          再来一局 →
        </button>
      ) : (
        <div className="fixed bottom-0 left-0 right-0 bg-base/80 backdrop-blur-md border-t border-white/5 py-3 px-4">
          <div className="max-w-4xl mx-auto flex gap-2">
            <div className="flex-1">
              <SearchInput onGuess={handleGuess} disabled={loading} />
            </div>
            <button onClick={handleRestart} className="btn-secondary text-sm" title="放弃重开">
              ↺
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
