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
| 마이그레이션 | Flyway |
| 캐시/분산락 | Redis (Spring Data Redis) + Redisson 3.45.1 |
| 인증 | Spring Security + OAuth2 + JWT (jjwt 0.12.6) |
| API 문서 | SpringDoc OpenAPI 2.3.0 (Swagger UI) |
| 파일 저장 | AWS S3 (AWS SDK 2.27.24) + CloudFront CDN |
| 테스트 | JUnit 5 + Mockito + Jacoco + Awaitility 4.2.2 (비동기 검증) |
| 포맷터 | ktlint 12.1.1 (Gradle 플러그인: org.jlleitschuh.gradle.ktlint) |
| 컨테이너 | JIB (Google Cloud Tools) |
| AOP | Spring Boot AOP |
| 관찰가능성 | Prometheus + Grafana 11.2 + Tempo 2.6 + OpenTelemetry Java Agent (자동 instrumentation, W3C tracecontext) |

## 명령어 (Scripts)

| 명령어 | 설명 |
|---|---|
| `./gradlew build` | 전체 모듈 빌드 + 테스트 실행 |
| `./gradlew test` | 전체 단위 테스트 실행 (concurrency 태그 제외) |
| `./gradlew :web:test` | web 모듈 테스트만 실행 |
| `./gradlew concurrencyTest` | 동시성 테스트 실행 |
| `./gradlew ktlintCheck` | 코드 스타일 검사 (전체 모듈) |
| `./gradlew ktlintFormat` | 코드 스타일 자동 수정 (전체 모듈) |
| `./gradlew jacocoTestReport` | 테스트 커버리지 리포트 생성 |
| `./gradlew :web:bootRun` | Web 진입점 로컬 실행 |
| `./gradlew :consumer:bootRun` | Consumer 진입점 로컬 실행 |
| `./gradlew :web:bootJar` | Web JAR 패키징 (`web/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :consumer:bootJar` | Consumer JAR 패키징 (`consumer/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :web:jib :consumer:jib` | Web/Consumer Docker 이미지 빌드 및 푸시 |

## 실행 단위 (Entry Points)

Gradle 멀티 프로젝트로 web/consumer 모듈을 분리해, **컴파일 타임 격리**로 빈 분리를 보장한다.
런타임 `@ComponentScan` 필터 대신 모듈 의존성(`web → common`, `consumer → common`, `web ↛ consumer`, `consumer ↛ web`)으로 분리된다.

| 진입점 | 모듈 | 클래스 | 책임 |
|---|---|---|---|
| Web | `:web` | `Sulmun2yongApplication` | REST API 서버, Outbox Producer relay, Spring Security/JWT, OAuth2, Swagger, ai 헬스체크 스케줄러 |
| Consumer | `:consumer` | `Sulmun2yongConsumerApplication` | Kafka 어댑터, 도메인 listener, SMS 보상 워커, 시간 기반 자동 마감 스케줄러, Notification/DLT 메트릭 |

`Sulmun2yongConsumerApplication`은 management-only Tomcat을 띄운다 (컨트롤러가 consumer 모듈에 없음 → Actuator/Prometheus만 노출).

### 실행 방법

| 환경 | 실행 명령 |
|---|---|
| 로컬 — Web | `./gradlew :web:bootRun` |
| 로컬 — Consumer | `./gradlew :consumer:bootRun` |
| 패키징 — Web | `./gradlew :web:bootJar` → `web/build/libs/web-*-SNAPSHOT.jar` |
| 패키징 — Consumer | `./gradlew :consumer:bootJar` → `consumer/build/libs/consumer-*-SNAPSHOT.jar` |
| 운영 — Web | `java -jar web-*.jar` |
| 운영 — Consumer | `java -jar consumer-*.jar` |

배포 시 두 JAR을 별도 이미지로 만들어 두 Deployment로 운영한다 (`{DOCKER_IMAGE_NAME}-web`, `{DOCKER_IMAGE_NAME}-consumer`). 양쪽 진입점 모두 Actuator 엔드포인트(`/management/health`, `/management/prometheus`)를 노출하므로 K8s HTTP probe로 헬스체크 가능.

### 통합 테스트

`@SpringBootTest` 대신 `@IntegrationTest` 메타 애너테이션을 사용한다. 양쪽 진입점 빈을 모두 로딩한다.
현재 `web/src/test`에 위치하며, web 모듈은 `testImplementation(project(":consumer"))`로 consumer 클래스를 testCompile 시점에 참조한다.

```kotlin
@IntegrationTest
class FooIntegrationTest { ... }
```

### 모듈에 새 코드 추가 시 주의

- 새 **컨트롤러**: `web/src/main/kotlin/.../{도메인}/controller/`
- 새 **KafkaListener / 도메인 listener / @Scheduled worker**: `consumer/src/main/kotlin/.../{도메인}/listener/` 또는 `worker/`
- 새 **service / repository / entity / dto / domain**: `common/src/main/kotlin/.../{도메인}/`
- web에서 consumer 코드를 참조하려고 하면 컴파일 에러 → 모듈 경계 위반 시그널

## 아키텍처

Gradle 멀티 프로젝트 — `:common`, `:web`, `:consumer` 3개 모듈로 컴파일 타임 격리.

```
{module}/src/main/kotlin/com/sbl/sulmun2yong/
├── Sulmun2yongApplication.kt          # :web 진입점
├── Sulmun2yongConsumerApplication.kt  # :consumer 진입점
├── ai/                          # AI 설문 생성 도메인
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
├── drawing/                     # 추첨 도메인
│   ├── controller/
│   ├── domain/
│   │   ├── drawingResult/
│   │   └── ticket/
│   ├── dto/
│   │   └── event/               # 도메인 이벤트 DTO (DrawingCompletedEvent, *ConsumedEvent 등)
│   ├── entity/                  # JPA 엔티티 + 도메인 로직 통합
│   ├── exception/
│   ├── listener/                # @EventListener (SMS 알림 잡 생성, SMS 비용 집계)
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
│   │   └── metrics/             # Lock 획득/대기 Histogram (DrawingLockMetrics)
│   ├── migration/               # Flyway 마이그레이션 (src/main/resources/db/migration/)
│   ├── resolver/                # 아규먼트 리졸버
│   ├── util/                    # 유틸리티
│   └── validator/               # 검증 로직
├── consumer/                    # Kafka 어댑터 — 얇은 진입점 (역직렬화 + ApplicationEvent 발행 + Ack 위임)
│   └── payload/                 # Consumer 역직렬화 DTO (Kafka 페이로드 ↔ 자바 객체)
├── notification/                # 알림 도메인 (Inbox 패턴 + DLT)
│   ├── dto/event/               # 도메인 이벤트 DTO (DltSmsNotificationEvent, *ConsumedEvent, SmsJobCreatedEvent)
│   ├── entity/                  # sms_notification_jobs, dlt_messages 엔티티
│   ├── listener/                # @EventListener + @TransactionalEventListener (DLT 저장, SMS 잡 처리)
│   ├── metrics/                 # SMS Job/DLT/Attempts 메트릭 (SmsNotificationMetrics)
│   ├── repository/              # SmsNotificationJobRepository, DltMessageRepository
│   ├── service/                 # SmsSender + SmsNotificationJobService
│   └── worker/                  # @Scheduled 보상 Worker (30초 폴링)
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
│   ├── listener/                # @EventListener (자동 마감, 응답 통계)
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

| 패키지 | 역할 |
|---|---|
| `controller/` | REST API 엔드포인트 정의 |
| `controller/doc/` | Swagger 문서용 인터페이스 |
| `consumer/` | Kafka 어댑터 — `@KafkaListener` 진입점 (역직렬화 + ApplicationEvent 발행 + Ack 위임만 수행, 도메인 로직 금지) |
| `{도메인}/listener/` | Spring `ApplicationEvent` 핸들러 — Consumer 어댑터에서 발행된 `*ConsumedEvent` 처리, 도메인 분기·락·저장 |
| `domain/` | 도메인 모델 (enum, sealed class, 값 객체, 라우팅 전략 등) |
| `dto/` | 요청/응답 데이터 전송 객체 |
| `dto/event/` | 이벤트 DTO (Kafka 토픽 payload + 도메인 ApplicationEvent payload `*ConsumedEvent`) |
| `entity/` | JPA 엔티티 + 도메인 로직 통합 클래스 (`@Entity`) |
| `adapter/` | 외부 시스템 연동 어댑터 (ai 패키지에만 존재, AI 서버·Redis 통신 전담) |
| `repository/` | Spring Data JPA 리포지토리 |
| `service/` | 비즈니스 로직 서비스 |
| `exception/` | 도메인별 커스텀 예외 |

### Kafka 컨슈머 구조 (ApplicationEvent 기반)

| 토픽 | groupId | Consumer 어댑터 (`consumer/`) | 도메인 리스너 (`{도메인}/listener/`) |
|---|---|---|---|
| `drawing-completed` | `drawing-notification` | `DrawingCompletedNotificationKafkaListener` | `drawing.DrawingSmsNotificationEventListener` |
| `drawing-completed` | `drawing-auto-close` | `DrawingCompletedAutoCloseKafkaListener` | `survey.SurveyAutoCloseOnDrawingExhaustedEventListener` |
| `drawing-completed` | `sms-cost-calculator` | `DrawingCompletedSmsCostKafkaListener` (`ConsumerSeekAware` 리플레이) | `drawing.SmsCostEventListener` |
| `survey-response-submitted` | `response-stats` | `SurveyResponseSubmittedStatsKafkaListener` | `survey.SurveyResponseStatsEventListener` |
| `drawing-notification.DLT` | `dlt-sms-notification` | `DltSmsNotificationKafkaListener` | `notification.DltMessageEventListener` |

### 관찰가능성 (Observability) 스택

LGTM 중 **G(Grafana) + M(Metrics) + T(Tracing)** 활성. Logs(Loki)는 후속 단계.

```
infra/
├── otel/
│   └── opentelemetry-javaagent.jar   # JVM 부착용 OTel Java Agent (-javaagent 옵션)
└── monitoring/
    ├── prometheus/prometheus.yml      # spring-apps(5 인스턴스) + kafka-exporter 스크레이프
    ├── tempo/tempo.yml                # OTLP receivers(4317/4318), 로컬 storage, 14일 retention
    └── grafana/
        ├── provisioning/
        │   ├── datasources/
        │   │   ├── prometheus.yml     # uid=prometheus
        │   │   └── tempo.yml          # uid=tempo (serviceMap → prometheus 연결)
        │   └── dashboards/            # 대시보드 자동 로드
        └── dashboards/                # outbox/sms-failover/drawing-lock/consumer-fanout/cluster-overview
```

| 컴포넌트 | 컨테이너 | 호스트 포트 | 역할 |
|---|---|---|---|
| Prometheus | `sulmun2yong-cluster-prometheus` | `19090` | 메트릭 수집 (15s scrape, 7d retention) |
| Grafana | `sulmun2yong-cluster-grafana` | `13000` | 대시보드 + Explore (datasource provisioning) |
| Tempo | `sulmun2yong-cluster-tempo` | `13200` (query), `14317` (OTLP gRPC) | 분산 trace 저장·질의 |
| Kafka Exporter | `sulmun2yong-cluster-kafka-exporter` | `19308` | Kafka consumer lag 메트릭 |

OTel Java Agent는 web/consumer 5개 JVM 모두에 `-javaagent` 옵션으로 부착되어 Spring MVC, JDBC, Spring Kafka, Redisson, Hibernate, HikariCP 등을 **코드 수정 없이 자동 instrument**. trace는 OTLP gRPC로 Tempo에 push되고 W3C tracecontext 헤더(`traceparent`)로 Kafka 메시지 경계를 자동 전파.

## 환경 변수

`application-secret.yml`에서 관리 (Git 미추적). `docker-compose.yml`에서 `.env` 파일 참조.

| 변수 | 용도 |
|---|---|
| `MYSQL_*` | MySQL 연결 정보 |
| `REDIS_PASSWORD` | Redis 비밀번호 |

### 외부 서비스 URL (application.yml)

| 설정 | 기본값 | 용도 |
|---|---|---|
| `frontend.base-url` | `http://localhost:3000` | 프론트엔드 |
| `backend.base-url` | `http://localhost:8080` | 백엔드 |
| `ai-server.base-url` | `http://localhost:8000` | AI 서버 |
| `cloudfront.base-url` | `https://file.sulmoon.io` | CDN |
| `cookie.domain` | `localhost` | 쿠키 도메인 |

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
| 로컬 인프라 | Docker Compose (MySQL + Redis) |
