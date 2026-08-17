CREATE TABLE badminton_allocations (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    court_id BIGINT NOT NULL REFERENCES badminton_courts(id),
    round_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_allocation_court_round UNIQUE (session_id, court_id, round_number),
    CONSTRAINT ck_badminton_allocation_status CHECK (status IN ('DRAFT','CONFIRMED'))
);

CREATE TABLE badminton_allocation_players (
    allocation_id BIGINT NOT NULL REFERENCES badminton_allocations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    position INTEGER NOT NULL,
    PRIMARY KEY (allocation_id, user_id),
    CONSTRAINT uk_badminton_allocation_position UNIQUE (allocation_id, position)
);

CREATE TABLE badminton_matches (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    season_id BIGINT NOT NULL REFERENCES badminton_seasons(id),
    court_id BIGINT NOT NULL REFERENCES badminton_courts(id),
    round_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    score_a INTEGER,
    score_b INTEGER,
    winner_side VARCHAR(2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_match_court_round UNIQUE (session_id, court_id, round_number),
    CONSTRAINT ck_badminton_match_status CHECK (status IN ('SCHEDULED','PLAYING','RESULT_SUBMITTED','CONFIRMED','CANCELLED')),
    CONSTRAINT ck_badminton_match_score CHECK (score_a IS NULL OR (score_a >= 0 AND score_b >= 0 AND score_a <> score_b)),
    CONSTRAINT ck_badminton_match_winner CHECK (winner_side IS NULL OR winner_side IN ('A','B'))
);

CREATE TABLE badminton_match_sides (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES badminton_matches(id) ON DELETE CASCADE,
    side_code VARCHAR(2) NOT NULL,
    CONSTRAINT uk_badminton_match_side UNIQUE (match_id, side_code),
    CONSTRAINT ck_badminton_match_side_code CHECK (side_code IN ('A','B'))
);

CREATE TABLE badminton_match_participants (
    side_id BIGINT NOT NULL REFERENCES badminton_match_sides(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (side_id, user_id)
);

CREATE TABLE badminton_player_stats (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    season_id BIGINT NOT NULL REFERENCES badminton_seasons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    matches_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    attended INTEGER NOT NULL DEFAULT 0,
    no_shows INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_badminton_player_stats_scope UNIQUE (group_id, season_id, user_id)
);

CREATE TABLE badminton_ranking_history (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES badminton_matches(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    season_id BIGINT NOT NULL REFERENCES badminton_seasons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    points_after INTEGER NOT NULL,
    wins_after INTEGER NOT NULL,
    matches_after INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_ranking_history_match_user UNIQUE (match_id, user_id)
);

CREATE TABLE group_news (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES users(id),
    news_type VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    source_key VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_group_news_source UNIQUE (source_key)
);

ALTER TABLE notifications ADD COLUMN source_key VARCHAR(160);
CREATE UNIQUE INDEX uk_notification_source_user ON notifications(source_key, user_id) WHERE source_key IS NOT NULL;
