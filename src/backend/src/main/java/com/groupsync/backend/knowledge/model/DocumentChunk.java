package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_chunk_id")
    private DocumentChunk parentChunk;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_level", nullable = false, length = 20)
    private ChunkLevel chunkLevel = ChunkLevel.CHILD;

    @Column(name = "chunking_version", nullable = false)
    private int chunkingVersion = 1;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(length = 500)
    private String section;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "character_count", nullable = false)
    private int characterCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "embedding_model", length = 120)
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DocumentChunk() { }

    public DocumentChunk(Resource resource, int chunkIndex, Integer pageNumber, String section, String content) {
        this(resource, null, ChunkLevel.CHILD, 1, chunkIndex, pageNumber, section, content);
    }

    public DocumentChunk(Resource resource, DocumentChunk parentChunk, ChunkLevel chunkLevel,
                         int chunkingVersion, int chunkIndex, Integer pageNumber,
                         String section, String content) {
        this.resource = resource;
        this.parentChunk = parentChunk;
        this.chunkLevel = chunkLevel != null ? chunkLevel : ChunkLevel.CHILD;
        this.chunkingVersion = chunkingVersion;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.section = section;
        this.content = content != null ? content : "";
        this.characterCount = this.content.length();
    }

    public void embed(float[] values, String model) {
        if (values == null || values.length != 768) {
            throw new IllegalArgumentException("Embeddings must contain 768 dimensions.");
        }
        this.embedding = values;
        this.embeddingModel = model;
        this.embeddingDimensions = 768;
    }

    public void setParentChunk(DocumentChunk parentChunk) {
        this.parentChunk = parentChunk;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public DocumentChunk getParentChunk() { return parentChunk; }
    public ChunkLevel getChunkLevel() { return chunkLevel; }
    public int getChunkingVersion() { return chunkingVersion; }
    public int getChunkIndex() { return chunkIndex; }
    public Integer getPageNumber() { return pageNumber; }
    public String getSection() { return section; }
    public String getContent() { return content; }
    public float[] getEmbedding() { return embedding; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Integer getEmbeddingDimensions() { return embeddingDimensions; }
    public Instant getCreatedAt() { return createdAt; }
}
