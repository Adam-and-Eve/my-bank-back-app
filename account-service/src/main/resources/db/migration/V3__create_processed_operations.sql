CREATE TABLE processed_operations (
    operation_id        VARCHAR(128)    NOT NULL    PRIMARY KEY,
    operation_type      VARCHAR(32)     NOT NULL,
    request_hash        VARCHAR(64)     NOT NULL,
    status              VARCHAR(16)     NOT NULL,
    response_json       TEXT,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);