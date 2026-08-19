import {
  Activity,
  BrainCircuit,
  Check,
  ChevronDown,
  ChevronUp,
  Compass,
  Copy,
  Loader2,
  Maximize2,
  MessageSquare,
  MessageSquarePlus,
  Minimize2,
  Send,
  Sparkles,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  askKnowledge,
  getChatSession,
  getChatSessions,
  getCollections,
  getResources,
  type ChatDetail,
  type ChatSession,
  type KnowledgeCollection,
  type RagExecutionTrace,
  type Resource,
} from '../api/knowledge'
import MarkdownView from '../components/MarkdownView'

type Scope = 'THIS_RESOURCE' | 'SELECTED_RESOURCES' | 'COLLECTION' | 'LIBRARY'

const SMART_PROMPT_PILLS = [
  {
    id: 'roadmap',
    icon: '🗺️',
    label: 'Roadmap kiến thức',
    prompt: 'Đưa cho tôi các đầu mục kiến thức chính của tài liệu này, đánh số thứ tự từ 1 đến hết như một lộ trình học tập (Roadmap).',
  },
  {
    id: 'compare',
    icon: '⚖️',
    label: 'So sánh & Đối chiếu',
    prompt: 'Phân tích sự khác biệt, ưu điểm và nhược điểm giữa các phương pháp hoặc khái niệm chính trong tài liệu.',
  },
  {
    id: 'summary',
    icon: '🧠',
    label: 'Tóm tắt 3 ý cốt lõi',
    prompt: 'Tóm tắt 3 luận điểm quan trọng nhất trong tài liệu này kèm các từ khóa chính và ý nghĩa thực tiễn.',
  },
  {
    id: 'rules',
    icon: '🎯',
    label: 'Quy tắc & Công thức',
    prompt: 'Trích xuất toàn bộ các định nghĩa, quy tắc suy diễn và công thức quan trọng được đề cập trong tài liệu.',
  },
  {
    id: 'quiz',
    icon: '❓',
    label: '5 Câu hỏi ôn tập',
    prompt: 'Dựa trên tài liệu, tạo 5 câu hỏi ôn tập quan trọng từ cơ bản đến nâng cao để kiểm tra mức độ hiểu bài.',
  },
]

export default function KnowledgeAskPage() {
  const [params] = useSearchParams()
  const initial = Number(params.get('resource'))
  const [resources, setResources] = useState<Resource[]>([])
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [sessions, setSessions] = useState<ChatSession[]>([])
  const [scope, setScope] = useState<Scope>(initial ? 'THIS_RESOURCE' : 'LIBRARY')
  const [selected, setSelected] = useState<number[]>(initial ? [initial] : [])
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [active, setActive] = useState<ChatDetail | null>(null)
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [elapsedSec, setElapsedSec] = useState<string>('0.0')
  const [copiedId, setCopiedId] = useState<number | null>(null)
  const [highlightedCitation, setHighlightedCitation] = useState<number | null>(null)
  const [zenMode, setZenMode] = useState(false)
  const [latestTrace, setLatestTrace] = useState<RagExecutionTrace | null>(null)
  const [showTraceJson, setShowTraceJson] = useState(false)
  const timerRef = useRef<number | null>(null)

  const load = () =>
    Promise.all([getResources(), getCollections(), getChatSessions()])
      .then(([r, c, s]) => {
        setResources(r)
        setCollections(c)
        setSessions(s)
      })
      .catch(() => setError('KnowledgeOS could not load this workspace.'))

  useEffect(() => {
    load()
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  const toggle = (id: number) =>
    setSelected(values =>
      values.includes(id) ? values.filter(value => value !== id) : [...values, id],
    )

  async function open(id: number) {
    try {
      const detail = await getChatSession(id)
      setActive(detail)
      setScope(detail.scope as Scope)
      setSelected(detail.resourceIds)
      setCollectionId(detail.collectionId || undefined)
      setError('')
    } catch {
      setError('This conversation could not be restored.')
    }
  }

  function startRequestTimer() {
    const startTime = Date.now()
    setElapsedSec('0.0')
    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = window.setInterval(() => {
      const sec = ((Date.now() - startTime) / 1000).toFixed(1)
      setElapsedSec(sec)
    }, 100)
  }

  function stopRequestTimer() {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (!question.trim()) return
    if (
      (scope === 'THIS_RESOURCE' && selected.length !== 1) ||
      (scope === 'SELECTED_RESOURCES' && !selected.length) ||
      (scope === 'COLLECTION' && !collectionId)
    ) {
      setError('Choose the sources KnowledgeOS should use.')
      return
    }

    setLoading(true)
    startRequestTimer()

    try {
      const response = await askKnowledge({
        sessionId: active?.id,
        question,
        scope,
        resourceId: scope === 'THIS_RESOURCE' ? selected[0] : undefined,
        resourceIds: scope === 'SELECTED_RESOURCES' ? selected : undefined,
        collectionId: scope === 'COLLECTION' ? collectionId : undefined,
      })
      if (response.trace) {
        setLatestTrace(response.trace)
      }
      setQuestion('')
      stopRequestTimer()
      await open(response.sessionId)
      await load()
    } catch {
      setError('KnowledgeOS could not answer right now.')
      stopRequestTimer()
    } finally {
      setLoading(false)
    }
  }

  function newChat() {
    setActive(null)
    setQuestion('')
    setError('')
    setElapsedSec('0.0')
    setLatestTrace(null)
  }

  function handleSelectPromptPill(pillPrompt: string) {
    setQuestion(pillPrompt)
    const textarea = document.getElementById('ask-question') as HTMLTextAreaElement | null
    if (textarea) {
      textarea.focus()
      textarea.selectionStart = textarea.value.length
      textarea.selectionEnd = textarea.value.length
    }
  }

  function copyMessage(id: number, content: string) {
    navigator.clipboard.writeText(content)
    setCopiedId(id)
    setTimeout(() => setCopiedId(null), 2000)
  }

  function handleCitationClick(citationNumber: number) {
    setHighlightedCitation(citationNumber)
    const el = document.getElementById(`citation-detail-${citationNumber}`)
    if (el) {
      ;(el as HTMLDetailsElement).open = true
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
    setTimeout(() => setHighlightedCitation(null), 2500)
  }

  return (
    <section className={`kos-page kos-ask ${zenMode ? 'kos-zen-mode' : ''}`}>
      <div className="kos-page-header">
        <div>
          <p className="kos-kicker">ASK KNOWLEDGEOS</p>
          <h1>Ask what your sources can answer.</h1>
        </div>
        <button
          type="button"
          className="kos-button kos-button--ghost kos-zen-toggle"
          onClick={() => setZenMode(prev => !prev)}
          title={zenMode ? 'Thoát chế độ tập trung' : 'Chuyển sang chế độ đọc toàn màn hình'}
        >
          {zenMode ? (
            <>
              <Minimize2 size={15} /> Giao diện chuẩn
            </>
          ) : (
            <>
              <Maximize2 size={15} /> Chế độ Zen Focus
            </>
          )}
        </button>
      </div>

      <div className="kos-ask-layout">
        {/* Left Column: 3 Dedicated Cards */}
        {!zenMode && (
          <aside className="kos-ask-sidebar">
            {/* BOX 1: New Conversation Action Card */}
            <div className="kos-ask-box kos-new-chat-box">
              <button className="kos-button kos-button--primary kos-button--block" onClick={newChat}>
                <MessageSquarePlus size={16} /> New conversation
              </button>
            </div>

            {/* BOX 2: Recent Conversations Card */}
            <div className="kos-ask-box kos-recent-box">
              <div className="kos-box-header">
                <div className="kos-box-title">
                  <MessageSquare size={16} className="kos-box-icon" />
                  <span>Recent conversations</span>
                </div>
                {sessions.length > 0 && (
                  <span className="kos-box-badge">{sessions.length}</span>
                )}
              </div>
              <div className="kos-recent-scroll-content">
                {sessions.length ? (
                  sessions.map(session => (
                    <button
                      key={session.id}
                      className={`kos-recent-item ${active?.id === session.id ? 'is-active' : ''}`}
                      onClick={() => open(session.id)}
                      title={session.title || 'Untitled conversation'}
                    >
                      <span className="kos-recent-bullet">•</span>
                      <span className="kos-recent-text">
                        {session.title || 'Untitled conversation'}
                      </span>
                    </button>
                  ))
                ) : (
                  <p className="kos-box-empty">Chưa có cuộc hội thoại nào.</p>
                )}
              </div>
            </div>

            {/* BOX 3: Truthful RAG Execution Trace Card */}
            <div className="kos-ask-box kos-reasoning-box">
              <div className="kos-box-header">
                <div className="kos-box-title">
                  <Activity size={16} className="kos-box-icon kos-pulse-icon" />
                  <span>RAG Execution Trace</span>
                </div>
                <span className={`kos-status-pill ${loading ? 'is-running' : 'is-idle'}`}>
                  {loading ? (
                    <>
                      <Loader2 size={11} className="kos-spin-fast" /> {elapsedSec}s
                    </>
                  ) : latestTrace ? (
                    `${latestTrace.durationMs}ms`
                  ) : (
                    'RAG v2 Ready'
                  )}
                </span>
              </div>

              <div className="kos-reasoning-scroll-content">
                {latestTrace ? (
                  <div className="kos-reasoning-timeline">
                    {/* Stage 1: Query Planner */}
                    <div className="kos-reasoning-step is-done">
                      <div className="kos-step-indicator">
                        <span className="kos-step-dot">1</span>
                        <span className="kos-step-line" />
                      </div>
                      <div className="kos-step-content">
                        <div className="kos-step-header">
                          <span className="kos-step-title">Query Planner</span>
                          <span className="kos-step-badge">{latestTrace.mode}</span>
                        </div>
                        <p className="kos-step-desc">
                          Operation: {latestTrace.operation}
                          {latestTrace.planner?.semanticQuery ? ` • Query: "${latestTrace.planner.semanticQuery}"` : ''}
                        </p>
                      </div>
                    </div>

                    {/* STRUCTURED PATH */}
                    {latestTrace.mode === 'STRUCTURED' ? (
                      <>
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">2</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Metadata Scope</span>
                              <span className="kos-step-badge">{latestTrace.filter?.scope || 'LIBRARY'}</span>
                            </div>
                            <p className="kos-step-desc">
                              {latestTrace.filter?.resourceType ? `Type: ${latestTrace.filter.resourceType} • ` : ''}
                              {latestTrace.filter?.favorite !== null && latestTrace.filter?.favorite !== undefined ? `Favorite: ${latestTrace.filter.favorite} • ` : ''}
                              Relational metadata filters evaluated.
                            </p>
                          </div>
                        </div>
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">3</span>
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">PostgreSQL Facts Query</span>
                              <span className="kos-step-badge">Direct SQL</span>
                            </div>
                            <p className="kos-step-desc">
                              Count/List computed directly from PostgreSQL tables without vector retrieval or hallucinations.
                            </p>
                          </div>
                        </div>
                      </>
                    ) : (
                      /* HYBRID / FILTERED_HYBRID PATH */
                      <>
                        {/* Stage 2: Metadata Filter (if present or scope) */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">2</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Metadata Filters</span>
                              <span className="kos-step-badge">{latestTrace.filter?.scope || 'LIBRARY'}</span>
                            </div>
                            <p className="kos-step-desc">
                              {latestTrace.filter?.resourceType ? `Type: ${latestTrace.filter.resourceType} • ` : ''}
                              {latestTrace.filter?.collectionCount ? `${latestTrace.filter.collectionCount} collection(s) • ` : ''}
                              Applied in SQL WHERE before vector distance calculation.
                            </p>
                          </div>
                        </div>

                        {/* Stage 3: Retrieval Branches */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">3</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Hybrid Retrieval</span>
                              <span className="kos-step-badge">pgvector + FTS</span>
                            </div>
                            <p className="kos-step-desc">
                              Semantic candidates: {latestTrace.retrieval?.semanticCandidates || 0} • Lexical candidates: {latestTrace.retrieval?.lexicalCandidates || 0}
                            </p>
                          </div>
                        </div>

                        {/* Stage 4: RRF Fusion */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">4</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">RRF Fusion (k={latestTrace.fusion?.rrfK || 60})</span>
                              <span className="kos-step-badge">{latestTrace.fusion?.selectedChildren || 0} child chunks</span>
                            </div>
                            <p className="kos-step-desc">
                              Fused {latestTrace.fusion?.inputCandidates || 0} total candidates into top {latestTrace.fusion?.selectedChildren || 0} precision child chunks.
                            </p>
                          </div>
                        </div>

                        {/* Stage 5: Parent Expansion */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">5</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Parent Expansion & Dedup</span>
                              <span className="kos-step-badge">{latestTrace.parentChild?.uniqueParentsFound || 0} parents</span>
                            </div>
                            <p className="kos-step-desc">
                              Expanded {latestTrace.parentChild?.childChunksRetrieved || 0} children → {latestTrace.parentChild?.uniqueParentsFound || 0} unique parents ({latestTrace.parentChild?.duplicateParentsDeduplicated || 0} duplicate parents deduplicated).
                            </p>
                          </div>
                        </div>

                        {/* Stage 6: Context Budget */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">6</span>
                            <span className="kos-step-line" />
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Context Budget</span>
                              <span className="kos-step-badge">{latestTrace.contextBudget?.charactersUsed || 0}/{latestTrace.contextBudget?.maxCharactersBudget || 6000} chars</span>
                            </div>
                            <p className="kos-step-desc">
                              {latestTrace.contextBudget?.parentsUsed || 0} parent contexts budgeted into grounded prompt.
                            </p>
                          </div>
                        </div>

                        {/* Stage 7: Grounded Generation */}
                        <div className="kos-reasoning-step is-done">
                          <div className="kos-step-indicator">
                            <span className="kos-step-dot">7</span>
                          </div>
                          <div className="kos-step-content">
                            <div className="kos-step-header">
                              <span className="kos-step-title">Generation & Citations</span>
                              <span className="kos-step-badge">{latestTrace.generation?.model || 'gemini-3.5-flash-lite'}</span>
                            </div>
                            <p className="kos-step-desc">
                              Synthesized with {latestTrace.generation?.verifiedCitationsCount || 0} verified source citations.
                            </p>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                ) : (
                  <div className="kos-reasoning-timeline">
                    <div className="kos-reasoning-step is-done">
                      <div className="kos-step-indicator">
                        <span className="kos-step-dot">1</span>
                        <span className="kos-step-line" />
                      </div>
                      <div className="kos-step-content">
                        <div className="kos-step-header">
                          <span className="kos-step-title">KnowledgeQueryPlanner</span>
                          <span className="kos-step-badge">Intent Aware</span>
                        </div>
                        <p className="kos-step-desc">Phân loại Structured / Semantic / Filtered Hybrid trước khi truy xuất.</p>
                      </div>
                    </div>
                    <div className="kos-reasoning-step is-done">
                      <div className="kos-step-indicator">
                        <span className="kos-step-dot">2</span>
                        <span className="kos-step-line" />
                      </div>
                      <div className="kos-step-content">
                        <div className="kos-step-header">
                          <span className="kos-step-title">Filtered Hybrid Retrieval</span>
                          <span className="kos-step-badge">pgvector + FTS</span>
                        </div>
                        <p className="kos-step-desc">Lọc metadata SQL trước khi cosine vector search và PostgreSQL FTS.</p>
                      </div>
                    </div>
                    <div className="kos-reasoning-step is-done">
                      <div className="kos-step-indicator">
                        <span className="kos-step-dot">3</span>
                      </div>
                      <div className="kos-step-content">
                        <div className="kos-step-header">
                          <span className="kos-step-title">Parent-Child Context Expansion</span>
                          <span className="kos-step-badge">Hierarchical</span>
                        </div>
                        <p className="kos-step-desc">Tìm kiếm trên child chunks chính xác cao, mở rộng lên parent chunk và tối ưu ngân sách.</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {latestTrace && (
                <div style={{ padding: '0.5rem 0.75rem', borderTop: '1px solid var(--kos-border, #333)' }}>
                  <button
                    type="button"
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--kos-accent, #60a5fa)',
                      cursor: 'pointer',
                      fontSize: '0.75rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                    }}
                    onClick={() => setShowTraceJson(v => !v)}
                  >
                    {showTraceJson ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                    {showTraceJson ? 'Ẩn chi tiết kỹ thuật' : 'Xem JSON Trace kỹ thuật'}
                  </button>
                  {showTraceJson && (
                    <pre
                      style={{
                        fontSize: '0.65rem',
                        marginTop: '0.5rem',
                        maxHeight: '120px',
                        overflowY: 'auto',
                        background: 'rgba(0,0,0,0.3)',
                        padding: '0.5rem',
                        borderRadius: '4px',
                      }}
                    >
                      {JSON.stringify(latestTrace, null, 2)}
                    </pre>
                  )}
                </div>
              )}

              <div className="kos-box-footer">
                <BrainCircuit size={13} />
                <span>
                  Scope: {scope === 'LIBRARY' ? 'Entire library' : scope === 'COLLECTION' ? 'Collection' : 'Selected resources'}
                  {active?.messages?.length ? ` • ${active.messages[active.messages.length - 1]?.citations?.length || 0} citations` : ''}
                </span>
              </div>
            </div>
          </aside>
        )}

        {/* Right Column: Interaction & Answers Stage */}
        <div className="kos-ask-stage">
          <form className="kos-ask-form" onSubmit={submit}>
            <div className="kos-ask-scope">
              <label>
                <Sparkles size={16} /> Scope
                <select
                  name="scope"
                  value={scope}
                  onChange={event => setScope(event.target.value as Scope)}
                >
                  <option value="LIBRARY">Entire library</option>
                  <option value="COLLECTION">One collection</option>
                  <option value="THIS_RESOURCE">One resource</option>
                  <option value="SELECTED_RESOURCES">Selected resources</option>
                </select>
              </label>
            </div>

            {scope === 'COLLECTION' && (
              <label>
                Collection
                <select
                  name="collectionId"
                  value={collectionId || ''}
                  onChange={event => setCollectionId(Number(event.target.value))}
                  required
                >
                  <option value="">Select a collection</option>
                  {collections.map(col => (
                    <option key={col.id} value={col.id}>
                      {col.name}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {(scope === 'THIS_RESOURCE' || scope === 'SELECTED_RESOURCES') && (
              <div className="kos-resource-picker">
                {resources.map(resource => (
                  <label key={resource.id}>
                    <input
                      name="resourceIds"
                      type={scope === 'THIS_RESOURCE' ? 'radio' : 'checkbox'}
                      checked={selected.includes(resource.id)}
                      onChange={() =>
                        scope === 'THIS_RESOURCE'
                          ? setSelected([resource.id])
                          : toggle(resource.id)
                      }
                    />
                    {resource.title}
                  </label>
                ))}
              </div>
            )}

            <label>
              Your question
              <textarea
                id="ask-question"
                name="question"
                value={question}
                onChange={event => setQuestion(event.target.value)}
                placeholder="Đặt câu hỏi hoặc chọn một mẫu gợi ý bên dưới để tra cứu nhanh..."
                required
              />
            </label>

            {/* Smart Prompt Pills Row */}
            <div className="kos-prompt-pills-bar">
              <span className="kos-pills-title">
                <Compass size={13} /> Gợi ý câu hỏi nhanh:
              </span>
              <div className="kos-pills-container">
                {SMART_PROMPT_PILLS.map(pill => (
                  <button
                    key={pill.id}
                    type="button"
                    className="kos-prompt-pill"
                    onClick={() => handleSelectPromptPill(pill.prompt)}
                    title={pill.prompt}
                  >
                    <span>{pill.icon}</span>
                    <span>{pill.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {error && <p className="kos-error">{error}</p>}

            <button className="kos-button kos-button--primary" disabled={loading}>
              {loading ? (
                <>
                  <Loader2 size={16} className="kos-btn-spin" /> Reasoning & Finding Evidence...
                </>
              ) : (
                <>
                  Ask sources <Send size={16} />
                </>
              )}
            </button>
          </form>

          <div className="kos-answer">
            {active?.messages.length ? (
              active.messages.map(message => (
                <article key={message.id} className={`kos-message-article kos-message--${message.role.toLowerCase()}`}>
                  <div className="kos-message-header">
                    <p className="kos-kicker">
                      {message.role === 'USER' ? 'YOU' : 'KNOWLEDGEOS'}
                    </p>
                    {message.role === 'ASSISTANT' && (
                      <button
                        type="button"
                        className="kos-copy-btn"
                        onClick={() => copyMessage(message.id, message.content)}
                        title="Sao chép nội dung câu trả lời"
                      >
                        {copiedId === message.id ? (
                          <>
                            <Check size={13} className="kos-text-success" />
                            <span>Đã chép</span>
                          </>
                        ) : (
                          <>
                            <Copy size={13} />
                            <span>Sao chép</span>
                          </>
                        )}
                      </button>
                    )}
                  </div>

                  <div className="kos-message-body">
                    <MarkdownView content={message.content} onCitationClick={handleCitationClick} />
                  </div>

                  {message.citations.length > 0 && (
                    <div className="kos-citations-section">
                      <p className="kos-citations-heading">
                        <Sparkles size={13} /> Bằng chứng trích dẫn đối chứng:
                      </p>
                      {message.citations.map(citation => (
                        <details
                          id={`citation-detail-${citation.citationOrder}`}
                          key={citation.chunkId}
                          className={`kos-citation-details ${highlightedCitation === citation.citationOrder ? 'kos-citation-glow' : ''}`}
                        >
                          <summary>
                            <span className="kos-cit-num">[{citation.citationOrder}]</span>
                            <span className="kos-cit-title">{citation.resourceTitle}</span>
                          </summary>
                          <p className="kos-cit-excerpt">{citation.evidenceExcerpt}</p>
                        </details>
                      ))}
                    </div>
                  )}
                </article>
              ))
            ) : (
              <div className="kos-answer-empty">
                <p>Bắt đầu cuộc trò chuyện grounded và tra cứu kiến thức từ thư viện của bạn.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
