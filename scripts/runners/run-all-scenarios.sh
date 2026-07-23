#!/usr/bin/env bash
# 전체 k6 시나리오 통합 러너
#
# 사전 준비:
#   1. 커스텀 k6 바이너리 빌드 (xk6 + xk6-sql + xk6-sql-driver-mysql)
#      go install go.k6.io/xk6/cmd/xk6@latest
#      xk6 build \
#        --with github.com/grafana/xk6-sql@latest \
#        --with github.com/grafana/xk6-sql-driver-mysql@latest
#      mv ./k6 ./scripts/bin/k6-custom
#
#   2. 환경변수 설정
#      export ACCESS_TOKEN="<JWT 토큰>"
#      export DB_DSN="user:password@tcp(localhost:3307)/test?parseTime=true"
#
#   3. 인프라 기동 (MySQL, Redis, Kafka)
#      docker-compose up -d
#      ./gradlew bootRun
#
# 사용:
#   ./run-all-scenarios.sh           # 전체 시나리오
#   ./run-all-scenarios.sh happy     # 정상 시나리오만
#   ./run-all-scenarios.sh failover  # 장애/복구 시나리오만

set -e

# 러너는 scripts/runners/ 에 있고, k6 자산은 scripts/{bin,scenarios,lib}/ 에 있다.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BIN_DIR="${REPO_ROOT}/scripts/bin"
SCEN_DIR="${REPO_ROOT}/scripts/scenarios"
K6="${K6_BIN:-${BIN_DIR}/k6-custom}"
MODE="${1:-all}"

# .env 자동 로드 — GRAFANA_URL/GRAFANA_TOKEN 등 k6 시나리오용 변수를 셸에 export
if [ -f "${REPO_ROOT}/.env" ]; then
    set -a
    source "${REPO_ROOT}/.env"
    set +a
fi

if [ ! -x "${K6}" ]; then
    echo "❌ 커스텀 k6 바이너리를 찾을 수 없습니다: ${K6}"
    echo "   xk6 build로 빌드 후 ${BIN_DIR}/k6-custom에 배치하세요."
    exit 1
fi

if [ -z "${ACCESS_TOKEN}" ]; then
    echo "⚠️  ACCESS_TOKEN 환경변수가 비어 있습니다 (인증 필요 시나리오는 실패합니다)."
fi

if [ -z "${DB_DSN}" ]; then
    echo "⚠️  DB_DSN 환경변수가 비어 있습니다. 기본값(user:password@tcp(localhost:3307)/test)이 사용됩니다."
fi

if [ -z "${GRAFANA_TOKEN}" ]; then
    echo "ℹ️  GRAFANA_TOKEN 미설정 — Grafana annotation 마킹은 graceful skip됩니다."
fi

run_scenario() {
    local name="$1"
    local script="$2"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "▶ ${name}"
    echo "  ${script}"
    echo "═══════════════════════════════════════════════════════════"
    "${K6}" run --env ACCESS_TOKEN="${ACCESS_TOKEN}" --env DB_DSN="${DB_DSN}" "${SCEN_DIR}/${script}" || {
        echo "❌ ${name} 실패"
        return 1
    }
}

run_happy_path() {
    run_scenario "정상 — 추첨 동시성"          concurrency/drawing-concurrency.js
    run_scenario "정상 — 추첨 Fan-out + 자동 종료" kafka/drawing-kafka-fanout.js
    run_scenario "정상 — Outbox 원자성"          outbox/outbox-atomicity.js
}

run_failover() {
    run_scenario "Relay — Outbox 보상 발행"   outbox/outbox-relay-recovery.js
    run_scenario "Relay — SKIP LOCKED 정합성" concurrency/skip-locked-concurrency.js
}

case "${MODE}" in
    happy)
        run_happy_path
        ;;
    failover)
        run_failover
        ;;
    all)
        run_happy_path
        run_failover
        ;;
    *)
        echo "사용법: $0 [happy|failover|all]"
        exit 1
        ;;
esac

echo ""
echo "✅ 시나리오 완료 (${MODE})"
