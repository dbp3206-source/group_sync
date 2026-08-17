CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    system_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_users_system_role CHECK (system_role IN ('USER', 'ADMIN'))
);

CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    group_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_groups_type CHECK (group_type IN ('STUDY', 'BADMINTON', 'OTHER'))
);

CREATE TABLE group_memberships (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_membership_group_user UNIQUE (group_id, user_id),
    CONSTRAINT ck_membership_role CHECK (role IN ('OWNER', 'ORGANIZER', 'MEMBER'))
);

CREATE TABLE group_invitations (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id),
    invitee_id BIGINT NOT NULL REFERENCES users(id),
    inviter_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    CONSTRAINT ck_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uk_pending_invitation_group_invitee
    ON group_invitations (group_id, invitee_id)
    WHERE status = 'PENDING';

CREATE INDEX ix_membership_user ON group_memberships (user_id);
CREATE INDEX ix_membership_group ON group_memberships (group_id);
CREATE INDEX ix_invitation_invitee_status ON group_invitations (invitee_id, status);
