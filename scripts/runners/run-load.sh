#!/usr/bin/env bash
# 카프카 대용량 특성 실증 (Phase 1) — 컨슈머 측 직접 주입.
#
# 증명 목표:
#   ① 버스트 흡수    : N건을 한꺼번에 주입 → notification 그룹 랙이 치솟았다 0으로 drain (곡선 CSV)
#   ② 프로듀서 비차단: 주입 처리량이 컨슈머 적체와 무관하게 유지 (produce throughput)
#   ③ 수평 확장      : notification 컨슈머(발송+비용 통합) 1개 vs 3개 → drawing-completed 랙 drain 시간 단축
#   ④ 사가 수렴 지연 : 주입~전건 CONFIRMED 까지 소요 (SC-005)
#
# 사용: [N=8000] ./scripts/runners/run-load.sh   |  SKIP_BUILD=1 …  |  KEEP=1 …
set -uo pipefail
# 러너는 scripts/runners/ 에, load 하네스는 tests/e2e/load/ 에 있고, 컨슈머 jar 는 루트 gradle 로 빌드된다.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CF="${CF:-${REPO_ROOT}/tests/e2e/load/docker-compose.load.yml}"
COMPOSE="docker compose -f $CF"
K="${KTOOLS:-load-kafka}"          # Kafka CLI 를 실행할 컨테이너 (로컬 브로커 or tools 컨테이너)
BOOT="${BOOT:-localhost:9092}"     # 부트스트랩 (로컬 or 3노드 클러스터)
CMDCFG="${CMDCFG:-}"               # 원격 클러스터용 client 타임아웃 설정 파일(컨테이너 내부 경로)
DB=load-mysql
N=${N:-8000}
OUT="${REPO_ROOT}/tests/e2e/load"

log(){ printf '\n\033[1;36m[LOAD]\033[0m %s\n' "$*"; }
teardown(){ [[ "${KEEP:-0}" == 1 ]] && log "KEEP=1 유지 — 정리: $COMPOSE --profile scale down -v" || { log teardown; $COMPOSE --profile scale down -v >/dev/null 2>&1; }; }
trap teardown EXIT

sql(){ docker exec "$DB" mysql -uroot -prootpw -N -B test -e "$1" 2>/dev/null; }
lag_drawing(){ docker exec "$K" /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server "$BOOT" ${CMDCFG:+--command-config $CMDCFG} --describe --group drawing-notification 2>/dev/null \
    | awk '$2=="drawing-completed" && $6 ~ /^[0-9]+$/ {s+=$6} END{print s+0}'; }
confirmed(){ sql "SELECT COUNT(*) FROM cost_record WHERE original_drawing_event_id LIKE '$1-%' AND state='CONFIRMED'"; }

gen_burst(){ # tag file
    awk -v n="$N" -v t="$1" 'BEGIN{for(i=1;i<=n;i++)printf "{\"eventId\":\"%s-%d\",\"surveyId\":\"sv\",\"participantId\":\"p%d\",\"selectedNumber\":7,\"isWinner\":true,\"rewardName\":\"C\",\"rewardCategory\":\"D\",\"remainingTickets\":0,\"timestamp\":\"2026-07-14T00:00:00Z\"}\n",t,i,i}' > "$2"; }

# 한 번의 버스트 실험. 인자: runtag, 라벨(리포트용). 전역에 결과 기록.
run_burst(){
    local tag=$1 label=$2
    local f="$OUT/burst-$tag.jsonl" csv="$OUT/lag-$tag.csv"
    gen_burst "$tag" "$f"
    log "[$label] 버스트 주입 시작 (N=$N, tag=$tag)"
    local t0=$SECONDS
    docker exec -i "$K" /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server "$BOOT" ${CMDCFG:+--producer.config $CMDCFG} --topic drawing-completed < "$f" >/dev/null 2>&1
    local tprod=$((SECONDS-t0)); (( tprod<1 )) && tprod=1
    local thr=$(( N / tprod ))
    echo "elapsed_s,drawing_lag" > "$csv"
    local peak=0 drain="" finish="" el l c
    while (( SECONDS-t0 < 300 )); do
        el=$((SECONDS-t0)); l=$(lag_drawing); c=$(confirmed "$tag")
        echo "$el,$l" >> "$csv"
        (( l > peak )) && peak=$l
        [[ -z "$drain" && $el -ge $tprod && $l -eq 0 ]] && drain=$el
        [[ "$c" == "$N" ]] && { finish=$((SECONDS-t0)); break; }
        sleep 1
    done
    [[ -z "$drain" ]] && drain=">300"
    [[ -z "$finish" ]] && finish=">300"
    printf '  produce=%ss  throughput≈%s msg/s  peak_lag=%s  drawing_drain=%ss  all_CONFIRMED=%ss\n' \
        "$tprod" "$thr" "$peak" "$drain" "$finish"
    RES_THR=$thr; RES_PEAK=$peak; RES_DRAIN=$drain; RES_DONE=$finish
}

# ── 0) 빌드 & 인프라 + baseline(notification 1개) ──
[[ "${SKIP_BUILD:-0}" != 1 ]] && { log "bootJar 빌드 (컨슈머 2종)"; (cd "${REPO_ROOT}" && ./gradlew :drawing-sms-notification-consumer:bootJar :dlt-sms-notification-consumer:bootJar -q) || exit 1; }
log "인프라 + 컨슈머 기동 (notification 1개)"
$COMPOSE up -d --force-recreate
# 원격 클러스터 모드면 CLI client 타임아웃 설정 파일을 tools 컨테이너에 심는다.
[[ -n "$CMDCFG" ]] && docker exec "$K" sh -c "printf 'request.timeout.ms=15000\ndefault.api.timeout.ms=20000\n' > $CMDCFG"
log "cost_record 테이블 + 구독 안정화 대기"
for i in $(seq 1 60); do [[ "$(sql "SHOW TABLES LIKE 'cost_record'")" == cost_record ]] && break; sleep 3; (( i==60 )) && { $COMPOSE logs --tail=40 notification-1; exit 1; }; done
sleep 20

# ── 실험 A: notification 컨슈머 1개 ──
run_burst a1 "notif×1"
A_PEAK=$RES_PEAK; A_DRAIN=$RES_DRAIN; A_DONE=$RES_DONE; A_THR=$RES_THR

# ── 스케일 업: notification 3개 ──
log "notification 컨슈머 3개로 스케일 업 (+notification-2, notification-3) — 리밸런스 대기"
$COMPOSE --profile scale up -d
sleep 25

# ── 실험 B: notification 컨슈머 3개 ──
run_burst b3 "notif×3"
B_PEAK=$RES_PEAK; B_DRAIN=$RES_DRAIN; B_DONE=$RES_DONE; B_THR=$RES_THR

# ── 리포트 ──
log "════════ 결과 요약 (N=$N per burst) ════════"
printf '  %-10s %-14s %-12s %-16s %-16s\n' "구성" "주입처리량" "peak랙" "drawing_drain" "전건CONFIRMED"
printf '  %-10s %-14s %-12s %-16s %-16s\n' "notif×1" "${A_THR}/s" "$A_PEAK" "${A_DRAIN}s" "${A_DONE}s"
printf '  %-10s %-14s %-12s %-16s %-16s\n' "notif×3" "${B_THR}/s" "$B_PEAK" "${B_DRAIN}s" "${B_DONE}s"
log "랙 곡선 CSV: $OUT/lag-a1.csv , $OUT/lag-b3.csv"
[[ "$A_DRAIN" != ">300" && "$B_DRAIN" != ">300" && "$B_DRAIN" -lt "$A_DRAIN" ]] \
    && log "③ 확인: notification 3개가 1개보다 drawing 랙을 더 빨리 소진 ($A_DRAIN s → $B_DRAIN s)" \
    || log "③ 참고: drain 비교는 CSV/수치로 판단 (DB 병목이 상한일 수 있음)"
