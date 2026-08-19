import { motion } from 'framer-motion'
import type { GuessEntry } from '../types'
import ComparisonRow from './ComparisonRow'

interface GuessCardProps {
  entry: GuessEntry
  index: number
}

export const TABLE_COLUMNS = [
  { key: 'name', label: '名字' },
  { key: 'platforms', label: '平台' },
  { key: 'group', label: '团体' },
  { key: 'debutYear', label: '出道' },
  { key: 'birthday', label: '生日' },
  { key: 'gender', label: '性别' },
  { key: 'status', label: '状态' },
  { key: 'hairColor', label: '发色' },
  { key: 'languages', label: '语言' },
  { key: 'followerCount', label: '粉丝' },
]

export default function GuessCard({ entry, index }: GuessCardProps) {
  const c = entry.comparison
  const fields = [
    c.platforms, c.group, c.debutYear, c.birthday,
    c.gender, c.status, c.hairColor, c.languages, c.followerCount,
  ]

  return (
    <motion.tr
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ type: 'spring', stiffness: 300, damping: 26 }}
      className={entry.correct ? 'bg-exact/10' : ''}
    >
      {/* 名字列 */}
      <td className="px-2 py-1.5 border border-white/5 text-left sticky left-0 bg-surface/80 backdrop-blur-sm z-10">
        <div className="flex items-center gap-1.5">
          <div className="w-7 h-7 rounded-full bg-gradient-to-br from-primary/30 to-secondary/30 flex items-center justify-center text-xs font-bold shrink-0">
            {entry.vtuberName?.charAt(0)}
          </div>
          <div className="w-20 truncate text-xs font-medium text-text-primary">
            {entry.vtuberName}
          </div>
        </div>
      </td>

      {/* 属性列 */}
      {fields.map((f, i) => (
        <ComparisonRow key={i} field={f} index={i} />
      ))}
    </motion.tr>
  )
}
