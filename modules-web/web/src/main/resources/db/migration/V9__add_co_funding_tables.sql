-- 공동 결제(더치페이) 모금 2종 테이블 + 기존 결제 테이블 제약 조정
-- co_fundings             : 공동 모금 — 설문당 1건, 사가 상태 머신(FUNDING→CONFIRMED/FAILED→REFUNDED) 보유
-- co_funding_participants : 모금 참여자 — 자리 선점(REGISTERED)→결제 확정(SETTLED)→환불(REFUNDED)

CREATE TABLE co_fundings (
    id                 BINARY(16)  NOT NULL PRIMARY KEY,
    survey_id          BINARY(16)  NOT NULL,
    owner_id           BINARY(16)  NOT NULL,
    capacity           INT         NOT NULL,
    registered_count   INT         NOT NULL DEFAULT 0,
    share_amount       INT         NOT NULL,
    owner_share_amount INT         NOT NULL,
    deadline           DATETIME(6) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'FUNDING',
    fail_reason        VARCHAR(30),
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,

    UNIQUE KEY uk_cofunding_survey_id (survey_id),          -- 모금:설문 1:1
    INDEX idx_cofunding_status_deadline (status, deadline)  -- 기한 스케줄러 SKIP LOCKED 스캔
);

CREATE TABLE co_funding_participants (
    id         BINARY(16)  NOT NULL PRIMARY KEY,
    funding_id BINARY(16)  NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    role       VARCHAR(10) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    order_id   VARCHAR(64),
    settled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    UNIQUE KEY uk_participant_funding_user (funding_id, user_id),  -- 중복 등록 차단 + 장벽 카운트 근거 행
    UNIQUE KEY uk_participant_order_id (order_id)                  -- 참여자:주문 1:1
);

-- payment_orders: 설문당 1건 제약 완화 — 공동 모금은 참여자별 주문 N건이 같은 설문을 가리킴
ALTER TABLE payment_orders
    DROP INDEX uk_survey_id,
    ADD INDEX idx_payment_orders_survey_id (survey_id);

-- payment_commands: 주문·명령타입당 1건 — CANCEL 이중 적재(이중 환불) DB 차단
ALTER TABLE payment_commands
    ADD UNIQUE KEY uk_command_aggregate_type (aggregate_id, command_type);
