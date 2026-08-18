CREATE TABLE study_topics (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    goal TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_study_topic_status CHECK(status IN ('ACTIVE', 'ARCHIVED', 'COMPLETED'))
);
CREATE INDEX ix_study_topics_owner_updated ON study_topics(owner_id, updated_at DESC);

CREATE TABLE study_topic_resources (
    topic_id BIGINT NOT NULL REFERENCES study_topics(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(topic_id, resource_id)
);

CREATE TABLE topic_concepts (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES study_topics(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    summary TEXT NOT NULL,
    why_it_matters TEXT,
    study_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_concept_study_status CHECK(study_status IN ('NOT_STARTED', 'LEARNING', 'REVIEW_NEEDED', 'CHECKED'))
);
CREATE INDEX ix_topic_concepts_topic_pos ON topic_concepts(topic_id, position ASC);

CREATE TABLE topic_concept_sources (
    concept_id BIGINT NOT NULL REFERENCES topic_concepts(id) ON DELETE CASCADE,
    document_chunk_id BIGINT NOT NULL REFERENCES document_chunks(id) ON DELETE CASCADE,
    relevance_note VARCHAR(240),
    PRIMARY KEY(concept_id, document_chunk_id)
);

CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES study_topics(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    concept_id BIGINT REFERENCES topic_concepts(id) ON DELETE SET NULL,
    score_correct INTEGER NOT NULL DEFAULT 0,
    total_questions INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_quiz_attempts_topic_created ON quiz_attempts(topic_id, created_at DESC);
CREATE INDEX ix_quiz_attempts_owner_created ON quiz_attempts(owner_id, created_at DESC);

CREATE TABLE quiz_items (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    concept_id BIGINT REFERENCES topic_concepts(id) ON DELETE SET NULL,
    question TEXT NOT NULL,
    options_json TEXT NOT NULL,
    correct_option INTEGER NOT NULL,
    user_answer INTEGER,
    explanation TEXT NOT NULL,
    source_resource_id BIGINT REFERENCES resources(id) ON DELETE SET NULL,
    source_chunk_id BIGINT REFERENCES document_chunks(id) ON DELETE SET NULL,
    source_snippet TEXT
);
CREATE INDEX ix_quiz_items_attempt ON quiz_items(attempt_id);
