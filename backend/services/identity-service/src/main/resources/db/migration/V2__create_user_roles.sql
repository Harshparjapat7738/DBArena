CREATE TABLE user_roles (
    user_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    TEXT NOT NULL COLLATE "C",
    PRIMARY KEY (user_id, role)
);
