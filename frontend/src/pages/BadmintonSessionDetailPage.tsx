import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { getBadmintonSession, type BadmintonSession } from '../api/badminton'
import { ClipboardCheck, MapPin, Users } from 'lucide-react'

const lifecycle = ['OPEN', 'CONFIRMED', 'CHECK-IN', 'PLAYING', 'COMPLETED'] as const

function lifecyclePosition(status: string) {
  if (status === 'DRAFT') return -1
  if (status === 'CONFIRMED') return 2
  return lifecycle.indexOf(status as (typeof lifecycle)[number])
}

function BadmintonSessionDetailPage() {
  const { sessionId } = useParams()
  const [session, setSession] = useState<BadmintonSession | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { if (sessionId) getBadmintonSession(Number(sessionId)).then(setSession).catch((e) => setError(getApiErrorMessage(e, 'Không thể tải buổi chơi cầu lông.'))) }, [sessionId])
  if (error) return <section><div className="alert alert-danger">{error}</div><Link to="/badminton">Quay lại cầu lông</Link></section>
  if (!session) return <div className="page-panel">Đang tải buổi chơi…</div>
  const progress = lifecyclePosition(session.status)
  const activeRegistrations = session.registrations.filter((registration) => registration.status === 'REGISTERED' || registration.status === 'CHECKED_IN')
  return <section className="session-detail-page">
    <Link className="back-link" to={`/badminton?groupId=${session.groupId}`}>← Quay lại workspace cầu lông</Link>
    <header className="session-detail-hero"><div><p className="eyebrow">BUỔI CHƠI CẦU LÔNG</p><h1>{session.title}</h1><p>{new Date(session.start).toLocaleString('vi-VN')} – {new Date(session.end).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</p></div><span className="source-tag source-badminton">{session.status}</span></header>
    <ol className="session-lifecycle" aria-label="Tiến trình buổi chơi">{lifecycle.map((step, index) => <li key={step} className={index <= progress ? 'is-complete' : ''} aria-current={index === progress ? 'step' : undefined}><b>{index + 1}</b><span>{step}</span></li>)}</ol>
    <div className="session-detail-grid"><article className="page-panel session-detail-panel"><div className="section-title"><MapPin size={18} /> Địa điểm & sân</div><h2>{session.venueName || 'Chưa chốt địa điểm'}</h2><div className="court-visuals">{session.courts.map((court) => <span key={court.id}><i aria-hidden="true" />{court.name}</span>)}</div></article><article className="page-panel session-detail-panel"><div className="section-title"><Users size={18} /> Danh sách đăng ký</div><p className="session-big-number">{activeRegistrations.length}<small>/{session.capacity} chỗ</small></p><div className="participant-pills">{session.registrations.filter((registration) => registration.status !== 'CANCELLED').map((registration) => <span key={registration.id}>{registration.displayName} · {registration.status}</span>)}</div></article><article className="page-panel session-detail-panel"><div className="section-title"><ClipboardCheck size={18} /> Phân công</div>{session.responsibilities.length === 0 ? <p className="hint">Chưa có nhiệm vụ nào. Organizer có thể phân công chuẩn bị cầu, nước hoặc dụng cụ.</p> : session.responsibilities.map((item) => <div className="member-row" key={item.id}><strong>{item.itemName}</strong><span>{item.assigneeName || 'Chưa phân công'} · {item.status}</span></div>)}</article></div>
  </section>
}

export default BadmintonSessionDetailPage
