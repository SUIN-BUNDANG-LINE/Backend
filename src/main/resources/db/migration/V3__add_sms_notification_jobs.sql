-- Inbox 패턴: 외부 API 발송 작업 큐
-- Consumer가 빠르게 ack 후 Worker가 비동기 처리 (max.poll.interval.ms 초과 방지)
-- 상태 머신: PENDING → EXECUTING → COMPLETED / FAILED
-- 재시도: Exponential backoff (1s, 2s, 4s, 8s, 16s), 최대 5회
CREATE TABLE sms_notification_jobs (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id          VARCHAR(36)  NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    payload           TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count       INT          NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)  NOT NULL,
    last_error        TEXT,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,

    -- 멱등성 보장: 같은 eventId + notificationType 조합은 단 하나의 job만 존재
    UNIQUE KEY uk_event_id_notification_type (event_id, notification_type),

    -- Worker 조회용: PENDING 상태의 실행 가능한 job 탐색
    INDEX idx_status_next_attempt (status, next_attempt_at)
);
