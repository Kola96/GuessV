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
  const displayValue = Array.isArray(field.value)
    ? field.value.join(' / ')
    : (field.value as string) || '—'

  const isArrow = match === 'higher' || match === 'lower'

  return (
    <motion.div
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.08 }}
      className={`px-3 py-2 rounded-lg border ${style.bg} flex items-center justify-between gap-2`}
    >
      <div className="flex items-center gap-2 min-w-0">
        <span className="text-text-muted text-xs w-14 shrink-0">{label}</span>
        <span className={`text-sm truncate ${style.text}`}>{displayValue}</span>
      </div>
      <span className={`text-lg shrink-0 ${style.text}`}>
        {isArrow ? field.direction : style.icon}
      </span>
    </motion.div>
  )
}
