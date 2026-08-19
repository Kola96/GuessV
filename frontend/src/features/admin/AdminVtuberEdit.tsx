import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import adminApi from '../../services/adminApi'
import type { Vtuber } from '../../types'

export default function AdminVtuberEdit() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [vtuber, setVtuber] = useState<Vtuber | null>(null)
  const [editing, setEditing] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    if (!id) return
    adminApi.get<Vtuber>(`/vtuber/${id}`).then(r => {
      setVtuber(r.data)
      // 初始化编辑字段为当前值
      const init: Record<string, string> = {}
      const fields = ['nameCn', 'nameEn', 'nameJp', 'region', 'groupName', 'debutYear', 'birthday', 'gender', 'activityStatus', 'hairColor', 'fanName', 'representativeColor', 'followerCount', 'platforms', 'languages']
      fields.forEach(f => {
        const val = (r.data as unknown as Record<string, unknown>)[f]
        init[f] = Array.isArray(val) ? val.join(', ') : (val as string || '')
      })
      setEditing(init)
    }).catch(() => {})
  }, [id])

  const handleSave = async () => {
    if (!vtuber) return
    setSaving(true)
    setMsg('')
    try {
      const fields: Record<string, unknown> = {}
      const arrayFields = ['hairColor', 'platforms', 'languages']
      Object.entries(editing).forEach(([key, value]) => {
        if (arrayFields.includes(key)) {
          fields[key] = value.split(',').map(s => s.trim()).filter(s => s)
        } else if (key === 'debutYear' || key === 'followerCount') {
          fields[key] = value ? parseInt(value) : null
        } else {
          fields[key] = value || null
        }
      })
      await adminApi.put(`/vtuber/${vtuber.id}/edit`, { fields, lockFields: true })
      setMsg('保存成功（字段已自动锁定）')
      // 重新加载
      const resp = await adminApi.get<Vtuber>(`/vtuber/${vtuber.id}`)
      setVtuber(resp.data)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '保存失败')
    }
    setSaving(false)
  }

  const unlockField = async (field: string) => {
    if (!vtuber) return
    try {
      await adminApi.post(`/vtuber/${vtuber.id}/unlock`, { fields: [field] })
      const resp = await adminApi.get<Vtuber>(`/vtuber/${vtuber.id}`)
      setVtuber(resp.data)
    } catch (e) {
      alert(e instanceof Error ? e.message : '解锁失败')
    }
  }

  if (!vtuber) return <div className="text-text-muted p-4">加载中...</div>

  const lockedFields = vtuber.lockedFields || []
  const formFields = [
    { key: 'nameCn', label: '中文名' },
    { key: 'nameEn', label: '英文名' },
    { key: 'nameJp', label: '日文名' },
    { key: 'region', label: '地区' },
    { key: 'groupName', label: '团体' },
    { key: 'debutYear', label: '出道年份' },
    { key: 'birthday', label: '生日 (MM-DD)' },
    { key: 'gender', label: '性别' },
    { key: 'activityStatus', label: '活动状态' },
    { key: 'hairColor', label: '发色 (逗号分隔)' },
    { key: 'fanName', label: '粉丝名' },
    { key: 'representativeColor', label: '代表色 (HEX)' },
    { key: 'followerCount', label: '粉丝量' },
    { key: 'platforms', label: '平台 (逗号分隔)' },
    { key: 'languages', label: '语言 (逗号分隔)' },
  ]

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/admin/vtuber')} className="btn-secondary text-sm">← 返回</button>
        <h1 className="text-xl font-bold text-text-primary">
          {vtuber.nameCn || vtuber.nameEn || `#${vtuber.id}`}
        </h1>
        <span className={`px-2 py-0.5 rounded text-xs ${vtuber.dataStatus === 'active' ? 'bg-primary/20 text-primary' : 'bg-white/5 text-text-muted'}`}>
          {vtuber.dataStatus}
        </span>
      </div>

      <div className="card p-6 max-w-2xl">
        <div className="grid grid-cols-2 gap-4">
          {formFields.map(f => (
            <div key={f.key}>
              <label className="text-xs text-text-secondary flex items-center gap-1 mb-1">
                {f.label}
                {lockedFields.includes(f.key) && (
                  <button
                    onClick={() => unlockField(f.key)}
                    className="text-partial hover:text-exact text-xs"
                    title="点击解锁此字段"
                  >🔒</button>
                )}
              </label>
              <input
                className="input-field text-sm"
                value={editing[f.key] || ''}
                onChange={(e) => setEditing(prev => ({ ...prev, [f.key]: e.target.value }))}
              />
            </div>
          ))}
        </div>

        {msg && <p className="text-sm mt-4 text-exact">{msg}</p>}

        <button
          className="btn-primary mt-6"
          disabled={saving}
          onClick={handleSave}
        >
          {saving ? '保存中...' : '保存（自动锁定修改字段）'}
        </button>
      </div>
    </div>
  )
}
