import { useRef, useLayoutEffect, useState } from 'react'
import { motion } from 'framer-motion'
import type { FieldComparison, MatchType } from '../types'

const matchStyles: Record<MatchType, { bg: string; text: string; icon: string }> = {
  exact:  { bg: 'bg-exact/20 border-exact/40', text: 'text-exact', icon: '✓' },
  partial:{ bg: 'bg-partial/20 border-partial/40', text: 'text-partial', icon: '◐' },
  none:   { bg: 'bg-none/15 border-white/5', text: 'text-text-muted', icon: '✗' },
  higher: { bg: 'bg-none/15 border-white/5', text: 'text-partial', icon: '↑' },
  lower:  { bg: 'bg-none/15 border-white/5', text: 'text-partial', icon: '↓' },
}

interface TableCellProps {
  field: FieldComparison
  index: number
}

const CELL_MAX_WIDTH = 100

export default function ComparisonRow({ field, index }: TableCellProps) {
  const match = field.match as MatchType
  const style = matchStyles[match] || matchStyles.none

  const formatValue = (val: unknown): string => {
    if (typeof val === 'number' && val >= 10000) {
      return (val / 10000).toFixed(1) + '万'
    }
    if (Array.isArray(val)) {
      return val.join(' / ')
    }
    return (val as string) || '—'
  }
  const displayValue = formatValue(field.value)
  const isArrow = match === 'higher' || match === 'lower'

  const textRef = useRef<HTMLDivElement>(null)
  const [needScroll, setNeedScroll] = useState(false)

  useLayoutEffect(() => {
    if (textRef.current) {
      const textWidth = textRef.current.scrollWidth
      setNeedScroll(textWidth > CELL_MAX_WIDTH - 8)
    }
  }, [displayValue])

  return (
    <motion.td
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.05 }}
      className={`px-2 py-1.5 border border-white/5 text-center ${style.bg}`}
    >
      <div className="flex items-center justify-center gap-1">
        <span className={`text-xs shrink-0 ${style.text}`}>
          {isArrow ? field.direction : style.icon}
        </span>
        <div className="overflow-hidden" style={{ maxWidth: `${CELL_MAX_WIDTH}px` }}>
          {needScroll ? (
            <div
              className="text-xs whitespace-nowrap inline-flex"
              style={{ animation: 'scroll-loop 8s linear infinite' }}
            >
              <span className="pr-6">{displayValue}</span>
              <span className="pr-6">{displayValue}</span>
            </div>
          ) : (
            <div ref={textRef} className="text-xs whitespace-nowrap" style={{ color: 'inherit' }}>
              <span className={style.text}>{displayValue}</span>
            </div>
          )}
        </div>
      </div>
    </motion.td>
  )
}
