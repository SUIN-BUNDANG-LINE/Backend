#!/usr/bin/env bash
# SMS 발송-비용 정합 사가 — 자급형 멀티앱 E2E 자동 검증.
#
# 3개 컨슈머 + Kafka/MySQL/Redis 를 docker-compose 로 띄우고, drawing-completed 등 사가 이벤트를
# 실제 Kafka 로 흘려 cost_record 최종 상태를 MySQL 에서 폴링·단언한다. backend 무의존, 운영 인프라 무의존.
#
# 사용:
#   ./scripts/runners/run-e2e.sh            # 전체 실행 후 자동 teardown
#   KEEP=1 ./scripts/runners/run-e2e.sh     # 종료 후 스택 유지(로그/DB 확인용)
#   SKIP_BUILD=1 ./scripts/runners/run-e2e.sh   # jar 재빌드 생략
set -uo pipefail

# 러너는 scripts/runners/ 에, compose 하네스는 tests/e2e/ 에 있고, 컨슈머 jar 는 루트 gradle 로 빌드된다.
# compose 파일은 절대경로로 지정 → 프로젝트 디렉토리가 tests/e2e/ 로 고정되어 jar 상대 마운트가 정합.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="docker compose -f ${REPO_ROOT}/tests/e2e/docker-compose.e2e.yml"
KAFKA=e2e-kafka
MYSQL=e2e-mysql

PASS=0; FAIL=0
log()  { printf '\n\033[1;36m[E2E]\033[0m %s\n' "$*"; }
ok()   { printf '  \033[1;32m✔ PASS\033[0m %s\n' "$*"; PASS=$((PASS+1)); }
bad()  { printf '  \033[1;31mx FAIL\033[0m %s\n' "$*"; FAIL=$((FAIL+1)); }

teardown() {
    if [[ "${KEEP:-0}" == "1" ]]; then
        log "KEEP=1 — 스택 유지. 정리: $COMPOSE down -v"
    else
        log "teardown"; $COMPOSE down -v >/dev/null 2>&1
    fi
}
trap teardown EXIT

# ── Kafka 발행 / MySQL 조회 헬퍼 ─────────────────────────────────────────────
produce() { # topic json
    echo "$2" | docker exec -i "$KAFKA" /opt/kafka/bin/kafka-console-producer.sh \
        --bootstrap-server localhost:9092 --topic "$1" >/dev/null 2>&1
}
sql() { docker exec "$MYSQL" mysql -uroot -prootpw -N -B test -e "$1" 2>/dev/null; }
state_of()  { sql "SELECT state FROM cost_record WHERE original_drawing_event_id='$1'"; }
count_of()  { sql "SELECT COUNT(*) FROM cost_record WHERE original_drawing_event_id='$1'"; }

await_state() { # key expected timeout_s  → 0 성공 / 1 타임아웃
    local key=$1 want=$2 timeout=$3 waited=0
    while (( waited < timeout )); do
        [[ "$(state_of "$key")" == "$want" ]] && return 0
        sleep 3; waited=$((waited+3))
    done
    return 1
}

drawing() { # eventId isWinner  → drawing-completed JSON
    printf '{"eventId":"%s","surveyId":"sv-%s","participantId":"p-%s","selectedNumber":7,"isWinner":%s,"rewardName":"Coffee","rewardCategory":"DRINK","remainingTickets":0,"timestamp":"2026-07-14T00:00:00Z"}' \
        "$1" "$1" "$1" "$2"
}

# ── 0) 빌드 & 기동 ───────────────────────────────────────────────────────────
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
    log "bootJar 빌드 (컨슈머 2종)"; (cd "${REPO_ROOT}" && ./gradlew :drawing-sms-notification-consumer:bootJar :dlt-sms-notification-consumer:bootJar -q) || { echo "빌드 실패"; exit 1; }
fi

log "인프라 + 컨슈머 기동 (notification: 실패율 0.0)"
SMS_MOCK_FAILURE_RATE=0.0 $COMPOSE up -d --force-recreate

log "MySQL + Flyway(cost_record 테이블) 준비 대기"
for i in $(seq 1 60); do
    [[ "$(sql "SHOW TABLES LIKE 'cost_record'")" == "cost_record" ]] && break
    sleep 3
    (( i == 60 )) && { echo "cost_record 테이블이 안 생김 — 컨슈머 부팅 로그 확인 필요"; $COMPOSE logs --tail=40 drawing-sms-notification-consumer; exit 1; }
done
log "컨슈머 그룹 워밍업 (구독 안정화 20s)"; sleep 20

# ── 시나리오 (모두 auto-offset-reset=earliest 라 발행/구독 순서 무관) ──────────

# S1 — Story1 #2: 당첨 + 발송 성공 → CONFIRMED
log "S1 당첨+발송성공 → CONFIRMED (FR-002)"
produce drawing-completed "$(drawing s1 true)"
if await_state s1 CONFIRMED 90; then ok "s1 = CONFIRMED"; else bad "s1 = '$(state_of s1)' (기대 CONFIRMED)"; fi

# S3 — FR-004 / edge: 비당첨 → 비용 레코드 없음
log "S3 비당첨(isWinner=false) → 레코드 없음 (FR-004)"
produce drawing-completed "$(drawing s3 false)"
sleep 15
if [[ "$(count_of s3)" == "0" ]]; then ok "s3 rows = 0"; else bad "s3 rows = $(count_of s3) (기대 0)"; fi

# S4 — FR-005 / Story2 #2: 중복 당첨 → 정확히 1행, CONFIRMED
log "S4 중복 당첨 이벤트 → 1행 멱등 (FR-005)"
produce drawing-completed "$(drawing s4 true)"
produce drawing-completed "$(drawing s4 true)"
if await_state s4 CONFIRMED 90; then
    [[ "$(count_of s4)" == "1" ]] && ok "s4 = CONFIRMED, rows=1" || bad "s4 rows=$(count_of s4) (기대 1)"
else bad "s4 = '$(state_of s4)' (기대 CONFIRMED)"; fi

# S5 — FR-010: 순서 역전 (permanently-failed 가 drawing 보다 먼저 도착) → REVERSED 수렴
# 발송 확정(CONFIRMED)은 이제 발송 성공과 같은 로컬 트랜잭션이라 역전이 불가능하고,
# 유일하게 서비스 경계를 넘는 취소 신호(permanently-failed)의 역전만 남는다.
# REVERSED 는 종결 상태이므로, 뒤늦은 drawing-completed(PENDING)와 발송 성공 확정이 모두 NoOp 로 흡수돼야 한다.
log "S5 순서 역전(permanently-failed→drawing) → REVERSED 수렴 (FR-010)"
produce sms-delivery-permanently-failed '{"eventId":"sig-s5","originalDrawingEventId":"s5","participantId":"p-s5","surveyId":"sv-s5"}'
sleep 5
produce drawing-completed "$(drawing s5 true)"
sleep 15
if [[ "$(state_of s5)" == "REVERSED" ]]; then
    [[ "$(count_of s5)" == "1" ]] && ok "s5 = REVERSED, rows=1" || bad "s5 rows=$(count_of s5) (기대 1)"
else bad "s5 = '$(state_of s5)' (기대 REVERSED)"; fi

# S2 — Story1 #1: 당첨 + 발송 영구 실패(DLT) → REVERSED (보상)
# notification 을 실패율 1.0 으로 재기동. MAX_RETRY=5 소진(즉시 1회 + 워커 30s 간격 재시도)이라 최대 ~3분 소요.
log "S2 발송 영구실패 준비 — notification 을 실패율 1.0 으로 재기동"
SMS_MOCK_FAILURE_RATE=1.0 $COMPOSE up -d --force-recreate --no-deps drawing-sms-notification-consumer
sleep 15
log "S2 당첨+발송영구실패 → DLT → REVERSED (FR-003/007, 최대 300s)"
produce drawing-completed "$(drawing s2 true)"
if await_state s2 REVERSED 300; then ok "s2 = REVERSED"; else bad "s2 = '$(state_of s2)' (기대 REVERSED)"; fi

# ── 결과 ─────────────────────────────────────────────────────────────────────
log "cost_record 최종 상태"; sql "SELECT original_drawing_event_id, state, amount FROM cost_record ORDER BY original_drawing_event_id"
log "결과: PASS=$PASS FAIL=$FAIL"
(( FAIL == 0 )) && { log "✅ E2E 전체 통과"; exit 0; } || { log "❌ 실패 있음"; exit 1; }
