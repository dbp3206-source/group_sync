import { CheckCircle2, CircleAlert, LoaderCircle, Timer } from 'lucide-react'
import type { Resource } from '../api/knowledge'

type ResourceStatusProps = {
  resource: Pick<Resource, 'processingStatus' | 'processingError'>
  compact?: boolean
}

const statusCopy: Record<string, { label: string; tone: string; icon: typeof Timer }> = {
  UPLOADING: { label: 'Uploading', tone: 'is-uploading', icon: LoaderCircle },
  UPLOADED: { label: 'Preparing', tone: 'is-preparing', icon: Timer },
  PARSING: { label: 'Preparing', tone: 'is-preparing', icon: Timer },
  CHUNKING: { label: 'Indexing', tone: 'is-indexing', icon: LoaderCircle },
  EMBEDDING: { label: 'Indexing', tone: 'is-indexing', icon: LoaderCircle },
  READY: { label: 'Ready to ask', tone: 'is-ready', icon: CheckCircle2 },
  FAILED: { label: 'Failed', tone: 'is-failed', icon: CircleAlert },
}

export default function ResourceStatus({ resource, compact = false }: ResourceStatusProps) {
  const status = statusCopy[resource.processingStatus] ?? {
    label: resource.processingStatus.toLowerCase(),
    tone: 'is-preparing',
    icon: Timer,
  }
  const Icon = status.icon
  return (
    <span className={`kos-resource-status ${status.tone} ${compact ? 'is-compact' : ''}`} role="status" aria-label={status.label} title={resource.processingError ?? status.label}>
      <Icon size={compact ? 14 : 15} aria-hidden="true" className={status.tone !== 'is-ready' && status.tone !== 'is-failed' ? 'is-spinning' : ''} />
      <span>{status.label}</span>
    </span>
  )
}
