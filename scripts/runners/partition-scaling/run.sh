#!/usr/bin/env bash
# 파티션 병렬 소비 확장 실험 러너.
#
#   ./scripts/runners/partition-scaling/run.sh smoke    # 부팅·소비 검증 + 인스턴스당 처리율 캘리브레이션
#   ./scripts/runners/partition-scaling/run.sh run      # 증설 실험: 60초마다 컨슈머 1->7, lag 시계열 기록
#   ./scripts/runners/partition-scaling/run.sh outage   # 장애 실험: 컨슈머 kill->재기동, 무유실 등식 판정
#
# 환경변수:
#   RATE=350        run 모드 프로듀서 고정 발행 속도(msg/s) - 3대(~330)로는 부족, 4대(~440)에서 반전하도록
#   OUTAGE_RATE=50  outage 모드 발행 속도 - 소비 능력(~90/s) 미만이어야 복귀 후 따라잡음
#   STEP_SECONDS=60 증설 간격
#   INSTANCES=6     최종 인스턴스 수 (= 파티션 수)
#   DB_LATENCY_MS=2 스텁의 DB 왕복 흉내 지연
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
COMPOSE_FILE="$ROOT/tests/partition-scaling/docker-compose.yml"
JAR="$ROOT/module-cofunding/build/libs/cofunding-0.0.1-SNAPSHOT.jar"
BROKER_IN=pscale-kafka:9092
KBIN=/opt/kafka/bin
TOPIC=payment-succeeded
GROUP=cofunding-payment-succeeded

RATE="${RATE:-350}"
STEP_SECONDS="${STEP_SECONDS:-60}"
INSTANCES="${INSTANCES:-6}"
DB_LATENCY_MS="${DB_LATENCY_MS:-2}"

MODE="${1:-run}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$ROOT/tests/partition-scaling/results/$MODE-$STAMP"
mkdir -p "$OUT"

INSTANCES_STARTED=()
POLLER_PID=""

kx() { docker exec pscale-kafka "$KBIN/$1" --bootstrap-server "$BROKER_IN" "${@:2}"; }

dump_instance_logs() {
    for i in "${INSTANCES_STARTED[@]:-}"; do
        docker logs "pscale-consumer-$i" > "$OUT/instance-$i.log" 2>&1 || true
    done
}

cleanup() {
    dump_instance_logs
    for i in "${INSTANCES_STARTED[@]:-}"; do docker rm -f "pscale-consumer-$i" >/dev/null 2>&1 || true; done
    [ -n "$POLLER_PID" ] && kill "$POLLER_PID" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

fresh_stack() {
    docker compose -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
    docker compose -f "$COMPOSE_FILE" up -d
    for _ in $(seq 1 30); do
        if kx kafka-topics.sh --list >/dev/null 2>&1; then return 0; fi
        sleep 2
    done
    echo "브로커 기동 실패" >&2
    exit 1
}

create_topics() {
    # 실험 대상만 6파티션, 나머지는 앱의 KafkaAdmin 생성 시도(RF=3)가 실패하지 않게 RF=1 로 선점
    kx kafka-topics.sh --create --topic "$TOPIC" --partitions 6 --replication-factor 1
    for t in drawing-completed drawing-notification.DLT co-funding-expired co-funding-created \
             payment-failed payment-refunded payment-cancel-requested co-funding-confirmed \
             co-funding-requested co-funding-reviewed saga.DLT; do
        kx kafka-topics.sh --create --topic "$t" --partitions 3 --replication-factor 1 >/dev/null
    done
}

make_payload() {
    local n=1000 f="$OUT/payload.txt"
    : > "$f"
    for i in $(seq 1 "$n"); do
        printf '{"eventId":"exp-evt-%s","orderId":"exp-order-%s","productType":"DRAWING_BOARD","productId":"exp-board-%s","succeededAt":"2026-08-16T00:00:00Z"}\n' \
            "$i" "$i" "$i" >> "$f"
    done
    docker cp "$f" pscale-kafka:/tmp/payload.txt
}

# 컨슈머 인스턴스 = 컨테이너 - cgroup 으로 CPU 1코어·메모리 하드 캡 (JVM 17 이 한도 자동 인식)
start_instance() {
    local i="$1"
    docker run -d --name "pscale-consumer-$i" \
        --network partition-scaling_default \
        --cpus=1 --memory 400m \
        -v "$JAR":/app/app.jar:ro \
        eclipse-temurin:17-jre \
        java -Xms128m -Xmx128m -XX:MaxMetaspaceSize=128m \
        -Dexperiment.db-latency-ms="$DB_LATENCY_MS" \
        ${DB_PROPS:-} \
        -jar /app/app.jar \
        --spring.profiles.active=experiment \
        --spring.kafka.bootstrap-servers=pscale-kafka:9092 \
        --server.port=8080 \
        >/dev/null
    # 모니터링 네트워크 접속 - Prometheus 스크레이프(무결성 게이지) + cluster MySQL 접근용
    docker network connect sulmun2yong-cluster "pscale-consumer-$i" >/dev/null 2>&1 || true
    INSTANCES_STARTED+=("$i")
    echo "[$(date +%T)] 인스턴스 $i 기동 (컨테이너 pscale-consumer-$i, cpus=1)"
}

# 그룹 상태 1줄: "offsetSum lagSum memberCount"
group_snapshot() {
    kx kafka-consumer-groups.sh --describe --group "$GROUP" 2>/dev/null |
        awk -v topic="$TOPIC" '
            $2 == topic {
                matched = 1
                cur += ($4 == "-" ? 0 : $4)
                lag += ($6 == "-" ? 0 : $6)
                if ($7 != "-" && $7 != "") members[$7] = 1
            }
            END {
                if (NR == 0 || matched == 0) { printf "0 -1 0\n" }
                else { printf "%d %d %d\n", cur, lag, length(members) }
            }'
}

wait_members() {
    local want="$1" timeout="${2:-90}"
    for _ in $(seq 1 "$timeout"); do
        read -r _ _ m <<< "$(group_snapshot)"
        [ "${m:-0}" -ge "$want" ] && return 0
        sleep 1
    done
    echo "컨슈머 $want 개 합류 대기 시간 초과" >&2
    return 1
}

start_poller() {
    local t0="$1"
    (
        echo "epoch,elapsed,members,offset_sum,lag_sum" > "$OUT/timeline.csv"
        while true; do
            now=$(date +%s)
            read -r cur lag m <<< "$(group_snapshot)"
            echo "$now,$((now - t0)),${m:-0},${cur:-0},${lag:-0}" >> "$OUT/timeline.csv"
            sleep 5
        done
    ) &
    POLLER_PID="$!"
}

produce() {
    local num="$1" rate="$2"
    docker exec pscale-kafka "$KBIN/kafka-producer-perf-test.sh" \
        --topic "$TOPIC" \
        --num-records "$num" \
        --throughput "$rate" \
        --payload-file /tmp/payload.txt \
        --producer-props bootstrap.servers="$BROKER_IN" \
        > "$OUT/producer.log" 2>&1
}

# 1..N 정수를 orderId 에 실어 "각 1회" 순차 발행 (perf-test 는 무작위 추출이라 부적합)
produce_sequential() {
    local n="$1" rate="$2"
    seq 1 "$n" | awk -v r="$rate" '{
        printf "{\"eventId\":\"exp-evt-%d\",\"orderId\":\"exp-order-%d\",\"productType\":\"DRAWING_BOARD\",\"productId\":\"exp-board-%d\",\"succeededAt\":\"2026-08-17T00:00:00Z\"}\n", $1, $1, $1
        if (NR % r == 0) { fflush(); system("sleep 1") }
    }' | docker exec -i pscale-kafka "$KBIN/kafka-console-producer.sh" \
        --topic "$TOPIC" --bootstrap-server "$BROKER_IN" \
        > "$OUT/producer.log" 2>&1
}

# 그라파나에 최신 실행만 보이게 - 이전 실험 시계열 삭제 (실험 잡 한정, 운영 메트릭 무관)
purge_experiment_series() {
    if curl -s -X POST 'http://localhost:19090/api/v1/admin/tsdb/delete_series' \
        --data-urlencode 'match[]={job="partition-scaling-kafka"}' >/dev/null 2>&1; then
        curl -s -X POST 'http://localhost:19090/api/v1/admin/tsdb/clean_tombstones' >/dev/null 2>&1 || true
        echo "이전 실험 시계열 삭제됨 (Prometheus)"
    else
        echo "(Prometheus admin API 미응답 - 이전 실험 시계열 유지된 채 진행)"
    fi
}

grafana_link() {
    echo "그라파나 (이번 실행 구간만): http://localhost:13000/d/partition-scaling?from=$((($1 - 30) * 1000))&to=$((($2 + 30) * 1000))"
}

case "$MODE" in
smoke)
    echo "== 스모크: 부팅 검증 + 인스턴스당 처리율 캘리브레이션 =="
    fresh_stack
    create_topics
    make_payload
    start_instance 1
    wait_members 1 120
    echo "컨슈머 합류 확인 - 3000건 즉시 발행"
    t0=$(date +%s)
    produce 3000 -1
    while true; do
        read -r cur lag _ <<< "$(group_snapshot)"
        echo "  +$(( $(date +%s) - t0 ))s 소비=${cur:-0} lag=${lag:-?}"
        [ "${cur:-0}" -ge 3000 ] && break
        sleep 1
    done
    t1=$(date +%s)
    elapsed=$((t1 - t0))
    dlt=$(kx kafka-get-offsets.sh --topic saga.DLT 2>/dev/null |
        awk -F: '{ s += $3 } END { print s+0 }') || dlt="?"
    rate=$((3000 / (elapsed > 0 ? elapsed : 1)))
    echo "3000건 소비 완료: ${elapsed}s -> 인스턴스당 약 ${rate} msg/s (saga.DLT 적재: ${dlt}건 - 0 이어야 정상)"
    echo "권장 본실험 발행 속도: RATE=$((rate * 7 / 2))  (인스턴스당 처리율 x 3.5)"
    docker inspect pscale-consumer-1 --format 'CPU 상한: {{.HostConfig.NanoCpus}} nano (1코어=1000000000), 메모리: {{.HostConfig.Memory}}'
    dump_instance_logs
    grep -iE "error|exception" "$OUT/instance-1.log" | grep -v "Bootstrap broker" | head -5 || true
    ;;
run)
    TAIL_SECONDS=120
    NUM_RECORDS=$((RATE * (STEP_SECONDS * INSTANCES + TAIL_SECONDS)))
    echo "== 증설 실험: 발행 ${RATE}msg/s x $((STEP_SECONDS * INSTANCES + TAIL_SECONDS))s (${NUM_RECORDS}건), ${STEP_SECONDS}s 간격 증설 1->${INSTANCES} =="
    purge_experiment_series
    fresh_stack
    create_topics
    make_payload

    start_instance 1
    wait_members 1 120
    echo "인스턴스 1 합류 완료 - 발행·측정 시작"

    T0=$(date +%s)
    start_poller "$T0"
    produce "$NUM_RECORDS" "$RATE" &
    PRODUCER_PID="$!"

    for i in $(seq 2 "$INSTANCES"); do
        sleep "$STEP_SECONDS"
        start_instance "$i"
    done

    wait "$PRODUCER_PID" || true
    echo "발행 완료 - 잔여 lag 소진 대기 (최대 750s)"
    for _ in $(seq 1 150); do
        read -r _ lag _ <<< "$(group_snapshot)"
        [ "${lag:--1}" -eq 0 ] && break
        sleep 5
    done

    # 마지막 멤버 배정표
    kx kafka-consumer-groups.sh --describe --group "$GROUP" --members --verbose \
        > "$OUT/final-members.txt" 2>/dev/null || true
    read -r cur lag m <<< "$(group_snapshot)"
    echo "종료 시점: 소비 오프셋 합=${cur}, lag=${lag}, 멤버=${m}"
    # 최종 판정값이 그라파나 스탯 패널에 찍히도록 스크레이프 3주기 대기
    sleep 16
    echo "결과: $OUT/timeline.csv , final-members.txt , instance-*.log , producer.log"
    grafana_link "$T0" "$(date +%s)"
    ;;
outage)
    # 무유실 실증(종단 검증판): 정수 1..N 을 각 1회 발행 -> 컨슈머가 DB(PK=n)에 영속화.
    # docker kill 로 프로세스 사망을 재현해도 DB 기록이 남아 다음 등식으로 판정한다:
    #   고유 수신 = N, 정수 합 = N(N+1)/2, 재전달(received_count-1 합)은 별도 계수(at-least-once 증거)
    OUTAGE_RATE="${OUTAGE_RATE:-50}"
    N_INTS="${N_INTS:-10000}"
    KILL_AT=60
    RESTART_AT=180
    EXPECTED_SUM=$((N_INTS * (N_INTS + 1) / 2))
    DB_PROPS="-Dexperiment.db-url=jdbc:mysql://sulmun2yong-cluster-mysql:3306/cofunding_db -Dexperiment.db-user=user -Dexperiment.db-password=password"
    echo "== 장애 실험: 정수 1..${N_INTS} 를 ${OUTAGE_RATE}건/s 발행, t=${KILL_AT}s kill / t=${RESTART_AT}s 재기동 =="
    echo "   기대: 고유 ${N_INTS} 건, 합 ${EXPECTED_SUM}"
    purge_experiment_series

    docker exec sulmun2yong-cluster-mysql mysql -uuser -ppassword cofunding_db -e \
        "DROP TABLE IF EXISTS experiment_received;
         CREATE TABLE experiment_received (id BIGINT AUTO_INCREMENT PRIMARY KEY, \`value\` BIGINT NOT NULL, received_count INT NOT NULL DEFAULT 1, UNIQUE KEY uk_experiment_received_value (\`value\`));" 2>/dev/null
    echo "무결성 테이블 초기화 (cofunding_db.experiment_received)"

    fresh_stack
    create_topics

    start_instance 1
    wait_members 1 120
    echo "인스턴스 합류 완료 - 발행·측정 시작"

    T0=$(date +%s)
    start_poller "$T0"
    produce_sequential "$N_INTS" "$OUTAGE_RATE" &
    PRODUCER_PID="$!"

    sleep "$KILL_AT"
    docker kill pscale-consumer-1 >/dev/null
    echo "[$(date +%T)] 컨슈머 강제 종료 (t=${KILL_AT}s) - 발행은 계속"

    sleep $((RESTART_AT - KILL_AT))
    docker start pscale-consumer-1 >/dev/null
    echo "[$(date +%T)] 같은 컨슈머 재기동 (t=${RESTART_AT}s) - DB 기록은 사망을 넘어 보존"

    wait "$PRODUCER_PID" || true
    echo "발행 완료 - 잔여 lag 소진 대기 (최대 750s)"
    for _ in $(seq 1 150); do
        read -r _ lag _ <<< "$(group_snapshot)"
        [ "${lag:--1}" -eq 0 ] && break
        sleep 5
    done

    # 게이지 갱신(5s)·스크레이프(5s) 반영 대기
    sleep 16

    read -r cur lag m <<< "$(group_snapshot)"
    row=$(docker exec sulmun2yong-cluster-mysql mysql -uuser -ppassword cofunding_db -N -e \
        "SELECT COUNT(*), COALESCE(SUM(\`value\`),0), COALESCE(SUM(received_count-1),0) FROM experiment_received;" 2>/dev/null)
    read -r uniq total dup <<< "$row"
    echo "── 종단 무결성 판정 (DB) ──"
    echo "고유 수신: ${uniq} / ${N_INTS}  |  정수 합: ${total} / ${EXPECTED_SUM}  |  재전달: ${dup}건"
    echo "브로커 관점: 소비 오프셋 합=${cur} (발행 $((N_INTS)) + 재전달분), 잔여 lag=${lag}"
    if [ "$uniq" -eq "$N_INTS" ] && [ "$total" -eq "$EXPECTED_SUM" ] && [ "$lag" -eq 0 ]; then
        echo "판정: 통과 - 프로세스 사망 $((RESTART_AT - KILL_AT))초에도 정수 1..${N_INTS} 전부 정확히 반영, 유실 0"
        echo "      (재전달 ${dup}건은 at-least-once 의 증거 - UNIQUE(value) 멱등이 흡수해 합은 불변)"
    else
        echo "판정: 실패 - 등식 불일치"
    fi
    echo "결과: $OUT/timeline.csv , producer.log , instance-1.log"
    grafana_link "$T0" "$(date +%s)"
    ;;
*)
    echo "사용법: $0 smoke|run|outage" >&2
    exit 1
    ;;
esac
