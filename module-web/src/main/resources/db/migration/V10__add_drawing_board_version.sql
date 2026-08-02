-- 낙관적 락 실험 — DrawingBoard @Version 매핑 컬럼.
-- OPTIMISTIC_FORCE_INCREMENT 조회로 추첨마다 보드 버전을 올려 동시 추첨 충돌을 커밋 시점에 검출한다.
ALTER TABLE drawing_boards
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
