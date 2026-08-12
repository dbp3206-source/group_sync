CREATE TABLE badminton_seasons (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_season_group_name UNIQUE (group_id, name),
    CONSTRAINT ck_badminton_season_dates CHECK (ends_on IS NULL OR starts_on <= ends_on)
);

CREATE UNIQUE INDEX uk_badminton_active_season ON badminton_seasons(group_id) WHERE active = TRUE;

CREATE TABLE badminton_profiles (
    id BIGSERIAL PRIMARY KEY,
    membership_id BIGINT NOT NULL UNIQUE REFERENCES group_memberships(id) ON DELETE CASCADE,
    skill_level VARCHAR(20) NOT NULL,
    bio VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE badminton_venues (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_venue_group_name UNIQUE (group_id, name)
);

CREATE TABLE badminton_courts (
    id BIGSERIAL PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES badminton_venues(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_badminton_court_venue_name UNIQUE (venue_id, name)
);

CREATE TABLE badminton_sessions (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    season_id BIGINT NOT NULL REFERENCES badminton_seasons(id),
    venue_id BIGINT NOT NULL REFERENCES badminton_venues(id),
    title VARCHAR(160) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    registration_deadline TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_badminton_session_time CHECK (start_at < end_at),
    CONSTRAINT ck_badminton_session_deadline CHECK (registration_deadline <= start_at),
    CONSTRAINT ck_badminton_session_capacity CHECK (capacity > 0),
    CONSTRAINT ck_badminton_session_status CHECK (status IN ('DRAFT','OPEN','CONFIRMED','PLAYING','COMPLETED','CANCELLED'))
);

CREATE INDEX ix_badminton_session_group_time ON badminton_sessions(group_id, start_at);

CREATE TABLE badminton_session_courts (
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    court_id BIGINT NOT NULL REFERENCES badminton_courts(id),
    PRIMARY KEY (session_id, court_id)
);

CREATE TABLE badminton_registrations (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    queued_at TIMESTAMPTZ,
    registered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_badminton_registration_session_user UNIQUE (session_id, user_id),
    CONSTRAINT ck_badminton_registration_status CHECK (status IN ('REGISTERED','WAITLISTED','CANCELLED','CHECKED_IN','NO_SHOW'))
);

CREATE INDEX ix_badminton_registration_session_status ON badminton_registrations(session_id, status, queued_at, id);
CREATE INDEX ix_badminton_registration_user ON badminton_registrations(user_id, status);

CREATE TABLE badminton_responsibilities (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_sessions(id) ON DELETE CASCADE,
    item_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assignee_id BIGINT REFERENCES users(id),
    note VARCHAR(300),
    CONSTRAINT uk_badminton_responsibility_session_item UNIQUE (session_id, item_name),
    CONSTRAINT ck_badminton_responsibility_status CHECK (status IN ('NEEDED','ASSIGNED'))
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_type VARCHAR(40),
    target_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_notifications_user_created ON notifications(user_id, created_at DESC);
