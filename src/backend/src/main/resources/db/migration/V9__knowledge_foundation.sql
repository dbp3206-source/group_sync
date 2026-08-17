CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    description TEXT,
    resource_type VARCHAR(20) NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    original_filename VARCHAR(512),
    mime_type VARCHAR(160),
    size_bytes BIGINT,
    storage_key VARCHAR(512),
    checksum_sha256 VARCHAR(64),
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    processing_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_resource_type CHECK (resource_type IN ('PDF', 'DOCX', 'TEXT', 'MARKDOWN', 'NOTE')),
    CONSTRAINT ck_resource_status CHECK (processing_status IN ('UPLOADED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'READY', 'FAILED')),
    CONSTRAINT ck_resource_size CHECK (size_bytes IS NULL OR size_bytes >= 0),
    CONSTRAINT ck_resource_priority CHECK (priority BETWEEN 0 AND 5)
);

CREATE UNIQUE INDEX uk_resources_owner_checksum
    ON resources(owner_id, checksum_sha256)
    WHERE checksum_sha256 IS NOT NULL;
CREATE INDEX ix_resources_owner_created ON resources(owner_id, created_at DESC);
CREATE INDEX ix_resources_owner_status ON resources(owner_id, processing_status);

CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_collections_owner_name UNIQUE(owner_id, name)
);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tags_owner_name UNIQUE(owner_id, name)
);

CREATE TABLE resource_collections (
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    collection_id BIGINT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    PRIMARY KEY(resource_id, collection_id)
);

CREATE TABLE resource_tags (
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY(resource_id, tag_id)
);

CREATE TABLE resource_relations (
    id BIGSERIAL PRIMARY KEY,
    source_resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    target_resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    relation_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_resource_relation_distinct CHECK (source_resource_id <> target_resource_id),
    CONSTRAINT uk_resource_relation UNIQUE(source_resource_id, target_resource_id, relation_type)
);

CREATE TABLE resource_notes (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_resource_notes_owner_resource ON resource_notes(owner_id, resource_id);

CREATE TABLE learning_progress (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    last_opened_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_learning_progress_owner_resource UNIQUE(owner_id, resource_id),
    CONSTRAINT ck_learning_progress_percent CHECK (progress_percent BETWEEN 0 AND 100)
);
