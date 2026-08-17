-- V13: Durable database-backed storage for original resource files (PDF, DOCX, TXT, MD, NOTE).
-- Guarantees durability across container restarts, redeployments, and cloud environments (Render, Neon, Docker).

CREATE TABLE IF NOT EXISTS storage_blobs (
    storage_key VARCHAR(512) PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(512) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_storage_blobs_owner ON storage_blobs(owner_id);
