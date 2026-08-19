interface AvatarProps {
  name: string
  color?: string
  size?: 'sm' | 'md' | 'lg'
}

// 从名字生成一个稳定的颜色
function colorFor(name: string): string {
  const colors = [
    '#c084fc', '#f472b6', '#60a5fa', '#34d399',
    '#fbbf24', '#f87171', '#a78bfa', '#22d3ee',
  ]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

export default function Avatar({ name, color, size = 'md' }: AvatarProps) {
  const initial = name?.charAt(0)?.toUpperCase() || '?'
  const bg = color || colorFor(name || '?')

  const sizes = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-12 h-12 text-base',
    lg: 'w-16 h-16 text-xl',
  }

  return (
    <div
      className={`${sizes[size]} rounded-full flex items-center justify-center font-bold text-white shrink-0`}
      style={{ background: `linear-gradient(135deg, ${bg}, ${bg}99)` }}
    >
      {initial}
    </div>
  )
}
