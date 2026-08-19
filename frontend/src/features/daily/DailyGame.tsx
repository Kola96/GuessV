import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { gameApi } from '../../services/game'
import { ApiError } from '../../services/api'
import type { DailyGameInfo, GuessEntry, GuessResponse, VtuberSearchResult } from '../../types'
import GuessCard, { TABLE_COLUMNS } from '../../components/GuessCard'
import SearchInput from '../../components/SearchInput'
import ResultBanner from '../../components/ResultBanner'
import Avatar from '../../components/Avatar'

export default function DailyGame() {
  const [info, setInfo] = useState<DailyGameInfo | null>(null)
  const [guesses, setGuesses] = useState<GuessEntry[]>([])
  const [gameOver, setGameOver] = useState(false)
  const [win, setWin] = useState(false)
  const [targetName, setTargetName] = useState<string | undefined>()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  // justFinished: 本轮猜测刚结束（弹窗）；restored: 从服务端恢复的已结束游戏（不弹窗）
  const [justFinished, setJustFinished] = useState(false)

  const maxAttempts = info?.maxAttempts ?? 8
  const attemptsUsed = guesses.length

  useEffect(() => {
    gameApi.dailyInfo().then((data) => {
      setInfo(data)
      setGuesses(data.guesses || [])
      // 恢复已结束的游戏：不弹窗
      const isOver = data.hasPlayed && (data.hasWon || data.attemptsUsed >= data.maxAttempts)
      setGameOver(isOver)
      setWin(data.hasWon)
      setTargetName(data.targetName)
      setJustFinished(false)
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
        setJustFinished(true)  // 本轮结束 → 弹窗
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
            {gameOver ? (win ? '已通关' : '已结束') : `剩 ${remaining} 次`}
          </span>
        </div>
      </div>

      {/* 弹窗：仅本轮刚结束时弹出 */}
      <ResultBanner
        win={win}
        gameOver={gameOver && justFinished}
        attemptsUsed={attemptsUsed}
        maxAttempts={maxAttempts}
        targetName={targetName}
        onDismiss={() => setJustFinished(false)}
      />

      {/* 已结束横幅（恢复进入时显示，不弹窗） */}
      {gameOver && !justFinished && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="card p-5 mb-3 flex items-center gap-4"
        >
          <Avatar name={targetName || '?'} size="lg" />
          <div className="flex-1 min-w-0">
            {win ? (
              <>
                <div className="text-exact font-bold text-lg">🎉 今日已通关</div>
                <div className="text-text-secondary text-sm">
                  用了 {attemptsUsed} / {maxAttempts} 次，答案是 {targetName}
                </div>
              </>
            ) : (
              <>
                <div className="text-secondary font-bold text-lg">😢 今日已结束</div>
                <div className="text-text-secondary text-sm">
                  答案是 {targetName}，明天再来挑战吧
                </div>
              </>
            )}
          </div>
          <a
            href="https://live.bilibili.com/"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-secondary text-sm shrink-0"
          >
            ▶ 直播间
          </a>
        </motion.div>
      )}

      {/* 猜测记录表格 */}
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

      {/* 已结束提示（表格下方） */}
      {gameOver && !justFinished && (
        <div className="text-center text-text-muted text-sm mt-4">
          明日 00:00 刷新，届时再来挑战 🌅
        </div>
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

      {/* 输入区：仅游戏未结束时显示 */}
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
