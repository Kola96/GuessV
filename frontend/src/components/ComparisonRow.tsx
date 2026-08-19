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
      <div className="overflow-hidden" style={{ minWidth: 0, maxWidth: '80px' }}>
        <div
          className="text-xs whitespace-nowrap"
          style={{
            animation: displayValue.length > 8 ? 'scroll-text 6s linear infinite' : 'none',
          }}
        >
          {displayValue}
        </div>
      </div>
    </motion.div>
  )
}
