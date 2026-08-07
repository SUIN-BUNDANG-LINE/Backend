#!/usr/bin/env bash
#
# 정합성 직렬화 5자 비교 러너
#   No-Lock(무보호 기준선) → SERIALIZABLE(DB 안) → 낙관락(앱+재시도) → synchronized(JVM 로컬) → Redisson(DB 앞)
#
# 같은 지속 부하(drawing-load.js, 기본 10 TPS × 30s)를 다섯 방식에 순서대로 걸고,
# MySQL 내부 낭비 지표(데드락·행 락 대기·롤백)와 앱 충돌·k6 성공률/p95 를 단계별로 집계한다.
#
# 다섯 방식은 전부 AbstractDrawingStrategy 의 구현이며(전략 패턴), 격리수준 전환도 전략 코드에
# 내장(@Transactional(isolation=SERIALIZABLE))되어 web 재기동 없이 같은 프로세스에서 연속 측정한다.
#
#   1단계: LOCK_MODE=off              (/draw-no-lock)          — 무보호 기준선
#   2단계: LOCK_MODE=serializable     (/draw-serializable)     — 트랜잭션 단위 SERIALIZABLE
#   3단계: LOCK_MODE=optimistic-retry (/draw-optimistic-retry) — 버전 검사+재시도5회, 시도·역전 집계
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

# ── 앱 통일 지표 스냅샷 (전략 무관 동일 조회) ──
# 다섯 전략이 같은 메트릭(mode 라벨만 다름)을 기록하므로, 단계마다 완전히 같은 질의를 던진다.
app_snapshot() { # $1=DrawMode 이름
    echo "$(prom "sum(drawing_outcome_total{mode=\"$1\",outcome=\"success\"})") \
$(prom "sum(drawing_outcome_total{mode=\"$1\"})") \
$(prom "sum(drawing_attempt_total{mode=\"$1\"})")"
}
app_delta() { # $1=before $2=after → "성공 N/M · 요청당 시도 X회"
    read -r ok1 all1 att1 <<< "$1"; read -r ok2 all2 att2 <<< "$2"
    local ok=$(echo "$ok2-$ok1" | bc) all=$(echo "$all2-$all1" | bc) att=$(echo "$att2-$att1" | bc)
    # 재시도 배수는 요청 대비로 잰다 — 성공 대비로 재면 성공률이 낮은 전략이 재시도를 안 해도 커진다
    local per="-"
    if [[ "$all" != "0" ]]; then per=$(echo "scale=2; $att/$all" | bc); fi
    echo "성공 $ok/$all · 시도 $att (요청당 ${per}회)"
}
# k6 LOCK_MODE → 앱 DrawMode 이름
draw_mode() {
    case "$1" in
        off) echo NO_LOCK ;; serializable) echo SERIALIZABLE ;;
        optimistic-retry) echo OPTIMISTIC_RETRY ;; synchronized) echo SYNCHRONIZED ;;
        *) echo REDISSON ;;
    esac
}

# ── web 재기동 (카운터 리셋용) + 헬스 대기 ──
# 격리수준 전환은 이제 불필요 — SERIALIZABLE 은 전략 코드(@Transactional(isolation=SERIALIZABLE))가
# 요청 트랜잭션 단위로 적용하므로, 실험 시작 시 1회만 재기동해 앱 카운터를 리셋한다.
restart_web() {
    ( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true \
        docker-compose up -d --force-recreate --no-deps web-1 web-2 ) >/dev/null 2>&1
    local booted=0
    for _ in $(seq 1 60); do
        curl -sf -m3 http://localhost:18080/management/health >/dev/null 2>&1 \
          && curl -sf -m3 http://localhost:18081/management/health >/dev/null 2>&1 && { booted=1; break; }
        sleep 3
    done
    [[ "$booted" == 1 ]] || { echo "❌ web 부팅 타임아웃 (docker logs sulmun2yong-web-1 --tail 40 확인)"; exit 1; }

    # 결제 우회 엔드포인트가 등록됐는지 확인한다. TEST_AUTH_ENABLED 가 전달되지 않으면 이 컨트롤러가
    # 빠지고, 설문이 PENDING_PAYMENT 에 머물러 모든 추첨이 거절된다 — 헬스체크는 통과하므로
    # 확인하지 않으면 전 단계가 0 으로 채워진 결과를 데이터처럼 출력하게 된다.
    local port code
    for port in 18080 18081; do
        code=$(curl -s -o /dev/null -w '%{http_code}' -m5 -X POST \
                 "http://localhost:$port/api/v1/test/surveys/00000000-0000-0000-0000-000000000000/activate")
        if [[ "$code" == 404 ]]; then
            echo "❌ :$port 에 결제 우회 엔드포인트가 없습니다 — TEST_AUTH_ENABLED 가 전달되지 않았습니다."
            echo "   확인: docker inspect sulmun2yong-web-1 --format '{{.Config.Env}}' | tr ' ' '\\n' | grep TEST_AUTH"
            exit 1
        fi
    done
}

# ── 한 단계 실행: 부하 → k6/DB 지표 집계 ──
run_phase() { # $1=라벨 $2=LOCK_MODE
    local label="$1" mode="$2"
    local before after
    # 인증: JWT 불필요 — k6 가 게이트웨이 헤더(X-Gateway-Auth + X-User-Id/Role)를 직접 넣는다 (lib/config.js)

    local dm; dm=$(draw_mode "$mode")
    before=$(snapshot); local abefore; abefore=$(app_snapshot "$dm")
    echo "  부하 주입: ${RATE} TPS × ${DURATION_S}s (LOCK_MODE=$mode → mode=$dm)"
    # k6 는 임계값(p95<5000ms) 실패 시 비제로 종료하므로 || true — 임계값 붕괴 자체가 측정 결과다
    ( cd "$K6_DIR" && LOCK_MODE="$mode" k6 run \
        --env RATE="$RATE" --env DURATION_S="$DURATION_S" \
        drawing-load.js ) > "$OUT_DIR/$label.txt" 2>&1 || true
    sleep 20   # Prometheus 스크레이프(15s) 반영 대기
    after=$(snapshot); local aafter; aafter=$(app_snapshot "$dm")

    # ── 이하 전 전략 동일 항목만 출력한다 (통제 실험) ──
    local p95 w50 w99
    p95=$(prom "histogram_quantile(0.95, sum by (le) (rate(drawing_duration_seconds_bucket{mode=\"$dm\"}[5m])))")
    # 요청이 앱 계측 지점에 하나도 닿지 않았으면 이 단계는 측정이 아니라 사고다 — 조용히 넘기지 않는다
    read -r _ all_before _ <<< "$abefore"; read -r _ all_after _ <<< "$aafter"
    if [[ "$(echo "$all_after-$all_before" | bc)" == "0" ]]; then
        echo "  ⚠️  이 단계의 추첨 요청이 0건입니다 — k6 로그($OUT_DIR/$label.txt)의 체크 실패를 확인하세요."
    fi
    echo "  앱:    $(app_delta "$abefore" "$aafter") · p95 ${p95}s"
    echo "  실패:  $(for o in deadlock version_conflict stale_row lock_timeout rejected other; do
                        v=$(prom "sum(drawing_outcome_total{mode=\"$dm\",outcome=\"$o\"})")
                        if [[ "$v" != "0" ]]; then printf "%s=%s " "$o" "$v"; fi
                    done)"
    echo "  MySQL: $(delta "$before" "$after")"
    # 정합성 — 같은 티켓이 두 명 이상에게 배정됐는가. 경쟁 제어가 실패해도 예외 없이 조용히
    # 커밋되는 유일한 증상이라, 성공률만 보면 놓친다. 0 이 아니면 그 단계는 데이터가 깨진 것이다.
    local sid dup
    sid=$(grep -aoE 'surveyId=[0-9a-f-]+' "$OUT_DIR/$label.txt" | head -1 | cut -d= -f2)
    if [[ -n "$sid" ]]; then
        dup=$(docker exec sulmun2yong-cluster-mysql mysql -uroot -ppassword -N -B test -e "
            SELECT IFNULL(SUM(n - 1), 0) FROM (
                SELECT COUNT(*) AS n FROM drawing_histories
                WHERE survey_id = UUID_TO_BIN('$sid')
                GROUP BY selected_ticket_index HAVING COUNT(*) > 1) d" 2>/dev/null || echo "?")
        echo "  정합성: 중복 배정 ${dup}건"
    fi
    # 공정성 — 진입 대기의 평균 대비 p99. 서버는 요청이 어떤 순서로 출발했는지 알 수 없으므로
    # "선착순을 지켰는가"는 잴 수 없다. 대신 "누구는 얼마나 더 오래 기다렸는가"의 배수로 본다.
    # 평균은 분위수가 아니라 sum/count 로 낸다 — 대기 0 인 전략은 최하 버킷 보간 때문에
    # 분위수가 0.0005s 처럼 나오지만 sum/count 는 정확히 0 이다.
    w50=$(prom "sum(rate(drawing_contention_wait_seconds_sum{mode=\"$dm\"}[5m])) / sum(rate(drawing_contention_wait_seconds_count{mode=\"$dm\"}[5m]))")
    w99=$(prom "histogram_quantile(0.99, sum by (le) (rate(drawing_contention_wait_seconds_bucket{mode=\"$dm\"}[5m])))")
    if awk -v a="$w50" 'BEGIN{exit !(a+0 > 0.0001)}'; then
        echo "  공정성: 진입 대기 avg ${w50}s · p99 ${w99}s$(
            awk -v a="$w50" -v b="$w99" 'BEGIN{ printf " (p99/avg %.1f배)", b/a }')"
    else
        echo "  공정성: 진입 제어 없음 — 대기 0"
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
echo "▶ [3/5] 낙관적 락 (보드 버전 검사 + 재시도 5회)"
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
