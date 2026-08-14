CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    page_number INTEGER,
    section VARCHAR(500),
    content TEXT NOT NULL,
    character_count INTEGER NOT NULL,
    embedding vector(768),
    embedding_model VARCHAR(120),
    embedding_dimensions INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_document_chunks_resource_index UNIQUE(resource_id, chunk_index),
    CONSTRAINT ck_document_chunk_index CHECK(chunk_index >= 0),
    CONSTRAINT ck_document_chunk_embedding_dimensions CHECK(embedding_dimensions IS NULL OR embedding_dimensions = 768)
);

CREATE INDEX ix_document_chunks_resource ON document_chunks(resource_id, chunk_index);
CREATE INDEX ix_document_chunks_embedding_cosine ON document_chunks USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
