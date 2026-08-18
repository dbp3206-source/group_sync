import {
  ArrowLeft,
  BookOpen,
  BrainCircuit,
  Compass,
  FileText,
  Network,
  Sparkles,
  Trash2,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  autoOrganizeResource,
  createResourceNote,
  deleteResource,
  deleteResourceNote,
  getCollections,
  getRelatedResources,
  getResource,
  getResourceActivity,
  getResourceContent,
  getResourceNotes,
  getResources,
  getTags,
  updateResourceProgress,
  type KnowledgeCollection,
  type KnowledgeTag,
  type Resource,
  type ResourceActivity,
  type ResourceNote,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

type Tab = 'Overview' | 'Reader' | 'Notes' | 'Related' | 'Activity'

export default function ResourceWorkspacePage() {
  const resourceId = Number(useParams().resourceId)
  const navigate = useNavigate()
  const [resource, setResource] = useState<Resource | null>(null)
  const [content, setContent] = useState('')
  const [tab, setTab] = useState<Tab>('Overview')
  const [notes, setNotes] = useState<ResourceNote[]>([])
  const [related, setRelated] = useState<Resource[]>([])
  const [activity, setActivity] = useState<ResourceActivity | null>(null)
  const [allResources, setAllResources] = useState<Resource[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [draft, setDraft] = useState('')
  const [noteBusy, setNoteBusy] = useState(false)
  const [error, setError] = useState('')
  const [autoOrganizeMsg, setAutoOrganizeMsg] = useState('')
  const [organizing, setOrganizing] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const load = () =>
    Promise.all([
      getResource(resourceId),
      getResourceContent(resourceId),
      getResourceNotes(resourceId),
      getRelatedResources(resourceId),
      getResourceActivity(resourceId),
      getResources().catch(() => []),
      getCollections().catch(() => []),
      getTags().catch(() => []),
    ])
      .then(([item, text, n, r, a, allR, cols, t]) => {
        setResource(item)
        setContent(text)
        setNotes(n)
        setRelated(r)
        setActivity(a)
        setAllResources(allR)
        setCollections(cols)
        setTags(t)
      })
      .catch(() => setError('This resource could not be loaded.'))

  useEffect(() => {
    load()
  }, [resourceId])

  async function handleAutoOrganize() {
    setOrganizing(true)
    setAutoOrganizeMsg('')
    try {
      await autoOrganizeResource(resourceId)
      setAutoOrganizeMsg('Đã tự động phân loại vào Collection & Tags phù hợp!')
      setTimeout(() => setAutoOrganizeMsg(''), 4000)
      await load()
    } catch {
      setAutoOrganizeMsg('Không thể tự động phân loại.')
    } finally {
      setOrganizing(false)
    }
  }

  async function addNote(e: React.FormEvent) {
    e.preventDefault()
    if (!draft.trim()) return
    setNoteBusy(true)
    try {
      const created = await createResourceNote(resourceId, draft.trim())
      setNotes(prev => [created, ...prev])
      setDraft('')
      setActivity(prev => (prev ? { ...prev, note_count: prev.note_count + 1 } : prev))
    } catch {
      setError('Could not save note.')
    } finally {
      setNoteBusy(false)
    }
  }

  async function removeNote(noteId: number) {
    try {
      await deleteResourceNote(resourceId, noteId)
      setNotes(prev => prev.filter(n => n.id !== noteId))
      setActivity(prev => (prev ? { ...prev, note_count: Math.max(0, prev.note_count - 1) } : prev))
    } catch {
      setError('Could not delete note.')
    }
  }

  async function saveProgress(percent: number) {
    try {
      const updated = await updateResourceProgress(resourceId, percent)
      setActivity(updated)
    } catch {
      setError('Could not update progress.')
    }
  }

  async function handleDelete() {
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

  if (error && !resource)
    return (
      <section className="kos-page">
        <p className="kos-error" role="alert">
          {error}
        </p>
      </section>
    )

  if (!resource)
    return (
      <section className="kos-page">
        <div className="kos-empty" aria-live="polite">
          <FileText size={28} aria-hidden="true" />
          <p>Loading resource…</p>
        </div>
      </section>
    )

  const metaItems = [
    { label: 'Type', value: resource.resourceType },
    { label: 'Status', value: resource.processingStatus },
    { label: 'Priority', value: resource.priority },
    { label: 'Favorite', value: resource.favorite ? 'Yes' : 'No' },
    { label: 'Filename', value: resource.originalFilename || '—' },
    {
      label: 'Size',
      value: resource.sizeBytes
        ? `${(resource.sizeBytes / 1024).toFixed(1)} KB`
        : '—',
    },
    { label: 'Created', value: new Date(resource.createdAt).toLocaleString() },
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
        <span className="kos-workspace-type-badge">{resource.resourceType}</span>
        <h1 className="kos-workspace-title">{resource.title}</h1>
        <div className="kos-workspace-actions">
          <Link
            to={`/knowledge/ask?resources=${resource.id}`}
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
                  <p style={{ margin: '0.5rem 0 0', fontSize: '0.82rem', color: 'var(--kos-green)' }}>
                    {autoOrganizeMsg}
                  </p>
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
          </div>
        </div>
      )}

      {/* Tab: Reader */}
      {tab === 'Reader' && (
        <div className="kos-workspace-panel kos-workspace-reader">
          <div className="kos-reader-toolbar">
            <span>{content ? `${content.length} characters extracted` : 'No text available'}</span>
            <Link
              to={`/knowledge/ask?resources=${resource.id}`}
              className="kos-button kos-button--sm"
            >
              <BrainCircuit size={14} /> Ground with RAG
            </Link>
          </div>
          {content ? (
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
            {notes.length > 0 ? (
              notes.map(n => (
                <div key={n.id} className="kos-note-card">
                  <p className="kos-note-body">{n.content}</p>
                  <div className="kos-note-footer">
                    <small>{new Date(n.created_at).toLocaleString()}</small>
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
            {related.length > 0 ? (
              related.map(r => (
                <div key={r.id} className="kos-related-card">
                  <span className="kos-workspace-type-badge">{r.resourceType}</span>
                  <div className="kos-related-info">
                    <h4>{r.title}</h4>
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
              Current progress: <strong>{activity?.progress_percent ?? 0}%</strong> · Notes:{' '}
              <strong>{activity?.note_count ?? 0}</strong>
            </p>
            <input
              type="range"
              min={0}
              max={100}
              value={activity?.progress_percent ?? 0}
              onChange={e => saveProgress(Number(e.target.value))}
              className="kos-range-slider"
              style={{ maxWidth: '100%', width: '100%', margin: '.6rem 0' }}
            />
            <small style={{ color: 'var(--kos-muted)' }}>
              Status: {activity?.processing_status} · Last updated:{' '}
              {activity?.updated_at ? new Date(activity.updated_at).toLocaleString() : 'Not yet recorded'}
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
                <Link to="/knowledge/focus" className="kos-button kos-button--primary">
                  <Compass size={16} /> Mở Topic Deepdive Studio
                </Link>
                <Link to={`/knowledge/ask?resources=${resource.id}`} className="kos-button">
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
              resources={related.length ? [resource, ...related] : allResources.slice(0, 8)}
              collections={collections}
              tags={tags}
            />
          </div>
        </div>
      )}
    </section>
  )
}
