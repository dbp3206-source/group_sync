import { ArrowUpRight, BookOpenText, BrainCircuit, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import hero from '../assets/knowledgeos-hero.png'
import { getFocusNext, type FocusNext } from '../api/knowledge'
import { useAuth } from '../auth'

export default function KnowledgeHomePage() {
  const { user } = useAuth(); const [focus, setFocus] = useState<FocusNext | null>(null)
  useEffect(() => { getFocusNext().then(setFocus).catch(() => setFocus(null)) }, [])
  return <section className="kos-home">
    <div className="kos-home-copy"><p className="kos-kicker">PERSONAL KNOWLEDGE INTELLIGENCE</p><h1>Your knowledge,<br /><em>finally connected.</em></h1><p className="kos-lead">Collect what matters. Ask better questions. Return to the ideas worth your attention.</p><div className="kos-hero-actions"><Link className="kos-button kos-button--primary" to="/ask">Ask KnowledgeOS <ArrowUpRight size={17} /></Link><Link className="kos-text-link" to="/library">Open library</Link></div><p className="kos-greeting">Good to see you, {user?.displayName ? user.displayName.split(' ')[0] : 'friend'}.</p></div>
    <div className="kos-hero-art"><img src={hero} alt="Study notes, an e-reader, and a cobalt bookmark on a desk" /><div className="kos-art-caption">A quieter place to think.</div></div>
    <aside className="kos-focus-card"><span>FOCUS NEXT</span>{focus ? <><h2>{focus.title}</h2><p>{focus.reason}</p><Link to="/library">Continue <ArrowUpRight size={16}/></Link></> : <><h2>Start a library worth returning to.</h2><p>Import a resource or save a note to create your first focus.</p><Link to="/library">Build library <ArrowUpRight size={16}/></Link></>}</aside>
    <div className="kos-home-band"><Link to="/library"><BookOpenText size={22}/><span><b>Library</b><small>Resources that stay findable.</small></span></Link><Link to="/ask"><BrainCircuit size={22}/><span><b>Ask</b><small>Answers grounded in evidence.</small></span></Link><Link to="/insights"><Search size={22}/><span><b>Discover</b><small>See what connects.</small></span></Link></div>
  </section>
}
