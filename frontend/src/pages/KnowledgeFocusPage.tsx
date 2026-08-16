import { ArrowUpRight, Sparkles } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getFocusNext, type FocusNext } from '../api/knowledge'

export default function KnowledgeFocusPage() {
  const [item, setItem] = useState<FocusNext | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getFocusNext()
      .then(setItem)
      .catch(() => setError('Focus could not be loaded.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <section className="kos-page kos-focus">
      <p className="kos-kicker">FOCUS</p>
      <h1>Make the next hour count.</h1>

      {loading ? (
        <div className="kos-empty" aria-live="polite">Finding your next resource…</div>
      ) : error ? (
        <p className="kos-error" role="alert">{error}</p>
      ) : item ? (
        <article className="kos-focus-stage">
          <Sparkles size={30} aria-hidden="true" />
          <p>{item.reason}</p>
          <h2>{item.title}</h2>
          <span>{item.progressPercent > 0 ? `${item.progressPercent}% read` : 'Ready when you are'}</span>
          {/* Navigate to the resource workspace rather than a dead button */}
          <Link
            className="kos-button kos-button--primary"
            to={`/library/${item.resourceId}`}
          >
            Start focus <ArrowUpRight size={16} aria-hidden="true" />
          </Link>
        </article>
      ) : (
        <div className="kos-empty">
          <Sparkles size={28} aria-hidden="true" />
          <h2>Your next resource will appear here.</h2>
          <p>Resources become focus candidates as soon as their processing is complete.</p>
          <Link className="kos-button" to="/library">
            Open library <ArrowUpRight size={16} aria-hidden="true" />
          </Link>
        </div>
      )}
    </section>
  )
}
