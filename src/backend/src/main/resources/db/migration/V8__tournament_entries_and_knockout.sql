ALTER TABLE tournaments
    ADD COLUMN competition_mode VARCHAR(20) NOT NULL DEFAULT 'SINGLES';

CREATE TABLE tournament_entries (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    display_name VARCHAR(180) NOT NULL,
    seed_number INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tournament_entry_seed UNIQUE (tournament_id, seed_number)
);

CREATE TABLE tournament_entry_members (
    entry_id BIGINT NOT NULL REFERENCES tournament_entries(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (entry_id, user_id)
);

CREATE UNIQUE INDEX uk_tournament_entry_member_user
    ON tournament_entry_members (user_id, entry_id);

INSERT INTO tournament_entries (tournament_id, display_name, seed_number, created_at)
SELECT tp.tournament_id, u.display_name, tp.seed_number, tp.registered_at
FROM tournament_participants tp
JOIN users u ON u.id = tp.user_id;

INSERT INTO tournament_entry_members (entry_id, user_id)
SELECT te.id, tp.user_id
FROM tournament_entries te
JOIN tournament_participants tp
  ON tp.tournament_id = te.tournament_id
 AND tp.registered_at = te.created_at;

ALTER TABLE tournaments
    ADD COLUMN champion_entry_id BIGINT REFERENCES tournament_entries(id);

UPDATE tournaments t
SET champion_entry_id = te.id
FROM tournament_entries te
JOIN tournament_entry_members tem ON tem.entry_id = te.id
WHERE t.champion_id = tem.user_id
  AND te.tournament_id = t.id;

ALTER TABLE tournament_matches
    ADD COLUMN entry_a_id BIGINT REFERENCES tournament_entries(id),
    ADD COLUMN entry_b_id BIGINT REFERENCES tournament_entries(id),
    ADD COLUMN winner_entry_id BIGINT REFERENCES tournament_entries(id);

ALTER TABLE tournament_matches
    ALTER COLUMN match_id DROP NOT NULL;
