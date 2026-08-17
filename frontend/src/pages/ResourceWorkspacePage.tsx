import { ArrowLeft, BrainCircuit, Check, FileText, Sparkles, Trash2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { applyOrganization, createResourceNote, deleteResource, deleteResourceNote, getOrganizationSuggestions, getRelatedResources, getResource, getResourceActivity, getResourceContent, getResourceNotes, updateResourceNote, updateResourceProgress, type OrganizationSuggestions, type Resource, type ResourceActivity, type ResourceNote } from '../api/knowledge'

type Tab = 'Overview' | 'Reader' | 'Notes' | 'Related' | 'Activity' | 'Organize'

export default function ResourceWorkspacePage() {
  const resourceId = Number(useParams().resourceId)
  const navigate = useNavigate()
  const [resource, setResource] = useState<Resource | null>(null)
  const [content, setContent] = useState('')
  const [tab, setTab] = useState<Tab>('Overview')
  const [notes, setNotes] = useState<ResourceNote[]>([])
  const [related, setRelated] = useState<Resource[]>([])
  const [activity, setActivity] = useState<ResourceActivity | null>(null)
  const [draft, setDraft] = useState('')
  const [noteBusy, setNoteBusy] = useState(false)
  const [error, setError] = useState('')
  const [suggestions, setSuggestions] = useState<OrganizationSuggestions | null>(null)
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [selectedCollections, setSelectedCollections] = useState<number[]>([])
  const [selectedNewCollections, setSelectedNewCollections] = useState<string[]>([])
  const [selectedRelated, setSelectedRelated] = useState<number[]>([])
  const [organizing, setOrganizing] = useState(false)
  const [organizationMessage, setOrganizationMessage] = useState('')
  const [deleteConfirm, setDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const load = () => Promise.all([
    getResource(resourceId),
    getResourceContent(resourceId),
    getResourceNotes(resourceId),
    getRelatedResources(resourceId),
    getResourceActivity(resourceId),
  ]).then(([item, text, n, r, a]) => {
    setResource(item); setContent(text); setNotes(n); setRelated(r); setActivity(a)
  }).catch(() => setError('This resource could not be loaded.'))

  useEffect(() => { load() }, [resourceId])

  async function openOrganization() {
    setTab('Organize'); setOrganizationMessage('')
    if (suggestions || organizing) return
    setOrganizing(true)
    try { setSuggestions(await getOrganizationSuggestions(resourceId)) }
    catch { setError('Organization suggestions are not available until this resource is ready.') }
    finally { setOrganizing(false) }
  }

  async function saveOrganization() {
    if (!suggestions) return
    setOrganizing(true)
    try {
      await applyOrganization(resourceId, { tagNames: selectedTags, collectionIds: selectedCollections, newCollectionNames: selectedNewCollections, relatedResourceIds: selectedRelated })
      setOrganizationMessage('Reviewed suggestions saved to your library.')
      setSuggestions(null)
      await load()
    } catch { setError('The reviewed organization could not be saved.') }
    finally { setOrganizing(false) }
  }

  async function addNote() {
    if (!draft.trim() || noteBusy) return
    setNoteBusy(true)
    try {
      await createResourceNote(resourceId, draft)
      setDraft('')
      const [nextNotes, nextActivity] = await Promise.all([getResourceNotes(resourceId), getResourceActivity(resourceId)])
      setNotes(nextNotes); setActivity(nextActivity)
    } catch { setError('The note could not be saved.') }
    finally { setNoteBusy(false) }
  }

  async function removeNote(id: number) {
    try { await deleteResourceNote(resourceId, id); setNotes(await getResourceNotes(resourceId)) }
    catch { setError('The note could not be deleted.') }
  }

  async function saveNote(id: number, value: string) {
    try { await updateResourceNote(resourceId, id, value); setNotes(await getResourceNotes(resourceId)) }
    catch { setError('The note could not be updated.') }
  }

  async function progress(value: number) {
    try { setActivity(await updateResourceProgress(resourceId, value)) }
    catch { setError('Progress could not be saved.') }
  }

  async function handleDelete() {
    if (!deleteConfirm) { setDeleteConfirm(true); return }
    setDeleting(true)
    try {
      await deleteResource(resourceId)
      navigate('/library')
    } catch { setError('This resource could not be deleted.'); setDeleteConfirm(false) }
    finally { setDeleting(false) }
  }

  if (error && !resource) return <section className="kos-page"><p className="kos-error" role="alert">{error}</p></section>
  if (!resource) return (
    <section className="kos-page">
      <div className="kos-empty" aria-live="polite">
        <FileText size={28} aria-hidden="true" />
        <p>Loading resource…</p>
      </div>
    </section>
  )

  return (
    <section className="kos-page kos-workspace">
      <Link className="kos-back" to="/library"><ArrowLeft size={16} aria-hidden="true" /> Library</Link>
      <header>
        <div>
          <p className="kos-kicker">{resource.resourceType}</p>
          <h1>{resource.title}</h1>
          <p>{resource.description || 'A resource in your KnowledgeOS library.'}</p>
        </div>
        <div className="kos-workspace-actions">
          <button
            className={`kos-button kos-button--danger${deleteConfirm ? ' is-confirming' : ''}`}
            onClick={handleDelete}
            disabled={deleting}
            aria-label={deleteConfirm ? 'Confirm deletion' : 'Delete this resource'}
          >
            <Trash2 size={17} aria-hidden="true" />
            {deleting ? 'Deleting…' : deleteConfirm ? 'Confirm delete' : 'Delete'}
          </button>
          {deleteConfirm && !deleting && (
            <button className="kos-text-button" onClick={() => setDeleteConfirm(false)}>Cancel</button>
          )}
          <button className="kos-button" onClick={openOrganization}><Sparkles size={17} aria-hidden="true" /> Organize</button>
          <Link className="kos-button kos-button--primary" to={`/ask?resource=${resource.id}`}><BrainCircuit size={17} aria-hidden="true" /> Ask</Link>
        </div>
      </header>

      <nav className="kos-tabs" role="tablist">
        {(['Overview', 'Reader', 'Notes', 'Related', 'Activity', 'Organize'] as Tab[]).map(item => (
          <button
            key={item}
            role="tab"
            aria-selected={tab === item}
            className={tab === item ? 'is-active' : ''}
            onClick={() => item === 'Organize' ? openOrganization() : setTab(item)}
          >
            {item}
          </button>
        ))}
      </nav>

      {error && <p className="kos-error" role="alert">{error}</p>}

      {tab === 'Overview' && (
        <div className="kos-workspace-overview">
          <FileText size={32} aria-hidden="true" />
          <h2>{resource.processingStatus === 'READY' ? 'Ready for retrieval' : 'Still processing'}</h2>
          <p>Status: {resource.processingStatus}. Ask stays constrained to the sources you choose.</p>
        </div>
      )}

      {tab === 'Reader' && <article className="kos-reader">{content}</article>}

      {tab === 'Notes' && (
        <div className="kos-notes">
          <label>
            Add a note
            <textarea
              value={draft}
              onChange={event => setDraft(event.target.value)}
              placeholder="Capture an idea or question…"
              disabled={noteBusy}
            />
          </label>
          <button className="kos-button kos-button--primary" onClick={addNote} disabled={noteBusy || !draft.trim()}>
            {noteBusy ? 'Saving…' : 'Save note'}
          </button>
          {notes.length
            ? notes.map(note => (
                <article key={note.id}>
                  <textarea defaultValue={note.content} onBlur={event => saveNote(note.id, event.target.value)} />
                  <button aria-label="Delete note" onClick={() => removeNote(note.id)}><Trash2 size={15} aria-hidden="true" /></button>
                </article>
              ))
            : <p className="kos-empty">No notes yet.</p>}
        </div>
      )}

      {tab === 'Related' && (
        <div className="kos-related">
          {related.length
            ? related.map(item => (
                <Link key={item.id} to={`/library/${item.id}`}>
                  <p className="kos-kicker">{item.resourceType}</p>
                  <h2>{item.title}</h2>
                  <p>{item.description}</p>
                </Link>
              ))
            : (
                <div className="kos-empty">
                  <h2>No related resources yet.</h2>
                  <p>Review suggestions from Organize when you are ready.</p>
                  <button className="kos-button" onClick={openOrganization}>Find related resources</button>
                </div>
              )}
        </div>
      )}

      {tab === 'Activity' && (
        <div className="kos-activity">
          <h2>Learning progress</h2>
          <p>{activity?.progress_percent ?? 0}% complete · {activity?.note_count ?? 0} notes</p>
          <input
            aria-label="Reading progress"
            type="range"
            min="0"
            max="100"
            value={activity?.progress_percent ?? 0}
            onChange={event => progress(Number(event.target.value))}
          />
          <p>Processing: {activity?.processing_status}</p>
          <p>Last updated: {activity?.updated_at ? new Date(activity.updated_at).toLocaleString() : 'Not yet recorded'}</p>
        </div>
      )}

      {tab === 'Organize' && (
        <div className="kos-organize">
          <div>
            <p className="kos-kicker">REVIEW BEFORE SAVING</p>
            <h2>Give this resource a place to land.</h2>
            <p>Suggestions use its extracted text and nearby library evidence. Nothing is assigned until you confirm.</p>
          </div>
          {organizing && <p className="kos-empty" aria-live="polite">Reading the resource and library context…</p>}
          {organizationMessage && <p className="kos-success"><Check size={16} aria-hidden="true" /> {organizationMessage}</p>}
          {suggestions && (
            <div className="kos-suggestion-list">
              <fieldset>
                <legend>Tags</legend>
                {suggestions.suggestedTags.map(tag => (
                  <label key={tag.name}>
                    <input type="checkbox" checked={selectedTags.includes(tag.name)} onChange={event => setSelectedTags(values => event.target.checked ? [...values, tag.name] : values.filter(value => value !== tag.name))} />
                    <span>{tag.name}</span><small>{tag.reason}</small>
                  </label>
                ))}
              </fieldset>
              <fieldset>
                <legend>Collections</legend>
                {suggestions.suggestedCollections.map(collection => collection.existingCollectionId ? (
                  <label key={collection.existingCollectionId}>
                    <input type="checkbox" checked={selectedCollections.includes(collection.existingCollectionId)} onChange={event => setSelectedCollections(values => event.target.checked ? [...values, collection.existingCollectionId] : values.filter(value => value !== collection.existingCollectionId))} />
                    <span>{collection.name}</span><small>{collection.reason}</small>
                  </label>
                ) : (
                  <label key={collection.name}>
                    <input type="checkbox" checked={selectedNewCollections.includes(collection.name)} onChange={event => setSelectedNewCollections(values => event.target.checked ? [...values, collection.name] : values.filter(value => value !== collection.name))} />
                    <span>Create "{collection.name}"</span><small>{collection.reason}</small>
                  </label>
                ))}
              </fieldset>
              <fieldset>
                <legend>Related resources</legend>
                {suggestions.suggestedRelatedResources.length
                  ? suggestions.suggestedRelatedResources.map(item => (
                      <label key={item.resourceId}>
                        <input type="checkbox" checked={selectedRelated.includes(item.resourceId)} onChange={event => setSelectedRelated(values => event.target.checked ? [...values, item.resourceId] : values.filter(value => value !== item.resourceId))} />
                        <span>{item.title}</span><small>{item.reason}</small>
                      </label>
                    ))
                  : <p>No strong semantic matches found.</p>}
              </fieldset>
              <button className="kos-button kos-button--primary" disabled={organizing} onClick={saveOrganization}><Check size={16} aria-hidden="true" /> Save reviewed suggestions</button>
              <button className="kos-text-button" onClick={() => { setSuggestions(null); setOrganizationMessage('Suggestions dismissed.') }}>Skip for now</button>
            </div>
          )}
        </div>
      )}
    </section>
  )
}
