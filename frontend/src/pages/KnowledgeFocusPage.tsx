import {
  AlertCircle,
  ArrowRight,
  BookOpen,
  BrainCircuit,
  Check,
  CheckCircle2,
  ChevronRight,
  Compass,
  Folder,
  FolderPlus,
  HelpCircle,
  Layers,
  Network,
  Plus,
  RefreshCw,
  RotateCcw,
  Sparkles,
  Trash2,
  Upload,
  X,
  XCircle,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  addTopicSource,
  createResourceNote,
  createStudyTopic,
  deleteStudyTopic,
  generateTopicPlan,
  generateTopicQuiz,
  getResources,
  getReviewQueue,
  getStudyTopicDetail,
  getStudyTopics,
  removeTopicSource,
  submitQuizAnswers,
  updateConceptStatus,
  uploadResource,
  type QuizAttemptResponse,
  type Resource,
  type ReviewQueueItem,
  type StudyTopic,
  type StudyTopicDetail,
  type SubmitQuizAnswersResponse,
} from '../api/knowledge'
import { getApiErrorMessage } from '../api/errors'

export default function KnowledgeFocusPage() {
  const [topics, setTopics] = useState<StudyTopic[]>([])
  const [selectedTopicId, setSelectedTopicId] = useState<number | null>(null)
  const [topicDetail, setTopicDetail] = useState<StudyTopicDetail | null>(null)
  const [allResources, setAllResources] = useState<Resource[]>([])
  const [reviewQueue, setReviewQueue] = useState<ReviewQueueItem[]>([])

  const [activeTab, setActiveTab] = useState<'PATH' | 'QUIZ' | 'MAP' | 'QUEUE' | 'SOURCES'>('PATH')
  const [selectedConceptId, setSelectedConceptId] = useState<number | null>(null)

  // Quiz state
  const [currentQuiz, setCurrentQuiz] = useState<QuizAttemptResponse | null>(null)
  const [quizAnswers, setQuizAnswers] = useState<Record<number, number>>({})
  const [quizResult, setQuizResult] = useState<SubmitQuizAnswersResponse | null>(null)
  const [quizLoading, setQuizLoading] = useState(false)

  // Takeaway Note state
  const [takeawayText, setTakeawayText] = useState('')
  const [takeawaySaved, setTakeawaySaved] = useState(false)

  // Modals & form state
  const [newTopicModal, setNewTopicModal] = useState(false)
  const [newTopicTitle, setNewTopicTitle] = useState('')
  const [newTopicGoal, setNewTopicGoal] = useState('')
  const [selectedResourceIds, setSelectedResourceIds] = useState<number[]>([])
  const [topicToDelete, setTopicToDelete] = useState<{ id: number; title: string } | null>(null)

  const [addSourceModal, setAddSourceModal] = useState(false)
  const [uploadBusy, setUploadBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [planBusy, setPlanBusy] = useState(false)
  const [error, setError] = useState('')

  const loadTopicDetail = useCallback(async (topicId: number) => {
    try {
      const detail = await getStudyTopicDetail(topicId)
      setTopicDetail(detail)
      if (detail.concepts.length > 0) {
        setSelectedConceptId(prev => (prev && detail.concepts.some(c => c.id === prev) ? prev : detail.concepts[0].id))
      } else {
        setSelectedConceptId(null)
      }
    } catch {
      setError('Không thể tải chi tiết chủ đề.')
    }
  }, [])

  // Initial Load
  const initLoad = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [tList, resList, qList] = await Promise.all([
        getStudyTopics(),
        getResources(),
        getReviewQueue().catch(() => []),
      ])
      setTopics(tList)
      setAllResources(resList.items)
      setReviewQueue(qList)

      if (tList.length > 0) {
        const targetId = selectedTopicId && tList.some(t => t.id === selectedTopicId)
          ? selectedTopicId
          : tList[0].id
        setSelectedTopicId(targetId)
        await loadTopicDetail(targetId)
      } else {
        setSelectedTopicId(null)
        setTopicDetail(null)
      }
    } catch {
      setError('Không thể tải dữ liệu Chủ đề học tập.')
    } finally {
      setLoading(false)
    }
  }, [loadTopicDetail, selectedTopicId])

  useEffect(() => {
    initLoad()
  }, [initLoad])

  const handleSelectTopic = async (topicId: number) => {
    setSelectedTopicId(topicId)
    setCurrentQuiz(null)
    setQuizResult(null)
    setTakeawaySaved(false)
    await loadTopicDetail(topicId)
  }

  // Create New Deep Dive Topic
  const handleCreateTopic = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newTopicTitle.trim()) return
    setError('')
    try {
      const created = await createStudyTopic(newTopicTitle.trim(), newTopicGoal.trim(), selectedResourceIds)
      setNewTopicTitle('')
      setNewTopicGoal('')
      setSelectedResourceIds([])
      setNewTopicModal(false)
      const tList = await getStudyTopics()
      setTopics(tList)
      setSelectedTopicId(created.id)
      setTopicDetail(created)
      if (created.concepts.length > 0) {
        setSelectedConceptId(created.concepts[0].id)
      }
    } catch {
      setError('Không thể tạo chủ đề học tập mới.')
    }
  }

  // Generate / Rebuild Learning Plan
  const handleGeneratePlan = async () => {
    if (!selectedTopicId) return
    setPlanBusy(true)
    setError('')
    try {
      const updated = await generateTopicPlan(selectedTopicId)
      setTopicDetail(updated)
      if (updated.concepts.length > 0) {
        setSelectedConceptId(updated.concepts[0].id)
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Chưa thể tạo lộ trình học từ các tài liệu này.')
    } finally {
      setPlanBusy(false)
    }
  }

  // Concept status toggle
  const handleUpdateStatus = async (conceptId: number, status: 'NOT_STARTED' | 'LEARNING' | 'REVIEW_NEEDED' | 'CHECKED') => {
    if (!selectedTopicId) return
    try {
      const updatedConcept = await updateConceptStatus(selectedTopicId, conceptId, status)
      setTopicDetail(prev => {
        if (!prev) return prev
        const newConcepts = prev.concepts.map(c => (c.id === conceptId ? updatedConcept : c))
        const checked = newConcepts.filter(c => c.studyStatus === 'CHECKED').length
        const review = newConcepts.filter(c => c.studyStatus === 'REVIEW_NEEDED').length
        const learning = newConcepts.filter(c => c.studyStatus === 'LEARNING').length
        const notStarted = newConcepts.filter(c => c.studyStatus === 'NOT_STARTED').length
        return {
          ...prev,
          concepts: newConcepts,
          checkedCount: checked,
          reviewNeededCount: review,
          learningCount: learning,
          notStartedCount: notStarted,
        }
      })
      const qList = await getReviewQueue().catch(() => [])
      setReviewQueue(qList)
    } catch {
      setError('Không thể cập nhật trạng thái khái niệm.')
    }
  }

  // Recall Check Quiz
  const handleStartQuiz = async (conceptId?: number) => {
    if (!selectedTopicId) return
    setQuizLoading(true)
    setError('')
    setQuizAnswers({})
    setQuizResult(null)
    setActiveTab('QUIZ')
    try {
      const quiz = await generateTopicQuiz(selectedTopicId, conceptId)
      setCurrentQuiz(quiz)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Chưa thể tạo bài kiểm tra ghi nhớ từ các nguồn này.')
    } finally {
      setQuizLoading(false)
    }
  }

  const handleSubmitQuiz = async () => {
    if (!currentQuiz) return
    setQuizLoading(true)
    setError('')
    try {
      const result = await submitQuizAnswers(currentQuiz.attemptId, quizAnswers)
      setQuizResult(result)
      if (selectedTopicId) {
        await loadTopicDetail(selectedTopicId)
      }
      const qList = await getReviewQueue().catch(() => [])
      setReviewQueue(qList)
    } catch {
      setError('Không thể nộp bài kiểm tra.')
    } finally {
      setQuizLoading(false)
    }
  }

  // Save Takeaway Note
  const handleSaveTakeaway = async (resourceId: number) => {
    if (!takeawayText.trim() || !resourceId) return
    try {
      await createResourceNote(resourceId, `[Takeaway]: ${takeawayText.trim()}`)
      setTakeawayText('')
      setTakeawaySaved(true)
      setTimeout(() => setTakeawaySaved(false), 3000)
    } catch {
      setError('Không thể lưu điều cần nhớ.')
    }
  }

  // Attach Source to Topic
  const handleAttachSource = async (resourceId: number) => {
    if (!selectedTopicId) return
    try {
      const updated = await addTopicSource(selectedTopicId, resourceId)
      setTopicDetail(updated)
      setAddSourceModal(false)
    } catch {
      setError('Không thể thêm tài liệu vào chủ đề.')
    }
  }

  // Remove Source from Topic
  const handleRemoveSource = async (resourceId: number) => {
    if (!selectedTopicId) return
    try {
      const updated = await removeTopicSource(selectedTopicId, resourceId)
      setTopicDetail(updated)
    } catch {
      setError('Không thể xóa tài liệu khỏi chủ đề.')
    }
  }

  // Delete Topic
  const handleDeleteTopic = async (topicId: number) => {
    setError('')
    try {
      await deleteStudyTopic(topicId)
      setTopicToDelete(null)
      const tList = await getStudyTopics()
      setTopics(tList)
      if (tList.length > 0) {
        const nextId = tList[0].id
        setSelectedTopicId(nextId)
        await loadTopicDetail(nextId)
      } else {
        setSelectedTopicId(null)
        setTopicDetail(null)
      }
      const qList = await getReviewQueue().catch(() => [])
      setReviewQueue(qList)
    } catch {
      setError('Không thể xóa chủ đề học tập.')
    }
  }

  // Direct Ingest into Topic (reuses standard ingestion)
  const handleDirectUpload = async (file?: File) => {
    if (!file || !selectedTopicId) return
    setUploadBusy(true)
    setError('')
    try {
      const created = await uploadResource(file)
      const updated = await addTopicSource(selectedTopicId, created.id)
      setTopicDetail(updated)
      const resList = await getResources()
      setAllResources(resList.items)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể nạp tệp tài liệu.'))
    } finally {
      setUploadBusy(false)
    }
  }

  const selectedConcept = topicDetail?.concepts.find(c => c.id === selectedConceptId)

  // Find next recommended concept
  const nextRecommendedConcept = topicDetail?.concepts.find(c => c.studyStatus === 'REVIEW_NEEDED') ||
    topicDetail?.concepts.find(c => c.studyStatus === 'LEARNING') ||
    topicDetail?.concepts.find(c => c.studyStatus === 'NOT_STARTED')

  if (loading) {
    return (
      <section className="kos-page kos-focus-hub">
        <div className="kos-empty">
          <Sparkles size={32} className="kos-spin" />
          <h2>Đang tải Topic Deepdive Learning Studio...</h2>
        </div>
      </section>
    )
  }

  return (
    <section className="kos-page kos-focus-hub">
      {/* Top Header */}
      <header className="kos-page-header">
        <div>
          <p className="kos-kicker">TOPIC DEEPDIVE LEARNING STUDIO</p>
          <h1>Learn → Recall → Verify → Connect → Continue</h1>
        </div>
        <div className="kos-library-actions">
          <button
            type="button"
            className={`kos-button ${activeTab === 'QUEUE' ? 'kos-button--primary' : ''}`}
            onClick={() => setActiveTab('QUEUE')}
          >
            <RotateCcw size={16} />
            Hàng đợi Ôn tập ({reviewQueue.length})
          </button>
          <button
            type="button"
            className="kos-button kos-button--primary"
            onClick={() => setNewTopicModal(true)}
          >
            <FolderPlus size={16} /> Tạo Topic Học Sâu
          </button>
        </div>
      </header>

      {error && (
        <div className="kos-callout kos-callout-warning" style={{ margin: '0 0 1.25rem' }}>
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* Sleek Topic Switcher Ribbon */}
      <div className="kos-topic-selector-bar">
        <div className="kos-topic-bar-header">
          <span className="kos-topic-bar-label">
            <Folder size={15} /> Danh sách Chủ đề ({topics.length})
          </span>
          <button
            type="button"
            className="kos-button kos-button--sm kos-button--primary"
            onClick={() => setNewTopicModal(true)}
          >
            <Plus size={14} /> Thêm Topic Mới
          </button>
        </div>

        <div className="kos-topic-pills-scroll-container">
          <div className="kos-topic-pills">
            {topics.map(t => {
              const isActive = selectedTopicId === t.id
              return (
                <div
                  key={t.id}
                  className={`kos-topic-pill-wrapper ${isActive ? 'is-active' : ''}`}
                  onClick={() => handleSelectTopic(t.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => {
                    if (e.key === 'Enter' || e.key === ' ') handleSelectTopic(t.id)
                  }}
                >
                  <span className="kos-topic-pill-title">🎯 {t.title}</span>
                  <span className="kos-topic-pill-count">{t.conceptCount} mục</span>
                  <button
                    type="button"
                    className="kos-topic-pill-delete-btn"
                    title={`Xóa chủ đề "${t.title}"`}
                    onClick={e => {
                      e.stopPropagation()
                      setTopicToDelete({ id: t.id, title: t.title })
                    }}
                  >
                    <Trash2 size={12} />
                  </button>
                </div>
              )
            })}
            {topics.length === 0 && (
              <span style={{ fontSize: '0.84rem', color: 'var(--kos-muted)', padding: '0.25rem 0.5rem' }}>
                Chưa có Chủ đề nào. Hãy bấm "Thêm Topic Mới" để bắt đầu!
              </span>
            )}
          </div>
        </div>
      </div>

      {topicDetail ? (
        <div className="kos-focus-stage-layout">
          {/* Left Column: Learning Path Sidebar */}
          <aside className="kos-focus-sidebar">
            <div className="kos-focus-sidebar-header">
              <div className="kos-sidebar-topic-info">
                <h3>{topicDetail.title}</h3>
                <p>{topicDetail.resources.length} tài liệu • {topicDetail.concepts.length} khái niệm</p>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="kos-icon-btn"
                  title="Tạo lại lộ trình học từ tài liệu"
                  onClick={handleGeneratePlan}
                  disabled={planBusy}
                >
                  <RefreshCw size={14} className={planBusy ? 'kos-spin' : ''} />
                </button>
                <button
                  type="button"
                  className="kos-icon-btn kos-icon-btn--danger"
                  title="Xóa chủ đề học tập này"
                  onClick={() => setTopicToDelete({ id: topicDetail.id, title: topicDetail.title })}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>

            {/* Discrete Status Summary Bar */}
            <div className="kos-status-bar-pill-row">
              <span className="kos-badge kos-badge--success" title="Đã nắm vững">
                ✓ {topicDetail.checkedCount} Đã hiểu
              </span>
              <span className="kos-badge kos-badge--danger" title="Cần ôn tập lại">
                ! {topicDetail.reviewNeededCount} Cần ôn
              </span>
              <span className="kos-badge kos-badge--primary" title="Đang học">
                ● {topicDetail.learningCount} Đang học
              </span>
              <span className="kos-badge kos-badge--neutral" title="Chưa bắt đầu">
                ○ {topicDetail.notStartedCount} Chưa học
              </span>
            </div>

            {/* Navigation Tabs within Topic */}
            <div className="kos-sidebar-subnav">
              <button
                type="button"
                className={`kos-subnav-btn ${activeTab === 'PATH' ? 'is-active' : ''}`}
                onClick={() => setActiveTab('PATH')}
              >
                <BookOpen size={14} /> Lộ trình Học sâu
              </button>
              <button
                type="button"
                className={`kos-subnav-btn ${activeTab === 'QUIZ' ? 'is-active' : ''}`}
                onClick={() => handleStartQuiz()}
              >
                <HelpCircle size={14} /> Kiểm tra Ghi nhớ (Recall)
              </button>
              <button
                type="button"
                className={`kos-subnav-btn ${activeTab === 'MAP' ? 'is-active' : ''}`}
                onClick={() => setActiveTab('MAP')}
              >
                <Network size={14} /> Bản đồ Nguồn (Map)
              </button>
              <button
                type="button"
                className={`kos-subnav-btn ${activeTab === 'SOURCES' ? 'is-active' : ''}`}
                onClick={() => setActiveTab('SOURCES')}
              >
                <Layers size={14} /> Nguồn tài liệu ({topicDetail.resources.length})
              </button>
            </div>

            {/* Concepts Ordered List */}
            <div className="kos-focus-resource-list">
              {topicDetail.concepts.length > 0 ? (
                topicDetail.concepts.map((concept, idx) => {
                  let statusBadge = '○'
                  let statusColor = 'var(--kos-muted)'
                  if (concept.studyStatus === 'CHECKED') {
                    statusBadge = '✓'
                    statusColor = 'var(--kos-green)'
                  } else if (concept.studyStatus === 'REVIEW_NEEDED') {
                    statusBadge = '!'
                    statusColor = 'var(--kos-red)'
                  } else if (concept.studyStatus === 'LEARNING') {
                    statusBadge = '●'
                    statusColor = 'var(--kos-blue)'
                  }

                  return (
                    <button
                      key={concept.id}
                      type="button"
                      className={`kos-focus-res-item ${selectedConceptId === concept.id && activeTab === 'PATH' ? 'is-active' : ''}`}
                      onClick={() => {
                        setSelectedConceptId(concept.id)
                        setActiveTab('PATH')
                      }}
                    >
                      <span className="kos-concept-order-badge" style={{ borderColor: statusColor, color: statusColor }}>
                        {statusBadge} {idx + 1 < 10 ? `0${idx + 1}` : idx + 1}
                      </span>
                      <div className="kos-res-meta">
                        <h4 className="kos-res-item-title">{concept.title}</h4>
                        <small>{concept.sources.length} phân đoạn nguồn đối chứng</small>
                      </div>
                      <ChevronRight size={14} className="kos-res-arrow" />
                    </button>
                  )
                })
              ) : (
                <div className="kos-box-empty" style={{ padding: '1.5rem 1rem' }}>
                  <p>Chưa có lộ trình khái niệm nào.</p>
                  <button
                    type="button"
                    className="kos-button kos-button--primary"
                    style={{ marginTop: '.75rem' }}
                    onClick={handleGeneratePlan}
                    disabled={planBusy}
                  >
                    <Sparkles size={15} /> {planBusy ? 'Đang tạo lộ trình...' : 'Tạo Lộ trình Học ngay'}
                  </button>
                </div>
              )}
            </div>
          </aside>

          {/* Right Column: Deep Dive Learning Studio Main Stage */}
          <main className="kos-focus-main-panel">
            {/* NEXT ACTION RECOMMENDATION BANNER */}
            {nextRecommendedConcept && activeTab === 'PATH' && (
              <div className="kos-next-action-banner">
                <div className="kos-next-action-info">
                  <span className="kos-next-action-tag">
                    {nextRecommendedConcept.studyStatus === 'REVIEW_NEEDED' ? '🚨 CẦN ÔN TẬP LẠI' : '⚡ BƯỚC HỌC TIẾP THEO'}
                  </span>
                  <h3>{nextRecommendedConcept.title}</h3>
                  <p>{nextRecommendedConcept.whyItMatters || nextRecommendedConcept.summary}</p>
                </div>
                <div className="kos-next-action-btns">
                  <button
                    type="button"
                    className="kos-button kos-button--primary"
                    onClick={() => {
                      setSelectedConceptId(nextRecommendedConcept.id)
                      handleUpdateStatus(nextRecommendedConcept.id, 'LEARNING')
                    }}
                  >
                    Học sâu khái niệm này <ArrowRight size={15} />
                  </button>
                  <button
                    type="button"
                    className="kos-button"
                    onClick={() => handleStartQuiz(nextRecommendedConcept.id)}
                  >
                    <HelpCircle size={15} /> Kiểm tra ghi nhớ
                  </button>
                </div>
              </div>
            )}

            {/* TAB 1: CONCEPT DEEP DIVE VIEW */}
            {activeTab === 'PATH' && selectedConcept && (
              <div className="kos-concept-deepdive-pane">
                <div className="kos-deepdive-header">
                  <div className="kos-deepdive-title-area">
                    <span className="kos-banner-kicker">KHÁI NIỆM TRỌNG TÂM #{selectedConcept.position}</span>
                    <h2>{selectedConcept.title}</h2>
                  </div>

                  {/* Status Toggle Button Group */}
                  <div className="kos-status-toggle-group">
                    <button
                      type="button"
                      className={`kos-toggle-btn ${selectedConcept.studyStatus === 'CHECKED' ? 'is-checked' : ''}`}
                      onClick={() => handleUpdateStatus(selectedConcept.id, 'CHECKED')}
                    >
                      <CheckCircle2 size={15} /> Đã hiểu
                    </button>
                    <button
                      type="button"
                      className={`kos-toggle-btn ${selectedConcept.studyStatus === 'REVIEW_NEEDED' ? 'is-review' : ''}`}
                      onClick={() => handleUpdateStatus(selectedConcept.id, 'REVIEW_NEEDED')}
                    >
                      <AlertCircle size={15} /> Cần ôn lại
                    </button>
                    <button
                      type="button"
                      className={`kos-toggle-btn ${selectedConcept.studyStatus === 'LEARNING' ? 'is-learning' : ''}`}
                      onClick={() => handleUpdateStatus(selectedConcept.id, 'LEARNING')}
                    >
                      <BookOpen size={15} /> Đang học
                    </button>
                  </div>
                </div>

                {/* Explanation / Why it Matters */}
                <div className="kos-deepdive-body">
                  <div className="kos-deepdive-card">
                    <h4>💡 Tóm tắt Cốt lõi & Bản chất</h4>
                    <p className="kos-deepdive-text">{selectedConcept.summary}</p>
                  </div>

                  {selectedConcept.whyItMatters && (
                    <div className="kos-deepdive-card kos-deepdive-card--accent">
                      <h4>🎯 Tại sao Khái niệm này quan trọng?</h4>
                      <p className="kos-deepdive-text">{selectedConcept.whyItMatters}</p>
                    </div>
                  )}

                  {/* Grounded Source Evidence Section */}
                  <div className="kos-deepdive-evidence-section">
                    <div className="kos-evidence-header">
                      <h4>
                        <Compass size={16} /> Bằng chứng Xác thực từ Tài liệu Nguồn ({selectedConcept.sources.length})
                      </h4>
                      <Link
                        to={`/knowledge/ask?resources=${selectedConcept.sources.map(s => s.resourceId).join(',')}`}
                        className="kos-ask-concept-link"
                      >
                        <BrainCircuit size={15} /> Hỏi RAG AI về khái niệm này
                      </Link>
                    </div>

                    <div className="kos-evidence-list">
                      {selectedConcept.sources.map((src, sIdx) => (
                        <div key={sIdx} className="kos-evidence-card">
                          <div className="kos-evidence-card-header">
                            <span className="kos-evidence-source-title">
                              📄 {src.resourceTitle} (Phân đoạn #{src.chunkId})
                            </span>
                            <Link
                              to={`/library/${src.resourceId}`}
                              className="kos-evidence-open-btn"
                              title="Mở tài liệu gốc trong Workspace"
                            >
                              Mở tài liệu gốc <ArrowRight size={13} />
                            </Link>
                          </div>
                          <blockquote className="kos-evidence-quote">
                            "{src.snippet}"
                          </blockquote>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Save Takeaway Note Box */}
                  <div className="kos-takeaway-box">
                    <h4>📝 Lưu điều cần nhớ (Key Takeaway)</h4>
                    <p style={{ fontSize: '0.82rem', color: 'var(--kos-muted)', margin: '0 0 0.5rem' }}>
                      Điều cốt lõi nhất bạn muốn ghi nhớ về "{selectedConcept.title}" là gì?
                    </p>
                    <div className="kos-takeaway-input-row">
                      <input
                        type="text"
                        value={takeawayText}
                        onChange={e => setTakeawayText(e.target.value)}
                        placeholder="Ví dụ: RRF kết hợp thứ hạng từ nhiều thuật toán tìm kiếm..."
                        onKeyDown={e => {
                          if (e.key === 'Enter' && selectedConcept.sources[0]) {
                            handleSaveTakeaway(selectedConcept.sources[0].resourceId)
                          }
                        }}
                      />
                      <button
                        type="button"
                        className="kos-button kos-button--primary"
                        onClick={() => selectedConcept.sources[0] && handleSaveTakeaway(selectedConcept.sources[0].resourceId)}
                        disabled={!takeawayText.trim() || !selectedConcept.sources[0]}
                      >
                        <Plus size={15} /> Lưu Note
                      </button>
                    </div>
                    {takeawaySaved && (
                      <p className="kos-text-success" style={{ fontSize: '0.82rem', marginTop: '0.4rem' }}>
                        ✓ Đã lưu điều cần nhớ vào ghi chú tài liệu!
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: ACTIVE RECALL CHECK (QUIZ) */}
            {activeTab === 'QUIZ' && (
              <div className="kos-quiz-pane">
                <div className="kos-quiz-header">
                  <div>
                    <span className="kos-banner-kicker">ACTIVE RECALL ASSESSMENT</span>
                    <h2>🎯 Kiểm tra Ghi nhớ & Nhận thức</h2>
                    <p>Bộ câu hỏi trắc nghiệm tự động đối chứng 100% từ phân đoạn tài liệu nguồn</p>
                  </div>
                  {quizResult && (
                    <div className="kos-quiz-score-badge">
                      Kết quả: {quizResult.scoreCorrect} / {quizResult.totalQuestions} ({quizResult.percentage}%)
                    </div>
                  )}
                </div>

                {quizLoading ? (
                  <div className="kos-empty">
                    <Sparkles size={28} className="kos-spin" />
                    <p>Đang chuẩn bị bộ câu hỏi đối chứng tri thức...</p>
                  </div>
                ) : currentQuiz && currentQuiz.questions.length > 0 ? (
                  <div className="kos-quiz-question-list">
                    {currentQuiz.questions.map((q, qIdx) => {
                      const evaluated = quizResult?.results.find(r => r.id === q.id)
                      const isCorrect = evaluated?.userAnswer === evaluated?.correctOption
                      const isSubmitted = !!quizResult

                      return (
                        <div key={q.id} className="kos-quiz-card">
                          <h4 className="kos-quiz-q-title">
                            <span className="kos-q-number">Câu {qIdx + 1}:</span> {q.question}
                          </h4>

                          <div className="kos-quiz-options">
                            {q.options.map((opt, optIdx) => {
                              const isSelected = isSubmitted
                                ? evaluated?.userAnswer === optIdx
                                : quizAnswers[q.id] === optIdx

                              let optClass = 'kos-quiz-opt'
                              if (isSelected) optClass += ' is-selected'
                              if (isSubmitted) {
                                if (optIdx === evaluated?.correctOption) optClass += ' is-correct'
                                else if (isSelected && !isCorrect) optClass += ' is-wrong'
                              }

                              return (
                                <button
                                  key={optIdx}
                                  type="button"
                                  className={optClass}
                                  onClick={() => !isSubmitted && setQuizAnswers(prev => ({ ...prev, [q.id]: optIdx }))}
                                  disabled={isSubmitted}
                                >
                                  <span className="kos-opt-letter">{String.fromCharCode(65 + optIdx)}</span>
                                  <span className="kos-opt-text">{opt}</span>
                                  {isSubmitted && optIdx === evaluated?.correctOption && (
                                    <CheckCircle2 size={16} className="kos-text-success" />
                                  )}
                                  {isSubmitted && isSelected && !isCorrect && (
                                    <XCircle size={16} className="kos-text-danger" />
                                  )}
                                </button>
                              )
                            })}
                          </div>

                          {/* Grounded Explanation & Source Citation on Submission */}
                          {isSubmitted && evaluated && (
                            <div className={`kos-quiz-explanation ${isCorrect ? 'is-correct-box' : 'is-wrong-box'}`}>
                              <div style={{ marginBottom: '0.4rem' }}>
                                <strong>{isCorrect ? '✓ Đúng rồi!' : '✗ Chưa chính xác:'}</strong> {evaluated.explanation}
                              </div>
                              {evaluated.sourceResourceTitle && (
                                <div className="kos-quiz-source-link">
                                  <span>📖 <strong>Nguồn đối chứng:</strong> {evaluated.sourceResourceTitle}</span>
                                  {evaluated.sourceResourceId && (
                                    <Link to={`/library/${evaluated.sourceResourceId}`} className="kos-link-sm">
                                      Mở tài liệu nguồn <ArrowRight size={12} />
                                    </Link>
                                  )}
                                </div>
                              )}
                              {evaluated.sourceSnippet && (
                                <blockquote className="kos-quiz-evidence-quote">
                                  "{evaluated.sourceSnippet}"
                                </blockquote>
                              )}
                            </div>
                          )}
                        </div>
                      )
                    })}

                    <div className="kos-quiz-actions">
                      {!quizResult ? (
                        <button
                          type="button"
                          className="kos-button kos-button--primary"
                          onClick={handleSubmitQuiz}
                          disabled={Object.keys(quizAnswers).length === 0 || quizLoading}
                        >
                          <Check size={16} /> Nộp bài & Xem đối chứng đáp án
                        </button>
                      ) : (
                        <div style={{ display: 'flex', gap: '0.75rem' }}>
                          <button
                            type="button"
                            className="kos-button kos-button--primary"
                            onClick={() => handleStartQuiz()}
                          >
                            <RotateCcw size={16} /> Làm lại bài kiểm tra
                          </button>
                          <button
                            type="button"
                            className="kos-button"
                            onClick={() => setActiveTab('PATH')}
                          >
                            Quay lại Lộ trình học
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="kos-box-empty">
                    <p>Chưa có bài kiểm tra ghi nhớ.</p>
                    <button
                      type="button"
                      className="kos-button kos-button--primary"
                      onClick={() => handleStartQuiz()}
                      style={{ marginTop: '0.75rem' }}
                    >
                      Bắt đầu bài Kiểm tra ngay
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: EVIDENCE MAP (BẢN ĐỒ KIẾN THỨC & NGUỒN) */}
            {activeTab === 'MAP' && (
              <div className="kos-evidence-map-pane">
                <div className="kos-map-header">
                  <div>
                    <span className="kos-banner-kicker">SOURCE EVIDENCE MAP</span>
                    <h2>🗺️ Bản đồ Kiến thức & Mạng lưới Nguồn</h2>
                    <p>Mỗi nút thể hiện một khái niệm có liên kết bằng chứng xác thực với tài liệu gốc</p>
                  </div>
                </div>

                <div className="kos-map-grid">
                  {topicDetail.concepts.map((concept, idx) => (
                    <div
                      key={concept.id}
                      className={`kos-map-concept-node ${selectedConceptId === concept.id ? 'is-active' : ''}`}
                      onClick={() => setSelectedConceptId(concept.id)}
                    >
                      <div className="kos-map-node-top">
                        <span className="kos-badge kos-badge--primary">#{idx + 1} {concept.studyStatus}</span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--kos-muted)' }}>
                          {concept.sources.length} nguồn
                        </span>
                      </div>
                      <h4>{concept.title}</h4>
                      <p>{concept.summary.slice(0, 120)}…</p>
                      <div className="kos-map-node-footer">
                        <button
                          type="button"
                          className="kos-button kos-button--sm"
                          onClick={e => {
                            e.stopPropagation()
                            setSelectedConceptId(concept.id)
                            setActiveTab('PATH')
                          }}
                        >
                          Học sâu <ArrowRight size={12} />
                        </button>
                        <button
                          type="button"
                          className="kos-button kos-button--sm"
                          onClick={e => {
                            e.stopPropagation()
                            handleStartQuiz(concept.id)
                          }}
                        >
                          Recall
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 4: REVIEW QUEUE (HÀNG ĐỢI ÔN TẬP) */}
            {activeTab === 'QUEUE' && (
              <div className="kos-review-queue-pane">
                <div className="kos-queue-header">
                  <div>
                    <span className="kos-banner-kicker">ACTIVE REVIEW QUEUE</span>
                    <h2>🔄 Hàng đợi Ôn tập (Các khái niệm cần củng cố)</h2>
                    <p>Tự động tổng hợp từ các câu trả lời chưa đúng hoặc được đánh dấu cần ôn tập</p>
                  </div>
                </div>

                {reviewQueue.length > 0 ? (
                  <div className="kos-queue-list">
                    {reviewQueue.map(item => (
                      <div key={item.conceptId} className="kos-queue-item-card">
                        <div className="kos-queue-item-header">
                          <div>
                            <span className="kos-badge kos-badge--danger">CẦN ÔN TẬP</span>
                            <h4>{item.conceptTitle}</h4>
                            <small>Thuộc chủ đề: <strong>{item.topicTitle}</strong></small>
                          </div>
                          <button
                            type="button"
                            className="kos-button kos-button--primary"
                            onClick={() => {
                              handleSelectTopic(item.topicId)
                              setSelectedConceptId(item.conceptId)
                              setActiveTab('PATH')
                            }}
                          >
                            Ôn tập ngay <ArrowRight size={14} />
                          </button>
                        </div>
                        <p>{item.summary}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="kos-box-empty">
                    <CheckCircle2 size={32} className="kos-text-success" />
                    <h3>Tuyệt vời! Hàng đợi ôn tập đang trống.</h3>
                    <p>Bạn đã hoàn thành tốt các bài kiểm tra ghi nhớ và nắm vững các khái niệm đã học.</p>
                  </div>
                )}
              </div>
            )}

            {/* TAB 5: TOPIC ATTACHED SOURCES (QUẢN LÝ NGUỒN) */}
            {activeTab === 'SOURCES' && (
              <div className="kos-sources-pane">
                <div className="kos-sources-header">
                  <div>
                    <span className="kos-banner-kicker">SOURCE ATTACHMENTS</span>
                    <h2>📚 Tài liệu Tri thức trong Chủ đề</h2>
                    <p>Các tài liệu được dùng để bóc tách khái niệm và sinh câu hỏi ôn tập</p>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      type="button"
                      className="kos-button"
                      onClick={() => setAddSourceModal(true)}
                    >
                      <Plus size={15} /> Thêm từ Thư viện
                    </button>
                    <label className="kos-button kos-button--primary">
                      <Upload size={15} /> {uploadBusy ? 'Đang tải...' : 'Tải file mới vào Topic'}
                      <input
                        type="file"
                        accept=".pdf,.docx,.txt,.md,.markdown"
                        hidden
                        disabled={uploadBusy}
                        onChange={e => {
                          const f = e.target.files?.[0]
                          e.target.value = ''
                          handleDirectUpload(f)
                        }}
                      />
                    </label>
                  </div>
                </div>

                <div className="kos-sources-list">
                  {topicDetail.resources.map(res => {
                    const isReady = res.processingStatus === 'READY'
                    return (
                      <div key={res.id} className="kos-source-item-card">
                        <div className="kos-source-info">
                          <span className="kos-res-type-badge">{res.resourceType}</span>
                          <div>
                            <h4>{res.title}</h4>
                            <small>
                              {isReady ? (
                                <span className="kos-text-success">🟢 Sẵn sàng tra cứu & học sâu</span>
                              ) : (
                                <span className="kos-text-amber">⏳ Đang xử lý: {res.processingStatus}</span>
                              )}
                            </small>
                          </div>
                        </div>
                        <div className="kos-source-actions">
                          <Link to={`/library/${res.id}`} className="kos-button">
                            Mở Workspace
                          </Link>
                          <button
                            type="button"
                            className="kos-icon-btn kos-icon-btn--danger"
                            title="Xóa khỏi Topic này"
                            onClick={() => handleRemoveSource(res.id)}
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
          </main>
        </div>
      ) : (
        <div className="kos-empty" style={{ maxWidth: '640px', margin: '2rem auto' }}>
          <Compass size={40} />
          <h2>Bắt đầu hành trình học tập chuyên sâu</h2>
          <p style={{ color: 'var(--kos-muted)', lineHeight: '1.5' }}>
            Phân hệ Focus giúp bạn biến tài liệu thô thành một chu trình học tập có cấu trúc và đối chứng:
          </p>

          <div style={{ textAlign: 'left', background: 'var(--kos-surface)', border: '1px solid var(--kos-border)', borderRadius: '10px', padding: '1.25rem', margin: '1.25rem 0', display: 'flex', flexDirection: 'column', gap: '0.75rem', width: '100%' }}>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              <span style={{ background: 'var(--kos-blue)', color: '#fff', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 'bold', flexShrink: 0 }}>1</span>
              <div>
                <strong style={{ fontSize: '0.9rem' }}>Nạp tài liệu học tập:</strong>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: 'var(--kos-muted)' }}>Tải lên tệp PDF, DOCX, TXT, Markdown hoặc Ghi chú vào Thư viện.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              <span style={{ background: 'var(--kos-blue)', color: '#fff', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 'bold', flexShrink: 0 }}>2</span>
              <div>
                <strong style={{ fontSize: '0.9rem' }}>Tạo Topic Học Sâu:</strong>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: 'var(--kos-muted)' }}>Đặt tên chủ đề, mô tả mục tiêu và tích chọn các tài liệu nguồn liên quan.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              <span style={{ background: 'var(--kos-blue)', color: '#fff', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 'bold', flexShrink: 0 }}>3</span>
              <div>
                <strong style={{ fontSize: '0.9rem' }}>Lộ trình Khái niệm & Bằng chứng:</strong>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: 'var(--kos-muted)' }}>Hệ thống tự động phân tách các khái niệm trọng tâm kèm đoạn trích dẫn nguồn đối chứng.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              <span style={{ background: 'var(--kos-blue)', color: '#fff', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 'bold', flexShrink: 0 }}>4</span>
              <div>
                <strong style={{ fontSize: '0.9rem' }}>Kiểm tra Ghi nhớ (Recall) & Ôn tập:</strong>
                <p style={{ margin: '0.2rem 0 0', fontSize: '0.82rem', color: 'var(--kos-muted)' }}>Làm bài trắc nghiệm tự động, câu sai sẽ tự động đưa vào Hàng đợi Ôn tập (Review Queue).</p>
              </div>
            </div>
          </div>

          <button
            type="button"
            className="kos-button kos-button--primary"
            onClick={() => setNewTopicModal(true)}
          >
            <FolderPlus size={16} /> Tạo Topic Học Sâu Ngay
          </button>
        </div>
      )}

      {/* MODAL: CREATE TOPIC */}
      {newTopicModal && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <form onSubmit={handleCreateTopic} style={{ maxWidth: '540px' }}>
            <button className="kos-modal-close" type="button" onClick={() => setNewTopicModal(false)}>
              <X size={16} />
            </button>
            <p className="kos-kicker">TẠO TOPIC HỌC SÂU MỚI</p>
            <h3>Bạn muốn tìm hiểu và làm chủ kiến thức gì?</h3>

            <label>
              Tên Chủ Đề (Topic Title)
              <input
                value={newTopicTitle}
                onChange={e => setNewTopicTitle(e.target.value)}
                placeholder="Ví dụ: Hiểu Strategy Pattern và cách áp dụng trong KnowledgeOS"
                required
              />
            </label>

            <label>
              Mục tiêu học tập cụ thể (Goal)
              <textarea
                value={newTopicGoal}
                onChange={e => setNewTopicGoal(e.target.value)}
                placeholder="Mô tả mục tiêu cụ thể bạn muốn đạt được sau khi học xong chủ đề này..."
                rows={3}
              />
            </label>

            <label>
              Chọn tài liệu nguồn từ Thư viện ({selectedResourceIds.length} đã chọn)
              <div className="kos-source-picker-list">
                {allResources.length === 0 ? (
                  <div style={{ padding: '1rem', textAlign: 'center', background: 'var(--kos-subtle)', borderRadius: '6px', fontSize: '0.84rem' }}>
                    <p style={{ margin: '0 0 0.5rem', color: 'var(--kos-muted)' }}>
                      Thư viện chưa có tài liệu. Tải tệp lên ngay tại đây:
                    </p>
                    <label className="kos-button kos-button--sm kos-button--primary" style={{ cursor: 'pointer', display: 'inline-flex' }}>
                      <Upload size={14} /> {uploadBusy ? 'Đang nạp...' : 'Tải lên tài liệu mới'}
                      <input
                        type="file"
                        accept=".pdf,.docx,.txt,.md,.markdown"
                        hidden
                        disabled={uploadBusy}
                        onChange={async e => {
                          const file = e.target.files?.[0]
                          e.target.value = ''
                          if (file) {
                            setUploadBusy(true)
                            try {
                              const created = await uploadResource(file)
                              setSelectedResourceIds(prev => [...prev, created.id])
                              const list = await getResources()
                              setAllResources(list.items)
                            } catch (err) {
                              setError(getApiErrorMessage(err, 'Không thể nạp tệp tài liệu.'))
                            } finally {
                              setUploadBusy(false)
                            }
                          }
                        }}
                      />
                    </label>
                  </div>
                ) : (
                  allResources.map(r => {
                    const isChecked = selectedResourceIds.includes(r.id)
                    return (
                      <label key={r.id} className="kos-source-picker-item">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={e => {
                            if (e.target.checked) {
                              setSelectedResourceIds(prev => [...prev, r.id])
                            } else {
                              setSelectedResourceIds(prev => prev.filter(id => id !== r.id))
                            }
                          }}
                        />
                        <span>[{r.resourceType}] {r.title}</span>
                      </label>
                    )
                  })
                )}
              </div>
            </label>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.25rem' }}>
              <button type="button" className="kos-button" onClick={() => setNewTopicModal(false)}>
                Hủy
              </button>
              <button type="submit" className="kos-button kos-button--primary" disabled={!newTopicTitle.trim()}>
                <FolderPlus size={16} /> Tạo Topic & Xây Lộ Trình
              </button>
            </div>
          </form>
        </div>
      )}

      {/* MODAL: ADD SOURCE TO ACTIVE TOPIC */}
      {addSourceModal && selectedTopicId && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <div style={{ maxWidth: '520px', background: 'var(--kos-surface)', padding: '1.5rem', borderRadius: '10px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3>Thêm tài liệu vào Topic</h3>
              <button className="kos-icon-btn" onClick={() => setAddSourceModal(false)}>
                <X size={16} />
              </button>
            </div>
            <p style={{ fontSize: '0.85rem', color: 'var(--kos-muted)', marginBottom: '1rem' }}>
              Chọn tài liệu có sẵn trong thư viện để gắn vào chủ đề này:
            </p>
            <div className="kos-source-picker-list">
              {allResources
                .filter(r => !topicDetail?.resources.some(tr => tr.id === r.id))
                .map(r => (
                  <div key={r.id} className="kos-source-picker-item" style={{ justifyContent: 'space-between' }}>
                    <span>[{r.resourceType}] {r.title}</span>
                    <button
                      type="button"
                      className="kos-button kos-button--sm"
                      onClick={() => handleAttachSource(r.id)}
                    >
                      Thêm
                    </button>
                  </div>
                ))}
            </div>
          </div>
        </div>
      )}

      {/* MODAL: DELETE TOPIC CONFIRMATION */}
      {topicToDelete && (
        <div className="kos-modal" role="dialog" aria-modal="true">
          <div style={{ maxWidth: '460px', background: 'var(--kos-surface)', padding: '1.5rem', borderRadius: '10px', border: '1px solid var(--kos-line)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--kos-red)', marginBottom: '0.75rem' }}>
              <AlertCircle size={22} />
              <h3 style={{ margin: 0, fontSize: '1.15rem' }}>Xác nhận xóa Chủ đề?</h3>
            </div>
            <p style={{ fontSize: '0.9rem', color: 'var(--kos-ink)', lineHeight: '1.5', margin: '0 0 1rem' }}>
              Bạn có chắc chắn muốn xóa chủ đề <strong>"{topicToDelete.title}"</strong>?
            </p>
            <p style={{ fontSize: '0.82rem', color: 'var(--kos-muted)', lineHeight: '1.4', margin: '0 0 1.25rem', padding: '0.6rem 0.75rem', background: 'var(--kos-subtle)', borderRadius: '6px' }}>
              💡 Lộ trình học và bài kiểm tra thuộc chủ đề này sẽ bị xóa. Toàn bộ tài liệu gốc trong Thư viện vẫn được bảo toàn nguyên vẹn.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                className="kos-button"
                onClick={() => setTopicToDelete(null)}
              >
                Hủy
              </button>
              <button
                type="button"
                className="kos-button kos-button--danger"
                onClick={() => handleDeleteTopic(topicToDelete.id)}
              >
                <Trash2 size={15} /> Xóa Chủ Đề
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
