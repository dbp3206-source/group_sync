import { ArrowRight, Folder, Network, Tag } from 'lucide-react'
import { Link } from 'react-router-dom'
import { type ResourceKnowledgeMap } from '../api/knowledge'

type KnowledgeGraphViewProps = {
  map?: ResourceKnowledgeMap
  // Kept for the Library's existing view contract; unprojected data is not connected speculatively.
  resources?: unknown[]
  collections?: unknown[]
  tags?: unknown[]
  totalItems?: number
}

export default function KnowledgeGraphView({ map }: KnowledgeGraphViewProps) {
  if (!map) return <div className="kos-map-empty"><Network size={24} /><p>Open a resource to explore its verified Knowledge Map.</p></div>
  const center = map.nodes.find(node => node.type === 'RESOURCE' && node.id === map.edges[0]?.source)
    || map.nodes.find(node => node.type === 'RESOURCE')
  const connections = map.nodes.filter(node => node.id !== center?.id)
  const edgesByTarget = new Map<string, ResourceKnowledgeMap['edges']>()
  map.edges.forEach(edge => edgesByTarget.set(edge.target, [...(edgesByTarget.get(edge.target) || []), edge]))

  if (!center) return <div className="kos-map-empty"><Network size={24} /><p>No verified relationships are available for this source yet.</p></div>

  return <div className="kos-knowledge-map"><div className="kos-map-center"><Network size={18} /><strong>{center.label}</strong><small>Current resource</small></div>{connections.length ? <div className="kos-map-connections">{connections.map(node => { const edges = edgesByTarget.get(node.id) || []; return <article className={`kos-map-node kos-map-node--${node.type.toLowerCase()}`} key={node.id}><div className="kos-map-node-heading">{node.type === 'TAG' ? <Tag size={15} /> : node.type === 'COLLECTION' ? <Folder size={15} /> : <Network size={15} />}<strong>{node.label}</strong>{node.type === 'RESOURCE' && node.resourceId && <Link to={`/library/${node.resourceId}`} aria-label={`Open ${node.label}`}><ArrowRight size={15} /></Link>}</div>{edges.map((edge, index) => <div className="kos-map-edge-reason" key={`${edge.target}-${edge.relationType}-${index}`}><span>{edge.reason}</span><small>{edge.provenance.replaceAll('_', ' ')}</small></div>)}</article> })}</div> : <div className="kos-map-empty"><Network size={24} /><p>No verified relationships are available for this source yet.</p></div>}</div>
}
