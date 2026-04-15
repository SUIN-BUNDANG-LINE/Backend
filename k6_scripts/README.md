# k6 부하 테스트 스크립트

## 개요

Kotlin 통합/동시성 테스트를 k6 HTTP 부하 테스트로 전환한 스크립트 모음.

## 디렉토리 구조

```
k6_scripts/
├── lib/
│   ├── config.js               # 공통 설정 (BASE_URL, 인증)
│   └── helpers.js              # API 호출 헬퍼 (설문 생성, 응답 제출, 추첨 등)
├── drawing-concurrency.js      # 추첨 동시성 테스트
├── drawing-kafka-fanout.js     # 추첨 Fan-out + 설문 자동 종료 테스트
├── survey-response-load.js     # 응답 제출 부하 + auto-close 테스트
├── outbox-atomicity.js         # Outbox 원자성 (중복 응답 방어) 테스트
└── README.md
```

## 원본 테스트 매핑

| k6 스크립트 | 원본 Kotlin 테스트 | 핵심 검증 |
|---|---|---|
| `drawing-concurrency.js` | `DrawingBoardServiceConcurrencyTest` | 분산락 동시성 (같은 티켓/같은 사용자/다른 번호) |
| `drawing-kafka-fanout.js` | `DrawingKafkaIntegrationTest` | 100명 추첨 → 티켓 소진 → 설문 CLOSED |
| `survey-response-load.js` | `SurveyResponseKafkaIntegrationTest` | 100명 응답 → target 도달 → 설문 CLOSED |
| `outbox-atomicity.js` | `OutboxAtomicityIntegrationTest` | 중복 visitorId → 1건만 성공 |

### 전환 불가 테스트

| 원본 테스트 | 미전환 사유 |
|---|---|
| `OutboxRelayIntegrationTest` | `@SpyBean`으로 서버 내부 Listener 차단 필요 |
| `ConsumerIdempotencyIntegrationTest` | Kafka Consumer 내부 멱등성 → DB 직접 조회 필요 |
| `SmsFailoverIntegrationTest` | `LoggingSmsSender.forcedFailCount` 서버 상태 제어 필요 |

## 사전 준비

### 1. k6 설치

```bash
# macOS
brew install k6

# Docker
docker pull grafana/k6
```

### 2. JWT 토큰 획득

설문 생성/시작 API는 인증이 필요합니다. OAuth2 로그인 후 브라우저 쿠키에서 `access-token` 값을 복사하세요.

```bash
# 브라우저 개발자 도구 → Application → Cookies → access-token 값 복사
export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

### 3. 서버 실행

```bash
# 로컬 인프라 (MySQL, Redis, Kafka)
docker-compose up -d

# Spring Boot 서버
./gradlew bootRun
```

## 실행 방법

### 기본 실행

```bash
cd k6_scripts

# 추첨 동시성 테스트
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-concurrency.js

# 추첨 Fan-out 테스트
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-kafka-fanout.js

# 응답 제출 부하 테스트
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN survey-response-load.js

# Outbox 원자성 테스트
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN outbox-atomicity.js
```

### 환경변수 커스터마이징

```bash
# 서버 주소 변경
k6 run --env BASE_URL=http://staging.example.com --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-concurrency.js

# Fan-out 테스트 파라미터 조정
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN \
       --env BOARD_SIZE=100 \
       --env WINNING_COUNT=20 \
       --env PARTICIPANT_COUNT=200 \
       drawing-kafka-fanout.js

# 응답 제출 VU 수 조정
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN \
       --env TARGET_COUNT=100 \
       --env SUBMISSION_COUNT=500 \
       --env VUS=50 \
       survey-response-load.js
```

### Docker로 실행

```bash
docker run --rm -i \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ACCESS_TOKEN=$ACCESS_TOKEN \
  -v $(pwd)/k6_scripts:/scripts \
  grafana/k6 run /scripts/drawing-concurrency.js
```

## 주요 메트릭

### 공통 (k6 빌트인)

| 메트릭 | 설명 |
|---|---|
| `http_req_duration` | 요청 응답 시간 (p95 < 5s 임계값) |
| `http_req_failed` | 요청 실패율 |
| `iterations` | 총 반복 수 |

### 스크립트별 커스텀 메트릭

| 스크립트 | 메트릭 | 설명 |
|---|---|---|
| `drawing-concurrency` | `drawing_success_total` | 추첨 성공 수 |
| `drawing-concurrency` | `drawing_fail_total` | 추첨 실패 수 |
| `drawing-concurrency` | `drawing_success_rate` | 추첨 성공률 |
| `drawing-kafka-fanout` | `drawing_win_total` | 당첨 수 |
| `drawing-kafka-fanout` | `drawing_lose_total` | 낙첨 수 |
| `survey-response-load` | `submit_success_total` | 응답 성공 수 |
| `survey-response-load` | `submit_success_rate` | 응답 성공률 |
| `outbox-atomicity` | `duplicate_submit_success` | 중복 요청 성공 수 (기대: 1) |
| `outbox-atomicity` | `duplicate_submit_fail` | 중복 요청 실패 수 |

## 기대 결과

### drawing-concurrency.js

- **같은 티켓 (same_ticket)**: `drawing_success_total` = 1, 나머지 9건 실패
- **동일 사용자 (same_user)**: `drawing_success_total` = 1, 나머지 9건 실패
- **다른 번호 (diff_tickets)**: `drawing_success_total` = 10, 전부 성공

### drawing-kafka-fanout.js

- `drawing_success_total` = BOARD_SIZE (50)
- `drawing_win_total` = WINNING_COUNT (10)
- 설문 상태: CLOSED (Fan-out Consumer에 의한 자동 종료)

### survey-response-load.js

- `submit_success_total` >= TARGET_COUNT (50)
- 설문 상태: CLOSED (auto-close Consumer에 의한 자동 종료)

### outbox-atomicity.js

- 기본 시나리오: 첫 응답 성공, 두 번째 응답 실패 (4xx)
- 동시 중복: `duplicate_submit_success` = 1 (나머지 실패)
