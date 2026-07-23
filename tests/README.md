# tests — E2E·비교 하네스(compose·자산)

테스트 실행 진입점(`.sh`)은 모두 `scripts/runners/` 로 모았고, 이 폴더는 그 러너들이 참조하는
**consumer E2E/load 하네스(compose·override·dashboard)와 브로커 비교 자산·다이어그램**만 보관한다.
각 러너는 `REPO_ROOT` 기준 절대경로로 이곳 자산을 찾아가고, 컨슈머 jar 만 `module-consumer/` 에서 빌드한다.

모두 **레포 루트에서** 실행한다.

| 진입점 (scripts/runners/) | 하네스/자산 위치 | 역할 |
|---|---|---|
| `run-all-scenarios.sh` | `scripts/{bin,scenarios,lib}/` | k6 통합 러너 (happy/failover/all) |
| `run-lock-experiment.sh` | `scripts/scenarios/concurrency/` | 분산락 Lock OFF vs ON 실험 |
| `run-e2e.sh` | `tests/e2e/` | SMS 발송-비용 정합 사가 멀티앱 E2E. compose·override·dashboard 는 `tests/e2e/` 에 위치 |
| `run-load.sh` | `tests/e2e/load/` | Kafka 대용량 특성 실증 부하 테스트. 버스트/랙 산출물도 `tests/e2e/load/` 에 생성 |
| `broker-comparison/run-comparison.sh` | `tests/broker-comparison/` | Kafka vs RabbitMQ vs Redis Pub/Sub — 내구 보존·리플레이 비교 데모 |
| `broker-comparison/run-fanout.sh` | `tests/broker-comparison/` | 〃 — 다중 독립 fan-out·나중 합류 소비자 비교 데모 |

## 사용

```bash
# k6 통합 러너
./scripts/runners/run-all-scenarios.sh            # 정상 + 복구 전체
./scripts/runners/run-all-scenarios.sh happy      # 정상만
./scripts/runners/run-all-scenarios.sh failover   # 복구만 (xk6-sql 필요)

# consumer E2E (Docker 필요)
./scripts/runners/run-e2e.sh                      # 전체 실행 후 자동 teardown
KEEP=1 ./scripts/runners/run-e2e.sh               # 스택 유지
SKIP_BUILD=1 ./scripts/runners/run-e2e.sh         # jar 재빌드 생략

# consumer 부하 테스트
N=8000 ./scripts/runners/run-load.sh
```

> 참고: `run-e2e.sh` / `run-load.sh` 는 컨슈머 jar 를 `module-consumer/<consumer>/build/libs/` 에서 마운트한다.
> jar 는 `(cd module-consumer && ./gradlew bootJar)` 로 빌드되므로 `module-consumer/`
> (별도 레포 `sulmoon2yong-consumer` 의 로컬 체크아웃) 가 존재해야 동작한다.
