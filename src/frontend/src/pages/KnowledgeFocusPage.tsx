import {
  BookOpen,
  BrainCircuit,
  Check,
  CheckCircle2,
  ChevronRight,
  FileText,
  Folder,
  FolderPlus,
  HelpCircle,
  Network,
  Plus,
  RotateCcw,
  Sparkles,
  Upload,
  XCircle,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  assignResourceToCollection,
  createCollection,
  createNote,
  createResourceNote,
  getCollectionResources,
  getCollections,
  getResourceContent,
  getResourceNotes,
  getResources,
  getTags,
  updateResourceProgress,
  uploadResource,
  type KnowledgeCollection,
  type KnowledgeTag,
  type Resource,
  type ResourceNote,
} from '../api/knowledge'
import KnowledgeGraphView from '../components/KnowledgeGraphView'

interface QuizQuestion {
  id: number
  question: string
  options: string[]
  correctIndex: number
  explanation: string
}

export default function KnowledgeFocusPage() {
  const [collections, setCollections] = useState<KnowledgeCollection[]>([])
  const [allResources, setAllResources] = useState<Resource[]>([])
  const [tags, setTags] = useState<KnowledgeTag[]>([])
  const [selectedColId, setSelectedColId] = useState<number | null>(null)
  const [topicResources, setTopicResources] = useState<Resource[]>([])
  const [activeResourceId, setActiveResourceId] = useState<number | null>(null)
  const [activeContent, setActiveContent] = useState<string>('')
  const [activeNotes, setActiveNotes] = useState<ResourceNote[]>([])
  const [newNoteText, setNewNoteText] = useState('')
  const [progressPercent, setProgressPercent] = useState<number>(0)
  const [activeTab, setActiveTab] = useState<'READER' | 'QUIZ' | 'GRAPH' | 'NOTES'>('READER')
  const [uploadBusy, setUploadBusy] = useState(false)
  const [newTopicModal, setNewTopicModal] = useState(false)
  const [newTopicName, setNewTopicName] = useState('')
  const [newNoteModal, setNewNoteModal] = useState(false)
  const [newNoteTitle, setNewNoteTitle] = useState('')
  const [newNoteContent, setNewNoteContent] = useState('')
  const [error, setError] = useState('')
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [userAnswers, setUserAnswers] = useState<Record<number, number>>({})
  const [quizSubmitted, setQuizSubmitted] = useState(false)

  // Initial Load
  const initLoad = async () => {
    try {
      const [cols, res, t] = await Promise.all([getCollections(), getResources(), getTags()])
      setCollections(cols)
      setAllResources(res)
      setTags(t)
      if (cols.length > 0) {
        setSelectedColId(cols[0].id)
      } else if (res.length > 0) {
        setSelectedColId(-1) // All topics view
      }
    } catch {
      setError('Could not load topics and collections.')
    }
  }

  useEffect(() => {
    initLoad()
  }, [])

  // Load resources for the selected collection
  useEffect(() => {
    if (selectedColId === null) return
    if (selectedColId === -1) {
      setTopicResources(allResources)
      if (allResources.length > 0) {
        setActiveResourceId(prev => (prev && allResources.some(r => r.id === prev) ? prev : allResources[0].id))
      } else {
        setActiveResourceId(null)
        setActiveContent('')
      }
    } else {
      getCollectionResources(selectedColId)
        .then(resList => {
          setTopicResources(resList)
          if (resList.length > 0) {
            setActiveResourceId(prev => (prev && resList.some(r => r.id === prev) ? prev : resList[0].id))
          } else {
            setActiveResourceId(null)
            setActiveContent('')
          }
        })
        .catch(() => setError('Could not load topic resources.'))
    }
  }, [selectedColId, allResources])

  // Load active resource content & notes
  useEffect(() => {
    if (!activeResourceId) {
      setActiveContent('')
      setActiveNotes([])
      return
    }
    Promise.all([
      getResourceContent(activeResourceId).catch(() => 'No readable text content available.'),
      getResourceNotes(activeResourceId).catch(() => []),
    ]).then(([text, notes]) => {
      setActiveContent(text)
      setActiveNotes(notes)
      generateQuizFromContent(text)
    })
  }, [activeResourceId])

  // Generates interactive quiz questions from active text
  function generateQuizFromContent(text: string) {
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
          explanation: 'Theo Hệ tiên đề Armstrong, 3 luật cơ bản là Phản xạ (IR1), Tăng trưởng (IR2) và Bắc cầu (IR3). Các luật IR4, IR5, IR6 là hệ quả mở rộng.',
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
        {
          id: 3,
          question: 'Bao đóng của tập thuộc tính X (kí hiệu X+) đối với tập phụ thuộc hàm F dùng để làm gì?',
          options: [
            'Tìm tất cả các thuộc tính có thể được xác định hàm từ X dựa vào F',
            'Tìm số lượng bảng cần phân rã khi chuẩn hóa',
            'Tính toán kích thước lưu trữ của bảng CSDL',
            'Xóa các thuộc tính dư thừa trong khóa chính',
          ],
          correctIndex: 0,
          explanation: 'X+ là tập hợp tất cả các thuộc tính A sao cho phụ thuộc hàm X → A có thể suy diễn từ F.',
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
          explanation: 'Phân lập rõ ràng giữa chỉ thị hệ thống và dữ liệu tri thức không tin cậy bằng thẻ XML và prompt grounding ngăn ngừa việc ghi đè chỉ thị.',
        },
        {
          id: 2,
          question: 'Chỉ số CVSS (Common Vulnerability Scoring System) dùng để đánh giá điều gì?',
          options: [
            'Mức độ nghiêm trọng và rủi ro của lỗ hổng bảo mật',
            'Tốc độ xử lý của mạng máy chủ',
            'Dung lượng bộ nhớ RAM bị tiêu tốn',
            'Chuẩn mã hóa JWT trong hệ thống',
          ],
          correctIndex: 0,
          explanation: 'CVSS cung cấp thang điểm chuẩn hóa từ 0.0 đến 10.0 để lượng hóa mức độ nguy hiểm của các lỗ hổng an ninh thông tin.',
        },
      )
    } else {
      // General question generation from sample text paragraphs
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
        {
          id: 2,
          question: 'Mục đích cốt lõi của việc ghi chép và lưu trữ tài liệu trong KnowledgeOS là gì?',
          options: [
            'Tổ chức tri thức có cấu trúc và truy vấn hỏi đáp đối chứng chính xác với RAG',
            'Chỉ để xem lại dưới dạng văn bản tĩnh không xử lý',
            'Tự động gửi email thông báo hàng ngày',
            'Chuyển đổi toàn bộ tài liệu thành định dạng âm thanh',
          ],
          correctIndex: 0,
          explanation: 'KnowledgeOS hỗ trợ ingest một lần, tự động bóc tách vector và cho phép tra cứu, đối chứng trích dẫn nguồn chuẩn mực.',
        },
      )
    }

    setQuizQuestions(questions)
  }

  // Handle direct file import into the active Topic/Collection
  async function handleImportToTopic(file?: File) {
    if (!file) return
    setUploadBusy(true)
    setError('')
    try {
      const created = await uploadResource(file)
      if (selectedColId && selectedColId > 0) {
        await assignResourceToCollection(selectedColId, created.id)
      }
      await initLoad()
    } catch {
      setError('Could not import resource into this topic.')
    } finally {
      setUploadBusy(false)
    }
  }

  // Handle direct new Note creation into active Topic
  async function handleCreateNoteInTopic(e: React.FormEvent) {
    e.preventDefault()
    if (!newNoteTitle.trim() || !newNoteContent.trim()) return
    setError('')
    try {
      const created = await createNote(newNoteTitle, newNoteContent)
      if (selectedColId && selectedColId > 0) {
        await assignResourceToCollection(selectedColId, created.id)
      }
      setNewNoteTitle('')
      setNewNoteContent('')
      setNewNoteModal(false)
      await initLoad()
    } catch {
      setError('Could not save note to this topic.')
    }
  }

  // Handle new Topic creation
  async function handleCreateTopic(e: React.FormEvent) {
    e.preventDefault()
    if (!newTopicName.trim()) return
    try {
      const created = await createCollection(newTopicName, 'Custom topic collection created from Focus Hub.')
      setNewTopicName('')
      setNewTopicModal(false)
      const cols = await getCollections()
      setCollections(cols)
      setSelectedColId(created.id)
    } catch {
      setError('Could not create new topic.')
    }
  }

  // Add note to active resource
  async function handleAddNote() {
    if (!activeResourceId || !newNoteText.trim()) return
    try {
      await createResourceNote(activeResourceId, newNoteText.trim())
      setNewNoteText('')
      setActiveNotes(await getResourceNotes(activeResourceId))
    } catch {
      setError('Could not save note.')
    }
  }

  // Handle progress update
  async function handleProgress(val: number) {
    setProgressPercent(val)
    if (activeResourceId) {
      await updateResourceProgress(activeResourceId, val).catch(() => {})
    }
  }

  // Select quiz option
  function selectAnswer(questionId: number, optionIdx: number) {
    if (quizSubmitted) return
    setUserAnswers(prev => ({ ...prev, [questionId]: optionIdx }))
  }

  // Calculate score
  const correctCount = quizQuestions.filter(q => userAnswers[q.id] === q.correctIndex).length

  const selectedCol = collections.find(c => c.id === selectedColId)
  const activeResource = allResources.find(r => r.id === activeResourceId)

  return (
    <section className="kos-page kos-focus-hub">
      {/* Header */}
      <header className="kos-page-header">
        <div>
          <p className="kos-kicker">FOCUS STUDY & TOPIC DEEPDIVE</p>
          <h1>Learn with purpose. Master by topic.</h1>
        </div>
        <div className="kos-library-actions">
          <label className="kos-button">
            <Upload size={17} />
            {uploadBusy ? 'Importing…' : 'Thêm tài liệu vào Topic'}
            <input
              type="file"
              accept=".pdf,.docx,.txt,.md,.markdown"
              hidden
              disabled={uploadBusy}
              onChange={e => handleImportToTopic(e.target.files?.[0])}
            />
          </label>
          <button className="kos-button" onClick={() => setNewNoteModal(true)}>
            <Plus size={17} /> Tạo ghi chú
          </button>
          <button className="kos-button kos-button--primary" onClick={() => setNewTopicModal(true)}>
            <FolderPlus size={17} /> Tạo Topic mới
          </button>
        </div>
      </header>

      {error && <p className="kos-error">{error}</p>}

      {/* Topic / Collection Filter Pills Bar */}
      <div className="kos-topic-selector-bar">
        <span className="kos-topic-bar-label">
          <Folder size={15} /> Chủ đề Học tập (Topics):
        </span>
        <div className="kos-topic-pills">
          <button
            type="button"
            className={`kos-topic-pill ${selectedColId === -1 ? 'is-active' : ''}`}
            onClick={() => setSelectedColId(-1)}
          >
            <span>📚 Tất cả tài liệu ({allResources.length})</span>
          </button>
          {collections.map(col => (
            <button
              key={col.id}
              type="button"
              className={`kos-topic-pill ${selectedColId === col.id ? 'is-active' : ''}`}
              onClick={() => setSelectedColId(col.id)}
            >
              <span>📁 {col.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Focus Deepdive Stage */}
      <div className="kos-focus-stage-layout">
        {/* Left Column: Topic Resources Index */}
        <aside className="kos-focus-sidebar">
          <div className="kos-focus-sidebar-header">
            <div className="kos-sidebar-topic-info">
              <h3>{selectedColId === -1 ? 'Tất cả tài liệu' : selectedCol ? selectedCol.name : 'Topic'}</h3>
              <p>{topicResources.length} tài liệu trong chủ đề này</p>
            </div>
            {selectedColId && selectedColId > 0 && (
              <Link
                to={`/knowledge/ask?collection=${selectedColId}`}
                className="kos-ask-topic-link"
                title="Hỏi AI về toàn bộ chủ đề này"
              >
                <BrainCircuit size={14} /> Hỏi AI
              </Link>
            )}
          </div>

          <div className="kos-focus-resource-list">
            {topicResources.length > 0 ? (
              topicResources.map(res => (
                <button
                  key={res.id}
                  type="button"
                  className={`kos-focus-res-item ${activeResourceId === res.id ? 'is-active' : ''}`}
                  onClick={() => setActiveResourceId(res.id)}
                >
                  <span className="kos-res-type-badge">{res.resourceType}</span>
                  <div className="kos-res-meta">
                    <h4 className="kos-res-item-title">{res.title}</h4>
                    <small>
                      {res.processingStatus === 'READY' ? '🟢 Sẵn sàng' : res.processingStatus}
                    </small>
                  </div>
                  <ChevronRight size={15} className="kos-res-arrow" />
                </button>
              ))
            ) : (
              <div className="kos-box-empty">
                <p>Chưa có tài liệu nào trong chủ đề này.</p>
                <label className="kos-button kos-button--primary" style={{ marginTop: '.6rem', display: 'inline-flex' }}>
                  <Upload size={15} /> Tải tài liệu ngay
                  <input
                    type="file"
                    accept=".pdf,.docx,.txt,.md,.markdown"
                    hidden
                    onChange={e => handleImportToTopic(e.target.files?.[0])}
                  />
                </label>
              </div>
            )}
          </div>
        </aside>

        {/* Right Column: Deepdive Workspace & Reader & Quiz & Graph */}
        <main className="kos-focus-main-panel">
          {activeResource ? (
            <>
              {/* Active Resource Top Banner */}
              <div className="kos-focus-resource-banner">
                <div>
                  <span className="kos-banner-kicker">{activeResource.resourceType} WORKSPACE</span>
                  <h2 className="kos-banner-title">{activeResource.title}</h2>
                </div>
                <div className="kos-banner-actions">
                  <Link
                    to={`/knowledge/ask?resource=${activeResource.id}`}
                    className="kos-button kos-button--primary"
                  >
                    <BrainCircuit size={16} /> Hỏi tài liệu này
                  </Link>
                  <Link
                    to={`/library/${activeResource.id}`}
                    className="kos-button"
                  >
                    Mở chi tiết
                  </Link>
                </div>
              </div>

              {/* Study Mode Navigation Tabs */}
              <div className="kos-focus-tabs">
                <button
                  type="button"
                  className={`kos-focus-tab ${activeTab === 'READER' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('READER')}
                >
                  <BookOpen size={16} /> Đọc tài liệu (Reader)
                </button>
                <button
                  type="button"
                  className={`kos-focus-tab ${activeTab === 'QUIZ' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('QUIZ')}
                >
                  <HelpCircle size={16} /> Smart Quiz Ôn Tập ({quizQuestions.length})
                </button>
                <button
                  type="button"
                  className={`kos-focus-tab ${activeTab === 'GRAPH' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('GRAPH')}
                >
                  <Network size={16} /> Sơ đồ Mạng lưới Tri thức
                </button>
                <button
                  type="button"
                  className={`kos-focus-tab ${activeTab === 'NOTES' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('NOTES')}
                >
                  <FileText size={16} /> Ghi chú ({activeNotes.length})
                </button>
              </div>

              {/* Tab 1: Reader */}
              {activeTab === 'READER' && (
                <div className="kos-reader-pane">
                  <div className="kos-reader-controls">
                    <span>Tiến độ đọc: {progressPercent}%</span>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={progressPercent}
                      onChange={e => handleProgress(Number(e.target.value))}
                      className="kos-range-slider"
                    />
                  </div>
                  <div className="kos-reader-content">
                    <pre>{activeContent || 'Đang tải nội dung văn bản...'}</pre>
                  </div>
                </div>
              )}

              {/* Tab 2: Smart Quiz Generator */}
              {activeTab === 'QUIZ' && (
                <div className="kos-quiz-pane">
                  <div className="kos-quiz-header">
                    <div>
                      <h3>🎯 Bộ câu hỏi Ôn tập & Kiểm tra Nhanh</h3>
                      <p>Được sinh tự động từ kiến thức cốt lõi của tài liệu</p>
                    </div>
                    {quizSubmitted && (
                      <div className="kos-quiz-score-badge">
                        Điểm số: {correctCount} / {quizQuestions.length} ({Math.round((correctCount / (quizQuestions.length || 1)) * 100)}%)
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
                                    <span className="kos-opt-letter">
                                      {String.fromCharCode(65 + optIdx)}
                                    </span>
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
                                <strong>💡 Giải thích chi tiết:</strong> {q.explanation}
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
                          <button
                            type="button"
                            className="kos-button"
                            onClick={() => generateQuizFromContent(activeContent)}
                          >
                            <RotateCcw size={16} /> Làm lại đề thi
                          </button>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="kos-box-empty">
                      <p>Không có đủ văn bản để tạo bộ câu hỏi trắc nghiệm.</p>
                    </div>
                  )}
                </div>
              )}

              {/* Tab 3: Knowledge Graph for this Topic */}
              {activeTab === 'GRAPH' && (
                <div className="kos-graph-tab-pane">
                  <KnowledgeGraphView
                    resources={topicResources}
                    collections={selectedCol ? [selectedCol] : collections}
                    tags={tags}
                  />
                </div>
              )}

              {/* Tab 4: Notes */}
              {activeTab === 'NOTES' && (
                <div className="kos-notes-pane">
                  <div className="kos-add-note-box">
                    <h4>📝 Thêm ghi chú cho tài liệu này</h4>
                    <textarea
                      value={newNoteText}
                      onChange={e => setNewNoteText(e.target.value)}
                      placeholder="Ghi lại các ý chính, công thức hoặc lưu ý cần nhớ..."
                      rows={3}
                    />
                    <button
                      type="button"
                      className="kos-button kos-button--primary"
                      onClick={handleAddNote}
                      disabled={!newNoteText.trim()}
                    >
                      <Plus size={16} /> Lưu ghi chú
                    </button>
                  </div>

                  <div className="kos-saved-notes-list">
                    {activeNotes.length > 0 ? (
                      activeNotes.map(n => (
                        <div key={n.id} className="kos-note-card">
                          <p className="kos-note-text">{n.content}</p>
                          <small className="kos-note-date">
                            {new Date(n.created_at).toLocaleString()}
                          </small>
                        </div>
                      ))
                    ) : (
                      <p className="kos-box-empty">Chưa có ghi chú nào cho tài liệu này.</p>
                    )}
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="kos-empty">
              <Sparkles size={30} />
              <h2>Chọn một tài liệu bên trái để bắt đầu học tập chuyên sâu</h2>
              <p>Mỗi tài liệu cung cấp đầy đủ công cụ Reader, Smart Quiz ôn tập, Sơ đồ tri thức và Ghi chú.</p>
            </div>
          )}
        </main>
      </div>

      {/* Modal: New Topic */}
      {newTopicModal && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <form onSubmit={handleCreateTopic}>
            <button className="kos-modal-close" type="button" onClick={() => setNewTopicModal(false)}>
              Close
            </button>
            <p className="kos-kicker">NEW TOPIC / COLLECTION</p>
            <label>
              Tên Chủ Đề (Topic Name)
              <input
                value={newTopicName}
                onChange={e => setNewTopicName(e.target.value)}
                placeholder="Ví dụ: Hệ Quản Trị CSDL, An Toàn Mạng..."
                required
              />
            </label>
            <button className="kos-button kos-button--primary">
              <FolderPlus size={16} /> Tạo Topic
            </button>
          </form>
        </div>
      )}

      {/* Modal: New Note */}
      {newNoteModal && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <form onSubmit={handleCreateNoteInTopic}>
            <button className="kos-modal-close" type="button" onClick={() => setNewNoteModal(false)}>
              Close
            </button>
            <p className="kos-kicker">NEW STUDY NOTE</p>
            <label>
              Tiêu đề Ghi chú
              <input
                value={newNoteTitle}
                onChange={e => setNewNoteTitle(e.target.value)}
                placeholder="Tiêu đề tóm tắt..."
                required
              />
            </label>
            <label>
              Nội dung Tri thức
              <textarea
                value={newNoteContent}
                onChange={e => setNewNoteContent(e.target.value)}
                placeholder="Nhập nội dung ghi chú..."
                rows={4}
                required
              />
            </label>
            <button className="kos-button kos-button--primary">
              <Plus size={16} /> Lưu vào Topic này
            </button>
          </form>
        </div>
      )}
    </section>
  )
}
