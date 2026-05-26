#!/usr/bin/env bash

./gradlew build

set -euo pipefail

cd "$(dirname "$0")"

COMPOSE_FILE="docker-compose.cluster.yml"
SKIP_RESET=0

# --no-reset 옵션 처리
ARGS=()
for arg in "$@"; do
    if [ "$arg" = "--no-reset" ]; then
        SKIP_RESET=1
    else
        ARGS+=("$arg")
    fi
done

echo "→ docker compose restart ${ARGS[*]:-(전체)}"
# bash 3.x(macOS)에서 set -u + 빈 배열 expansion 호환 idiom
docker compose -f "$COMPOSE_FILE" up -d --force-recreate ${ARGS[@]+"${ARGS[@]}"}

if [ "$SKIP_RESET" -eq 1 ]; then
    echo "→ --no-reset 옵션 — 데이터 초기화 건너뜀"
    exit 0
fi

# MySQL이 다시 준비될 때까지 대기 (healthcheck 의존)
echo "→ MySQL ready 대기..."
for i in $(seq 1 30); do
    if docker exec sulmun2yong-cluster-mysql mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD:-password}" --silent 2>/dev/null; then
        break
    fi
    sleep 1
done

./reset-data.sh
