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

// Redirect to /login on session expiry so the app never loops on 401 errors.
apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error?.response?.status === 401 && window.location.pathname !== '/login' && window.location.pathname !== '/register') {
      window.location.replace('/login')
    }
    return Promise.reject(error)
  }
)
