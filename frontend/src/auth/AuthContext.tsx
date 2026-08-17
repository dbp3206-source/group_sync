import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import {
  getCsrfToken,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  type RegisterInput,
  type User,
} from '../api/auth'
import { rememberEmail } from './emailHistory'

type AuthContextValue = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  updateCurrentUser: (user: User) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCsrfToken()
      .then(() => getCurrentUser())
      .then(setUser)
      .catch((error: unknown) => {
        if (!axios.isAxiosError(error) || error.response?.status !== 401) {
          setUser(null)
        }
      })
      .finally(() => setLoading(false))
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    async login(email, password) {
      await getCsrfToken()
      setUser(await loginRequest(email, password))
      rememberEmail(email)
    },
    async register(input) {
      await getCsrfToken()
      setUser(await registerRequest(input))
      rememberEmail(input.email)
    },
    updateCurrentUser(updatedUser) {
      setUser(updatedUser)
    },
    async logout() {
      await getCsrfToken()
      await logoutRequest()
      setUser(null)
    },
  }), [loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
