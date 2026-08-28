-- token_hash is SHA-256(opaque refresh token) - the plaintext token is
-- never stored, only ever returned to the caller once, in the cookie.
CREATE TABLE refresh_tokens (
    id             TEXT PRIMARY KEY,
    user_id        TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash     TEXT NOT NULL,
    issued_at      TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    replaced_by_id TEXT REFERENCES refresh_tokens (id)
);

CREATE UNIQUE INDEX refresh_tokens_token_hash_unique_idx ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
