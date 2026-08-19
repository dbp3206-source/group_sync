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
  timeout: 120000,
})

// Bounded retry for idempotent GET requests during cold start (502, 503, network timeout)
apiClient.interceptors.response.use(
  response => response,
  async error => {
    const config = error.config
    if (!config) return Promise.reject(error)

    const isGet = (config.method || '').toLowerCase() === 'get'
    const status = error.response?.status
    const isGatewayError = status === 502 || status === 503 || error.code === 'ECONNABORTED' || !error.response

    config.__retryCount = config.__retryCount || 0
    const MAX_RETRIES = 2

    if (isGet && isGatewayError && config.__retryCount < MAX_RETRIES) {
      config.__retryCount += 1
      const delayMs = config.__retryCount * 2000
      await new Promise(resolve => setTimeout(resolve, delayMs))
      return apiClient(config)
    }

    if (error?.response?.status === 401 && window.location.pathname !== '/login' && window.location.pathname !== '/register') {
      window.location.replace('/login')
    }
    return Promise.reject(error)
  }
)
