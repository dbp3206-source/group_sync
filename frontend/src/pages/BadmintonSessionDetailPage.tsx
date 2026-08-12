import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { getBadmintonSession, type BadmintonSession } from '../api/badminton'

function BadmintonSessionDetailPage() {
  const { sessionId } = useParams()
  const [session, setSession] = useState<BadmintonSession | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { if (sessionId) getBadmintonSession(Number(sessionId)).then(setSession).catch((e) => setError(getApiErrorMessage(e, 'Could not load the badminton session.'))) }, [sessionId])
  if (error) return <section><div className="alert alert-danger">{error}</div><Link to="/badminton">Back to badminton</Link></section>
  if (!session) return <div className="page-panel">Loading session…</div>
  return <section>
    <Link className="back-link" to={`/badminton?groupId=${session.groupId}`}>← Back to badminton</Link>
    <div className="page-heading"><div><p className="eyebrow">Badminton session detail</p><h1>{session.title}</h1><p className="intro">{new Date(session.start).toLocaleString()} – {new Date(session.end).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p></div><span className="source-tag source-badminton">{session.status}</span></div>
    <div className="content-grid"><div className="page-panel"><div className="section-title">Venue and courts</div><p>{session.venueName}</p><div className="participant-pills">{session.courts.map((court) => <span key={court.id}>{court.name}</span>)}</div></div><div className="page-panel"><div className="section-title">Registration</div><p>{session.registrations.filter((r) => r.status === 'REGISTERED' || r.status === 'CHECKED_IN').length} / {session.capacity} places</p><div className="participant-pills">{session.registrations.filter((r) => r.status !== 'CANCELLED').map((registration) => <span key={registration.id}>{registration.displayName} · {registration.status}</span>)}</div></div><div className="page-panel"><div className="section-title">Responsibilities</div>{session.responsibilities.length === 0 ? <p className="hint">No responsibilities added yet.</p> : session.responsibilities.map((item) => <div className="member-row" key={item.id}><strong>{item.itemName}</strong><span>{item.assigneeName || 'Unassigned'} · {item.status}</span></div>)}</div></div>
  </section>
}

export default BadmintonSessionDetailPage
