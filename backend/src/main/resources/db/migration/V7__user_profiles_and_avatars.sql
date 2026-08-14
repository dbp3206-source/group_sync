ALTER TABLE users
    ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    ADD COLUMN profile_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_avatars (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL,
    image_bytes BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_user_avatars_user ON user_avatars (user_id);
