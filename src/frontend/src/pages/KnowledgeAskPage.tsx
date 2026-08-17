import {
  Activity,
  BrainCircuit,
  CheckCircle2,
  Loader2,
  MessageSquare,
  MessageSquarePlus,
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
  type Resource,
} from '../api/knowledge'
import MarkdownView from '../components/MarkdownView'

type Scope = 'THIS_RESOURCE' | 'SELECTED_RESOURCES' | 'COLLECTION' | 'LIBRARY'

interface ReasoningStep {
  id: string
  title: string
  detail: string
  badge: string
  status: 'pending' | 'running' | 'completed'
}

const DEFAULT_STEPS: ReasoningStep[] = [
  {
    id: 'parse',
    title: 'Phân tích truy vấn & Chuẩn hóa',
    detail: 'Bóc tách token kỹ thuật, lọc stopwords và nhận diện ngôn ngữ câu hỏi.',
    badge: 'NLP & Tokenizer',
    status: 'pending',
  },
  {
    id: 'semantic',
    title: 'Truy xuất Ngữ nghĩa pgvector',
    detail: 'Tạo embedding 768 chiều và tìm kiếm Cosine trên chỉ mục HNSW.',
    badge: 'pgvector / HNSW',
    status: 'pending',
  },
  {
    id: 'lexical',
    title: 'Truy xuất Từ khóa FTS',
    detail: 'Khớp chính xác mã số, thuật ngữ kỹ thuật qua tsvector và chỉ mục GIN.',
    badge: 'PostgreSQL GIN',
    status: 'pending',
  },
  {
    id: 'rrf',
    title: 'Hợp nhất Xếp hạng RRF (k=60)',
    detail: 'Tổng hợp điểm số 2 nhánh bằng Reciprocal Rank Fusion, lọc ứng viên tối ưu.',
    badge: 'RRF Fusion',
    status: 'pending',
  },
  {
    id: 'grounding',
    title: 'Đóng gói Ngữ cảnh Grounding',
    detail: 'Bọc chứng cứ vào thẻ XML <evidence> thụ động, kích hoạt chỉ thị chống ảo giác.',
    badge: 'Anti-Hallucination',
    status: 'pending',
  },
  {
    id: 'synthesis',
    title: 'Sinh Phản hồi & Trích dẫn',
    detail: 'Mô hình Gemini tổng hợp câu trả lời và gắn nhãn trích dẫn đối chứng [1], [2].',
    badge: 'Gemini 3.5 Flash',
    status: 'pending',
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
  const [steps, setSteps] = useState<ReasoningStep[]>(DEFAULT_STEPS)
  const [elapsedSec, setElapsedSec] = useState<string>('0.0')
  const timerRef = useRef<number | null>(null)
  const animIntervalRef = useRef<number | null>(null)

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
      if (animIntervalRef.current) clearInterval(animIntervalRef.current)
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

      setSteps(
        DEFAULT_STEPS.map(s => ({
          ...s,
          status: 'completed',
        })),
      )
    } catch {
      setError('This conversation could not be restored.')
    }
  }

  function startReasoningAnimation() {
    const startTime = Date.now()
    setElapsedSec('0.0')

    setSteps(
      DEFAULT_STEPS.map((s, idx) => ({
        ...s,
        status: idx === 0 ? 'running' : 'pending',
      })),
    )

    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = window.setInterval(() => {
      const sec = ((Date.now() - startTime) / 1000).toFixed(1)
      setElapsedSec(sec)
    }, 100)

    let currentStepIndex = 0
    if (animIntervalRef.current) clearInterval(animIntervalRef.current)
    animIntervalRef.current = window.setInterval(() => {
      currentStepIndex++
      if (currentStepIndex < DEFAULT_STEPS.length) {
        setSteps(prev =>
          prev.map((s, idx) => ({
            ...s,
            status:
              idx < currentStepIndex
                ? 'completed'
                : idx === currentStepIndex
                  ? 'running'
                  : 'pending',
          })),
        )
      } else {
        if (animIntervalRef.current) clearInterval(animIntervalRef.current)
      }
    }, 350)
  }

  function completeReasoningAnimation() {
    if (animIntervalRef.current) clearInterval(animIntervalRef.current)
    if (timerRef.current) clearInterval(timerRef.current)

    setSteps(prev =>
      prev.map(s => ({
        ...s,
        status: 'completed',
      })),
    )
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
    startReasoningAnimation()

    try {
      const response = await askKnowledge({
        sessionId: active?.id,
        question,
        scope,
        resourceId: scope === 'THIS_RESOURCE' ? selected[0] : undefined,
        resourceIds: scope === 'SELECTED_RESOURCES' ? selected : undefined,
        collectionId: scope === 'COLLECTION' ? collectionId : undefined,
      })
      setQuestion('')
      completeReasoningAnimation()
      await open(response.sessionId)
      await load()
    } catch {
      setError('KnowledgeOS could not answer right now.')
      if (animIntervalRef.current) clearInterval(animIntervalRef.current)
      if (timerRef.current) clearInterval(timerRef.current)
    } finally {
      setLoading(false)
    }
  }

  function newChat() {
    setActive(null)
    setQuestion('')
    setError('')
    setSteps(DEFAULT_STEPS)
    setElapsedSec('0.0')
  }

  const completedCount = steps.filter(s => s.status === 'completed').length

  return (
    <section className="kos-page kos-ask">
      <p className="kos-kicker">ASK KNOWLEDGEOS</p>
      <h1>Ask what your sources can answer.</h1>

      <div className="kos-ask-layout">
        {/* Left Column: 3 Distinct Dedicated Boxes */}
        <aside className="kos-ask-sidebar">
          {/* BOX 1: New Conversation Action Card */}
          <div className="kos-ask-box kos-new-chat-box">
            <button className="kos-button kos-button--primary kos-button--block" onClick={newChat}>
              <MessageSquarePlus size={16} /> New conversation
            </button>
          </div>

          {/* BOX 2: Recent Conversations Card with Independent Scroll */}
          <div className="kos-ask-box kos-recent-box">
            <div className="kos-box-header">
              <div className="kos-box-header-title">
                <MessageSquare size={15} className="kos-box-icon" />
                <h2>Recent conversations</h2>
              </div>
              <span className="kos-box-count">{sessions.length}</span>
            </div>
            <div className="kos-recent-scroll-content">
              {sessions.length ? (
                sessions.map(session => (
                  <button
                    key={session.id}
                    className={`kos-session-item ${active?.id === session.id ? 'is-active' : ''}`}
                    onClick={() => open(session.id)}
                    title={session.title}
                  >
                    <span className="kos-chat-dot" />
                    <span className="kos-chat-title">{session.title}</span>
                  </button>
                ))
              ) : (
                <p className="kos-empty-hint">No saved conversations yet.</p>
              )}
            </div>
          </div>

          {/* BOX 3: RAG Reasoning & Pipeline Brief Card with Independent Scroll */}
          <div className="kos-ask-box kos-reasoning-box">
            <div className="kos-box-header">
              <div className="kos-box-header-title">
                <Activity size={15} className={`kos-activity-icon ${loading ? 'is-pulsing' : ''}`} />
                <h3>RAG Reasoning & Pipeline</h3>
              </div>
              <span
                className={`kos-status-pill kos-status-pill--${
                  loading ? 'running' : completedCount === steps.length ? 'success' : 'idle'
                }`}
              >
                {loading ? `${elapsedSec}s` : completedCount === steps.length ? 'Done' : 'Ready'}
              </span>
            </div>

            <div className="kos-reasoning-scroll-content">
              {steps.map((step, idx) => (
                <div key={step.id} className={`kos-reasoning-step is-${step.status}`}>
                  <div className="kos-step-indicator">
                    {step.status === 'completed' ? (
                      <CheckCircle2 size={14} className="kos-step-icon kos-step-icon--success" />
                    ) : step.status === 'running' ? (
                      <Loader2 size={14} className="kos-step-icon kos-step-icon--spin" />
                    ) : (
                      <span className="kos-step-num">{idx + 1}</span>
                    )}
                    {idx < steps.length - 1 && <span className="kos-step-line" />}
                  </div>
                  <div className="kos-step-body">
                    <div className="kos-step-heading">
                      <span className="kos-step-title">{step.title}</span>
                      <span className="kos-step-badge">{step.badge}</span>
                    </div>
                    <p className="kos-step-detail">{step.detail}</p>
                  </div>
                </div>
              ))}
            </div>

            {active && (
              <div className="kos-box-footer">
                <Sparkles size={12} className="kos-sparkle-icon" />
                <span>
                  Scope: <strong>{scope}</strong> •{' '}
                  {active.messages.reduce(
                    (acc, m) => acc + (m.citations?.length || 0),
                    0,
                  )}{' '}
                  trích dẫn
                </span>
              </div>
            )}
          </div>
        </aside>

        {/* Right Main Panel: Question Stage & Answer Area */}
        <div className="kos-ask-main-panel">
          <form className="kos-ask-stage" onSubmit={submit}>
            <BrainCircuit size={30} />
            <label>
              Scope
              <select
                id="ask-scope"
                name="scope"
                value={scope}
                onChange={event => {
                  setScope(event.target.value as Scope)
                  setSelected([])
                  setCollectionId(undefined)
                  setActive(null)
                }}
              >
                <option value="LIBRARY">Entire library</option>
                <option value="THIS_RESOURCE">One resource</option>
                <option value="SELECTED_RESOURCES">Selected resources</option>
                <option value="COLLECTION">Collection</option>
              </select>
            </label>

            {scope === 'COLLECTION' && (
              <label>
                Collection
                <select
                  id="ask-collection"
                  name="collectionId"
                  value={collectionId ?? ''}
                  onChange={event =>
                    setCollectionId(Number(event.target.value) || undefined)
                  }
                >
                  <option value="">Choose collection</option>
                  {collections.map(collection => (
                    <option key={collection.id} value={collection.id}>
                      {collection.name}
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
                placeholder="What would you like to understand?"
                required
              />
            </label>

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
                <article key={message.id}>
                  <p className="kos-kicker">
                    {message.role === 'USER' ? 'YOU' : 'KNOWLEDGEOS'}
                  </p>
                  <div className="kos-message-body">
                    <MarkdownView content={message.content} />
                  </div>
                  {message.citations.map(citation => (
                    <details key={citation.chunkId}>
                      <summary>
                        [{citation.citationOrder}] {citation.resourceTitle}
                      </summary>
                      <p>{citation.evidenceExcerpt}</p>
                    </details>
                  ))}
                </article>
              ))
            ) : (
              <p>Start a grounded conversation with your library.</p>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
