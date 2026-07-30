# Tasks: 공동 결제(더치페이) 설문 개설 — 경량판(학습 특화)

**Input**: Design documents from `/specs/001-co-funding-saga/`

**Prerequisites**: plan.md, spec.md, research.md(D1~D11), data-model.md, contracts/, quickstart.md

**경량화 기준(2026-07-26)**: 목적은 코레오그래피 보상 트랜잭션 학습. 사가 체인(이벤트
발행 → 집계 장벽 → 무산 → 부채꼴 환불 → 수렴)과 경합·멱등 방어에 기여하지 않는
태스크는 유예했다. **유예 목록**: 현황 조회 API(US3 전체), 메트릭·Grafana 대시보드,
Swagger doc 인터페이스, 독립 단위 테스트 태스크(핵심 검증은 각 태스크 수락 기준에
흡수). 나중에 되살릴 때는 `/speckit-converge`로 재파생.

**Organization**: `(consumer 모듈)` 표시는 `module-consumer/co-funding-consumer` 신규 자족형 모듈 작업 (같은 레포, D8·D12).

## Format: `[ID] [P?] Description`

## Phase 1~2: 완료분

- [x] T001 Flyway V9 마이그레이션 — co_fundings·co_funding_participants + payment_orders UNIQUE 완화 + payment_commands UNIQUE 추가 (적용·검증 완료)
- [x] T002 CoFunding·CoFundingParticipant 엔티티 + 상태 enum + 전이 가드 (ddl-auto validate 기동 검증 완료)

## Phase 3: 사가 기반 (Foundational)

- [x] T003 리포지토리 (web 호출분만, D12) — findBySurveyId·tryMarkRefunded(환불 수렴 CAS, 릴레이 후처리용) + 참여자 findByFundingIdAndUserId·findByOrderId. tryConfirm·tryFail은 컨슈머 사본 리포지토리 소관(T008·T010) (build·ktlint·기동 검증 완료)
- [x] T004 이벤트 DTO 2종(contracts/events.md 그대로) + Kafka 토픽 2종 설정(파티션 3·복제 3·min ISR 2) in modules-web/support/.../cofunding/dto/event/, modules-web/produce/.../global/kafka/config/ (build·ktlint·기동 검증 완료)
- [x] T005 TossPaymentsAdapter.cancel(전액·Idempotency-Key·TossConfirmResult/TossPaymentResponse 재사용·ALREADY_CANCELED 성공 간주) + PaymentCommandRelay CANCEL 분기 + CANCEL settle 후처리(전이 tx: order CANCELED+참여자 REFUNDED / 판정 tx: tryMarkRefunded 단독 — 잠금 교차 제거, @Version 미도입) (build·ktlint·기동 검증 완료)

## Phase 4: 정방향 체인 (개시 → 결제 → 활성화)

- [ ] T006 CoFundingService — 개시(분담금 factory+참여자 명단·주문 일괄 INSERT+Survey PENDING_PAYMENT 한 트랜잭션)·내 주문 조회 + 최소 컨트롤러 2종(개시/내 주문 조회, doc 없이) in modules-web/produce/.../cofunding/service/, modules-web/web/.../cofunding/controller/
- [ ] T007 settle 확장(D6 전체) — FUNDING이면 SETTLED 전이+co-payment-settled Outbox 발행, FAILED/REFUNDED면 즉시 CANCEL 커맨드 적재(늦은 확정 보상), 한 트랜잭션 + CoFundingEventPublisher in modules-web/produce/.../payment/service/PaymentSettleService.kt, cofunding/publisher/
- [ ] T008 (consumer 모듈) 집계 리스너 — co-funding-consumer 모듈 부트스트랩(기존 컨슈머 보일러플레이트 복제) + JPA 엔티티·리포지토리 사본(CoFunding·참여자 + 슬림 Survey, D12) + co-payment-settled 소비, tryConfirm CAS 승자만 설문 활성화 CAS, 재수신·패배 no-op + DTO 사본(module-consumer/common) 동기화

## Phase 5: 보상 체인 (무산 → 부채꼴 환불 → 수렴)

- [ ] T010 (consumer 모듈) 환불 리스너 + 기한 스케줄러 — 리스너: failed 소비→DB 재조회→주문별 CANCEL 적재(PaymentCommand 사본, UNIQUE 흡수, 적재까지만 — 수렴 전이는 릴레이 후처리 소관) / 스케줄러: 만료 FUNDING 스캔→tryFail 승자만 failed 발행 (JPA 사본 리포지토리, D12)

## Phase 6: 종단 검증

- [ ] T011 종단 검증 + 문서 — k6/수동 시나리오 3종(전원 결제→활성화, 기한 만료→전원 환불 누락·이중 0, 경합: 동시 결제·마지막 결제 vs 마감) + ./gradlew build ktlintCheck + CLAUDE.md 동기화(cofunding·토픽 표·V9) in scripts/scenarios/saga/, CLAUDE.md

---

## Dependencies

- T003·T004·T005 병렬 가능 (T003은 T002 완료로 즉시 착수 가능)
- T006 ← T003 / T007 ← T004·T005·T006 / T008 ← T004·T007
- T010 ← T003·T004·T005
- T011 ← T006·T007·T008·T010

## 진행률 가중치 기준표 (계기판용, 합계 100%)

태스크 완료(verify 통과) 시점에 적산. 브리핑마다 `[전체 XX% | 이 단계 +Y%]` 표시.

| 태스크 | 가중치 | 누적 |
|---|---|---|
| T001 스키마 (완료) | 4 | 4 |
| T002 엔티티 (완료) | 4 | 8 |
| T003 리포지토리 CAS | 10 | 18 |
| T004 DTO+토픽 | 6 | 24 |
| T005 CANCEL 어댑터+릴레이 | 14 | 38 |
| T006 모금 서비스+API | 12 | 50 |
| T007 settle 확장(정방향+보상 분기) | 14 | 64 |
| T008 (consumer) 집계 리스너 | 14 | 78 |
| T010 (consumer) 환불 리스너+스케줄러 | 14 | 92 |
| T011 종단 검증+문서 | 8 | 100 |

배분 원칙: 사가 핵심(CAS·settle 분기·집계·환불 수렴·릴레이 CANCEL)에 10~12,
발행·설정·검증 마무리에 6~8.
