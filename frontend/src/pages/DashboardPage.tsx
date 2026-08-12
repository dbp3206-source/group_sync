import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { getGroups, type GroupSummary } from '../api/groups'
import { getBadmintonSeasons } from '../api/badminton'
import { getGroupDashboard, type Dashboard } from '../api/dashboard'

function DashboardPage() {
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [groupId, setGroupId] = useState<number | null>(null)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [error, setError] = useState('')

  useEffect(() => { getGroups().then((items) => { setGroups(items); setGroupId(items[0]?.id ?? null) }).catch((e) => setError(getApiErrorMessage(e, 'Could not load groups.'))) }, [])
  useEffect(() => {
    if (!groupId) return
    getBadmintonSeasons(groupId).then((seasons) => seasons[0] && getGroupDashboard(groupId, seasons[0].id)).then((data) => data && setDashboard(data)).catch((e) => setError(getApiErrorMessage(e, 'Could not load dashboard.')))
  }, [groupId])

  return <section>
    <div className="page-heading"><div><p className="eyebrow">Group overview</p><h1>Dashboard</h1></div><select value={groupId ?? ''} onChange={(e) => setGroupId(Number(e.target.value))}>{groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select></div>
    {error && <div className="alert alert-danger">{error}</div>}
    {!dashboard && !error && <div className="page-panel empty-state">Choose a group to load its activities.</div>}
    {dashboard && <div className="content-grid">
      <div className="page-panel"><div className="section-title">Next activities</div><p className="hint">{dashboard.registrationCount} registrations across upcoming sessions.</p>{dashboard.nextActivities.map((item) => <div className="member-row" key={item.sessionId}><strong>{item.title}</strong><span>{new Date(item.start).toLocaleString()} · {item.status}</span></div>)}</div>
      <div className="page-panel"><div className="section-title">Leaderboard</div>{dashboard.leaderboard.map((item) => <div className="member-row" key={item.userId}><strong>{item.displayName}</strong><span>{item.points} pts · {item.wins}W/{item.losses}L</span></div>)}</div>
      <div className="page-panel"><div className="section-title">Recent results</div>{dashboard.recentMatches.map((match) => <div className="member-row" key={match.id}><strong>Match #{match.id}</strong><span>{match.scoreA ?? '—'} : {match.scoreB ?? '—'} · {match.status}</span></div>)}</div>
      <div className="page-panel"><div className="section-title">News</div>{dashboard.news.map((item) => <div className="news-row" key={item.id}><strong>{item.title}</strong><span>{item.content}</span></div>)}</div>
    </div>}
  </section>
}

export default DashboardPage
