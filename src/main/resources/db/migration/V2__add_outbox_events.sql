-- Outbox 패턴: DB 저장과 Kafka 발행의 원자성 보장
CREATE TABLE outbox_events (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(50)  NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    kafka_topic     VARCHAR(100) NOT NULL,
    kafka_key       VARCHAR(100) NOT NULL,
    kafka_payload   TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6)  NOT NULL,
    published_at    DATETIME(6),

    INDEX idx_outbox_status_created (status, created_at)
);
