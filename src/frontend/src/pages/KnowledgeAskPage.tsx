import {
  Check,
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
import RagExecutionTracePanel from '../components/RagExecutionTracePanel'

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
    prompt: 'So sánh và phân tích sự khác biệt giữa các giải pháp hoặc trường phái được nhắc đến trong tài liệu.',
  },
  {
    id: 'summary',
    icon: '🧠',
    label: 'Tóm tắt 3 ý cốt lục',
    prompt: 'Tóm tắt 3 luận điểm quan trọng nhất trong tài liệu này kèm các từ khóa chính và ý nghĩa thực tiễn.',
  },
  {
    id: 'critique',
    icon: '🔍',
    label: 'Phản biện chuyên sâu',
    prompt: 'Chỉ ra các điểm mạnh, giới hạn kỹ thuật và rủi ro tiềm ẩn của phương pháp được trình bày.',
  },
  {
    id: 'exam',
    icon: '🎯',
    label: 'Câu hỏi ôn tập',
    prompt: 'Tạo 3 câu hỏi đánh giá kèm đáp án ngắn để kiểm tra mức độ hiểu sâu kiến thức từ nguồn này.',
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
  const timerRef = useRef<number | null>(null)

  const load = () => {
    setError('')
    Promise.allSettled([getResources(), getCollections(), getChatSessions()]).then(
      ([resResult, colResult, sessResult]) => {
        if (resResult.status === 'fulfilled') {
          setResources(resResult.value.items)
        }
        if (colResult.status === 'fulfilled') {
          setCollections(colResult.value)
        }
        if (sessResult.status === 'fulfilled') {
          setSessions(sessResult.value)
        }

        if (
          resResult.status === 'rejected' &&
          colResult.status === 'rejected' &&
          sessResult.status === 'rejected'
        ) {
          setError('KnowledgeOS could not load this workspace. Please check your connection and retry.')
        }
      },
    )
  }

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
            <RagExecutionTracePanel
              trace={latestTrace}
              elapsedSec={elapsedSec}
              loading={loading}
              scope={scope}
              citationCount={active?.messages?.length ? active.messages[active.messages.length - 1]?.citations?.length || 0 : 0}
            />
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
