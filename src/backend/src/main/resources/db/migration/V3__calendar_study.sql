CREATE TABLE personal_busy_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(160) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_busy_event_time CHECK (start_at < end_at)
);

CREATE INDEX ix_busy_event_user_time ON personal_busy_events (user_id, start_at, end_at);

CREATE TABLE weekly_schedules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(160) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    timezone VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_weekly_schedule_time CHECK (start_time < end_time),
    CONSTRAINT ck_weekly_schedule_dates CHECK (valid_from <= valid_until)
);

CREATE TABLE weekly_schedule_days (
    schedule_id BIGINT NOT NULL REFERENCES weekly_schedules(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (schedule_id, day_of_week),
    CONSTRAINT ck_weekly_schedule_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

CREATE TABLE study_sessions (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id),
    organizer_id BIGINT NOT NULL REFERENCES users(id),
    topic VARCHAR(160) NOT NULL,
    goal VARCHAR(500),
    location VARCHAR(240),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_study_session_time CHECK (start_at < end_at),
    CONSTRAINT ck_study_session_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_study_session_status CHECK (status IN ('DRAFT','OPEN','CONFIRMED','COMPLETED','CANCELLED'))
);

CREATE INDEX ix_study_session_group_time ON study_sessions (group_id, start_at);

CREATE TABLE study_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    attendance VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_study_participant UNIQUE (session_id, user_id),
    CONSTRAINT ck_study_attendance CHECK (attendance IN ('REGISTERED','ATTENDED','ABSENT'))
);

CREATE TABLE study_materials (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    url VARCHAR(1000) NOT NULL
);

CREATE TABLE study_goals (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    description VARCHAR(300) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE
);
