-- 토스 카드결제 3종 테이블
-- payment_orders        : 주문·결제 원장(설문당 1건)
-- payment_commands      : Command Outbox — confirm 명령 큐(id=엔티티 PK)
-- payment_webhook_inbox : Webhook Inbox — 취소 웹훅 멱등 장부

CREATE TABLE payment_orders (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    survey_id   BINARY(16)   NOT NULL,
    maker_id    BINARY(16)   NOT NULL,
    order_id    VARCHAR(64)  NOT NULL,
    amount      INT          NOT NULL,
    payment_key VARCHAR(255),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    UNIQUE KEY uk_survey_id (survey_id),   -- 설문당 결제 주문 1건
    UNIQUE KEY uk_order_id (order_id)      -- 토스 주문 고유
);

CREATE TABLE payment_commands (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    command_type    VARCHAR(20)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    request_payload TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    sent_at         DATETIME(6),
    confirmed_at    DATETIME(6),

    INDEX idx_status_created (status, created_at)   -- 릴레이 SKIP LOCKED 클레임
);

CREATE TABLE payment_webhook_inbox (
    id           BINARY(16)   NOT NULL PRIMARY KEY,
    webhook_id   VARCHAR(64)  NOT NULL,
    order_id     VARCHAR(64)  NOT NULL,
    event_type   VARCHAR(40)  NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    received_at  DATETIME(6)  NOT NULL,
    processed_at DATETIME(6),

    UNIQUE KEY uk_webhook_id (webhook_id)   -- 인바운드 웹훅 멱등
);
