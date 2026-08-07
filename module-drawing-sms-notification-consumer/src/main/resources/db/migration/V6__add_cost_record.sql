-- SMS 발송-비용 정합 사가의 영속 진실 원천 (Spec 001)
-- 상태 머신(state 컬럼): PENDING (사가 시작) → CONFIRMED (발송 확정) / REVERSED (영구 실패 = 보상)
-- 멱등성 키: original_drawing_event_id UNIQUE (drawing-completed eventId)
-- 보관 정책: Q1 결정 — 90일 일괄 삭제 (상태 무관)
CREATE TABLE cost_record (
    id                          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    original_drawing_event_id   VARCHAR(36)   NOT NULL,
    participant_id              VARCHAR(36)   NOT NULL,
    survey_id                   VARCHAR(36)   NOT NULL,
    state                       VARCHAR(20)   NOT NULL,
    amount                      DECIMAL(10,2) NOT NULL,
    created_at                  DATETIME(6)   NOT NULL,
    updated_at                  DATETIME(6)   NOT NULL,

    UNIQUE KEY uk_cost_record_original_drawing_event_id (original_drawing_event_id)
);
