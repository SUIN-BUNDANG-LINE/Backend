#!/usr/bin/env bash
# 브로커 Fan-out 비교 데모 — "한 번 발행한 메시지를 독립 소비자 여럿이 각자 전량 받는가,
# 그리고 나중에 합류한 소비자도 받는가".
#
# 증명 대상:
#   ① 다중 독립 fan-out : 소비자(그룹) 3개가 같은 발행분을 각자 N건 전량 수신
#   ② 나중 합류         : 발행 이후에 등장한 소비자가 과거분을 받는가
#
# 기대 결과:
#   Kafka         : 3그룹 각 N · 나중합류 N  → 새 group.id 만 추가하면 언제든 전량 소비
#   RabbitMQ      : 3큐 각 N · 나중합류 0    → fanout exchange+큐를 미리 선언해야 하고, 발행 후 만든 큐는 과거 못 봄
#   Redis Pub/Sub : 3구독 각 N · 나중합류 0  → 동시에 연결돼 있어야만 live fan-out, 나중 연결은 유실
#
# 사용 (레포 루트에서):
#   ./scripts/runners/broker-comparison/run-fanout.sh
#   N=100 ./scripts/runners/broker-comparison/run-fanout.sh
#   KEEP=1 ./scripts/runners/broker-comparison/run-fanout.sh
set -uo pipefail

# scripts/runners/broker-comparison/ 로 세 단계 깊이 → repo 루트는 세 단계 위.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE="docker compose -f ${REPO_ROOT}/tests/broker-comparison/docker-compose.yml"
N=${N:-40}
RUN=$(date +%s) # 재실행 격리용 접미사 (그룹/큐/토픽 이름)

K=bcmp-kafka; R=bcmp-redis
RMQ="http://localhost:15672/api"; AUTH="guest:guest"
TOPIC="fan-$RUN"; CH="fanch-$RUN"; FX="bcmp.fx.$RUN"; STR="fanstream-$RUN"

log()  { printf '\n\033[1;36m[FANOUT]\033[0m %s\n' "$*"; }
ok()   { printf '  \033[1;32m✔\033[0m %s\n' "$*"; }
bad()  { printf '  \033[1;31mx\033[0m %s\n' "$*"; }

teardown() {
    if [[ "${KEEP:-0}" == "1" ]]; then log "KEEP=1 — 스택 유지. 정리: $COMPOSE down -v"; else log "teardown"; $COMPOSE down -v >/dev/null 2>&1; fi
}
trap teardown EXIT

kexec() { docker exec "$K" /opt/kafka/bin/"$@"; }
rmq()   { curl -s -u "$AUTH" -H "content-type:application/json" "$@"; }
# Kafka: 이름 그룹으로 처음부터 소비 → 수신 건수
kconsume() { kexec kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" --from-beginning --group "$1" --max-messages "$N" --timeout-ms 10000 2>/dev/null | grep -c '^msg-'; }
# RabbitMQ: 큐 선언 + fanout exchange 에 바인딩
rq_bind() { rmq -XPUT "$RMQ/queues/%2F/$1" -d '{"durable":true}' >/dev/null; rmq -XPOST "$RMQ/bindings/%2F/e/$FX/q/$1" -d '{"routing_key":""}' >/dev/null; }
rq_drain(){ rmq -XPOST "$RMQ/queues/%2F/$1/get" -d "{\"count\":$N,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}" | grep -o '"payload":"msg-' | wc -l | tr -d ' '; }

# ── 0) 기동 & 준비 ───────────────────────────────────────────────────────────
log "브로커 3종 기동"
$COMPOSE up -d --force-recreate >/dev/null 2>&1
log "Kafka 준비 대기"
for i in $(seq 1 60); do kexec kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1 && break; sleep 2; (( i==60 )) && { bad "Kafka 미기동"; exit 1; }; done
log "Redis 준비 대기"
for i in $(seq 1 20); do [[ "$(docker exec "$R" redis-cli ping 2>/dev/null)" == "PONG" ]] && break; sleep 2; (( i==20 )) && { bad "Redis 미기동"; exit 1; }; done
log "RabbitMQ management API 준비 대기"
for i in $(seq 1 60); do rmq "$RMQ/overview" 2>/dev/null | grep -q rabbitmq_version && break; sleep 2; (( i==60 )) && { bad "RabbitMQ 미기동"; exit 1; }; done

# ══════════════════════════════════════════════════════════════════════════════
# Kafka — 토픽 하나에 발행 → 서로 다른 group.id 3개가 각자 전량 → 나중 group 도 전량
# ══════════════════════════════════════════════════════════════════════════════
log "Kafka: 토픽 발행 ${N}건 → 독립 그룹 3개가 각자 소비 (사전 인프라 0, group.id 만 다름)"
kexec kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic "$TOPIC" --partitions 1 --replication-factor 1 >/dev/null 2>&1
docker exec -i "$K" /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" < <(seq 1 "$N" | sed 's/^/msg-/') >/dev/null 2>&1
K_A=$(kconsume "gA-$RUN"); K_B=$(kconsume "gB-$RUN"); K_C=$(kconsume "gC-$RUN")
K_LATE=$(kconsume "gLATE-$RUN") # 위 3그룹이 다 읽은 뒤 새 그룹 합류

# ══════════════════════════════════════════════════════════════════════════════
# RabbitMQ — fanout exchange + 큐 3개 사전 바인딩 → 발행 → 각 큐 전량. 나중 큐는 유실
# ══════════════════════════════════════════════════════════════════════════════
log "RabbitMQ: fanout exchange + 큐 3개 사전 선언·바인딩 → 발행 ${N}건"
rmq -XPUT "$RMQ/exchanges/%2F/$FX" -d '{"type":"fanout","durable":true}' >/dev/null
rq_bind "qA-$RUN"; rq_bind "qB-$RUN"; rq_bind "qC-$RUN"
for i in $(seq 1 "$N"); do
    rmq -XPOST "$RMQ/exchanges/%2F/$FX/publish" -d "{\"properties\":{\"delivery_mode\":2},\"routing_key\":\"\",\"payload\":\"msg-$i\",\"payload_encoding\":\"string\"}" >/dev/null
done
Q_A=$(rq_drain "qA-$RUN"); Q_B=$(rq_drain "qB-$RUN"); Q_C=$(rq_drain "qC-$RUN")
rq_bind "qLATE-$RUN"                # 발행 후에 큐 선언·바인딩
Q_LATE=$(rq_drain "qLATE-$RUN")     # 과거 발행분은 이 큐에 없음 → 0

# ══════════════════════════════════════════════════════════════════════════════
# Redis Pub/Sub — 구독자 3개 동시 연결 → 발행 → 각자 전량. 나중 연결은 유실
# ══════════════════════════════════════════════════════════════════════════════
log "Redis Pub/Sub: 구독자 3개 동시 연결 후 발행 ${N}건 (컨테이너 내부에서 병렬 처리)"
REDIS_OUT=$(docker exec "$R" sh -c "
  timeout 6 redis-cli SUBSCRIBE $CH > /tmp/a 2>/dev/null &
  timeout 6 redis-cli SUBSCRIBE $CH > /tmp/b 2>/dev/null &
  timeout 6 redis-cli SUBSCRIBE $CH > /tmp/c 2>/dev/null &
  sleep 2
  for i in \$(seq 1 $N); do redis-cli PUBLISH $CH msg-\$i >/dev/null; done
  wait
  timeout 3 redis-cli SUBSCRIBE $CH > /tmp/d 2>/dev/null &   # 발행 뒤 합류
  sleep 1; wait 2>/dev/null
  echo \"A=\$(grep -c '^message\$' /tmp/a) B=\$(grep -c '^message\$' /tmp/b) C=\$(grep -c '^message\$' /tmp/c) D=\$(grep -c '^message\$' /tmp/d)\"
")
R_A=$(sed -n 's/.*A=\([0-9]*\).*/\1/p' <<<"$REDIS_OUT"); R_B=$(sed -n 's/.*B=\([0-9]*\).*/\1/p' <<<"$REDIS_OUT")
R_C=$(sed -n 's/.*C=\([0-9]*\).*/\1/p' <<<"$REDIS_OUT"); R_LATE=$(sed -n 's/.*D=\([0-9]*\).*/\1/p' <<<"$REDIS_OUT")

# ══════════════════════════════════════════════════════════════════════════════
# Redis Streams — 지속 로그 + 컨슈머 그룹. 그룹 3개가 각자 전량 → 나중 합류 그룹도 전량
# (Pub/Sub 과 달리 Kafka 처럼 다중 독립 그룹 fan-out + 나중 합류가 된다)
# ══════════════════════════════════════════════════════════════════════════════
log "Redis Streams: XADD ${N}건 → 컨슈머 그룹 3개가 각자 소비 (0부터) → 나중 합류 그룹"
STREAM_OUT=$(docker exec "$R" sh -c "
  for i in \$(seq 1 $N); do redis-cli XADD $STR '*' v msg-\$i >/dev/null; done
  for g in gA gB gC; do redis-cli XGROUP CREATE $STR \$g 0 >/dev/null 2>&1; done
  A=\$(redis-cli XREADGROUP GROUP gA cA COUNT $N STREAMS $STR '>' | grep -cE '[0-9]{13}-[0-9]')
  B=\$(redis-cli XREADGROUP GROUP gB cB COUNT $N STREAMS $STR '>' | grep -cE '[0-9]{13}-[0-9]')
  C=\$(redis-cli XREADGROUP GROUP gC cC COUNT $N STREAMS $STR '>' | grep -cE '[0-9]{13}-[0-9]')
  redis-cli XGROUP CREATE $STR gLATE 0 >/dev/null 2>&1   # 다른 그룹이 다 읽은 뒤 합류
  D=\$(redis-cli XREADGROUP GROUP gLATE cL COUNT $N STREAMS $STR '>' | grep -cE '[0-9]{13}-[0-9]')
  echo \"A=\$A B=\$B C=\$C D=\$D\"
")
S_A=$(sed -n 's/.*A=\([0-9]*\).*/\1/p' <<<"$STREAM_OUT"); S_B=$(sed -n 's/.*B=\([0-9]*\).*/\1/p' <<<"$STREAM_OUT")
S_C=$(sed -n 's/.*C=\([0-9]*\).*/\1/p' <<<"$STREAM_OUT"); S_LATE=$(sed -n 's/.*D=\([0-9]*\).*/\1/p' <<<"$STREAM_OUT")

# ── 결과 ─────────────────────────────────────────────────────────────────────
log "════════ Fan-out 결과 (발행 N=$N, 독립 소비자 3 + 나중 합류 1) ════════"
printf '  %-16s %-8s %-8s %-8s %-12s %s\n' "브로커" "소비자A" "소비자B" "소비자C" "나중합류" "특징"
printf '  %-16s %-8s %-8s %-8s %-12s %s\n' "Kafka"         "$K_A" "$K_B" "$K_C" "$K_LATE" "group.id만 추가"
printf '  %-16s %-8s %-8s %-8s %-12s %s\n' "RabbitMQ"      "$Q_A" "$Q_B" "$Q_C" "$Q_LATE" "exchange+큐 사전선언"
printf '  %-16s %-8s %-8s %-8s %-12s %s\n' "Redis Pub/Sub" "$R_A" "$R_B" "$R_C" "$R_LATE" "동시연결만"
printf '  %-16s %-8s %-8s %-8s %-12s %s\n' "Redis Streams" "$S_A" "$S_B" "$S_C" "$S_LATE" "그룹+지속(Kafka급)"

log "판정"
[[ "$K_A" == "$N" && "$K_B" == "$N" && "$K_C" == "$N" && "$K_LATE" == "$N" ]] && ok "Kafka: 3그룹 전량 + 나중 합류 그룹도 전량 ($K_LATE) — 새 소비자 언제든 추가" || bad "Kafka 예상과 다름 (A/B/C/LATE=$K_A/$K_B/$K_C/$K_LATE, 기대 전부 $N)"
[[ "$Q_A" == "$N" && "$Q_B" == "$N" && "$Q_C" == "$N" && "$Q_LATE" == "0" ]] && ok "RabbitMQ: 사전 큐 3개엔 fan-out 되나, 발행 후 만든 큐는 과거 유실 (나중합류=$Q_LATE)" || bad "RabbitMQ 예상과 다름 (A/B/C/LATE=$Q_A/$Q_B/$Q_C/$Q_LATE, 기대 $N/$N/$N/0)"
[[ "$R_A" == "$N" && "$R_B" == "$N" && "$R_C" == "$N" && "$R_LATE" == "0" ]] && ok "Redis Pub/Sub: 동시 연결 3구독엔 live fan-out, 나중 연결은 유실 (나중합류=$R_LATE)" || bad "Redis Pub/Sub 예상과 다름 (A/B/C/LATE=$R_A/$R_B/$R_C/$R_LATE, 기대 $N/$N/$N/0)"
[[ "$S_A" == "$N" && "$S_B" == "$N" && "$S_C" == "$N" && "$S_LATE" == "$N" ]] && ok "Redis Streams: 그룹 3개 전량 + 나중 합류 그룹도 전량 ($S_LATE) — Pub/Sub 과 달리 Kafka 처럼 됨" || bad "Redis Streams 예상과 다름 (A/B/C/LATE=$S_A/$S_B/$S_C/$S_LATE, 기대 전부 $N)"
