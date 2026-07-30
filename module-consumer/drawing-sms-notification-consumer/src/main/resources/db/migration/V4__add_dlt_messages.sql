CREATE TABLE dlt_messages (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id          VARCHAR(36)  NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    payload           TEXT         NOT NULL,
    retry_count       INT          NOT NULL,
    last_error        TEXT,
    failed_at         DATETIME(6)  NOT NULL,
    reprocessed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,

    INDEX idx_reprocessed (reprocessed, created_at)
);
