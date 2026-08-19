-- 모금 서비스 스키마 (cofunding_db) — co_fundings·participants + 자기 Outbox

CREATE TABLE co_fundings (
    id               BINARY(16)  NOT NULL PRIMARY KEY,
    survey_id        BINARY(16)  NOT NULL,
    owner_id         BINARY(16)  NOT NULL,
    capacity         INT         NOT NULL,
    share_amount     INT         NOT NULL,
    deadline         DATETIME(6) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,

    INDEX idx_cofunding_survey_id (survey_id),              -- 종착 모금은 이력 누적 - 진행 중 1건 보장은 접수 가드 몫
    INDEX idx_cofunding_status_deadline (status, deadline)  -- 기한 스케줄러 SKIP LOCKED 스캔
);

CREATE TABLE co_funding_participants (
    id         BINARY(16)  NOT NULL PRIMARY KEY,
    funding_id BINARY(16)  NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    order_id   VARCHAR(64),
    paid_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_participant_funding_user (funding_id, user_id),  -- 중복 등록 차단 + 장벽 카운트 근거 행
    UNIQUE KEY uk_participant_order_id (order_id)                  -- 참여자:주문 1:1
);

-- Outbox 패턴: DB 저장과 Kafka 발행의 원자성 보장
CREATE TABLE kafka_record_outbox (
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
