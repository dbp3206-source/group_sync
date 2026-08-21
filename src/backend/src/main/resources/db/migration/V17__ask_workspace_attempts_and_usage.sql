ALTER TABLE chat_messages
    ADD COLUMN message_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
    ADD COLUMN failure_category VARCHAR(40);

CREATE TABLE ask_attempts (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    user_message_id BIGINT NOT NULL UNIQUE REFERENCES chat_messages(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    query_mode VARCHAR(30),
    failure_category VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_ask_attempt_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETE', 'FAILED'))
);

CREATE INDEX ix_ask_attempts_owner_updated ON ask_attempts(owner_id, updated_at DESC);
CREATE INDEX ix_ask_attempts_session_created ON ask_attempts(session_id, created_at ASC);

CREATE TABLE ai_usage_events (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempt_id BIGINT REFERENCES ask_attempts(id) ON DELETE SET NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    request_status VARCHAR(20) NOT NULL,
    query_mode VARCHAR(30),
    prompt_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    context_chars INTEGER,
    retrieved_chunks INTEGER,
    duration_ms BIGINT,
    failure_category VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_ai_usage_events_owner_created ON ai_usage_events(owner_id, created_at DESC);
