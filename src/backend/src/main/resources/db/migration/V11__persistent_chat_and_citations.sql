CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    collection_id BIGINT REFERENCES collections(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_chat_scope_type CHECK(scope_type IN ('THIS_RESOURCE', 'SELECTED_RESOURCES', 'COLLECTION', 'LIBRARY'))
);
CREATE INDEX ix_chat_sessions_owner_updated ON chat_sessions(owner_id, updated_at DESC);

CREATE TABLE chat_session_resources (
    chat_session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    PRIMARY KEY(chat_session_id, resource_id)
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    message_role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_chat_message_role CHECK(message_role IN ('USER', 'ASSISTANT'))
);
CREATE INDEX ix_chat_messages_session_created ON chat_messages(chat_session_id, created_at);

CREATE TABLE citations (
    id BIGSERIAL PRIMARY KEY,
    chat_message_id BIGINT NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    document_chunk_id BIGINT NOT NULL REFERENCES document_chunks(id) ON DELETE RESTRICT,
    citation_order INTEGER NOT NULL,
    relevance_score DOUBLE PRECISION,
    evidence_excerpt TEXT NOT NULL,
    CONSTRAINT uk_citation_message_order UNIQUE(chat_message_id, citation_order)
);
