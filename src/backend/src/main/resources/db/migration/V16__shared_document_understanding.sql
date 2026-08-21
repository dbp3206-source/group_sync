CREATE TABLE document_understandings (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    source_checksum VARCHAR(64) NOT NULL,
    chunking_version INTEGER NOT NULL,
    model VARCHAR(120) NOT NULL,
    understanding_version VARCHAR(40) NOT NULL,
    normalized_title VARCHAR(240),
    summary TEXT,
    key_ideas_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    candidate_tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    broad_themes_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    difficulty_level VARCHAR(120),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_document_understanding_status
        CHECK (status IN ('CURRENT', 'STALE', 'FAILED', 'UNSUPPORTED')),
    CONSTRAINT ck_document_understanding_chunking_version CHECK (chunking_version > 0)
);

CREATE UNIQUE INDEX uk_document_understandings_current_resource
    ON document_understandings(resource_id)
    WHERE status = 'CURRENT';
CREATE INDEX ix_document_understandings_resource_created
    ON document_understandings(resource_id, created_at DESC);
CREATE INDEX ix_document_understandings_configuration
    ON document_understandings(resource_id, source_checksum, chunking_version, model, understanding_version);

CREATE TABLE document_understanding_evidence (
    understanding_id BIGINT NOT NULL REFERENCES document_understandings(id) ON DELETE CASCADE,
    document_chunk_id BIGINT NOT NULL REFERENCES document_chunks(id) ON DELETE CASCADE,
    item_key VARCHAR(80) NOT NULL DEFAULT 'document',
    PRIMARY KEY (understanding_id, document_chunk_id, item_key)
);

CREATE INDEX ix_document_understanding_evidence_chunk
    ON document_understanding_evidence(document_chunk_id);
