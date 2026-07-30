# sulmun2yong Backend

## 프로젝트 개요

설문이용(sulmun2yong) — 설문조사 생성·배포·응답 수집·추첨 기능을 제공하는 웹 서비스의 백엔드 API 서버.

## 기술 스택

| 분류 | 기술 |
|---|---|
| 언어 | Kotlin 1.9.24 |
| 프레임워크 | Spring Boot 3.3.1 |
| 빌드 | Gradle Kotlin DSL (build.gradle.kts) |
| JDK | 17 |
| 데이터베이스 | MySQL 8.0 (Spring Data JPA + Hibernate) |
| ORM | JPA/Hibernate + QueryDSL 5.1.0 |
| 마이그레이션 | Flyway (`modules-web/web/src/main/resources/db/migration/`, V1~V10) |
| 캐시/분산락 | Redis (Spring Data Redis) + Redisson 3.45.1 |
| 인증 | Spring Security + OAuth2 + JWT (jjwt 0.12.6) |
| API 문서 | SpringDoc OpenAPI 2.3.0 (Swagger UI) |
| 파일 저장 | AWS S3 (AWS SDK 2.27.24) + CloudFront CDN |
| 테스트 | JUnit 5 + Mockito + Jacoco |
| 포맷터 | ktlint 12.1.1 (Gradle 플러그인: org.jlleitschuh.gradle.ktlint) |
| 컨테이너 | JIB (Google Cloud Tools) |
| AOP | Spring Boot AOP |
| 관찰가능성 | Prometheus + Grafana 11.2 + Tempo 2.6 + OpenTelemetry Java Agent (자동 instrumentation, W3C tracecontext) |

## 명령어 (Scripts)

| 명령어 | 설명 |
|---|---|
| `./gradlew build` | 전체 모듈 빌드 + 테스트 실행 |
| `./gradlew test` | 전체 단위 테스트 실행 |
| `./gradlew :web:test` | web 모듈 테스트만 실행 |
| `./gradlew ktlintCheck` | 코드 스타일 검사 (전체 모듈) |
| `./gradlew ktlintFormat` | 코드 스타일 자동 수정 (전체 모듈) |
| `./gradlew jacocoTestReport` | 테스트 커버리지 리포트 생성 |
| `./gradlew :web:bootRun` | Web 진입점 로컬 실행 |
| `./gradlew :web:bootJar` | Web JAR 패키징 (`modules-web/web/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :web:jib` | Web Docker 이미지 빌드 및 푸시 |

## 실행 단위 (Entry Points)

Gradle 멀티 프로젝트로 빈 경계를 보장한다. 프로듀서/웹 계열(`modules-web/`)은 `:support`, `:produce`, `:web` — 의존 방향은 `:support` ← `:produce` ← `:web` 선형(순환 없음, `:web`은 `:support`도 직접 의존).
- `:support` — 도메인/엔티티/리포지토리 + 공유 기반(global error·data·util·converter, oauth2 provider) + 비프로듀서 도메인 로직(ai/aws/notification/user). 기반 라이브러리.
- `:produce` — Kafka 를 produce 하는 도메인(drawing/survey)의 서비스/퍼블리셔/컨트롤러 + outbox·publisher·kafka config + 분산락(global/lock) + `@LoginUser` 등 인증 애노테이션. 라이브러리.
- `:web` — 실행 진입점(`Sulmun2yongApplication`) + 비프로듀서 도메인 컨트롤러(ai/aws/user) + 보안/JWT/resolver/전역 config·예외 핸들러. 유일한 실행 모듈.

컨슈머 계열(`module-consumer/`)은 `:common`(컨슈머 공유 DTO 라이브러리 — `:support`와 별개) + `:drawing-sms-notification-consumer` + `:dlt-sms-notification-consumer` + `:co-funding-consumer`. 각 컨슈머는 `:common`에만 의존하며, 각자 SpringBootApplication/bootJar/이미지를 가진 MSA 자족형 독립 서비스다. 컨슈머의 DB 접근은 JPA 슬림 사본(만지는 컬럼만 매핑, 스키마 주인은 web의 Flyway·컨슈머는 validate만)이다.

| 진입점 | 모듈 | 클래스 | 책임 |
|---|---|---|---|
| Web | `:web` | `Sulmun2yongApplication` | REST API 서버, Outbox Producer relay, Payment Command Relay(토스 confirm 자력 발송), Spring Security/JWT, OAuth2, Swagger, ai 헬스체크 스케줄러 |

Consumer 진입점은 3개 — `SmsNotificationConsumerApplication`(drawing-completed 구독, Kafka 어댑터·도메인 listener·SMS 보상 워커·Notification 메트릭), `DltSmsNotificationConsumerApplication`(DLT 구독·적재, DLT 메트릭), `CoFundingConsumerApplication`(co-payment-settled·co-funding-failed 구독 — 장벽 집계·환불 팬아웃·기한 만료 무산 스케줄러). 진입점 클래스는 패키지 루트(`com.sbl.sulmun2yong`) 배치가 관례이며, `module-consumer/` 각 모듈에서 관리한다.

### 실행 방법

| 환경 | 실행 명령 |
|---|---|
| 로컬 — Web | `./gradlew :web:bootRun` |
| 패키징 — Web | `./gradlew :web:bootJar` → `modules-web/web/build/libs/web-*-SNAPSHOT.jar` |
| 운영 — Web | `java -jar web-*.jar` |

배포 시 Web JAR을 이미지로 만들어 Deployment로 운영한다 (`{DOCKER_IMAGE_NAME}-web`). Consumer 이미지는 `module-consumer/` 각 모듈의 JIB 설정으로 빌드한다. Actuator 엔드포인트(`/management/health`, `/management/prometheus`)는 양쪽 모두 동일하게 노출되어 K8s HTTP probe로 헬스체크 가능.

### 통합 테스트

Spring 런타임을 띄우는 JVM 통합 테스트는 제거되었고, 런타임 검증은 `scripts/scenarios/`의 k6 시나리오로 대체한다 (concurrency: drawing-concurrency·drawing-load·skip-locked, outbox: atomicity·relay-recovery, kafka: drawing-kafka-fanout, saga: cost-integrity).
테스트 실행 진입점 `.sh`(k6 통합 러너·분산락 실험·consumer E2E/부하·브로커 비교 러너)는 `scripts/runners/` 에 모여 있고 레포 루트에서 실행한다. k6 자산(바이너리·시나리오·lib)은 `scripts/{bin,scenarios,lib}/`, consumer E2E/부하 하네스(compose·override·dashboard)와 브로커 비교 자산은 `tests/e2e/`·`tests/broker-comparison/` 아래에 둔다 — 컨슈머 jar 만 `module-consumer/<consumer>/build/libs/` 에서 `bootJar` 로 빌드해 마운트한다.
Consumer end-to-end 흐름(Kafka listener 처리 검증)은 `module-consumer/` 각 모듈의 단위 테스트와 `tests/e2e/` 하네스로 검증한다.

### 모듈에 새 코드 추가 시 주의

- 새 **컨트롤러**: `web/src/main/kotlin/.../{도메인}/controller/`
- 새 **KafkaListener / 도메인 listener / @Scheduled worker**: `module-consumer/<consumer>/src/main/kotlin/.../`
- 새 **entity / repository / domain / 공유 기반 util** 또는 **비프로듀서 도메인(ai/aws/notification/user) 로직**: `support/src/main/kotlin/.../{도메인}/`
- 새 **프로듀서 도메인(drawing/survey)의 service / publisher / controller** 또는 **kafka·outbox·lock**: `produce/src/main/kotlin/.../{도메인}/`
- 새 **비프로듀서 도메인 컨트롤러 / 보안·JWT·resolver·전역 config**: `web/src/main/kotlin/.../{도메인}/`
- 새 **payment(결제) 코드**는 3모듈 분산: entity·repository·adapter·dto → `:support`, service·relay → `:produce`, controller·`RestTemplateConfig`(toss 빈) → `:web`. 토스 API 통신은 `payment/adapter/`(TossPaymentsAdapter)로 격리한다
- 판단 기준: 의존은 `:support` ← `:produce` ← `:web` 한 방향만. 하위 모듈이 상위를 참조하면 순환이므로, 참조당하는 타입은 항상 더 아래(공유) 모듈에 둔다. `kotlin("kapt")`가 있는 `:support`에는 메타-애노테이트된 애노테이션(@AuthenticationPrincipal 파생 등)을 두지 말 것 — kapt 스텁 생성이 깨진다(그래서 `@LoginUser`류는 `:produce`에 있다)
- consumer 측과 공유되는 Kafka 이벤트 DTO(`DrawingCompletedEvent`)는 `modules-web`(`:support`)과 `module-consumer`(`:common`)가 각자 사본을 보유 — wire 스키마 계약이므로 코드 변경 시 양쪽 동기화 필요

## 아키텍처

Gradle 멀티 프로젝트 — 프로듀서/웹 계열(`modules-web/`)은 `:support`(기반 라이브러리), `:produce`(프로듀서 도메인), `:web`(실행 진입점) 3개 모듈로 컴파일 타임 격리. `:support` ← `:produce` ← `:web` 단방향 의존.
컨슈머 계열은 `module-consumer/`(`:common` + 컨슈머 2개)로 같은 빌드에 포함된다.

```
{module}/src/main/kotlin/com/sbl/sulmun2yong/
├── Sulmun2yongApplication.kt          # :web 진입점
├── ai/                          # AI 설문 생성 도메인
│   ├── adapter/                 # 외부 시스템 어댑터 (AI 서버 통신)
│   ├── controller/              # REST 컨트롤러
│   │   └── doc/                 # Swagger API 문서 인터페이스
│   ├── domain/                  # 도메인 모델 (AIGeneratedSurvey, PythonFormatted*)
│   ├── dto/                     # 요청/응답 DTO
│   │   ├── python/              # AI 서버 통신용 DTO
│   │   └── request/
│   ├── entity/                  # JPA 엔티티 + 도메인 로직 통합
│   ├── exception/               # 도메인 예외
│   ├── repository/              # 데이터 접근 계층
│   ├── scheduler/               # 스케줄러
│   └── service/                 # 비즈니스 로직
├── aws/                         # AWS 연동 (S3 파일 업로드)
│   ├── controller/
│   ├── dto/
│   └── service/
├── cofunding/                   # 공동 모금(더치페이) 도메인 (코레오그래피 사가 — specs/001-co-funding-saga/)
│   ├── entity/·exception/·repository/·dto/  # CoFunding·CoFundingParticipant(초대제·주문 사전 발급) + tryMarkRefunded 수렴 CAS + 요청/응답·사가 이벤트 DTO (:support)
│   ├── service/·publisher/      # CoFundingService(개시·내 주문 조회), CoFundingEventPublisher(settled Outbox 발행) (:produce)
│   └── controller/              # 모금 개시·내 주문 조회 (:web) ※ 장벽 CAS·환불 팬아웃·기한 스케줄러는 co-funding-consumer 모듈
├── drawing/                     # 추첨 도메인
│   ├── controller/
│   ├── domain/
│   │   ├── drawingResult/
│   │   └── ticket/
│   ├── dto/
│   │   └── event/               # 도메인 이벤트 DTO (DrawingCompletedEvent, *ConsumedEvent 등)
│   ├── entity/                  # JPA 엔티티 + 도메인 로직 통합
│   ├── exception/
│   ├── metrics/                 # 도메인 메트릭 (DrawingProcessMetrics — winner/non_winner persistence, 낙관락 시도 결과)
│   ├── publisher/               # Kafka 발행 단일 진입점 (DrawingEventPublisher — Outbox 저장 + ApplicationEvent)
│   ├── repository/
│   └── service/
│                                # ※ listener/ 는 module-consumer 컨슈머 모듈에 위치
├── global/                      # 공통 모듈
│   ├── annotation/              # 커스텀 어노테이션
│   ├── config/                  # 설정 클래스
│   │   └── oauth2/              # OAuth2 설정
│   │       ├── handler/
│   │       └── provider/
│   ├── converter/               # 타입 변환기
│   ├── data/                    # 공통 데이터 클래스
│   ├── entity/                  # 공통 엔티티
│   ├── error/                   # 전역 예외 처리
│   ├── jwt/                     # JWT 처리
│   ├── kafka/                   # Kafka 인프라 (Producer/Consumer/Topic Config, Outbox)
│   │   ├── config/              # Kafka 설정
│   │   ├── consumer/            # Consumer 어댑터 공통 인프라 (KafkaAckEvent, KafkaAckEventListener, CorrelationIdRecordInterceptor)
│   │   ├── outbox/              # Outbox 패턴 (엔티티, 팩토리, 리스너, Relay)
│   │   └── publisher/           # KafkaEventPublisher
│   ├── filter/                  # HTTP Filter (CorrelationIdFilter — X-Correlation-Id 헤더 + MDC 발급)
│   ├── lock/                    # 분산 락 (Redisson)
│   │   └── metrics/             # Lock 획득/대기 Histogram (DrawingLockMetrics)
│   ├── metrics/                 # 전역 메트릭 (DeadlockMetrics — MySQL deadlock 카운터, OptimisticLockMetrics — 낙관락 충돌 카운터)
│   ├── resolver/                # 아규먼트 리졸버
│   ├── util/                    # 유틸리티
│   └── validator/               # 검증 로직
├── notification/                # 알림 도메인 (Inbox 패턴 + DLT)
│   ├── dto/event/               # 도메인 이벤트 DTO (DltSmsNotificationEvent, *ConsumedEvent, SmsJobCreatedEvent)
│   ├── entity/                  # sms_notification_jobs, dlt_messages 엔티티
│   ├── metrics/                 # SMS Job/DLT/Attempts 메트릭 (SmsNotificationMetrics)
│   ├── repository/              # SmsNotificationJobRepository, DltMessageRepository
│   └── service/                 # SmsSender + SmsNotificationJobService
│                                # ※ listener/, worker/ 는 module-consumer 컨슈머 모듈에 위치
├── payment/                     # 결제 도메인 (토스페이먼츠 카드결제 — Command Outbox + Webhook Inbox)
│   ├── adapter/                 # TossPaymentsAdapter(confirm/cancel/getOrder), TossConfirmResult(삼분법 sealed)
│   ├── controller/              # 결제 success/fail 착지·checkout-info·webhook (:web)
│   ├── dto/                     # Toss confirm/webhook 요청·응답 DTO
│   ├── entity/                  # payment_orders(장부)·payment_commands(Outbox)·payment_webhook_inbox
│   ├── relay/                   # PaymentCommandRelay (@Scheduled + SKIP LOCKED 자력 발송, :produce)
│   ├── repository/
│   └── service/                 # PaymentConfirmService(오케스트레이터·HTTP) ↔ PaymentSettleService(짧은 tx) + PaymentFailService·PaymentWebhookService
├── survey/                      # 설문조사 도메인 (핵심)
│   ├── controller/
│   ├── domain/
│   │   ├── question/            # 질문 도메인
│   │   ├── response/            # 응답 도메인
│   │   ├── result/              # 결과 분석
│   │   ├── reward/              # 리워드
│   │   ├── routing/             # 라우팅 전략
│   │   └── section/             # 섹션
│   ├── dto/
│   │   └── event/               # 도메인 이벤트 DTO (*ConsumedEvent 등)
│   ├── entity/
│   ├── exception/
│   ├── repository/
│   ├── scheduler/
│   └── service/
│                                # ※ listener/ 는 module-consumer 컨슈머 모듈에 위치
└── user/                        # 사용자 도메인
    ├── controller/
    ├── domain/
    ├── dto/
    ├── entity/
    ├── exception/
    ├── repository/
    ├── service/
    └── util/
```

### 패키지별 역할

| 패키지 | 역할 |
|---|---|
| `controller/` | REST API 엔드포인트 정의 |
| `controller/doc/` | Swagger 문서용 인터페이스 |
| `domain/` | 도메인 모델 (enum, sealed class, 값 객체, 라우팅 전략 등) |
| `dto/` | 요청/응답 데이터 전송 객체 |
| `dto/event/` | 이벤트 DTO (Kafka 토픽 payload + 도메인 ApplicationEvent payload `*ConsumedEvent`) |
| `entity/` | JPA 엔티티 + 도메인 로직 통합 클래스 (`@Entity`) |
| `adapter/` | 외부 시스템 연동 어댑터 (ai: AI 서버·Redis 통신, payment: 토스페이먼츠 API 통신) |
| `repository/` | Spring Data JPA 리포지토리 |
| `service/` | 비즈니스 로직 서비스 |
| `exception/` | 도메인별 커스텀 예외 |

### Kafka 컨슈머 구조 (ApplicationEvent 기반)

Web 측에서는 Producer / Outbox Relay만 동작하며, Kafka Consumer는 `module-consumer/` 컨슈머 모듈에서 관리한다.

| 토픽 | groupId | Consumer 어댑터 (module-consumer) | 도메인 리스너 (module-consumer) |
|---|---|---|---|
| `drawing-completed` | `drawing-notification` | `DrawingCompletedNotificationKafkaListener` (`ConsumerSeekAware` 리플레이) | `drawing.DrawingSmsNotificationEventListener` (SMS 잡 생성) |
| `sms-delivery-permanently-failed` | — | 발행만 존재 (dlt 컨슈머가 발행, 현재 구독자 없음 — PG 정산 사가의 환불 리스너가 구독 예정, `docs/kafka-distribute-lock/PRD.md` 참조) | — |
| `drawing-notification.DLT` | `dlt-sms-notification` | `DltSmsNotificationKafkaListener` | `notification.DltMessageEventListener` |
| `co-payment-settled` | `co-funding-settlement` | `CoPaymentSettledKafkaListener` | `cofunding.CoFundingSettlementEventListener` (장벽 tryConfirm 승자만 설문 활성화) |
| `co-funding-failed` | `co-funding-refund` | `CoFundingFailedKafkaListener` | `cofunding.CoFundingRefundEventListener` (SETTLED 재조회 → CANCEL 팬아웃, 0명 무산만 직접 종착) |

co-funding 사가의 무산 트리거는 기한 만료 하나 — `CoFundingDeadlineScheduler`(co-funding-consumer)가 만료 FUNDING 을 SKIP LOCKED 스캔해 tryFail 승자만 `co-funding-failed` 를 직접 발행한다(발행은 tx 밖). 환불 실행·FAILED→REFUNDED 수렴은 web `PaymentCommandRelay` 의 CANCEL 후처리(전이 tx/판정 tx 분리) 소관. 종단 검증: `scripts/scenarios/saga/co-funding-saga-verify.sh` (주입+관찰, 컨슈머 기동 필요).

티켓 소진 시 설문 자동 종료는 응답 일관성을 위해 동기 처리 — `DrawingProcessService.closeSurveyIfTicketsExhausted`가 추첨 트랜잭션 안에서 직접 수행한다 (Kafka fan-out 대상 아님).

### 관찰가능성 (Observability) 스택

LGTM 중 **G(Grafana) + M(Metrics) + T(Tracing)** 활성. Logs(Loki)는 후속 단계.

```
infra/
├── otel/
│   └── opentelemetry-javaagent.jar   # JVM 부착용 OTel Java Agent (-javaagent 옵션)
└── monitoring/
    ├── prometheus/prometheus.yml      # spring-apps(web 2)·sms-consumers(컨슈머 2)·kafka/mysqld-exporter 스크레이프
    ├── prometheus/saga-alerts.yml     # SMS 사가 알림 규칙 (StuckPendingSaga 등, rule_files 로 로드)
    ├── tempo/tempo.yml                # OTLP receivers(4317/4318), 로컬 storage, 14일 retention
    └── grafana/
        ├── provisioning/
        │   ├── datasources/
        │   │   ├── prometheus.yml     # uid=prometheus
        │   │   └── tempo.yml          # uid=tempo (serviceMap → prometheus 연결)
        │   └── dashboards/            # 대시보드 자동 로드
        └── dashboards/                # outbox/sms-failover/drawing-lock/consumer-fanout/cluster-overview/race-comparison
```

| 컴포넌트 | 컨테이너 | 호스트 포트 | 역할 |
|---|---|---|---|
| Prometheus | `sulmun2yong-cluster-prometheus` | `19090` | 메트릭 수집 (15s scrape, 7d retention) |
| Grafana | `sulmun2yong-cluster-grafana` | `13000` | 대시보드 + Explore (datasource provisioning) |
| Tempo | `sulmun2yong-cluster-tempo` | `13200` (query), `14317` (OTLP gRPC) | 분산 trace 저장·질의 |
| Kafka Exporter | `sulmun2yong-cluster-kafka-exporter` | `19308` | Kafka consumer lag 메트릭 |
| MySQL Exporter | `sulmun2yong-cluster-mysqld-exporter` | `19104` | InnoDB 행 락 대기·데드락·롤백 (분산락 전-후 DB 부하 비교) |

OTel Java Agent는 web 2개 JVM에 `-javaagent` 옵션으로 부착되어 Spring MVC, JDBC, Spring Kafka, Redisson, Hibernate, HikariCP 등을 **코드 수정 없이 자동 instrument**. trace는 OTLP gRPC로 Tempo에 push되고 W3C tracecontext 헤더(`traceparent`)로 Kafka 메시지 경계를 자동 전파. consumer 측 trace도 동일하게 Tempo로 push 가능 (동일 클러스터 공유).

### 프로젝트 문서

```
docs/                                   # Git 미추적(.gitignore) 로컬 문서
├── kafka-distribute-lock/              # 도메인 PRD·시나리오·중복요청·EVENT-GLOSSARY·토스 API 참조
├── broker-docker-compose/              # Kafka 브로커 1~3대 비교용 docker-compose
└── vsRABBITMQ.md·vsREDIS.md·카프카브로커옵션들.md + 결제 아키텍처 SVG 다이어그램
```

## 환경 변수

`application-secret.yml`에서 관리 (Git 미추적). `docker-compose.yml`에서 `.env` 파일 참조.

| 변수 | 용도 |
|---|---|
| `MYSQL_*` | MySQL 연결 정보 |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `toss.client-key` | 토스페이먼츠 클라이언트 키 (결제창 SDK용, 프론트 노출) |
| `toss.secret-key` | 토스페이먼츠 시크릿 키 (백엔드 confirm/cancel Basic Auth용) |
| `toss.base-url` | 토스페이먼츠 결제 API 주소 (기본 `https://api.tosspayments.com`) |
| `TEST_AUTH_ENABLED` | 부하테스트용 엔드포인트 활성화 (JWT 발급·설문 활성화, 기본 false) |
| `TX_ISOLATION` | 트랜잭션 격리수준 실험용 (HikariCP, 기본 TRANSACTION_REPEATABLE_READ) |

### 외부 서비스 URL (application.yml)

| 설정 | 기본값 | 용도 |
|---|---|---|
| `frontend.base-url` | `http://localhost:3000` | 프론트엔드 |
| `backend.base-url` | `http://localhost:8080` | 백엔드 |
| `ai-server.base-url` | `http://localhost:8000` | AI 서버 |
| `cloudfront.base-url` | `https://file.sulmoon.io` | CDN |
| `cookie.domain` | `localhost` | 쿠키 도메인 |
| `payment.reward-unit-price` | `2000` | 경품 1개당 단가(원) — 결제 금액 = 단가 × 경품 수 |

## 개발 도구 및 설정

| 도구 | 설정 |
|---|---|
| 패키지 매니저 | Gradle 8.x (Wrapper) |
| JDK | 17 (Temurin) |
| 포맷터 | ktlint (`./gradlew ktlintFormat`) |
| 포맷 검사 | ktlint (`./gradlew ktlintCheck`) |
| 테스트 커버리지 | Jacoco |
| CI | GitHub Actions (PR CI + 배포) |
| 컨테이너 빌드 | JIB |
| 로컬 인프라 | Docker Compose (MySQL·Redis·web×2 + Prometheus/Grafana/Tempo/exporters) |

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/001-co-funding-saga/plan.md`
<!-- SPECKIT END -->
