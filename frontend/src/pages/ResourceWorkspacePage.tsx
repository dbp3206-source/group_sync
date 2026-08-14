import { ArrowLeft, BrainCircuit, FileText } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { getResource, getResourceContent, type Resource } from '../api/knowledge'

export default function ResourceWorkspacePage() {
  const resourceId = Number(useParams().resourceId); const [resource,setResource]=useState<Resource|null>(null); const [content,setContent]=useState(''); const [tab,setTab]=useState('Overview'); const [error,setError]=useState('')
  useEffect(()=>{ Promise.all([getResource(resourceId),getResourceContent(resourceId)]).then(([item,text])=>{setResource(item);setContent(text)}).catch(()=>setError('This resource could not be loaded.')) },[resourceId])
  if(error) return <section className="kos-page"><p className="kos-error">{error}</p></section>
  if(!resource) return <section className="kos-page"><div className="kos-empty">Loading resource...</div></section>
  return <section className="kos-page kos-workspace"><Link className="kos-back" to="/library"><ArrowLeft size={16}/> Library</Link><header><div><p className="kos-kicker">{resource.resourceType}</p><h1>{resource.title}</h1><p>{resource.description || 'A resource in your KnowledgeOS library.'}</p></div><Link className="kos-button kos-button--primary" to={`/ask?resource=${resource.id}`}><BrainCircuit size={17}/> Ask this resource</Link></header><nav className="kos-tabs">{['Overview','Reader','Notes','Related','Activity'].map(item=><button key={item} className={tab===item?'is-active':''} onClick={()=>setTab(item)}>{item}</button>)}</nav>{tab==='Overview' && <div className="kos-workspace-overview"><FileText size={32}/><h2>Ready for retrieval</h2><p>Processing status: {resource.processingStatus}. Use Ask this resource to keep answers constrained to this source.</p></div>}{tab==='Reader' && <article className="kos-reader">{content}</article>}{tab==='Notes' && <div className="kos-empty"><h2>Notes endpoint is not available yet.</h2><p>This workspace keeps the tab visible without presenting a non-functional editor.</p></div>}{tab==='Related' && <div className="kos-empty"><h2>Related resources appear from retrieval evidence.</h2></div>}{tab==='Activity' && <div className="kos-empty"><h2>Reading activity appears as progress is recorded.</h2></div>}</section>
}
