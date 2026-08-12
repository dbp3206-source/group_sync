ALTER TABLE personal_busy_events
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN category VARCHAR(40),
    ADD COLUMN location VARCHAR(200),
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN reminder_minutes INTEGER;

ALTER TABLE weekly_schedules
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN category VARCHAR(40),
    ADD COLUMN location VARCHAR(200),
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN reminder_minutes INTEGER,
    ADD COLUMN frequency VARCHAR(20) NOT NULL DEFAULT 'WEEKLY';

ALTER TABLE badminton_seasons
    ADD COLUMN ranking_strategy VARCHAR(20) NOT NULL DEFAULT 'POINTS';

CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_notification_preference_user_type UNIQUE (user_id, notification_type)
);

CREATE TABLE badminton_checkin_tokens (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    token VARCHAR(120) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    season_id BIGINT NOT NULL REFERENCES badminton_seasons(id),
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id),
    name VARCHAR(160) NOT NULL,
    format VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    max_participants INTEGER NOT NULL,
    champion_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_tournament_format CHECK (format IN ('GROUP_KNOCKOUT','KNOCKOUT')),
    CONSTRAINT ck_tournament_status CHECK (status IN ('DRAFT','REGISTRATION_OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT ck_tournament_capacity CHECK (max_participants BETWEEN 2 AND 64)
);

CREATE TABLE tournament_participants (
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    seed_number INTEGER,
    registered_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tournament_id, user_id)
);

CREATE TABLE tournament_matches (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL UNIQUE REFERENCES badminton_matches(id) ON DELETE CASCADE,
    stage VARCHAR(20) NOT NULL,
    match_number INTEGER NOT NULL,
    next_match_number INTEGER,
    winner_user_id BIGINT REFERENCES users(id),
    CONSTRAINT ck_tournament_match_stage CHECK (stage IN ('GROUP','KNOCKOUT','FINAL')),
    CONSTRAINT uk_tournament_stage_number UNIQUE (tournament_id, stage, match_number)
);
