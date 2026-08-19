import { motion } from 'framer-motion'
import type { GuessEntry } from '../types'
import Avatar from './Avatar'
import ComparisonRow from './ComparisonRow'

interface GuessCardProps {
  entry: GuessEntry
  index: number
}

const fieldLabels: Record<string, string> = {
  name: '名称', region: '地区', group: '团体', debutYear: '出道',
  gender: '性别', status: '状态', hairColor: '发色', fanName: '粉丝',
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
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', stiffness: 260, damping: 24 }}
      className={`card p-4 mb-2 ${entry.correct ? 'border-exact/40 shadow-[0_0_20px_rgba(74,222,128,0.15)]' : ''}`}
    >
      {/* 头部：头像 + 名字 + 第 N 次 */}
      <div className="flex items-center gap-3 mb-3">
        <Avatar name={entry.vtuberName} size="md" />
        <div className="flex-1 min-w-0">
          <div className="font-medium text-text-primary truncate">{entry.vtuberName}</div>
          <div className="text-text-muted text-xs">第 {entry.attemptNumber} 次猜测</div>
        </div>
        {entry.correct && (
          <span className="text-exact text-sm font-bold">✓ 命中</span>
        )}
      </div>

      {/* 属性对比网格 */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
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
