-- V15: RAG v2 Parent-Child Chunking and Ingestion Metadata
-- Supports hierarchical chunking: Parent chunks provide context, Child chunks provide search precision.

ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS parent_chunk_id BIGINT REFERENCES document_chunks(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS chunk_level VARCHAR(20) NOT NULL DEFAULT 'CHILD',
    ADD COLUMN IF NOT EXISTS chunking_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE document_chunks
    ADD CONSTRAINT ck_document_chunk_level CHECK (chunk_level IN ('PARENT', 'CHILD'));

CREATE INDEX IF NOT EXISTS ix_document_chunks_parent_id ON document_chunks(parent_chunk_id);
CREATE INDEX IF NOT EXISTS ix_document_chunks_resource_level ON document_chunks(resource_id, chunk_level);
