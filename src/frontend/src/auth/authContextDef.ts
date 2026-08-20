import { createContext } from 'react'
import type { RegisterInput, User } from '../api/auth'

export type AuthConnectionState = 'BOOTSTRAPPING' | 'WAKING' | 'ONLINE' | 'UNAVAILABLE'

export type AuthContextValue = {
  user: User | null
  loading: boolean
  connectionState: AuthConnectionState
  retryBootstrap: () => Promise<void>
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  updateCurrentUser: (user: User) => void
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
