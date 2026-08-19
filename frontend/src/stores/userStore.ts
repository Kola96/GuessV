import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserProfile } from '../types'

interface UserState {
  token: string | null
  profile: UserProfile | null
  setAuth: (token: string, profile: UserProfile) => void
  updateProfile: (profile: UserProfile) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: null,
      profile: null,
      setAuth: (token, profile) => set({ token, profile }),
      updateProfile: (profile) => set({ profile }),
      logout: () => set({ token: null, profile: null }),
    }),
    {
      name: 'guessv-user',
      partialize: (state) => ({ token: state.token, profile: state.profile }),
    }
  )
)
