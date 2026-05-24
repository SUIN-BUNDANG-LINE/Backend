# k6 부하·E2E 테스트 스크립트

## 개요

Phase 1 전체 이벤트 파이프라인(정상 + 장애/복구)을 k6로 검증한다.
HTTP 트래픽은 k6 표준 모듈로, DB 검증은 **xk6-sql** 확장으로 수행한다.

## 디렉토리 구조

```
k6_scripts/
├── lib/
│   ├── config.js                  # 공통 설정 (BASE_URL, 인증)
│   ├── helpers.js                 # API 호출 헬퍼
│   └── db.js                      # MySQL 직접 검증 헬퍼 (xk6-sql)
├── drawing-concurrency.js         # 정상 — 추첨 동시성
├── drawing-kafka-fanout.js        # 정상 — 추첨 Fan-out + 자동 종료
├── outbox-atomicity.js            # 정상 — Outbox 원자성 (중복 방어)
├── outbox-relay-recovery.js       # 복구 — Outbox Relay (SKIP LOCKED + age filter)
├── outbox-producer-dlq.js         # 페일오버 — Producer DLQ (5회 재시도 → FAILED)
├── skip-locked-concurrency.js     # 복구 — SKIP LOCKED 정합성 (200건 일괄 발행)
├── sms-failover.js                # 페일오버 — SMS Inbox 재시도 + DLT 라우팅
├── run-all-scenarios.sh           # 통합 러너 (happy/failover/all 모드)
└── README.md
```

## 시나리오 매핑

### 정상 (Happy Path) — 3종

| k6 스크립트 | 원본 Kotlin 테스트 | 핵심 검증 |
|---|---|---|
| `drawing-concurrency.js` | `DrawingBoardServiceConcurrencyTest` | 분산락 동시성 (같은 티켓/같은 사용자/다른 번호) |
| `drawing-kafka-fanout.js` | `DrawingKafkaIntegrationTest` | 100명 추첨 → 티켓 소진 → 설문 CLOSED |
| `outbox-atomicity.js` | `OutboxAtomicityIntegrationTest` | 중복 visitorId → 1건만 성공 |

### 장애/복구 (Failover) — 4종 (xk6-sql 필요)

| k6 스크립트 | 검증 대상 | 원본 Kotlin 테스트 |
|---|---|---|
| `outbox-relay-recovery.js` | Phase 1-8 — Relay가 PENDING 100건을 SKIP LOCKED로 발행 | `OutboxRelayIntegrationTest` |
| `outbox-producer-dlq.js` | Phase 1-10 — 5회 재시도 후 status=FAILED, retry_count=5 | (신규 — Phase 1-10) |
| `skip-locked-concurrency.js` | Phase 1-10 — 200건 일괄 INSERT 후 retry_count=0 정합 | (신규 — Phase 1-10) |
| `sms-failover.js` | Phase 1-7 확장 — SMS 5회 실패 → dlt_messages 적재 | `SmsFailoverIntegrationTest` |

## 사전 준비

### 1. k6 설치 (정상 시나리오만 실행할 경우)

```bash
# macOS
brew install k6

# Docker
docker pull grafana/k6
```

### 1-bis. 커스텀 k6 바이너리 빌드 (장애/복구 시나리오 실행 시 필수)

장애/복구 4종 스크립트는 MySQL 직접 검증을 위해 **xk6-sql** 확장이 필요합니다.

```bash
# Go 1.21+ 필요
go install go.k6.io/xk6/cmd/xk6@latest

xk6 build \
  --with github.com/grafana/xk6-sql@latest \
  --with github.com/grafana/xk6-sql-driver-mysql@latest

mv ./k6 k6_scripts/k6-custom        # k6_scripts 디렉토리로 이동
```

이후 `./k6_scripts/k6-custom run <script>.js` 형태로 실행합니다.
표준 `k6` 바이너리로 장애/복구 스크립트를 실행하면 `import 'k6/x/sql'`에서 실패합니다.

### 1-ter. 환경변수 — DB DSN

```bash
export DB_DSN="user:password@tcp(localhost:3307)/test?parseTime=true"
```

`docker-compose.yml`의 `${MYSQL_USER}:${MYSQL_PASSWORD}` 값과 일치해야 합니다.

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

### 통합 러너 (권장)

```bash
cd k6_scripts
chmod +x run-all-scenarios.sh

./run-all-scenarios.sh           # 정상 + 장애/복구 전체 실행
./run-all-scenarios.sh happy     # 정상 시나리오만 (4종)
./run-all-scenarios.sh failover  # 장애/복구 시나리오만 (4종, xk6-sql 필요)

# SMS 시나리오 건너뛰기 (forcedFailCount 설정 안 한 경우)
SKIP_SMS=1 ./run-all-scenarios.sh failover
```

### 개별 실행 (정상 시나리오, 표준 k6)

```bash
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-concurrency.js
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-kafka-fanout.js
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN outbox-atomicity.js
```

### 개별 실행 (장애/복구 시나리오, 커스텀 k6)

```bash
./k6-custom run --env DB_DSN="$DB_DSN" outbox-relay-recovery.js
./k6-custom run --env DB_DSN="$DB_DSN" outbox-producer-dlq.js
./k6-custom run --env DB_DSN="$DB_DSN" skip-locked-concurrency.js

# SMS Failover: 사전에 application-secret.yml 수정 + 서버 재기동 필요
#   loggingSmsSender:
#     forcedFailCount: 0   # 0 = 항상 실패
./k6-custom run --env ACCESS_TOKEN=$ACCESS_TOKEN --env DB_DSN="$DB_DSN" sms-failover.js
```

### 환경변수 커스터마이징

```bash
# 서버 주소 변경
k6 run --env BASE_URL=http://staging.example.com --env ACCESS_TOKEN=$ACCESS_TOKEN drawing-concurrency.js

# Fan-out 테스트 파라미터 조정
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN \
       --env BOARD_SIZE=100 \
       --env WINNING_COUNT=20 \
       --env PARTICIPANT_COUNT=100 \
       drawing-kafka-fanout.js
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
| `outbox-atomicity` | `duplicate_submit_success` | 중복 요청 성공 수 (기대: 1) |
| `outbox-atomicity` | `duplicate_submit_fail` | 중복 요청 실패 수 |

## 기대 결과

### drawing-concurrency.js

- **같은 티켓 (same_ticket)**: `drawing_success_total` = 1, 나머지 9건 실패
- **동일 사용자 (same_user)**: `drawing_success_total` = 1, 나머지 9건 실패
- **다른 번호 (diff_tickets)**: `drawing_success_total` = 10, 전부 성공

### drawing-kafka-fanout.js

- `drawing_success_total` = BOARD_SIZE (100)
- `drawing_win_total` = WINNING_COUNT (20)
- 설문 상태: CLOSED (drawing-auto-close Consumer에 의한 추첨 소진 종료)

### outbox-atomicity.js

- 기본 시나리오: 첫 응답 성공, 두 번째 응답 실패 (4xx)
- 동시 중복: `duplicate_submit_success` = 1 (나머지 실패)

### outbox-relay-recovery.js

- 주입한 `INJECT_COUNT`(기본 100)건이 60초 내 모두 PUBLISHED
- `outbox_events.aggregate_type='TEST_K6'` 행은 teardown에서 cleanup

### outbox-producer-dlq.js

- 잘못된 토픽으로 PENDING 1건 주입
- 6분 내 `status='FAILED'` + `retry_count=5` 도달
- Kafka 클러스터의 `auto.create.topics.enable=false`가 권장 (없으면 토픽 자동 생성으로 시나리오 무력화 가능)

### skip-locked-concurrency.js

- 200건 일괄 주입 후 모두 PUBLISHED
- `retry_count > 0`인 행이 0개 → SKIP LOCKED가 락 충돌 없이 한 번에 처리했음을 의미
- PENDING/FAILED 잔량 모두 0

### sms-failover.js

- 추첨 1건 100% 당첨 (boardSize=1)
- 90초 내 `sms_notification_jobs.status='FAILED'` (retry_count=5) + `dlt_messages` 1건 추가
- 실행 전 `application-secret.yml`에 `loggingSmsSender.forcedFailCount: 0` 설정 후 서버 재기동 필수
