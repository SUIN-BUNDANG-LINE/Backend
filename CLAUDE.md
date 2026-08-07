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
| 마이그레이션 | Flyway (`module-survey-drawing/src/main/resources/db/migration/`, V1~V10) |
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
| `./gradlew :survey-drawing:test` | web 모듈 테스트만 실행 |
| `./gradlew ktlintCheck` | 코드 스타일 검사 (전체 모듈) |
| `./gradlew ktlintFormat` | 코드 스타일 자동 수정 (전체 모듈) |
| `./gradlew jacocoTestReport` | 테스트 커버리지 리포트 생성 |
| `./gradlew :survey-drawing:bootRun` | Web 진입점 로컬 실행 |
| `./gradlew :survey-drawing:bootJar` | Web JAR 패키징 (`module-survey-drawing/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :survey-drawing:jib` | Web Docker 이미지 빌드 및 푸시 |

## 실행 단위 (Entry Points)

Gradle 멀티 프로젝트로 빈 경계를 보장한다. 모든 도메인 서비스는 최상위 단일 모듈(`module-*/`)이며 공유 기반 `:support`(`module-support/`)에만 의존한다.
- `:support`(`module-support/`) — 도메인/엔티티/리포지토리 + 공유 기반(global error·data·util·converter, kafka·outbox·분산락 인프라, oauth2 provider) + notification 도메인 로직. 기반 라이브러리 (bootJar 비활성). (ai/aws/user 서비스 계층은 `:survey-drawing` 소속 — 데이터 계층만 여기)
- `:survey-drawing`(`module-survey-drawing/`) — 실행 진입점(`Sulmun2yongApplication`) + 설문/추첨 도메인(서비스·퍼블리셔·컨트롤러·사가 리스너) + `@LoginUser` 등 인증 애노테이션 + 비프로듀서 도메인(ai/aws/user)의 컨트롤러·서비스·어댑터 + 보안/JWT/resolver/전역 config·예외 핸들러.

컨슈머 계열은 `:common`(`module-common/` — 컨슈머 공유 DTO 라이브러리, `:support`와 별개) + `:drawing-sms-notification-consumer`(`module-drawing-sms-notification-consumer/`) + `:dlt-sms-notification-consumer`(`module-dlt-sms-notification-consumer/`). 각 컨슈머는 `:common`에만 의존하며, 각자 SpringBootApplication/bootJar/이미지를 가진 MSA 자족형 독립 서비스다. (`:co-funding-consumer`는 구조변경 Phase 3에서 `module-cofunding`으로 흡수·해체)

도메인 서비스 계열(구조변경, `docs/kafka-distribute-lock/구조변경-실행계획.md`): `module-gateway`(8000, 라우팅·JWT 검증) · `module-auth`(8090, OAuth2·발급) · `module-cofunding`(8083, 개설 접수 API+판정 회신·④⑦리스너+⏰기한 스케줄러 — co_fundings·participants 단일 기록자, 교차 접근 0) · `module-payment`(8082, confirm 착지·webhook·checkout-info API+커맨드 릴레이+②⑥⑧·단독발급 리스너 — payment_* 단일 기록자, 토스 어댑터 유일 구동처). 신설 서비스는 `:support`만 의존하고 필요한 패키지만 스캔한다. 컨슈머의 DB 접근은 JPA 슬림 사본(만지는 컬럼만 매핑, 스키마 주인은 web의 Flyway·컨슈머는 validate만)이다.

| 진입점 | 모듈 | 클래스 | 책임 |
|---|---|---|---|
| Web | `:survey-drawing` | `Sulmun2yongApplication` | REST API 서버(설문·추첨·사용자·AI·파일), Outbox Producer relay, HeaderAuth/GatewayOnly 필터, Swagger, ai 헬스체크 스케줄러 (JWT 발급→auth · 결제 릴레이→payment 로 이관) |

Consumer 진입점은 2개 — `SmsNotificationConsumerApplication`(drawing-completed·co-funding-failed 구독, Kafka 어댑터·도메인 listener·SMS 보상 워커·Notification 메트릭), `DltSmsNotificationConsumerApplication`(SMS DLT + 사가 `saga.DLT` 구독·적재, DLT 메트릭). 진입점 클래스는 패키지 루트(`com.sbl.sulmun2yong`) 배치가 관례다.

**포트 배정** — `application.yml` 값은 로컬 동시 기동용 고유 포트이고, 컨테이너는 compose 의 `SERVER_PORT=8080` 으로 통일 오버라이드된다(Prometheus 스크레이프도 8080 기준).

| 서비스 | 로컬 포트 |
|---|---|
| `GatewayApplication` | 8000 |
| `Sulmun2yongApplication`(설문·추첨) | 8081 |
| `PaymentApplication` | 8082 |
| `CoFundingApplication` | 8083 |
| `SmsNotificationConsumerApplication` | 8084 |
| `DltSmsNotificationConsumerApplication` | 8085 |
| `AuthApplication` | 8090 |

### 실행 방법

| 환경 | 실행 명령 |
|---|---|
| 로컬 — Web | `./gradlew :survey-drawing:bootRun` |
| 패키징 — Web | `./gradlew :survey-drawing:bootJar` → `module-survey-drawing/build/libs/survey-drawing-*-SNAPSHOT.jar` |
| 운영 — Web | `java -jar survey-drawing-*.jar` |

로컬 전체 스택은 `docker compose up -d` 로 6개 앱(web×2·gateway·auth·payment·cofunding)+인프라가 함께 뜬다 — 외부 노출은 게이트웨이(`18000:8080`)와 web(`18080`·`18081`)뿐이고, auth·payment·cofunding 은 관문 뒤 내부망 전용이다. 게이트웨이의 라우팅 대상은 `services` 표(application.yml)를 compose 가 `SERVICES_*` 환경변수로 컨테이너명 기준으로 덮어쓴다.

배포 시 Web JAR을 이미지로 만들어 EC2에서 docker 컨테이너로 운영한다 (`{DOCKER_IMAGE_NAME}-web`, JIB→Docker Hub push→EC2 `docker pull`/run). Consumer 이미지는 각 컨슈머 모듈(`module-*-consumer/`)의 JIB 설정으로 빌드한다. Actuator 엔드포인트(`/management/health`, `/management/prometheus`)는 양쪽 모두 동일하게 노출되어 docker-compose healthcheck·Prometheus 스크레이프로 헬스체크 가능.

### 통합 테스트

Spring 런타임을 띄우는 JVM 통합 테스트는 제거되었고, 런타임 검증은 `scripts/scenarios/`의 k6 시나리오로 대체한다 (concurrency: drawing-concurrency·drawing-load·skip-locked, outbox: atomicity·relay-recovery, kafka: drawing-kafka-fanout, saga: cost-integrity).
테스트 실행 진입점 `.sh`(k6 통합 러너·분산락 실험·consumer E2E/부하·브로커 비교 러너)는 `scripts/runners/` 에 모여 있고 레포 루트에서 실행한다. k6 자산(바이너리·시나리오·lib)은 `scripts/{bin,scenarios,lib}/`, consumer E2E/부하 하네스(compose·override·dashboard)와 브로커 비교 자산은 `tests/e2e/`·`tests/broker-comparison/` 아래에 둔다 — 컨슈머 jar 만 `module-<consumer>/build/libs/` 에서 `bootJar` 로 빌드해 마운트한다.
Consumer end-to-end 흐름(Kafka listener 처리 검증)은 각 컨슈머 모듈의 단위 테스트와 `tests/e2e/` 하네스로 검증한다.

### 모듈에 새 코드 추가 시 주의

- 새 **설문/추첨 도메인의 controller / service / publisher / 사가 리스너**: `module-survey-drawing/src/main/kotlin/.../{도메인}/`
- 새 **SMS·DLT 컨슈머의 KafkaListener / 도메인 listener / @Scheduled worker**: `module-<consumer>-consumer/src/main/kotlin/.../`
- 새 **entity / repository / domain / 공유 기반 util** 또는 **notification 도메인 로직**: `module-support/src/main/kotlin/.../{도메인}/`
- 새 **kafka·outbox·분산락 인프라**: `module-support/src/main/kotlin/.../global/{kafka,lock}/`
- 새 **비프로듀서 도메인(ai/aws/user)의 컨트롤러·서비스 / 보안·JWT·resolver·전역 config**: `module-survey-drawing/src/main/kotlin/.../{도메인}/`
- 새 **payment(결제) 코드**: entity·repository·adapter·dto → `:support`, service·relay·controller·리스너 → `module-payment`. 토스 API 통신은 `payment/adapter/`(TossPaymentsAdapter)로 격리한다. **cofunding(모금) 코드**도 동형 — entity·repository·dto → `:support`, service·publisher·controller·리스너·스케줄러 → `module-cofunding`
- 판단 기준: 참조당하는 타입(entity·repository·이벤트 DTO·공유 인프라)은 항상 `:support`, 서비스 고유 로직은 각 `module-*`. 서비스 모듈끼리는 서로 참조하지 않는다(이벤트/내부 API로만 소통). `kotlin("kapt")`가 있는 `:support`에는 메타-애노테이트된 애노테이션(@AuthenticationPrincipal 파생 등)을 두지 말 것 — kapt 스텁 생성이 깨진다(그래서 `@LoginUser`류는 `module-survey-drawing`에 있다)
- consumer 측과 공유되는 Kafka 이벤트 DTO(`DrawingCompletedEvent` 등)는 `module-support`(`:support`)와 `module-common`(`:common`)가 각자 사본을 보유 — wire 스키마 계약이므로 코드 변경 시 양쪽 동기화 필요

## 아키텍처

Gradle 멀티 프로젝트 — 공유 기반 `module-support/`(`:support`) 위에 도메인 서비스 모듈들(`module-survey-drawing`·`module-auth`·`module-cofunding`·`module-payment`·`module-gateway`)이 각각 최상위 단일 모듈로 올라선다. 서비스 모듈은 `:support`에만 의존하고 서로 참조하지 않는다.
컨슈머 계열(`module-common` + 컨슈머 2개)도 같은 빌드에 포함된다.

```
{module}/src/main/kotlin/com/sbl/sulmun2yong/
├── Sulmun2yongApplication.kt          # :survey-drawing 진입점
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
│   ├── entity/·exception/·repository/·dto/  # CoFunding·CoFundingParticipant(초대제·주문 사전 발급) + 사가 CAS(tryConfirm·tryFail·tryMarkRefunded) + 요청/응답·사가 이벤트 DTO (:support 잔류 — 공유 기반)
│   └── ※ service·controller·publisher·listener·scheduler 는 전부 module-cofunding (Phase 3 완전체)
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
│                                # ※ listener/ 는 컨슈머 모듈(module-*-consumer)에 위치
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
│                                # ※ listener/, worker/ 는 컨슈머 모듈(module-*-consumer)에 위치
├── payment/                     # 결제 도메인 (토스페이먼츠 카드결제 — Command Outbox + Webhook Inbox)
│   ├── adapter/·dto/·entity/·repository/  # (:support 잔류 — 공유 기반) TossPaymentsAdapter·장부/커맨드/웹훅 인박스
│   └── ※ service·controller·relay·publisher·listener 는 전부 module-payment (Phase 4 완전체)
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
│                                # ※ listener/ 는 컨슈머 모듈(module-*-consumer)에 위치
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

Kafka Consumer 는 컨슈머 모듈(`module-*-consumer/`)이 주 관리처다. 단, 구조변경 Phase 2(단일 기록자 이벤트화, `docs/kafka-distribute-lock/구조변경-실행계획.md`)부터 목표 서비스 소속 리스너가 웹 서비스(`module-survey-drawing`)·신설 서비스 모듈에도 있다.

| 토픽 | groupId | Consumer 어댑터 (컨슈머 모듈) | 도메인 리스너 (컨슈머 모듈) |
|---|---|---|---|
| `drawing-completed` | `drawing-notification` | `DrawingCompletedNotificationKafkaListener` (`ConsumerSeekAware` 리플레이) | `drawing.DrawingSmsNotificationEventListener` (SMS 잡 생성) |
| `sms-delivery-permanently-failed` | — | 발행만 존재 (dlt 컨슈머가 발행, 현재 구독자 없음 — PG 정산 사가의 환불 리스너가 구독 예정, `docs/kafka-distribute-lock/PRD.md` 참조) | — |
| `drawing-notification.DLT` | `dlt-sms-notification` | `DltSmsNotificationKafkaListener` | `notification.DltMessageEventListener` |
| (구 co-payment-settled·co-funding-failed 컨슈머 리스너들은 Phase 2c·3에서 module-cofunding·module-payment 리스너로 대체 — co-payment-settled 토픽은 발행 중단) | | | |

개설 판정 사가 (전 구간 이벤트화) — 개설 API 는 접수(PENDING_APPROVAL)만 하고, 설문 검증·총액 확정은 데이터 소유자(설문)의 판정 리스너가 수행해 회신한다. 모금은 설문을 읽지 않는다(교차 접근 0). ②(`co-funding-created`)는 승인 리스너 tx 에서 발행되는 "개설 확정 사실"로 의미가 이동했다:

| groupId | 리스너 (위치) | 하는 일 |
|---|---|---|
| `survey-cofunding-requested` | `survey.listener.CoFundingRequestedSurveyListener` (`module-survey-drawing`) | 판정(소유자·NOT_STARTED·경품 설정) → `co-funding-reviewed` 회신(verdict=APPROVED 면 PENDING_PAYMENT 전이+총액 탑재, REJECTED 면 reason) |
| `cofunding-cofunding-reviewed` | `cofunding.listener.CoFundingReviewedListener` (`module-cofunding`) | 승인: approve CAS(분담금 확정+FUNDING)+같은 tx 에서 ② created 발행 / 거절: PENDING_APPROVAL→REJECTED 종착 |
| `payment-cofunding-created` | `payment.listener.CoFundingCreatedPaymentListener` (`module-payment`) | payment_orders 참여자별 발급 (origin=CO_FUNDING, tossOrderId 멱등) |
| `drawing-cofunding-created` | `drawing.listener.CoFundingCreatedDrawingListener` (`module-survey-drawing`) | drawing_boards (findBySurveyId 멱등) |

판정 미회신 안전망: 기한 지난 PENDING_APPROVAL 은 스케줄러가 REJECTED 로 종착(`rejectExpiredPendingApprovals` CAS). 프론트는 개설 접수 응답 후 `GET /api/v1/co-fundings/{id}` 폴링으로 FUNDING/REJECTED 확정을 받는다.

사가 리스너의 처리 실패는 토픽별로 쪼개지 않고 **`saga.DLT` 하나**로 모은다(`KafkaDltConfig` 의 고정 destination resolver — 1s×2 재시도 후 재발행). 원본 토픽은 `kafka_dlt-original-topic` 헤더가 보존하고 `SagaDltKafkaListener`(groupId `dlt-saga`)가 헤더에서 꺼내 `dlt_messages.notification_type` 에 적재한다.

단독(비모금) 개시도 이벤트 — `survey-payment-pending`(발행: `SurveySagaPublisher`, startSurvey tx 의 Outbox — 전이·보드 생성과 원자). 구독: `payment-survey-payment-pending`(`payment.listener.SurveyPaymentPendingListener`)이 주문 발급(origin=SOLO, 설문당 1행 멱등). checkoutUrl 은 `?surveyId=` 로 조립되어 설문은 orderId 를 모른다 — 결제창(checkout.html)이 주문 미수렴 404 를 재시도로 흡수한다. (구 내부 API `POST /internal/payments/orders`·`PaymentOrderClient` 는 폐지)

`payment-settled`·`payment-failed`(confirm 성공/거절·이탈 사실, 발행: `PaymentEventPublisher` Outbox — 단독·모금 불문). 이벤트에 origin(주문 발급 출처)이 실려 설문 리스너가 교차 읽기 없이 단독/모금을 판별한다(모금 활성화는 장벽 ⑤, 모금 거절은 기한 만료 무산 경로):

| groupId | 리스너 (위치) | 쓰는 테이블 |
|---|---|---|
| `survey-payment-settled` | `survey.listener.PaymentSettledSurveyListener` (`module-survey-drawing`) | surveys (origin=SOLO 만 PENDING_PAYMENT→start, 멱등) |
| `survey-payment-failed` | `survey.listener.PaymentFailedSurveyListener` (`module-survey-drawing`) | surveys (origin=SOLO 만 PENDING_PAYMENT→NOT_STARTED 복귀, 멱등) |

구조변경 Phase 2c 신설 — 모금·결제 사가 재배선. 결제의 participants·co_fundings 접근이 0 이 됐고(교차 FOR UPDATE 잠금 → 모금 로컬 tx 직렬화), `co-payment-settled` 발행이 중단됐다(기존 co-funding-consumer 의 장벽·설문 활성화 리스너는 자연 휴면 — Phase 3 에서 해체). 릴레이 CANCEL 후처리는 전이/판정 2-tx 에서 단일 tx(`settleCancelled`)로:

| groupId | 리스너 (위치) | 쓰는 테이블 |
|---|---|---|
| `cofunding-payment-settled` | `cofunding.listener.PaymentSettledCoFundingListener` (`module-cofunding` ★신설 모듈) | participants SETTLED + co_fundings 장벽 CAS(tryConfirm) — 승자만 ⑤ 발행, 무산 후 늦은 결제엔 ⑧ 발행 |
| `cofunding-payment-refunded` | `cofunding.listener.PaymentRefundedCoFundingListener` (`module-cofunding`) | participants REFUNDED + co_fundings 수렴 CAS(tryMarkRefunded) |
| `payment-cancel-requested` | `payment.listener.PaymentCancelRequestedListener` (`module-payment`) | payment_commands (CANCEL 적재, UNIQUE 멱등) |
| `survey-cofunding-confirmed` | `survey.listener.CoFundingConfirmedSurveyListener` (`module-survey-drawing`) | surveys (PENDING_PAYMENT→start, 멱등) |

구조변경 Phase 3 — `module-cofunding` 완전체(개설 API·⏰기한 스케줄러 흡수, `CoFundingSagaPublisher`가 ②⑤⑥⑧ 발행 전담) + `co-funding-consumer` 해체:

| groupId | 리스너 (위치) | 쓰는 테이블 |
|---|---|---|
| `payment-cofunding-failed` | `payment.listener.CoFundingFailedPaymentListener` (`module-payment`) | payment_commands (⑥ 스냅샷 CANCEL 팬아웃, UNIQUE 멱등) |

co-funding 사가의 무산 트리거는 기한 만료 하나 — `CoFundingDeadlineScheduler`(module-cofunding)가 만료 FUNDING 을 SKIP LOCKED 스캔해 tryFail 승자만 ⑥을 Outbox 발행(컨슈머 시절 "tx 밖 직접 발행"을 Outbox 로 개선 — 유령 신호·유실 모두 제거). 결제자 0명 무산은 스케줄러가 즉시 FAILED→REFUNDED 종착. 환불 실행은 web `PaymentCommandRelay` 의 CANCEL 발송(`settleCancelled` 단일 tx → ⑦ 발행), REFUNDED 수렴은 모금 ⑦ 리스너 소관. 종단 검증 스크립트: `scripts/scenarios/saga/co-funding-saga-verify.sh` (구 배선 기준 — 신 배선 반영 필요).

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
