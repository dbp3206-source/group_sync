import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
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
import { isWakeUpError } from '../api/errors'

const WAKE_UP_THRESHOLD_MS = 2500

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [connectionState, setConnectionState] = useState<'BOOTSTRAPPING' | 'WAKING' | 'ONLINE' | 'UNAVAILABLE'>('BOOTSTRAPPING')
  const bootstrapStarted = useRef(false)

  const runRequest = useCallback(async <T,>(request: () => Promise<T>) => {
    setConnectionState('BOOTSTRAPPING')
    const wakeTimer = window.setTimeout(() => {
      setConnectionState(current => current === 'BOOTSTRAPPING' ? 'WAKING' : current)
    }, WAKE_UP_THRESHOLD_MS)

    try {
      const result = await request()
      setConnectionState('ONLINE')
      return result
    } catch (error) {
      setConnectionState(isWakeUpError(error) ? 'UNAVAILABLE' : 'ONLINE')
      throw error
    } finally {
      window.clearTimeout(wakeTimer)
    }
  }, [])

  const bootstrap = useCallback(async () => {
    setLoading(true)
    try {
      const currentUser = await runRequest(async () => {
        await getCsrfToken()
        return getCurrentUser()
      })
      setUser(currentUser)
    } catch (error: unknown) {
      setUser(null)
      if (!axios.isAxiosError(error) || error.response?.status !== 401) {
        // The connection state carries the user-facing wake-up/unavailable explanation.
      }
    } finally {
      setLoading(false)
    }
  }, [runRequest])

  useEffect(() => {
    if (bootstrapStarted.current) return
    bootstrapStarted.current = true
    void bootstrap()
  }, [bootstrap])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    connectionState,
    retryBootstrap: bootstrap,
    async login(email, password) {
      const loggedInUser = await runRequest(async () => {
        await getCsrfToken()
        return loginRequest(email, password)
      })
      setUser(loggedInUser)
      rememberEmail(email)
    },
    async register(input) {
      const registeredUser = await runRequest(async () => {
        await getCsrfToken()
        return registerRequest(input)
      })
      setUser(registeredUser)
      rememberEmail(input.email)
    },
    updateCurrentUser(updatedUser) {
      setUser(updatedUser)
    },
    async logout() {
      await runRequest(async () => {
        await getCsrfToken()
        await logoutRequest()
      })
      setUser(null)
    },
  }), [bootstrap, connectionState, loading, runRequest, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
