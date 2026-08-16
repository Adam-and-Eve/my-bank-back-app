CREATE TABLE accounts (
    id          BIGSERIAL       NOT NULL    PRIMARY KEY,
    login       VARCHAR(64)     NOT NULL    UNIQUE,
    name        VARCHAR(120)    NOT NULL,
    birthdate   DATE            NOT NULL,
    balance     NUMERIC(19, 2)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL,
    version     BIGINT          NOT NULL    DEFAULT 0
);