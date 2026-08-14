import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { getGroups, type GroupSummary } from '../api/groups'
import { cancelStudySession, confirmStudySession, createStudySession, getStudySessions, joinStudySession, type StudySession } from '../api/study'
import { useAuth } from '../auth/AuthContext'

function localDateTime(value: string) {
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function StudyPage() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [groupId, setGroupId] = useState(Number(params.get('groupId') ?? 0))
  const [sessions, setSessions] = useState<StudySession[]>([])
  const [topic, setTopic] = useState('')
  const [goal, setGoal] = useState('')
  const [location, setLocation] = useState('')
  const [start, setStart] = useState(params.get('start') ? localDateTime(params.get('start')!) : '')
  const [end, setEnd] = useState(params.get('end') ? localDateTime(params.get('end')!) : '')
  const [capacity, setCapacity] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const studyGroups = groups.filter((group) => group.type === 'STUDY')
  const selectedGroup = groups.find((group) => group.id === groupId)
  const canManage = selectedGroup?.role === 'OWNER' || selectedGroup?.role === 'ORGANIZER'

  async function refresh() { if (groupId) setSessions(await getStudySessions(groupId)) }
  useEffect(() => { getGroups().then((items) => { setGroups(items); if (!groupId) setGroupId(items.find((item) => item.type === 'STUDY')?.id ?? 0) }).catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải nhóm học tập.'))) }, [])
  useEffect(() => { refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải các buổi học.'))) }, [groupId])

  async function create(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage('')
    try { await createStudySession(groupId, { topic, goal, location, start: new Date(start).toISOString(), end: new Date(end).toISOString(), capacity: capacity ? Number(capacity) : null }); setTopic(''); setGoal(''); setLocation(''); setCapacity(''); setMessage('Đã tạo buổi học ở trạng thái mở.'); await refresh() } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể tạo buổi học.')) }
  }
  async function act(action: () => Promise<StudySession>, success: string) { setError(''); setMessage(''); try { await action(); setMessage(success); await refresh() } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể cập nhật buổi học.')) } }

  return <section className="activity-page activity-page--study"><header className="activity-hero"><div><p className="eyebrow">STUDY WORKSPACE</p><h1>Học cùng nhau, đúng nhịp.</h1><p>Từ lịch chung đến buổi học được xác nhận: mọi thành viên nhìn thấy cùng một kế hoạch, không phải nhắn tin dò lịch.</p></div><div className="activity-hero__actions"><select value={groupId} onChange={(event) => setGroupId(Number(event.target.value))} aria-label="Chọn nhóm học tập">{studyGroups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select>{groupId && <Link className="button button--secondary" to={`/groups/${groupId}/availability`}>Tìm lịch chung</Link>}</div></header>
    {(error || message) && <div className={`status-card ${error ? 'status-card--error' : 'status-card--success'}`} role={error ? 'alert' : 'status'}>{error || message}</div>}
    {!groupId ? <div className="activity-empty"><h2>Bạn chưa có nhóm học tập.</h2><p>Tạo hoặc nhận lời mời vào một nhóm để lên lịch buổi học đầu tiên.</p><Link className="button button--primary" to="/groups">Đi tới nhóm</Link></div> : <div className="activity-layout"><section className="activity-feed"><div className="panel-heading"><div><p className="eyebrow">BUỔI HỌC</p><h2>Kế hoạch sắp tới</h2></div><span>{sessions.length} buổi</span></div>{sessions.length ? <div className="session-list">{sessions.map((session) => { const joined = session.participants.some((participant) => participant.userId === user?.id); return <article className="session-card" key={session.id}><div className="session-card__time"><time>{new Date(session.start).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })}</time><b>{new Date(session.start).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</b></div><div className="session-card__body"><div><span className={`activity-status activity-status--${session.status.toLowerCase()}`}>{session.status === 'OPEN' ? 'Đang mở' : session.status === 'CONFIRMED' ? 'Đã chốt' : 'Đã hủy'}</span><h3>{session.topic}</h3></div><p>{session.goal || 'Chưa có mục tiêu cụ thể.'}{session.location ? ` · ${session.location}` : ''}</p><small>{session.participants.length}{session.capacity ? `/${session.capacity}` : ''} người tham gia · kết thúc {new Date(session.end).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</small><div className="session-card__actions">{session.status === 'OPEN' && !joined && <button className="button button--secondary" onClick={() => act(() => joinStudySession(session.id), 'Bạn đã đăng ký tham gia buổi học.')}>Tham gia</button>}{session.status === 'OPEN' && canManage && <button className="button button--primary" onClick={() => act(() => confirmStudySession(session.id), 'Buổi học đã được chốt và đồng bộ vào lịch người tham gia.')}>Chốt buổi học</button>}{(session.status === 'OPEN' || session.status === 'CONFIRMED') && canManage && <button className="button button--danger" onClick={() => act(() => cancelStudySession(session.id), 'Buổi học đã hủy; lịch liên quan đã được gỡ.')}>Hủy</button>}</div></div></article> })}</div> : <p className="panel-empty">Chưa có buổi học nào. Hãy dùng form bên phải hoặc bắt đầu từ công cụ tìm lịch chung.</p>}</section>
      <aside className="activity-form"><form className="form-stack" onSubmit={create}><div><p className="eyebrow">TẠO BUỔI HỌC</p><h2>Thêm một kế hoạch rõ ràng</h2></div><label>Chủ đề<input value={topic} onChange={(event) => setTopic(event.target.value)} placeholder="Ví dụ: Ôn chương 4" required /></label><label>Mục tiêu<textarea value={goal} onChange={(event) => setGoal(event.target.value)} rows={2} placeholder="Kết quả cả nhóm cần hoàn thành" /></label><label>Địa điểm hoặc link<input value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Thư viện / Google Meet" /></label><label>Bắt đầu<input type="datetime-local" value={start} onChange={(event) => setStart(event.target.value)} required /></label><label>Kết thúc<input type="datetime-local" value={end} onChange={(event) => setEnd(event.target.value)} required /></label><label>Sức chứa <small>(tùy chọn)</small><input type="number" min="1" value={capacity} onChange={(event) => setCapacity(event.target.value)} /></label><button className="button button--primary" disabled={!canManage}>Tạo buổi học</button>{!canManage && <p className="form-note">Chỉ owner hoặc organizer có thể tạo và chốt buổi học.</p>}</form></aside></div>}
  </section>
}

export default StudyPage
