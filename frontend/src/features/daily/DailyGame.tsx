import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { gameApi } from '../../services/game'
import { ApiError } from '../../services/api'
import type { DailyGameInfo, GuessEntry, GuessResponse, VtuberSearchResult } from '../../types'
import GuessCard from '../../components/GuessCard'
import SearchInput from '../../components/SearchInput'
import ResultBanner from '../../components/ResultBanner'

export default function DailyGame() {
  const [info, setInfo] = useState<DailyGameInfo | null>(null)
  const [guesses, setGuesses] = useState<GuessEntry[]>([])
  const [gameOver, setGameOver] = useState(false)
  const [win, setWin] = useState(false)
  const [targetName, setTargetName] = useState<string | undefined>()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [dismissed, setDismissed] = useState(false)

  const maxAttempts = info?.maxAttempts ?? 8
  const attemptsUsed = guesses.length

  useEffect(() => {
    gameApi.dailyInfo().then((data) => {
      setInfo(data)
      setGuesses(data.guesses || [])
      setGameOver(data.hasPlayed && (data.hasWon || data.attemptsUsed >= data.maxAttempts))
      setWin(data.hasWon)
    }).catch(() => {})
  }, [])

  const handleGuess = async (v: VtuberSearchResult) => {
    setError('')
    setLoading(true)
    try {
      const resp: GuessResponse = await gameApi.dailyGuess(v.id)
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

  const remaining = maxAttempts - attemptsUsed

  return (
    <div className="max-w-4xl mx-auto px-6 pb-24">
      {/* 状态栏 */}
      <div className="flex items-center justify-between py-4">
        <div className="flex items-center gap-2">
          <span className="text-text-muted text-sm">每日猜V</span>
          {info && <span className="text-text-muted text-xs">{info.date}</span>}
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
            {remaining > 0 ? `剩 ${remaining} 次` : '已用完'}
          </span>
        </div>
      </div>

      {/* 结果弹窗 */}
      <ResultBanner
        win={win}
        gameOver={gameOver && !dismissed}
        attemptsUsed={attemptsUsed}
        maxAttempts={maxAttempts}
        targetName={targetName}
        onDismiss={() => setDismissed(true)}
      />

      {/* 猜测记录 */}
      <div className="space-y-2">
        {guesses.map((g, i) => (
          <GuessCard key={`${g.vtuberId}-${i}`} entry={g} index={i} />
        ))}
      </div>

      {/* 空状态 */}
      {guesses.length === 0 && !gameOver && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="card p-8 text-center text-text-muted"
        >
          <p className="mb-2">🎯 今日目标已就绪</p>
          <p className="text-sm">输入一个 VTuber 名字开始推理吧</p>
        </motion.div>
      )}

      {/* 错误提示 */}
      {error && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-red-400 text-sm text-center py-2"
        >
          {error}
        </motion.div>
      )}

      {/* 输入区 */}
      {!gameOver && (
        <div className="fixed bottom-0 left-0 right-0 bg-base/80 backdrop-blur-md border-t border-white/5 py-3 px-4">
          <div className="max-w-4xl mx-auto">
            <SearchInput onGuess={handleGuess} disabled={loading || gameOver} />
          </div>
        </div>
      )}
    </div>
  )
}
