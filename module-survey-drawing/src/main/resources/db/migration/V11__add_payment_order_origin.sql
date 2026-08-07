-- 주문 발급 출처(단독/모금) - settled·failed 이벤트에 실려 나가, 설문 리스너가
-- co_fundings 교차 읽기 없이 단독/모금을 판별하게 한다.
ALTER TABLE payment_orders
    ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'SOLO';

-- 기존 행 backfill: 모금 참여자 주문(co_funding_participants 의 order_id 와 일치)은 CO_FUNDING
UPDATE payment_orders po
SET po.origin = 'CO_FUNDING'
WHERE EXISTS (
    SELECT 1 FROM co_funding_participants p
    WHERE p.order_id = po.order_id
);
