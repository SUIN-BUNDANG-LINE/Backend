# REST API 계약: 공동 결제(더치페이) 설문 개설

**Feature**: 001-co-funding-saga | 인증: 기존 JWT(`@LoginUser`), 표기된 곳만 예외

## 1. 모금 개시 (개설자)

```
POST /api/v1/surveys/{surveyId}/co-funding
Authorization: 필요 (설문 소유자)
{ "participantUserIds": ["<uuid>", "<uuid>"], "deadline": "2026-08-02T23:59:59" }
```

- 선행 조건: 설문이 시작 전 상태 + ImmediateDraw(경품) 설정 존재.
- 참여자 명단은 개설 시점에 확정(초대제, D7) — capacity = 명단 크기 + 1(개설자).
- 동작(한 트랜잭션): 분담금 산정(총액/capacity, 잔액→개설자) → `co_fundings`
  FUNDING 적재 + 참여자 행 일괄 INSERT(개설자 OWNER + 명단 MEMBER, 전원
  REGISTERED) + 참여자별 `payment_orders` PENDING 사전 적재(`order_id` 지정, D7)
  → Survey `PENDING_PAYMENT` 전이 → 설문·경품 잠금(FR-014).
- 응답 200: `{ "fundingId", "shareAmount", "ownerShareAmount", "deadline",
  "inviteUrl" }` — inviteUrl은 참여자에게 공유하는 현황·결제 진입 페이지 URL.
- 오류: 400(명단이 비어 있음(총 2인 미만) | 명단 중복·개설자 포함 | 기한 > 7일),
  409(이미 모금 존재 | 설문 상태 부적합).

## 2. 분담금 결제 진입 (내 주문 조회)

```
GET /api/v1/co-fundings/{fundingId}/participants/me/order
Authorization: 필요
```

- 동작: 본인 참여자의 사전 발급된 주문 조회(조회 전용 — 주문은 개설 트랜잭션에서
  이미 적재됨, D7).
- 응답 200: `{ "orderId", "amount", "checkoutUrl" }` — checkoutUrl은 기존
  `/payments/checkout?orderId=` 재사용. 오류: 404(참여자 아님 | 모금 없음).
- 이후 흐름은 기존 결제 계약과 동일: checkout → 토스 위젯 → success/fail 착지 →
  confirm(동기+릴레이). **차이점**: settle 시 모금 상태 검사(D6) — FUNDING이면
  참여자 SETTLED 전이 + `co-payment-settled` 발행(Outbox), FAILED면 즉시 CANCEL
  커맨드 적재.

## 3. 모금 현황 (참여자·개설자)

```
GET /api/v1/co-fundings/{fundingId}
Authorization: 필요 (참여자 또는 개설자)
```

- 응답 200:

```json
{
  "status": "FUNDING",
  "capacity": 3,
  "settledCount": 2,
  "deadline": "2026-08-02T23:59:59",
  "participants": [
    { "role": "OWNER", "nickname": "...", "status": "SETTLED" },
    { "role": "MEMBER", "nickname": "...", "status": "REGISTERED" }
  ],
  "myRefundStatus": null
}
```

- 무산 후: `status=FAILED|REFUNDED`, 본인 환불 상태
  `myRefundStatus`(REFUNDING/REFUNDED — CANCEL 커맨드·주문 상태에서 유도).
- 폴링 대상 API(US3) — 종착 상태(CONFIRMED/REFUNDED) 도달 시 프런트가 폴링 중단.
- 무산 트리거는 기한 만료 하나(FR-006) — 철회 엔드포인트는 없다.

## 기존 계약 재사용 (변경 없음)

- `GET /payments/checkout?orderId=` · `GET /api/v1/payments/checkout-info`
- `GET /api/v1/payments/success|fail` (settle 내부에 D6 분기만 추가)
- `POST /api/v1/payments/webhook` (웹훅 Inbox — 취소 웹훅이 CANCEL 수렴의 보조 신호)
