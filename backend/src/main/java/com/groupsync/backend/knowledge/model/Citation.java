package com.groupsync.backend.knowledge.model;

import jakarta.persistence.*;

@Entity
@Table(name = "citations")
public class Citation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_message_id", nullable = false) private ChatMessage message;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "document_chunk_id", nullable = false) private DocumentChunk chunk;
    @Column(name = "citation_order", nullable = false) private int citationOrder;
    @Column(name = "relevance_score") private Double relevanceScore;
    @Column(name = "evidence_excerpt", nullable = false, columnDefinition = "TEXT") private String evidenceExcerpt;
    protected Citation() { }
    public Citation(ChatMessage message, DocumentChunk chunk, int citationOrder, double relevanceScore, String evidenceExcerpt) { this.message = message; this.chunk = chunk; this.citationOrder = citationOrder; this.relevanceScore = relevanceScore; this.evidenceExcerpt = evidenceExcerpt; }
    public Long getId() { return id; } public DocumentChunk getChunk() { return chunk; } public int getCitationOrder() { return citationOrder; } public Double getRelevanceScore() { return relevanceScore; } public String getEvidenceExcerpt() { return evidenceExcerpt; }
}
