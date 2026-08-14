import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { acceptInvitation, createGroup, getGroups, getPendingInvitations, type GroupSummary, type GroupType, type Invitation } from '../api/groups'
import { getApiErrorMessage } from '../api/errors'

function GroupsPage() {
  const navigate = useNavigate()
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [isCreateOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState<GroupType>('STUDY')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function refresh() {
    const [groupData, invitationData] = await Promise.all([getGroups(), getPendingInvitations()])
    setGroups(groupData)
    setInvitations(invitationData)
  }
  useEffect(() => { refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải nhóm.'))) }, [])

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const group = await createGroup({ name, description, type })
      navigate(`/groups/${group.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tạo nhóm.'))
    } finally { setSaving(false) }
  }
  async function accept(invitationId: number) {
    setError('')
    try { await acceptInvitation(invitationId); await refresh() } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể nhận lời mời.')) }
  }

  return <section className="groups-page">
    <header className="groups-hero"><div><p className="eyebrow">KHÔNG GIAN NHÓM</p><h1>Nhóm của bạn.</h1><p>Chuyển từ lịch cá nhân sang nhịp làm việc chung — học tập có tổ chức, chơi cầu lông có kết quả.</p></div><button className="button button--primary" onClick={() => setCreateOpen(true)}>Tạo nhóm mới</button></header>
    {error && <div className="status-card status-card--error" role="alert">{error}</div>}
    {invitations.length > 0 && <section className="invitation-strip" aria-labelledby="invitations-title"><div><p className="eyebrow">LỜI MỜI</p><h2 id="invitations-title">Bạn có {invitations.length} lời mời đang chờ</h2></div><div className="invitation-actions">{invitations.map((invitation) => <div key={invitation.id}><span><strong>{invitation.groupName}</strong><small>Từ {invitation.inviterDisplayName}</small></span><button className="button button--secondary" onClick={() => accept(invitation.id)}>Tham gia</button></div>)}</div></section>}
    <div className="group-section-heading"><div><p className="eyebrow">TẤT CẢ KHÔNG GIAN</p><h2>{groups.length === 0 ? 'Bắt đầu với nhóm đầu tiên' : `${groups.length} nhóm đang hoạt động`}</h2></div></div>
    {groups.length === 0 ? <div className="groups-empty"><h2>Chưa có nhóm nào.</h2><p>Tạo nhóm học tập hoặc cầu lông để bắt đầu đồng bộ lịch và hoạt động.</p><button className="button button--primary" onClick={() => setCreateOpen(true)}>Tạo nhóm đầu tiên</button></div> : <div className="group-grid">{groups.map((group) => <Link className={`workspace-card workspace-card--${group.type.toLowerCase()}`} to={`/groups/${group.id}`} key={group.id}><span className="workspace-card__mark">{group.type === 'BADMINTON' ? 'B' : 'S'}</span><div className="workspace-card__meta"><span>{group.type === 'BADMINTON' ? 'Cầu lông' : 'Học tập'}</span><span>{group.role}</span></div><h2>{group.name}</h2><p>{group.description || (group.type === 'BADMINTON' ? 'Quản lý buổi chơi, thành viên và kết quả.' : 'Lập kế hoạch học, mục tiêu và tài liệu chung.')}</p><b>Mở không gian <span aria-hidden="true">→</span></b></Link>)}</div>}
    {isCreateOpen && <div className="modal-backdrop" role="presentation" onMouseDown={() => !saving && setCreateOpen(false)}><form className="create-group-modal form-stack" onSubmit={submit} onMouseDown={(event) => event.stopPropagation()}><div className="modal-heading"><div><p className="eyebrow">KHÔNG GIAN MỚI</p><h2>Tạo nhóm</h2></div><button type="button" className="modal-close" onClick={() => setCreateOpen(false)} aria-label="Đóng">×</button></div><p className="auth-copy">Chọn loại nhóm để GroupSync chuẩn bị đúng workflow cho bạn.</p><fieldset className="group-type-choice"><legend>Loại nhóm</legend><label className={type === 'STUDY' ? 'is-selected' : ''}><input type="radio" value="STUDY" checked={type === 'STUDY'} onChange={() => setType('STUDY')} /><b>Học tập</b><span>Buổi học, tài liệu và mục tiêu chung.</span></label><label className={type === 'BADMINTON' ? 'is-selected' : ''}><input type="radio" value="BADMINTON" checked={type === 'BADMINTON'} onChange={() => setType('BADMINTON')} /><b>Cầu lông</b><span>Buổi chơi, sân, trận đấu và ranking.</span></label></fieldset><label htmlFor="group-name">Tên nhóm<input id="group-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required autoFocus /></label><label htmlFor="group-description">Mô tả <small>(không bắt buộc)</small><textarea id="group-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} rows={3} /></label><div className="modal-actions"><button type="button" className="button button--secondary" onClick={() => setCreateOpen(false)} disabled={saving}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? 'Đang tạo…' : 'Tạo nhóm'}</button></div></form></div>}
  </section>
}

export default GroupsPage
