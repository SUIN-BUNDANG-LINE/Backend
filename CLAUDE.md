# sulmun2yong Backend

## 프로젝트 개요

설문이용 (sulmun2yong) — 설문조사 생성·배포·응답 수집·추첨 기능을 제공하는 웹 서비스의 백엔드 API 서버.

## 기술 스택

| 분류         | 기술                                                                                                      |
|--------------|-----------------------------------------------------------------------------------------------------------|
| 언어         | Kotlin 2.4.10                                                                                             |
| 프레임워크   | Spring Boot 3.3.1                                                                                         |
| 빌드         | Gradle Kotlin DSL (build.gradle.kts)                                                                      |
| JDK          | 17                                                                                                        |
| 데이터베이스 | MySQL 8.0 (Spring Data JPA + Hibernate)                                                                   |
| ORM          | JPA/Hibernate (QueryDSL 제거 — 커스텀 조회는 JPQL/EntityManager)                                          |
| 마이그레이션 | Flyway — **서비스마다 자기 스키마를 소유** (`module-*/src/main/resources/db/migration/V1__init.sql`)      |
| 캐시/분산락  | Redis (Spring Data Redis) + Redisson 3.45.1                                                               |
| 인증         | Spring Security + OAuth2 + JWT (jjwt 0.12.6)                                                              |
| API 문서     | SpringDoc OpenAPI 2.3.0 (Swagger UI)                                                                      |
| 파일 저장    | AWS S3 (AWS SDK 2.27.24) + CloudFront CDN                                                                 |
| 테스트       | JUnit 5 + Mockito + Jacoco                                                                                |
| 포맷터       | ktlint 14.2.0 (Gradle 플러그인: org.jlleitschuh.gradle.ktlint)                                            |
| 컨테이너     | JIB (Google Cloud Tools)                                                                                  |
| AOP          | Spring Boot AOP                                                                                           |
| 관찰가능성   | Prometheus + Grafana 11.2 + Tempo 2.6 + OpenTelemetry Java Agent (자동 instrumentation, W3C tracecontext) |

## 명령어 (Scripts)

| 명령어                              | 설명                                                               |
|-------------------------------------|--------------------------------------------------------------------|
| `./gradlew build`                   | 전체 모듈 빌드 + 테스트 실행                                       |
| `./gradlew test`                    | 전체 단위 테스트 실행                                              |
| `./gradlew :survey-drawing:test`    | web 모듈 테스트만 실행                                             |
| `./gradlew ktlintCheck`             | 코드 스타일 검사 (전체 모듈)                                       |
| `./gradlew ktlintFormat`            | 코드 스타일 자동 수정 (전체 모듈)                                  |
| `./gradlew jacocoTestReport`        | 테스트 커버리지 리포트 생성                                        |
| `./gradlew :survey-drawing:bootRun` | Web 진입점 로컬 실행                                               |
| `./gradlew :survey-drawing:bootJar` | Web JAR 패키징 (`module-survey-drawing/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :survey-drawing:jib`     | Web Docker 이미지 빌드 및 푸시                                     |

## 실행 단위 (Entry Points)

Gradle 멀티 프로젝트로 빈 경계를 보장한다. 모든 도메인 서비스는 최상위 단일 모듈 (`module-*/`)이며 메시징 기반 `:messaging`
(`module-messaging/`)에만 의존한다.

- `:messaging`(`module-messaging/`) — **카프카 전용** 기반 라이브러리 (bootJar 비활성, 27개). ① `global/kafka/`
  (Outbox 적재·릴레이·발행, 토픽/DLT 설정) ② wire 이벤트 계약 (`*/dto/event/` — 서비스 간 계약 13개).
  그 외 도메인 엔티티·리포지토리·유틸·에러코드는 **전부 각 서비스 모듈 소유**다.
- `:survey-drawing`(`module-survey-drawing/`) — 실행 진입점 (`Sulmun2yongApplication`) + 설문/추첨 도메인
  (서비스·퍼블리셔·컨트롤러·사가 리스너) + `@LoginUser` 등 인증 애노테이션 + 비프로듀서 도메인 (ai/aws/user)의 컨트롤러·서비스·어댑터 +
  보안/JWT/resolver/전역 config·예외 핸들러.

별도 컨슈머 모듈은 없다 — 모든 리스너는 도메인 서비스 모듈 소속이다. (SMS 알림·DLT 컨슈머와 `:common`
DTO 라이브러리는 삭제, `:co-funding-consumer`는 구조변경 Phase 3에서 `module-cofunding`으로 흡수·해체)

도메인 서비스 계열 (구조변경, `docs/kafka-distribute-lock/구조변경-실행계획.md`): `module-gateway`(8000, 라우팅·JWT 검증) ·
`module-auth`(8090, OAuth2·발급) · `module-cofunding`(8083, 개설 접수 API+판정 회신·④⑦리스너+⏰기한 스케줄러 —
co_fundings·participants 단일 기록자, 교차 접근 0) · `module-payment`(8082, confirm 착지·webhook·checkout-info
API+토스 호출 아웃박스 릴레이+②⑥⑧ 리스너 — payment_* 단일 기록자, 토스 어댑터 유일 구동처). 신설 서비스는 `:messaging`만 의존하고 필요한 패키지만 스캔한다.
**DB per service** — 서비스마다 자기 스키마를 갖고 각자 Flyway로 마이그레이션한다 (같은 MySQL 인스턴스, 스키마 분리).
교차 조회가 불가능해 사가·이벤트가 유일한 소통 수단이다.

| 스키마 | 주인 | 테이블 |
|---|---|---|
| `survey_db` | survey-drawing | surveys·sections·questions·choices·rewards·responses·participants·drawing_boards·tickets·drawing_histories·ai_* + `kafka_record_outbox` |
| `cofunding_db` | cofunding | co_fundings·co_funding_participants + `kafka_record_outbox` |
| `payment_db` | payment | toss_orders(시도 이력 - PK=토스 orderId)·toss_api_call_outbox·payment_webhook_inbox + `kafka_record_outbox` |
| `auth_db` | auth | users·refresh_tokens |

카프카 아웃박스(`kafka_record_outbox`)는 발행하는 서비스마다 **자기 스키마에 하나씩** 둔다 (Outbox 는 도메인 변경과 같은 트랜잭션이어야 하므로).
스키마 생성은 `infra/mysql/init/01-create-service-schemas.sql` — **최초 기동 시 1회**만 실행되므로
스키마를 다시 만들려면 `docker compose down -v` 로 볼륨을 비워야 한다.

| 진입점 | 모듈              | 클래스                   | 책임                                                                                                                                                                     |
|--------|-------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Web    | `:survey-drawing` | `Sulmun2yongApplication` | REST API 서버(설문·추첨·사용자·AI·파일), Outbox Producer relay, HeaderAuth/GatewayOnly 필터, Swagger, ai 헬스체크 스케줄러 (JWT 발급→auth · 결제 릴레이→payment 로 이관) |

**포트 배정** — `application.yml` 값은 로컬 동시 기동용 고유 포트이고, 컨테이너는 compose 의 `SERVER_PORT=8080` 으로 통일 오버라이드된다
(Prometheus 스크레이프도 8080 기준).

| 서비스                                  | 로컬 포트 |
|-----------------------------------------|-----------|
| `GatewayApplication`                    | 8000      |
| `Sulmun2yongApplication`(설문·추첨)     | 8081      |
| `PaymentApplication`                    | 8082      |
| `CoFundingApplication`                  | 8083      |
| `AuthApplication`                       | 8090      |

### 실행 방법

| 환경         | 실행 명령                                                                                              |
|--------------|--------------------------------------------------------------------------------------------------------|
| 로컬 — Web   | `./gradlew :survey-drawing:bootRun`                                                                    |
| 패키징 — Web | `./gradlew :survey-drawing:bootJar` → `module-survey-drawing/build/libs/survey-drawing-*-SNAPSHOT.jar` |
| 운영 — Web   | `java -jar survey-drawing-*.jar`                                                                       |

로컬 전체 스택은 `docker compose up -d` 로 6개 앱 (web×2·gateway·auth·payment·cofunding)+인프라가 함께 뜬다 — 외부 노출은
게이트웨이 (`18000:8080`)와 web (`18080`·`18081`)뿐이고, auth·payment·cofunding 은 관문 뒤 내부망 전용이다. 게이트웨이의 라우팅
대상은 `services` 표 (application.yml)를 compose 가 `SERVICES_*` 환경변수로 컨테이너명 기준으로 덮어쓴다.

배포 시 Web JAR을 이미지로 만들어 EC2에서 docker 컨테이너로 운영한다 (`{DOCKER_IMAGE_NAME}-web`, JIB→Docker Hub push→EC2
`docker pull`/run). Actuator 엔드포인트
(`/management/health`, `/management/prometheus`)는 양쪽 모두 동일하게 노출되어 docker-compose
healthcheck·Prometheus 스크레이프로 헬스체크 가능.

### 통합 테스트

Spring 런타임을 띄우는 JVM 통합 테스트는 제거되었고, 런타임 검증은 `scripts/scenarios/`의 k6 시나리오로 대체한다 (concurrency:
drawing-concurrency·drawing-load·skip-locked, outbox: atomicity·relay-recovery). 테스트 실행 진입점 `.sh`(k6 통합 러너·분산락 실험·브로커 비교 러너)는 `scripts/runners/` 에 모여 있고
레포 루트에서 실행한다. k6 자산 (바이너리·시나리오·lib)은 `scripts/{bin,scenarios,lib}/`,
브로커 비교 자산은 `tests/broker-comparison/` 아래에 둔다.

### 모듈에 새 코드 추가 시 주의

- 새 **설문/추첨 도메인의 controller / service / publisher / 사가 리스너**:
  `module-survey-drawing/src/main/kotlin/.../{도메인}/`
- 새 **entity / repository / domain / util / 에러코드**: 그 도메인을 소유한 **서비스 모듈** 안에
  (`module-{서비스}/src/main/kotlin/.../`). 여러 서비스가 쓰게 되면 복제한다 — 공유 모듈로 올리지 않는다.
- 새 **kafka·outbox 인프라**: `module-messaging/src/main/kotlin/.../global/kafka/` (분산락은 `module-survey-drawing`)
- 새 **비프로듀서 도메인 (ai/aws/user)의 컨트롤러·서비스 / 보안·JWT·resolver·전역 config**:
  `module-survey-drawing/src/main/kotlin/.../{도메인}/`
- 새 **payment (결제) 코드**: 전부 `module-payment` (entity·repository·adapter·dto·service·relay·controller·리스너).
  토스 API 통신은 `payment/adapter/`(TossPaymentsAdapter)로 격리한다. **cofunding (모금) 코드**도 동형 — 전부
  `module-cofunding`. 단 **wire 이벤트 DTO(`dto/event/`)만 `:messaging`** — 다른 서비스가 역직렬화하는 계약이라서다.
- 판단 기준: **카프카 인프라와 서비스 간 wire 계약만 `:messaging`**, 나머지는 전부 소유 서비스 모듈.
  두 서비스가 같은 유틸을 쓰면 각자 사본을 갖는다 (`BaseTimeEntity`·`EncryptionUtils`·`ErrorCode` 가 그렇게 복제돼 있다).
  서비스 모듈끼리는 서로 참조하지 않는다 (이벤트로만 소통 — 실측 교차 참조 0).

## 아키텍처

Gradle 멀티 프로젝트 — 카프카 기반 `module-messaging/`(`:messaging`, 27개) 위에 도메인 서비스 모듈들
(`module-survey-drawing` 249 · `module-auth` 41 · `module-payment` 37 · `module-cofunding` 26 ·
`module-gateway` 3)이 각각 최상위 단일 모듈로 올라선다. 서비스 모듈은 `:messaging`에만 의존하고 서로 참조하지 않는다.

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
│   ├── entity/·exception/·repository/·dto/  # CoFunding·CoFundingParticipant(초대제·주문 사전 발급) + 사가 CAS(tryConfirm·tryFail·tryMarkRefunded) + 요청/응답 DTO — 전부 module-cofunding 소유 (사가 이벤트 DTO 만 :messaging)
│   └── ※ service·controller·publisher·listener·scheduler 는 전부 module-cofunding (Phase 3 완전체)
├── drawing/                     # 추첨 도메인
│   ├── controller/
│   ├── domain/
│   │   ├── drawingResult/
│   │   └── ticket/
│   ├── dto/
│   ├── entity/                  # JPA 엔티티 + 도메인 로직 통합
│   ├── exception/
│   ├── metrics/                 # 도메인 메트릭 (DrawingProcessMetrics — winner/non_winner persistence, 경쟁 제어 전략별 통일 지표 outcome/duration/contention_wait/entry/attempt)
│   ├── publisher/               # Kafka 발행 단일 진입점 (DrawingEventPublisher — Outbox 저장 + ApplicationEvent)
│   ├── repository/
│   └── service/
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
│   ├── resolver/                # 아규먼트 리졸버
│   ├── util/                    # 유틸리티
│   └── validator/               # 검증 로직
├── payment/                     # 결제 도메인 (토스페이먼츠 카드결제 — Command Outbox + Webhook Inbox)
│   ├── adapter/·dto/·entity/·repository/  # module-payment 소유 — TossPaymentsAdapter·장부/커맨드/웹훅 인박스 (사가 이벤트 DTO 만 :messaging)
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

| 패키지            | 역할                                                                               |
|-------------------|------------------------------------------------------------------------------------|
| `controller/`     | REST API 엔드포인트 정의                                                           |
| `controller/doc/` | Swagger 문서용 인터페이스                                                          |
| `domain/`         | 도메인 모델 (enum, sealed class, 값 객체, 라우팅 전략 등)                          |
| `dto/`            | 요청/응답 데이터 전송 객체                                                         |
| `dto/event/`      | 이벤트 DTO (Kafka 토픽 payload + 도메인 ApplicationEvent payload `*ConsumedEvent`) |
| `entity/`         | JPA 엔티티 + 도메인 로직 통합 클래스 (`@Entity`)                                   |
| `adapter/`        | 외부 시스템 연동 어댑터 (ai: AI 서버·Redis 통신, payment: 토스페이먼츠 API 통신)   |
| `repository/`     | Spring Data JPA 리포지토리                                                         |
| `service/`        | 비즈니스 로직 서비스                                                               |
| `exception/`      | 도메인별 커스텀 예외                                                               |

### Kafka 컨슈머 구조 (ApplicationEvent 기반)

사가 리스너는 전부 각 도메인 서비스 모듈 소속이다.

개설 판정 사가 (전 구간 이벤트화) — 개설 API 는 접수 (PENDING)만 하고, 설문 검증·총액 확정은 데이터 소유자 (설문)의 판정 리스너가 수행해 회신한다. 모금은
설문을 읽지 않는다 (교차 접근 0). ② (`co-funding-created`)는 승인 리스너 tx 에서 발행되는 "개설 확정 사실"로 의미가 이동했다:

| groupId                        | 리스너 (위치)                                                                | 하는 일                                                                                                                                 |
|--------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `survey-cofunding-requested`   | `survey.listener.CoFundingRequestedSurveyListener` (`module-survey-drawing`) | 판정(소유자·NOT_STARTED·경품 설정) → `co-funding-reviewed` 회신(verdict=APPROVED 면 결제 대기 보드 생성(findBySurveyId 멱등)+총액 탑재 한 tx — 단독 startSurvey 와 동형. 설문 행 불변 — 결제 대기 상태는 보드가 지고 그 존재가 수정 잠금. REJECTED 면 reason) |
| `cofunding-cofunding-reviewed` | `cofunding.listener.CoFundingReviewedListener` (`module-cofunding`)          | 승인: approve CAS(분담금 확정+FUNDING)+같은 tx 에서 ② created 발행 / 거절: PENDING→REJECTED 종착                                        |
| `payment-cofunding-created`    | `payment.listener.CoFundingCreatedPaymentListener` (`module-payment`)        | toss_orders 참여자별 발급 (PK=orderId 멱등)                                                                      |

판정 미회신 안전망: 기한 지난 PENDING 은 스케줄러가 REJECTED 로 종착 (`rejectExpiredPendingApprovals` CAS). 프론트는 개설 접수 응답
후 `GET /api/v1/co-fundings/{id}` 폴링으로 FUNDING/REJECTED 확정을 받는다.

사가 리스너의 처리 실패는 토픽별로 쪼개지 않고 **`saga.DLT` 하나**로 모은다 (`KafkaDltConfig` 의 고정 destination resolver — 1s×2
재시도 후 재발행). 원본 토픽·예외 메시지는 `kafka_dlt-*` 헤더가 보존한다. 구독자는 없다 — 죽은 편지는 토픽에 보관하고
Kafbat UI 로 조사·수동 재발행한다(재발행 시 원본 키 유지 권장 — 추적성·같은 엔티티 이벤트의 파티션 응집. 순서 의존은 없다(리스너가 상태머신·CAS 로 도착 순서 불문 수렴)). DB 적재·재처리 API 방식(parking lot)은
과설계로 판단해 제거했다.

단독 (비모금) 개시도 **1인 모금 접수로 일원화** — 유료 개시의 유일한 길은 `POST /surveys/{id}/co-funding`
(participants 1명 = 단독, capacity 하한 1). `startSurvey` 는 유료 설문이면 paymentRequired=true 신호만 주고
보드 생성·주문 발급은 접수 사가(판정 tx·③)가 담당한다. checkout 진입은 `?orderId=` 단일(by-order),
my-order 가 좌표를 교부한다. (구 `survey-payment-pending` 토픽·`checkout-info/by-board`·주문 origin 필드는 폐지 —
④는 좌표 (productType, productId)만 나른다)

`payment-succeeded`(confirm 확정 사실, 발행: `PaymentEventPublisher` Outbox).
산 물건의 일반 좌표 (productType, productId)를 실어 소비자가 교차 읽기 없이 반응한다. 구독자는 모금 하나 —
설문 개시는 장벽 통과 후의 ⑤가 전담한다. 결제 실패는 이벤트 없이 장부(FAILED)에만 기록한다
(한 명의 실패는 사가를 움직이지 않고, 기한 내 재결제 자유):

| groupId                  | 리스너 (위치)                                                            | 쓰는 테이블                                                     |
|--------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------|

구조변경 Phase 2c 신설 — 모금·결제 사가 재배선. 결제의 participants·co_fundings 접근이 0 이 됐고 (교차 FOR UPDATE 잠금 → 모금 로컬
tx 직렬화), `co-payment-settled` 발행이 중단됐다 (기존 co-funding-consumer 의 장벽·설문 활성화 리스너는 자연 휴면 — Phase 3 에서
해체). 릴레이 CANCEL 후처리는 전이/판정 2-tx 에서 단일 tx (`settleCancelled`)로:

| groupId                      | 리스너 (위치)                                                                         | 쓰는 테이블                                                                                         |
|------------------------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `cofunding-payment-succeeded`  | `cofunding.listener.PaymentSucceededCoFundingListener` (`module-cofunding` ★신설 모듈) | participants PAID + co_fundings 장벽 CAS(tryConfirm) — 승자만 ⑤ 발행, 무산 후 늦은 결제엔 ⑧ 발행 |
| `cofunding-payment-refunded` | `cofunding.listener.PaymentRefundedCoFundingListener` (`module-cofunding`)            | participants REFUNDED + co_fundings 수렴 CAS(tryMarkRefunded)                                       |
| `payment-cancel-requested`   | `payment.listener.PaymentCancelRequestedListener` (`module-payment`)                  | toss_api_call_outbox (CANCEL 의도 선커밋, UNIQUE 멱등)                                                         |
| `survey-cofunding-confirmed` | `survey.listener.CoFundingConfirmedSurveyListener` (`module-survey-drawing`)          | 보드 ACTIVE(대금 확정)+surveys start 한 tx — 결제 대기 보드 가드 멱등                                |

구조변경 Phase 3 — `module-cofunding` 완전체 (개설 API·⏰기한 스케줄러 흡수, `CoFundingOutboxKafkaPublisher`가 ②⑤⑥⑧ 발행
전담) + `co-funding-consumer` 해체:

| groupId                    | 리스너 (위치)                                                        | 쓰는 테이블                                            |
|----------------------------|----------------------------------------------------------------------|--------------------------------------------------------|
| `payment-cofunding-expired` | `payment.listener.CoFundingExpiredPaymentListener` (`module-payment`) | toss_api_call_outbox (⑥ 스냅샷 CANCEL 팬아웃, UNIQUE 멱등) |

co-funding 사가의 무산 트리거는 기한 만료 하나 — `CoFundingDeadlineScheduler`(module-cofunding)가 만료 FUNDING 을 SKIP
LOCKED 스캔해 tryFail 승자만 ⑥을 Outbox 발행 (컨슈머 시절 "tx 밖 직접 발행"을 Outbox 로 개선 — 유령 신호·유실 모두 제거). 무산(⑥)은 web 의 `drawing.listener.CoFundingExpiredDrawingListener`(groupId `drawing-cofunding-expired`)도 구독해
결제 대기 보드를 폐기한다(수정 잠금 해제 — expiredAt 이후 태어난 재접수 보드는 시간 가드로 보호). 종착(REJECTED·FAILED·REFUNDED)
뒤 재접수는 허용(접수 가드는 진행 중 상태만 차단, UNIQUE(survey_id)는 일반 인덱스로 완화 — 종착 행은 이력 누적). 결제자 0명 무산은
스케줄러가 즉시 FAILED→REFUNDED 종착. 환불 실행은 payment `TossApiCallOutboxRelay` 의 CANCEL 발송 (`settleCancelled` 단일 tx →
⑦ 발행), REFUNDED 수렴은 모금 ⑦ 리스너 소관. 종단 검증 스크립트: `scripts/scenarios/saga/co-funding-saga-verify.sh` (구
배선 기준 — 신 배선 반영 필요).

티켓 소진 시 설문 자동 종료는 응답 일관성을 위해 동기 처리 — `DrawingProcessService.closeSurveyIfTicketsExhausted`가 추첨 트랜잭션
안에서 직접 수행한다 (Kafka fan-out 대상 아님).

### 관찰가능성 (Observability) 스택

LGTM 중 **G (Grafana) + M (Metrics) + T (Tracing)** 활성. Logs (Loki)는 후속 단계.

```
infra/
├── otel/
│   └── opentelemetry-javaagent.jar   # JVM 부착용 OTel Java Agent (-javaagent 옵션)
└── monitoring/
    ├── prometheus/prometheus.yml      # spring-apps(web 2)·domain-services(게이트웨이·인증·결제·모금 4)·kafka/mysqld-exporter 스크레이프
    ├── tempo/tempo.yml                # OTLP receivers(4317/4318), 로컬 storage, 14일 retention
    └── grafana/
        ├── provisioning/
        │   ├── datasources/
        │   │   ├── prometheus.yml     # uid=prometheus
        │   │   └── tempo.yml          # uid=tempo (serviceMap → prometheus 연결)
        │   └── dashboards/            # 대시보드 자동 로드
        └── dashboards/                # outbox/drawing-lock/cluster-overview/race-comparison
```

| 컴포넌트       | 컨테이너                              | 호스트 포트                          | 역할                                                      |
|----------------|---------------------------------------|--------------------------------------|-----------------------------------------------------------|
| Prometheus     | `sulmun2yong-cluster-prometheus`      | `19090`                              | 메트릭 수집 (15s scrape, 7d retention)                    |
| Grafana        | `sulmun2yong-cluster-grafana`         | `13000`                              | 대시보드 + Explore (datasource provisioning)              |
| Tempo          | `sulmun2yong-cluster-tempo`           | `13200` (query), `14317` (OTLP gRPC) | 분산 trace 저장·질의                                      |
| Kafka Exporter | `sulmun2yong-cluster-kafka-exporter`  | `19308`                              | Kafka consumer lag 메트릭                                 |
| MySQL Exporter | `sulmun2yong-cluster-mysqld-exporter` | `19104`                              | InnoDB 행 락 대기·데드락·롤백 (분산락 전-후 DB 부하 비교) |

OTel Java Agent는 web 2개 JVM에 `-javaagent` 옵션으로 부착되어 Spring MVC, JDBC, Spring Kafka, Redisson,
Hibernate, HikariCP 등을 **코드 수정 없이 자동 instrument**. trace는 OTLP gRPC로 Tempo에 push되고 W3C tracecontext
헤더 (`traceparent`)로 Kafka 메시지 경계를 자동 전파. consumer 측 trace도 동일하게 Tempo로 push 가능 (동일 클러스터 공유).

### 프로젝트 문서

```
docs/                                   # Git 미추적(.gitignore) 로컬 문서
├── kafka-distribute-lock/              # 도메인 PRD·시나리오·중복요청·EVENT-GLOSSARY·토스 API 참조
├── broker-docker-compose/              # Kafka 브로커 1~3대 비교용 docker-compose
└── vsRABBITMQ.md·vsREDIS.md·카프카브로커옵션들.md + 결제 아키텍처 SVG 다이어그램
```

## 환경 변수

`application-secret.yml`에서 관리 (Git 미추적). `docker-compose.yml`에서 `.env` 파일 참조.

| 변수                | 용도                                                                  |
|---------------------|-----------------------------------------------------------------------|
| `MYSQL_*`           | MySQL 연결 정보                                                       |
| `REDIS_PASSWORD`    | Redis 비밀번호                                                        |
| `toss.client-key`   | 토스페이먼츠 클라이언트 키 (결제창 SDK용, 프론트 노출)                |
| `toss.secret-key`   | 토스페이먼츠 시크릿 키 (백엔드 confirm/cancel Basic Auth용)           |
| `toss.base-url`     | 토스페이먼츠 결제 API 주소 (기본 `https://api.tosspayments.com`)      |
| `TEST_AUTH_ENABLED` | 부하테스트용 엔드포인트 활성화 (JWT 발급·설문 활성화, 기본 false)     |
| `TX_ISOLATION`      | 트랜잭션 격리수준 실험용 (HikariCP, 기본 TRANSACTION_REPEATABLE_READ) |

### 외부 서비스 URL (application.yml)

| 설정                        | 기본값                    | 용도                                             |
|-----------------------------|---------------------------|--------------------------------------------------|
| `frontend.base-url`         | `http://localhost:3000`   | 프론트엔드                                       |
| `backend.base-url`          | `http://localhost:8080`   | 백엔드                                           |
| `ai-server.base-url`        | `http://localhost:8000`   | AI 서버                                          |
| `cloudfront.base-url`       | `https://file.sulmoon.io` | CDN                                              |
| `cookie.domain`             | `localhost`               | 쿠키 도메인                                      |
| `payment.reward-unit-price` | `2000`                    | 경품 1개당 단가(원) — 결제 금액 = 단가 × 경품 수 |

## 개발 도구 및 설정

| 도구            | 설정                                                                    |
|-----------------|-------------------------------------------------------------------------|
| 패키지 매니저   | Gradle 8.x (Wrapper)                                                    |
| JDK             | 17 (Temurin)                                                            |
| 포맷터          | ktlint (`./gradlew ktlintFormat`)                                       |
| 포맷 검사       | ktlint (`./gradlew ktlintCheck`)                                        |
| 테스트 커버리지 | Jacoco                                                                  |
| CI              | GitHub Actions (PR CI + 배포)                                           |
| 컨테이너 빌드   | JIB                                                                     |
| 로컬 인프라     | Docker Compose (MySQL·Redis·web×2 + Prometheus/Grafana/Tempo/exporters) |

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure, shell commands, and other
important information, read the current plan:
`specs/001-co-funding-saga/plan.md`
<!-- SPECKIT END -->
