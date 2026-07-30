<!--
Sync Impact Report
- Version change: (template) → 1.0.0 (최초 제정)
- Modified principles: 없음 (신규 작성)
- Added sections:
  - Core Principles (5개: 모듈 경계, 신뢰성 상태 전이, 품질 게이트, 관찰가능성, 단순성·문서 동기화)
  - 추가 제약 (Additional Constraints)
  - 개발 워크플로 (Development Workflow)
  - Governance
- Removed sections: 없음
- Templates:
  - ✅ .specify/templates/plan-template.md — Constitution Check 게이트가 본 원칙을 참조하도록 작성됨 (수정 불요, plan 작성 시점에 본 문서 기준으로 평가)
  - ✅ .specify/templates/spec-template.md — 명세 범위와 충돌 없음
  - ✅ .specify/templates/tasks-template.md — 태스크 분류와 충돌 없음
- Follow-up TODOs: 없음
-->

# sulmun2yong Backend Constitution

## Core Principles

### I. 모듈 경계와 단방향 의존 (NON-NEGOTIABLE)

Gradle 멀티 프로젝트의 의존 방향은 `:support` ← `:produce` ← `:web` 한 방향만 허용한다.

- 하위 모듈이 상위 모듈을 참조하는 순환은 금지한다. 여러 모듈이 참조하는 타입은 항상
  더 아래(공유) 모듈에 둔다.
- 코드 배치 기준: entity·repository·domain·공유 기반·비프로듀서 도메인 로직은 `:support`,
  프로듀서 도메인(drawing/survey/payment) service·publisher·relay·kafka·outbox·lock은
  `:produce`, 실행 진입점·보안·JWT·전역 config·컨트롤러(비프로듀서)는 `:web`.
- `kotlin("kapt")`가 있는 `:support`에는 메타-애노테이트된 애노테이션을 두지 않는다
  (kapt 스텁 생성 파손 — `@LoginUser`류는 `:produce`에 위치).
- KafkaListener·도메인 listener·`@Scheduled` 컨슈머 워커는 이 레포가 아닌
  `sulmoon2yong-consumer` 레포에 작성한다.

근거: 컴파일 타임 격리로 빈 경계·배포 단위를 보장하고, 순환 의존으로 인한 빌드·설계
붕괴를 원천 차단한다.

### II. 신뢰성 우선 상태 전이 (Outbox·Inbox·멱등성)

외부 시스템(PG·Kafka·SMS)과 DB 상태를 함께 바꾸는 모든 흐름은 이중 쓰기(dual-write)를
금지하고 신뢰성 패턴을 강제한다.

- 발신은 Outbox(또는 Command Outbox) 선적재 후 릴레이(@Scheduled + SKIP LOCKED)가
  재시도로 수렴시킨다. 핸들러가 도중에 죽어도 정확히 한 번 확정되어야 한다.
- 수신은 Inbox(고유키 UNIQUE)로 멱등 흡수한다. at-least-once 전달을 전제로 같은
  메시지 2회 수신 시 1회만 반영되어야 한다.
- 외부 호출 결과는 성공/실패 이분법이 아니라 승인됨/미확정/명시적 거절 삼분법으로
  다룬다. 타임아웃·5xx는 미확정으로 취급해 재시도(멱등키 보호) 대상으로 남긴다.
- 금전·추첨 등 정합성이 걸린 전이는 조건부 상태 전이(이미 확정이면 skip)로 이중
  처리를 흡수한다.

근거: 결제·추첨·알림 도메인은 부분 실패 시 돈/당첨 불일치가 발생하는 영역이며,
패턴 없는 임기응변 재시도는 이중 결제·유실로 이어진다.

### III. 품질 게이트 상시 통과

`./gradlew build`와 `./gradlew ktlintCheck`는 어느 커밋에서도 성공하는 상태를
유지한다(NON-NEGOTIABLE).

- Spring 런타임을 띄우는 JVM 통합 테스트는 작성하지 않는다. 런타임 검증은
  `scripts/scenarios/`의 k6 시나리오와 `scripts/runners/`의 실행 스크립트로 대체한다.
- Consumer end-to-end 검증(Kafka listener 처리)은 `sulmoon2yong-consumer` 레포에서
  작성한다.
- 도메인 로직 단위 테스트는 JUnit 5로 작성하며, 검증 기준이 명시된 수락 조건을
  태스크마다 정의한다.

근거: 빌드가 깨진 브랜치는 모든 후속 작업을 막고, 런타임 검증을 k6로 일원화한
기존 결정과 충돌하는 통합 테스트는 유지 비용만 늘린다.

### IV. 관찰가능성 내장

새 도메인 흐름은 코드와 함께 관찰 수단을 배송한다.

- 핵심 상태 전이·실패 경로에는 Micrometer 메트릭(도메인 metrics 패키지)을 추가하고
  Prometheus/Grafana 대시보드에 반영한다.
- 분산 추적은 OTel Java Agent 자동 instrumentation을 전제로 하되, Kafka 경계의
  W3C tracecontext 전파를 깨뜨리는 수동 개입을 금지한다.
- 상관관계 추적이 필요한 흐름은 X-Correlation-Id(MDC) 체계를 따른다.

근거: Outbox·릴레이·컨슈머로 흐름이 비동기 분산되어 있어, 메트릭·트레이스 없이는
수렴 실패를 운영 중에 발견할 수 없다.

### V. 단순성과 문서 동기화

요청 범위를 벗어난 코드를 만들지 않고, 코드와 문서를 같은 작업 단위로 움직인다.

- 요청되지 않은 기능·추측성 유연성·단일 사용 추상화를 금지한다(YAGNI).
- 기술 스택·아키텍처·환경변수·명령어가 바뀌면 같은 커밋에서 CLAUDE.md의 해당
  섹션을 갱신한다. 문서 갱신 누락 커밋은 허용하지 않는다.
- consumer 레포와 공유되는 Kafka 이벤트 DTO(wire 스키마 계약)를 변경하면 양쪽
  레포를 동기화한다.

근거: 문서-코드 불일치는 다음 세션·다음 개발자의 판단을 오염시키며, 과잉 설계는
3모듈 경계 유지 비용을 기하급수적으로 늘린다.

## 추가 제약 (Additional Constraints)

- 기술 스택: Kotlin 1.9.24 / Spring Boot 3.3.1 / JDK 17 / MySQL 8.0(JPA·QueryDSL) /
  Redis(Redisson) / Kafka. 스택 변경은 CLAUDE.md 갱신과 동일 작업 단위로만 허용.
- 환경 변수·비밀값을 코드에 하드코딩하지 않는다. `application.yml` + `@Value` 또는
  `@ConfigurationProperties`를 사용하고, 비밀값은 `application-secret.yml`(Git 미추적)에 둔다.
- DB 스키마 변경은 Flyway 마이그레이션(`db/migration/`)으로만 수행한다. 엔티티와
  DDL은 1:1 일치해야 한다.
- 분산 락은 Redisson 기반 `global/lock`의 AOP 애노테이션을 사용한다.
- 외부 시스템 통신은 어댑터 계층(`{도메인}/adapter/`)으로 격리한다.

## 개발 워크플로 (Development Workflow)

- 새 기능은 Spec Kit 파이프라인을 따른다: `/speckit-specify` → `/speckit-clarify` →
  `/speckit-plan` → `/speckit-tasks` → `/speckit-shrimp-sync`(shrimp 보드 시딩).
- 실행 추적의 진실은 shrimp-task-manager다. 태스크는 `execute_task` → 구현 →
  `verify_task`(80점 이상 자동 완료) 루프로 진행하며, 보드를 거치지 않은 임의
  진행을 금지한다.
- Phase 경계마다 품질 게이트(`build`·`ktlintCheck`)를 통과시키고 git 커밋을 남긴다.
  커밋은 `/commit` 스킬(이모지 컨벤셔널 포맷)을 사용한다.
- 코드 주석과 문서는 한국어를 기본으로 한다.

## Governance

- 본 constitution은 다른 관행·가이드 문서보다 우선한다. 원칙과 충돌하는 구현은
  plan 단계의 Constitution Check에서 정당화되지 않는 한 진행할 수 없다.
- 개정 절차: 개정안을 커밋(PR)으로 제안하고, Sync Impact Report를 본 문서 상단
  주석에 갱신하며, 의존 템플릿(plan/spec/tasks)과의 정합성을 함께 검증한다.
- 버전 정책(semver): 원칙 제거·재정의 등 하위 호환 파괴는 MAJOR, 원칙·섹션 추가나
  실질적 확장은 MINOR, 표현 명확화·오타는 PATCH.
- 준수 검토: 모든 피처의 plan.md는 Constitution Check 게이트에서 원칙 위반 여부를
  명시하고, 위반 시 Complexity Tracking에 정당화를 기록해야 한다.

**Version**: 1.0.0 | **Ratified**: 2026-07-26 | **Last Amended**: 2026-07-26
