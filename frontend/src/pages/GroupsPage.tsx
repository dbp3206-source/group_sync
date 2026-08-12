import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  acceptInvitation,
  createGroup,
  getGroups,
  getPendingInvitations,
  type GroupSummary,
  type GroupType,
  type Invitation,
} from '../api/groups'
import { getApiErrorMessage } from '../api/errors'

function GroupsPage() {
  const navigate = useNavigate()
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
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

  useEffect(() => {
    refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Could not load groups.')))
  }, [])

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const group = await createGroup({ name, description, type })
      navigate(`/groups/${group.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not create the group.'))
    } finally {
      setSaving(false)
    }
  }

  async function accept(invitationId: number) {
    try {
      await acceptInvitation(invitationId)
      await refresh()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not accept the invitation.'))
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div><p className="eyebrow">Your workspace</p><h1>My groups</h1></div>
        <span className="subtle">{groups.length} group{groups.length === 1 ? '' : 's'}</span>
      </div>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="content-grid">
        <div>
          <div className="section-title">Current groups</div>
          {groups.length === 0 && <div className="page-panel empty-state">No groups yet. Create one or accept an invitation.</div>}
          <div className="group-list">
            {groups.map((group) => (
              <Link className="group-card" to={`/groups/${group.id}`} key={group.id}>
                <div className="group-card-top"><span className="type-tag">{group.type}</span><span className="role-tag">{group.role}</span></div>
                <h2>{group.name}</h2>
                <p>{group.description || 'No description yet.'}</p>
              </Link>
            ))}
          </div>
          {invitations.length > 0 && <>
            <div className="section-title">Pending invitations</div>
            <div className="invitation-list">
              {invitations.map((invitation) => (
                <div className="page-panel invitation-row" key={invitation.id}>
                  <div><strong>{invitation.groupName}</strong><span>Invited by {invitation.inviterDisplayName}</span></div>
                  <button className="btn btn-outline-primary" onClick={() => accept(invitation.id)}>Accept</button>
                </div>
              ))}
            </div>
          </>}
        </div>
        <form className="page-panel form-stack" onSubmit={submit}>
          <div><p className="eyebrow">New space</p><h2>Create a group</h2></div>
          <label>Name<input value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required /></label>
          <label>Description<textarea value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} rows={3} /></label>
          <label>Type<select value={type} onChange={(event) => setType(event.target.value as GroupType)}><option value="STUDY">Study</option><option value="BADMINTON">Badminton</option><option value="OTHER">Other</option></select></label>
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Creating…' : 'Create group'}</button>
        </form>
      </div>
    </section>
  )
}

export default GroupsPage
