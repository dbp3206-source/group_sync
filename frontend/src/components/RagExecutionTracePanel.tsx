import { BrainCircuit, ChevronDown, ChevronUp, Clock, Sparkles } from 'lucide-react'
import { useState } from 'react'
import type { RagExecutionTrace } from '../api/knowledge'

interface RagExecutionTracePanelProps {
  trace: RagExecutionTrace | null
  elapsedSec?: string
  loading?: boolean
  scope?: string
  citationCount?: number
}

export default function RagExecutionTracePanel({
  trace,
  elapsedSec,
  loading = false,
  scope = 'LIBRARY',
  citationCount = 0,
}: RagExecutionTracePanelProps) {
  const [showJson, setShowJson] = useState(false)

  return (
    <div className="kos-reasoning-card">
      <div className="kos-box-header">
        <div className="kos-box-title">
          <Sparkles size={16} />
          <span>RAG v2 Execution Trace</span>
        </div>
        <div className="kos-box-timer">
          <Clock size={13} />
          <span>{loading ? `${elapsedSec || '0.0'}s` : trace ? `${trace.durationMs}ms` : 'Ready'}</span>
        </div>
      </div>

      <div className="kos-reasoning-scroll-content">
        {trace ? (
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
                  <span className="kos-step-badge">{trace.mode}</span>
                </div>
                <p className="kos-step-desc">
                  Operation: {trace.operation}
                  {trace.planner?.semanticQuery ? ` • Query: "${trace.planner.semanticQuery}"` : ''}
                </p>
              </div>
            </div>

            {/* STRUCTURED PATH */}
            {trace.mode === 'STRUCTURED' ? (
              <>
                {trace.filter && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">2</span>
                      <span className="kos-step-line" />
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">Metadata Scope</span>
                        <span className="kos-step-badge">{trace.filter.scope || 'LIBRARY'}</span>
                      </div>
                      <p className="kos-step-desc">
                        {trace.filter.resourceType ? `Type: ${trace.filter.resourceType} • ` : ''}
                        {trace.filter.favorite !== null && trace.filter.favorite !== undefined ? `Favorite: ${trace.filter.favorite} • ` : ''}
                        Relational metadata evaluated.
                      </p>
                    </div>
                  </div>
                )}
                <div className="kos-reasoning-step is-done">
                  <div className="kos-step-indicator">
                    <span className="kos-step-dot">{trace.filter ? 3 : 2}</span>
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
              /* SEMANTIC / HYBRID / FILTERED_HYBRID PATH */
              <>
                {/* Filter Stage (if present) */}
                {trace.filter && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">2</span>
                      <span className="kos-step-line" />
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">Metadata Filters</span>
                        <span className="kos-step-badge">{trace.filter.scope || 'LIBRARY'}</span>
                      </div>
                      <p className="kos-step-desc">
                        {trace.filter.resourceType ? `Type: ${trace.filter.resourceType} • ` : ''}
                        {trace.filter.collectionCount ? `${trace.filter.collectionCount} collection(s) • ` : ''}
                        {trace.filter.tagCount ? `${trace.filter.tagCount} tag(s) • ` : ''}
                        Applied in SQL WHERE before vector distance calculation.
                      </p>
                    </div>
                  </div>
                )}

                {/* Retrieval Stage */}
                {trace.retrieval && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">{trace.filter ? 3 : 2}</span>
                      {(trace.fusion || trace.parentChild || trace.generation) && <span className="kos-step-line" />}
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">
                          {trace.mode === 'SEMANTIC' ? 'Semantic Retrieval' : 'Hybrid Retrieval'}
                        </span>
                        <span className="kos-step-badge">
                          {trace.mode === 'SEMANTIC' ? 'pgvector Cosine' : 'pgvector + FTS'}
                        </span>
                      </div>
                      <p className="kos-step-desc">
                        Semantic candidates: {trace.retrieval.semanticCandidates}
                        {trace.mode !== 'SEMANTIC' ? ` • Lexical candidates: ${trace.retrieval.lexicalCandidates}` : ''}
                      </p>
                    </div>
                  </div>
                )}

                {/* Fusion Stage (Only if executed) */}
                {trace.fusion && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">•</span>
                      {(trace.parentChild || trace.generation) && <span className="kos-step-line" />}
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">RRF Fusion (k={trace.fusion.rrfK})</span>
                        <span className="kos-step-badge">{trace.fusion.selectedChildren} child chunks</span>
                      </div>
                      <p className="kos-step-desc">
                        {trace.fusion.inputCandidates} combined candidates fused via Reciprocal Rank Fusion.
                      </p>
                    </div>
                  </div>
                )}

                {/* Parent-Child Expansion Stage */}
                {trace.parentChild && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">•</span>
                      {trace.generation && <span className="kos-step-line" />}
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">Parent Context Expansion</span>
                        <span className="kos-step-badge">{trace.parentChild.uniqueParentsFound} parents</span>
                      </div>
                      <p className="kos-step-desc">
                        {trace.parentChild.childChunksRetrieved} child chunks expanded to {trace.parentChild.uniqueParentsFound} deduplicated parent chunks
                        {trace.contextBudget ? ` (${trace.contextBudget.charactersUsed} / ${trace.contextBudget.maxCharactersBudget} chars)` : ''}.
                      </p>
                    </div>
                  </div>
                )}

                {/* Generation Stage */}
                {trace.generation && (
                  <div className="kos-reasoning-step is-done">
                    <div className="kos-step-indicator">
                      <span className="kos-step-dot">•</span>
                    </div>
                    <div className="kos-step-content">
                      <div className="kos-step-header">
                        <span className="kos-step-title">Grounded Generation</span>
                        <span className="kos-step-badge">{trace.generation.model}</span>
                      </div>
                      <p className="kos-step-desc">
                        {trace.generation.promptChunksCount} chunks passed to prompt • {trace.generation.verifiedCitationsCount} citations verified.
                      </p>
                    </div>
                  </div>
                )}
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
                  <span className="kos-step-title">Query Planner</span>
                  <span className="kos-step-badge">Automatic Routing</span>
                </div>
                <p className="kos-step-desc">Tự động phân tích câu hỏi và phân luồng STRUCTURED vs SEMANTIC vs HYBRID.</p>
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

      {trace && (
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
            onClick={() => setShowJson(v => !v)}
          >
            {showJson ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
            {showJson ? 'Ẩn chi tiết kỹ thuật' : 'Xem JSON Trace kỹ thuật'}
          </button>
          {showJson && (
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
              {JSON.stringify(trace, null, 2)}
            </pre>
          )}
        </div>
      )}

      <div className="kos-box-footer">
        <BrainCircuit size={13} />
        <span>
          Scope: {scope === 'LIBRARY' ? 'Entire library' : scope === 'COLLECTION' ? 'Collection' : 'Selected resources'}
          {citationCount > 0 ? ` • ${citationCount} citations` : ''}
        </span>
      </div>
    </div>
  )
}
