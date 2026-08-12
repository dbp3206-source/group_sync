import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  changeMemberRole,
  getGroup,
  inviteUser,
  type GroupDetail,
  type GroupRole,
} from '../api/groups'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'

function GroupDetailPage() {
  const { groupId } = useParams()
  const { user } = useAuth()
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [inviteEmail, setInviteEmail] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function refresh() {
    if (groupId) setGroup(await getGroup(Number(groupId)))
  }

  useEffect(() => {
    refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Could not load the group.')))
  }, [groupId])

  if (!group) return <div className="page-panel">{error || 'Loading group…'}</div>
  const currentGroup = group

  const currentMember = currentGroup.members.find((member) => member.userId === user?.id)
  const canManage = currentMember?.role === 'OWNER' || currentMember?.role === 'ORGANIZER'

  async function invite(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      await inviteUser(currentGroup.id, inviteEmail)
      setInviteEmail('')
      setMessage('Invitation sent.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not send the invitation.'))
    }
  }

  async function promote(userId: number, role: GroupRole) {
    setError('')
    try {
      setGroup(await changeMemberRole(currentGroup.id, userId, role))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not update the member role.'))
    }
  }

  return (
    <section>
      <Link className="back-link" to="/groups">← Back to groups</Link>
      <div className="page-heading group-detail-heading">
        <div><p className="eyebrow">{currentGroup.type} group</p><h1>{currentGroup.name}</h1><p className="intro">{currentGroup.description || 'A shared space for your next activity.'}</p></div>
        <span className="member-count">{currentGroup.members.length} members</span>
      </div>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {message && <div className="alert alert-success" role="status">{message}</div>}
      <div className="content-grid">
        <div className="page-panel">
          <div className="section-title">Members</div>
          <div className="member-list">
            {currentGroup.members.map((member) => (
              <div className="member-row" key={member.userId}>
                <div><strong>{member.displayName}</strong><span>{member.email}</span></div>
                <div className="member-actions"><span className={`role-tag role-${member.role.toLowerCase()}`}>{member.role}</span>{canManage && currentMember?.role === 'OWNER' && member.role !== 'OWNER' && <select value={member.role} onChange={(event) => promote(member.userId, event.target.value as GroupRole)} aria-label={`Role for ${member.displayName}`}><option value="MEMBER">MEMBER</option><option value="ORGANIZER">ORGANIZER</option></select>}</div>
              </div>
            ))}
          </div>
        </div>
        {canManage && <form className="page-panel form-stack" onSubmit={invite}><div><p className="eyebrow">Grow the group</p><h2>Invite a member</h2></div><label>User email<input type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} required /></label><button className="btn btn-primary">Send invitation</button><p className="hint">The person must already have a GroupSync account.</p></form>}
      </div>
    </section>
  )
}

export default GroupDetailPage
