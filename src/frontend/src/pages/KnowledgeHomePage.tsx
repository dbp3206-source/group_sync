import { ArrowUpRight, BookOpenText, BrainCircuit, Clock3, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import hero from '../assets/knowledgeos-hero.png'
import { getRecentActivity, type RecentActivity } from '../api/knowledge'
import { useAuth } from '../auth'

function formatRelativeTime(value: string) {
  const occurredAt = new Date(value).getTime()
  const elapsedMinutes = Math.max(0, Math.floor((Date.now() - occurredAt) / 60000))
  if (elapsedMinutes < 1) return 'Just now'
  if (elapsedMinutes < 60) return `${elapsedMinutes} min ago`
  if (elapsedMinutes < 24 * 60) return `${Math.floor(elapsedMinutes / 60)} hr ago`
  if (elapsedMinutes < 48 * 60) return 'Yesterday'
  return new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

export default function KnowledgeHomePage() {
  const { user } = useAuth()
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [activityUnavailable, setActivityUnavailable] = useState(false)
  useEffect(() => {
    getRecentActivity().then(setActivities).catch(() => setActivityUnavailable(true))
  }, [])

  const lastActivity = activities[0]
  return <section className="kos-home">
    <div className="kos-home-copy"><p className="kos-kicker">PERSONAL KNOWLEDGE INTELLIGENCE</p><h1>Your knowledge,<br /><em>finally connected.</em></h1><p className="kos-lead">Collect what matters. Ask better questions. Return to the ideas worth your attention.</p><div className="kos-hero-actions"><Link className="kos-button kos-button--primary" to="/ask">Ask KnowledgeOS <ArrowUpRight size={17} /></Link><Link className="kos-text-link" to="/library">Open library</Link></div><p className="kos-greeting">Good to see you, {user?.displayName ? user.displayName.split(' ')[0] : 'friend'}.</p></div>
    <div className="kos-hero-art"><img src={hero} alt="Study notes, an e-reader, and a cobalt bookmark on a desk" /><div className="kos-art-caption">A quieter place to think.</div></div>
    <aside className="kos-resume-card">
      <span>RESUME YOUR LAST SESSION</span>
      {lastActivity ? <>
        <p className="kos-resume-time"><Clock3 size={14} /> {formatRelativeTime(lastActivity.occurredAt)}</p>
        <h2>{lastActivity.title}</h2>
        <p>{lastActivity.context}</p>
        <Link to={lastActivity.resumeUrl}>Resume session <ArrowUpRight size={16} /></Link>
        {activities.length > 1 && <div className="kos-recent-list" aria-label="Recent activity">
          {activities.slice(1, 4).map(activity => <Link key={`${activity.type}-${activity.occurredAt}-${activity.title}`} to={activity.resumeUrl}><time>{formatRelativeTime(activity.occurredAt)}</time><span><b>{activity.title}</b><small>{activity.context}</small></span></Link>)}
        </div>}
      </> : <>
        <h2>Start building your knowledge library.</h2>
        <p>{activityUnavailable ? 'Recent activity is unavailable right now. Your library is still ready when you are.' : 'Import a source or save a note to create your first session.'}</p>
        <Link to="/library">Import your first source <ArrowUpRight size={16} /></Link>
      </>}
    </aside>
    <div className="kos-home-band"><Link to="/library"><BookOpenText size={22}/><span><b>Library</b><small>Resources that stay findable.</small></span></Link><Link to="/ask"><BrainCircuit size={22}/><span><b>Ask</b><small>Answers grounded in evidence.</small></span></Link><Link to="/insights"><Search size={22}/><span><b>Discover</b><small>See what connects.</small></span></Link></div>
  </section>
}
