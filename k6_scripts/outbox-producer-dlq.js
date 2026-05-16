/**
 * Outbox Producer DLQ 검증 (시나리오: Phase 1-10)
 *
 * 목적: 발행 불가능한 토픽(존재하지 않는 토픽 또는 ACL 거부)으로 PENDING 주입 후,
 *       OutboxRelayScheduler가 5회 재시도 끝에 status=FAILED, retry_count=5 마킹하는지 검증.
 *
 * 사전 조건:
 *   - Kafka 클러스터에 auto.create.topics.enable=false 설정 (또는 권한 없는 토픽 사용)
 *   - 또는 broker가 도달 불가능한 가상의 잘못된 토픽명 사용
 *
 * 흐름:
 *   1. setup: 잘못된 토픽으로 PENDING 1건 INSERT
 *   2. default: 트래픽 없음
 *   3. teardown: 최대 6분 폴링 (Relay 5초 주기 × 5회 + 여유) → FAILED + retry_count=5 검증
 *
 * 실행:
 *   ./k6-custom run --env DB_DSN="..." --env BAD_TOPIC="non-existent-topic-k6test" outbox-producer-dlq.js
 */
import {check} from 'k6';
import {
    db,
    closeDb,
    insertPendingOutbox,
    getMaxOutboxRetryCountForKey,
    cleanupTestOutbox,
    awaitCondition,
} from './lib/db.js';

const BAD_TOPIC = __ENV.BAD_TOPIC || 'non-existent-topic-k6dlqtest';
const TIMEOUT_SEC = Number(__ENV.TIMEOUT_SEC || 360); // 6분

export const options = {
    scenarios: {
        producer_dlq: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '7m',
        },
    },
};

export function setup() {
    cleanupTestOutbox();

    const key = `dlq-test-${Date.now()}`;
    const payload = JSON.stringify({test: 'producer-dlq', key});

    insertPendingOutbox('TestDlqEvent', BAD_TOPIC, key, payload, 90);
    console.log(`잘못된 토픽 ${BAD_TOPIC}로 PENDING 1건 주입 (key=${key})`);
    return {key};
}

export default function () {
    // 트래픽 없음
}

export function teardown(data) {
    const ok = awaitCondition(
        () => {
            const r = getMaxOutboxRetryCountForKey(data.key);
            console.log(`현재 상태: status=${r.status}, retry_count=${r.maxRetry}`);
            return r.status === 'FAILED' && r.maxRetry === 5;
        },
        `outbox key=${data.key}가 FAILED + retry_count=5에 도달`,
        TIMEOUT_SEC,
        5,
    );

    const final = getMaxOutboxRetryCountForKey(data.key);

    check(null, {
        'status=FAILED 마킹됨': () => final.status === 'FAILED',
        'retry_count=5 (MAX_RETRY_COUNT)': () => final.maxRetry === 5,
        'awaitCondition 타임아웃 미발생': () => ok,
    });

    cleanupTestOutbox();
    closeDb();
}
