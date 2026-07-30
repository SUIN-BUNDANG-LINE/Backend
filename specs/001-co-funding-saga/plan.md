# Implementation Plan: 공동 결제(더치페이) 설문 개설

**Branch**: `001-co-funding-saga` | **Date**: 2026-07-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-co-funding-saga/spec.md`

## Summary

여러 메이커가 경품 비용을 나눠 결제하는 공동 모금으로 설문을 개설한다(참여자
명단은 개설 시점 초대제로 확정). 전원이 기한 내 결제하면 설문이 자동
활성화되고(all-or-nothing), 기한 만료 시
이미 승인된 결제를 전액 자동 환불하는 코레오그래피 사가로 구현한다. 기술 접근:
신규 `co_fundings`·`co_funding_participants` 엔티티 + 기존 결제 파이프라인
(Command Outbox·릴레이·웹훅 Inbox) 재사용 + Kafka 이벤트
2토픽(settled/failed) + 장벽·무산 판정은 조건부 상태 전이(CAS)로 경합 방어. 컨슈머(집계·
환불·기한 스케줄러)는 `module-consumer/co-funding-consumer` 신규 자족형 모듈에 배치한다.

## Technical Context

**Language/Version**: Kotlin 1.9.24, JDK 17

**Primary Dependencies**: Spring Boot 3.3.1, Spring Data JPA/Hibernate,
QueryDSL 5.1.0, Spring Kafka, Redisson 3.45.1(기존 — 본 피처는 CAS 우선, 락
미사용), RestTemplate(토스), Micrometer

**Storage**: MySQL 8.0 (Flyway V9 마이그레이션), Kafka(이벤트), Redis(기존 용도)

**Testing**: JUnit 5 + Mockito 단위 테스트, k6 시나리오(`scripts/scenarios/saga/`)
— JVM 통합 테스트 금지(헌장 III)

**Target Platform**: Linux 서버(K8s Deployment) — web 모듈 + 독립 consumer 모듈 배포

**Project Type**: web-service (Gradle 멀티 모듈 `:support` ← `:produce` ← `:web`
+ `module-consumer/` 자족형 컨슈머 모듈)

**Performance Goals**: 마지막 결제 확정 → 설문 활성화 1분 내(SC-001), 무산 확정
→ 전원 환불 10분 내(SC-002)

**Constraints**: 개설·무산 상호 배타 모순 0건(SC-003), 중단·재시작 후 100% 수렴
(SC-004), 이중 쓰기 금지·Outbox 필수(헌장 II)

**Scale/Scope**: 모금당 참여자 2~10명 수준, 신규 테이블 2 + 마이그레이션 1 +
토픽 2 + 컨슈머 2 + 스케줄러 1, REST 엔드포인트 3(현황은 유예)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 모듈 경계와 단방향 의존 | PASS | 엔티티·리포지토리·이벤트 DTO → `:support`, 서비스·publisher·릴레이 확장 → `:produce`, 컨트롤러 → `:web`, 리스너·스케줄러 → co-funding-consumer 모듈 (data-model.md 배치표) |
| II. 신뢰성 우선 상태 전이 | PASS | web 발신은 Outbox(settled), 수신 멱등(CAS 패배 no-op·CANCEL UNIQUE), 토스 응답 삼분법 유지, 조건부 전이로 이중 처리 흡수 (research D3~D6) |
| III. 품질 게이트 상시 통과 | PASS | JVM 통합 테스트 없음 — 단위 테스트 + k6 S1~S4 (research D10, quickstart.md) |
| IV. 관찰가능성 내장 | PASS | 전이 카운터·환불 수렴 히스토그램·미수렴 gauge + 대시보드 1장 (research D9) |
| V. 단순성과 문서 동기화 | PASS | 기존 결제 파이프라인 재사용(신규 릴레이·전용 주문 테이블 안 만듦), CLAUDE.md 갱신과 이벤트 DTO 사본 동기화를 작업 단위에 포함 (research D1·D5·D11) |

**Post-Phase 1 재점검**: 설계 산출물(data-model·contracts) 기준 재평가 — 위반
없음. Redisson 락을 쓰지 않고 CAS로 해결(D4·D7)해 락 남용도 없음. Complexity
Tracking 해당 없음.

## Project Structure

### Documentation (this feature)

```text
specs/001-co-funding-saga/
├── plan.md              # 이 파일
├── spec.md              # 기능 명세 (+Clarifications 3건)
├── research.md          # Phase 0 — 설계 결정 D1~D11
├── data-model.md        # Phase 1 — 테이블·상태 머신·불변식·V9
├── quickstart.md        # Phase 1 — 검증 시나리오 S1~S4
├── contracts/
│   ├── rest-api.md      # REST 6종 + 기존 결제 계약 재사용 명세
│   └── events.md        # Kafka 3토픽 wire 계약 + 멱등성 매트릭스
├── checklists/requirements.md
└── tasks.md             # Phase 2 — /speckit-tasks가 생성 (미생성)
```

### Source Code (repository root)

```text
modules-web/
├── support/src/main/kotlin/com/sbl/sulmun2yong/
│   ├── cofunding/
│   │   ├── entity/          # CoFunding, CoFundingParticipant (+상태 enum)
│   │   ├── repository/      # CoFundingRepository, CoFundingParticipantRepository
│   │   ├── dto/event/       # CoPaymentSettledEvent, CoFundingFailedEvent
│   │   └── exception/
│   └── payment/entity/      # PaymentCommandType에 CANCEL 추가
├── produce/src/main/kotlin/com/sbl/sulmun2yong/
│   ├── cofunding/service/   # CoFundingService(개시 — 명단·주문 일괄 확정, 내 주문 조회)
│   ├── cofunding/publisher/ # CoFundingEventPublisher (Outbox 경유)
│   └── payment/
│       ├── service/         # PaymentSettleService에 D6 분기(모금 상태 검사)
│       └── relay/           # PaymentCommandRelay에 CANCEL 분기 (+TossPaymentsAdapter.cancel)
├── web/src/main/kotlin/com/sbl/sulmun2yong/
│   └── cofunding/controller/ # 모금 개시·결제 진입 (현황은 유예)
└── web/src/main/resources/db/migration/
    └── V9__add_co_funding_tables.sql   # 신규 2테이블 + payment_orders UNIQUE 완화 + commands UNIQUE 추가

scripts/scenarios/saga/       # co-funding-*.js (S1~S4)
scripts/runners/              # 공동 모금 러너 추가
infra/monitoring/grafana/dashboards/  # co-funding 대시보드

# module-consumer/co-funding-consumer (신규 자족형 모듈, 기존 컨슈머 관례)
#   집계 리스너(co-funding-settlement), 환불 리스너(co-funding-refund),
#   기한 스케줄러, JPA 엔티티·리포지토리 사본(CoFunding·참여자·PaymentCommand
#   + 슬림 Survey, D12) — 이벤트 DTO 사본은 module-consumer/common(D11)
```

**Structure Decision**: 기존 도메인별 패키지 관례를 따라 신규 `cofunding/` 도메인
패키지를 3모듈에 분산 배치한다. 결제 파이프라인 변경은 기존 `payment/` 내 최소
확장(CANCEL 분기·D6 검사)에 그친다. 컨슈머 코드는 web 계열(modules-web)에 두지
않고 `module-consumer/co-funding-consumer` 자족형 모듈로 신설한다(헌장 I) —
DB 접근은 기존 컨슈머 관례대로 JPA 엔티티 사본(D12).

## Complexity Tracking

> Constitution Check 위반 없음 — 해당 사항 없음.
