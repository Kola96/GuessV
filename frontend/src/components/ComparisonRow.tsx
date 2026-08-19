import { motion } from 'framer-motion'
import type { FieldComparison, MatchType } from '../types'

const matchStyles: Record<MatchType, { bg: string; text: string; icon: string }> = {
  exact:  { bg: 'bg-exact/20 border-exact/40', text: 'text-exact', icon: '✓' },
  partial:{ bg: 'bg-partial/20 border-partial/40', text: 'text-partial', icon: '◐' },
  none:   { bg: 'bg-none/15 border-white/5', text: 'text-text-muted', icon: '✗' },
  higher: { bg: 'bg-none/15 border-white/5', text: 'text-partial', icon: '↑' },
  lower:  { bg: 'bg-none/15 border-white/5', text: 'text-partial', icon: '↓' },
}

interface ComparisonRowProps {
  label: string
  field: FieldComparison
  index: number
}

const PILL_MAX_WIDTH = 120
const SCROLL_THRESHOLD = 8

export default function ComparisonRow({ label, field, index }: ComparisonRowProps) {
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
  const needScroll = displayValue.length > SCROLL_THRESHOLD

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.06 }}
      className={`shrink-0 px-2 py-1 rounded-md border ${style.bg} flex items-center gap-1 overflow-hidden`}
      style={{ minWidth: 0 }}
    >
      <span className={`text-xs shrink-0 ${style.text}`}>
        {isArrow ? field.direction : style.icon}
      </span>
      <div className="overflow-hidden" style={{ minWidth: 0, maxWidth: `${PILL_MAX_WIDTH}px` }}>
        {needScroll ? (
          // 循环滚动：文字重复两份，translateX 0→-50% 无缝循环
          <div
            className="text-xs whitespace-nowrap inline-flex"
            style={{ animation: 'scroll-loop 8s linear infinite' }}
          >
            <span className="pr-8">{displayValue}</span>
            <span className="pr-8">{displayValue}</span>
          </div>
        ) : (
          <div className="text-xs whitespace-nowrap">
            {displayValue}
          </div>
        )}
      </div>
    </motion.div>
  )
}
