CREATE TABLE browser_sessions (
    id                UUID         PRIMARY KEY,
    browser_type      VARCHAR(32)  NOT NULL,
    environment       VARCHAR(32)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    is_mobile         BOOLEAN      NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    last_used_at      TIMESTAMPTZ  NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    closed_at         TIMESTAMPTZ,
    closed_reason     VARCHAR(32),
    idle_ttl_secs     INTEGER      NOT NULL,
    absolute_ttl_secs INTEGER      NOT NULL
);

CREATE INDEX idx_browser_sessions_status         ON browser_sessions (status);
CREATE INDEX idx_browser_sessions_created_at     ON browser_sessions (created_at DESC);
CREATE INDEX idx_browser_sessions_status_expires ON browser_sessions (status, expires_at);
