# SMS 발송-비용 정합 사가 — 멀티앱 E2E

2개 컨슈머(`notification` / `dlt`)를 **모두 실제로 띄우고**, Kafka 로 사가 이벤트를
흘려 `cost_record` 최종 상태를 검증하는 **자동 E2E**. 운영 인프라에 의존하지 않고 자체 Kafka/MySQL/Redis 를 띄운다.

## 구성

| 파일 | 역할 |
|---|---|
| `docker-compose.e2e.yml` | 자체 Kafka(단일 KRaft) + MySQL + Redis + 2개 컨슈머. 운영 secret 값은 ENV 로 로컬 격리 |
| `load/` | Kafka 대용량 부하 하네스 (`docker-compose.load*.yml` + 생성 산출물) |
| `phase2/`, `observability/` | 컨슈머 override compose · Grafana 사가 대시보드 |

> 실행 진입점은 `scripts/runners/run-e2e.sh`, compose 하네스는 이 폴더(`tests/e2e/`) 에 있다.
> 컨슈머 jar 는 `module-<consumer>/build/libs/` 에서 마운트하며 `./gradlew :<consumer>:bootJar` 로 빌드된다.

## 실행

백엔드 레포 루트에서:

```bash
./scripts/runners/run-e2e.sh              # 전체 실행 (종료 시 자동 정리)
KEEP=1 ./scripts/runners/run-e2e.sh       # 스택 유지 (로그/DB 확인)
SKIP_BUILD=1 ./scripts/runners/run-e2e.sh # jar 재빌드 생략
```

전제: Docker 데몬 실행 중. 소요 시간 ~4–5분 (대부분 S2 의 MAX_RETRY 소진 대기 ~3분).

## 격리 원칙 (중요)

`application-secret.yml` 에는 **운영 Kafka/DB/AWS/OAuth 크리덴셜**이 들어 있다. E2E 는 이 값에 붙으면 안 되므로
compose 의 `environment` 로 `SPRING_KAFKA_BOOTSTRAP_SERVERS` / `SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*` 를
**로컬 인프라로 강제 오버라이드**한다 (OS 환경변수가 yml 프로퍼티보다 우선). 운영 토픽은 절대 건드리지 않는다.

## 시나리오 ↔ 스펙 매핑

| ID | 시나리오 | 발행 | 기대 결과 | 스펙 |
|----|---|---|---|---|
| **S1** | 당첨 + 발송 성공 | `drawing-completed(isWinner=true)` | `cost_record = CONFIRMED` | Story1 #2 / FR-002 |
| **S2** | 당첨 + 발송 영구 실패 | 위 + notification 실패율 1.0 → DLT | `cost_record = REVERSED` | Story1 #1 / FR-003·007 |
| **S3** | 비당첨 | `drawing-completed(isWinner=false)` | 레코드 없음 (0행) | Edge / FR-004 |
| **S4** | 중복 당첨 | 같은 `drawing-completed` 2회 | `CONFIRMED`, 정확히 1행 | Story2 #2 / FR-005 |
| **S5** | 순서 역전 | `sms-delivery-permanently-failed` **먼저** → `drawing-completed` 나중 | `REVERSED` 로 수렴, 1행 | FR-010 |

### 왜 이 흐름이 사가 전 구간을 태우나

- **S1**: notification 이 `drawing-completed` 소비 트랜잭션에서 SMS 잡 + 비용 PENDING 을 함께 커밋 →
  발송 성공 시 잡 완료 + PENDING→CONFIRMED 를 **한 로컬 트랜잭션**으로 커밋. 잡·비용 정합을 함께 검증.
- **S2**: notification 발송이 MAX_RETRY(5) 소진 → `drawing-notification.DLT` → dlt 컨슈머가 저장 후
  `sms-delivery-permanently-failed` 발행 → notification 의 비용 모듈이 REVERSED. **DLT 경로 + 보상**을 검증.
- **S5**: 확정은 로컬 트랜잭션이라 역전이 불가능하고, 서비스 경계를 넘는 취소 신호만 역전이 남는다.
  permanently-failed 를 먼저 주입해 `CostStateTransition` 의 `null→Insert(REVERSED)` 방어가 동작하는지 확인
  (뒤늦은 drawing-completed 의 PENDING 과 발송 성공 확정은 종결 상태 REVERSED 에서 NoOp 흡수).

## 다루지 않는 것 (별도 검증)

- **SC-002 리플레이 멱등**: `KafkaReplayActuatorEndpoint` (`POST /management/kafkaReplay/beginning`) 로
  offset 리셋 후 재소비 → 누적 비용 불변. 단위/통합 테스트(`KafkaReplayActuatorEndpointTest`, `SmsCostEventListenerTest`)가 커버.
- **FR-013 메트릭 / FR-006a TTL**: `CostMetricsTest`, `CostRecordRetentionWorkerTest` 로 검증.
- **backend 0줄(SC-003)**: 본 E2E 는 컨슈머만 기동하며 backend 이미지를 포함하지 않는다.
