import {
  ArrowLeft,
  BookOpen,
  BrainCircuit,
  Check,
  CheckCircle2,
  FileText,
  HelpCircle,
  Network,
  RotateCcw,
  Sparkles,
  Trash2,
  XCircle,
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
  updateResourceNote,
  updateResourceProgress,
  type KnowledgeCollection,
  type KnowledgeTag,
  type Resource,
  type ResourceActivity,
  type ResourceNote,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

type Tab = 'Overview' | 'Reader' | 'Notes' | 'Related' | 'Activity'

interface QuizQuestion {
  id: number
  question: string
  options: string[]
  correctIndex: number
  explanation: string
}

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

  // Quiz state in Activity tab
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [userAnswers, setUserAnswers] = useState<Record<number, number>>({})
  const [quizSubmitted, setQuizSubmitted] = useState(false)

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
        generateQuiz(text)
      })
      .catch(() => setError('This resource could not be loaded.'))

  useEffect(() => {
    load()
  }, [resourceId])

  function generateQuiz(text: string) {
    setUserAnswers({})
    setQuizSubmitted(false)

    if (!text || text.length < 50) {
      setQuizQuestions([])
      return
    }

    const lines = text.split('\n').filter(l => l.trim().length > 20)
    const questions: QuizQuestion[] = []

    if (text.toLowerCase().includes('armstrong') || text.toLowerCase().includes('phụ thuộc hàm')) {
      questions.push(
        {
          id: 1,
          question: 'Hệ tiên đề Armstrong gồm 3 luật cơ bản nào đóng vai trò đúng đắn và đầy đủ?',
          options: [
            'Luật phản xạ (Reflexivity), Luật tăng trưởng (Augmentation), Luật bắc cầu (Transitivity)',
            'Luật chiếu (Decomposition), Luật cộng (Union), Luật giả bắc cầu (Pseudo-transitivity)',
            'Luật giao hoán, Luật kết hợp, Luật phân phối',
            'Luật bảo toàn thông tin, Luật bảo toàn phụ thuộc hàm',
          ],
          correctIndex: 0,
          explanation: '3 luật cơ bản của hệ tiên đề Armstrong là Phản xạ, Tăng trưởng và Bắc cầu.',
        },
        {
          id: 2,
          question: 'Một phụ thuộc hàm X → Y được gọi là hiển nhiên (Trivial) khi nào?',
          options: [
            'Khi Y ⊆ X (Vế phải là tập con của vế trái)',
            'Khi X ⊆ Y (Vế trái là tập con của vế phải)',
            'Khi X ∩ Y = ∅ (X và Y rời nhau)',
            'Khi X là siêu khóa của lược đồ quan hệ',
          ],
          correctIndex: 0,
          explanation: 'Phụ thuộc hàm X → Y là hiển nhiên khi và chỉ khi Y là tập con của X (Y ⊆ X).',
        },
      )
    } else if (text.toLowerCase().includes('cve') || text.toLowerCase().includes('security') || text.toLowerCase().includes('injection')) {
      questions.push(
        {
          id: 1,
          question: 'Đâu là giải pháp cốt lõi để phòng thủ tấn công Prompt Injection trong hệ thống RAG?',
          options: [
            'Đóng gói chứng cứ vào thẻ XML <evidence> và chỉ thị AI xử lý chứng cứ dưới dạng dữ liệu thụ động',
            'Tăng nhiệt độ (temperature) của mô hình ngôn ngữ lớn',
            'Chỉ sử dụng mô hình AI có số lượng tham số lớn hơn 70B',
            'Xóa bỏ toàn bộ trích dẫn nguồn đối chứng',
          ],
          correctIndex: 0,
          explanation: 'Phân lập rõ ràng giữa chỉ thị hệ thống và dữ liệu tri thức không tin cậy bằng thẻ XML và prompt grounding.',
        },
      )
    } else {
      const sampleTopic = lines[0] ? lines[0].slice(0, 60) : 'nội dung tài liệu'
      questions.push(
        {
          id: 1,
          question: `Chủ đề trọng tâm được đề cập trong phần đầu của tài liệu là gì?`,
          options: [
            sampleTopic,
            'Cấu trúc mạng máy tính diện rộng',
            'Phương pháp quản trị kinh doanh hiện đại',
            'Lý thuyết đồ thị rời rạc nâng cao',
          ],
          correctIndex: 0,
          explanation: `Nội dung phần mở đầu tài liệu tập trung trực tiếp vào: "${sampleTopic}".`,
        },
      )
    }

    setQuizQuestions(questions)
  }

  async function handleAutoOrganize() {
    setOrganizing(true)
    setAutoOrganizeMsg('')
    try {
      await autoOrganizeResource(resourceId)
      setAutoOrganizeMsg('Đã tự động phân loại vào Collection & Tags phù hợp!')
      setTimeout(() => setAutoOrganizeMsg(''), 4000)
      await load()
    } catch {
      setError('Could not auto-organize resource.')
    } finally {
      setOrganizing(false)
    }
  }

  async function addNote() {
    if (!draft.trim() || noteBusy) return
    setNoteBusy(true)
    try {
      await createResourceNote(resourceId, draft)
      setDraft('')
      const [nextNotes, nextActivity] = await Promise.all([
        getResourceNotes(resourceId),
        getResourceActivity(resourceId),
      ])
      setNotes(nextNotes)
      setActivity(nextActivity)
    } catch {
      setError('The note could not be saved.')
    } finally {
      setNoteBusy(false)
    }
  }

  async function removeNote(id: number) {
    try {
      await deleteResourceNote(resourceId, id)
      setNotes(await getResourceNotes(resourceId))
    } catch {
      setError('The note could not be deleted.')
    }
  }

  async function saveNote(id: number, value: string) {
    try {
      await updateResourceNote(resourceId, id, value)
      setNotes(await getResourceNotes(resourceId))
    } catch {
      setError('The note could not be updated.')
    }
  }

  async function progress(value: number) {
    try {
      setActivity(await updateResourceProgress(resourceId, value))
    } catch {
      setError('Progress could not be saved.')
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

  function selectAnswer(questionId: number, optionIdx: number) {
    if (quizSubmitted) return
    setUserAnswers(prev => ({ ...prev, [questionId]: optionIdx }))
  }

  const correctCount = quizQuestions.filter(q => userAnswers[q.id] === q.correctIndex).length

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

  return (
    <section className="kos-page kos-workspace">
      <Link className="kos-back" to="/library">
        <ArrowLeft size={16} aria-hidden="true" /> Library
      </Link>
      <header>
        <div>
          <p className="kos-kicker">{resource.resourceType}</p>
          <h1>{resource.title}</h1>
          <p>{resource.description || 'A resource in your KnowledgeOS library.'}</p>
          {autoOrganizeMsg && (
            <p className="kos-success" style={{ marginTop: '.4rem' }}>
              <Check size={14} /> {autoOrganizeMsg}
            </p>
          )}
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
            <button className="kos-text-button" onClick={() => setDeleteConfirm(false)}>
              Cancel
            </button>
          )}
          <button
            className="kos-button"
            onClick={handleAutoOrganize}
            disabled={organizing}
            title="Tự động bóc tách từ khóa và gán vào Collection & Tags"
          >
            <Sparkles size={17} aria-hidden="true" /> {organizing ? 'Organizing…' : 'Auto-Organize'}
          </button>
          <Link className="kos-button kos-button--primary" to={`/knowledge/ask?resource=${resource.id}`}>
            <BrainCircuit size={17} aria-hidden="true" /> Ask
          </Link>
        </div>
      </header>

      <nav className="kos-tabs" role="tablist">
        {(['Overview', 'Reader', 'Notes', 'Related', 'Activity'] as Tab[]).map(item => (
          <button
            key={item}
            type="button"
            role="tab"
            aria-selected={tab === item}
            className={`kos-tab${tab === item ? ' is-active' : ''}`}
            onClick={() => setTab(item)}
          >
            {item === 'Activity' ? '⚡ Activity & Quiz' : item}
          </button>
        ))}
      </nav>

      {tab === 'Overview' && (
        <div className="kos-workspace-overview">
          <dl>
            <dt>Status</dt>
            <dd>{resource.processingStatus}</dd>
            <dt>Original file</dt>
            <dd>{resource.originalFilename || 'Direct note'}</dd>
            <dt>Size</dt>
            <dd>{resource.sizeBytes ? `${Math.round(resource.sizeBytes / 1024)} KB` : 'Embedded text'}</dd>
            <dt>Created</dt>
            <dd>{new Date(resource.createdAt).toLocaleDateString()}</dd>
          </dl>
          <div>
            <h2>Start studying</h2>
            <p>Read the extracted text, record private notes, or test your knowledge with Smart Quiz.</p>
            <div style={{ display: 'flex', gap: '.5rem', flexWrap: 'wrap', marginTop: '.75rem' }}>
              <button className="kos-button kos-button--primary" onClick={() => setTab('Reader')}>
                <BookOpen size={16} /> Open reader
              </button>
              <button className="kos-button" onClick={() => setTab('Activity')}>
                <HelpCircle size={16} /> Open Quiz & Activity
              </button>
            </div>
          </div>
        </div>
      )}

      {tab === 'Reader' && (
        <div className="kos-reader">
          <pre>{content || 'No readable text content is available for this resource.'}</pre>
        </div>
      )}

      {tab === 'Notes' && (
        <div className="kos-notes">
          <div className="kos-notes-form">
            <textarea
              aria-label="Add a private note"
              placeholder="Record a thought, insight, or follow-up question for this resource…"
              value={draft}
              onChange={event => setDraft(event.target.value)}
              rows={3}
            />
            <button className="kos-button kos-button--primary" disabled={noteBusy || !draft.trim()} onClick={addNote}>
              Save note
            </button>
          </div>
          <div className="kos-notes-list">
            {notes.length ? (
              notes.map(item => (
                <article key={item.id} className="kos-note-card">
                  <textarea
                    defaultValue={item.content}
                    onBlur={event => saveNote(item.id, event.target.value)}
                    rows={2}
                  />
                  <footer>
                    <small>{new Date(item.created_at).toLocaleString()}</small>
                    <button className="kos-text-button" onClick={() => removeNote(item.id)}>
                      Delete
                    </button>
                  </footer>
                </article>
              ))
            ) : (
              <p className="kos-empty">No notes recorded for this resource yet.</p>
            )}
          </div>
        </div>
      )}

      {tab === 'Related' && (
        <div className="kos-related">
          {related.length ? (
            <div className="kos-resource-grid">
              {related.map(item => (
                <article key={item.id} className="kos-resource-card">
                  <p className="kos-kicker">{item.resourceType}</p>
                  <h3>
                    <Link to={`/library/${item.id}`}>{item.title}</Link>
                  </h3>
                  <p>{item.description || 'Related reading from your library.'}</p>
                </article>
              ))}
            </div>
          ) : (
            <div className="kos-empty">
              <h2>No related resources found yet.</h2>
              <p>KnowledgeOS automatically discovers related material using hybrid vector search.</p>
            </div>
          )}
        </div>
      )}

      {tab === 'Activity' && (
        <div className="kos-activity" style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          {/* Progress & Status Card */}
          <div style={{ background: 'var(--kos-surface-1)', padding: '1.25rem', borderRadius: '8px', border: '1px solid var(--kos-line)' }}>
            <h2>Learning progress</h2>
            <p>
              {activity?.progress_percent ?? 0}% complete · {activity?.note_count ?? 0} notes
            </p>
            <input
              aria-label="Reading progress"
              type="range"
              min="0"
              max="100"
              value={activity?.progress_percent ?? 0}
              onChange={event => progress(Number(event.target.value))}
              className="kos-range-slider"
              style={{ maxWidth: '100%', width: '100%', margin: '.6rem 0' }}
            />
            <small style={{ color: 'var(--kos-muted)' }}>
              Status: {activity?.processing_status} · Last updated:{' '}
              {activity?.updated_at ? new Date(activity.updated_at).toLocaleString() : 'Not yet recorded'}
            </small>
          </div>

          {/* Smart Quiz Generator Card */}
          <div className="kos-quiz-pane" style={{ background: 'var(--kos-surface)', padding: '1.5rem', borderRadius: '8px', border: '1px solid var(--kos-line)' }}>
            <div className="kos-quiz-header">
              <div>
                <h3>🎯 Smart Quiz Ôn Tập Kiến Thức</h3>
                <p>Tự động sinh từ nội dung tài liệu này</p>
              </div>
              {quizSubmitted && (
                <div className="kos-quiz-score-badge">
                  Điểm: {correctCount} / {quizQuestions.length} ({Math.round((correctCount / (quizQuestions.length || 1)) * 100)}%)
                </div>
              )}
            </div>

            {quizQuestions.length > 0 ? (
              <div className="kos-quiz-question-list">
                {quizQuestions.map((q, qIdx) => {
                  const isCorrect = userAnswers[q.id] === q.correctIndex

                  return (
                    <div key={q.id} className="kos-quiz-card">
                      <h4 className="kos-quiz-q-title">
                        <span className="kos-q-number">Câu {qIdx + 1}:</span> {q.question}
                      </h4>

                      <div className="kos-quiz-options">
                        {q.options.map((opt, optIdx) => {
                          const isSelected = userAnswers[q.id] === optIdx
                          let optClass = 'kos-quiz-opt'
                          if (isSelected) optClass += ' is-selected'
                          if (quizSubmitted) {
                            if (optIdx === q.correctIndex) optClass += ' is-correct'
                            else if (isSelected && !isCorrect) optClass += ' is-wrong'
                          }

                          return (
                            <button
                              key={optIdx}
                              type="button"
                              className={optClass}
                              onClick={() => selectAnswer(q.id, optIdx)}
                              disabled={quizSubmitted}
                            >
                              <span className="kos-opt-letter">{String.fromCharCode(65 + optIdx)}</span>
                              <span className="kos-opt-text">{opt}</span>
                              {quizSubmitted && optIdx === q.correctIndex && (
                                <CheckCircle2 size={16} className="kos-text-success" />
                              )}
                              {quizSubmitted && isSelected && !isCorrect && (
                                <XCircle size={16} className="kos-text-danger" />
                              )}
                            </button>
                          )
                        })}
                      </div>

                      {quizSubmitted && (
                        <div className={`kos-quiz-explanation ${isCorrect ? 'is-correct-box' : 'is-wrong-box'}`}>
                          <strong>💡 Giải thích:</strong> {q.explanation}
                        </div>
                      )}
                    </div>
                  )
                })}

                <div className="kos-quiz-actions">
                  {!quizSubmitted ? (
                    <button
                      type="button"
                      className="kos-button kos-button--primary"
                      onClick={() => setQuizSubmitted(true)}
                      disabled={Object.keys(userAnswers).length === 0}
                    >
                      <Check size={16} /> Nộp bài & Xem đáp án
                    </button>
                  ) : (
                    <button type="button" className="kos-button" onClick={() => generateQuiz(content)}>
                      <RotateCcw size={16} /> Làm lại đề thi
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <p className="kos-box-empty">Không có đủ văn bản để tạo câu hỏi trắc nghiệm.</p>
            )}
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
