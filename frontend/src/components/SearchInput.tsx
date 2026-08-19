import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { gameApi } from '../services/game'
import type { VtuberSearchResult } from '../types'

interface SearchInputProps {
  onGuess: (vtuber: VtuberSearchResult) => void
  disabled?: boolean
  placeholder?: string
}

export default function SearchInput({ onGuess, disabled, placeholder = '输入 VTuber 名字...' }: SearchInputProps) {
  const [keyword, setKeyword] = useState('')
  const [results, setResults] = useState<VtuberSearchResult[]>([])
  const [showResults, setShowResults] = useState(false)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)  // 是否已执行过搜索（区分"未搜索"和"搜索结果为空"）
  const [shake, setShake] = useState(false)
  const debounceRef = useRef<ReturnType<typeof setTimeout>>()
  const blurTimerRef = useRef<ReturnType<typeof setTimeout>>()
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (keyword.trim().length < 1) {
      setResults([])
      setSearched(false)
      return
    }
    setLoading(true)
    debounceRef.current = setTimeout(async () => {
      try {
        const data = await gameApi.search(keyword.trim(), 8)
        setResults(data)
        setShowResults(true)
        setSearched(true)
      } catch {
        setResults([])
        setSearched(true)
      } finally {
        setLoading(false)
      }
    }, 250)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [keyword])

  // 用 mousedown 阻止 input blur，确保点击下拉项时不会先关闭下拉
  const handleSelect = (v: VtuberSearchResult) => {
    onGuess(v)
    setKeyword('')
    setResults([])
    setShowResults(false)
    setSearched(false)
  }

  const handleBlur = () => {
    // 延迟关闭，让 mousedown 有机会触发；如果点击在下拉项上，mousedown 会阻止 blur
    blurTimerRef.current = setTimeout(() => {
      setShowResults(false)
    }, 200)
  }

  const handleFocus = () => {
    if (blurTimerRef.current) clearTimeout(blurTimerRef.current)
    if (results.length > 0) setShowResults(true)
  }

  // 回车不做任何事——必须点击候选项才能提交，避免误选第一个
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      // 输入了但没匹配到 → 抖动提示
      if (searched && results.length === 0 && keyword.trim()) {
        setShake(true)
        setTimeout(() => setShake(false), 500)
      }
    }
    if (e.key === 'Escape') {
      setShowResults(false)
    }
  }

  return (
    <div className="relative" ref={containerRef}>
      <motion.div animate={shake ? { x: [0, -6, 6, -4, 4, 0] } : {}}>
        <input
          className="input-field"
          placeholder={placeholder}
          value={keyword}
          disabled={disabled}
          onChange={(e) => setKeyword(e.target.value)}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          autoComplete="off"
        />
        {loading && (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted text-xs">
            搜索中...
          </span>
        )}
      </motion.div>

      <AnimatePresence>
        {showResults && results.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            className="absolute z-30 w-full bottom-full mb-1 card overflow-hidden max-h-72 overflow-y-auto"
          >
            {results.map((v) => (
              <button
                key={v.id}
                type="button"
                className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-elevated active:bg-elevated transition-colors text-left border-b border-white/5 last:border-0"
                // mousedown 在 blur 之前触发，确保点击能注册
                onMouseDown={(e) => {
                  e.preventDefault()  // 阻止 input blur
                  handleSelect(v)
                }}
              >
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary/40 to-secondary/40 flex items-center justify-center text-sm font-bold shrink-0">
                  {v.name?.charAt(0)}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-text-primary text-sm truncate">{v.name}</div>
                  {v.groupName && (
                    <div className="text-text-muted text-xs truncate">{v.groupName}</div>
                  )}
                </div>
                {v.region && (
                  <span className="text-text-muted text-xs shrink-0">{v.region}</span>
                )}
              </button>
            ))}
          </motion.div>
        )}

        {/* 搜索了但无结果 */}
        {showResults && searched && results.length === 0 && keyword.trim() && !loading && (
          <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 4 }}
            className="absolute z-30 w-full bottom-full mb-1 card px-4 py-3 text-text-muted text-sm text-center"
          >
            未找到匹配的 VTuber
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
