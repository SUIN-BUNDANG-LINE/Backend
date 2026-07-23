#!/usr/bin/env bash
# 브로커 비교 데모 — "발행 시점에 구독자가 없을 때 / 재소비(리플레이)할 때" 각 브로커의 차이를 실증한다.
#
# 증명하는 속성 (사가·이벤트소싱 워크로드에서 Kafka 를 택한 이유):
#   ① 늦은 구독자 : 발행 뒤 붙은 소비자가 과거 메시지를 받는가 (내구 보존)
#   ② 리플레이    : 이미 소비한 메시지를 처음부터 다시 받을 수 있는가
#
# 기대 결과:
#   Redis Pub/Sub : 늦은구독자 0 (유실)         · 리플레이 불가   → 휘발성 fire-and-forget
#   Redis Streams : 늦은구독자 N                 · 리플레이 N      → 지속 로그(Kafka 급). Pub/Sub 과 정반대
#   RabbitMQ      : 늦은구독자 N (내구 큐 보존)  · 리플레이 0      → 보존은 되나 ack 후 삭제
#   Kafka         : 늦은구독자 N                 · 리플레이 N      → 내구 로그 + offset 리셋
#
# 사용 (레포 루트에서):
#   ./scripts/runners/broker-comparison/run-comparison.sh          # 전체 실행 후 자동 teardown
#   N=500 ./scripts/runners/broker-comparison/run-comparison.sh    # 메시지 수 조정
#   KEEP=1 ./scripts/runners/broker-comparison/run-comparison.sh   # 종료 후 스택 유지
set -uo pipefail

# 이 스크립트는 scripts/runners/broker-comparison/ 로 세 단계 깊이 → repo 루트는 세 단계 위.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE="docker compose -f ${REPO_ROOT}/tests/broker-comparison/docker-compose.yml"
N=${N:-200}

K=bcmp-kafka; R=bcmp-redis
RMQ="http://localhost:15672/api"; AUTH="guest:guest"
TOPIC=bcmp-topic; CH=bcmp-ch; Q=bcmp-q; ST=bcmp-stream

log()  { printf '\n\033[1;36m[BCMP]\033[0m %s\n' "$*"; }
ok()   { printf '  \033[1;32m✔\033[0m %s\n' "$*"; }
bad()  { printf '  \033[1;31mx\033[0m %s\n' "$*"; }

teardown() {
    if [[ "${KEEP:-0}" == "1" ]]; then log "KEEP=1 — 스택 유지. 정리: $COMPOSE down -v"; else log "teardown"; $COMPOSE down -v >/dev/null 2>&1; fi
}
trap teardown EXIT

kexec() { docker exec "$K" /opt/kafka/bin/"$@"; }
rmq()   { curl -s -u "$AUTH" -H "content-type:application/json" "$@"; }

# ── 0) 기동 & 준비 ───────────────────────────────────────────────────────────
log "브로커 3종 기동 (kafka / rabbitmq / redis)"
$COMPOSE up -d --force-recreate >/dev/null 2>&1

log "Kafka 준비 대기 (콜드 이미지면 첫 부팅이 느릴 수 있음)"
for i in $(seq 1 60); do kexec kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1 && break; sleep 2; (( i==60 )) && { bad "Kafka 미기동"; exit 1; }; done

log "Redis 준비 대기"
for i in $(seq 1 20); do [[ "$(docker exec "$R" redis-cli ping 2>/dev/null)" == "PONG" ]] && break; sleep 2; (( i==20 )) && { bad "Redis 미기동"; exit 1; }; done

log "RabbitMQ management API 준비 대기 (부팅 ~20-40s)"
for i in $(seq 1 60); do rmq "$RMQ/overview" 2>/dev/null | grep -q rabbitmq_version && break; sleep 2; (( i==60 )) && { bad "RabbitMQ 미기동"; exit 1; }; done

# ══════════════════════════════════════════════════════════════════════════════
# Redis Pub/Sub — 구독자 없이 발행 → 뒤늦게 구독 → 유실
# ══════════════════════════════════════════════════════════════════════════════
log "Redis Pub/Sub: 구독자 없이 ${N}건 발행"
docker exec "$R" sh -c "for i in \$(seq 1 $N); do redis-cli PUBLISH $CH msg-\$i >/dev/null; done"
# 발행 이후 구독 → 과거분은 못 받음 (3초 창)
R_LATE=$(docker exec "$R" sh -c "timeout 3 redis-cli SUBSCRIBE $CH" 2>/dev/null | grep -c '^message$')
R_REPLAY="—" # 히스토리 없음 → 리플레이 개념 자체가 없음

# ══════════════════════════════════════════════════════════════════════════════
# Redis Streams — Pub/Sub 과 달리 지속 로그. 소비자 없이 XADD → 뒤늦게 XRANGE(N) → 재조회(N)
# (같은 Redis 라도 Pub/Sub 은 휘발, Streams 는 Kafka 급에 가깝다는 걸 대조)
# ══════════════════════════════════════════════════════════════════════════════
log "Redis Streams: 소비자 없이 XADD ${N}건 (지속 로그)"
docker exec "$R" sh -c "for i in \$(seq 1 $N); do redis-cli XADD $ST '*' v msg-\$i >/dev/null; done"
# 뒤늦게 처음부터 조회 (XRANGE 은 소비하지 않음)
S_LATE=$(docker exec "$R" redis-cli XRANGE "$ST" - + 2>/dev/null | grep -cE '[0-9]{13}-[0-9]')
# 리플레이 — 다시 읽어도 전량 (소비해도 지워지지 않음)
S_REPLAY=$(docker exec "$R" redis-cli XRANGE "$ST" - + 2>/dev/null | grep -cE '[0-9]{13}-[0-9]')

# ══════════════════════════════════════════════════════════════════════════════
# RabbitMQ — 내구 큐에 소비자 없이 발행 → 뒤늦게 소비(N) → 재소비(0, ack 후 삭제)
# ══════════════════════════════════════════════════════════════════════════════
log "RabbitMQ: 내구 큐 선언 + 소비자 없이 ${N}건 발행"
rmq -XPUT "$RMQ/queues/%2F/$Q" -d '{"durable":true}' >/dev/null
for i in $(seq 1 "$N"); do
    rmq -XPOST "$RMQ/exchanges/%2F/amq.default/publish" \
        -d "{\"properties\":{\"delivery_mode\":2},\"routing_key\":\"$Q\",\"payload\":\"msg-$i\",\"payload_encoding\":\"string\"}" >/dev/null
done
# 뒤늦게 소비 (ack 하여 제거)
Q_LATE=$(rmq -XPOST "$RMQ/queues/%2F/$Q/get" -d "{\"count\":$N,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}" | grep -o '"payload":"msg-' | wc -l | tr -d ' ')
# 리플레이 시도 — 이미 ack 되어 큐가 비었으므로 0
Q_REPLAY=$(rmq -XPOST "$RMQ/queues/%2F/$Q/get" -d "{\"count\":$N,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}" | grep -o '"payload":"msg-' | wc -l | tr -d ' ')

# ══════════════════════════════════════════════════════════════════════════════
# Kafka — 소비자 없이 발행 → 뒤늦게 처음부터 소비(N) → 독립 소비자가 재소비(N)
# ══════════════════════════════════════════════════════════════════════════════
log "Kafka: 토픽 생성 + 소비자 없이 ${N}건 발행"
kexec kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic "$TOPIC" --partitions 1 --replication-factor 1 >/dev/null 2>&1
docker exec -i "$K" /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" \
    < <(seq 1 "$N" | sed 's/^/msg-/') >/dev/null 2>&1
# 뒤늦게 처음부터 소비 (--from-beginning, 임의 그룹)
K_LATE=$(kexec kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" --from-beginning --max-messages "$N" --timeout-ms 10000 2>/dev/null | grep -c '^msg-')
# 리플레이 — 또 다른 독립 소비자가 로그를 처음부터 다시 읽음
K_REPLAY=$(kexec kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" --from-beginning --max-messages "$N" --timeout-ms 10000 2>/dev/null | grep -c '^msg-')

# ── 결과 ─────────────────────────────────────────────────────────────────────
log "════════ 결과 (발행 N=$N) ════════"
printf '  %-16s %-12s %-14s %-12s %s\n' "브로커" "발행" "늦은구독자" "리플레이" "성격"
printf '  %-16s %-12s %-14s %-12s %s\n' "Redis Pub/Sub" "$N" "$R_LATE" "$R_REPLAY"  "휘발성(유실)"
printf '  %-16s %-12s %-14s %-12s %s\n' "Redis Streams" "$N" "$S_LATE" "$S_REPLAY"  "지속 로그(Kafka급)"
printf '  %-16s %-12s %-14s %-12s %s\n' "RabbitMQ"      "$N" "$Q_LATE" "$Q_REPLAY"   "내구 큐, 리플레이X"
printf '  %-16s %-12s %-14s %-12s %s\n' "Kafka"         "$N" "$K_LATE" "$K_REPLAY"   "내구 로그 + 리플레이"

log "판정"
[[ "$R_LATE" == "0" ]]                         && ok "Redis Pub/Sub: 발행 전 메시지 전량 유실 (늦은구독자=0)"        || bad "Redis Pub/Sub 예상과 다름 (늦은구독자=$R_LATE, 기대 0)"
[[ "$S_LATE" == "$N" && "$S_REPLAY" == "$N" ]] && ok "Redis Streams: 지속 로그라 보존($S_LATE) + 재조회($S_REPLAY) 성립 — Pub/Sub 과 정반대" || bad "Redis Streams 예상과 다름 (늦은구독자=$S_LATE, 리플레이=$S_REPLAY / 기대 $N,$N)"
[[ "$Q_LATE" == "$N" && "$Q_REPLAY" == "0" ]]  && ok "RabbitMQ: 내구 큐로 보존($Q_LATE) 되나 ack 후 재소비 불가($Q_REPLAY)" || bad "RabbitMQ 예상과 다름 (늦은구독자=$Q_LATE, 리플레이=$Q_REPLAY / 기대 $N,0)"
[[ "$K_LATE" == "$N" && "$K_REPLAY" == "$N" ]] && ok "Kafka: 보존($K_LATE) + 리플레이($K_REPLAY) 모두 성립"            || bad "Kafka 예상과 다름 (늦은구독자=$K_LATE, 리플레이=$K_REPLAY / 기대 $N,$N)"
