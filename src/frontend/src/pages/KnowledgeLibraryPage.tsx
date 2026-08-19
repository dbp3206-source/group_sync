import {
  FileText,
  FolderPlus,
  LayoutGrid,
  Network,
  Plus,
  Search,
  Sparkles,
  Tags,
  Upload,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  assignResourceToCollection,
  autoOrganizeAll,
  createCollection,
  createNote,
  getCollections,
  getResource,
  getResources,
  getTags,
  uploadResource,
  type KnowledgeCollection,
  type KnowledgeTag,
  type Resource,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

export default function KnowledgeLibraryPage() {
  const [resources, setResources] = useState<Resource[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [query, setQuery] = useState('')
  const [activeQuery, setActiveQuery] = useState('')
  const [tagId, setTagId] = useState<number | undefined>()
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [sort, setSort] = useState<string>('updated_desc')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [open, setOpen] = useState(false)
  const [collectionOpen, setCollectionOpen] = useState(false)
  const [collectionName, setCollectionName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [viewMode, setViewMode] = useState<'GRID' | 'GRAPH'>('GRID')

  // Refs keep polling callback stable without stale closures
  const activeQueryRef = useRef(activeQuery)
  const tagIdRef = useRef(tagId)
  const collectionIdRef = useRef(collectionId)
  const sortRef = useRef(sort)
  activeQueryRef.current = activeQuery
  tagIdRef.current = tagId
  collectionIdRef.current = collectionId
  sortRef.current = sort

  const load = useCallback(
    (
      q = activeQueryRef.current,
      nextTag = tagIdRef.current,
      nextCollection = collectionIdRef.current,
      nextPage = 0,
      nextSort = sortRef.current,
      append = false
    ) => {
      setError('')
      return getResources(q, nextTag, nextCollection, nextPage, 24, nextSort)
        .then(response => {
          if (append) {
            setResources(prev => [...prev, ...response.items])
          } else {
            setResources(response.items)
          }
          setPage(response.page)
          setTotalPages(response.totalPages)
          setTotalItems(response.totalItems)
          setHasNext(response.hasNext)
        })
        .catch(() => setError('Your library could not be loaded.'))
    },
    [],
  )

  // Targeted polling: refreshes only in-flight resource items without collapsing loaded pages
  const isPollingRef = useRef(false)
  useEffect(() => {
    const PROCESSING = new Set(['UPLOADED', 'UPLOADING', 'PARSING', 'CHUNKING', 'EMBEDDING', 'PROCESSING'])
    const interval = setInterval(async () => {
      const processingIds = resources.filter(r => r && PROCESSING.has(r.processingStatus)).map(r => r.id)
      if (processingIds.length === 0) return
      if (isPollingRef.current) return
      isPollingRef.current = true
      try {
        const updatedList: (Resource | null)[] = await Promise.all(
          processingIds.map(id => getResource(id).catch(() => null))
        )
        const updatedMap = new Map<number, Resource>()
        updatedList.forEach((r: Resource | null) => {
          if (r) updatedMap.set(r.id, r)
        })
        if (updatedMap.size > 0) {
          setResources(current =>
            current.map(item => updatedMap.get(item.id) || item)
          )
        }
      } finally {
        isPollingRef.current = false
      }
    }, 3000)
    return () => clearInterval(interval)
  }, [resources])

  useEffect(() => {
    load()
    getCollections()
      .then(setCollections)
      .catch(() => console.warn('Collections could not be loaded.'))
    getTags()
      .then(setTags)
      .catch(() => console.warn('Tags could not be loaded.'))
  }, [load])

  async function saveNote(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await createNote(title, content)
      setTitle('')
      setContent('')
      setOpen(false)
      await load(activeQueryRef.current, tagIdRef.current, collectionIdRef.current, 0, sortRef.current, false)
    } catch {
      setError('The note could not be saved.')
    }
  }

  async function importFile(file?: File) {
    if (!file) return
    setBusy(true)
    setError('')
    try {
      await uploadResource(file)
      await load(activeQueryRef.current, tagIdRef.current, collectionIdRef.current, 0, sortRef.current, false)
    } catch {
      setError('The resource could not be imported. Please verify file format and size.')
    } finally {
      setBusy(false)
    }
  }

  async function saveCollection(e: React.FormEvent) {
    e.preventDefault()
    try {
      const created = await createCollection(collectionName)
      setCollections(values => [created, ...values])
      setCollectionName('')
      setCollectionOpen(false)
    } catch {
      setError('The collection could not be created.')
    }
  }

  async function assign(collection: number, resource: number) {
    try {
      await assignResourceToCollection(collection, resource)
    } catch {
      setError('The resource could not be added to that collection.')
    }
  }

  function clearFilters() {
    setQuery('')
    setActiveQuery('')
    setTagId(undefined)
    setCollectionId(undefined)
    load('', undefined, undefined)
  }

  const hasFilters = Boolean(activeQuery || tagId || collectionId)

  return (
    <section className="kos-page kos-library">
      <header className="kos-page-header">
        <div>
          <p className="kos-kicker">YOUR LIBRARY</p>
          <h1>Keep the good stuff close.</h1>
        </div>
        <div className="kos-library-actions">
          <label className="kos-button">
            <Upload size={17} />
            {busy ? 'Importing...' : 'Import file'}
            <input
              type="file"
              accept=".pdf,.docx,.txt,.md,.markdown"
              hidden
              disabled={busy}
              onChange={e => {
                const f = e.target.files?.[0]
                e.target.value = ''
                importFile(f)
              }}
            />
          </label>
          <button className="kos-button" onClick={() => setCollectionOpen(true)}>
            <FolderPlus size={17} /> Collection
          </button>
          <button className="kos-button kos-button--primary" onClick={() => setOpen(true)}>
            <Plus size={17} /> New note
          </button>
        </div>
      </header>

      <div className="kos-library-toolbar">
        <label>
          <Search size={18} />
          <input
            aria-label="Search resource titles"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && (setActiveQuery(query), load(query))}
            placeholder="Search titles"
          />
        </label>
        <label className="kos-filter">
          <Tags size={16} />
          <select
            aria-label="Filter by tag"
            value={tagId ?? ''}
            onChange={e => {
              const next = e.target.value ? Number(e.target.value) : undefined
              setTagId(next)
              load(activeQuery, next, collectionId)
            }}
          >
            <option value="">All tags</option>
            {tags.map(tag => (
              <option key={tag.id} value={tag.id}>
                {tag.name}
              </option>
            ))}
          </select>
        </label>
        <label className="kos-filter">
          <FolderPlus size={16} />
          <select
            aria-label="Filter by collection"
            value={collectionId ?? ''}
            onChange={e => {
              const next = e.target.value ? Number(e.target.value) : undefined
              setCollectionId(next)
              load(activeQuery, tagId, next, 0, sort, false)
            }}
          >
            <option value="">All collections</option>
            {collections.map(collection => (
              <option key={collection.id} value={collection.id}>
                {collection.name}
              </option>
            ))}
          </select>
        </label>
        <label className="kos-filter">
          <select
            aria-label="Sort resources"
            value={sort}
            onChange={e => {
              const nextSort = e.target.value
              setSort(nextSort)
              load(activeQuery, tagId, collectionId, 0, nextSort, false)
            }}
          >
            <option value="updated_desc">Recently updated</option>
            <option value="created_desc">Recently created</option>
            <option value="title_asc">Title (A–Z)</option>
            <option value="title_desc">Title (Z–A)</option>
          </select>
        </label>
        <button
          type="button"
          className="kos-button"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            try {
              await autoOrganizeAll()
              await Promise.all([load(activeQuery, tagId, collectionId, 0, sort, false), getCollections().then(setCollections), getTags().then(setTags)])
            } finally {
              setBusy(false)
            }
          }}
          title="Tự động phân loại toàn bộ tài liệu vào các Topic & Collection phù hợp"
        >
          <Sparkles size={15} /> Auto-Organize
        </button>
        {hasFilters && (
          <button type="button" className="kos-button kos-button--ghost" onClick={clearFilters}>
            <X size={15} /> Clear
          </button>
        )}

        {/* View Switcher Button Group */}
        <div className="kos-view-switcher">
          <button
            type="button"
            className={`kos-view-btn ${viewMode === 'GRID' ? 'is-active' : ''}`}
            onClick={() => setViewMode('GRID')}
            title="Xem danh sách dạng lưới"
          >
            <LayoutGrid size={15} /> Lưới
          </button>
          <button
            type="button"
            className={`kos-view-btn ${viewMode === 'GRAPH' ? 'is-active' : ''}`}
            onClick={() => setViewMode('GRAPH')}
            title="Xem sơ đồ mạng lưới tri thức"
          >
            <Network size={15} /> Sơ đồ Tri thức
          </button>
        </div>

        <span className="kos-library-counter">
          {totalItems} resources (page {page + 1}/{totalPages}) · {collections.length} collections · {tags.length} tags
        </span>
      </div>

      {error && (
        <div className="kos-error" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.75rem 1rem', background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', borderRadius: '8px', marginBottom: '1.5rem' }}>
          <span style={{ fontSize: '0.9rem' }}>{error}</span>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <button type="button" className="kos-button kos-button--sm" onClick={() => load()}>
              Thử lại
            </button>
            <button type="button" className="kos-icon-btn" onClick={() => setError('')} aria-label="Đóng thông báo lỗi">
              <X size={15} />
            </button>
          </div>
        </div>
      )}

      {resources.length ? (
        viewMode === 'GRAPH' ? (
          <KnowledgeGraphView
            resources={resources}
            collections={collections}
            tags={tags}
            totalItems={totalItems}
          />
        ) : (
          <>
            <div className="kos-resource-grid">
              {resources.map((resource, index) => (
                <article className="kos-resource" key={resource.id}>
                  <Link to={`/library/${resource.id}`}>
                    <div className={`kos-resource-cover kos-resource-cover--${index % 4}`}>
                      <FileText size={26} />
                    </div>
                    <p>{resource.resourceType}</p>
                    <h2>{resource.title}</h2>
                    <small>
                      {resource.processingStatus === 'READY'
                        ? 'Ready to ask'
                        : resource.processingStatus.toLowerCase()}
                    </small>
                  </Link>
                  {collections.length > 0 && (
                    <label className="kos-collection-select">
                      Add to collection
                      <select
                        defaultValue=""
                        onChange={e => {
                          if (e.target.value) assign(Number(e.target.value), resource.id)
                        }}
                      >
                        <option value="">Choose…</option>
                        {collections.map(collection => (
                          <option key={collection.id} value={collection.id}>
                            {collection.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  )}
                </article>
              ))}
            </div>

            {/* Pagination Controls */}
            {hasNext && (
              <div style={{ display: 'flex', justifyContent: 'center', margin: '2rem 0' }}>
                <button
                  type="button"
                  className="kos-button kos-button--quiet"
                  disabled={busy}
                  onClick={() => load(activeQuery, tagId, collectionId, page + 1, sort, true)}
                >
                  Tải thêm tài liệu ({resources.length} / {totalItems})
                </button>
              </div>
            )}
          </>
        )
      ) : (
        <div className="kos-empty">
          <FileText size={28} />
          <h2>{hasFilters ? 'No resources match those filters.' : 'Nothing here yet.'}</h2>
          <p>
            {hasFilters
              ? 'Try clearing one filter or searching a broader title.'
              : 'Save a note first, then import the resources you want to question later.'}
          </p>
          {hasFilters && (
            <button className="kos-button" onClick={clearFilters}>
              Clear filters
            </button>
          )}
        </div>
      )}

      {open && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <form onSubmit={saveNote}>
            <button className="kos-modal-close" type="button" onClick={() => setOpen(false)}>
              Close
            </button>
            <p className="kos-kicker">NEW NOTE</p>
            <label>
              Title
              <input value={title} onChange={e => setTitle(e.target.value)} required />
            </label>
            <label>
              Thoughts
              <textarea value={content} onChange={e => setContent(e.target.value)} required />
            </label>
            <button className="kos-button kos-button--primary">Save note</button>
          </form>
        </div>
      )}

      {collectionOpen && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <form onSubmit={saveCollection}>
            <button
              className="kos-modal-close"
              type="button"
              onClick={() => setCollectionOpen(false)}
            >
              Close
            </button>
            <p className="kos-kicker">NEW COLLECTION</p>
            <label>
              Name
              <input
                value={collectionName}
                onChange={e => setCollectionName(e.target.value)}
                required
              />
            </label>
            <button className="kos-button kos-button--primary">Create collection</button>
          </form>
        </div>
      )}
    </section>
  )
}
