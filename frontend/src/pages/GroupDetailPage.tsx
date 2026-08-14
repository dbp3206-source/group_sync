import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { changeMemberRole, getGroup, inviteUser, type GroupDetail, type GroupRole } from '../api/groups'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'

function GroupDetailPage() {
  const { groupId } = useParams()
  const { user } = useAuth()
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [inviteEmail, setInviteEmail] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function refresh() { if (groupId) setGroup(await getGroup(Number(groupId))) }
  useEffect(() => { refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải nhóm.'))) }, [groupId])
  if (!group) return <div className="page-panel">{error || 'Đang mở không gian nhóm…'}</div>
  const currentGroup = group
  const currentMember = currentGroup.members.find((member) => member.userId === user?.id)
  const canManage = currentMember?.role === 'OWNER' || currentMember?.role === 'ORGANIZER'
  const moduleUrl = currentGroup.type === 'BADMINTON' ? `/badminton?groupId=${currentGroup.id}` : `/study?groupId=${currentGroup.id}`

  async function invite(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage('')
    try { await inviteUser(currentGroup.id, inviteEmail); setInviteEmail(''); setMessage('Đã gửi lời mời.') } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể gửi lời mời.')) }
  }
  async function promote(userId: number, role: GroupRole) {
    setError(''); setMessage('')
    try { setGroup(await changeMemberRole(currentGroup.id, userId, role)); setMessage('Vai trò thành viên đã được cập nhật.') } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể cập nhật vai trò.')) }
  }

  return <section className={`group-workspace group-workspace--${group.type.toLowerCase()}`}>
    <Link className="back-link" to="/groups">← Tất cả nhóm</Link>
    <header className="workspace-hero"><div className="workspace-hero__mark">{group.type === 'BADMINTON' ? 'B' : 'S'}</div><div><p className="eyebrow">{group.type === 'BADMINTON' ? 'NHÓM CẦU LÔNG' : 'NHÓM HỌC TẬP'}</p><h1>{group.name}</h1><p>{group.description || (group.type === 'BADMINTON' ? 'Không gian để tổ chức buổi chơi và theo dõi thành tích.' : 'Không gian để cùng học, đặt mục tiêu và theo dõi tiến độ.')}</p></div><div className="workspace-hero__actions"><span>{group.members.length} thành viên</span><Link className="button button--primary" to={moduleUrl}>{group.type === 'BADMINTON' ? 'Mở hoạt động cầu lông' : 'Mở buổi học'}</Link></div></header>
    {error && <div className="status-card status-card--error" role="alert">{error}</div>}{message && <div className="status-card status-card--success" role="status">{message}</div>}
    <div className="group-context-tabs"><Link className="is-active" to={`/groups/${group.id}`}>Tổng quan</Link><Link to={`/groups/${group.id}/availability`}>Tìm lịch chung</Link><Link to={moduleUrl}>{group.type === 'BADMINTON' ? 'Buổi chơi & trận đấu' : 'Buổi học'}</Link>{group.type === 'BADMINTON' && <Link to={`/tournaments?groupId=${group.id}`}>Tournament</Link>}</div>
    <div className="workspace-content-grid"><article className="members-panel"><div className="panel-heading"><div><p className="eyebrow">THÀNH VIÊN</p><h2>Những người trong nhóm</h2></div><span>{group.members.length} người</span></div><div className="member-roster">{group.members.map((member) => <div className="roster-row" key={member.userId}><span className="avatar-fallback">{member.displayName.slice(0, 1).toUpperCase()}</span><div><strong>{member.displayName}</strong><small>{member.role === 'OWNER' ? 'Chủ nhóm' : member.role === 'ORGANIZER' ? 'Điều phối' : 'Thành viên'}</small></div><div className="roster-actions">{canManage && currentMember?.role === 'OWNER' && member.role !== 'OWNER' ? <select value={member.role} onChange={(event) => promote(member.userId, event.target.value as GroupRole)} aria-label={`Vai trò của ${member.displayName}`}><option value="MEMBER">Thành viên</option><option value="ORGANIZER">Điều phối</option></select> : <span className={`role-badge role-badge--${member.role.toLowerCase()}`}>{member.role}</span>}</div></div>)}</div></article>
      <aside className="workspace-side">{canManage ? <form className="invite-panel form-stack" onSubmit={invite}><div><p className="eyebrow">MỜI THÀNH VIÊN</p><h2>Thêm người vào nhóm</h2></div><p className="auth-copy">Họ cần có tài khoản GroupSync trước khi nhận lời mời.</p><label htmlFor="invite-email">Email<input id="invite-email" type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} required /></label><button className="button button--secondary">Gửi lời mời</button></form> : <div className="workspace-tip"><p className="eyebrow">VAI TRÒ CỦA BẠN</p><h2>{currentMember?.role === 'MEMBER' ? 'Bạn là thành viên' : 'Bạn đang ở trong nhóm'}</h2><p>Hãy mở hoạt động của nhóm để xem lịch, đăng ký và các cập nhật liên quan.</p><Link className="button button--secondary" to={moduleUrl}>Xem hoạt động</Link></div>}</aside>
    </div>
  </section>
}

export default GroupDetailPage
