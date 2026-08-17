import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { getGroups, type GroupSummary } from '../api/groups'
import { getBadmintonSeasons } from '../api/badminton'
import { getGroupDashboard, type Dashboard } from '../api/dashboard'
import { getNotifications, type Notification } from '../api/notifications'
import { getStudySessions, type StudySession } from '../api/study'
import { useAuth } from '../auth/AuthContext'

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function DashboardPage() {
  const { user } = useAuth()
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [groupId, setGroupId] = useState<number | null>(null)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [studySessions, setStudySessions] = useState<StudySession[]>([])
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [error, setError] = useState('')
  const selectedGroup = groups.find((group) => group.id === groupId)

  useEffect(() => {
    Promise.all([getGroups(), getNotifications()])
      .then(([items, inbox]) => { setGroups(items); setGroupId(items[0]?.id ?? null); setNotifications(inbox) })
      .catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải trang chủ.')))
  }, [])

  useEffect(() => {
    if (!selectedGroup) return
    setDashboard(null)
    setStudySessions([])
    if (selectedGroup.type === 'BADMINTON') {
      getBadmintonSeasons(selectedGroup.id)
        .then((seasons) => seasons[0] ? getGroupDashboard(selectedGroup.id, seasons[0].id) : null)
        .then((data) => data && setDashboard(data))
        .catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải hoạt động cầu lông.')))
    }
    if (selectedGroup.type === 'STUDY') {
      getStudySessions(selectedGroup.id)
        .then(setStudySessions)
        .catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải hoạt động học tập.')))
    }
  }, [selectedGroup])

  const unread = notifications.filter((item) => !item.read).length
  const upcomingStudy = useMemo(() => studySessions
    .filter((session) => new Date(session.start) > new Date() && session.status !== 'CANCELLED')
    .sort((a, b) => a.start.localeCompare(b.start)).slice(0, 3), [studySessions])
  const nextActivities = selectedGroup?.type === 'BADMINTON' ? dashboard?.nextActivities ?? [] : upcomingStudy.map((session) => ({ sessionId: session.id, title: session.topic, start: session.start, end: session.end, status: session.status }))

  return <section className="home-page">
    <header className="home-hero">
      <div><p className="eyebrow">TRANG CHỦ</p><h1>Chào {user?.displayName.split(' ')[0]}.</h1><p>Đây là nhịp hoạt động của bạn hôm nay — lịch riêng, nhóm và những việc cần phản hồi.</p></div>
      <div className="home-actions"><Link className="button button--secondary" to="/calendar">Mở lịch của tôi</Link><Link className="button button--primary" to="/groups">Vào nhóm</Link></div>
    </header>
    {error && <div className="status-card status-card--error" role="alert">{error}</div>}
    <div className="home-overview">
      <Link className="overview-card overview-card--notice" to="/notifications"><span>Thông báo chưa đọc</span><strong>{unread}</strong><small>{unread === 0 ? 'Mọi thứ đã được cập nhật.' : 'Xem những điều cần bạn chú ý.'}</small></Link>
      <Link className="overview-card" to="/groups"><span>Nhóm của bạn</span><strong>{groups.length}</strong><small>{groups.length === 0 ? 'Tạo hoặc tham gia nhóm đầu tiên.' : 'Chuyển đổi giữa các không gian nhóm.'}</small></Link>
      <Link className="overview-card overview-card--schedule" to="/calendar"><span>Việc tiếp theo</span><strong>{nextActivities.length}</strong><small>{nextActivities.length === 0 ? 'Chưa có hoạt động sắp tới.' : 'Hoạt động trong nhóm đang chọn.'}</small></Link>
    </div>
    <div className="home-workspace-head"><div><p className="eyebrow">KHÔNG GIAN ĐANG XEM</p><h2>{selectedGroup?.name ?? 'Chọn một nhóm'}</h2></div>{groups.length > 0 && <label className="group-switcher" htmlFor="home-group"><span>Nhóm</span><select id="home-group" value={groupId ?? ''} onChange={(event) => setGroupId(Number(event.target.value))}>{groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select></label>}</div>
    {!selectedGroup ? <div className="home-empty"><h2>Bắt đầu từ một nhóm.</h2><p>Nhóm học tập và cầu lông sẽ biến lịch riêng thành hoạt động chung có tổ chức.</p><Link className="button button--primary" to="/groups">Khám phá nhóm</Link></div> : <div className="home-grid">
      <article className="home-panel home-panel--next"><div className="panel-heading"><div><p className="eyebrow">SẮP DIỄN RA</p><h2>Hoạt động kế tiếp</h2></div><Link to={selectedGroup.type === 'BADMINTON' ? '/badminton' : '/study'}>Xem tất cả</Link></div>{nextActivities.length === 0 ? <p className="panel-empty">Chưa có buổi hoạt động nào sắp tới trong nhóm này.</p> : <div className="activity-list">{nextActivities.map((item) => <div className="activity-row" key={item.sessionId}><time>{formatDateTime(item.start)}</time><div><strong>{item.title}</strong><span>{item.status}</span></div></div>)}</div>}</article>
      <article className="home-panel home-panel--groups"><div className="panel-heading"><div><p className="eyebrow">KHÔNG GIAN CỦA BẠN</p><h2>Nhóm gần đây</h2></div><Link to="/groups">Quản lý nhóm</Link></div><div className="compact-group-list">{groups.slice(0, 4).map((group) => <Link to={`/groups/${group.id}`} key={group.id}><span className={`group-type-mark group-type-mark--${group.type.toLowerCase()}`}>{group.type === 'BADMINTON' ? 'B' : 'S'}</span><span><strong>{group.name}</strong><small>{group.type === 'BADMINTON' ? 'Cầu lông' : 'Học tập'} · {group.role}</small></span></Link>)}</div></article>
      {selectedGroup.type === 'BADMINTON' && <article className="home-panel home-panel--leaderboard"><div className="panel-heading"><div><p className="eyebrow">BADMINTON</p><h2>Bảng xếp hạng</h2></div><Link to="/badminton">Chi tiết</Link></div>{dashboard?.leaderboard.length ? <ol className="leaderboard-list">{dashboard.leaderboard.slice(0, 5).map((item, index) => <li key={item.userId}><b>{index + 1}</b><span>{item.displayName}</span><strong>{item.points} đ</strong></li>)}</ol> : <p className="panel-empty">Kết quả đã xác nhận sẽ xuất hiện ở đây.</p>}</article>}
      {selectedGroup.type === 'STUDY' && <article className="home-panel home-panel--study"><div className="panel-heading"><div><p className="eyebrow">HỌC TẬP</p><h2>Giữ nhịp học đều</h2></div><Link to="/study">Mở nhóm học</Link></div><p className="panel-empty">Lịch buổi học, tài liệu và mục tiêu của nhóm được tập hợp trong một không gian.</p></article>}
      <article className="home-panel home-panel--notifications"><div className="panel-heading"><div><p className="eyebrow">HỘP THƯ</p><h2>Mới nhất</h2></div><Link to="/notifications">Mở thông báo</Link></div>{notifications.length ? <div className="compact-notice-list">{notifications.slice(0, 3).map((notice: Notification) => <div key={notice.id} className={!notice.read ? 'is-unread' : ''}><b aria-hidden="true"/><span>{notice.message}</span></div>)}</div> : <p className="panel-empty">Chưa có thông báo mới.</p>}</article>
    </div>}
  </section>
}

export default DashboardPage
