import {
  AlertCircle, ArrowRight, BookOpen, CheckCircle2, ChevronDown, Circle, CircleDot,
  FolderOpen, Library, Loader2, Network, RefreshCw, RotateCcw, Route,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  buildLearningArea, generateTopicQuiz, getLearningArea, getLearningAreas, getLearningAreaSourceMap,
  getStudyTopicDetail, getStudyTopics, initializeLearningArea, refreshLearningArea, submitQuizAnswers,
  updateConceptStatus, type LearningArea, type LearningAreaDetail, type LearningAreaSourceMap,
  type LearningModule, type QuizAttemptResponse, type StudyTopic, type StudyTopicDetail,
  type SubmitQuizAnswersResponse, type TopicConcept,
} from '../api/knowledge'
import { getApiErrorMessage } from '../api/errors'
import '../styles/focus-learning.css'

type FocusView = 'PATH' | 'SOURCES' | 'LEGACY'
const stageLabels: Record<LearningModule['stage'], string> = {
  FOUNDATION: 'Nền tảng', CORE: 'Cốt lõi', APPLICATION: 'Ứng dụng', ADVANCED: 'Nâng cao',
}
const statusMeta = {
  CHECKED: { label: 'Đã hiểu', icon: CheckCircle2 },
  REVIEW_NEEDED: { label: 'Cần ôn', icon: RotateCcw },
  LEARNING: { label: 'Đang học', icon: CircleDot },
  NOT_STARTED: { label: 'Chưa học', icon: Circle },
} as const

export default function KnowledgeFocusPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [areas, setAreas] = useState<LearningArea[]>([])
  const [legacyTopics, setLegacyTopics] = useState<StudyTopic[]>([])
  const [selectedCollectionId, setSelectedCollectionId] = useState<number | null>(null)
  const [detail, setDetail] = useState<LearningAreaDetail | null>(null)
  const [legacyDetail, setLegacyDetail] = useState<StudyTopicDetail | null>(null)
  const [selectedModuleId, setSelectedModuleId] = useState<number | null>(null)
  const [selectedConceptId, setSelectedConceptId] = useState<number | null>(null)
  const [view, setView] = useState<FocusView>('PATH')
  const [loading, setLoading] = useState(true)
  const [generationBusy, setGenerationBusy] = useState(false)
  const [error, setError] = useState('')
  const [sourceSelection, setSourceSelection] = useState<number[]>([])
  const [sourceMap, setSourceMap] = useState<LearningAreaSourceMap | null>(null)
  const [quiz, setQuiz] = useState<QuizAttemptResponse | null>(null)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [quizResult, setQuizResult] = useState<SubmitQuizAnswersResponse | null>(null)
  const [quizBusy, setQuizBusy] = useState(false)

  const loadArea = useCallback(async (areaId: number) => {
    const next = await getLearningArea(areaId)
    setDetail(next)
    setLegacyDetail(null)
    const recommended = next.modules.find(module => module.reviewNeededCount > 0)
      ?? next.modules.find(module => module.checkedCount < module.conceptCount) ?? next.modules[0]
    setSelectedModuleId(previous => next.modules.some(module => module.id === previous) ? previous : recommended?.id ?? null)
    setSelectedConceptId(previous => next.modules.some(module => module.concepts.some(concept => concept.id === previous)) ? previous : null)
    setSourceSelection(previous => previous.length
      ? previous.filter(id => next.resources.some(resource => resource.id === id))
      : next.resources.slice(0, 6).map(resource => resource.id))
  }, [])

  const loadInitial = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [areaList, topicList] = await Promise.all([getLearningAreas(), getStudyTopics()])
      setAreas(areaList)
      setLegacyTopics(topicList)
      const requestedAreaId = Number(searchParams.get('area')) || null
      const requestedCollectionId = Number(searchParams.get('collection')) || null
      const requestedLegacyId = Number(searchParams.get('topicId')) || null
      if (requestedLegacyId && topicList.some(topic => topic.id === requestedLegacyId)) {
        setView('LEGACY')
        setLegacyDetail(await getStudyTopicDetail(requestedLegacyId))
        setSelectedCollectionId(null)
        return
      }
      const selected = areaList.find(area => area.id === requestedAreaId)
        ?? areaList.find(area => area.collectionId === requestedCollectionId) ?? areaList[0]
      if (selected) {
        setSelectedCollectionId(selected.collectionId)
        if (selected.id) await loadArea(selected.id)
        else setDetail(null)
      }
    } catch (cause) {
      setError(getApiErrorMessage(cause, 'Không thể tải Learning Areas.'))
    } finally {
      setLoading(false)
    }
  }, [loadArea, searchParams])

  useEffect(() => { void loadInitial() }, [loadInitial])

  const selectedArea = areas.find(area => area.collectionId === selectedCollectionId) ?? null
  const selectedModule = detail?.modules.find(module => module.id === selectedModuleId) ?? null
  const selectedConcept = useMemo(() => {
    if (!detail || !selectedConceptId) return null
    return detail.modules.flatMap(module => module.concepts).find(concept => concept.id === selectedConceptId) ?? null
  }, [detail, selectedConceptId])

  const selectArea = async (area: LearningArea) => {
    setView('PATH')
    setSelectedCollectionId(area.collectionId)
    setSourceMap(null)
    setQuiz(null)
    setQuizResult(null)
    setError('')
    setSearchParams(area.id ? { area: String(area.id) } : { collection: String(area.collectionId) })
    if (area.id) await loadArea(area.id)
    else { setDetail(null); setSelectedModuleId(null); setSelectedConceptId(null) }
  }

  const buildOrRefresh = async () => {
    if (!selectedArea) return
    setGenerationBusy(true)
    setError('')
    try {
      let current = detail
      if (!selectedArea.id) current = await initializeLearningArea(selectedArea.collectionId)
      if (!current) return
      const built = current.area.currentVersion > 0
        ? await refreshLearningArea(current.area.id!) : await buildLearningArea(current.area.id!)
      setDetail(built)
      setAreas(await getLearningAreas())
      setSearchParams({ area: String(built.area.id) })
      setSelectedModuleId(built.modules[0]?.id ?? null)
    } catch (cause) {
      setError(getApiErrorMessage(cause, 'Không thể xây lộ trình. Lộ trình hiện tại vẫn được giữ nguyên.'))
      if (selectedArea.id) { try { await loadArea(selectedArea.id) } catch { /* preserve error */ } }
    } finally { setGenerationBusy(false) }
  }

  const setConceptStatus = async (concept: TopicConcept, status: TopicConcept['studyStatus']) => {
    if (!detail?.area.id) return
    try {
      await updateConceptStatus(detail.area.id, concept.id, status)
      await loadArea(detail.area.id)
      setSelectedConceptId(concept.id)
    } catch (cause) { setError(getApiErrorMessage(cause, 'Không thể cập nhật trạng thái học.')) }
  }

  const startRecall = async (concept: TopicConcept) => {
    if (!detail?.area.id) return
    setQuizBusy(true); setQuiz(null); setQuizResult(null); setAnswers({})
    try { setQuiz(await generateTopicQuiz(detail.area.id, concept.id)) }
    catch (cause) { setError(getApiErrorMessage(cause, 'Chưa thể tạo Recall từ bằng chứng của khái niệm này.')) }
    finally { setQuizBusy(false) }
  }

  const submitRecall = async () => {
    if (!quiz) return
    setQuizBusy(true)
    try {
      setQuizResult(await submitQuizAnswers(quiz.attemptId, answers))
      if (detail?.area.id) await loadArea(detail.area.id)
    } catch (cause) { setError(getApiErrorMessage(cause, 'Không thể nộp bài Recall.')) }
    finally { setQuizBusy(false) }
  }

  const loadSourceMap = async () => {
    if (!detail?.area.id) return
    try { setSourceMap(await getLearningAreaSourceMap(detail.area.id, sourceSelection)) }
    catch (cause) { setError(getApiErrorMessage(cause, 'Không thể tải Selected Sources Map.')) }
  }

  if (loading) return <FocusLoading />

  return <section className="kos-page kos-learning-focus">
    <header className="kos-learning-focus__top"><div><p className="kos-kicker">FOCUS</p><h1>Học theo vùng kiến thức</h1>
      <p>Collection trở thành lộ trình có thứ tự. Recall và mastery tiếp tục bám vào bằng chứng thật.</p></div>
      <Link to="/library" className="kos-button"><Library size={16} /> Quản lý nguồn</Link></header>
    {error && <div className="kos-learning-alert" role="alert"><AlertCircle size={17} /><span>{error}</span></div>}
    <div className="kos-learning-shell">
      <aside className="kos-learning-areas" aria-label="Learning Areas"><div className="kos-learning-areas__heading"><strong>Learning Areas</strong><span>{areas.length}</span></div>
        {areas.length > 0 ? <><label className="kos-learning-area-select"><span>Chọn vùng kiến thức</span><select value={selectedCollectionId ?? ''}
          onChange={event => { const next = areas.find(area => area.collectionId === Number(event.target.value)); if (next) void selectArea(next) }}>
          {areas.map(area => <option key={area.collectionId} value={area.collectionId}>{area.title}</option>)}</select></label>
          <div className="kos-learning-area-list">{areas.map(area => <button key={area.collectionId} type="button"
            aria-current={area.collectionId === selectedCollectionId ? 'page' : undefined} className={area.collectionId === selectedCollectionId ? 'is-active' : ''}
            onClick={() => void selectArea(area)}><span>{area.title}</span><small>{area.sourceCount} nguồn{area.reviewNeededCount > 0 ? `, ${area.reviewNeededCount} cần ôn` : ''}</small></button>)}</div></>
          : <div className="kos-learning-areas__empty"><FolderOpen size={20} /><p>Chưa có Collection.</p><Link to="/library">Tạo trong Library</Link></div>}
        {legacyTopics.length > 0 && <div className="kos-learning-legacy-link"><button type="button" className={view === 'LEGACY' ? 'is-active' : ''}
          onClick={() => setView('LEGACY')}>Legacy Topics <span>{legacyTopics.length}</span></button></div>}
      </aside>
      <main className="kos-learning-main">{view === 'LEGACY'
        ? <LegacyTopics topics={legacyTopics} detail={legacyDetail} onSelect={async topic => { setLegacyDetail(await getStudyTopicDetail(topic.id)); setSearchParams({ topicId: String(topic.id) }) }} />
        : selectedArea ? <LearningAreaWorkspace area={selectedArea} detail={detail} generationBusy={generationBusy} view={view}
            onViewChange={next => { setView(next); if (next === 'SOURCES' && !sourceMap) void loadSourceMap() }} onBuild={buildOrRefresh}
            selectedModule={selectedModule} selectedConcept={selectedConcept}
            onSelectModule={module => { setSelectedModuleId(module.id); setSelectedConceptId(null); setQuiz(null); setQuizResult(null) }}
            onSelectConcept={concept => { setSelectedConceptId(concept.id); setQuiz(null); setQuizResult(null) }} onStatus={setConceptStatus}
            onRecall={startRecall} quiz={quiz} answers={answers} setAnswers={setAnswers} quizResult={quizResult} quizBusy={quizBusy}
            onSubmitRecall={submitRecall} sourceSelection={sourceSelection} setSourceSelection={setSourceSelection} sourceMap={sourceMap} onLoadSourceMap={loadSourceMap} />
          : <NoCollections />}</main>
    </div>
  </section>
}

function LearningAreaWorkspace(props: {
  area: LearningArea; detail: LearningAreaDetail | null; generationBusy: boolean; view: FocusView; onViewChange: (view: FocusView) => void
  onBuild: () => void; selectedModule: LearningModule | null; selectedConcept: TopicConcept | null; onSelectModule: (module: LearningModule) => void
  onSelectConcept: (concept: TopicConcept) => void; onStatus: (concept: TopicConcept, status: TopicConcept['studyStatus']) => void
  onRecall: (concept: TopicConcept) => void; quiz: QuizAttemptResponse | null; answers: Record<number, number>
  setAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>; quizResult: SubmitQuizAnswersResponse | null; quizBusy: boolean
  onSubmitRecall: () => void; sourceSelection: number[]; setSourceSelection: React.Dispatch<React.SetStateAction<number[]>>
  sourceMap: LearningAreaSourceMap | null; onLoadSourceMap: () => void
}) {
  const area = props.detail?.area ?? props.area
  const hasCurrentPath = area.currentVersion > 0 && Boolean(props.detail?.modules.length)
  const needsRefresh = area.refreshStatus === 'NEW_KNOWLEDGE_AVAILABLE' || (area.refreshStatus === 'FAILED' && hasCurrentPath)
  return <>
    <header className="kos-learning-area-header"><div><span>Learning Area</span><h2>{area.title}</h2>
      <p>{area.sourceCount} nguồn, {area.moduleCount} module, {area.conceptCount} khái niệm</p></div><div className="kos-learning-area-actions">
      {hasCurrentPath && <button type="button" className="kos-button kos-button--primary" onClick={() => {
        const module = props.detail?.modules.find(item => item.reviewNeededCount > 0 || item.checkedCount < item.conceptCount) ?? props.detail?.modules[0]
        if (module) props.onSelectModule(module)
      }}><ArrowRight size={16} /> Tiếp tục học</button>}
      {(!hasCurrentPath || needsRefresh) && <button type="button" className={hasCurrentPath ? 'kos-button' : 'kos-button kos-button--primary'}
        disabled={props.generationBusy || area.sourceCount === 0} onClick={props.onBuild}>{props.generationBusy
          ? <Loader2 size={16} className="kos-spin-fast" /> : <Route size={16} />}{hasCurrentPath ? 'Làm mới lộ trình' : 'Xây lộ trình'}</button>}
    </div></header>
    <div className="kos-learning-statuses" aria-label="Tóm tắt mastery"><StatusSummary status="CHECKED" value={area.checkedCount} />
      <StatusSummary status="REVIEW_NEEDED" value={area.reviewNeededCount} /><StatusSummary status="LEARNING" value={area.learningCount} />
      <StatusSummary status="NOT_STARTED" value={area.notStartedCount} /></div>
    {area.refreshStatus === 'NEW_KNOWLEDGE_AVAILABLE' && <div className="kos-learning-notice" aria-live="polite"><RefreshCw size={17} />
      <div><strong>Có kiến thức mới</strong><span>{area.newSourceCount || 1} nguồn mới có thể mở rộng lộ trình. Mastery hiện tại chưa thay đổi.</span></div></div>}
    {area.refreshStatus === 'FAILED' && <div className="kos-learning-notice is-error" role="alert"><AlertCircle size={17} />
      <div><strong>Chưa thể cập nhật lộ trình</strong><span>{area.generationFailure || 'Lộ trình đang dùng vẫn còn nguyên.'}</span></div></div>}
    <nav className="kos-learning-view-tabs" aria-label="Chế độ Focus"><button type="button" className={props.view === 'PATH' ? 'is-active' : ''}
      onClick={() => props.onViewChange('PATH')}><BookOpen size={15} /> Full Collection</button><button type="button"
      className={props.view === 'SOURCES' ? 'is-active' : ''} onClick={() => props.onViewChange('SOURCES')}><Network size={15} /> Selected Sources</button></nav>
    {props.view === 'SOURCES' ? <SelectedSources detail={props.detail} selection={props.sourceSelection} setSelection={props.setSourceSelection}
      map={props.sourceMap} onLoad={props.onLoadSourceMap} /> : !hasCurrentPath ? <PathEmpty area={area} busy={props.generationBusy} onBuild={props.onBuild} />
      : <div className="kos-learning-path-layout"><Pathway modules={props.detail!.modules} selectedId={props.selectedModule?.id ?? null} onSelect={props.onSelectModule} />
        <ModuleDetail module={props.selectedModule} concept={props.selectedConcept} onConcept={props.onSelectConcept} onStatus={props.onStatus}
          onRecall={props.onRecall} quiz={props.quiz} answers={props.answers} setAnswers={props.setAnswers} quizResult={props.quizResult}
          quizBusy={props.quizBusy} onSubmit={props.onSubmitRecall} /></div>}
  </>
}

function Pathway({ modules, selectedId, onSelect }: { modules: LearningModule[]; selectedId: number | null; onSelect: (module: LearningModule) => void }) {
  return <section className="kos-learning-path" aria-label="Lộ trình học">{modules.map((module, index) => <button key={module.id} type="button"
    aria-current={selectedId === module.id ? 'step' : undefined} className={selectedId === module.id ? 'is-active' : ''} onClick={() => onSelect(module)}>
    <span className="kos-learning-path__number">{String(index + 1).padStart(2, '0')}</span><span className="kos-learning-path__body"><small>{stageLabels[module.stage]}</small>
      <strong>{module.title}</strong><em>{module.conceptCount} khái niệm, {module.checkedCount} đã hiểu{module.reviewNeededCount ? `, ${module.reviewNeededCount} cần ôn` : ''}</em></span>
    <ArrowRight size={16} /></button>)}</section>
}

function ModuleDetail(props: { module: LearningModule | null; concept: TopicConcept | null; onConcept: (concept: TopicConcept) => void
  onStatus: (concept: TopicConcept, status: TopicConcept['studyStatus']) => void; onRecall: (concept: TopicConcept) => void
  quiz: QuizAttemptResponse | null; answers: Record<number, number>; setAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>
  quizResult: SubmitQuizAnswersResponse | null; quizBusy: boolean; onSubmit: () => void }) {
  if (!props.module) return <div className="kos-learning-detail-empty"><BookOpen size={24} /><p>Chọn một module để xem mục tiêu và khái niệm.</p></div>
  return <section className="kos-learning-module-detail"><header><span>{stageLabels[props.module.stage]}</span><h3>{props.module.title}</h3><p>{props.module.objective}</p></header>
    <div className="kos-learning-module-sources"><div><strong>Nguồn chính</strong>{props.module.primaryResources.map(source => <Link key={source.id} to={`/library/${source.id}`}>{source.title}</Link>)}</div>
      {props.module.supportingResources.length > 0 && <div><strong>Nguồn hỗ trợ</strong>{props.module.supportingResources.map(source => <Link key={source.id} to={`/library/${source.id}`}>{source.title}</Link>)}</div>}</div>
    <div className="kos-learning-concepts">{props.module.concepts.map(concept => <button key={concept.id} type="button" className={props.concept?.id === concept.id ? 'is-active' : ''}
      onClick={() => props.onConcept(concept)}><StatusIcon status={concept.studyStatus} /><span><strong>{concept.title}</strong><small>{statusMeta[concept.studyStatus].label}</small></span><ChevronDown size={15} /></button>)}</div>
    {props.concept && <ConceptDetail concept={props.concept} onStatus={props.onStatus} onRecall={props.onRecall} quiz={props.quiz} answers={props.answers}
      setAnswers={props.setAnswers} quizResult={props.quizResult} quizBusy={props.quizBusy} onSubmit={props.onSubmit} />}
  </section>
}

function ConceptDetail(props: { concept: TopicConcept; onStatus: (concept: TopicConcept, status: TopicConcept['studyStatus']) => void
  onRecall: (concept: TopicConcept) => void; quiz: QuizAttemptResponse | null; answers: Record<number, number>
  setAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>; quizResult: SubmitQuizAnswersResponse | null; quizBusy: boolean; onSubmit: () => void }) {
  return <article className="kos-learning-concept-detail"><div className="kos-learning-concept-detail__heading"><div><StatusIcon status={props.concept.studyStatus} />
    <span>{statusMeta[props.concept.studyStatus].label}</span></div><h4>{props.concept.title}</h4></div><p>{props.concept.summary}</p>
    {props.concept.whyItMatters && <div className="kos-learning-why"><strong>Vì sao cần học</strong><span>{props.concept.whyItMatters}</span></div>}
    <div className="kos-learning-concept-actions">{props.concept.studyStatus === 'NOT_STARTED' && <button type="button" className="kos-button"
      onClick={() => props.onStatus(props.concept, 'LEARNING')}>Bắt đầu học</button>}<button type="button" className="kos-button kos-button--primary"
      disabled={props.quizBusy} onClick={() => props.onRecall(props.concept)}>{props.quizBusy ? <Loader2 size={15} className="kos-spin-fast" /> : <RotateCcw size={15} />} Kiểm tra hiểu biết</button></div>
    <details className="kos-learning-evidence"><summary>Bằng chứng đã xác minh ({props.concept.sources.length})</summary><div>{props.concept.sources.map(source => <blockquote key={source.chunkId}>
      <Link to={`/library/${source.resourceId}`}>{source.resourceTitle}</Link><p>{source.snippet}</p></blockquote>)}</div></details>
    {props.quiz && <RecallPanel quiz={props.quiz} answers={props.answers} setAnswers={props.setAnswers} result={props.quizResult} busy={props.quizBusy} onSubmit={props.onSubmit} />}
  </article>
}

function RecallPanel({ quiz, answers, setAnswers, result, busy, onSubmit }: { quiz: QuizAttemptResponse; answers: Record<number, number>
  setAnswers: React.Dispatch<React.SetStateAction<Record<number, number>>>; result: SubmitQuizAnswersResponse | null; busy: boolean; onSubmit: () => void }) {
  return <section className="kos-learning-recall" aria-live="polite"><h4>Recall</h4>{quiz.questions.map((question, index) => {
    const revealedQuestion = result?.results.find(item => item.id === question.id) ?? question
    return <fieldset key={question.id}><legend>{index + 1}. {question.question}</legend>
      {question.options.map((option, optionIndex) => <label key={option}><input type="radio" name={`question-${question.id}`} disabled={Boolean(result)}
        checked={answers[question.id] === optionIndex} onChange={() => setAnswers(previous => ({ ...previous, [question.id]: optionIndex }))} /><span>{option}</span></label>)}
      {result && <div className="kos-learning-recall__explanation"><strong>{revealedQuestion.correctOption === answers[question.id] ? 'Chính xác' : 'Cần ôn lại'}</strong>
        <p>{revealedQuestion.explanation}</p>{revealedQuestion.sourceSnippet && <small>Nguồn: {revealedQuestion.sourceSnippet}</small>}</div>}</fieldset>
  })}
    {!result ? <button type="button" className="kos-button kos-button--primary" disabled={busy || Object.keys(answers).length < quiz.questions.length} onClick={onSubmit}>Nộp Recall</button>
      : <p className="kos-learning-recall__score">Kết quả: {result.scoreCorrect}/{result.totalQuestions}</p>}</section>
}

function SelectedSources({ detail, selection, setSelection, map, onLoad }: { detail: LearningAreaDetail | null; selection: number[]
  setSelection: React.Dispatch<React.SetStateAction<number[]>>; map: LearningAreaSourceMap | null; onLoad: () => void }) {
  if (!detail) return <PathEmpty area={null} busy={false} onBuild={() => undefined} />
  return <section className="kos-learning-source-map"><header><h3>Selected Sources Map</h3><p>Chọn tối đa 12 nguồn để xem quan hệ Collection và bằng chứng đã lưu. Không có cạnh suy đoán từ vị trí hiển thị.</p></header>
    <div className="kos-learning-source-picker">{detail.resources.map(resource => <label key={resource.id}><input type="checkbox" checked={selection.includes(resource.id)}
      disabled={!selection.includes(resource.id) && selection.length >= 12} onChange={event => setSelection(previous => event.target.checked ? [...previous, resource.id] : previous.filter(id => id !== resource.id))} />
      <span>{resource.title}</span></label>)}</div><button type="button" className="kos-button" onClick={onLoad}><Network size={15} /> Cập nhật bản đồ</button>
    {map && <div className="kos-learning-source-relations">{map.edges.length ? map.edges.map((edge, index) => { const source = map.nodes.find(node => node.id === edge.source)?.label ?? edge.source
      const target = map.nodes.find(node => node.id === edge.target)?.label ?? edge.target
      return <div key={`${edge.source}-${edge.target}-${index}`}><strong>{source}</strong><ArrowRight size={14} /><strong>{target}</strong><small>{edge.reason}</small></div> })
      : <p>Chưa có quan hệ bằng chứng trong lựa chọn này.</p>}</div>}</section>
}

function PathEmpty({ area, busy, onBuild }: { area: LearningArea | null; busy: boolean; onBuild: () => void }) {
  const noSources = area?.sourceCount === 0
  return <div className="kos-learning-path-empty"><BookOpen size={28} /><h3>{noSources ? 'Collection chưa có nguồn READY' : 'Chưa có lộ trình học'}</h3>
    <p>{noSources ? 'Đưa tài liệu vào Collection và chờ xử lý hoàn tất. Focus chỉ kế thừa nguồn READY.' : 'Xây một lộ trình có thứ tự từ Document Understanding và bằng chứng đã xác minh.'}</p>
    {noSources ? <Link to="/library" className="kos-button"><Library size={15} /> Mở Library</Link> : <button type="button" className="kos-button kos-button--primary" disabled={busy} onClick={onBuild}>
      {busy ? <Loader2 size={15} className="kos-spin-fast" /> : <Route size={15} />} Xây lộ trình</button>}</div>
}

function LegacyTopics({ topics, detail, onSelect }: { topics: StudyTopic[]; detail: StudyTopicDetail | null; onSelect: (topic: StudyTopic) => void }) {
  return <section className="kos-learning-legacy"><header><span>Compatibility</span><h2>Legacy Topics</h2><p>Lịch sử mastery và Recall cũ vẫn được giữ. Lộ trình mới được xây từ Collections.</p></header>
    <div className="kos-learning-legacy__layout"><nav>{topics.map(topic => <button key={topic.id} type="button" className={detail?.id === topic.id ? 'is-active' : ''}
      onClick={() => onSelect(topic)}><strong>{topic.title}</strong><small>{topic.conceptCount} khái niệm</small></button>)}</nav><div>{detail ? <><h3>{detail.title}</h3><p>{detail.goal}</p>
      {detail.concepts.map(concept => <div className="kos-learning-legacy-concept" key={concept.id}><StatusIcon status={concept.studyStatus} /><span><strong>{concept.title}</strong>
        <small>{statusMeta[concept.studyStatus].label}</small></span></div>)}</> : <p>Chọn một Legacy Topic để xem lịch sử.</p>}</div></div></section>
}

function StatusSummary({ status, value }: { status: TopicConcept['studyStatus']; value: number }) {
  return <div className={`is-${status.toLowerCase()}`}><StatusIcon status={status} /><span><strong>{value}</strong>{statusMeta[status].label}</span></div>
}
function StatusIcon({ status }: { status: TopicConcept['studyStatus'] }) { const Icon = statusMeta[status].icon; return <Icon size={16} aria-hidden="true" /> }
function FocusLoading() { return <section className="kos-page kos-learning-focus"><div className="kos-learning-loading" aria-live="polite"><Loader2 size={24} className="kos-spin-fast" />
  <h2>Đang mở Learning Areas</h2><span>Đọc lộ trình đã lưu, không gọi Gemini.</span></div></section> }
function NoCollections() { return <div className="kos-learning-path-empty"><FolderOpen size={30} /><h2>Tạo Collection đầu tiên</h2><p>Learning Areas được kế thừa trực tiếp từ Collections trong Library.</p>
  <Link to="/library" className="kos-button kos-button--primary">Mở Library</Link></div> }
