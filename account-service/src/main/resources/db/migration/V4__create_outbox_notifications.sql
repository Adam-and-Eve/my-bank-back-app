CREATE TABLE outbox_notifications (
    id                  UUID            NOT NULL    PRIMARY KEY,
    event_id            UUID            NOT NULL,
    operation_id        VARCHAR(128)    NOT NULL,
    payload             TEXT            NOT NULL,
    status              VARCHAR(16)     NOT NULL,
    attempt_count       INT             NOT NULL    DEFAULT 0,
    next_attempt_at     TIMESTAMP       NOT NULL,
    last_error          TEXT,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);

CREATE INDEX idx_outbox_status_next_attempt ON outbox_notifications (status, next_attempt_at);