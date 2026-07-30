# Kafka 이벤트 계약: 공동 결제(더치페이) 설문 개설

**Feature**: 001-co-funding-saga | wire 스키마 계약 — module-consumer/common과 사본 동기화(D11)
**공통**: 파티션 키 = `fundingId`. W3C tracecontext 헤더 자동 전파(OTel). 모든
컨슈머는 at-least-once 재수신을 멱등 흡수해야 한다.

## 1. `co-payment-settled` — 참여자 결제 확정

- **발행**: web — settle 트랜잭션(D6)에서 참여자 SETTLED 전이와 함께 Outbox 적재
- **구독**: co-funding-consumer 모듈 집계 리스너 (groupId `co-funding-settlement`)

```json
{
  "eventId": "uuid",
  "fundingId": "uuid",
  "surveyId": "uuid",
  "participantId": "uuid",
  "orderId": "string(6~64)",
  "settledAt": "ISO-8601"
}
```

- **컨슈머 계약**: SETTLED 행 수를 세어 장벽 CAS(D4) 시도. CAS 승자만 설문
  활성화(Survey.start). 재수신 시 CAS 패배로 무해.

## 2. `co-funding-failed` — 무산 확정

- **발행**: 무산 CAS(`FUNDING → FAILED`) 승자만 — 기한 스케줄러(consumer, 직접
  발행). 무산 트리거는 기한 만료 하나(FR-006).
- **구독**: 환불 리스너 (groupId `co-funding-refund`), 알림 리스너(선택)

```json
{
  "eventId": "uuid",
  "fundingId": "uuid",
  "surveyId": "uuid",
  "settledOrderIds": ["orderId", "..."],
  "failedAt": "ISO-8601"
}
```

- **컨슈머 계약(환불 리스너)**: `settledOrderIds` 각각에 CANCEL 커맨드 적재 —
  `UNIQUE(aggregate_id, command_type)`로 재수신·중복 발행 흡수. 리스너 책임은
  적재까지다: 실제 토스 호출과 주문 CANCELED·참여자 REFUNDED·`FAILED → REFUNDED`
  수렴 전이는 web 릴레이의 CANCEL settle 후처리가 수행한다(D5). **주의**:
  이벤트의 settledOrderIds는 발행 시점 스냅샷 — 리스너는 DB의 SETTLED 행을
  재조회해 늦은 확정(D6 경계)을 놓치지 않는다(스냅샷은 참고, 진실은 DB).

## DLT

각 groupId의 처리 불능 메시지는 기존 관례대로 `<topic>.DLT`로 이동, 기존
`dlt-sms-notification` 인프라 패턴으로 수거·메트릭 집계.

## 멱등성 매트릭스 (요약)

| 재수신 상황 | 흡수 장치 |
|---|---|
| settled 재수신 | 장벽 CAS 패배 → no-op |
| failed 재수신 | CANCEL 커맨드 UNIQUE → no-op |
| 스케줄러 중복 스캔·재발행 | 무산 CAS가 발행 전에 승자 1명만 허용 |
