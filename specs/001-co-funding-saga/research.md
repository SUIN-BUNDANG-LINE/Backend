# Research: 공동 결제(더치페이) 설문 개설

**Feature**: 001-co-funding-saga | **Date**: 2026-07-26

기술 맥락의 미확정 지점을 결정으로 확정한다. 각 결정은 Decision / Rationale /
Alternatives 형식.

## D1. 주문-설문 관계: `payment_orders.survey_id` UNIQUE 해제

- **Decision**: Flyway V9에서 `payment_orders`의 `survey_id UNIQUE`를 일반
  인덱스로 완화한다. 공동 모금은 참여자별 주문 N건이 같은 설문을 가리킨다.
  주문 식별은 기존대로 `order_id UNIQUE`가 담당하고, "설문당 활성 주문 1건"
  불변식은 단독 결제 흐름의 애플리케이션 로직(기존 PENDING 재사용)이 유지한다.
- **Rationale**: 기존 단독 결제 스키마를 최소 변경으로 재사용하는 유일한 경로.
  주문 생성·confirm·relay·webhook 전체 파이프라인이 그대로 동작한다.
- **Alternatives**: (a) 공동 모금 전용 주문 테이블 신설 — 결제 파이프라인(릴레이·
  웹훅·장부)을 복제해야 해서 기각. (b) UNIQUE(survey_id, participant_id) 복합 —
  단독 결제는 participant가 없어 NULL 조합 문제, MySQL에 부분 유니크 없음 → 기각.

## D2. 설문 상태: `PENDING_PAYMENT` 재사용 + 모금 상세 상태는 `co_fundings`가 보유

- **Decision**: 모금 중 설문은 기존 `SurveyStatus.PENDING_PAYMENT`를 그대로 쓴다
  (응답 차단·미노출 로직 기존 보장). 모금 고유 생명주기(FUNDING → CONFIRMED /
  FAILED → REFUNDED)는 새 엔티티 `co_fundings.status`가 가진다.
- **Rationale**: SurveyStatus enum 확장은 exhaustive when 전수 보정 파급이 크고,
  "결제 대기 중"이라는 의미는 동일하다. 사가의 조건부 전이(CAS)는 모금 상태에서
  수행하는 것이 단독 결제 경로와의 간섭을 없앤다.
- **Alternatives**: SurveyStatus.FUNDING 신설 — when 분기 전수 수정 + 참여/노출
  판정 재검증 비용 대비 이득 없음 → 기각.

## D3. 이벤트 토폴로지 (Kafka 2토픽, 파티션 키 = 모금 ID)

- **Decision**:
  | 토픽 | 발행 주체 | 구독(co-funding-consumer 모듈) | 의미 |
  |---|---|---|---|
  | `co-payment-settled` | web(참여자 confirm 확정 시, Outbox) | 집계 리스너 | 참여자 1명 결제 확정 |
  | `co-funding-failed` | 기한 스케줄러(consumer) | 환불 리스너·알림 | 무산 확정(CAS 승자만 발행) |
  파티션 키는 모금 ID — 같은 모금의 이벤트를 한 파티션에 직렬화해 집계 병렬성을
  제거한다(1차 방어). 개설 확정 통지 토픽은 두지 않는다 — 설문 활성화는 집계
  리스너의 CAS 승자가 직접 수행하고, 구독자(알림)가 유예 상태라 통지 이벤트는
  수신자 없는 발행이 된다.
- **Rationale**: 발행은 기존 Outbox 경로(web) 재사용으로 원자성 보장. 컨슈머 측
  발행(스케줄러·집계)은 DB CAS가 선행돼 중복 발행이 하류 멱등으로 흡수된다.
- **Alternatives**: 단일 토픽 + 이벤트 타입 필드 — 컨슈머 관심사(집계 vs 환불)가
  달라 groupId 분리가 자연스러운 다토픽을 택함.

## D4. 장벽 판정: UNIQUE 확정 행 카운트 + 조건부 상태 전이(CAS)

- **Decision**: 전원 완료 판정은 `co_funding_participants`의 결제 확정 행(참여자당
  UNIQUE)을 세어, `UPDATE co_fundings SET status='CONFIRMED' WHERE id=? AND
  status='FUNDING' AND (확정 수 = 정원)` 형태의 조건부 전이로 결정한다. 갱신 행
  수 1을 얻은 호출자만 승자로서 설문을 활성화한다. 무산
  전이(`FUNDING → FAILED`)도 동일 CAS. 카운터 컬럼의 읽고-더하고-쓰기는 금지.
- **Rationale**: 갱신 유실(영구 미개설)과 중복 발화(이중 활성화·이중 환불)를
  DB 원자성으로 동시에 차단. 파티션 직렬화(1차)와 독립적으로 성립하는 2차 방어.
- **Alternatives**: Redisson 분산락으로 판정 직렬화 — 락 보유 중 장애 시 TTL까지
  판정 지연, CAS만으로 충분해 기각(락은 불변식이 아니라 성능 수단일 때만).

## D5. 환불: `PaymentCommandType.CANCEL` 신설 + 기존 릴레이 확장

- **Decision**: 환불은 `payment_commands`에 CANCEL 커맨드(전액, aggregate_id =
  orderId)를 적재하고 기존 `PaymentCommandRelay`가 command_type 분기로 토스
  `POST /v1/payments/{paymentKey}/cancel`(cancelAmount 생략 = 전액)을 호출해
  수렴시킨다. Idempotency-Key = `cancel:{orderId}`. 커맨드 멱등은
  UNIQUE(aggregate_id, command_type). 응답 삼분법(승인/미확정/명시적 거절)은
  confirm과 동일 — 결과 타입(`TossConfirmResult`)·응답
  DTO(`TossPaymentResponse`)·에러 파싱을 그대로 재사용하고,
  `ALREADY_CANCELED_PAYMENT`류는 성공으로 간주. CANCEL 승인 settle 후처리는
  짧은 트랜잭션 2개로 분리한다 — (전이 tx) 커맨드 CONFIRMED + 주문 CANCELED +
  해당 참여자 REFUNDED, (판정 tx) `tryMarkRefunded` 단독. 판정 tx는 참여자 행
  잠금을 쥐지 않은 채 수렴 서브쿼리를 읽으므로 마지막 두 환불이 동시 확정될 때의
  잠금 교차(데드락)가 구조적으로 사라지고, 자기 전이 커밋 후 판정하므로 마지막
  커밋자의 판정이 반드시 전원 REFUNDED를 본다(이중 승자는 status CAS가 차단,
  판정 누락은 릴레이 재시도가 복구). JPA @Version 낙관적 락은 도입하지 않는다 —
  조건부 UPDATE의 status 검사가 이미 낙관적 동시성 제어이며, 벌크 @Modifying은
  버전 메커니즘을 우회한다. 컨슈머 환불 리스너는 CANCEL 적재만 담당해 수렴
  책임이 한곳에 모인다. 유일한 예외는 결제자 0명 무산 — CANCEL 이 0건이라
  릴레이 후처리가 발동할 수 없으므로, 환불 리스너가 적재 대상 0건일 때에 한해
  직접 `tryMarkRefunded`(컨슈머 사본)로 종착 전이를 수행한다.
- **Rationale**: 기존 T2~T4에서 검증된 "커맨드 선적재 + 릴레이 수렴" 구조를
  그대로 확장. 부채꼴 환불 N건이 각각 독립 커맨드라 부분 진행·재개가 자동 성립
  (FR-013).
- **Alternatives**: 환불 전용 릴레이 신설 — SKIP LOCKED claim·retry 로직 중복
  → 기각. 부분취소 — 전액취소만 필요하므로 `PARTIAL_CANCELED` 상태 분리 불필요.

## D6. 무산 후 늦은 결제 승인 (FR-010)

- **Decision**: 참여자 결제의 settle 처리(동기 핸들러·릴레이 공통)가 확정 직전에
  모금 상태를 검사한다 — `FUNDING`이면 확정 행 기록 + `co-payment-settled` 발행,
  `FAILED/REFUNDED`면 확정 기록 없이 즉시 CANCEL 커맨드를 적재한다. 검사와 기록은
  한 트랜잭션이며, 검사는 `co_fundings` 행 잠금 조회(SELECT ... FOR UPDATE,
  `findByIdForUpdate`)로 무산 CAS(`tryFail`)와 직렬화한다 — 일반 읽기면 "검사 통과
  직후 무산 확정" 틈새에서 늦게 커밋된 SETTLED 를 환불 리스너의 1회성 재조회가
  놓치는 환불 누락 창이 열린다. 잠금 순서는 모든 트랜잭션이
  `co_fundings → participants` 한 방향이라 데드락 교차가 없다.
- **Rationale**: "무산 확정 vs 결제 확정" 경합의 결승선을 D4의 CAS와 같은 DB
  트랜잭션 경계로 통일 — 어느 쪽이 이기든 돈의 최종 귀속이 결정된다.
- **Alternatives**: 무산 후 결제 승인을 거부(confirm 안 함) — 토스 결제창에서
  이미 인증된 결제는 30분 내 confirm 안 하면 자동 만료되지만, 만료 전 승인 경로가
  존재하는 한 환불 수렴이 더 결정적 → 확정 후 즉시 환불을 택함.

## D7. 참여자 확정: 개설 시점 초대제 (명단 일괄 확정, FR-003)

- **Decision**: 참여자 명단은 개설자가 모금 개시 요청에 담고, 개설 트랜잭션에서
  참여자 행 N개를 일괄 INSERT로 확정한다(초대제). 별도 참여 등록 API·정원
  카운터·조건부 증가는 두지 않는다. 명단 내 중복 계정은
  UNIQUE(funding_id, user_id)가 차단한다(FR-004). V9의 `registered_count` 컬럼은
  미사용(DEFAULT 0 잔존, 엔티티 미매핑). 참여자별 결제 주문(`payment_orders`
  PENDING + `order_id`)도 같은 개설 트랜잭션에서 사전 발급한다 — 명단·금액이
  개설 시점에 전부 확정되므로 멱등 발급 로직 없이 조회만 남는다.
- **Rationale**: 이 피처의 학습 목표는 코레오그래피 보상 트랜잭션이다. 선착순
  등록의 정원 경합은 사가 본질과 무관한 경합 표면이라 설계에서 제거하는 것이
  가장 싸다. 명단이 개설 시점에 닫히므로 장벽 판정(D4)의 분모(capacity)도 개설
  이후 불변이 된다.
- **Alternatives**: 링크 공유 자율 등록(선착순) — 정원 카운터의 조건부 증가 CAS와
  등록·취소 API가 추가로 필요해 표면이 커짐 → 기각.

## D8. 모금 기한 스케줄러 (co-funding-consumer 모듈)

- **Decision**: `module-consumer/co-funding-consumer`에 @Scheduled 워커를 추가 —
  기한 경과 + `status='FUNDING'`인 모금을 SKIP LOCKED로 스캔해 무산 CAS(D4)를
  수행하고, 승자에 한해 `co-funding-failed`를 발행한다. 기존 시간 기반 자동 마감
  스케줄러 패턴 복제.
- **Rationale**: 스케줄러·워커는 컨슈머 모듈 담당이라는 모듈 경계 원칙(헌장 I)
  준수. CAS 선행으로 스캔 중복·재발행이 무해.
- **Alternatives**: web 측 @Scheduled — 헌장 I 위반(컨슈머·워커는 컨슈머 모듈)
  → 기각.

## D9. 관찰가능성 (헌장 IV)

- **Decision**: Micrometer 메트릭 신설 — 모금 상태 전이 카운터
  (`cofunding_transitions_total{to=confirmed|failed|refunded}`), 환불 수렴 시간
  히스토그램(무산 확정→전원 환불 완료), 미수렴 CANCEL 커맨드 gauge. Grafana에
  co-funding 대시보드 1장 추가. 트레이스는 OTel 자동 instrumentation에 위임.
- **Rationale**: SC-002(10분 내 전원 환불)·SC-003(모순 0건)을 운영 중 관측할 수
  있는 최소 신호.

## D10. 검증 전략 (헌장 III)

- **Decision**: JVM 통합 테스트 없이 — 도메인 단위 테스트(JUnit5: 분담금 산정,
  상태 전이, CAS 판정 로직) + k6 시나리오 4종(`scripts/scenarios/saga/`):
  S1 전원 결제 → 자동 개설, S2 기한 만료 → 부채꼴 전액 환불, S3 경합(동시 결제
  ×N, 마지막 결제 vs 마감), S4 중단·재개(릴레이 킬 후 수렴). 토스 테스트 키 사용.
- **Rationale**: 기존 saga-cost-integrity 등 k6 자산·러너 체계 재사용.

## D11. 컨슈머 측 이벤트 DTO 사본 동기화 (헌장 V)

- **Decision**: `co-payment-settled`·`co-funding-failed` payload DTO는 wire
  스키마 계약으로 양쪽이 사본을 보유한다. `modules-web`(`:support`
  `cofunding/dto/event/`)에 원본을 두고, contracts/events.md를 계약 문서로 삼아
  `module-consumer/common`에 동일 필드 사본을 작성한다(기존
  `DrawingCompletedEvent` 사본 관례와 동일).

## D12. 컨슈머 DB 접근: JPA 엔티티 사본 (기존 컨슈머 관례)

- **Decision**: co-funding-consumer는 기존 컨슈머 관례(SmsNotificationJobEntity
  사본)대로 JPA를 쓴다 — `CoFunding`·`CoFundingParticipant`(+상태 enum)·
  `PaymentCommand` 엔티티·리포지토리 사본을 모듈 안에 두고, 장벽 CAS
  `tryConfirm`·무산 CAS `tryFail`은 컨슈머 사본 리포지토리의 JPQL 조건부
  UPDATE(@Modifying, web T003과 동일 형태)로 작성한다. Survey는 도메인 로직이
  얽힌 원본 대신 활성화에 필요한 컬럼(id·status)만 매핑한 슬림 엔티티 사본 +
  `UPDATE ... SET status='IN_PROGRESS' WHERE id=:id AND
  status='PENDING_PAYMENT'` CAS 쿼리로 처리한다(JPA는 테이블 전체 컬럼 매핑을
  요구하지 않는다). 환불 수렴 CAS(`tryMarkRefunded`)는 여기 없다 — 릴레이
  settle 후처리(web) 담당(D5). CAS의 위치는 호출자를 따라 갈린다: web이 부르는
  것만 web 리포지토리에, 컨슈머가 부르는 것은 컨슈머 사본 리포지토리에.
- **Rationale**: 컨슈머 모듈 전체가 JPA 관례로 통일되어 있고(설정·감사·컨버터),
  같은 JPQL CAS 형태를 web과 공유해 인지 부담이 없다. 사본 파일이 늘어나는
  비용은 자족형 모듈 원칙(헌장 I)의 수용된 대가이며, 스키마 변경 시 동기화는
  D11 wire 계약과 같은 규율로 관리한다.
- **Alternatives**: JdbcTemplate 단일 클래스 — 사본 없이 1파일로 끝나지만
  모듈의 JPA 관례와 이질적이고 행 매핑·타입 변환을 수작업으로 지게 됨 → 기각.
