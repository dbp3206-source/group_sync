-- V12: Lexical / full-text search index for document_chunks.
-- Uses 'simple' configuration: no language-specific stemming, safe for mixed EN/VI content.
-- Generated column keeps tsvector automatically in sync with content changes.

ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS fts_content tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', coalesce(content, ''))) STORED;

CREATE INDEX IF NOT EXISTS ix_document_chunks_fts
    ON document_chunks USING gin (fts_content);
