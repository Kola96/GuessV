import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import adminApi from '../../services/adminApi'

interface DashboardData {
  raw: number
  candidate: number
  active: number
  verified: number
  total: number
}

export default function AdminDashboard() {
  const [data, setData] = useState<DashboardData | null>(null)

  useEffect(() => {
    adminApi.get<DashboardData>('/dashboard').then(r => setData(r.data)).catch(() => {})
  }, [])

  const cards = data ? [
    { label: '全量池 (raw)', value: data.raw, color: 'text-text-muted', bg: 'bg-white/5' },
    { label: '候选队列 (candidate)', value: data.candidate, color: 'text-partial', bg: 'bg-partial/10' },
    { label: '正式池 (active)', value: data.active, color: 'text-primary', bg: 'bg-primary/10' },
    { label: '已核实 (verified)', value: data.verified, color: 'text-exact', bg: 'bg-exact/10' },
  ] : []

  return (
    <div>
      <h1 className="text-xl font-bold text-text-primary mb-6">数据看板</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {cards.map((c, i) => (
          <motion.div
            key={c.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.08 }}
            className={`card p-5 ${c.bg}`}
          >
            <div className={`text-2xl font-bold ${c.color}`}>{c.value}</div>
            <div className="text-text-muted text-xs mt-1">{c.label}</div>
          </motion.div>
        ))}
      </div>

      {data && (
        <div className="card p-5">
          <div className="text-text-secondary text-sm mb-2">数据总览</div>
          <div className="text-3xl font-bold text-text-primary">{data.total}</div>
          <div className="text-text-muted text-xs mt-1">VTuber 总数</div>
        </div>
      )}
    </div>
  )
}
