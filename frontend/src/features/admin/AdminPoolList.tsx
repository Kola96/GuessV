import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import adminApi from '../../services/adminApi'
import type { AdminPool, Vtuber } from '../../types'

export default function AdminPoolList() {
  const [pools, setPools] = useState<AdminPool[]>([])
  const [selectedPool, setSelectedPool] = useState<AdminPool | null>(null)
  const [poolItems, setPoolItems] = useState<Vtuber[]>([])
  const [showCreate, setShowCreate] = useState(false)
  const [newPool, setNewPool] = useState({ name: '', description: '', market: 'cn', mode: 'daily' })
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<Vtuber[]>([])

  const fetchPools = async () => {
    try {
      const resp = await adminApi.get<AdminPool[]>('/pool')
      setPools(resp.data)
    } catch {}
  }

  const fetchItems = async (poolId: number) => {
    try {
      const resp = await adminApi.get<Vtuber[]>(`/pool/${poolId}/items`)
      setPoolItems(resp.data)
    } catch {}
  }

  useEffect(() => { fetchPools() }, [])

  const handleCreate = async () => {
    try {
      await adminApi.post('/pool', newPool)
      setShowCreate(false)
      setNewPool({ name: '', description: '', market: 'cn', mode: 'daily' })
      fetchPools()
    } catch (e) {
      alert(e instanceof Error ? e.message : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此题库？')) return
    try {
      await adminApi.delete(`/pool/${id}`)
      if (selectedPool?.id === id) {
        setSelectedPool(null)
        setPoolItems([])
      }
      fetchPools()
    } catch {}
  }

  const handleSelectPool = (pool: AdminPool) => {
    setSelectedPool(pool)
    fetchItems(pool.id)
  }

  const handleRemoveItem = async (vtuberId: number) => {
    if (!selectedPool) return
    try {
      await adminApi.delete(`/pool/${selectedPool.id}/items/${vtuberId}`)
      fetchItems(selectedPool.id)
    } catch {}
  }

  const handleSearch = async () => {
    if (!searchKeyword.trim()) return
    try {
      const resp = await adminApi.get<{ records: Vtuber[]; total: number }>('/vtuber', {
        params: { keyword: searchKeyword, size: 10 }
      })
      setSearchResults(resp.data.records || [])
    } catch {
      setSearchResults([])
    }
  }

  const handleAddItem = async (vtuberId: number) => {
    if (!selectedPool) return
    try {
      await adminApi.post(`/pool/${selectedPool.id}/items`, { vtuberIds: [vtuberId] })
      fetchItems(selectedPool.id)
      setSearchResults(searchResults.filter(v => v.id !== vtuberId))
    } catch {}
  }

  const modeLabels: Record<string, string> = { daily: '每日', single: '单人', multi: '对战' }
  const marketLabels: Record<string, string> = { cn: '中文', intl: '国际', all: '全市场' }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold text-text-primary">题库管理</h1>
        <button className="btn-primary text-sm" onClick={() => setShowCreate(!showCreate)}>+ 新建题库</button>
      </div>

      {/* 新建题库表单 */}
      {showCreate && (
        <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} className="card p-4 mb-4">
          <div className="grid grid-cols-2 gap-3">
            <input className="input-field text-sm" placeholder="题库名" value={newPool.name} onChange={e => setNewPool({ ...newPool, name: e.target.value })} />
            <input className="input-field text-sm" placeholder="描述" value={newPool.description} onChange={e => setNewPool({ ...newPool, description: e.target.value })} />
            <select className="input-field text-sm" value={newPool.market} onChange={e => setNewPool({ ...newPool, market: e.target.value })}>
              <option value="cn">中文市场</option>
              <option value="intl">国际市场</option>
              <option value="all">全市场</option>
            </select>
            <select className="input-field text-sm" value={newPool.mode} onChange={e => setNewPool({ ...newPool, mode: e.target.value })}>
              <option value="daily">每日挑战</option>
              <option value="single">单人模式</option>
              <option value="multi">对战模式</option>
            </select>
          </div>
          <button className="btn-primary mt-3 text-sm" onClick={handleCreate}>创建</button>
        </motion.div>
      )}

      <div className="flex gap-4">
        {/* 题库列表 */}
        <div className="w-64 shrink-0">
          <div className="space-y-2">
            {pools.map(p => (
              <div
                key={p.id}
                onClick={() => handleSelectPool(p)}
                className={`card p-3 cursor-pointer transition-colors ${selectedPool?.id === p.id ? 'border-primary/40 bg-primary/10' : 'hover:border-white/10'}`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-text-primary">{p.name}</span>
                  <button onClick={(e) => { e.stopPropagation(); handleDelete(p.id) }} className="text-text-muted hover:text-red-400 text-xs">删除</button>
                </div>
                <div className="flex gap-2 mt-1">
                  <span className="text-xs px-1.5 py-0.5 rounded bg-white/5 text-text-muted">{modeLabels[p.mode] || p.mode}</span>
                  <span className="text-xs px-1.5 py-0.5 rounded bg-white/5 text-text-muted">{marketLabels[p.market] || p.market}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 题库成员管理 */}
        {selectedPool ? (
          <div className="flex-1">
            <h2 className="text-lg font-bold text-text-primary mb-3">{selectedPool.name}（{poolItems.length} 位 V）</h2>

            {/* 搜索添加 */}
            <div className="flex gap-2 mb-4">
              <input
                className="input-field text-sm flex-1"
                placeholder="搜索 VTuber 添加到题库..."
                value={searchKeyword}
                onChange={e => setSearchKeyword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSearch()}
              />
              <button className="btn-secondary text-sm" onClick={handleSearch}>搜索</button>
            </div>

            {/* 搜索结果 */}
            {searchResults.length > 0 && (
              <div className="card p-2 mb-4">
                <div className="text-xs text-text-muted mb-2">搜索结果（点击添加）：</div>
                {searchResults.map(v => (
                  <button
                    key={v.id}
                    onClick={() => handleAddItem(v.id)}
                    className="w-full px-3 py-2 flex items-center gap-2 hover:bg-elevated rounded text-left"
                  >
                    <span className="text-sm text-text-primary">{v.nameCn || v.nameEn || v.id}</span>
                    {v.groupName && <span className="text-xs text-text-muted">{v.groupName}</span>}
                    <span className="ml-auto text-primary text-xs">+ 添加</span>
                  </button>
                ))}
              </div>
            )}

            {/* 成员列表 */}
            <div className="overflow-x-auto rounded-lg border border-white/5">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-elevated/60">
                    <th className="px-3 py-2 text-xs text-text-secondary text-left border border-white/5">名称</th>
                    <th className="px-3 py-2 text-xs text-text-secondary text-left border border-white/5">团体</th>
                    <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">市场</th>
                    <th className="px-3 py-2 text-xs text-text-secondary text-center border border-white/5">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {poolItems.map(v => (
                    <tr key={v.id} className="hover:bg-elevated/30">
                      <td className="px-3 py-2 text-sm text-text-primary border border-white/5">{v.nameCn || v.nameEn || v.id}</td>
                      <td className="px-3 py-2 text-xs text-text-secondary border border-white/5">{v.groupName || '—'}</td>
                      <td className="px-3 py-2 text-xs text-center text-text-muted border border-white/5">{v.market || '—'}</td>
                      <td className="px-3 py-2 text-center border border-white/5">
                        <button onClick={() => handleRemoveItem(v.id)} className="text-xs text-red-400 hover:text-red-300">移除</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center text-text-muted text-sm">← 选择一个题库查看成员</div>
        )}
      </div>
    </div>
  )
}
