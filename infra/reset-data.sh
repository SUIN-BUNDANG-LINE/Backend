#!/usr/bin/env bash
# 부하 테스트 데이터 초기화 — 컨테이너 재시작 없이 모든 테이블 TRUNCATE + Redis FLUSHALL
# Flyway 메타데이터(flyway_schema_history)는 보존하여 마이그레이션 재실행 회피.
#
# 사용:
#   ./reset-data.sh

set -euo pipefail

cd "$(dirname "$0")"

# .env 파일 로드 (있으면)
if [ -f .env ]; then
    set -a; source .env; set +a
fi

MYSQL_CONTAINER="sulmun2yong-cluster-mysql"
REDIS_CONTAINER="sulmun2yong-cluster-redis"
MYSQL_DB="${MYSQL_DATABASE:-test}"
MYSQL_PW="${MYSQL_ROOT_PASSWORD:-password}"
REDIS_PW="${REDIS_PASSWORD:-}"

echo "→ MySQL 테이블 TRUNCATE (DB: $MYSQL_DB)"

# 모든 사용자 테이블에 대한 TRUNCATE 문 생성 (Flyway 메타 제외)
TRUNCATES=$(docker exec "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PW" -N -B -e "
SELECT CONCAT('TRUNCATE TABLE \`', table_name, '\`;')
FROM information_schema.tables
WHERE table_schema = '$MYSQL_DB'
  AND table_name NOT IN ('flyway_schema_history');
" 2>/dev/null)

if [ -z "$TRUNCATES" ]; then
    echo "  (사용자 테이블 없음)"
else
    docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_PW" "$MYSQL_DB" 2>/dev/null <<EOF
SET FOREIGN_KEY_CHECKS = 0;
$TRUNCATES
SET FOREIGN_KEY_CHECKS = 1;
EOF
    echo "  ✓ $(echo "$TRUNCATES" | wc -l | tr -d ' ')개 테이블 비움"
fi

echo "→ Redis FLUSHALL"
if [ -n "$REDIS_PW" ]; then
    docker exec "$REDIS_CONTAINER" redis-cli -a "$REDIS_PW" FLUSHALL 2>/dev/null
else
    docker exec "$REDIS_CONTAINER" redis-cli FLUSHALL
fi
echo "  ✓ Redis 비움"

echo "✓ 데이터 초기화 완료"
