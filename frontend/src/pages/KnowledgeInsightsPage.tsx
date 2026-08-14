import { useEffect, useState } from 'react'
import { getInsights, type InsightOverview } from '../api/knowledge'

export default function KnowledgeInsightsPage() {
  const [data, setData] = useState<InsightOverview | null>(null); const [error, setError] = useState('')
  useEffect(() => { getInsights().then(setData).catch(() => setError('Insights could not be loaded.')) }, [])
  return <section className="kos-page kos-insights"><p className="kos-kicker">INSIGHTS</p><h1>See your learning library take shape.</h1>{error ? <p className="kos-error">{error}</p> : !data ? <div className="kos-empty">Loading your library signal...</div> : <><div className="kos-insight-numbers"><div><b>{data.totalResources}</b><span>resources collected</span></div><div><b>{data.readyResources}</b><span>ready to question</span></div><div><b>{data.inProgressResources}</b><span>in progress</span></div></div><div className="kos-composition"><h2>Library composition</h2>{data.composition.length ? data.composition.map(item => <div key={item.resourceType}><span>{item.resourceType}</span><b>{item.count}</b></div>) : <p>No resources yet.</p>}</div></>}</section>
}
