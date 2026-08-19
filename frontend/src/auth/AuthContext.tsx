import { useEffect, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import {
  getCsrfToken,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  type User,
} from '../api/auth'
import { rememberEmail } from './emailHistory'
import { AuthContext, type AuthContextValue } from './authContextDef'

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
