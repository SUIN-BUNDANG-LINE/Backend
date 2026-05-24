#!/usr/bin/env bash
# 전체 k6 시나리오 통합 러너
#
# 사전 준비:
#   1. 커스텀 k6 바이너리 빌드 (xk6 + xk6-sql + xk6-sql-driver-mysql)
#      go install go.k6.io/xk6/cmd/xk6@latest
#      xk6 build \
#        --with github.com/grafana/xk6-sql@latest \
#        --with github.com/grafana/xk6-sql-driver-mysql@latest
#      mv ./k6 ./k6_scripts/k6-custom
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6="${K6_BIN:-${SCRIPT_DIR}/k6-custom}"
MODE="${1:-all}"

if [ ! -x "${K6}" ]; then
    echo "❌ 커스텀 k6 바이너리를 찾을 수 없습니다: ${K6}"
    echo "   xk6 build로 빌드 후 ${SCRIPT_DIR}/k6-custom에 배치하세요."
    exit 1
fi

if [ -z "${ACCESS_TOKEN}" ]; then
    echo "⚠️  ACCESS_TOKEN 환경변수가 비어 있습니다 (인증 필요 시나리오는 실패합니다)."
fi

if [ -z "${DB_DSN}" ]; then
    echo "⚠️  DB_DSN 환경변수가 비어 있습니다. 기본값(user:password@tcp(localhost:3307)/test)이 사용됩니다."
fi

run_scenario() {
    local name="$1"
    local script="$2"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "▶ ${name}"
    echo "  ${script}"
    echo "═══════════════════════════════════════════════════════════"
    "${K6}" run --env ACCESS_TOKEN="${ACCESS_TOKEN}" --env DB_DSN="${DB_DSN}" "${SCRIPT_DIR}/${script}" || {
        echo "❌ ${name} 실패"
        return 1
    }
}

run_happy_path() {
    run_scenario "정상 — 추첨 동시성"          drawing-concurrency.js
    run_scenario "정상 — 추첨 Fan-out + 자동 종료" drawing-kafka-fanout.js
    run_scenario "정상 — Outbox 원자성"          outbox-atomicity.js
}

run_failover() {
    run_scenario "Relay — Outbox 보상 발행"   outbox-relay-recovery.js
    run_scenario "Relay — SKIP LOCKED 정합성" skip-locked-concurrency.js
    echo ""
    echo "▶ Producer DLQ 시나리오: 잘못된 토픽으로 Outbox 주입 후 5회 실패 마킹 검증"
    echo "  사전 조건: Kafka 클러스터의 auto.create.topics.enable=false 권장"
    run_scenario "Producer DLQ" outbox-producer-dlq.js
    echo ""
    echo "▶ SMS Failover 시나리오: application-secret.yml의 forcedFailCount=0 + 서버 재기동 필요"
    if [ "${SKIP_SMS:-0}" = "1" ]; then
        echo "  SKIP_SMS=1 → 건너뜀"
    else
        run_scenario "SMS Failover" sms-failover.js
    fi
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
