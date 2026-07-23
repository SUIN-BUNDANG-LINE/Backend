# k6 시나리오 관찰 가이드 (F014)

k6 부하 시나리오 실행 시 어느 Grafana 대시보드의 어느 패널을 봐야 하는지,
디버깅 시 어떤 PromQL 쿼리를 던질지 매핑한다.

- 관련 PRD: docs/kafka-distribute-lock/MONITORING-PRD.md (F014)
- 실측 결과: docs/kafka-distribute-lock/MONITORING-RESULTS.md (F015)
- 시나리오 ↔ Kotlin 테스트 매핑: scripts/README.md

## Grafana 대시보드 카탈로그

| UID | 제목 | 책임 도메인 | URL |
|---|---|---|---|
| `outbox-pipeline` | Outbox Pipeline | PENDING / Relay TPS / FAILED | http://localhost:13000/d/outbox-pipeline |
| `sms-failover` | SMS Failover | SMS Jobs / DLT / Attempts | http://localhost:13000/d/sms-failover |
| `drawing-lock` | Drawing Lock | Redisson 분산락 획득/대기 | http://localhost:13000/d/drawing-lock |
| `consumer-fanout` | Consumer Fan-out | Kafka lag / Spring Kafka listener | http://localhost:13000/d/consumer-fanout |
| `cluster-overview` | Cluster Overview | JVM / HTTP / process baseline | http://localhost:13000/d/cluster-overview |
| `race-comparison` | Race Condition Comparison | Lock ON vs OFF + DB deadlock | http://localhost:13000/d/race-comparison |

## 시나리오 관찰 매핑

### 1. drawing-concurrency

| 항목 | 내용 |
|---|---|
| 대상 대시보드 | `drawing-lock` (primary), `race-comparison`, `cluster-overview` (보조) |
| 핵심 패널 | Lock Acquire Rate — by Result · Lock Wait Latency p50/p95/p99 · Lock Success Ratio (5m) · Lock Acquire Rate — by Lock Key Type · Race Detection (race-comparison) |
| 기대 결과 | same_ticket 10 VU → success=1, fail=9 · same_user 10 VU → success=1, fail=9 · diff_tickets 10 VU → success=10 (분산락 mutex 정상). `LOCK_MODE=off`로 동일 endpoint 비교 시 deadlock 27건/회 발생 |
| 디버깅 PromQL | `sum by (result, lock_key_type) (rate(drawing_lock_acquire_total[1m]))` · `histogram_quantile(0.95, sum by (le) (rate(drawing_lock_wait_seconds_bucket[1m])))` · `sum(db_deadlock_total)` |

### 2. drawing-kafka-fanout

| 항목 | 내용 |
|---|---|
| 대상 대시보드 | `consumer-fanout` (primary), `drawing-lock` + `cluster-overview` (보조) |
| 핵심 패널 | Topic: drawing-completed — Produced vs Consumed (rate by group) · Listener TPS Layer 2 · Consumer Lag Layer 1 |
| 기대 결과 | BOARD_SIZE=100 → drawing_win_total=20, 설문 CLOSED, 3 consumer groups (drawing-notification / auto-close / sms-cost-calculator) 모두 동일 메시지 수신 |
| 디버깅 PromQL | `sum by (consumergroup) (kafka_consumergroup_lag{topic="drawing-completed"})` · `sum by (group_id) (rate(spring_kafka_listener_seconds_count{topic="drawing-completed"}[1m]))` |

### 3. outbox-atomicity

| 항목 | 내용 |
|---|---|
| 대상 대시보드 | `outbox-pipeline` (primary), `cluster-overview` (보조) |
| 핵심 패널 | PENDING by aggregate_type & instance · Relay Publish Rate by instance & status · Publish Latency p50/p95/p99 |
| 기대 결과 | duplicate visitorId 중복 응답 1건만 성공 (4xx), outbox 1건만 INSERT 후 즉시 PUBLISHED |
| 디버깅 PromQL | `sum by (aggregate_type) (outbox_events_pending)` · `sum by (status, instance) (rate(outbox_relay_publish_total[1m]))` |

### 4. outbox-relay-recovery

| 항목 | 내용 |
|---|---|
| 대상 대시보드 | `outbox-pipeline` (primary) |
| 핵심 패널 | PENDING (스파이크 → 60초 내 0) · Publish Rate · Publish Latency p95 |
| 기대 결과 | INJECT_COUNT=100 → 60초 내 전부 PUBLISHED, FAILED=0 |
| 디버깅 PromQL | `outbox_events_pending` · `sum(rate(outbox_relay_publish_total{status="success"}[30s]))` · `histogram_quantile(0.95, sum by (le, topic) (rate(outbox_relay_publish_duration_seconds_bucket[1m])))` |

### 5. skip-locked-concurrency

| 항목 | 내용 |
|---|---|
| 대상 대시보드 | `outbox-pipeline` (primary) |
| 핵심 패널 | PENDING (200 스파이크 → 0) · Publish Rate by instance (2개 web 인스턴스 분담) · Publish Latency p95 |
| 기대 결과 | 200건 일괄 INSERT → 전부 PUBLISHED, `retry_count > 0` 행 0개 (SKIP LOCKED 정합성) |
| 디버깅 PromQL | `outbox_events_pending` · `sum by (instance, status) (rate(outbox_relay_publish_total[30s]))` — 두 인스턴스가 동시 분담하면서 충돌 없는 패턴 확인 |

## 비교 시나리오 절차

### Lock OFF vs Lock ON — Deadlock Prevention (F015 핵심)

1. cluster 기동 후 `LOCK_MODE=off ./k6 run drawing-concurrency.js` 3회 실행 → Before 측정
2. Grafana `race-comparison` 대시보드에서 `db_deadlock_total` 누적 카운트 캡처
3. `LOCK_MODE=on ./k6 run drawing-concurrency.js` 3회 실행 → After 측정
4. 동일 대시보드에서 deadlock 추가 발생 없음(0건) 확인
5. p95 latency, 가용성, lock_acquire_total{result="fail"} 비교 표 작성

### P:1 vs P:2 처리량 비교

1. cluster 전체 기동, 임의 시나리오(권장: skip-locked-concurrency) 3회 실행 → P:2 평균/p95 기록
2. `docker stop sulmun2yong-web-2` → P:1으로 전환
3. 동일 시나리오 3회 재실행 → P:1 평균/p95 기록
4. `outbox-pipeline` · `consumer-fanout` 대시보드에서 차이 캡처
5. `docker start sulmun2yong-web-2` → 원상복구

### SKIP LOCKED 전/후 비교

- skip-locked-concurrency.js 실행으로 SKIP LOCKED 정합성 측정 (retry_count=0, 200건 일괄 처리 시간)
- SKIP LOCKED 도입 전 추정치: OutboxRelayIntegrationTest 결과 또는 PR 본문에 기록된 기존 측정값
- 표 형식으로 전·후 처리량/락 대기 시간 비교
