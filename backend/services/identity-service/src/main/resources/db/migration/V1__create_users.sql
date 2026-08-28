-- Collation pinned to C per root CLAUDE.md hard rule #9.
CREATE TABLE users (
    id             TEXT PRIMARY KEY,
    email          TEXT NOT NULL COLLATE "C",
    password_hash  TEXT NOT NULL,
    display_name   TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX users_email_unique_idx ON users (lower(email));
