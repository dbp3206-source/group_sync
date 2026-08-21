import {
  ArrowLeft,
  BookOpen,
  BrainCircuit,
  Compass,
  FileText,
  Loader2,
  Network,
  RefreshCw,
  Sparkles,
  Trash2,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  autoOrganizeResource,
  applyOrganization,
  createResourceNote,
  deleteResource,
  deleteResourceNote,
  getCollections,
  getOrganizationSuggestions,
  getRelatedResources,
  getResource,
  getResourceActivity,
  getResourceContent,
  getResourceIngestionTrace,
  getResourceNotes,
  getResources,
  getTags,
  recordResourceOpened,
  updateResourceProgress,
  type KnowledgeCollection,
  type KnowledgeTag,
  type OrganizationSuggestions,
  type RelatedResource,
  type Resource,
  type ResourceActivity,
  type ResourceIngestionTrace,
  type ResourceNote,
  type SemanticOrganizationResult,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

type Tab = 'Overview' | 'Reader' | 'Notes' | 'Related' | 'Activity'

export default function ResourceWorkspacePage() {
  const params = useParams()
  const rawId = params.resourceId || params.id
  const resourceId = Number(rawId)
  const navigate = useNavigate()

  // Core metadata state
  const [resource, setResource] = useState<Resource | null>(null)
  const [activity, setActivity] = useState<ResourceActivity | null>(null)
  const [ingestionTrace, setIngestionTrace] = useState<ResourceIngestionTrace | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // Active tab
  const [tab, setTab] = useState<Tab>('Overview')

  // Lazy tab caches
  const [content, setContent] = useState<string | null>(null)
  const [contentLoading, setContentLoading] = useState(false)
  const [contentError, setContentError] = useState('')

  const [notes, setNotes] = useState<ResourceNote[] | null>(null)
  const [notesLoading, setNotesLoading] = useState(false)

  const [related, setRelated] = useState<RelatedResource[] | null>(null)
  const [relatedLoading, setRelatedLoading] = useState(false)

  const [graphDataLoaded, setGraphDataLoaded] = useState(false)
  const [allResources, setAllResources] = useState<Resource[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])

  // Note form & UI state
  const [draft, setDraft] = useState('')
  const [noteBusy, setNoteBusy] = useState(false)
  const [autoOrganizeMsg, setAutoOrganizeMsg] = useState('')
  const [organizationResult, setOrganizationResult] = useState<SemanticOrganizationResult | null>(null)
  const [organizationSuggestions, setOrganizationSuggestions] = useState<OrganizationSuggestions | null>(null)
  const [suggestionError, setSuggestionError] = useState('')
  const [selectedTagNames, setSelectedTagNames] = useState<string[]>([])
  const [selectedCollectionKeys, setSelectedCollectionKeys] = useState<string[]>([])
  const [suggestionBusy, setSuggestionBusy] = useState(false)
  const [organizing, setOrganizing] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [progressVal, setProgressVal] = useState<number | null>(null)
  const resourceOpenRecorded = useRef<number | null>(null)

  const loadResourceData = useCallback(async (id: number) => {
    if (!id || isNaN(id)) {
      setError('Mã tài liệu không hợp lệ.')
      setLoading(false)
      return
    }

    setLoading(true)
    setError('')

    try {
      const [resResult, actResult, traceResult] = await Promise.allSettled([
        getResource(id),
        getResourceActivity(id),
        getResourceIngestionTrace(id),
      ])

      if (resResult.status === 'fulfilled' && resResult.value) {
        setResource(resResult.value)
        if (resourceOpenRecorded.current !== id) {
          resourceOpenRecorded.current = id
          recordResourceOpened(id).then(setActivity).catch(() => {})
        }
      } else {
        setError('Không thể tải thông tin tài liệu. Tài liệu có thể không tồn tại hoặc đã bị xoá.')
      }

      if (actResult.status === 'fulfilled' && actResult.value) {
        setActivity(actResult.value)
      }

      if (traceResult.status === 'fulfilled' && traceResult.value) {
        setIngestionTrace(traceResult.value)
      }
    } catch {
      setError('Đã xảy ra lỗi khi tải không gian làm việc.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setContent(null)
    setNotes(null)
    setRelated(null)
    setGraphDataLoaded(false)
    loadResourceData(resourceId)
  }, [resourceId, loadResourceData])

  // Lazy loading on tab activation
  useEffect(() => {
    if (!resourceId || isNaN(resourceId)) return

    if (tab === 'Reader' && content === null && !contentLoading) {
      setContentLoading(true)
      setContentError('')
      getResourceContent(resourceId)
        .then(text => setContent(text || ''))
        .catch(() => setContentError('Không thể đọc nội dung tài liệu. Vui lòng thử lại.'))
        .finally(() => setContentLoading(false))
    } else if (tab === 'Notes' && notes === null && !notesLoading) {
      setNotesLoading(true)
      getResourceNotes(resourceId)
        .then(n => setNotes(n || []))
        .catch(() => setNotes([]))
        .finally(() => setNotesLoading(false))
    } else if (tab === 'Related' && related === null && !relatedLoading) {
      setRelatedLoading(true)
      getRelatedResources(resourceId)
        .then(r => setRelated(r || []))
        .catch(() => setRelated([]))
        .finally(() => setRelatedLoading(false))
    } else if (tab === 'Activity' && !graphDataLoaded) {
      setGraphDataLoaded(true)
      Promise.allSettled([
        getResources(undefined, undefined, undefined, 0, 24).then(r => r.items),
        getCollections(),
        getTags(),
      ]).then(([allR, cols, t]) => {
        if (allR.status === 'fulfilled') setAllResources(allR.value || [])
        if (cols.status === 'fulfilled') setCollections(cols.value || [])
        if (t.status === 'fulfilled') setTags(t.value || [])
      })
    }
  }, [tab, resourceId, content, contentLoading, notes, notesLoading, related, relatedLoading, graphDataLoaded])

  function handleRetryReader() {
    if (!resourceId || isNaN(resourceId)) return
    setContentLoading(true)
    setContentError('')
    getResourceContent(resourceId)
      .then(text => setContent(text || ''))
      .catch(() => setContentError('Không thể đọc nội dung tài liệu. Vui lòng thử lại.'))
      .finally(() => setContentLoading(false))
  }

  async function handleAutoOrganize() {
    if (!resourceId || isNaN(resourceId)) return
    setOrganizing(true)
    setAutoOrganizeMsg('')
    setOrganizationResult(null)
    try {
      const result = await autoOrganizeResource(resourceId)
      setOrganizationResult(result)
      setAutoOrganizeMsg(result.understandingStatus === 'CURRENT' ? 'Phân tích ngữ nghĩa đã hoàn tất.' : 'Không đủ bằng chứng để tự động phân loại an toàn.')
      if (related !== null) {
        getRelatedResources(resourceId).then(r => setRelated(r || [])).catch(() => {})
      }
    } catch {
      setAutoOrganizeMsg('Không thể tự động phân loại.')
    } finally {
      setOrganizing(false)
    }
  }

  async function loadOrganizationSuggestions() {
    if (!resourceId || isNaN(resourceId)) return
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
    if (!resourceId || isNaN(resourceId) || !organizationSuggestions) return
    setSuggestionBusy(true)
    setSuggestionError('')
    try {
      const chosenCollections = organizationSuggestions.suggestedCollections.filter(item =>
        selectedCollectionKeys.includes(item.existingCollectionId > 0 ? `existing:${item.existingCollectionId}` : `new:${item.name}`))
      await applyOrganization(resourceId, {
        tagNames: selectedTagNames,
        collectionIds: chosenCollections.filter(item => item.existingCollectionId > 0).map(item => item.existingCollectionId),
        newCollectionNames: chosenCollections.filter(item => item.existingCollectionId <= 0).map(item => item.name),
        relatedResourceIds: [],
      })
      setAutoOrganizeMsg('Đã áp dụng các đề xuất bạn chọn.')
      setOrganizationSuggestions(null)
    } catch {
      setSuggestionError('Không thể áp dụng đề xuất đã chọn.')
    } finally {
      setSuggestionBusy(false)
    }
  }

  async function addNote(e: React.FormEvent) {
    e.preventDefault()
    if (!draft.trim() || !resourceId || isNaN(resourceId)) return
    setNoteBusy(true)
    try {
      const created = await createResourceNote(resourceId, draft.trim())
      setNotes(prev => (prev ? [created, ...prev] : [created]))
      setDraft('')
      setActivity(prev => (prev ? { ...prev, noteCount: (prev.noteCount || 0) + 1 } : prev))
    } catch {
      setError('Could not save note.')
    } finally {
      setNoteBusy(false)
    }
  }

  async function removeNote(noteId: number) {
    if (!resourceId || isNaN(resourceId)) return
    try {
      await deleteResourceNote(resourceId, noteId)
      setNotes(prev => (prev ? prev.filter(n => n.id !== noteId) : []))
      setActivity(prev => (prev ? { ...prev, noteCount: Math.max(0, (prev.noteCount || 0) - 1) } : prev))
    } catch {
      setError('Could not delete note.')
    }
  }

  async function saveProgress(percent: number) {
    if (!resourceId || isNaN(resourceId)) return
    try {
      const updated = await updateResourceProgress(resourceId, percent)
      setActivity(updated)
      setProgressVal(null)
    } catch {
      setError('Could not update progress.')
    }
  }

  async function handleDelete() {
    if (!resourceId || isNaN(resourceId)) return
    if (!deleteConfirm) {
      setDeleteConfirm(true)
      return
    }
    setDeleting(true)
    try {
      await deleteResource(resourceId)
      navigate('/library')
    } catch {
      setError('This resource could not be deleted.')
      setDeleteConfirm(false)
    } finally {
      setDeleting(false)
    }
  }

  if (loading) {
    return (
      <section className="kos-page">
        <div className="kos-empty" aria-live="polite" style={{ padding: '4rem 1rem', textAlign: 'center' }}>
          <Loader2 size={32} className="kos-spin-fast" style={{ margin: '0 auto 1rem' }} />
          <h2>Đang mở không gian tài liệu...</h2>
          <p>Đang nạp thông tin và bối cảnh tri thức từ KnowledgeOS</p>
        </div>
      </section>
    )
  }

  if (error || !resource) {
    return (
      <section className="kos-page">
        <div className="kos-empty" role="alert" style={{ padding: '4rem 1rem', textAlign: 'center' }}>
          <FileText size={32} style={{ margin: '0 auto 1rem', color: 'var(--kos-danger, #ef4444)' }} />
          <h2>{error || 'Không tìm thấy tài liệu'}</h2>
          <p>Tài liệu này có thể đã bị xóa hoặc bạn không có quyền truy cập.</p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', marginTop: '1.5rem' }}>
            <button type="button" className="kos-button" onClick={() => navigate('/library')}>
              <ArrowLeft size={16} /> Quay lại Thư viện
            </button>
            <button type="button" className="kos-button kos-button--primary" onClick={() => loadResourceData(resourceId)}>
              <RefreshCw size={16} /> Thử lại
            </button>
          </div>
        </div>
      </section>
    )
  }

  const metaItems = [
    { label: 'Type', value: resource.resourceType || 'DOCUMENT' },
    { label: 'Status', value: resource.processingStatus || 'READY' },
    { label: 'Priority', value: resource.priority ?? 0 },
    { label: 'Favorite', value: resource.favorite ? 'Yes' : 'No' },
    { label: 'Filename', value: resource.originalFilename || '—' },
    {
      label: 'Size',
      value: resource.sizeBytes
        ? `${(resource.sizeBytes / 1024).toFixed(1)} KB`
        : '—',
    },
    { label: 'Created', value: resource.createdAt ? new Date(resource.createdAt).toLocaleString() : '—' },
  ]

  return (
    <section className="kos-page kos-workspace">
      {/* Top action bar */}
      <div className="kos-workspace-nav">
        <button
          type="button"
          className="kos-icon-btn"
          onClick={() => navigate('/library')}
          aria-label="Back to Library"
        >
          <ArrowLeft size={16} />
        </button>
        <span className="kos-workspace-type-badge">{resource.resourceType || 'DOCUMENT'}</span>
        <h1 className="kos-workspace-title">{resource.title || 'Untitled'}</h1>
        <div className="kos-workspace-actions">
          <Link
            to={`/ask?resource=${resource.id}`}
            className="kos-button kos-button--primary"
          >
            <BrainCircuit size={16} /> Ask with this source
          </Link>
          <button
            type="button"
            className="kos-button kos-button--danger"
            onClick={handleDelete}
            disabled={deleting}
          >
            <Trash2 size={16} />
            {deleting ? 'Deleting…' : deleteConfirm ? 'Confirm Delete?' : 'Delete'}
          </button>
        </div>
      </div>

      {/* Tabs */}
      <nav className="kos-workspace-tabs" aria-label="Resource sections">
        {(['Overview', 'Reader', 'Notes', 'Related', 'Activity'] as Tab[]).map(item => (
          <button
            key={item}
            type="button"
            className={`kos-workspace-tab ${tab === item ? 'is-active' : ''}`}
            onClick={() => setTab(item)}
          >
            {item === 'Overview' ? '📋 Tổng quan' : item === 'Reader' ? '📖 Đọc tài liệu' : item === 'Notes' ? '📝 Ghi chú' : item === 'Related' ? '🔗 Liên quan' : '⚡ Hoạt động & Recall'}
          </button>
        ))}
      </nav>

      {/* Tab: Overview */}
      {tab === 'Overview' && (
        <div className="kos-workspace-panel">
          <div className="kos-workspace-overview-grid">
            <div className="kos-overview-card">
              <h3>Description</h3>
              <p>{resource.description || 'No description provided.'}</p>

              <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--kos-line)' }}>
                <button
                  type="button"
                  className="kos-button kos-button--sm"
                  onClick={handleAutoOrganize}
                  disabled={organizing}
                >
                  <Sparkles size={14} className={organizing ? 'kos-spin' : ''} />
                  {organizing ? 'Đang phân loại...' : 'Tự động phân loại vào Collection/Tag'}
                </button>
                {autoOrganizeMsg && (
                  <p style={{ margin: '0.5rem 0 0', fontSize: '0.82rem', color: 'var(--kos-green)' }} aria-live="polite">
                    {autoOrganizeMsg}
                  </p>
                )}
                {organizationResult && (
                  <div className="kos-semantic-resource-result">
                    <span>{organizationResult.tagsAssigned.length} tags added</span>
                    <span>{organizationResult.collectionsAssigned.length} strong collection matches applied</span>
                    <span>{organizationResult.collectionSuggestions.length + organizationResult.newCollectionSuggestions.length} suggestions need review</span>
                    {(organizationResult.collectionSuggestions.length + organizationResult.newCollectionSuggestions.length > 0) && (
                      <button type="button" className="kos-button kos-button--quiet" disabled={suggestionBusy} onClick={() => void loadOrganizationSuggestions()}>
                        {suggestionBusy ? 'Loading...' : 'Review suggestions'}
                      </button>
                    )}
                  </div>
                )}
                {suggestionError && <p className="kos-modal-error" role="alert">{suggestionError}</p>}
                {organizationSuggestions && (
                  <div className="kos-semantic-review" aria-label="Semantic organization suggestions">
                    <h4>Review possible matches</h4>
                    {organizationSuggestions.suggestedTags.length > 0 && (
                      <fieldset>
                        <legend>Semantic tags</legend>
                        {organizationSuggestions.suggestedTags.map(tag => (
                          <label key={`${tag.existingTagId}-${tag.name}`}>
                            <input type="checkbox" checked={selectedTagNames.includes(tag.name)} onChange={event => setSelectedTagNames(current => event.target.checked ? [...current, tag.name] : current.filter(name => name !== tag.name))} />
                            <span><strong>{tag.name}</strong><small>{tag.confidence >= 0.8 ? 'Strong match' : 'Possible match'}</small></span>
                          </label>
                        ))}
                      </fieldset>
                    )}
                    {organizationSuggestions.suggestedCollections.length > 0 && (
                      <fieldset>
                        <legend>Collections</legend>
                        {organizationSuggestions.suggestedCollections.map(collection => {
                          const key = collection.existingCollectionId > 0 ? `existing:${collection.existingCollectionId}` : `new:${collection.name}`
                          return <label key={key}>
                            <input type="checkbox" checked={selectedCollectionKeys.includes(key)} onChange={event => setSelectedCollectionKeys(current => event.target.checked ? [...current, key] : current.filter(value => value !== key))} />
                            <span><strong>{collection.name}</strong><small>{collection.existingCollectionId > 0 ? 'Existing collection' : 'New collection suggestion'} · {collection.confidence >= 0.8 ? 'Strong match' : 'Possible match'}</small></span>
                          </label>
                        })}
                      </fieldset>
                    )}
                    <div className="kos-modal-actions">
                      <button type="button" className="kos-button kos-button--quiet" onClick={() => setOrganizationSuggestions(null)}>Cancel</button>
                      <button type="button" className="kos-button kos-button--primary" disabled={suggestionBusy || (selectedTagNames.length === 0 && selectedCollectionKeys.length === 0)} onClick={() => void applySelectedSuggestions()}>{suggestionBusy ? 'Applying...' : 'Apply selected'}</button>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="kos-overview-card">
              <h3>Metadata</h3>
              <dl className="kos-metadata-list">
                {metaItems.map(m => (
                  <div key={m.label} className="kos-metadata-row">
                    <dt>{m.label}</dt>
                    <dd>{m.value}</dd>
                  </div>
                ))}
              </dl>
            </div>

            {ingestionTrace && (
              <div className="kos-overview-card" style={{ gridColumn: '1 / -1', background: 'var(--kos-subtle)' }}>
                <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <BrainCircuit size={16} /> RAG Ingestion & Index Trace (Explainability)
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '0.75rem' }}>
                  <div>
                    <small style={{ color: 'var(--kos-muted)', display: 'block' }}>Structure-aware Chunking</small>
                    <strong style={{ fontSize: '0.9rem' }}>
                      {(ingestionTrace.chunkingVersion ?? 2) >= 2 ? (
                        `${ingestionTrace.parentChunkCount ?? 0} Parents (~1500 chars) · ${ingestionTrace.childChunkCount ?? 0} Children (~500 chars)`
                      ) : (
                        `${ingestionTrace.childChunkCount ?? 0} Chunks (Legacy Flat Index)`
                      )}
                    </strong>
                  </div>
                  <div>
                    <small style={{ color: 'var(--kos-muted)', display: 'block' }}>Vector Embeddings</small>
                    <strong style={{ fontSize: '0.9rem' }}>
                      {ingestionTrace.embeddingModel || 'gemini-embedding-001'} ({ingestionTrace.embeddingDimensions || 768}d)
                      {(ingestionTrace.chunkingVersion ?? 2) >= 2 && (ingestionTrace.embeddingBatchCount || 0) > 0 ? ` · ${ingestionTrace.embeddingBatchCount} Embedding Batches` : ''}
                    </strong>
                  </div>
                  <div>
                    <small style={{ color: 'var(--kos-muted)', display: 'block' }}>Chunking Engine Version</small>
                    <strong style={{ fontSize: '0.9rem' }}>
                      v{ingestionTrace.chunkingVersion ?? 2} {(ingestionTrace.chunkingVersion ?? 2) >= 2 ? '(Hierarchical Parent–Child)' : '(Legacy Flat Chunking)'}
                    </strong>
                  </div>
                  <div>
                    <small style={{ color: 'var(--kos-muted)', display: 'block' }}>Semantic Metadata Preloaded</small>
                    <strong style={{ fontSize: '0.9rem', color: ingestionTrace.semanticMetadataIncluded ? 'var(--kos-green)' : 'var(--kos-muted)' }}>
                      {ingestionTrace.semanticMetadataIncluded ? '✓ Tags & Collections Embedded' : 'Not available for legacy index'}
                    </strong>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab: Reader */}
      {tab === 'Reader' && (
        <div className="kos-workspace-panel kos-workspace-reader">
          <div className="kos-reader-toolbar">
            <span>{content ? `${content.length} characters extracted` : 'Document text reader'}</span>
            <Link
              to={`/ask?resource=${resource.id}`}
              className="kos-button kos-button--sm"
            >
              <BrainCircuit size={14} /> Ground with RAG
            </Link>
          </div>
          {contentLoading ? (
            <div className="kos-box-empty">
              <Loader2 size={24} className="kos-spin-fast" />
              <p>Đang tải và xử lý nội dung văn bản từ lưu trữ...</p>
            </div>
          ) : contentError ? (
            <div className="kos-box-empty">
              <BookOpen size={24} />
              <p style={{ color: 'var(--kos-danger, #ef4444)' }}>{contentError}</p>
              <button
                type="button"
                className="kos-button kos-button--sm"
                onClick={handleRetryReader}
                style={{ marginTop: '0.5rem' }}
              >
                <RefreshCw size={14} /> Thử lại
              </button>
            </div>
          ) : content ? (
            <pre className="kos-reader-content">{content}</pre>
          ) : (
            <div className="kos-box-empty">
              <BookOpen size={24} />
              <p>No text has been extracted for this resource yet.</p>
            </div>
          )}
        </div>
      )}

      {/* Tab: Notes */}
      {tab === 'Notes' && (
        <div className="kos-workspace-panel">
          <form className="kos-note-form" onSubmit={addNote}>
            <textarea
              value={draft}
              onChange={e => setDraft(e.target.value)}
              placeholder="Write a private note about this resource…"
              rows={3}
              required
            />
            <button
              type="submit"
              className="kos-button kos-button--primary"
              disabled={noteBusy || !draft.trim()}
            >
              {noteBusy ? 'Saving…' : 'Save Note'}
            </button>
          </form>

          <div className="kos-notes-list">
            {notesLoading ? (
              <div className="kos-box-empty">
                <Loader2 size={20} className="kos-spin-fast" />
                <p>Loading notes…</p>
              </div>
            ) : notes && notes.length > 0 ? (
              notes.map(n => (
                <div key={n.id} className="kos-note-card">
                  <p className="kos-note-body">{n.content}</p>
                  <div className="kos-note-footer">
                    <small>{n.createdAt ? new Date(n.createdAt).toLocaleString() : '—'}</small>
                    <button
                      type="button"
                      className="kos-icon-btn kos-icon-btn--danger"
                      onClick={() => removeNote(n.id)}
                      aria-label="Delete note"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <p className="kos-box-empty">No notes yet. Add your first note above.</p>
            )}
          </div>
        </div>
      )}

      {/* Tab: Related */}
      {tab === 'Related' && (
        <div className="kos-workspace-panel">
          <div className="kos-related-list">
            {relatedLoading ? (
              <div className="kos-box-empty">
                <Loader2 size={20} className="kos-spin-fast" />
                <p>Loading related resources…</p>
              </div>
            ) : related && related.length > 0 ? (
              related.map(r => (
                <div key={r.id} className="kos-related-card">
                  <span className="kos-workspace-type-badge">{r.resourceType || 'DOCUMENT'}</span>
                  <div className="kos-related-info">
                    <h4>{r.title || 'Untitled'}</h4>
                    <p>{r.description || 'No description'}</p>
                  </div>
                  <Link to={`/library/${r.id}`} className="kos-button kos-button--sm">
                    Open
                  </Link>
                </div>
              ))
            ) : (
              <p className="kos-box-empty">
                No related resources found. Organize collections or tags to connect knowledge.
              </p>
            )}
          </div>
        </div>
      )}

      {/* Tab: Activity */}
      {tab === 'Activity' && (
        <div className="kos-workspace-panel">
          <div className="kos-activity-summary-card">
            <h3>Reading Progress</h3>
            <p>
              Current progress: <strong>{progressVal !== null ? progressVal : (activity?.progressPercent ?? 0)}%</strong> · Notes:{' '}
              <strong>{activity?.noteCount ?? 0}</strong>
            </p>
            <input
              type="range"
              min={0}
              max={100}
              value={progressVal !== null ? progressVal : (activity?.progressPercent ?? 0)}
              onChange={e => setProgressVal(Number(e.target.value))}
              onPointerUp={() => {
                if (progressVal !== null) {
                  saveProgress(progressVal)
                }
              }}
              onKeyUp={() => {
                if (progressVal !== null) {
                  saveProgress(progressVal)
                }
              }}
              className="kos-range-slider"
              style={{ maxWidth: '100%', width: '100%', margin: '.6rem 0' }}
            />
            <small style={{ color: 'var(--kos-muted)' }}>
              Status: {activity?.processingStatus || resource.processingStatus || 'READY'} · Last updated:{' '}
              {activity?.updatedAt ? new Date(activity.updatedAt).toLocaleString() : 'Not yet recorded'}
            </small>
          </div>

          {/* Grounded Active Recall & Learning Studio Card */}
          <div className="kos-quiz-pane" style={{ background: 'var(--kos-surface)', padding: '1.5rem', borderRadius: '8px', border: '1px solid var(--kos-line)' }}>
            <div className="kos-quiz-header">
              <div>
                <span className="kos-banner-kicker">TOPIC DEEPDIVE & ACTIVE RECALL</span>
                <h3 style={{ margin: '0.2rem 0' }}>🎯 Kiểm Tra Ghi Nhớ & Khái Niệm Chuyên Sâu</h3>
                <p style={{ margin: 0, color: 'var(--kos-muted)', fontSize: '0.86rem' }}>
                  Hệ thống trắc nghiệm và bóc tách khái niệm đối chứng 100% từ phân đoạn tài liệu nguồn
                </p>
              </div>
            </div>

            <div style={{ padding: '1.25rem', background: 'var(--kos-subtle)', borderRadius: '8px', marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--kos-ink)', lineHeight: '1.5' }}>
                💡 Tài liệu này đã được phân đoạn (chunking) và vector hóa. Bạn có thể mở <strong>Topic Deepdive Studio</strong> để xây dựng lộ trình học tập theo khái niệm trọng tâm hoặc làm bài kiểm tra <strong>Active Recall 5 câu hỏi có đối chứng</strong>.
              </p>
              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginTop: '0.25rem' }}>
                <Link to="/focus" className="kos-button kos-button--primary">
                  <Compass size={16} /> Mở Topic Deepdive Studio
                </Link>
                <Link to={`/ask?resource=${resource.id}`} className="kos-button">
                  <BrainCircuit size={16} /> Hỏi RAG AI về tài liệu này
                </Link>
              </div>
            </div>
          </div>

          {/* Embedded Knowledge Graph Section */}
          <div style={{ background: 'var(--kos-surface)', padding: '1.25rem', borderRadius: '8px', border: '1px solid var(--kos-line)' }}>
            <h3 style={{ font: '700 1.05rem var(--kos-font-display)', margin: '0 0 1rem', display: 'flex', alignItems: 'center', gap: '.4rem' }}>
              <Network size={18} /> Sơ đồ Mạng lưới Tri thức Liên quan
            </h3>
            <KnowledgeGraphView
              resources={related && related.length ? ([resource, ...related].filter(Boolean) as unknown as Resource[]) : (allResources || []).slice(0, 8)}
              collections={collections || []}
              tags={tags || []}
            />
          </div>
        </div>
      )}
    </section>
  )
}
