import { useEffect, useState } from 'react'
import axios from 'axios'
import { getHealth, type HealthResponse } from '../api/health'

type HealthState =
  | { status: 'loading' }
  | { status: 'success'; data: HealthResponse }
  | { status: 'error'; message: string }

function HealthPage() {
  const [health, setHealth] = useState<HealthState>({ status: 'loading' })

  useEffect(() => {
    getHealth()
      .then((data) => setHealth({ status: 'success', data }))
      .catch((error: unknown) => {
        const message = axios.isAxiosError(error)
          ? error.response?.data?.message ?? 'Backend is not reachable.'
          : 'Backend is not reachable.'
        setHealth({ status: 'error', message })
      })
  }, [])

  return (
    <section className="health-page" aria-labelledby="health-title">
      <p className="eyebrow">Phase 0 · connectivity</p>
      <h1 id="health-title">The foundation is ready to connect.</h1>
      <p className="intro">
        This small page calls the real Spring Boot endpoint through the Vite development proxy.
      </p>

      {health.status === 'loading' && (
        <div className="status-card" role="status">
          Checking backend connectivity…
        </div>
      )}

      {health.status === 'success' && (
        <div className="status-card status-card--success" role="status">
          <span className="status-dot" aria-hidden="true" />
          <div>
            <strong>Backend connected</strong>
            <p>
              {health.data.service} · {health.data.status}
            </p>
          </div>
        </div>
      )}

      {health.status === 'error' && (
        <div className="status-card status-card--error" role="alert">
          <strong>Backend unavailable</strong>
          <p>{health.message}</p>
          <p className="hint">Start the backend on port 8080, then refresh this page.</p>
        </div>
      )}
    </section>
  )
}

export default HealthPage

