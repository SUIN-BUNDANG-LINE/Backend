# broker-comparison — Kafka vs RabbitMQ vs Redis Pub/Sub

세 브로커의 차이를 **앱 의존 없이 docker + 각 브로커 CLI/HTTP API 로만** 실증한다.
같은 compose(`docker-compose.yml`: kafka/rabbitmq/redis)를 두 러너가 공유한다.

이 저장소가 사가·이벤트소싱 워크로드에서 **Kafka 를 택한 이유**를 숫자로 보여주는 것이 목적이다.
"카프카가 만능"이 아니라 **특정 속성에서 우월**하다는 좁고 정직한 주장이다.

| 러너 | 증명 속성 |
|---|---|
| `run-comparison.sh` | ① 늦은 구독자(내구 보존) ② 리플레이(재소비) |
| `run-fanout.sh` | ③ 다중 독립 fan-out ④ 나중 합류 소비자 |

브로커별 정리 문서: [`vsRABBITMQ.md`](./vsRABBITMQ.md) · [`vsREDIS.md`](./vsREDIS.md)

## 실행

레포 루트에서 (Docker 필요). 각 러너는 스택을 스스로 기동→검증→teardown 한다:

```bash
./scripts/runners/broker-comparison/run-comparison.sh          # 내구 보존 · 리플레이
./scripts/runners/broker-comparison/run-fanout.sh              # fan-out · 나중 합류
N=200 ./scripts/runners/broker-comparison/run-comparison.sh    # 메시지 수 (기본 comparison 200 / fanout 40)
KEEP=1 ./scripts/runners/broker-comparison/run-fanout.sh       # 종료 후 스택 유지
```

## 1) run-comparison.sh — 내구 보존 · 리플레이

```
브로커            발행   늦은구독자   리플레이   성격
Redis Pub/Sub     N      0            —          휘발성 fire-and-forget (유실)
Redis Streams     N      N            N          지속 로그(Kafka 급) — Pub/Sub 과 정반대
RabbitMQ          N      N            0          내구 큐로 보존, ack 후 삭제 → 리플레이 불가
Kafka             N      N            N          내구 로그 + offset 리셋 → 보존·리플레이
```

| 브로커 | 발행 (소비자 없음) | 늦은 구독자 | 리플레이 |
|---|---|---|---|
| Redis Pub/Sub | `PUBLISH` N회 | 발행 뒤 `SUBSCRIBE` (3초 창) → 0 수신 | 히스토리 없음 → 개념 부재 |
| RabbitMQ | 내구 큐 선언 후 default exchange 로 N건 | `queues/../get` `ackmode=ack_requeue_false` → N 수신·제거 | 다시 `get` → 이미 삭제되어 0 |
| Kafka | 토픽에 N건 produce | `--from-beginning` 소비 → N 수신 | 또 다른 독립 소비자가 `--from-beginning` → 다시 N |

## 2) run-fanout.sh — 다중 독립 fan-out · 나중 합류

**한 번 발행**한 메시지를 독립 소비자 3개가 각자 전량 받는가, 그리고 **발행 이후 합류한** 소비자가 받는가.

```
브로커          소비자A  소비자B  소비자C  나중합류   특징
Kafka           N        N        N        N          group.id 만 추가하면 됨
RabbitMQ        N        N        N        0          exchange+큐를 미리 선언·바인딩해야
Redis Pub/Sub   N        N        N        0          동시에 연결돼 있어야만
Redis Streams   N        N        N        N          컨슈머 그룹 + 지속 로그 (Kafka 급)
```

| 브로커 | 3개 독립 소비자 | 나중 합류 |
|---|---|---|
| Kafka | 서로 다른 `group.id` 3개가 `--from-beginning` → 각자 N (사전 인프라 0) | 새 group.id → 로그 보존이라 N |
| RabbitMQ | fanout exchange + 큐 3개 사전 선언·바인딩 → 각 큐 N | 발행 뒤 만든 큐 → 과거 유실 0 |
| Redis Pub/Sub | 구독자 3개 동시 연결 → 각자 N (live fan-out) | 발행 뒤 연결 → 유실 0 |
| Redis Streams | 컨슈머 그룹 3개가 각자 `0`부터 → 각자 N | 나중 만든 그룹도 `0`부터 → N |

> 사전 등록된 소비자에겐 넷 다 fan-out 된다. 카프카/Streams 는 **나중에 합류한 소비자도 전량**을 받고(지속 로그), RabbitMQ 클래식 큐·Pub/Sub 은 못 받는다.
> 참고: RabbitMQ 는 한 큐에 여러 소비자를 붙이면 fan-out 이 아니라 **작업 분배(competing consumer)** 가 된다. Redis Pub/Sub 은 작업 분배 자체가 불가(모든 구독자가 전량 수신).

## 정직한 한계

- Redis Pub/Sub 은 애초에 휘발성 도구다 — "유실"은 버그가 아니라 설계다. Redis 로 지속·재소비가 필요하면 **Streams**(데모에 포함) 를 쓴다.
- **Redis Streams** 는 보존·리플레이·fan-out 에서 Kafka 급이다. Kafka 의 우위는 이 데모가 재현하지 못하는 **파티션 수평 확장·대용량 장기 보존·강한 내구성** 축에 있다 → [`vsREDIS.md`](./vsREDIS.md) 참조.
- RabbitMQ 도 Streams(3.9+) 를 쓰면 리플레이가 가능하다. 여기서 비교한 건 **클래식 큐**다.
- 처리량·지연·라우팅 유연성 등 다른 축은 측정하지 않는다. 그 축에서는 RabbitMQ·Redis 가 유리한 경우도 많다.
