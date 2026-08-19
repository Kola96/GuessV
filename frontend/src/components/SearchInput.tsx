import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { gameApi } from '../services/game'
import type { VtuberSearchResult } from '../types'
import { ApiError } from '../services/api'

interface SearchInputProps {
  onGuess: (vtuber: VtuberSearchResult) => void
  disabled?: boolean
  placeholder?: string
}

export default function SearchInput({ onGuess, disabled, placeholder = '输入 VTuber 名字...' }: SearchInputProps) {
  const [keyword, setKeyword] = useState('')
  const [results, setResults] = useState<VtuberSearchResult[]>([])
  const [showResults, setShowResults] = useState(false)
  const [shake, setShake] = useState(false)
  const debounceRef = useRef<ReturnType<typeof setTimeout>>()

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (keyword.trim().length < 1) {
      setResults([])
      return
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const data = await gameApi.search(keyword.trim(), 8)
        setResults(data)
        setShowResults(true)
      } catch {
        setResults([])
      }
    }, 250)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [keyword])

  const handleSelect = (v: VtuberSearchResult) => {
    onGuess(v)
    setKeyword('')
    setResults([])
    setShowResults(false)
  }

  const handleError = () => {
    setShake(true)
    setTimeout(() => setShake(false), 500)
  }

  return (
    <div className="relative">
      <motion.div animate={shake ? { x: [0, -6, 6, -4, 4, 0] } : {}}>
        <input
          className="input-field"
          placeholder={placeholder}
          value={keyword}
          disabled={disabled}
          onChange={(e) => setKeyword(e.target.value)}
          onFocus={() => results.length > 0 && setShowResults(true)}
          onBlur={() => setTimeout(() => setShowResults(false), 150)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && results.length > 0) {
              handleSelect(results[0])
            }
          }}
        />
      </motion.div>

      <AnimatePresence>
        {showResults && results.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            className="absolute z-10 w-full mt-1 card overflow-hidden"
          >
            {results.map((v) => (
              <button
                key={v.id}
                className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-elevated transition-colors text-left border-b border-white/5 last:border-0"
                onMouseDown={() => handleSelect(v)}
              >
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary/40 to-secondary/40 flex items-center justify-center text-sm font-bold shrink-0">
                  {v.name?.charAt(0)}
                </div>
                <div className="min-w-0">
                  <div className="text-text-primary text-sm truncate">{v.name}</div>
                  {v.groupName && (
                    <div className="text-text-muted text-xs truncate">{v.groupName}</div>
                  )}
                </div>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
