#!/usr/bin/env bash
#
# 정합성 직렬화 5자 비교 러너
#   No-Lock(무보호 기준선) → SERIALIZABLE(DB 안) → 낙관락(앱+재시도) → synchronized(JVM 로컬) → Redisson(DB 앞)
#
# 같은 지속 부하(drawing-load.js, 기본 10 TPS × 30s)를 다섯 방식에 순서대로 걸고,
# MySQL 내부 낭비 지표(데드락·행 락 대기·롤백)와 앱 충돌·k6 성공률/p95 를 단계별로 집계한다.
#
# 다섯 방식은 전부 DrawingStrategy 인터페이스의 구현이며(전략 패턴), 격리수준 전환도 전략 코드에
# 내장(@Transactional(isolation=SERIALIZABLE))되어 web 재기동 없이 같은 프로세스에서 연속 측정한다.
#
#   1단계: LOCK_MODE=off              (/draw-no-lock)          — 무보호 기준선
#   2단계: LOCK_MODE=serializable     (/draw-serializable)     — 트랜잭션 단위 SERIALIZABLE
#   3단계: LOCK_MODE=optimistic-retry (/draw-optimistic-retry) — @Version+재시도5회, 시도·역전 집계
#   4단계: LOCK_MODE=synchronized     (/draw-synchronized)     — JVM 로컬, cross-JVM 한계 검증
#   5단계: LOCK_MODE=on               (/draw)                  — Redisson (운영)
#
# 사용법:
#   ./scripts/runners/run-lock-experiment.sh                 # 10 TPS × 30s
#   RATE=20 DURATION_S=60 ./scripts/runners/run-lock-experiment.sh
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:18080}"
PROM_URL="${PROM_URL:-http://localhost:19090}"
RATE="${RATE:-10}"
DURATION_S="${DURATION_S:-30}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_DIR="$REPO_ROOT/scripts/scenarios/concurrency"
OUT_DIR="$REPO_ROOT/scripts/results/lock-experiment"
mkdir -p "$OUT_DIR"

for cmd in k6 curl jq docker; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "❌ '$cmd' 가 필요합니다."; exit 1; }
done

# ── MySQL 내부 카운터 스냅샷 (mysqld-exporter → Prometheus) ──
prom() { curl -s --data-urlencode "query=$1" "$PROM_URL/api/v1/query" | jq -r '.data.result[0].value[1] // "0"'; }
snapshot() {
    echo "$(prom 'mysql_info_schema_innodb_metrics_lock_lock_deadlocks_total') \
$(prom 'mysql_global_status_innodb_row_lock_waits') \
$(prom 'mysql_global_status_innodb_row_lock_time') \
$(prom 'mysql_global_status_commands_total{command="rollback"}')"
}
delta() { # $1=before $2=after → "데드락 Δ / 행락대기 Δ / 행락시간 Δms / 롤백 Δ"
    read -r d1 w1 t1 r1 <<< "$1"; read -r d2 w2 t2 r2 <<< "$2"
    echo "데드락 +$(echo "$d2-$d1" | bc) · 행락대기 +$(echo "$w2-$w1" | bc) · 행락대기시간 +$(echo "$t2-$t1" | bc)ms · 롤백 +$(echo "$r2-$r1" | bc)"
}

# ── web 재기동 (카운터 리셋용) + 헬스 대기 ──
# 격리수준 전환은 이제 불필요 — SERIALIZABLE 은 전략 코드(@Transactional(isolation=SERIALIZABLE))가
# 요청 트랜잭션 단위로 적용하므로, 실험 시작 시 1회만 재기동해 앱 카운터를 리셋한다.
restart_web() {
    ( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true \
        docker-compose up -d --force-recreate --no-deps web-1 web-2 ) >/dev/null 2>&1
    for _ in $(seq 1 60); do
        curl -sf -m3 http://localhost:18080/management/health >/dev/null 2>&1 \
          && curl -sf -m3 http://localhost:18081/management/health >/dev/null 2>&1 && return 0
        sleep 3
    done
    echo "❌ web 부팅 타임아웃 (docker logs sulmun2yong-web-1 --tail 40 확인)"; exit 1
}

# ── 한 단계 실행: 부하 → k6/DB 지표 집계 ──
run_phase() { # $1=라벨 $2=LOCK_MODE
    local label="$1" mode="$2"
    local before after
    # 인증: JWT 불필요 — k6 가 게이트웨이 헤더(X-Gateway-Auth + X-User-Id/Role)를 직접 넣는다 (lib/config.js)

    before=$(snapshot)
    echo "  부하 주입: ${RATE} TPS × ${DURATION_S}s (LOCK_MODE=$mode)"
    # k6 는 임계값(p95<5000ms) 실패 시 비제로 종료하므로 || true — 임계값 붕괴 자체가 측정 결과다
    ( cd "$K6_DIR" && LOCK_MODE="$mode" k6 run \
        --env RATE="$RATE" --env DURATION_S="$DURATION_S" \
        drawing-load.js ) > "$OUT_DIR/$label.txt" 2>&1 || true
    sleep 20   # Prometheus 스크레이프(15s) 반영 대기
    after=$(snapshot)

    local s f p95
    s=$(grep -a drawing_success_total "$OUT_DIR/$label.txt" | grep -oE ': [0-9]+' | head -1 | tr -d ': ' || true)
    f=$(grep -a drawing_fail_total "$OUT_DIR/$label.txt" | grep -oE ': [0-9]+' | head -1 | tr -d ': ' || true)
    p95=$(grep -a http_req_duration "$OUT_DIR/$label.txt" | grep -oE 'p\(95\)=[0-9.]+m?s' | head -1 || true)
    echo "  k6: 성공=${s:-0} 실패=${f:-0} $p95"
    echo "  MySQL: $(delta "$before" "$after")"
    echo "  앱 카운터(누적): 낙관락충돌=$(prom 'sum(optimistic_lock_failure_total)') · 데드락예외=$(prom 'sum(db_deadlock_total)')"

    # 낙관락 계열이면 심화 지표: 성공당 시도 횟수 + 선착순 역전 쌍
    if [[ "$mode" == optimistic* ]]; then
        local att_ok att_vc att_dl
        att_ok=$(prom 'sum(drawing_attempts_total{result="success"})')
        att_vc=$(prom 'sum(drawing_attempts_total{result="version_conflict"})')
        att_dl=$(prom 'sum(drawing_attempts_total{result="deadlock"})')
        if [[ "$att_ok" != "0" ]]; then
            echo "  시도(누적): 성공 $att_ok · 버전충돌 $att_vc · 데드락 $att_dl → 성공당 시도 $(echo "scale=2; ($att_ok+$att_vc+$att_dl)/$att_ok" | bc)회"
        fi
        # 선착순 역전 — 발사 순번(selected_ticket_index)과 확정 시각(created_at)의 역전 쌍 수
        local sid
        sid=$(grep -aoE 'surveyId=[0-9a-f-]+' "$OUT_DIR/$label.txt" | head -1 | cut -d= -f2)
        if [[ -n "$sid" ]]; then
            local inv
            inv=$(docker exec sulmun2yong-cluster-mysql mysql -uroot -ppassword -N -B test -e "
                SELECT COUNT(*) FROM drawing_histories a
                JOIN drawing_histories b
                  ON a.survey_id = b.survey_id
                 AND a.selected_ticket_index < b.selected_ticket_index
                 AND a.created_at > b.created_at
                WHERE a.survey_id = UUID_TO_BIN('$sid')" 2>/dev/null || echo "?")
            echo "  선착순 역전: ${inv}쌍 (먼저 발사됐는데 늦게 확정된 조합)"
        fi
    fi

    # synchronized 이면: JVM 로컬 락 활동 (활동 있음 + 데드락 잔존 = cross-JVM 경합 통과 증거)
    if [[ "$mode" == "synchronized" ]]; then
        echo "  JVM 락 진입: count=$(prom 'sum(jvm_lock_wait_seconds_count)') · 최대 대기 $(prom 'max(jvm_lock_wait_seconds_max)')s — 로컬 직렬화 작동 증거"
    fi
}

echo "════════════════════════════════════════════════"
echo " 직렬화 5자 비교 — No-Lock → SERIALIZABLE → 낙관락 → synchronized → Redisson"
echo " 부하: ${RATE} TPS × ${DURATION_S}s · 결과: $OUT_DIR"
echo "════════════════════════════════════════════════"

# ── 전체 스택 선기동 (idempotent) — 내려가 있으면 MySQL·Redis·Prometheus 까지 올린다 ──
echo ""
echo "▶ 스택 확인/기동: docker-compose up -d"
( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true docker-compose up -d ) >/dev/null 2>&1 \
    || { echo "❌ docker-compose up 실패"; exit 1; }

echo ""
echo "▶ web 재기동 (앱 카운터 리셋 — 이후 5단계는 같은 web 에서 연속 실행)"
restart_web

echo ""
echo "▶ [1/5] No-Lock (무보호 기준선)"
run_phase "no-lock" "off"

echo ""
echo "▶ [2/5] SERIALIZABLE (DB 안 직렬화 — 전략이 트랜잭션 단위로 격리수준 적용)"
run_phase "serializable" "serializable"

echo ""
echo "▶ [3/5] 낙관적 락 (@Version + 재시도 5회)"
run_phase "optimistic" "optimistic-retry"

echo ""
echo "▶ [4/5] synchronized (JVM 로컬 직렬화 — cross-JVM 한계 검증)"
run_phase "synchronized" "synchronized"

echo ""
echo "▶ [5/5] Redisson 분산락 (DB 앞 직렬화)"
run_phase "redisson" "on"

echo ""
echo "✅ 완료. 원시 로그: $OUT_DIR/{no-lock,serializable,optimistic,synchronized,redisson}.txt"
echo "   Grafana( http://localhost:13000/d/race-comparison )에서 두 구간을 시간축으로 대조하세요."
