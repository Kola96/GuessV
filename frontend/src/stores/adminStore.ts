import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AdminState {
  token: string | null
  setToken: (token: string) => void
  logout: () => void
}

export const useAdminStore = create<AdminState>()(
  persist(
    (set) => ({
      token: null,
      setToken: (token) => set({ token }),
      logout: () => set({ token: null }),
    }),
    { name: 'guessv-admin' }
  )
)
