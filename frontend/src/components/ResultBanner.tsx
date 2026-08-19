import { motion, AnimatePresence } from 'framer-motion'
import Avatar from './Avatar'
import Confetti from './Confetti'

interface ResultBannerProps {
  win: boolean
  gameOver: boolean
  attemptsUsed: number
  maxAttempts: number
  targetName?: string
}

export default function ResultBanner({
  win, gameOver, attemptsUsed, maxAttempts, targetName,
}: ResultBannerProps) {
  return (
    <AnimatePresence>
      {gameOver && (
        <motion.div
          initial={{ opacity: 0, y: 40, scale: 0.95 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -20 }}
          className="card p-6 mb-3 text-center"
        >
          <Confetti active={win} />

          {win ? (
            <>
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 200, delay: 0.1 }}
                className="text-4xl mb-2"
              >
                🎉
              </motion.div>
              <h2 className="text-2xl font-bold text-exact mb-1">猜对了！</h2>
              <p className="text-text-secondary">
                用了 {attemptsUsed} / {maxAttempts} 次
              </p>
            </>
          ) : (
            <>
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 200, delay: 0.1 }}
                className="text-4xl mb-2"
              >
                😢
              </motion.div>
              <h2 className="text-2xl font-bold text-secondary mb-3">差一点...</h2>
              {targetName && (
                <div className="flex items-center justify-center gap-3">
                  <Avatar name={targetName} size="lg" />
                  <div className="text-left">
                    <div className="text-text-muted text-xs">答案是</div>
                    <div className="text-xl font-bold text-text-primary">{targetName}</div>
                  </div>
                </div>
              )}
            </>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
