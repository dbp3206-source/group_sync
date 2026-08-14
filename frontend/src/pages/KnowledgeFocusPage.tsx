import { Sparkles } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getFocusNext, type FocusNext } from '../api/knowledge'

export default function KnowledgeFocusPage() {
  const [item, setItem] = useState<FocusNext | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('')
  useEffect(() => { getFocusNext().then(setItem).catch(() => setError('Focus could not be loaded.')).finally(() => setLoading(false)) }, [])
  return <section className="kos-page kos-focus"><p className="kos-kicker">FOCUS</p><h1>Make the next hour count.</h1>{loading ? <div className="kos-empty">Finding your next resource...</div> : error ? <p className="kos-error">{error}</p> : item ? <article className="kos-focus-stage"><Sparkles size={30}/><p>{item.reason}</p><h2>{item.title}</h2><span>{item.progressPercent > 0 ? `${item.progressPercent}% read` : 'Ready when you are'}</span><button className="kos-button kos-button--primary">Start focus</button></article> : <div className="kos-empty"><Sparkles size={28}/><h2>Your next resource will appear here.</h2><p>Resources become focus candidates as soon as their processing is complete.</p></div>}</section>
}
