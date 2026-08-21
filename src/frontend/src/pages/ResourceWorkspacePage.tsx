import {
  ArrowLeft,
  Check,
  ChevronDown,
  FileText,
  Folder,
  Loader2,
  MoreHorizontal,
  Network,
  Pencil,
  RefreshCw,
  Sparkles,
  Tag,
  Trash2,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  applyOrganization,
  autoOrganizeResource,
  createResourceNote,
  deleteResource,
  deleteResourceNote,
  getResource,
  getResourceCollections,
  getResourceDeepDive,
  getResourceIngestionTrace,
  getResourceKnowledgeMap,
  getResourceNotes,
  getResourceTags,
  getResourceUnderstanding,
  recordResourceOpened,
  getOrganizationSuggestions,
  retryResource,
  updateResourceNote,
  type KnowledgeCollection,
  type KnowledgeTag,
  type OrganizationSuggestions,
  type Resource,
  type ResourceDeepDive,
  type ResourceIngestionTrace,
  type ResourceKnowledgeMap,
  type ResourceNote,
  type ResourceUnderstanding,
  type SemanticOrganizationResult,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

type Surface = 'overview' | 'deep-dive' | 'knowledge-map'

function cleanFilename(value: string | null | undefined) {
  const raw = (value || '').trim()
  if (!raw) return 'Untitled resource'
  const cleaned = raw.replace(/\.(pdf|docx|txt|md)$/i, '').replace(/(?:[_-](?:final|copy|draft|v\d+)(?:[_-]?\d+)?)+$/i, '').replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim()
  return cleaned || 'Untitled resource'
}

function displayTitle(resource: Resource, understanding: ResourceUnderstanding | null) {
  if (understanding?.status === 'CURRENT' && understanding.normalizedTitle?.trim()) return understanding.normalizedTitle.trim()
  return cleanFilename(resource.title || resource.originalFilename)
}

function statusLabel(status: string | undefined) {
  switch (status) {
    case 'CURRENT': return 'Understanding ready'
    case 'FAILED': return 'Understanding failed'
    case 'UNSUPPORTED': return 'Not enough readable evidence'
    case 'STALE': return 'Understanding needs refresh'
    default: return 'Understanding not available'
  }
}

function formatBytes(value: number | null | undefined) {
  if (!value) return '—'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function ResourceWorkspacePage() {
  const { resourceId: resourceParam, id: legacyId } = useParams()
  const resourceId = Number(resourceParam || legacyId)
  const navigate = useNavigate()
  const [resource, setResource] = useState<Resource | null>(null)
  const [understanding, setUnderstanding] = useState<ResourceUnderstanding | null>(null)
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [notes, setNotes] = useState<ResourceNote[]>([])
  const [deepDive, setDeepDive] = useState<ResourceDeepDive | null>(null)
  const [knowledgeMap, setKnowledgeMap] = useState<ResourceKnowledgeMap | null>(null)
  const [ingestionTrace, setIngestionTrace] = useState<ResourceIngestionTrace | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [surface, setSurface] = useState<Surface>('overview')
  const [optionalLoading, setOptionalLoading] = useState(true)
  const [optionalErrors, setOptionalErrors] = useState<Record<string, string>>({})
  const [notesOpen, setNotesOpen] = useState(false)
  const [draft, setDraft] = useState('')
  const [editingNoteId, setEditingNoteId] = useState<number | null>(null)
  const [editingNoteDraft, setEditingNoteDraft] = useState('')
  const [noteDeleteId, setNoteDeleteId] = useState<number | null>(null)
  const [noteBusy, setNoteBusy] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [retryBusy, setRetryBusy] = useState(false)
  const [organizing, setOrganizing] = useState(false)
  const [autoOrganizeMsg, setAutoOrganizeMsg] = useState('')
  const [organizationResult, setOrganizationResult] = useState<SemanticOrganizationResult | null>(null)
  const [organizationSuggestions, setOrganizationSuggestions] = useState<OrganizationSuggestions | null>(null)
  const [suggestionError, setSuggestionError] = useState('')
  const [selectedTagNames, setSelectedTagNames] = useState<string[]>([])
  const [selectedCollectionKeys, setSelectedCollectionKeys] = useState<string[]>([])
  const [suggestionBusy, setSuggestionBusy] = useState(false)
  const openedResource = useRef<number | null>(null)

  const loadResource = useCallback(async (id: number) => {
    if (!id || Number.isNaN(id)) {
      setError('Mã tài liệu không hợp lệ.')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const loaded = await getResource(id)
      setResource(loaded)
      if (openedResource.current !== id) {
        openedResource.current = id
        void recordResourceOpened(id).catch(() => {})
      }
    } catch {
      setError('Không thể tải thông tin tài liệu. Tài liệu có thể không tồn tại hoặc đã bị xoá.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setResource(null)
    setUnderstanding(null)
    setTags([])
    setCollections([])
    setNotes([])
    setDeepDive(null)
    setKnowledgeMap(null)
    setIngestionTrace(null)
    setOptionalErrors({})
    setSurface('overview')
    setNotesOpen(false)
    openedResource.current = null
    void loadResource(resourceId)
  }, [loadResource, resourceId])

  useEffect(() => {
    if (!resource || !resourceId || Number.isNaN(resourceId)) return
    let cancelled = false
    setOptionalLoading(true)
    const requests: [string, Promise<unknown>][] = [
      ['understanding', getResourceUnderstanding(resourceId)],
      ['tags', getResourceTags(resourceId)],
      ['collections', getResourceCollections(resourceId)],
      ['notes', getResourceNotes(resourceId)],
      ['deepDive', getResourceDeepDive(resourceId)],
      ['knowledgeMap', getResourceKnowledgeMap(resourceId)],
      ['trace', getResourceIngestionTrace(resourceId)],
    ]
    Promise.allSettled(requests.map(([, request]) => request)).then(results => {
      if (cancelled) return
      const nextErrors: Record<string, string> = {}
      results.forEach((result, index) => {
        const [key] = requests[index]
        if (result.status === 'rejected') nextErrors[key] = 'Không khả dụng lúc này.'
        if (result.status === 'fulfilled') {
          switch (key) {
            case 'understanding': setUnderstanding(result.value as ResourceUnderstanding); break
            case 'tags': setTags((result.value as KnowledgeTag[]) || []); break
            case 'collections': setCollections((result.value as KnowledgeCollection[]) || []); break
            case 'notes': setNotes((result.value as ResourceNote[]) || []); break
            case 'deepDive': setDeepDive(result.value as ResourceDeepDive); break
            case 'knowledgeMap': setKnowledgeMap(result.value as ResourceKnowledgeMap); break
            case 'trace': setIngestionTrace(result.value as ResourceIngestionTrace); break
          }
        }
      })
      setOptionalErrors(nextErrors)
      setOptionalLoading(false)
    })
    return () => { cancelled = true }
  }, [resource, resourceId])

  async function handleRetry() {
    if (!resourceId || Number.isNaN(resourceId)) return
    setRetryBusy(true)
    try {
      const updated = await retryResource(resourceId)
      setResource(updated)
      setUnderstanding(null)
      setOptionalErrors({})
    } catch {
      setError('Không thể yêu cầu xử lý lại tài liệu.')
    } finally {
      setRetryBusy(false)
    }
  }

  async function handleDelete() {
    if (!resourceId || Number.isNaN(resourceId)) return
    if (!deleteConfirm) {
      setDeleteConfirm(true)
      return
    }
    setDeleting(true)
    try {
      await deleteResource(resourceId)
      navigate('/library')
    } catch {
      setError('Không thể xoá tài liệu lúc này.')
      setDeleteConfirm(false)
    } finally {
      setDeleting(false)
    }
  }

  async function addNote(event: React.FormEvent) {
    event.preventDefault()
    if (!draft.trim() || !resourceId || Number.isNaN(resourceId)) return
    setNoteBusy(true)
    try {
      const created = await createResourceNote(resourceId, draft.trim())
      setNotes(current => [created, ...current])
      setDraft('')
    } catch {
      setOptionalErrors(current => ({ ...current, notes: 'Không thể lưu Quick Note.' }))
    } finally {
      setNoteBusy(false)
    }
  }

  async function saveNote(noteId: number) {
    if (!editingNoteDraft.trim() || !resourceId) return
    setNoteBusy(true)
    try {
      const updated = await updateResourceNote(resourceId, noteId, editingNoteDraft.trim())
      setNotes(current => current.map(note => note.id === noteId ? updated : note))
      setEditingNoteId(null)
      setEditingNoteDraft('')
    } catch {
      setOptionalErrors(current => ({ ...current, notes: 'Không thể cập nhật Quick Note.' }))
    } finally {
      setNoteBusy(false)
    }
  }

  async function removeNote(noteId: number) {
    if (!resourceId) return
    setNoteBusy(true)
    try {
      await deleteResourceNote(resourceId, noteId)
      setNotes(current => current.filter(note => note.id !== noteId))
      setNoteDeleteId(null)
    } catch {
      setOptionalErrors(current => ({ ...current, notes: 'Không thể xoá Quick Note.' }))
    } finally {
      setNoteBusy(false)
    }
  }

  async function handleAutoOrganize() {
    if (!resourceId) return
    setOrganizing(true)
    setAutoOrganizeMsg('')
    setOrganizationResult(null)
    try {
      const result = await autoOrganizeResource(resourceId)
      setOrganizationResult(result)
      setAutoOrganizeMsg(result.understandingStatus === 'CURRENT' ? 'Phân tích ngữ nghĩa đã hoàn tất.' : 'Chưa đủ bằng chứng để tự động phân loại an toàn.')
      const [nextTags, nextCollections, nextMap] = await Promise.allSettled([getResourceTags(resourceId), getResourceCollections(resourceId), getResourceKnowledgeMap(resourceId)])
      if (nextTags.status === 'fulfilled') setTags(nextTags.value)
      if (nextCollections.status === 'fulfilled') setCollections(nextCollections.value)
      if (nextMap.status === 'fulfilled') setKnowledgeMap(nextMap.value)
    } catch {
      setAutoOrganizeMsg('Không thể tự động phân loại lúc này.')
    } finally {
      setOrganizing(false)
    }
  }

  async function loadOrganizationSuggestions() {
    if (!resourceId) return
    setSuggestionBusy(true)
    setSuggestionError('')
    try {
      const suggestions = await getOrganizationSuggestions(resourceId)
      setOrganizationSuggestions(suggestions)
      setSelectedTagNames([])
      setSelectedCollectionKeys([])
    } catch {
      setSuggestionError('Không thể tải đề xuất tổ chức lúc này.')
    } finally {
      setSuggestionBusy(false)
    }
  }

  async function applySelectedSuggestions() {
    if (!resourceId || !organizationSuggestions) return
    setSuggestionBusy(true)
    try {
      const selectedCollections = organizationSuggestions.suggestedCollections.filter(item => selectedCollectionKeys.includes(item.existingCollectionId > 0 ? `existing:${item.existingCollectionId}` : `new:${item.name}`))
      await applyOrganization(resourceId, { tagNames: selectedTagNames, collectionIds: selectedCollections.filter(item => item.existingCollectionId > 0).map(item => item.existingCollectionId), newCollectionNames: selectedCollections.filter(item => item.existingCollectionId <= 0).map(item => item.name), relatedResourceIds: [] })
      setAutoOrganizeMsg('Đã áp dụng các đề xuất đã chọn.')
      setOrganizationSuggestions(null)
      const [nextTags, nextCollections, nextMap] = await Promise.all([getResourceTags(resourceId), getResourceCollections(resourceId), getResourceKnowledgeMap(resourceId)])
      setTags(nextTags)
      setCollections(nextCollections)
      setKnowledgeMap(nextMap)
    } catch {
      setSuggestionError('Không thể áp dụng đề xuất đã chọn.')
    } finally {
      setSuggestionBusy(false)
    }
  }

  if (loading) return <section className="kos-page"><div className="kos-empty kos-workspace-loading" aria-live="polite"><Loader2 size={30} className="kos-spin-fast" /><h2>Đang mở Resource Workspace...</h2><p>Đang nạp tài liệu và các lớp hiểu biết đã lưu.</p></div></section>

  if (error || !resource) return <section className="kos-page"><div className="kos-empty" role="alert"><FileText size={32} /><h2>{error || 'Không tìm thấy tài liệu'}</h2><p>Tài liệu có thể đã bị xoá hoặc bạn không có quyền truy cập.</p><div className="kos-workspace-error-actions"><button type="button" className="kos-button" onClick={() => navigate('/library')}><ArrowLeft size={16} /> Quay lại Thư viện</button><button type="button" className="kos-button kos-button--primary" onClick={() => loadResource(resourceId)}><RefreshCw size={16} /> Thử lại</button></div></div></section>

  const title = displayTitle(resource, understanding)
  const understandingStatus = understanding?.status || 'NOT_AVAILABLE'
  const profileItems: [string, string][] = [['Type', resource.resourceType || 'DOCUMENT'], ['Status', resource.processingStatus || 'UNKNOWN'], ['Imported', formatDate(resource.createdAt)], ['Size', formatBytes(resource.sizeBytes)]]

  return <section className="kos-page kos-workspace kos-workspace-v2">
    <header className="kos-workspace-v2-header"><div className="kos-workspace-header-main"><Link to="/library" className="kos-workspace-back"><ArrowLeft size={16} /> Library</Link><div className="kos-workspace-title-row"><span className="kos-workspace-type-badge">{resource.resourceType || 'DOCUMENT'}</span><span className={`kos-understanding-status kos-understanding-status--${understandingStatus.toLowerCase()}`}>{statusLabel(understandingStatus)}</span></div><h1 className="kos-workspace-title-v2">{title}</h1><p className="kos-workspace-filename">{resource.originalFilename || 'Original filename unavailable'}</p></div><div className="kos-workspace-header-actions"><Link to={`/ask?resource=${resource.id}`} className="kos-button kos-button--primary">Ask this source</Link><details className="kos-workspace-more"><summary className="kos-button kos-button--quiet"><MoreHorizontal size={17} /> <span>More</span></summary><div className="kos-workspace-more-menu">{resource.processingStatus === 'FAILED' && <button type="button" onClick={handleRetry} disabled={retryBusy}>{retryBusy ? <Loader2 size={15} className="kos-spin-fast" /> : <RefreshCw size={15} />} Retry processing</button>}<button type="button" className="is-danger" onClick={handleDelete} disabled={deleting}><Trash2 size={15} /> {deleting ? 'Deleting…' : 'Delete resource'}</button></div></details></div></header>
    {deleteConfirm && <div className="kos-workspace-confirm" role="alert"><strong>Delete this resource?</strong><span>Its Workspace will no longer be available.</span><button type="button" className="kos-button kos-button--danger" onClick={handleDelete} disabled={deleting}>{deleting ? 'Deleting…' : 'Confirm delete'}</button><button type="button" className="kos-button" onClick={() => setDeleteConfirm(false)}>Cancel</button></div>}
    <nav className="kos-workspace-v2-nav" aria-label="Resource Workspace">{([['overview', 'Overview'], ['deep-dive', 'Deep Dive'], ['knowledge-map', 'Knowledge Map']] as [Surface, string][]).map(([key, label]) => <button type="button" key={key} className={surface === key ? 'is-active' : ''} onClick={() => setSurface(key)}>{label}</button>)}</nav>
    {surface === 'overview' && <OverviewSurface resource={resource} understanding={understanding} understandingStatus={understandingStatus} profileItems={profileItems} tags={tags} collections={collections} notes={notes} notesOpen={notesOpen} setNotesOpen={setNotesOpen} draft={draft} setDraft={setDraft} addNote={addNote} editingNoteId={editingNoteId} setEditingNoteId={setEditingNoteId} editingNoteDraft={editingNoteDraft} setEditingNoteDraft={setEditingNoteDraft} saveNote={saveNote} noteDeleteId={noteDeleteId} setNoteDeleteId={setNoteDeleteId} removeNote={removeNote} noteBusy={noteBusy} optionalLoading={optionalLoading} optionalErrors={optionalErrors} ingestionTrace={ingestionTrace} organizationSuggestions={organizationSuggestions} selectedTagNames={selectedTagNames} setSelectedTagNames={setSelectedTagNames} selectedCollectionKeys={selectedCollectionKeys} setSelectedCollectionKeys={setSelectedCollectionKeys} suggestionBusy={suggestionBusy} suggestionError={suggestionError} loadOrganizationSuggestions={loadOrganizationSuggestions} applySelectedSuggestions={applySelectedSuggestions} organizing={organizing} autoOrganizeMsg={autoOrganizeMsg} organizationResult={organizationResult} handleAutoOrganize={handleAutoOrganize} />}
    {surface === 'deep-dive' && <DeepDiveSurface deepDive={deepDive} loading={optionalLoading && !deepDive} error={optionalErrors.deepDive} />}
    {surface === 'knowledge-map' && <MapSurface map={knowledgeMap} loading={optionalLoading && !knowledgeMap} error={optionalErrors.knowledgeMap} />}
  </section>
}

type OverviewProps = {
  resource: Resource; understanding: ResourceUnderstanding | null; understandingStatus: string; profileItems: [string, string][]; tags: KnowledgeTag[]; collections: KnowledgeCollection[]; notes: ResourceNote[]; notesOpen: boolean; setNotesOpen: (open: boolean) => void; draft: string; setDraft: (value: string) => void; addNote: (event: React.FormEvent) => void; editingNoteId: number | null; setEditingNoteId: (id: number | null) => void; editingNoteDraft: string; setEditingNoteDraft: (value: string) => void; saveNote: (id: number) => void; noteDeleteId: number | null; setNoteDeleteId: (id: number | null) => void; removeNote: (id: number) => void; noteBusy: boolean; optionalLoading: boolean; optionalErrors: Record<string, string>; ingestionTrace: ResourceIngestionTrace | null; organizationSuggestions: OrganizationSuggestions | null; selectedTagNames: string[]; setSelectedTagNames: (names: string[]) => void; selectedCollectionKeys: string[]; setSelectedCollectionKeys: (keys: string[]) => void; suggestionBusy: boolean; suggestionError: string; loadOrganizationSuggestions: () => void; applySelectedSuggestions: () => void; organizing: boolean; autoOrganizeMsg: string; organizationResult: SemanticOrganizationResult | null; handleAutoOrganize: () => void
}

function OverviewSurface(props: OverviewProps) {
  const currentUnderstanding = props.understandingStatus === 'CURRENT'
  return <div className="kos-workspace-v2-surface"><div className="kos-workspace-overview-grid-v2"><main className="kos-workspace-primary-column"><section className="kos-workspace-section kos-brief-section"><div className="kos-section-kicker">Document Brief</div><h2>{currentUnderstanding ? 'What this source is about' : 'Document understanding is not available'}</h2>{currentUnderstanding ? <p className="kos-brief-copy">{props.understanding?.summary}</p> : <p className="kos-muted">This Workspace only shows persisted understanding. It will not invent a summary while the source is unavailable or needs refresh.</p>}{currentUnderstanding && props.understanding && <div className="kos-evidence-line">{props.understanding.evidenceCount} verified evidence chunk{props.understanding.evidenceCount === 1 ? '' : 's'} · updated {formatDate(props.understanding.updatedAt)}</div>}</section><section className="kos-workspace-section"><div className="kos-section-heading"><div><div className="kos-section-kicker">Key Ideas</div><h2>Keep these in view</h2></div>{props.optionalLoading && <Loader2 size={17} className="kos-spin-fast" aria-label="Loading" />}</div>{currentUnderstanding && props.understanding?.keyIdeas.length ? <ol className="kos-key-ideas">{props.understanding.keyIdeas.map((idea, index) => <li key={`${idea}-${index}`}><span>{String(index + 1).padStart(2, '0')}</span><p>{idea}</p></li>)}</ol> : <p className="kos-muted">No persisted key ideas are available for this source.</p>}</section><section className="kos-workspace-section"><div className="kos-section-kicker">Knowledge Profile</div><h2>Source context</h2><div className="kos-profile-grid">{props.profileItems.map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}</div><div className="kos-profile-groups"><div><span className="kos-profile-label"><Tag size={14} /> Resource Tags</span>{props.tags.length ? <div className="kos-chip-row">{props.tags.map(tag => <span className="kos-chip" key={tag.id}>{tag.name}</span>)}</div> : <p className="kos-muted">No assigned tags.</p>}</div><div><span className="kos-profile-label"><Folder size={14} /> Collections</span>{props.collections.length ? <div className="kos-membership-list">{props.collections.map(collection => <span key={collection.id}>{collection.name}</span>)}</div> : <p className="kos-muted">No collection membership.</p>}</div></div></section><OrganizationPanel {...props} /><details className="kos-workspace-advanced"><summary><span><ChevronDown size={16} /> Advanced</span><small>Ingestion trace and operational detail</small></summary><div className="kos-advanced-body">{props.ingestionTrace ? <div className="kos-trace-grid"><TraceItem label="Processing" value={props.ingestionTrace.processingStatus} /><TraceItem label="Chunks" value={`${props.ingestionTrace.parentChunkCount} parent / ${props.ingestionTrace.childChunkCount} child`} /><TraceItem label="Embeddings" value={`${props.ingestionTrace.embeddingModel} · ${props.ingestionTrace.embeddingDimensions}d`} /><TraceItem label="Semantic metadata" value={props.ingestionTrace.semanticMetadataIncluded ? 'Included' : 'Not included'} /></div> : <p className="kos-muted">{props.optionalErrors.trace || 'Ingestion trace is not available.'}</p>}</div></details></main><aside className="kos-workspace-side-column"><button type="button" className="kos-notes-card" onClick={() => props.setNotesOpen(true)}><span className="kos-section-kicker">Quick Notes</span><strong>{props.notes.length} note{props.notes.length === 1 ? '' : 's'}</strong><small>Capture a thought without leaving the source.</small><span className="kos-text-action">Open notes <ChevronDown size={15} /></span></button><div className="kos-workspace-ask-card"><Sparkles size={18} /><div><strong>Ask this source</strong><p>Use the grounded answer flow with this resource already in scope.</p><Link to={`/ask?resource=${props.resource.id}`} className="kos-text-action">Open Ask <ChevronDown size={15} /></Link></div></div></aside></div>{props.notesOpen && <NotesDrawer {...props} />}</div>
}

function TraceItem({ label, value }: { label: string; value: string }) { return <div><span>{label}</span><strong>{value}</strong></div> }

function OrganizationPanel(props: OverviewProps) {
  return <section className="kos-workspace-section kos-organization-panel"><div className="kos-section-heading"><div><div className="kos-section-kicker">Semantic Organization</div><h2>Keep structure intentional</h2></div><button type="button" className="kos-button" onClick={props.handleAutoOrganize} disabled={props.organizing}>{props.organizing ? <Loader2 size={15} className="kos-spin-fast" /> : <Sparkles size={15} />} Auto-organize</button></div><p className="kos-muted">Assigned tags and collections above are the saved state. Suggestions stay reviewable until you apply them.</p>{props.autoOrganizeMsg && <p className="kos-inline-success">{props.autoOrganizeMsg}</p>}{props.organizationResult && <p className="kos-muted">Assigned {props.organizationResult.tagsAssigned.length} tag{props.organizationResult.tagsAssigned.length === 1 ? '' : 's'} and {props.organizationResult.collectionsAssigned.length} collection{props.organizationResult.collectionsAssigned.length === 1 ? '' : 's'}.</p>}<div className="kos-organization-actions"><button type="button" className="kos-button kos-button--quiet" onClick={props.loadOrganizationSuggestions} disabled={props.suggestionBusy}>{props.suggestionBusy ? <Loader2 size={15} className="kos-spin-fast" /> : <Network size={15} />} Review suggestions</button></div>{props.suggestionError && <p className="kos-inline-error">{props.suggestionError}</p>}{props.organizationSuggestions && <div className="kos-suggestion-review"><div><strong>Suggested tags</strong>{props.organizationSuggestions.suggestedTags.length ? props.organizationSuggestions.suggestedTags.map(item => <label key={item.name}><input type="checkbox" checked={props.selectedTagNames.includes(item.name)} onChange={event => props.setSelectedTagNames(event.target.checked ? [...props.selectedTagNames, item.name] : props.selectedTagNames.filter(name => name !== item.name))} />{item.name}</label>) : <span className="kos-muted">None</span>}</div><div><strong>Suggested collections</strong>{props.organizationSuggestions.suggestedCollections.length ? props.organizationSuggestions.suggestedCollections.map(item => { const key = item.existingCollectionId > 0 ? `existing:${item.existingCollectionId}` : `new:${item.name}`; return <label key={key}><input type="checkbox" checked={props.selectedCollectionKeys.includes(key)} onChange={event => props.setSelectedCollectionKeys(event.target.checked ? [...props.selectedCollectionKeys, key] : props.selectedCollectionKeys.filter(value => value !== key))} />{item.name}</label> }) : <span className="kos-muted">None</span>}</div><button type="button" className="kos-button kos-button--primary" onClick={props.applySelectedSuggestions} disabled={props.suggestionBusy}><Check size={15} /> Apply selected</button></div>}</section>
}

function NotesDrawer(props: OverviewProps) {
  return <div className="kos-notes-drawer-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) props.setNotesOpen(false) }}><section className="kos-notes-drawer" role="dialog" aria-modal="true" aria-labelledby="quick-notes-title"><div className="kos-section-heading"><div><div className="kos-section-kicker">Quick Notes</div><h2 id="quick-notes-title">Thoughts to keep</h2></div><button type="button" className="kos-icon-btn" onClick={() => props.setNotesOpen(false)} aria-label="Close Quick Notes"><X size={17} /></button></div><form className="kos-note-form" onSubmit={props.addNote}><textarea value={props.draft} onChange={event => props.setDraft(event.target.value)} placeholder="Write a note about this source…" rows={3} /><button type="submit" className="kos-button kos-button--primary" disabled={props.noteBusy || !props.draft.trim()}>{props.noteBusy ? <Loader2 size={15} className="kos-spin-fast" /> : 'Add note'}</button></form>{props.optionalErrors.notes && <p className="kos-inline-error">{props.optionalErrors.notes}</p>}<div className="kos-note-list">{props.notes.length ? props.notes.map(note => <article className="kos-note-item" key={note.id}>{props.editingNoteId === note.id ? <><textarea value={props.editingNoteDraft} onChange={event => props.setEditingNoteDraft(event.target.value)} rows={4} /><div className="kos-note-actions"><button type="button" className="kos-button kos-button--primary" onClick={() => props.saveNote(note.id)} disabled={props.noteBusy}>Save</button><button type="button" className="kos-button" onClick={() => props.setEditingNoteId(null)}>Cancel</button></div></> : <><p>{note.content}</p><div className="kos-note-meta"><time>{formatDate(note.updatedAt)}</time><span><button type="button" onClick={() => { props.setEditingNoteId(note.id); props.setEditingNoteDraft(note.content) }}><Pencil size={13} /> Edit</button><button type="button" onClick={() => props.setNoteDeleteId(note.id)}><Trash2 size={13} /> Delete</button></span></div>{props.noteDeleteId === note.id && <div className="kos-note-delete-confirm"><span>Delete this note?</span><button type="button" className="kos-button kos-button--danger" onClick={() => props.removeNote(note.id)} disabled={props.noteBusy}>Delete</button><button type="button" className="kos-button" onClick={() => props.setNoteDeleteId(null)}>Keep</button></div>}</>}</article>) : <p className="kos-muted">No Quick Notes yet.</p>}</div></section></div>
}

function DeepDiveSurface({ deepDive, loading, error }: { deepDive: ResourceDeepDive | null; loading: boolean; error?: string }) {
  if (loading) return <div className="kos-workspace-v2-surface"><div className="kos-workspace-section kos-loading-inline"><Loader2 size={22} className="kos-spin-fast" /> Loading Deep Dive…</div></div>
  if (error || !deepDive) return <div className="kos-workspace-v2-surface"><div className="kos-workspace-section"><div className="kos-section-kicker">Deep Dive</div><h2>Deep Dive is unavailable</h2><p className="kos-muted">{error || 'The learning studio could not be reached. The rest of this Workspace remains available.'}</p></div></div>
  const percent = deepDive.conceptCount > 0 ? Math.round((deepDive.checkedCount / deepDive.conceptCount) * 100) : 0
  return <div className="kos-workspace-v2-surface"><section className="kos-workspace-section kos-deep-dive-hero"><div className="kos-section-kicker">Deep Dive</div>{deepDive.available ? <><h2>{deepDive.topicTitle}</h2><p className="kos-brief-copy">{deepDive.goal}</p><div className="kos-deep-dive-progress"><div><span>Checked concepts</span><strong>{percent}%</strong></div><div className="kos-progress-track"><span style={{ width: `${percent}%` }} /></div><small>{deepDive.checkedCount} of {deepDive.conceptCount} concepts checked. Derived from the current concept statuses.</small></div><div className="kos-deep-dive-stats"><TraceItem label="Checked" value={String(deepDive.checkedCount)} /><TraceItem label="Review needed" value={String(deepDive.reviewNeededCount)} /><TraceItem label="Learning" value={String(deepDive.learningCount)} /><TraceItem label="Not started" value={String(deepDive.notStartedCount)} /></div><Link to="/focus" className="kos-button kos-button--primary">Continue in Deep Dive</Link></> : <><h2>Build a source-grounded learning path</h2><p className="kos-muted">No Study Topic currently includes this resource. Build Deep Dive in the existing Learning Studio when you are ready.</p><Link to="/focus" className="kos-button kos-button--primary"><Sparkles size={15} /> Build Deep Dive</Link></>}</section></div>
}

function MapSurface({ map, loading, error }: { map: ResourceKnowledgeMap | null; loading: boolean; error?: string }) {
  if (loading) return <div className="kos-workspace-v2-surface"><div className="kos-workspace-section kos-loading-inline"><Loader2 size={22} className="kos-spin-fast" /> Loading Knowledge Map…</div></div>
  if (error || !map) return <div className="kos-workspace-v2-surface"><div className="kos-workspace-section"><div className="kos-section-kicker">Knowledge Map</div><h2>Knowledge Map is unavailable</h2><p className="kos-muted">{error || 'The map could not be reached. It will not substitute arbitrary library links.'}</p></div></div>
  return <div className="kos-workspace-v2-surface"><section className="kos-workspace-section"><div className="kos-section-heading"><div><div className="kos-section-kicker">Knowledge Map</div><h2>How this source connects</h2><p className="kos-muted">Only stored memberships, tags, and bounded related-resource links are shown.</p></div><Network size={21} /></div><KnowledgeGraphView map={map} /></section></div>
}
