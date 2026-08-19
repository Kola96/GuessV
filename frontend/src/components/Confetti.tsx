import { useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'

interface ConfettiProps {
  active: boolean
}

const PARTICLE_COUNT = 40
const COLORS = ['#c084fc', '#f472b6', '#4ade80', '#fbbf24', '#60a5fa', '#a78bfa']

export default function Confetti({ active }: ConfettiProps) {
  const pieces = useRef(
    Array.from({ length: PARTICLE_COUNT }).map((_, i) => ({
      id: i,
      x: Math.random() * 100,
      delay: Math.random() * 0.3,
      duration: 2 + Math.random() * 2,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
      size: 6 + Math.random() * 8,
      drift: (Math.random() - 0.5) * 200,
    }))
  )

  useEffect(() => {
    if (!active) return
    const timer = setTimeout(() => {}, 4000)
    return () => clearTimeout(timer)
  }, [active])

  return (
    <AnimatePresence>
      {active && (
        <div className="fixed inset-0 pointer-events-none z-50 overflow-hidden">
          {pieces.current.map((p) => (
            <motion.div
              key={p.id}
              initial={{ opacity: 1, y: -20, x: `${p.x}vw`, rotate: 0 }}
              animate={{
                opacity: [1, 1, 0],
                y: ['0vh', '100vh'],
                x: [`${p.x}vw`, `${p.x + p.drift / 10}vw`],
                rotate: [0, 360 * (Math.random() > 0.5 ? 1 : -1)],
              }}
              transition={{
                duration: p.duration,
                delay: p.delay,
                ease: 'easeIn',
              }}
              style={{
                position: 'absolute',
                width: p.size,
                height: p.size,
                background: p.color,
                borderRadius: Math.random() > 0.5 ? '50%' : '2px',
              }}
            />
          ))}
        </div>
      )}
    </AnimatePresence>
  )
}
