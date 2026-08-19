import { useEffect, useState } from 'react'
import { getInsights, type InsightOverview } from '../api/knowledge'

export default function KnowledgeInsightsPage() {
  const [data, setData] = useState<InsightOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getInsights()
      .then(setData)
      .catch(() => setError('Insights could not be loaded.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <section className="kos-page kos-insights">
      <p className="kos-kicker">INSIGHTS</p>
      <h1>See your learning library take shape.</h1>

      {loading ? (
        <div className="kos-empty" aria-live="polite">Loading your library signal…</div>
      ) : error ? (
        <p className="kos-error" role="alert">{error}</p>
      ) : !data ? (
        <div className="kos-empty">No insight data available yet.</div>
      ) : (
        <>
          <div className="kos-insight-numbers">
            <div>
              <b>{data.totalResources}</b>
              <span>resources collected</span>
            </div>
            <div>
              <b>{data.readyResources}</b>
              <span>ready to question</span>
            </div>
            <div>
              <b>{data.inProgressResources}</b>
              <span>in progress</span>
            </div>
            <div>
              <b>{data.completedResources}</b>
              <span>completed</span>
            </div>
          </div>
          <div className="kos-composition">
            <h2>Library composition</h2>
            {data.composition && data.composition.length
              ? data.composition.map(item => (
                  <div key={item.resourceType}>
                    <span>{item.resourceType}</span>
                    <b>{item.count}</b>
                  </div>
                ))
              : <p>No resources yet.</p>}
          </div>
        </>
      )}
    </section>
  )
}
