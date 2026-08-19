import { motion } from 'framer-motion'
import type { GuessEntry } from '../types'
import ComparisonRow from './ComparisonRow'

interface GuessCardProps {
  entry: GuessEntry
  index: number
}

export default function GuessCard({ entry, index }: GuessCardProps) {
  const c = entry.comparison
  const fields = [
    { key: 'platforms', label: '平台', field: c.platforms },
    { key: 'group', label: '团体', field: c.group },
    { key: 'debutYear', label: '出道', field: c.debutYear },
    { key: 'birthday', label: '生日', field: c.birthday },
    { key: 'gender', label: '性别', field: c.gender },
    { key: 'status', label: '状态', field: c.status },
    { key: 'hairColor', label: '发色', field: c.hairColor },
    { key: 'languages', label: '语言', field: c.languages },
    { key: 'followerCount', label: '粉丝', field: c.followerCount },
  ]

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 26 }}
      className={`flex items-center gap-1.5 px-3 py-2 rounded-lg border ${
        entry.correct
          ? 'bg-exact/10 border-exact/40 shadow-[0_0_15px_rgba(74,222,128,0.12)]'
          : 'bg-surface/60 border-white/5'
      }`}
    >
      {/* 名字：固定宽度，截断 */}
      <div className="shrink-0 w-24 truncate text-sm font-medium text-text-primary">
        {entry.vtuberName}
      </div>

      {/* 属性 pills：横向排列，可换行但紧凑 */}
      <div className="flex flex-wrap gap-1 flex-1" style={{ minWidth: 0 }}>
        {fields.map((f, i) => (
          <ComparisonRow
            key={f.key}
            label={f.label}
            field={f.field}
            index={i}
          />
        ))}
      </div>
    </motion.div>
  )
}
