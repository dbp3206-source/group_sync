import { Layers, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { type KnowledgeCollection, type KnowledgeTag, type Resource } from '../api/knowledge'

interface KnowledgeGraphViewProps {
  resources: Resource[]
  collections: KnowledgeCollection[]
  tags?: KnowledgeTag[]
  totalItems?: number
}

const TYPE_COLORS: Record<string, { bg: string; border: string; text: string; glow: string }> = {
  PDF: { bg: '#fee2e2', border: '#ef4444', text: '#991b1b', glow: 'rgba(239, 68, 68, 0.4)' },
  DOCX: { bg: '#e0f2fe', border: '#0284c7', text: '#075985', glow: 'rgba(2, 132, 199, 0.4)' },
  MARKDOWN: { bg: '#fef3c7', border: '#d97706', text: '#92400e', glow: 'rgba(217, 119, 6, 0.4)' },
  TXT: { bg: '#f3e8ff', border: '#9333ea', text: '#6b21a8', glow: 'rgba(147, 51, 234, 0.4)' },
  NOTE: { bg: '#dcfce7', border: '#16a34a', text: '#166534', glow: 'rgba(22, 163, 74, 0.4)' },
}

const DEFAULT_COLOR = { bg: '#f1f5f9', border: '#64748b', text: '#334155', glow: 'rgba(100, 116, 139, 0.3)' }

export default function KnowledgeGraphView({ resources, collections, tags = [], totalItems }: KnowledgeGraphViewProps) {
  const [hoveredNode, setHoveredNode] = useState<number | null>(null)
  const [selectedType, setSelectedType] = useState<string>('ALL')

  const filteredResources = selectedType === 'ALL'
    ? resources
    : resources.filter(r => r.resourceType.toUpperCase() === selectedType.toUpperCase())

  // Dynamic layout calculation for SVG Canvas (860 x 520)
  const width = 860
  const height = 520
  const centerX = width / 2
  const centerY = height / 2

  // Hub Collections nodes
  const collectionNodes = collections.map((col, idx) => {
    const angle = (idx / (collections.length || 1)) * 2 * Math.PI - Math.PI / 2
    const radius = 130
    return {
      id: `col-${col.id}`,
      rawId: col.id,
      name: col.name,
      x: centerX + Math.cos(angle) * radius,
      y: centerY + Math.sin(angle) * radius,
      type: 'COLLECTION',
    }
  })

  // Resource nodes positioned around the center or orbiting their collection
  const resourceNodes = filteredResources.map((res, idx) => {
    const total = filteredResources.length || 1
    const angle = (idx / total) * 2 * Math.PI
    // Vary radius slightly for an organic constellation look
    const radius = 210 + (idx % 3) * 35
    const x = centerX + Math.cos(angle) * radius
    const y = centerY + Math.sin(angle) * radius

    const colors = TYPE_COLORS[res.resourceType.toUpperCase()] || DEFAULT_COLOR

    return {
      id: `res-${res.id}`,
      rawId: res.id,
      title: res.title,
      type: res.resourceType,
      x,
      y,
      colors,
    }
  })

  // Compute connections (links) from center to collections and collections to resources
  const links: { x1: number; y1: number; x2: number; y2: number; key: string; highlighted: boolean }[] = []

  // Links from center hub to collections
  collectionNodes.forEach(col => {
    links.push({
      x1: centerX,
      y1: centerY,
      x2: col.x,
      y2: col.y,
      key: `center-${col.id}`,
      highlighted: false,
    })
  })

  // Links from collections or center to resources
  resourceNodes.forEach(res => {
    // Connect directly to KnowledgeOS core hub
    const isConnected = hoveredNode === res.rawId

    links.push({
      x1: centerX,
      y1: centerY,
      x2: res.x,
      y2: res.y,
      key: `link-${res.id}`,
      highlighted: isConnected,
    })
  })

  return (
    <div className="kos-graph-container">
      {totalItems !== undefined && totalItems > resources.length && (
        <div style={{ padding: '0.45rem 0.85rem', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--kos-border, rgba(255,255,255,0.08))', borderRadius: '6px', fontSize: '0.75rem', color: 'var(--kos-text-muted, #94a3b8)', marginBottom: '0.75rem' }}>
          <Sparkles size={13} style={{ verticalAlign: 'middle', marginRight: '0.35rem', color: 'var(--kos-accent, #60a5fa)' }} />
          <span>Showing relationships among {resources.length} loaded resources ({totalItems} total in library). Load more in the grid view to expand this graph.</span>
        </div>
      )}
      {/* Graph Toolbar */}
      <div className="kos-graph-toolbar">
        <div className="kos-graph-legend">
          <span className="kos-legend-title">
            <Layers size={14} /> Phân loại Tri thức:
          </span>
          <button
            type="button"
            className={`kos-legend-pill ${selectedType === 'ALL' ? 'is-active' : ''}`}
            onClick={() => setSelectedType('ALL')}
          >
            Tất cả ({resources.length})
          </button>
          {['PDF', 'DOCX', 'MARKDOWN', 'NOTE'].map(t => {
            const count = resources.filter(r => r.resourceType.toUpperCase() === t).length
            if (!count) return null
            return (
              <button
                key={t}
                type="button"
                className={`kos-legend-pill ${selectedType === t ? 'is-active' : ''}`}
                onClick={() => setSelectedType(t)}
                style={{
                  borderColor: selectedType === t ? TYPE_COLORS[t]?.border : undefined,
                  color: selectedType === t ? TYPE_COLORS[t]?.border : undefined,
                }}
              >
                <span className="kos-legend-dot" style={{ background: TYPE_COLORS[t]?.border }} />
                {t} ({count})
              </button>
            )
          })}
        </div>

        <div className="kos-graph-stats">
          <Sparkles size={14} className="kos-text-amber" />
          <span>
            {filteredResources.length} Node tri thức • {collections.length} Cụm chủ đề • {tags.length} Thẻ
          </span>
        </div>
      </div>

      {/* SVG Canvas Area */}
      <div className="kos-graph-canvas-wrapper">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="kos-graph-svg"
          preserveAspectRatio="xMidYMid meet"
        >
          <defs>
            <radialGradient id="hubGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="var(--kos-blue)" stopOpacity="0.25" />
              <stop offset="100%" stopColor="var(--kos-blue)" stopOpacity="0" />
            </radialGradient>
          </defs>

          {/* Background Central Glow */}
          <circle cx={centerX} cy={centerY} r={180} fill="url(#hubGlow)" />

          {/* Connection Lines */}
          <g className="kos-graph-links">
            {links.map(link => (
              <line
                key={link.key}
                x1={link.x1}
                y1={link.y1}
                x2={link.x2}
                y2={link.y2}
                className={`kos-graph-line ${link.highlighted ? 'is-highlighted' : ''}`}
              />
            ))}
          </g>

          {/* Central KnowledgeOS Core Hub Node */}
          <g className="kos-hub-node">
            <circle
              cx={centerX}
              cy={centerY}
              r={36}
              className="kos-hub-circle"
            />
            <text
              x={centerX}
              y={centerY - 5}
              textAnchor="middle"
              className="kos-hub-title"
            >
              KnowledgeOS
            </text>
            <text
              x={centerX}
              y={centerY + 12}
              textAnchor="middle"
              className="kos-hub-sub"
            >
              Core Library
            </text>
          </g>

          {/* Collection Hub Nodes */}
          {collectionNodes.map(col => (
            <g key={col.id} className="kos-col-node">
              <circle
                cx={col.x}
                cy={col.y}
                r={22}
                className="kos-col-circle"
              />
              <text
                x={col.x}
                y={col.y + 4}
                textAnchor="middle"
                className="kos-col-icon-text"
              >
                📁
              </text>
              <text
                x={col.x}
                y={col.y + 34}
                textAnchor="middle"
                className="kos-col-label"
              >
                {col.name.length > 14 ? `${col.name.slice(0, 12)}…` : col.name}
              </text>
            </g>
          ))}

          {/* Resource Leaf Nodes */}
          {resourceNodes.map(node => {
            const isHovered = hoveredNode === node.rawId
            const isDimmed = hoveredNode !== null && hoveredNode !== node.rawId

            return (
              <g
                key={node.id}
                className={`kos-resource-node ${isHovered ? 'is-hovered' : ''} ${isDimmed ? 'is-dimmed' : ''}`}
                onMouseEnter={() => setHoveredNode(node.rawId)}
                onMouseLeave={() => setHoveredNode(null)}
                style={{ cursor: 'pointer' }}
              >
                <Link to={`/library/${node.rawId}`}>
                  {/* Outer Pulsing Aura when hovered */}
                  {isHovered && (
                    <circle
                      cx={node.x}
                      cy={node.y}
                      r={26}
                      fill={node.colors.glow}
                      className="kos-node-aura"
                    />
                  )}

                  {/* Main Node Circle */}
                  <circle
                    cx={node.x}
                    cy={node.y}
                    r={18}
                    fill={node.colors.bg}
                    stroke={node.colors.border}
                    strokeWidth={isHovered ? 2.5 : 1.5}
                    className="kos-node-circle"
                  />

                  {/* Resource Type Tag / Initial inside Node */}
                  <text
                    x={node.x}
                    y={node.y + 4}
                    textAnchor="middle"
                    fill={node.colors.text}
                    className="kos-node-initial"
                  >
                    {node.type.slice(0, 3)}
                  </text>

                  {/* Node Label Below */}
                  <text
                    x={node.x}
                    y={node.y + 28}
                    textAnchor="middle"
                    className="kos-node-label"
                  >
                    {node.title.length > 16 ? `${node.title.slice(0, 14)}…` : node.title}
                  </text>
                </Link>
              </g>
            )
          })}
        </svg>

        {/* Floating Tooltip Card for Active Hovered Node */}
        {hoveredNode !== null && (() => {
          const res = resources.find(r => r.id === hoveredNode)
          if (!res) return null
          const colors = TYPE_COLORS[res.resourceType.toUpperCase()] || DEFAULT_COLOR

          return (
            <div className="kos-graph-tooltip" style={{ borderColor: colors.border }}>
              <div className="kos-tooltip-header">
                <span className="kos-tooltip-badge" style={{ background: colors.bg, color: colors.text }}>
                  {res.resourceType}
                </span>
                <span className="kos-tooltip-status">
                  {res.processingStatus === 'READY' ? '🟢 Sẵn sàng tra cứu' : res.processingStatus}
                </span>
              </div>
              <h4 className="kos-tooltip-title">{res.title}</h4>
              <p className="kos-tooltip-hint">
                👉 Nhấp chuột để mở không gian nghiên cứu tài liệu (Workspace)
              </p>
            </div>
          )
        })()}
      </div>
    </div>
  )
}
