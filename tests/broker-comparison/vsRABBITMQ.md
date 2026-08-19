# Kafka vs RabbitMQ

## 본질 차이

- **RabbitMQ** = 스마트 브로커 / 큐. 메시지를 큐에 넣고 소비자에게 **밀어주며(push)**, ack 되면 **삭제**한다. 라우팅·재시도·우선순위 같은 "메시지 처리 지능"이 브로커에 있다.
- **Kafka** = 재생 가능한 분산 로그. 메시지를 **소비해도 지우지 않고**(보존 기간까지) 남기며, 소비자가 자기 offset으로 **당겨간다(pull)**. 브로커는 단순하고 지능은 소비자 쪽에 있다.

한 줄로: RabbitMQ는 "전달하고 잊는 큐", Kafka는 "쌓아두고 다시 읽는 로그".

## 축별 비교

| 축 | Kafka | RabbitMQ | 이 워크로드 기준 |
|---|---|---|---|
| 메시지 수명 | 소비해도 보존 (retention) | ack 하면 삭제 | **Kafka** (이벤트 원장) |
| 리플레이(재소비) | offset 리셋으로 전량 재소비 | 클래식 큐는 불가 (Streams는 가능) | **Kafka** |
| 다중 fan-out | `group.id` 만 추가하면 됨 (사전 인프라 0) | exchange + 그룹마다 큐·바인딩 사전 선언 | **Kafka** |
| 나중 합류 소비자 | 로그 보존이라 과거 전량 수신 | 발행 후 만든 큐는 과거 유실 | **Kafka** |
| 처리량 | 파티션 + 순차 I/O + zero-copy → 매우 높음 | 메시지별 ack/상태 부기 → 중간 | **Kafka** (대량 이벤트) |
| 순서 + 병렬 | 파티션(키) 단위 순서 유지하며 소비자 확장 | 단일 큐는 순서 보장, competing consumer로 확장 시 순서 깨짐 | **Kafka** |
| 복잡한 라우팅 | 토픽 + 파티션 키, 필터는 소비자 몫 | topic/headers/direct/fanout exchange 로 풍부 | **RabbitMQ** |
| 작업 큐(정확히 1워커 + 재시도/재큐/우선순위/TTL/지연) | 파티션 단위라 거칠고 별도 장치 필요 | 네이티브 (핵심 강점) | **RabbitMQ** |
| 저지연 · 요청/응답(RPC) | 상대적으로 높은 지연 | 낮은 지연, RPC 패턴 적합 | **RabbitMQ** |
| 운영 단순성 | 무겁다 (KRaft로 개선) | 가볍다 | **RabbitMQ** |

## 데모로 증명된 것 (`run-comparison.sh`, `run-fanout.sh`)

발행 N건 기준, RabbitMQ 열 vs Kafka 열:

| 시나리오 | RabbitMQ | Kafka |
|---|---|---|
| 늦은 구독자 (내구 큐 사전 존재) | N (보존됨) | N |
| **리플레이** (재소비) | **0** (ack 후 삭제) | **N** (offset 리셋) |
| 다중 독립 소비자 3개 | N / N / N (큐 3개 선언 필요) | N / N / N (group.id 뿐) |
| **나중 합류 소비자** | **0** (발행 후 만든 큐는 과거 못 봄) | **N** (로그 보존) |

→ RabbitMQ 도 fan-out·내구 보존은 된다. 차이는 **리플레이 불가**와 **나중 합류 소비자 유실**, 그리고 **소비자마다 큐·바인딩을 미리 만들어야 하는** 구조.

## RabbitMQ 가 더 나은 경우 (정직)

- 메시지별 재시도·재큐·데드레터·우선순위·TTL·지연 발송이 필요한 **작업 분배 큐**
- exchange 기반 **복잡한 라우팅**(라우팅 키/헤더로 선별 전달)
- 낮은 지연, 요청/응답(RPC) 패턴
- 작은 규모에서 **가볍게** 운영

## 이 저장소가 Kafka 를 택한 이유

핵심 요구가 **이벤트 원장 + 다중 fan-out + 리플레이 + 백필**이기 때문:
- `drawing-completed` 을 `drawing-notification` · `sms-cost-calculator` **두 그룹이 독립 구독** (fan-out)
- Outbox → Kafka 로그가 **진실의 원천**, `KafkaReplayActuatorEndpoint` 로 비용 재계산(리플레이)
- 파티션(=3)으로 **순서 유지하며 수평 확장**
- at-least-once + DB커밋 후 offset 커밋으로 유실 없는 전달

이 요구들은 RabbitMQ 클래식 큐로는 어색하거나 불가능하다. (반대로 우리가 라우팅·작업큐 중심이었다면 RabbitMQ가 나았을 것이다.)

## 결론

> 이벤트를 **여러 소비자가 각자, 여러 번, 나중에도** 읽어야 하면 Kafka.
> 메시지를 **정확히 한 워커에게 라우팅해 처리·재시도**하는 게 핵심이면 RabbitMQ.

재현: `./scripts/runners/broker-comparison/run-comparison.sh`, `./scripts/runners/broker-comparison/run-fanout.sh`
