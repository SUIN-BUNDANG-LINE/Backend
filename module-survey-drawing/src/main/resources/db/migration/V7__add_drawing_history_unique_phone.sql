-- 중복 참여 차단을 SELECT 검증에서 UNIQUE 제약으로 이관한다.
--
-- 기존 검증(조회 후 없으면 진행)은 check-then-act 라 원자적이지 않다 — 같은 전화번호의 동시
-- 요청 둘이 모두 "이력 없음"을 보고 통과해 둘 다 커밋된다. 판정을 INSERT 시점의 제약 위반으로
-- 옮기면 검사와 기록이 한 문장이 되어 끼어들 틈이 없다 (티켓의 조건부 UPDATE 와 같은 원리).
-- 전화번호는 결정적 암호화(같은 입력 → 같은 암호문)라 암호문 동등성이 곧 번호 동등성이다.

-- 제약을 걸기 전에 기존 중복을 정리한다. 같은 번호의 이력이 여러 건이면 가장 먼저 확정된
-- 한 건만 남긴다 (created_at 동률이면 id 순).
DELETE h
FROM drawing_histories h
         JOIN (SELECT id
               FROM (SELECT id,
                            ROW_NUMBER() OVER (
                                PARTITION BY survey_id, phone_number
                                ORDER BY created_at, id
                                ) AS row_number_in_phone
                     FROM drawing_histories) ranked
               WHERE ranked.row_number_in_phone > 1) duplicated ON duplicated.id = h.id;

ALTER TABLE drawing_histories
    ADD CONSTRAINT uk_drawing_histories_survey_phone
        UNIQUE (survey_id, phone_number);
