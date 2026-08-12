const EMAIL_HISTORY_KEY = 'groupsync.email-history'
const MAX_EMAILS = 5

export const DEMO_EMAIL = 'demo.organizer@groupsync.local'

export function getEmailHistory() {
  if (typeof window === 'undefined') return []
  try {
    const value = JSON.parse(window.localStorage.getItem(EMAIL_HISTORY_KEY) ?? '[]')
    return Array.isArray(value) ? value.filter((email): email is string => typeof email === 'string') : []
  } catch {
    return []
  }
}

export function rememberEmail(email: string) {
  if (typeof window === 'undefined') return
  const normalized = email.trim().toLowerCase()
  if (!normalized) return
  const history = [normalized, ...getEmailHistory().filter((item) => item !== normalized)].slice(0, MAX_EMAILS)
  window.localStorage.setItem(EMAIL_HISTORY_KEY, JSON.stringify(history))
}
