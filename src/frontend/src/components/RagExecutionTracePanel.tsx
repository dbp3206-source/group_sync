import { BrainCircuit, CheckCircle2, ChevronDown, ChevronUp, CircleAlert, Clock3, Loader2, Radio } from 'lucide-react'
import { useState } from 'react'
import type { AskTraceEvent } from '../api/knowledge'

interface Props {
  events: AskTraceEvent[]
  loading?: boolean
  scope?: string
  citationCount?: number
}

const examples: Record<string, { beginner: string; technical: string }> = {
  PLAN_READY: { beginner: 'KnowledgeOS đã chọn cách tìm phù hợp với câu hỏi.', technical: 'Planner quyết định QueryMode; không phải suy nghĩ ẩn của mô hình.' },
  RRF_COMPLETE: { beginner: 'Hai kiểu kết quả tìm kiếm đã được gộp lại để giảm bỏ sót.', technical: 'Reciprocal Rank Fusion kết hợp thứ hạng semantic và lexical.' },
  CITATIONS_VERIFIED: { beginner: 'Các số [1], [2] đã được đối chiếu với đoạn nguồn thật.', technical: 'Citation markers được kiểm tra và lưu cùng DocumentChunk.' },
}

export default function RagExecutionTracePanel({ events, loading = false, scope = 'LIBRARY', citationCount = 0 }: Props) {
  const [showJson, setShowJson] = useState(false)
  const latest = events.at(-1)
  const renderedEvents: AskTraceEvent[] = events.length ? events : [{ attemptId: 0, sequence: 0, stage: 'PLAN_READY', status: 'RUNNING', occurredAt: new Date().toISOString(), durationMs: 0, beginnerMessage: 'Khi bạn hỏi, KnowledgeOS sẽ hiển thị từng bước đã thực sự chạy.', technicalSummary: 'backend-driven trace; no chain-of-thought', technicalDetails: { mode: null, operation: null, semanticCandidates: null, lexicalCandidates: null, totalCandidates: null, selectedChildren: null, parentsUsed: null, charactersUsed: null, maxCharactersBudget: null, citationsVerified: null, model: null, failureCategory: null } }]

  return (
    <section className="kos-reasoning-card" aria-label="RAG execution trace">
      <div className="kos-box-header"><div className="kos-box-title"><Radio size={15} /><span>Live retrieval trace</span></div><div className="kos-box-timer"><Clock3 size={13} /><span>{loading ? `${latest?.durationMs ?? 0}ms` : latest ? `${latest.durationMs}ms` : 'Ready'}</span></div></div>
      <div className="kos-trace-intro">System stages only. Không hiển thị chain-of-thought.</div>
      <div className="kos-reasoning-scroll-content"><div className="kos-reasoning-timeline">
        {renderedEvents.map(event => {
          const isFailed = event.status === 'FAILED'; const isLast = event === renderedEvents.at(-1); const example = examples[event.stage]
          return <div className={`kos-reasoning-step ${isFailed ? 'is-failed' : event.status === 'COMPLETE' ? 'is-done' : 'is-live'}`} key={`${event.attemptId ?? 'idle'}-${event.sequence}`}>
            <div className="kos-step-indicator">{isFailed ? <CircleAlert size={16} /> : event.status === 'RUNNING' && isLast ? <Loader2 size={16} className="kos-btn-spin" /> : <CheckCircle2 size={16} />}{!isLast && <span className="kos-step-line" />}</div>
            <div className="kos-step-content"><div className="kos-step-header"><span className="kos-step-title">{event.stage.replaceAll('_', ' ')}</span><span className="kos-step-badge">{event.technicalDetails.mode || event.status}</span></div><p className="kos-step-desc">{event.beginnerMessage}</p>{example && <p className="kos-step-tech"><span>{example.beginner}</span> {example.technical}</p>}{(event.technicalDetails.semanticCandidates !== null || event.technicalDetails.citationsVerified !== null || event.technicalDetails.failureCategory) && <p className="kos-step-tech">{event.technicalDetails.semanticCandidates !== null && `semantic ${event.technicalDetails.semanticCandidates}`}{event.technicalDetails.lexicalCandidates !== null && ` • lexical ${event.technicalDetails.lexicalCandidates}`}{event.technicalDetails.citationsVerified !== null && ` • citations ${event.technicalDetails.citationsVerified}`}{event.technicalDetails.failureCategory && ` • ${event.technicalDetails.failureCategory}`}</p>}</div>
          </div>
        })}
      </div></div>
      {events.length > 0 && <div className="kos-trace-advanced"><button type="button" onClick={() => setShowJson(value => !value)}><span>{showJson ? <ChevronUp size={13} /> : <ChevronDown size={13} />} Advanced</span><small>raw JSON</small></button>{showJson && <pre>{JSON.stringify(events, null, 2)}</pre>}</div>}
      <div className="kos-box-footer"><BrainCircuit size={13} /><span>Scope: {scope === 'LIBRARY' ? 'Library' : scope === 'COLLECTION' ? 'Collection' : 'Selected sources'}{citationCount ? ` • ${citationCount} citations` : ''}</span></div>
    </section>
  )
}
