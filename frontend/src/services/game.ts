import api from './api'
import type {
  DailyGameInfo, GuessResponse, PoolVO,
  SingleStartResponse, VtuberSearchResult,
} from '../types'

export const gameApi = {
  // VTuber 搜索
  search: (keyword: string, limit = 10) =>
    api.get<VtuberSearchResult[]>(`/vtuber/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
      .then(r => r.data),

  // 每日模式
  dailyInfo: () =>
    api.get<DailyGameInfo>('/game/daily').then(r => r.data),

  dailyGuess: (vtuberId: number) =>
    api.post<GuessResponse>('/game/daily/guess', { vtuberId }).then(r => r.data),

  // 单人模式
  pools: () =>
    api.get<PoolVO[]>('/game/single/pools').then(r => r.data),

  singleStart: (poolTag: string) =>
    api.post<SingleStartResponse>('/game/single/start', { poolTag }).then(r => r.data),

  singleGuess: (sessionId: number, vtuberId: number) =>
    api.post<GuessResponse>('/game/single/guess', { sessionId, vtuberId }).then(r => r.data),

  singleState: (sessionId: number) =>
    api.get<DailyGameInfo>(`/game/single/${sessionId}`).then(r => r.data),
}
