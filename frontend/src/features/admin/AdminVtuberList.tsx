import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import adminApi from '../../services/adminApi'

interface VtuberListItem {
  id: number
  uuid: string
  nameCn: string | null
  nameEn: string | null
  groupName: string | null
  dataStatus: string
  dataSource: string | null
  lockedFields: string[] | null
  updatedAt: string | null
}

const statusLabels: Record<string, { label: string; color: string }> = {
  raw: { label: 'raw', color: 'text-text-muted bg-white/5' },
  candidate: { label: 'candidate', color: 'text-partial bg-partial/10' },
  active: { label: 'active', color: 'text-primary bg-primary/10' },
  verified: { label: 'verified', color: 'text-exact bg-exact/10' },
}

export default function AdminVtuberList() {
  const [records, setRecords] = useState<VtuberListItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const size = 20

  const fetchList = async () => {
    setLoading(true)
    try {
      const params: Record<string, string | number> = { page, size }
      if (statusFilter) params.status = statusFilter
      if (keyword) params.keyword = keyword
      const resp = await adminApi.get<{ records: VtuberListItem[]; total: number }>('/vtuber', { params })
      setRecords(resp.data.records)
      setTotal(resp.data.total)
    } catch {}
    setLoading(false)
  }

  useEffect(() => { fetchList() }, [page, statusFilter])

  const totalPages = Math.ceil(total / size)
  const statusOptions = ['', 'raw', 'candidate', 'active', 'verified']

  const promote = async (id: number, target: string) => {
    try {
      await adminApi.post(`/vtuber/${id}/promote`, { targetStatus: target })
      fetchList()
    } catch (e) {
      alert(e instanceof Error ? e.message : '操作失败')
    }
  }

  return (
    <div>
      <h1 className="text-xl font-bold text-text-primary mb-4">VTuber 管理</h1>

      {/* 筛选栏 */}
      <div className="flex gap-3 mb-4">
        <select
          className="input-field w-40"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(1) }}
        >
          {statusOptions.map(s => (
            <option key={s} value={s}>{s === '' ? '全部状态' : s}</option>
          ))}
        </select>
        <input
          className="input-field flex-1"
          placeholder="搜索名称..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setPage(1), fetchList())}
        />
        <button className="btn-primary" onClick={fetchList}>搜索</button>
      </div>

      {/* 表格 */}
      <div className="overflow-x-auto rounded-lg border border-white/5">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-elevated/60">
              <th className="px-3 py-2 text-xs text-text-secondary text-left border border-white/5">ID</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-left border border-white/5">名称</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-left border border-white/5">团体</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">状态</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">来源</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">锁定</th>
              <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">操作</th>
            </tr>
          </thead>
          <tbody>
            {records.map((v) => {
              const st = statusLabels[v.dataStatus] || statusLabels.raw
              const lockedCount = v.lockedFields?.length || 0
              return (
                <motion.tr
                  key={v.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="hover:bg-elevated/30"
                >
                  <td className="px-3 py-2 text-xs text-text-muted border border-white/5">{v.id}</td>
                  <td className="px-3 py-2 border border-white/5">
                    <Link to={`/admin/vtuber/${v.id}`} className="text-sm text-primary hover:underline">
                      {v.nameCn || v.nameEn || v.uuid.slice(0, 8)}
                    </Link>
                  </td>
                  <td className="px-3 py-2 text-xs text-text-secondary border border-white/5">{v.groupName || '—'}</td>
                  <td className="px-3 py-2 border border-white/5 text-center">
                    <span className={`px-2 py-0.5 rounded text-xs ${st.color}`}>{st.label}</span>
                  </td>
                  <td className="px-3 py-2 text-xs text-text-muted border border-white/5 text-center">{v.dataSource || '—'}</td>
                  <td className="px-3 py-2 text-xs text-center border border-white/5">
                    {lockedCount > 0 ? <span className="text-partial">🔒{lockedCount}</span> : '—'}
                  </td>
                  <td className="px-3 py-2 border border-white/5 text-center">
                    <div className="flex gap-1 justify-center">
                      {v.dataStatus === 'raw' && (
                        <button onClick={() => promote(v.id, 'candidate')} className="text-xs px-2 py-1 rounded bg-partial/20 text-partial hover:bg-partial/30">→候选</button>
                      )}
                      {v.dataStatus === 'candidate' && (
                        <button onClick={() => promote(v.id, 'active')} className="text-xs px-2 py-1 rounded bg-primary/20 text-primary hover:bg-primary/30">→正式</button>
                      )}
                      {v.dataStatus === 'active' && (
                        <button onClick={() => promote(v.id, 'verified')} className="text-xs px-2 py-1 rounded bg-exact/20 text-exact hover:bg-exact/30">→核实</button>
                      )}
                    </div>
                  </td>
                </motion.tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-4">
          <button
            className="btn-secondary text-sm"
            disabled={page <= 1}
            onClick={() => setPage(page - 1)}
          >上一页</button>
          <span className="text-text-muted text-sm">{page} / {totalPages}</span>
          <button
            className="btn-secondary text-sm"
            disabled={page >= totalPages}
            onClick={() => setPage(page + 1)}
          >下一页</button>
        </div>
      )}
    </div>
  )
}
