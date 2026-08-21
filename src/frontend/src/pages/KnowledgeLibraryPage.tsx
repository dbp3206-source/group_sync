import {
  Check,
  FileText,
  FolderPlus,
  LayoutGrid,
  MoreHorizontal,
  Network,
  Pencil,
  Plus,
  Search,
  Sparkles,
  Tags,
  Trash2,
  Upload,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  assignResourceToCollection,
  assignResourcesToCollection,
  autoOrganizeAll,
  createCollection,
  createNote,
  deleteCollection,
  deleteResource,
  deleteResources,
  getCollectionResources,
  getCollections,
  getResource,
  getResources,
  getTags,
  removeResourceFromCollection,
  retryResource,
  updateCollection,
  uploadResource,
  type KnowledgeCollection,
  type KnowledgeTag,
  type OrganizationBatchResult,
  type Resource,
} from '../api/knowledge'
import { getApiErrorMessage } from '../api/errors'
import KnowledgeGraphView from '../components/KnowledgeGraphView'
import ResourceStatus from '../components/ResourceStatus'

type LibraryState = 'INITIAL_LOADING' | 'ERROR' | 'EMPTY' | 'CONTENT'
type CollectionModal = 'CREATE' | 'EDIT' | null
type ConfirmAction = { type: 'RESOURCE'; resource: Resource } | { type: 'COLLECTION'; collection: KnowledgeCollection } | null

const PROCESSING_STATUSES = new Set(['UPLOADING', 'UPLOADED', 'PARSING', 'CHUNKING', 'EMBEDDING'])

function mergeResources(current: Resource[], incoming: Resource[]) {
  const byId = new Map(current.map(resource => [resource.id, resource]))
  incoming.forEach(resource => byId.set(resource.id, resource))
  return [...byId.values()]
}
export default function KnowledgeLibraryPage() {
  const [resources, setResources] = useState<Resource[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [query, setQuery] = useState('')
  const [activeQuery, setActiveQuery] = useState('')
  const [tagId, setTagId] = useState<number | undefined>()
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [sort, setSort] = useState('updated_desc')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [initialLoadComplete, setInitialLoadComplete] = useState(false)
  const [loading, setLoading] = useState(false)
  const [resourceError, setResourceError] = useState('')
  const [collectionLoadError, setCollectionLoadError] = useState('')
  const [tagLoadError, setTagLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [noteOpen, setNoteOpen] = useState(false)
  const [collectionModal, setCollectionModal] = useState<CollectionModal>(null)
  const [editingCollection, setEditingCollection] = useState<KnowledgeCollection | null>(null)
  const [collectionName, setCollectionName] = useState('')
  const [collectionDescription, setCollectionDescription] = useState('')
  const [collectionError, setCollectionError] = useState('')
  const [collectionBusy, setCollectionBusy] = useState(false)
  const [detailCollection, setDetailCollection] = useState<KnowledgeCollection | null>(null)
  const [detailResources, setDetailResources] = useState<Resource[]>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [bulkCollectionId, setBulkCollectionId] = useState<number | undefined>()
  const [confirmAction, setConfirmAction] = useState<ConfirmAction>(null)
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [viewMode, setViewMode] = useState<'GRID' | 'GRAPH'>('GRID')
  const [organizationResult, setOrganizationResult] = useState<OrganizationBatchResult | null>(null)

  const resourcesRef = useRef(resources)
  const filtersRef = useRef({ activeQuery, tagId, collectionId, sort })
  const pollingRef = useRef(false)
  resourcesRef.current = resources
  filtersRef.current = { activeQuery, tagId, collectionId, sort }

  const load = useCallback(async (
    q = filtersRef.current.activeQuery,
    nextTag = filtersRef.current.tagId,
    nextCollection = filtersRef.current.collectionId,
    nextPage = 0,
    nextSort = filtersRef.current.sort,
    append = false,
  ) => {
    if (!append && resourcesRef.current.length === 0) setLoading(true)
    try {
      const response = await getResources(q, nextTag, nextCollection, nextPage, 24, nextSort)
      setResourceError('')
      setResources(current => append ? mergeResources(current, response.items) : response.items)
      setPage(response.page)
      setTotalPages(response.totalPages)
      setTotalItems(response.totalItems)
      setHasNext(response.hasNext)
      if (!append) setSelectedIds([])
    } catch (err) {
      setResourceError(getApiErrorMessage(err, 'Your library could not be loaded.'))
    } finally {
      setInitialLoadComplete(true)
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const interval = window.setInterval(async () => {
      const processingIds = resourcesRef.current
        .filter(resource => resource.id > 0 && PROCESSING_STATUSES.has(resource.processingStatus))
        .map(resource => resource.id)
      if (!processingIds.length || pollingRef.current) return
      pollingRef.current = true
      try {
        const updated = await Promise.all(processingIds.map(id => getResource(id).catch(() => null)))
        const updatedMap = new Map(updated.filter((resource): resource is Resource => Boolean(resource)).map(resource => [resource.id, resource]))
        if (updatedMap.size) setResources(current => current.map(resource => updatedMap.get(resource.id) ?? resource))
      } finally {
        pollingRef.current = false
      }
    }, 3000)
    return () => window.clearInterval(interval)
  }, [])

  const refreshCollections = useCallback(async () => {
    const values = await getCollections()
    setCollections(values)
    setCollectionLoadError('')
    setDetailCollection(current => current ? values.find(collection => collection.id === current.id) ?? null : null)
  }, [])

  const refreshTags = useCallback(async () => {
    const values = await getTags()
    setTags(values)
    setTagLoadError('')
  }, [])

  useEffect(() => {
    void load()
    void refreshCollections().catch(err => setCollectionLoadError(getApiErrorMessage(err, 'Collections could not be loaded.')))
    void refreshTags().catch(err => setTagLoadError(getApiErrorMessage(err, 'Tags could not be loaded.')))
  }, [load, refreshCollections, refreshTags])

  async function retryCollections() {
    setCollectionLoadError('')
    try {
      await refreshCollections()
    } catch (err) {
      setCollectionLoadError(getApiErrorMessage(err, 'Collections could not be loaded.'))
    }
  }

  async function organizeLibrary() {
    setBusy(true)
    setActionError('')
    setOrganizationResult(null)
    try {
      const result = await autoOrganizeAll()
      setOrganizationResult(result)
      await load()
      await refreshCollections().catch(err => setCollectionLoadError(getApiErrorMessage(err, 'Collections could not be loaded.')))
      await refreshTags().catch(err => setTagLoadError(getApiErrorMessage(err, 'Tags could not be loaded.')))
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Auto-organize could not be completed.'))
    } finally {
      setBusy(false)
    }
  }

  async function retryTags() {
    setTagLoadError('')
    try {
      await refreshTags()
    } catch (err) {
      setTagLoadError(getApiErrorMessage(err, 'Tags could not be loaded.'))
    }
  }

  async function saveNote(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || !content.trim()) {
      setActionError('Add a title and note content before saving.')
      return
    }
    setBusy(true)
    try {
      await createNote(title, content)
      setTitle('')
      setContent('')
      setNoteOpen(false)
      await load()
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The note could not be saved.'))
    } finally {
      setBusy(false)
    }
  }

  async function importFile(file?: File) {
    if (!file) return
    const temporaryId = -Date.now()
    const temporaryResource: Resource = {
      id: temporaryId,
      title: file.name.replace(/\.[^/.]+$/, '') || file.name,
      description: null,
      resourceType: 'FILE',
      processingStatus: 'UPLOADING',
      processingError: null,
      favorite: false,
      priority: 0,
      originalFilename: file.name,
      sizeBytes: file.size,
      createdAt: new Date().toISOString(),
    }
    setResources(current => [temporaryResource, ...current])
    setTotalItems(current => current + 1)
    setBusy(true)
    setActionError('')
    try {
      const uploaded = await uploadResource(file)
      setResources(current => [uploaded, ...current.filter(resource => resource.id !== temporaryId && resource.id !== uploaded.id)])
      await load()
    } catch (err) {
      setResources(current => current.filter(resource => resource.id !== temporaryId))
      setTotalItems(current => Math.max(0, current - 1))
      setActionError(getApiErrorMessage(err, 'The resource could not be imported. Please verify file format and size.'))
    } finally {
      setBusy(false)
    }
  }

  function openCreateCollection() {
    setEditingCollection(null)
    setCollectionName('')
    setCollectionDescription('')
    setCollectionError('')
    setCollectionModal('CREATE')
  }

  function openEditCollection(collection: KnowledgeCollection) {
    setEditingCollection(collection)
    setCollectionName(collection.name)
    setCollectionDescription(collection.description ?? '')
    setCollectionError('')
    setCollectionModal('EDIT')
  }

  async function saveCollection(event: FormEvent) {
    event.preventDefault()
    setCollectionBusy(true)
    setCollectionError('')
    try {
      const saved = editingCollection
        ? await updateCollection(editingCollection.id, collectionName.trim(), collectionDescription)
        : await createCollection(collectionName.trim(), collectionDescription)
      setCollections(current => editingCollection
        ? current.map(collection => collection.id === saved.id ? saved : collection)
        : [saved, ...current])
      if (detailCollection?.id === saved.id) setDetailCollection(saved)
      setCollectionModal(null)
      setCollectionError('')
    } catch (err) {
      setCollectionError(getApiErrorMessage(err, 'The collection could not be saved.'))
    } finally {
      setCollectionBusy(false)
    }
  }

  async function openCollection(collection: KnowledgeCollection) {
    setDetailCollection(collection)
    setDetailLoading(true)
    try {
      setDetailResources(await getCollectionResources(collection.id))
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The collection could not be opened.'))
    } finally {
      setDetailLoading(false)
    }
  }

  async function confirmCollectionDelete(collection: KnowledgeCollection) {
    setBusy(true)
    try {
      await deleteCollection(collection.id)
      setCollections(current => current.filter(value => value.id !== collection.id))
      if (detailCollection?.id === collection.id) {
        setDetailCollection(null)
        setDetailResources([])
      }
      if (collectionId === collection.id) {
        setCollectionId(undefined)
        await load(activeQuery, tagId, undefined, 0, sort, false)
      }
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The collection could not be deleted.'))
    } finally {
      setBusy(false)
      setConfirmAction(null)
    }
  }

  async function assign(collection: number, resource: number) {
    try {
      await assignResourceToCollection(collection, resource)
      await refreshCollections()
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The resource could not be added to that collection.'))
    }
  }

  async function removeFromCollection(resource: Resource) {
    if (!detailCollection) return
    try {
      await removeResourceFromCollection(detailCollection.id, resource.id)
      setDetailResources(current => current.filter(value => value.id !== resource.id))
      setCollections(current => current.map(collection => collection.id === detailCollection.id
        ? { ...collection, resourceCount: Math.max(0, (collection.resourceCount ?? 1) - 1) }
        : collection))
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The resource could not be removed from that collection.'))
    }
  }

  async function deleteSingleResource(resource: Resource) {
    setBusy(true)
    try {
      await deleteResource(resource.id)
      setResources(current => current.filter(value => value.id !== resource.id))
      setSelectedIds(current => current.filter(id => id !== resource.id))
      setTotalItems(current => Math.max(0, current - 1))
      await refreshCollections()
      if (detailCollection) setDetailResources(current => current.filter(value => value.id !== resource.id))
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The resource could not be deleted.'))
    } finally {
      setBusy(false)
      setConfirmAction(null)
    }
  }

  async function bulkAdd() {
    if (!bulkCollectionId || !selectedIds.length) return
    setBusy(true)
    try {
      await assignResourcesToCollection(bulkCollectionId, selectedIds)
      setSelectedIds([])
      setBulkCollectionId(undefined)
      await refreshCollections()
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The selected resources could not be added.'))
    } finally {
      setBusy(false)
    }
  }

  async function bulkDelete() {
    setBusy(true)
    try {
      await deleteResources(selectedIds)
      const selected = new Set(selectedIds)
      setResources(current => current.filter(resource => !selected.has(resource.id)))
      setTotalItems(current => Math.max(0, current - selectedIds.length))
      setSelectedIds([])
      await refreshCollections()
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The selected resources could not be deleted.'))
    } finally {
      setBusy(false)
      setBulkDeleteOpen(false)
    }
  }

  async function retry(resource: Resource) {
    try {
      const updated = await retryResource(resource.id)
      setResources(current => current.map(value => value.id === updated.id ? updated : value))
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'The resource could not be retried.'))
    }
  }

  function applySearch() {
    const nextQuery = query.trim()
    setActiveQuery(nextQuery)
    void load(nextQuery, tagId, collectionId, 0, sort, false)
  }

  function clearFilters() {
    setQuery('')
    setActiveQuery('')
    setTagId(undefined)
    setCollectionId(undefined)
    void load('', undefined, undefined, 0, sort, false)
  }

  const hasFilters = Boolean(activeQuery || tagId || collectionId)
  const currentIds = resources.filter(resource => resource.id > 0).map(resource => resource.id)
  const allCurrentSelected = currentIds.length > 0 && currentIds.every(id => selectedIds.includes(id))
  const libraryState: LibraryState = !initialLoadComplete ? 'INITIAL_LOADING' : resourceError ? 'ERROR' : resources.length === 0 ? 'EMPTY' : 'CONTENT'

  function toggleSelection(resourceId: number) {
    setSelectedIds(current => current.includes(resourceId) ? current.filter(id => id !== resourceId) : [...current, resourceId])
  }

  return (
    <section className="kos-page kos-library">
      <header className="kos-page-header">
        <div>
          <p className="kos-kicker">YOUR LIBRARY</p>
          <h1>Keep the good stuff close.</h1>
          <p className="kos-page-intro">A calm shelf for the files, notes, and ideas you want ready when questions arrive.</p>
        </div>
        <div className="kos-library-actions">
          <label className="kos-button">
            <Upload size={17} />
            {busy ? 'Working...' : 'Import file'}
            <input type="file" accept=".pdf,.docx,.txt,.md,.markdown" hidden disabled={busy} onChange={event => { const file = event.target.files?.[0]; event.target.value = ''; void importFile(file) }} />
          </label>
          <button className="kos-button" onClick={openCreateCollection}><FolderPlus size={17} /> Collection</button>
          <button className="kos-button kos-button--primary" onClick={() => setNoteOpen(true)}><Plus size={17} /> New note</button>
        </div>
      </header>

      <section className="kos-library-collections" aria-label="Collections">
        <div className="kos-section-heading">
          <div><p className="kos-kicker">ORGANIZE</p><h2>Collections</h2></div>
          <button type="button" className="kos-button kos-button--quiet" onClick={openCreateCollection}><Plus size={15} /> New collection</button>
        </div>
        {collectionLoadError ? (
          <div className="kos-library-dependency-warning" role="alert"><span>Collections are temporarily unavailable. Resource content is still available.</span><button type="button" className="kos-button kos-button--quiet" onClick={() => void retryCollections()}>Retry collections</button></div>
        ) : collections.length ? (
          <div className="kos-collection-grid">
            {collections.map(collection => (
              <article key={collection.id} className={`kos-collection-card ${detailCollection?.id === collection.id ? 'is-selected' : ''}`}>
                <button type="button" className="kos-collection-card-main" onClick={() => void openCollection(collection)}>
                  <FolderPlus size={18} aria-hidden="true" />
                  <span><strong>{collection.name}</strong><small>{collection.resourceCount ?? 0} resources</small></span>
                  {detailCollection?.id === collection.id && <Check size={16} aria-label="Open" />}
                </button>
                <div className="kos-collection-card-actions">
                  <button type="button" className="kos-icon-btn" onClick={() => openEditCollection(collection)} aria-label={`Edit ${collection.name}`}><Pencil size={15} /></button>
                  <button type="button" className="kos-icon-btn" onClick={() => setConfirmAction({ type: 'COLLECTION', collection })} aria-label={`Delete ${collection.name}`}><Trash2 size={15} /></button>
                </div>
              </article>
            ))}
          </div>
        ) : <p className="kos-muted-note">Create a collection when a group of resources deserves its own shelf.</p>}
      </section>

      {detailCollection && (
        <section className="kos-collection-detail" aria-label={`${detailCollection.name} collection`}>
          <div className="kos-section-heading">
            <div><p className="kos-kicker">COLLECTION</p><h2>{detailCollection.name}</h2><p>{detailCollection.description || 'No description yet.'}</p></div>
            <button type="button" className="kos-icon-btn" onClick={() => setDetailCollection(null)} aria-label="Close collection detail"><X size={17} /></button>
          </div>
          {detailLoading ? <p className="kos-muted-note">Loading collection resources...</p> : detailResources.length ? (
            <div className="kos-collection-resource-list">
              {detailResources.map(resource => <div key={resource.id} className="kos-collection-resource-row"><Link to={`/library/${resource.id}`}>{resource.title}</Link><ResourceStatus resource={resource} compact /><button type="button" className="kos-button kos-button--quiet" onClick={() => void removeFromCollection(resource)}>Remove</button></div>)}
            </div>
          ) : <p className="kos-muted-note">This collection has no resources yet.</p>}
        </section>
      )}

      <div className="kos-library-toolbar">
        <label className="kos-search-field"><Search size={18} /><input aria-label="Search resource titles" value={query} onChange={event => setQuery(event.target.value)} onKeyDown={event => event.key === 'Enter' && applySearch()} placeholder="Search titles, then press Enter" /></label>
        <label className="kos-filter"><Tags size={16} /><select aria-label="Filter by tag" disabled={Boolean(tagLoadError)} value={tagId ?? ''} onChange={event => { const next = event.target.value ? Number(event.target.value) : undefined; setTagId(next); void load(activeQuery, next, collectionId, 0, sort, false) }}><option value="">{tagLoadError ? 'Tags unavailable' : 'All tags'}</option>{tags.map(tag => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select></label>
        <label className="kos-filter"><FolderPlus size={16} /><select aria-label="Filter by collection" disabled={Boolean(collectionLoadError)} value={collectionId ?? ''} onChange={event => { const next = event.target.value ? Number(event.target.value) : undefined; setCollectionId(next); void load(activeQuery, tagId, next, 0, sort, false) }}><option value="">{collectionLoadError ? 'Collections unavailable' : 'All collections'}</option>{collections.map(collection => <option key={collection.id} value={collection.id}>{collection.name}</option>)}</select></label>
        <label className="kos-filter"><select aria-label="Sort resources" value={sort} onChange={event => { const nextSort = event.target.value; setSort(nextSort); void load(activeQuery, tagId, collectionId, 0, nextSort, false) }}><option value="updated_desc">Recently updated</option><option value="created_desc">Recently created</option><option value="title_asc">Title (A-Z)</option><option value="title_desc">Title (Z-A)</option></select></label>
        <button type="button" className="kos-button" disabled={busy} onClick={() => void organizeLibrary()}><Sparkles size={15} /> {busy ? 'Analyzing...' : 'Auto-Organize'}</button>
        {hasFilters && <button type="button" className="kos-button kos-button--ghost" onClick={clearFilters}><X size={15} /> {activeQuery && !tagId && !collectionId ? 'Clear search' : 'Clear all filters'}</button>}
        <div className="kos-view-switcher"><button type="button" className={`kos-view-btn ${viewMode === 'GRID' ? 'is-active' : ''}`} onClick={() => setViewMode('GRID')}><LayoutGrid size={15} /> Grid</button><button type="button" className={`kos-view-btn ${viewMode === 'GRAPH' ? 'is-active' : ''}`} onClick={() => setViewMode('GRAPH')}><Network size={15} /> Knowledge map</button></div>
        <span className="kos-library-counter">{totalItems} resources <span aria-hidden="true">·</span> {collections.length} collections <span aria-hidden="true">·</span> {tags.length} tags</span>
      </div>

      {organizationResult && (
        <section className="kos-semantic-result" aria-live="polite">
          <div>
            <strong>{organizationResult.processed} resources analyzed</strong>
            <p>{organizationResult.assigned} organized automatically, {organizationResult.suggested} need review, {organizationResult.skipped} had no safe match, {organizationResult.failed} could not be analyzed.</p>
          </div>
          {organizationResult.suggested > 0 && (() => {
            const review = organizationResult.results.find(result => result.collectionSuggestions.length + result.newCollectionSuggestions.length > 0)
            return review ? <Link className="kos-button kos-button--quiet" to={`/library/${review.resourceId}`}>Review suggestions</Link> : null
          })()}
          <button type="button" className="kos-icon-btn" onClick={() => setOrganizationResult(null)} aria-label="Close organization result"><X size={15} /></button>
        </section>
      )}

      {tagLoadError && <div className="kos-library-dependency-warning" role="alert"><span>Tags are temporarily unavailable. Resource content is still available.</span><button type="button" className="kos-button kos-button--quiet" onClick={() => void retryTags()}>Retry tags</button></div>}

      {selectedIds.length > 0 && <div className="kos-bulk-bar" role="region" aria-label="Bulk resource actions"><strong>{selectedIds.length} selected</strong><label><span className="sr-only">Collection for selected resources</span><select value={bulkCollectionId ?? ''} onChange={event => setBulkCollectionId(event.target.value ? Number(event.target.value) : undefined)}><option value="">Add to collection...</option>{collections.map(collection => <option key={collection.id} value={collection.id}>{collection.name}</option>)}</select></label><button type="button" className="kos-button kos-button--primary" disabled={!bulkCollectionId || busy} onClick={() => void bulkAdd()}>Add to collection</button><button type="button" className="kos-button kos-button--danger" disabled={busy} onClick={() => setBulkDeleteOpen(true)}><Trash2 size={15} /> Delete selected</button><button type="button" className="kos-button kos-button--quiet" onClick={() => setSelectedIds([])}>Clear selection</button></div>}

      {resourceError && <div className="kos-error kos-library-alert" role="alert"><span>{resourceError}</span><div><button type="button" className="kos-button kos-button--sm" onClick={() => void load()}>Retry resources</button></div></div>}
      {actionError && <div className="kos-error kos-library-alert" role="alert"><span>{actionError}</span><button type="button" className="kos-icon-btn" onClick={() => setActionError('')} aria-label="Close action error"><X size={15} /></button></div>}

      {libraryState === 'INITIAL_LOADING' && <div className="kos-resource-grid kos-resource-grid--skeleton" aria-label="Loading library">{Array.from({ length: 6 }, (_, index) => <div key={index} className="kos-resource-skeleton" />)}</div>}
      {libraryState === 'ERROR' && <div className="kos-empty"><FileText size={28} /><h2>Library unavailable</h2><p>We could not load your resources. Try again when the workspace is reachable.</p><button type="button" className="kos-button" onClick={() => void load()}>Try again</button></div>}
      {libraryState === 'EMPTY' && <div className="kos-empty"><FileText size={28} /><h2>{hasFilters ? 'No resources match those filters.' : 'Nothing here yet.'}</h2><p>{hasFilters ? 'Try clearing one filter or searching a broader title.' : 'Save a note first, then import the resources you want to question later.'}</p>{hasFilters && <button className="kos-button" onClick={clearFilters}>Clear all filters</button>}</div>}

      {libraryState === 'CONTENT' && (viewMode === 'GRAPH' ? <KnowledgeGraphView resources={resources} collections={collections} tags={tags} totalItems={totalItems} /> : <>
        <div className="kos-library-select-all"><label><input type="checkbox" checked={allCurrentSelected} onChange={event => setSelectedIds(event.target.checked ? currentIds : [])} /> Select all loaded resources</label><span>Page {page + 1} of {totalPages}</span></div>
        <div className="kos-resource-grid">
          {resources.map((resource, index) => {
            const selectable = resource.id > 0
            return <article className={`kos-resource ${selectedIds.includes(resource.id) ? 'is-selected' : ''}`} key={resource.id}>
              <div className="kos-resource-card-head"><label className="kos-resource-check"><input type="checkbox" checked={selectedIds.includes(resource.id)} disabled={!selectable} onChange={() => toggleSelection(resource.id)} aria-label={`Select ${resource.title}`} /><span /></label><span className="kos-resource-type">{resource.resourceType}</span><details className="kos-resource-menu"><summary aria-label={`Actions for ${resource.title}`}><MoreHorizontal size={18} /></summary><div className="kos-resource-menu-panel"><Link to={`/library/${resource.id}`}>Open</Link><Link to={`/ask?resource=${resource.id}`}>Ask</Link>{collections.length > 0 && <label>Add to collection<select defaultValue="" onChange={event => { if (event.target.value) void assign(Number(event.target.value), resource.id) }}><option value="">Choose...</option>{collections.map(collection => <option key={collection.id} value={collection.id}>{collection.name}</option>)}</select></label>}<button type="button" onClick={() => setConfirmAction({ type: 'RESOURCE', resource })}><Trash2 size={14} /> Delete</button></div></details></div>
              <Link className="kos-resource-main" to={`/library/${resource.id}`}><div className={`kos-resource-cover kos-resource-cover--${index % 4}`}><FileText size={26} /><span>{resource.originalFilename || resource.resourceType}</span></div><div className="kos-resource-body"><h2 title={resource.title}>{resource.title}</h2><p>{resource.description || 'No description yet.'}</p></div></Link>
              <div className="kos-resource-footer"><ResourceStatus resource={resource} />{resource.processingStatus === 'FAILED' && <details className="kos-resource-failure"><summary>View reason</summary><p>{resource.processingError || 'Processing stopped unexpectedly.'}</p><button type="button" className="kos-button kos-button--quiet" onClick={() => void retry(resource)}>Retry</button></details>}</div>
            </article>
          })}
        </div>
        {hasNext && <div className="kos-load-more"><button type="button" className="kos-button kos-button--quiet" disabled={busy || loading} onClick={() => void load(activeQuery, tagId, collectionId, page + 1, sort, true)}>Load more <span>({resources.length} / {totalItems})</span></button></div>}
      </>)}

      {noteOpen && <div className="kos-modal" role="dialog" aria-modal="true" aria-labelledby="note-modal-title"><form onSubmit={saveNote}><button className="kos-modal-close" type="button" onClick={() => setNoteOpen(false)} aria-label="Close note dialog"><X size={18} /></button><p className="kos-kicker">NEW NOTE</p><h2 id="note-modal-title">Save a thought</h2><label>Title<input value={title} onChange={event => setTitle(event.target.value)} required /></label><label>Thoughts<textarea value={content} onChange={event => setContent(event.target.value)} required /></label><button className="kos-button kos-button--primary" disabled={busy || !title.trim() || !content.trim()}>{busy ? 'Saving...' : 'Save note'}</button></form></div>}

      {collectionModal && <div className="kos-modal" role="dialog" aria-modal="true" aria-labelledby="collection-modal-title"><form onSubmit={saveCollection}><button className="kos-modal-close" type="button" onClick={() => { setCollectionModal(null); setCollectionError('') }} aria-label="Close collection dialog"><X size={18} /></button><p className="kos-kicker">{collectionModal === 'EDIT' ? 'EDIT COLLECTION' : 'NEW COLLECTION'}</p><h2 id="collection-modal-title">{collectionModal === 'EDIT' ? 'Shape the shelf' : 'Create a shelf'}</h2>{collectionError && <div className="kos-modal-error" role="alert">{collectionError}</div>}<label>Name<input value={collectionName} onChange={event => setCollectionName(event.target.value)} required /></label><label>Description <span className="kos-label-optional">optional</span><textarea value={collectionDescription} onChange={event => setCollectionDescription(event.target.value)} rows={4} /></label><div className="kos-modal-actions"><button type="button" className="kos-button kos-button--quiet" onClick={() => { setCollectionModal(null); setCollectionError('') }}>Cancel</button><button className="kos-button kos-button--primary" disabled={collectionBusy}>{collectionBusy ? 'Saving...' : collectionModal === 'EDIT' ? 'Save changes' : 'Create collection'}</button></div></form></div>}

      {confirmAction && <div className="kos-modal" role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title"><form onSubmit={event => { event.preventDefault(); if (confirmAction.type === 'RESOURCE') void deleteSingleResource(confirmAction.resource); else void confirmCollectionDelete(confirmAction.collection) }}><button className="kos-modal-close" type="button" onClick={() => setConfirmAction(null)} aria-label="Close confirmation"><X size={18} /></button><p className="kos-kicker">CONFIRM ACTION</p><h2 id="confirm-modal-title">{confirmAction.type === 'RESOURCE' ? 'Delete this resource?' : `Delete ${confirmAction.collection.name}?`}</h2><p>{confirmAction.type === 'RESOURCE' ? 'The stored file and its library membership will be removed. This cannot be undone.' : 'The collection will be removed, but its resources will stay in your library.'}</p><div className="kos-modal-actions"><button type="button" className="kos-button kos-button--quiet" onClick={() => setConfirmAction(null)}>Cancel</button><button className="kos-button kos-button--danger" disabled={busy}>{busy ? 'Deleting...' : 'Delete'}</button></div></form></div>}

      {bulkDeleteOpen && <div className="kos-modal" role="dialog" aria-modal="true" aria-labelledby="bulk-delete-title"><form onSubmit={event => { event.preventDefault(); void bulkDelete() }}><button className="kos-modal-close" type="button" onClick={() => setBulkDeleteOpen(false)} aria-label="Close bulk delete confirmation"><X size={18} /></button><p className="kos-kicker">BULK ACTION</p><h2 id="bulk-delete-title">Delete {selectedIds.length} resources?</h2><p>All selected files will be removed from your library. This cannot be undone.</p><div className="kos-modal-actions"><button type="button" className="kos-button kos-button--quiet" onClick={() => setBulkDeleteOpen(false)}>Cancel</button><button className="kos-button kos-button--danger" disabled={busy}>{busy ? 'Deleting...' : 'Delete selected'}</button></div></form></div>}
    </section>
  )
}
