import api from './api'
import type {
  UserInitResponse, UserProfile, VtuberSearchResult,
} from '../types'

export const userApi = {
  init: (nickname: string | null, useRandom: boolean, fingerprint: string) =>
    api.post<UserInitResponse>('/user/init', {
      nickname,
      useRandomNickname: useRandom,
      deviceFingerprint: fingerprint,
    }).then(r => r.data),

  randomNicknames: (count = 5) =>
    api.get<string[]>(`/user/nickname/random?count=${count}`).then(r => r.data),

  checkNickname: (nickname: string) =>
    api.post<{ valid: boolean; reason: string }>(
      `/user/nickname/check?nickname=${encodeURIComponent(nickname)}`
    ).then(r => r.data),

  profile: () =>
    api.get<UserProfile>('/user/profile').then(r => r.data),

  changeNickname: (nickname: string) =>
    api.put<UserProfile>('/user/nickname', { nickname }).then(r => r.data),
}
