import React from 'react'

interface MarkdownViewProps {
  content: string
  className?: string
  onCitationClick?: (citationNumber: number) => void
}

/**
 * Robust, lightweight Markdown and structured text renderer for KnowledgeOS.
 * Parses headings, numbered roadmaps, nested bullet lists, bold emphasis,
 * inline code, arrows (=> / ->), math notations ($...$), and citation badges ([1], [2]).
 */
export default function MarkdownView({ content, className = '', onCitationClick }: MarkdownViewProps) {
  if (!content) return null

  // Normalize newlines
  const lines = content.replace(/\r\n/g, '\n').split('\n')
  const elements: React.ReactNode[] = []

  let currentList: { type: 'ol' | 'ul'; items: string[] } | null = null
  let inCodeBlock = false
  let codeBlockLines: string[] = []

  function flushList() {
    if (!currentList) return
    const isOrdered = currentList.type === 'ol'
    const ListTag = isOrdered ? 'ol' : 'ul'
    elements.push(
      <ListTag key={`list-${elements.length}`} className={`kos-md-list kos-md-list--${currentList.type}`}>
        {currentList.items.map((itemText, idx) => (
          <li key={idx} className="kos-md-list-item">
            {renderInline(itemText, onCitationClick)}
          </li>
        ))}
      </ListTag>,
    )
    currentList = null
  }

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i]
    const trimmed = rawLine.trim()

    // Handle code blocks (```)
    if (trimmed.startsWith('```')) {
      if (inCodeBlock) {
        flushList()
        elements.push(
          <pre key={`code-${elements.length}`} className="kos-md-code-block">
            <code>{codeBlockLines.join('\n')}</code>
          </pre>,
        )
        codeBlockLines = []
        inCodeBlock = false
      } else {
        flushList()
        inCodeBlock = true
      }
      continue
    }

    if (inCodeBlock) {
      codeBlockLines.push(rawLine)
      continue
    }

    // Blank line -> flush current list & add spacer
    if (!trimmed) {
      flushList()
      continue
    }

    // Headings (###, ####, ##, #)
    if (trimmed.startsWith('#### ')) {
      flushList()
      elements.push(
        <h4 key={`h4-${elements.length}`} className="kos-md-h4">
          {renderInline(trimmed.slice(5), onCitationClick)}
        </h4>,
      )
      continue
    }
    if (trimmed.startsWith('### ')) {
      flushList()
      elements.push(
        <h3 key={`h3-${elements.length}`} className="kos-md-h3">
          {renderInline(trimmed.slice(4), onCitationClick)}
        </h3>,
      )
      continue
    }
    if (trimmed.startsWith('## ')) {
      flushList()
      elements.push(
        <h2 key={`h2-${elements.length}`} className="kos-md-h2">
          {renderInline(trimmed.slice(3), onCitationClick)}
        </h2>,
      )
      continue
    }
    if (trimmed.startsWith('# ')) {
      flushList()
      elements.push(
        <h2 key={`h1-${elements.length}`} className="kos-md-h2">
          {renderInline(trimmed.slice(2), onCitationClick)}
        </h2>,
      )
      continue
    }

    // Blockquote (> )
    if (trimmed.startsWith('> ')) {
      flushList()
      elements.push(
        <blockquote key={`quote-${elements.length}`} className="kos-md-quote">
          {renderInline(trimmed.slice(2), onCitationClick)}
        </blockquote>,
      )
      continue
    }

    // Numbered list (e.g. "1. ", "2. ", "10. ")
    const numMatch = trimmed.match(/^(\d+)\.\s+(.*)$/)
    if (numMatch) {
      if (!currentList || currentList.type !== 'ol') {
        flushList()
        currentList = { type: 'ol', items: [] }
      }
      currentList.items.push(numMatch[2])
      continue
    }

    // Bullet list (e.g. "- ", "* ", "• ")
    const bulletMatch = trimmed.match(/^[-*•]\s+(.*)$/)
    if (bulletMatch) {
      if (!currentList || currentList.type !== 'ul') {
        flushList()
        currentList = { type: 'ul', items: [] }
      }
      currentList.items.push(bulletMatch[1])
      continue
    }

    // Indented sub-bullet (e.g. "  - ", "    * ")
    const subBulletMatch = rawLine.match(/^\s{2,}[-*•]\s+(.*)$/)
    if (subBulletMatch && currentList) {
      currentList.items.push(`↳ ${subBulletMatch[1]}`)
      continue
    }

    // Normal paragraph
    flushList()
    elements.push(
      <p key={`p-${elements.length}`} className="kos-md-p">
        {renderInline(trimmed, onCitationClick)}
      </p>,
    )
  }

  flushList()

  return <div className={`kos-markdown-body ${className}`}>{elements}</div>
}

/**
 * Parses inline formatting:
 * - **bold** or __bold__
 * - *italic* or _italic_
 * - `code`
 * - [1], [2] citation badges
 * - => and -> reasoning arrows
 * - $math$ inline expressions
 */
function renderInline(text: string, onCitationClick?: (num: number) => void): React.ReactNode {
  if (!text) return null

  // Tokenize by regex matching: code, bold, italic, citation, arrow, math
  const regex = /(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__|(?<!\*)\*[^*]+\*(?!\*)|\[\d+(?:,\s*\d+)*\]|=>|->|==>|-->|\$[^$]+\$)/g
  const parts = text.split(regex)

  return parts.map((part, index) => {
    if (!part) return null

    // Inline code (`code`)
    if (part.startsWith('`') && part.endsWith('`') && part.length > 1) {
      return (
        <code key={index} className="kos-md-code-inline">
          {part.slice(1, -1)}
        </code>
      )
    }

    // Bold (**bold** or __bold__)
    if ((part.startsWith('**') && part.endsWith('**')) || (part.startsWith('__') && part.endsWith('__'))) {
      const inner = part.slice(2, -2)
      return (
        <strong key={index} className="kos-md-strong">
          {inner}
        </strong>
      )
    }

    // Italic (*italic* or _italic_)
    if ((part.startsWith('*') && part.endsWith('*')) || (part.startsWith('_') && part.endsWith('_'))) {
      const inner = part.slice(1, -1)
      return (
        <em key={index} className="kos-md-em">
          {inner}
        </em>
      )
    }

    // Citation badge ([1], [2], [1, 2])
    const citMatch = part.match(/^\[(\d+(?:,\s*\d+)*)\]$/)
    if (citMatch) {
      const nums = citMatch[1].split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
      return (
        <span key={index} className="kos-md-citation-group">
          {nums.map((num, nIdx) => (
            <button
              key={nIdx}
              type="button"
              className="kos-md-citation-badge"
              onClick={() => onCitationClick?.(num)}
              title={`Xem trích dẫn [${num}]`}
            >
              [{num}]
            </button>
          ))}
        </span>
      )
    }

    // Logic & reasoning arrows (=>, ->, ==>, -->)
    if (part === '=>' || part === '==>' || part === '->' || part === '-->') {
      return (
        <span key={index} className="kos-md-arrow" title="Suy luận / Hệ quả">
          {part === '=>' || part === '==>' ? ' ⟹ ' : ' ⟶ '}
        </span>
      )
    }

    // Math / LaTeX inline ($...$)
    if (part.startsWith('$') && part.endsWith('$') && part.length > 1) {
      const mathContent = part.slice(1, -1)
        .replace(/\\to/g, '→')
        .replace(/\\implies/g, '⟹')
        .replace(/\\models/g, '⊨')
        .replace(/\\equiv/g, '≡')
        .replace(/\\supseteq/g, '⊇')
        .replace(/\\subseteq/g, '⊆')
        .replace(/\\land/g, '∧')
        .replace(/\\lor/g, '∨')
        .replace(/\\mid/g, '|')
      return (
        <span key={index} className="kos-md-math">
          {mathContent}
        </span>
      )
    }

    return part
  })
}
