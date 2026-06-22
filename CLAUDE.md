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
| `./gradlew :web:bootJar` | Web JAR 패키징 (`web/build/libs/*-SNAPSHOT.jar`) |
| `./gradlew :web:jib` | Web Docker 이미지 빌드 및 푸시 |

## 실행 단위 (Entry Points)

Gradle 멀티 프로젝트(`:common`, `:web`)로 빈 경계를 보장한다.
`:consumer` 모듈은 별도 레포지토리(`sulmoon2yong-consumer`)로 분리되어 독립적으로 빌드·배포된다.

| 진입점 | 모듈 | 클래스 | 책임 |
|---|---|---|---|
| Web | `:web` | `Sulmun2yongApplication` | REST API 서버, Outbox Producer relay, Spring Security/JWT, OAuth2, Swagger, ai 헬스체크 스케줄러 |

Consumer 진입점(`Sulmun2yongConsumerApplication`, Kafka 어댑터, 도메인 listener, SMS 보상 워커, 시간 기반 자동 마감 스케줄러, Notification/DLT 메트릭)은 `sulmoon2yong-consumer` 레포에서 관리한다.

### 실행 방법

| 환경 | 실행 명령 |
|---|---|
| 로컬 — Web | `./gradlew :web:bootRun` |
| 패키징 — Web | `./gradlew :web:bootJar` → `web/build/libs/web-*-SNAPSHOT.jar` |
| 운영 — Web | `java -jar web-*.jar` |

배포 시 Web JAR을 이미지로 만들어 Deployment로 운영한다 (`{DOCKER_IMAGE_NAME}-web`). Consumer 이미지·배포는 분리된 레포에서 관리한다. Actuator 엔드포인트(`/management/health`, `/management/prometheus`)는 양쪽 모두 동일하게 노출되어 K8s HTTP probe로 헬스체크 가능.

### 통합 테스트

Spring 런타임을 띄우는 JVM 통합 테스트는 제거되었고, 런타임 검증은 `k6_scripts/`의 시나리오로 대체한다 (drawing concurrency, outbox atomicity/relay/producer-dlq, skip-locked, sms-failover, drawing-kafka-fanout).
Consumer end-to-end 흐름(Kafka listener 처리 검증)이 필요한 테스트는 `sulmoon2yong-consumer` 레포에서 작성한다 — 이 레포의 기존 Kafka E2E 테스트는 `@Disabled`로 보존되어 있다.

### 모듈에 새 코드 추가 시 주의

- 새 **컨트롤러**: `web/src/main/kotlin/.../{도메인}/controller/`
- 새 **KafkaListener / 도메인 listener / @Scheduled worker**: `sulmoon2yong-consumer` 레포에서 작성
- 새 **service / repository / entity / dto / domain**: `common/src/main/kotlin/.../{도메인}/`
- consumer 측에서 사용되는 공통 인프라(이벤트 DTO, Outbox, Kafka config 등)는 `:common`에서 관리 — 두 레포가 같은 `:common` 코드를 공유한다 (현재는 sulmoon2yong-consumer가 `:common`을 자체 복사 보유, 코드 변경 시 양쪽 동기화 필요)

## 아키텍처

Gradle 멀티 프로젝트 — `:common`, `:web` 2개 모듈로 컴파일 타임 격리.
`:consumer`는 별도 레포(`sulmoon2yong-consumer`)로 분리됨.

```
{module}/src/main/kotlin/com/sbl/sulmun2yong/
├── Sulmun2yongApplication.kt          # :web 진입점
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
│   ├── metrics/                 # 도메인 메트릭 (DrawingProcessMetrics — winner/non_winner persistence)
│   ├── repository/
│   └── service/
│                                # ※ listener/ 는 sulmoon2yong-consumer 레포에 위치
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
│   ├── metrics/                 # 전역 메트릭 (DeadlockMetrics — MySQL deadlock 카운터)
│   ├── migration/               # Flyway 마이그레이션 (src/main/resources/db/migration/)
│   ├── resolver/                # 아규먼트 리졸버
│   ├── util/                    # 유틸리티
│   └── validator/               # 검증 로직
├── notification/                # 알림 도메인 (Inbox 패턴 + DLT)
│   ├── dto/event/               # 도메인 이벤트 DTO (DltSmsNotificationEvent, *ConsumedEvent, SmsJobCreatedEvent)
│   ├── entity/                  # sms_notification_jobs, dlt_messages 엔티티
│   ├── metrics/                 # SMS Job/DLT/Attempts 메트릭 (SmsNotificationMetrics)
│   ├── repository/              # SmsNotificationJobRepository, DltMessageRepository
│   └── service/                 # SmsSender + SmsNotificationJobService
│                                # ※ listener/, worker/ 는 sulmoon2yong-consumer 레포에 위치
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
│                                # ※ listener/ 는 sulmoon2yong-consumer 레포에 위치
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
| `adapter/` | 외부 시스템 연동 어댑터 (ai 패키지에만 존재, AI 서버·Redis 통신 전담) |
| `repository/` | Spring Data JPA 리포지토리 |
| `service/` | 비즈니스 로직 서비스 |
| `exception/` | 도메인별 커스텀 예외 |

### Kafka 컨슈머 구조 (ApplicationEvent 기반)

Web 측에서는 Producer / Outbox Relay만 동작하며, Kafka Consumer는 `sulmoon2yong-consumer` 레포에서 관리한다.
다음 토픽 → groupId → Consumer 어댑터 → 도메인 리스너 매핑은 새 레포 기준이며 참고용으로 유지한다.

| 토픽 | groupId | Consumer 어댑터 (sulmoon2yong-consumer) | 도메인 리스너 (sulmoon2yong-consumer) |
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
    ├── prometheus/prometheus.yml      # spring-apps(web 2 인스턴스) + kafka-exporter 스크레이프
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

OTel Java Agent는 web 2개 JVM에 `-javaagent` 옵션으로 부착되어 Spring MVC, JDBC, Spring Kafka, Redisson, Hibernate, HikariCP 등을 **코드 수정 없이 자동 instrument**. trace는 OTLP gRPC로 Tempo에 push되고 W3C tracecontext 헤더(`traceparent`)로 Kafka 메시지 경계를 자동 전파. consumer 측 trace는 sulmoon2yong-consumer 레포에서 동일하게 Tempo로 push 가능 (동일 클러스터 공유).

### 프로젝트 문서

```
docs/
├── PRD.md                              # 기능 명세, 데이터 모델
├── roadmaps/ROADMAP_v*.md              # Phase/Task 계획, 진행 상태
├── kafka-distribute-lock/
│   ├── PRD.md                          # 도메인 PRD
│   ├── MONITORING-PRD.md               # F001~F016 옵저버빌리티 명세
│   └── MONITORING-RESULTS.md           # F015 측정 결과 (Lock OFF vs ON, p95/3회 평균)
└── portfolio/
    └── RESUME-BULLETS.md               # F016 STAR 6 + 면접 Q&A 18 페어 (학습 모드 자료)
```

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
