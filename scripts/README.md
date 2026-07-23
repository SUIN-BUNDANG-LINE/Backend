# k6 부하·E2E 테스트 스크립트

## 개요

Phase 1 전체 이벤트 파이프라인(정상 + 장애/복구)을 k6로 검증한다.
HTTP 트래픽은 k6 표준 모듈로, DB 검증은 **xk6-sql** 확장으로 수행한다.

## 디렉토리 구조

```
scripts/
├── bin/                            # k6 / k6-custom 바이너리
├── lib/                            # 공통 헬퍼 (시나리오가 ../../lib 로 참조)
│   ├── config.js                   # 공통 설정 (BASE_URL, 인증)
│   ├── helpers.js                  # API 호출 헬퍼
│   └── db.js                       # MySQL 직접 검증 헬퍼 (xk6-sql)
├── scenarios/                      # 책임별 k6 시나리오
│   ├── concurrency/
│   │   ├── drawing-concurrency.js      # Redisson 분산락 동시성
│   │   └── skip-locked-concurrency.js  # SKIP LOCKED 정합성 (200건 일괄)
│   ├── outbox/
│   │   ├── outbox-atomicity.js         # Outbox 원자성 (중복 방어)
│   │   └── outbox-relay-recovery.js    # Outbox Relay 복구
│   ├── kafka/
│   │   └── drawing-kafka-fanout.js     # 추첨 Fan-out + 자동 종료
│   └── saga/
│       └── saga-cost-integrity.js      # SMS 비용 정합 SAGA
├── runners/                        # 실행 진입점 .sh (레포 루트에서 실행)
│   ├── run-all-scenarios.sh            # k6 통합 러너 (happy/failover/all)
│   ├── run-lock-experiment.sh          # 분산락 Lock OFF vs ON 실험
│   ├── run-e2e.sh                       # consumer E2E (tests/e2e 하네스)
│   ├── run-load.sh                      # consumer 부하 (tests/e2e/load 하네스)
│   └── broker-comparison/               # 브로커 비교 데모
├── MONITORING.md
└── README.md
```

## 시나리오 매핑

### 정상 (Happy Path) — 3종

| k6 스크립트 | 원본 Kotlin 테스트 | 핵심 검증 |
|---|---|---|
| `drawing-concurrency.js` | `DrawingBoardServiceConcurrencyTest` | 분산락 동시성 (같은 티켓/같은 사용자/다른 번호) |
| `drawing-kafka-fanout.js` | `DrawingKafkaIntegrationTest` | 100명 추첨 → 티켓 소진 → 설문 CLOSED |
| `outbox-atomicity.js` | `OutboxAtomicityIntegrationTest` | 중복 visitorId → 1건만 성공 |

### 복구 (Relay) — 2종 (xk6-sql 필요)

| k6 스크립트 | 검증 대상 | 원본 Kotlin 테스트 |
|---|---|---|
| `outbox-relay-recovery.js` | Phase 1-8 — Relay가 PENDING 100건을 SKIP LOCKED로 발행 | `OutboxRelayIntegrationTest` |
| `skip-locked-concurrency.js` | Phase 1-10 — 200건 일괄 INSERT 후 retry_count=0 정합 | (신규 — Phase 1-10) |

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

mv ./k6 scripts/bin/k6-custom       # scripts/bin 으로 이동
```

이후 `./scripts/bin/k6-custom run <script>.js` 형태로 실행합니다.
표준 `k6` 바이너리로 장애/복구 스크립트를 실행하면 `import 'k6/x/sql'`에서 실패합니다.

### 1-ter. 환경변수 — DB DSN

```bash
export DB_DSN="user:password@tcp(localhost:3307)/test?parseTime=true"
```

`docker-compose.yml`의 `${MYSQL_USER}:${MYSQL_PASSWORD}` 값과 일치해야 합니다.

### 2. JWT 토큰 획득

설문 생성/시작 API는 인증이 필요합니다. 테스트용 발급 엔드포인트를 쓰면 브라우저 로그인 없이 얻을 수 있습니다.

```bash
# 서버를 test-auth 켜서 실행: TEST_AUTH_ENABLED=true ./gradlew :web:bootRun
export ACCESS_TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/test/token | jq -r .accessToken)

# (대안) 브라우저 개발자 도구 → Application → Cookies → access-token 값 복사
# export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
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
# 레포 루트에서 실행
./scripts/runners/run-all-scenarios.sh           # 정상 + 복구 전체 실행
./scripts/runners/run-all-scenarios.sh happy     # 정상 시나리오만
./scripts/runners/run-all-scenarios.sh failover  # 복구 시나리오만 (2종, xk6-sql 필요)
```

### 개별 실행 (정상 시나리오, 표준 k6)

```bash
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN scripts/scenarios/concurrency/drawing-concurrency.js
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN scripts/scenarios/kafka/drawing-kafka-fanout.js
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN scripts/scenarios/outbox/outbox-atomicity.js
```

### 개별 실행 (장애/복구 시나리오, 커스텀 k6)

```bash
./scripts/bin/k6-custom run --env DB_DSN="$DB_DSN" scripts/scenarios/outbox/outbox-relay-recovery.js
./scripts/bin/k6-custom run --env DB_DSN="$DB_DSN" scripts/scenarios/concurrency/skip-locked-concurrency.js
```

### 환경변수 커스터마이징

```bash
# 서버 주소 변경
k6 run --env BASE_URL=http://staging.example.com --env ACCESS_TOKEN=$ACCESS_TOKEN scripts/scenarios/concurrency/drawing-concurrency.js

# Fan-out 테스트 파라미터 조정
k6 run --env ACCESS_TOKEN=$ACCESS_TOKEN \
       --env BOARD_SIZE=100 \
       --env WINNING_COUNT=20 \
       --env PARTICIPANT_COUNT=100 \
       scripts/scenarios/kafka/drawing-kafka-fanout.js
```

### Docker로 실행

```bash
docker run --rm -i \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ACCESS_TOKEN=$ACCESS_TOKEN \
  -v $(pwd)/scripts:/scripts \
  grafana/k6 run /scripts/scenarios/concurrency/drawing-concurrency.js
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

### skip-locked-concurrency.js

- 200건 일괄 주입 후 모두 PUBLISHED
- `retry_count > 0`인 행이 0개 → SKIP LOCKED가 락 충돌 없이 한 번에 처리했음을 의미
- PENDING/FAILED 잔량 모두 0
