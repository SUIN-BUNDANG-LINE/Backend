#!/usr/bin/env bash
#
# 분산락 효과 측정 실험 러너 (Lock OFF vs ON)
#
# docs/kafka-distribute-lock/시나리오.md 의 실측 절차를 하나로 묶은 스크립트.
# drawing-concurrency.js 를 LOCK_MODE=off 3회 → on 3회 실행하고 결과를 파일로 저장한다.
#
# 타깃은 docker-compose 의 web-1(18080)/web-2(18081) — Prometheus 가 스크레이프하는 인스턴스라
# 이걸 때려야 Grafana(race-comparison/drawing-lock) 대시보드에 지표가 잡힌다.
# 스택은 TEST_AUTH_ENABLED=true 로 기동해 /api/v1/test/token 을 활성화한다.
#
# 사용법:
#   ./scripts/runners/run-lock-experiment.sh              # 스택 up → OFF 3회 + ON 3회
#   RUNS=5 ./scripts/runners/run-lock-experiment.sh       # 회차 조정
#   NO_UP=1 ./scripts/runners/run-lock-experiment.sh      # 스택 기동 생략(이미 떠 있음)
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:18080}"                       # 토큰/헬스 (web-1)
K6_TARGETS="${K6_TARGETS:-http://localhost:18080,http://localhost:18081}"  # k6 부하 대상 (web-1,web-2 라운드로빈)
RUNS="${RUNS:-3}"
NO_UP="${NO_UP:-0}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_DIR="$REPO_ROOT/scripts/scenarios/concurrency"
OUT_DIR="$REPO_ROOT/scripts/results/lock-experiment"
mkdir -p "$OUT_DIR"

# ── 의존 도구 확인 ──
for cmd in k6 curl jq; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "❌ '$cmd' 가 필요합니다. (brew install $cmd)"; exit 1; }
done

# ── 스택 기동 (idempotent) ──
if [[ "$NO_UP" != 1 ]]; then
    echo "▶ 스택 기동: TEST_AUTH_ENABLED=true docker-compose up -d"
    ( cd "$REPO_ROOT" && TEST_AUTH_ENABLED=true docker-compose up -d ) || { echo "❌ docker-compose 실패"; exit 1; }
fi

# ── web 헬스 대기 (web-1, web-2 모두) ──
wait_health() {
    local url="$1" name="$2"
    echo "▶ $name 헬스 대기: $url/management/health (최대 180s)"
    for _ in $(seq 1 90); do
        curl -sf "$url/management/health" >/dev/null 2>&1 && { echo "✅ $name 준비 완료"; return 0; }
        sleep 2
    done
    echo "❌ $name 부팅 타임아웃. 로그: docker logs sulmun2yong-web-1 --tail 40"
    exit 1
}
wait_health "http://localhost:18080" "web-1"
wait_health "http://localhost:18081" "web-2"

# ── 테스트 엔드포인트 확인 ──
if ! curl -sf -X POST "$BASE_URL/api/v1/test/token" >/dev/null 2>&1; then
    echo "❌ 테스트 JWT 엔드포인트가 비활성입니다. (스택을 TEST_AUTH_ENABLED=true 로 기동했는지 확인)"
    exit 1
fi

# ── 실험 함수 ──
run_mode() {
    local mode="$1"
    for i in $(seq 1 "$RUNS"); do
        echo ""
        echo "=== Lock ${mode^^} run #$i / $RUNS ==="
        # access token 만료(10분) 방지 — 회차마다 재발급
        local token
        token=$(curl -s -X POST "$BASE_URL/api/v1/test/token" | jq -r .accessToken)
        local logfile="$OUT_DIR/${mode}-run${i}.txt"
        ( cd "$K6_DIR" && LOCK_MODE="$mode" k6 run \
            --env ACCESS_TOKEN="$token" --env BASE_URLS="$K6_TARGETS" drawing-concurrency.js ) | tee "$logfile"
    done
}

echo "════════════════════════════════════════"
echo " 분산락 실험 시작 — 각 모드 ${RUNS}회 · 타깃 $K6_TARGETS"
echo " 결과 저장: $OUT_DIR"
echo "════════════════════════════════════════"

run_mode off
run_mode on

echo ""
echo "✅ 완료. 원시 로그: $OUT_DIR/{off,on}-run*.txt"
echo ""
echo "다음으로 Grafana( http://localhost:13000 )에서 캡처하세요:"
echo "  • race-comparison → db_deadlock_total (OFF 3회 후 vs ON 3회 후 누적 차이 = 데드락 건수)"
echo "  • drawing-lock    → 락 대기 p95, 획득 성공/실패 비율"
echo ""
echo "k6 로그의 drawing_success_total / http_req_duration(p95) 값을 시나리오.md 결과 표에 옮기세요."
