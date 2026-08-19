-- 결제 서비스 스키마 (payment_db) — 주문/커맨드/웹훅 인박스 + 자기 Outbox

-- 토스 결제 시도 장부 - 시도 1회 = 행 1개(불변). 재결제는 행 갱신이 아니라 새 행 발급.
-- PK = 토스 orderId - 시도마다 새로 뽑는 불변 자연 키를 그대로 정체성으로 쓴다.
CREATE TABLE toss_orders
(
    id           VARCHAR(64) NOT NULL PRIMARY KEY,
    -- 산 물건의 좌표 - (타입 = 해석할 도메인, id = 그 테이블의 행)
    product_type VARCHAR(20) NOT NULL,
    product_id   BINARY(16)  NOT NULL,
    payer_id     BINARY(16)  NOT NULL,            -- 결제자(단독=설문 주인, 모금=참여자)
    amount       INT         NOT NULL,
    payment_key  VARCHAR(255),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- 발급 출처(SOLO/CO_FUNDING) - settled·failed 이벤트에 실려 나가,
    -- 설문 리스너가 co_fundings 교차 읽기 없이 단독/모금을 판별하게 한다
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,

    INDEX idx_toss_orders_product_id (product_id) -- 모금 참여자별·단독 재시도별 주문 N건이 같은 상품을 가리킨다
);

CREATE TABLE toss_api_call_outbox
(
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    call_type       VARCHAR(20)  NOT NULL,
    order_id        VARCHAR(64)  NOT NULL,
    request_payload TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    succeeded_at    DATETIME(6),
    payment_key     VARCHAR(255) NOT NULL,

    UNIQUE KEY uk_call_order_type (order_id, call_type, payment_key), -- CANCEL 이중 적재(이중 환불) DB 차단
    INDEX idx_status_created (status, created_at)                     -- 릴레이 SKIP LOCKED 클레임
);

CREATE TABLE payment_webhook_inbox
(
    id           BINARY(16)  NOT NULL PRIMARY KEY,
    webhook_id   VARCHAR(64) NOT NULL,
    order_id     VARCHAR(64) NOT NULL,
    event_type   VARCHAR(40) NOT NULL,
    payload      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at  DATETIME(6) NOT NULL,
    processed_at DATETIME(6),

    UNIQUE KEY uk_webhook_id (webhook_id) -- 인바운드 웹훅 멱등
);

-- Outbox 패턴: DB 저장과 Kafka 발행의 원자성 보장
CREATE TABLE kafka_record_outbox
(
    id                 BINARY(16)   NOT NULL PRIMARY KEY,
    kafka_topic        VARCHAR(100) NOT NULL,
    kafka_record_key   VARCHAR(100) NOT NULL,
    kafka_record_value TEXT         NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count        INT          NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    published_at       DATETIME(6),

    INDEX idx_outbox_status_created (status, created_at)
);
