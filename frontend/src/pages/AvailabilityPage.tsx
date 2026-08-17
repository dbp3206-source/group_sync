import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { searchAvailability, type AvailabilityCandidate } from '../api/availability'
import { getApiErrorMessage } from '../api/errors'
import { getGroup, type GroupDetail } from '../api/groups'
import { useAuth } from '../auth/AuthContext'

function toInputValue(value: Date) {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`
}

function defaultSearchRange() {
  const from = new Date()
  from.setMinutes(0, 0, 0)
  from.setHours(Math.max(from.getHours() + 1, 8))
  const to = new Date(from)
  to.setDate(to.getDate() + 7)
  to.setHours(22)
  return { from: toInputValue(from), to: toInputValue(to) }
}

function AvailabilityPage() {
  const { groupId } = useParams()
  const { user } = useAuth()
  const defaults = useMemo(defaultSearchRange, [])
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [from, setFrom] = useState(defaults.from)
  const [to, setTo] = useState(defaults.to)
  const [duration, setDuration] = useState('90')
  const [minimumAttendance, setMinimumAttendance] = useState(1)
  const [strategy, setStrategy] = useState('MAXIMUM')
  const [requiredMemberIds, setRequiredMemberIds] = useState<number[]>([])
  const [candidates, setCandidates] = useState<AvailabilityCandidate[]>([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!groupId) return
    getGroup(Number(groupId)).then((nextGroup) => {
      setGroup(nextGroup)
      setMinimumAttendance(Math.min(Math.max(1, nextGroup.members.length), 2))
    }).catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải nhóm.')))
  }, [groupId])

  const currentRole = group?.members.find((member) => member.userId === user?.id)?.role
  const canManage = currentRole === 'OWNER' || currentRole === 'ORGANIZER'

  function toggleRequired(userId: number) {
    setRequiredMemberIds((current) => current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId])
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!group) return
    setError('')
    setMessage('')
    try {
      const results = await searchAvailability(group.id, {
        from: new Date(from).toISOString(), to: new Date(to).toISOString(), durationMinutes: Number(duration), requiredMemberIds,
        minimumAttendance: Math.max(minimumAttendance, requiredMemberIds.length), strategy,
      })
      setCandidates(results)
      setMessage(results.length ? `Đã tìm thấy ${results.length} khung giờ phù hợp nhất.` : 'Chưa tìm thấy khung giờ đạt điều kiện. Hãy mở rộng khoảng tìm kiếm hoặc giảm số người tối thiểu.')
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể tìm lịch chung.')) }
  }

  if (!group) return <div className="page-panel">{error || 'Đang mở công cụ tìm lịch chung…'}</div>
  const activityPath = group.type === 'BADMINTON' ? '/badminton' : '/study'
  const activityName = group.type === 'BADMINTON' ? 'buổi chơi' : 'buổi học'
  const memberName = (userId: number) => group.members.find((member) => member.userId === userId)?.displayName ?? 'Thành viên'

  return <section className={`availability-page group-workspace--${group.type.toLowerCase()}`}>
    <Link className="back-link" to={`/groups/${group.id}`}>← {group.name}</Link>
    <header className="availability-hero"><div><p className="eyebrow">TÌM LỊCH CHUNG</p><h1>Chốt một khung giờ có cơ sở.</h1><p>GroupSync đối chiếu lịch bận của mọi thành viên trong nhóm. Chỉ organizer nhìn thấy kết quả tổng hợp này để bảo vệ riêng tư.</p></div><span>{group.members.length} thành viên</span></header>
    {(error || message) && <div className={`status-card ${error ? 'status-card--error' : 'status-card--success'}`} role={error ? 'alert' : 'status'}>{error || message}</div>}
    {!canManage ? <div className="availability-restricted"><h2>Bạn không có quyền tìm lịch cho cả nhóm.</h2><p>Owner hoặc organizer có thể xem mức độ sẵn sàng tổng hợp và dùng kết quả để tạo {activityName}.</p></div> : <div className="availability-layout"><form className="availability-form form-stack" onSubmit={submit}><div><p className="eyebrow">ĐIỀU KIỆN</p><h2>Thiết lập tìm kiếm</h2></div><label>Từ thời điểm<input type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} required /></label><label>Đến thời điểm<input type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} required /></label><div className="two-fields"><label>Thời lượng<select value={duration} onChange={(event) => setDuration(event.target.value)}><option value="30">30 phút</option><option value="60">1 giờ</option><option value="90">90 phút</option><option value="120">2 giờ</option></select></label><label>Tối thiểu có mặt<input type="number" min={Math.max(1, requiredMemberIds.length)} max={group.members.length} value={minimumAttendance} onChange={(event) => setMinimumAttendance(Number(event.target.value))} /></label></div><fieldset className="member-picker"><legend>Ai cần có mặt? <small>(tùy chọn)</small></legend>{group.members.map((member) => <label key={member.userId}><input type="checkbox" checked={requiredMemberIds.includes(member.userId)} onChange={() => toggleRequired(member.userId)} /><span className="avatar-fallback">{member.displayName.slice(0, 1).toUpperCase()}</span>{member.displayName}</label>)}</fieldset><label>Ưu tiên<select value={strategy} onChange={(event) => setStrategy(event.target.value)}><option value="MAXIMUM">Nhiều người rảnh nhất</option><option value="EARLIEST">Khung sớm nhất đạt điều kiện</option></select></label><button className="button button--primary">Tìm khung giờ</button></form><section className="availability-results"><div className="panel-heading"><div><p className="eyebrow">ĐỀ XUẤT</p><h2>Khung giờ nên chọn</h2></div><span>{candidates.length ? `${candidates.length} kết quả` : 'Chưa tìm'}</span></div>{!candidates.length ? <p className="panel-empty">Chọn khoảng thời gian, điều kiện có mặt, rồi GroupSync sẽ xếp hạng các khung giờ phù hợp.</p> : <div className="candidate-list">{candidates.slice(0, 12).map((candidate, index) => <article key={candidate.start} className="availability-candidate"><div><b>#{index + 1}</b><time>{new Date(candidate.start).toLocaleString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })} – {new Date(candidate.end).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</time><strong>{candidate.attendance}/{group.members.length} người có thể tham gia</strong><small>{candidate.availableMemberIds.map(memberName).join(' · ')}</small></div><Link className="button button--secondary" to={`${activityPath}?groupId=${group.id}&start=${encodeURIComponent(candidate.start)}&end=${encodeURIComponent(candidate.end)}`}>Dùng khung này</Link></article>)}</div>}</section></div>}
  </section>
}

export default AvailabilityPage
