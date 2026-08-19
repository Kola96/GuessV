// ===== VTuber =====
export interface Vtuber {
  id: number
  name: string
  nameCn?: string
  nameEn?: string
  avatarUrl?: string
  groupName?: string
  region?: string
}

export interface VtuberSearchResult {
  id: number
  name: string
  nameCn?: string
  nameEn?: string
  avatarUrl?: string
  groupName?: string
  region?: string
}

// ===== 对比结果 =====
export type MatchType = 'exact' | 'partial' | 'none' | 'higher' | 'lower'

export interface FieldComparison {
  value: unknown
  match: MatchType
  direction?: string
}

export interface ComparisonResult {
  name: FieldComparison
  platforms: FieldComparison
  group: FieldComparison
  debutYear: FieldComparison
  birthday: FieldComparison
  gender: FieldComparison
  status: FieldComparison
  hairColor: FieldComparison
  languages: FieldComparison
  followerCount: FieldComparison
}

// ===== 游戏 =====
export interface GuessEntry {
  vtuberId: number
  vtuberName: string
  attemptNumber: number
  correct: boolean
  comparison: ComparisonResult
  guessedAt: string
}

export interface DailyGameInfo {
  date: string
  maxAttempts: number
  totalVtuberCount: number
  hasPlayed: boolean
  hasWon: boolean
  attemptsUsed: number
  guesses: GuessEntry[]
}

export interface GuessResponse {
  correct: boolean
  gameOver: boolean
  win: boolean
  remainingAttempts: number
  attemptsUsed: number
  comparison: ComparisonResult
  targetVtuber?: {
    id: number
    name: string
    avatarUrl?: string
  }
}

export interface PoolVO {
  tag: string
  description: string
  vtuberCount: number
}

export interface SingleStartResponse {
  sessionId: number
  maxAttempts: number
  poolTag: string
  vtuberCount: number
}

// ===== 用户 =====
export interface UserInitResponse {
  userId: string
  nickname: string
  gameId: string
  displayName: string
  token: string
  isAnonymous: boolean
}

export interface UserProfile {
  userId: string
  nickname: string
  gameId: string
  displayName: string
  isAnonymous: boolean
  username?: string
  avatarUrl?: string
  createdAt?: string
}

// ===== 统一响应 =====
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
