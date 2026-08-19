#!/usr/bin/env bash
#
# 경쟁 제어 4자 비교 러너
#
# 같은 부하를 네 방식에 순서대로 걸고, MySQL 내부 낭비(행 락 대기)와
# 앱 지표(성공률·지연·진입 대기)를 단계별로 집계한다. 여섯은 전부
# AbstractDrawingStrategy 의 구현이라 web 재기동 없이 같은 프로세스에서 연속 측정된다.
#
# 핵심 축은 **판정 단위를 어디에 두는가** 다 — 실제 경합 단위(칸 하나)보다 크게 잡을수록 나빠진다.
#
#   1단계: default           (/draw-default)           칸 하나  — 기본 판정, 조건부 UPDATE (is_selected 가 버전)
#   2단계: serializable      (/draw-serializable)      읽기 범위 — 공유 락 승격 충돌
#   3단계: synchronized      (/draw-synchronized)      설문 하나 — JVM 로컬(인스턴스 간 무방비)
#   4단계: on                (/draw)                   칸 하나  — Redisson (운영)
#
# 사용법:
#   ./scripts/lock-experiment/run.sh    # 10 TPS × 30s, 1초마다 10건 동시 · 버스트마다 자기 칸
#   RATE=30 DURATION_S=60 ./scripts/lock-experiment/run.sh
#   TICKETS=0 ./...  버스트 안 요청이 각자 다른 칸 (논리적 충돌 0 — 경합 단위를 크게 잡은 전략만 손해)
#   BURST=0   ./...  균등 간격 (요청이 겹치지 않아 경합 없음)
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:18080}"
PROM_URL="${PROM_URL:-http://localhost:19090}"
RATE="${RATE:-10}"
DURATION_S="${DURATION_S:-30}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_DIR="$REPO_ROOT/scripts/scenarios/concurrency"
OUT_DIR="$(dirname "${BASH_SOURCE[0]}")/results"
mkdir -p "$OUT_DIR"

for cmd in k6 curl jq docker; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "❌ '$cmd' 가 필요합니다."; exit 1; }
done

# ── MySQL 내부 카운터 스냅샷 (mysqld-exporter → Prometheus) ──
prom() { curl -s --data-urlencode "query=$1" "$PROM_URL/api/v1/query" | jq -r '.data.result[0].value[1] // "0"'; }
snapshot() {
    echo "$(prom 'mysql_global_status_innodb_row_lock_waits') \
$(prom 'mysql_global_status_innodb_row_lock_time')"
}
delta() { # $1=before $2=after → "행락대기 Δ / 행락시간 Δms"
    read -r w1 t1 <<< "$1"; read -r w2 t2 <<< "$2"
    echo "행락대기 +$(echo "$w2-$w1" | bc) · 행락대기시간 +$(echo "$t2-$t1" | bc)ms"
}
# mode 별 DB 증분을 Prometheus 에 되밀어 넣는다(remote write). MySQL 카운터에는 mode 라벨이
# 없으므로, 러너만 아는 "이 단계 = 이 mode" 사실을 라벨로 붙여 대시보드가 mode 별로 읽게 한다.
push_mode_db() { # $1=mode $2=before $3=after
    read -r w1 t1 <<< "$2"; read -r w2 t2 <<< "$3"
    printf 'lock_experiment_row_lock_waits{mode="%s"} %s\nlock_experiment_row_lock_time_ms{mode="%s"} %s\n' \
        "$1" "$(echo "$w2-$w1" | bc)" "$1" "$(echo "$t2-$t1" | bc)" \
      | docker exec -i sulmun2yong-cluster-prometheus promtool push metrics http://localhost:9090/api/v1/write >/dev/null 2>&1 \
      || echo "  ⚠️  mode별 DB 증분 push 실패 (promtool/remote-write 확인)"
}

# ── 앱 통일 지표 스냅샷 (전략 무관 동일 조회) ──
# 다섯 전략이 같은 메트릭(mode 라벨만 다름)을 기록하므로, 단계마다 완전히 같은 질의를 던진다.
app_snapshot() { # $1=DrawMode 이름
    echo "$(prom "sum(drawing_outcome_total{mode=\"$1\",outcome=\"success\"})") \
$(prom "sum(drawing_outcome_total{mode=\"$1\"})")"
}
app_delta() { # $1=before $2=after → "성공 N/M"
    read -r ok1 all1 <<< "$1"; read -r ok2 all2 <<< "$2"
    echo "성공 $(echo "$ok2-$ok1" | bc)/$(echo "$all2-$all1" | bc)"
}
# k6 LOCK_MODE → 앱 DrawMode 이름
draw_mode() {
    case "$1" in
        default) echo DEFAULT ;; serializable) echo SERIALIZABLE ;;
        synchronized) echo SYNCHRONIZED ;;
        *) echo REDISSON ;;
    esac
}

# ── web 재기동 (카운터 리셋용) + 헬스 대기 ──
# 격리수준 전환은 이제 불필요 — SERIALIZABLE 은 전략 코드(@Transactional(isolation=SERIALIZABLE))가
# 요청 트랜잭션 단위로 적용하므로, 실험 시작 시 1회만 재기동해 앱 카운터를 리셋한다.
restart_web() {
    ( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true LOCK_EXPERIMENT_ENABLED=true \
        docker-compose up -d --force-recreate --no-deps web-1 web-2 ) >/dev/null 2>&1
    local booted=0
    for _ in $(seq 1 60); do
        curl -sf -m3 http://localhost:18080/management/health >/dev/null 2>&1 \
          && curl -sf -m3 http://localhost:18081/management/health >/dev/null 2>&1 && { booted=1; break; }
        sleep 3
    done
    [[ "$booted" == 1 ]] || { echo "❌ web 부팅 타임아웃 (docker logs sulmun2yong-web-1 --tail 40 확인)"; exit 1; }

    # 결제 우회 엔드포인트가 등록됐는지 확인한다. TEST_AUTH_ENABLED 가 전달되지 않으면 이 컨트롤러가
    # 빠지고, 설문이 결제 대기(미활성)에 머물러 모든 추첨이 거절된다 — 헬스체크는 통과하므로
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
        # 실험 경로 3개는 lock-experiment.enabled 로 닫혀 있다. 닫힌 채 돌리면 세 단계가
        # 404 로 전멸하고 Redisson 단계만 값이 남아, 비교표가 아니라 사고가 된다.
        code=$(curl -s -o /dev/null -w '%{http_code}' -m5 -X POST \
                 -H 'Content-Type: application/json' -d '{}' \
                 "http://localhost:$port/api/v1/drawing-board/draw-default")
        if [[ "$code" == 404 ]]; then
            echo "❌ :$port 에 실험 엔드포인트가 없습니다 — LOCK_EXPERIMENT_ENABLED 가 전달되지 않았습니다."
            echo "   확인: docker inspect sulmun2yong-web-1 --format '{{.Config.Env}}' | tr ' ' '\\n' | grep LOCK_EXPERIMENT"
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
        --env RATE="$RATE" --env DURATION_S="$DURATION_S" --env TICKETS="${TICKETS:-}" --env BURST="${BURST:-}" \
        drawing-load.js ) > "$OUT_DIR/$label.txt" 2>&1 || true
    sleep 20   # Prometheus 스크레이프(15s) 반영 대기
    after=$(snapshot); local aafter; aafter=$(app_snapshot "$dm")

    # ── 이하 전 전략 동일 항목만 출력한다 (통제 실험) ──
    local p95 w50 w99
    p95=$(prom "histogram_quantile(0.95, sum by (le) (rate(drawing_duration_seconds_bucket{mode=\"$dm\"}[5m])))")
    # 요청이 앱 계측 지점에 하나도 닿지 않았으면 이 단계는 측정이 아니라 사고다 — 조용히 넘기지 않는다
    read -r _ all_before <<< "$abefore"; read -r _ all_after <<< "$aafter"
    if [[ "$(echo "$all_after-$all_before" | bc)" == "0" ]]; then
        echo "  ⚠️  이 단계의 추첨 요청이 0건입니다 — k6 로그($OUT_DIR/$label.txt)의 체크 실패를 확인하세요."
    fi
    echo "  앱:    $(app_delta "$abefore" "$aafter") · p95 ${p95}s"
    echo "  실패:  $(for o in duplicate_ticket deadlock lock_timeout rejected other; do
                        v=$(prom "sum(drawing_outcome_total{mode=\"$dm\",outcome=\"$o\"})")
                        if [[ "$v" != "0" ]]; then printf "%s=%s " "$o" "$v"; fi
                    done)"
    echo "  MySQL: $(delta "$before" "$after")"
    push_mode_db "$dm" "$before" "$after"
    # 정합성 — 경쟁 제어가 뚫려 같은 티켓이 두 명에게 가려다 UNIQUE 제약에 막힌 횟수.
    # 이력 테이블을 직접 세던 집계는 그라파나(MySQL 데이터소스)로 옮겼다. 여기서는 앱이
    # 기록한 시계열을 읽어, 다른 실패 사유와 같은 자로 잰다.
    echo "  정합성: 중복 배정 시도 $(prom "sum(drawing_outcome_total{mode=\"$dm\",outcome=\"duplicate_ticket\"})")건"
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
echo " 직렬화 4자 비교 — 기본 판정 → SERIALIZABLE → synchronized → Redisson"
echo " 부하: ${RATE} TPS × ${DURATION_S}s · 버스트 ${BURST:-10}건 동시 · 경합 폭 ${TICKETS:-버스트마다 자기 칸}"
echo " 결과: $OUT_DIR"
echo "════════════════════════════════════════════════"

# ── 전체 스택 선기동 (idempotent) — 내려가 있으면 MySQL·Redis·Prometheus 까지 올린다 ──
echo ""
echo "▶ 스택 확인/기동: docker-compose up -d"
( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true LOCK_EXPERIMENT_ENABLED=true docker-compose up -d ) >/dev/null 2>&1 \
    || { echo "❌ docker-compose up 실패"; exit 1; }

echo ""
echo "▶ web 재기동 (앱 카운터 리셋 — 이후 4단계는 같은 web 에서 연속 실행)"
restart_web

echo ""
echo "▶ [1/4] 기본 판정 (조건부 UPDATE)"
run_phase "default" "default"

echo ""
echo "▶ [2/4] SERIALIZABLE (DB 안 직렬화 — 전략이 트랜잭션 단위로 격리수준 적용)"
run_phase "serializable" "serializable"


echo ""
echo "▶ [3/4] synchronized (메서드 수준 — this 잠금이라 판정 단위가 서버 전체, cross-JVM 무방비)"
run_phase "synchronized" "synchronized"

echo ""
echo "▶ [4/4] Redisson 분산락 (DB 앞 직렬화)"
run_phase "redisson" "on"

echo ""
echo "✅ 완료. 원시 로그: $OUT_DIR/{default,serializable,synchronized,redisson}.txt"
echo "   Grafana( http://localhost:13000/d/race-comparison )에서 두 구간을 시간축으로 대조하세요."
