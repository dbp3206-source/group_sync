import axios from 'axios'

export const apiClient = axios.create({
  // Keep every browser request on the frontend origin. Vite proxies this path
  // locally and Vercel rewrites it to the Render backend in production, so
  // session and CSRF cookies are always first-party.
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
  headers: {
    Accept: 'application/json',
  },
})
