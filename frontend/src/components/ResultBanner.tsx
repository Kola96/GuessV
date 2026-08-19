import { motion, AnimatePresence } from 'framer-motion'
import Avatar from './Avatar'
import Confetti from './Confetti'

interface ResultBannerProps {
  win: boolean
  gameOver: boolean
  attemptsUsed: number
  maxAttempts: number
  targetName?: string
  onRestart?: () => void
  onDismiss?: () => void
}

export default function ResultBanner({
  win, gameOver, attemptsUsed, maxAttempts, targetName, onRestart, onDismiss,
}: ResultBannerProps) {
  return (
    <AnimatePresence>
      {gameOver && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-base/70 backdrop-blur-sm"
          onClick={onDismiss}
        >
          <Confetti active={win} />

          <motion.div
            initial={{ scale: 0.7, opacity: 0, y: 30 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.7, opacity: 0, y: 30 }}
            transition={{ type: 'spring', stiffness: 200, damping: 20 }}
            className="card p-8 max-w-sm w-full mx-4 text-center border border-primary/20 shadow-[0_0_40px_rgba(192,132,252,0.15)]"
            onClick={(e) => e.stopPropagation()}
          >
            {/* 大头像 */}
            {targetName && (
              <div className="flex justify-center mb-4">
                <div className="w-20 h-20 rounded-full bg-gradient-to-br from-primary/30 to-secondary/30 flex items-center justify-center text-3xl font-bold shadow-lg">
                  {targetName.charAt(0)}
                </div>
              </div>
            )}

            {win ? (
              <>
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', stiffness: 200, delay: 0.15 }}
                  className="text-5xl mb-2"
                >
                  🎉
                </motion.div>
                <h2 className="text-2xl font-bold text-exact mb-1">猜对了！</h2>
                <p className="text-text-secondary mb-3">
                  用了 {attemptsUsed} / {maxAttempts} 次
                </p>
              </>
            ) : (
              <>
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', stiffness: 200, delay: 0.15 }}
                  className="text-5xl mb-2"
                >
                  😢
                </motion.div>
                <h2 className="text-2xl font-bold text-secondary mb-1">差一点...</h2>
                <p className="text-text-muted text-sm mb-3">答案是</p>
              </>
            )}

            {/* V 名字 */}
            {targetName && (
              <p className="text-lg font-bold text-text-primary mb-3">{targetName}</p>
            )}

            {/* 直播间链接 */}
            <a
              href="https://live.bilibili.com/"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary inline-block mb-4 text-sm"
            >
              ▶ 直播间
            </a>

            <div className="flex gap-2">
              {onRestart && (
                <button onClick={onRestart} className="btn-primary flex-1">
                  再来一局
                </button>
              )}
              {!onRestart && (
                <button onClick={onDismiss} className="btn-secondary flex-1">
                  关闭
                </button>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
