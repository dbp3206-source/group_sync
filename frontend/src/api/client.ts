import axios from 'axios'

function getApiBaseUrl() {
  const configured = import.meta.env.VITE_API_URL
  if (!configured || typeof window === 'undefined') return configured || '/api'

  try {
    const apiUrl = new URL(configured, window.location.origin)
    const localHosts = new Set(['localhost', '127.0.0.1'])
    if (localHosts.has(window.location.hostname) && localHosts.has(apiUrl.hostname)) {
      apiUrl.hostname = window.location.hostname
    }
    return apiUrl.toString().replace(/\/$/, '')
  } catch {
    return configured
  }
}

export const apiClient = axios.create({
  baseURL: getApiBaseUrl(),
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
  headers: {
    Accept: 'application/json',
  },
})
