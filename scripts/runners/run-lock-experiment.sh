#!/usr/bin/env bash
#
# 분산락 효과 측정 실험 러너 (Lock OFF vs ON)
#
# docs/kafka-distribute-lock/시나리오.md 의 실측 절차를 하나로 묶은 스크립트.
# drawing-concurrency.js 를 LOCK_MODE=off 3회 → on 3회 실행하고 결과를 파일로 저장한다.
#
# 사전 조건 (스크립트가 직접 띄우지 않음 — bootRun 은 블로킹이므로 별도 터미널에서 실행):
#   1) docker-compose up -d
#   2) TEST_AUTH_ENABLED=true ./gradlew :web:bootRun
#
# 사용법:
#   ./scripts/runners/run-lock-experiment.sh              # OFF 3회 + ON 3회
#   RUNS=5 ./scripts/runners/run-lock-experiment.sh       # 회차 조정
#   BASE_URL=http://localhost:8081 ./scripts/runners/run-lock-experiment.sh
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
RUNS="${RUNS:-3}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_DIR="$REPO_ROOT/scripts/scenarios/concurrency"
OUT_DIR="$REPO_ROOT/scripts/results/lock-experiment"
mkdir -p "$OUT_DIR"

# ── 의존 도구 확인 ──
for cmd in k6 curl jq; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "❌ '$cmd' 가 필요합니다. (brew install $cmd)"; exit 1; }
done

# ── 서버 헬스체크 ──
echo "▶ 서버 확인: $BASE_URL/management/health"
if ! curl -sf "$BASE_URL/management/health" >/dev/null 2>&1; then
    echo "❌ 서버에 연결할 수 없습니다."
    echo "   먼저 실행하세요: TEST_AUTH_ENABLED=true ./gradlew :web:bootRun"
    exit 1
fi

# ── 테스트 엔드포인트 확인 ──
if ! curl -sf -X POST "$BASE_URL/api/v1/test/token" >/dev/null 2>&1; then
    echo "❌ 테스트 JWT 엔드포인트가 비활성입니다."
    echo "   서버를 TEST_AUTH_ENABLED=true 로 실행했는지 확인하세요."
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
        ( cd "$K6_DIR" && LOCK_MODE="$mode" k6 run --env ACCESS_TOKEN="$token" drawing-concurrency.js ) | tee "$logfile"
    done
}

echo "════════════════════════════════════════"
echo " 분산락 실험 시작 — 각 모드 ${RUNS}회"
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
