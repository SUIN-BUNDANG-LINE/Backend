/**
 * Outbox Relay 보상 경로 검증 (시나리오: S5 / Phase 1-8)
 *
 * 목적: Outbox에 직접 PENDING 행을 주입하여, OutboxRelayScheduler가
 *       SKIP LOCKED + JPA hint(-2)로 PENDING을 폴링·발행하는지 검증한다.
 *
 * 흐름:
 *   1. setup: kafka_record_outbox에 PENDING 100건 직접 INSERT (created_at = now() - 90s)
 *   2. default: HTTP 트래픽 없음 (DB 주입만으로 Relay 동작 확인)
 *   3. teardown: Relay가 60초 내에 모두 PUBLISHED로 전환했는지 폴링
 *
 * 사전 조건: Relay가 정상 동작하는 토픽이어야 함 (k6-outbox-test — 전용 실험 트랙)
 *
 * 실행:
 *   ./k6-custom run --env DB_DSN="user:pass@tcp(localhost:13306)/test?parseTime=true" outbox-relay-recovery.js
 */
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import {
    db,
    closeDb,
    insertPendingOutbox,
    getOutboxStatsByTopic,
    cleanupTestOutbox,
    awaitCondition,
} from '../../lib/db.js';
import { startAnnotation, endAnnotation } from '../../lib/annotations.js';

const TOPIC = 'k6-outbox-test';   // 구독자 없는 전용 실험 트랙 (도메인 무관)
const INJECT_COUNT = Number(__ENV.INJECT_COUNT || 100);
const TIMEOUT_SEC = Number(__ENV.TIMEOUT_SEC || 60);

const relayedTotal = new Counter('relay_published_total');
const relayPendingRemaining = new Counter('relay_pending_remaining');

export const options = {
    scenarios: {
        relay_recovery: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '5m',
        },
    },
};

export function setup() {
    cleanupTestOutbox();

    const keyPrefix = `k6-relay-test-${Date.now()}`;
    console.log(`PENDING ${INJECT_COUNT}건 주입 중... (키 prefix: ${keyPrefix})`);

    for (let i = 0; i < INJECT_COUNT; i++) {
        const key = `${keyPrefix}-${i}`;
        const payload = JSON.stringify({ test: true, idx: i, key });
        insertPendingOutbox(TOPIC, key, payload, 90);
    }

    const before = getOutboxStatsByTopic(TOPIC);
    console.log(`주입 완료. 토픽 ${TOPIC} 현황: ${JSON.stringify(before)}`);

    const annotationId = startAnnotation('outbox-relay-recovery', ['outbox', 'recovery']);

    return { keyPrefix, beforePending: before.PENDING, annotationId };
}

export default function () {
    // 트래픽 없음 — DB 주입만으로 Relay가 동작하는지 검증
}

export function teardown(data) {
    const expectedPublished = data.beforePending; // 주입 직전 PENDING 수가 모두 PUBLISHED 되어야 함

    const ok = awaitCondition(
        () => {
            const stats = getOutboxStatsByTopic(TOPIC);
            // 주입한 INJECT_COUNT만큼 PUBLISHED가 늘어났는지 (다른 정상 트래픽 영향 최소화 가정)
            return stats.PENDING === 0 || stats.PUBLISHED >= expectedPublished;
        },
        `Relay가 PENDING ${INJECT_COUNT}건을 모두 발행`,
        TIMEOUT_SEC,
        2,
    );

    const finalStats = getOutboxStatsByTopic(TOPIC);
    console.log(`최종 outbox stats (topic=${TOPIC}): ${JSON.stringify(finalStats)}`);

    relayedTotal.add(finalStats.PUBLISHED);
    relayPendingRemaining.add(finalStats.PENDING);

    check(null, {
        'Relay가 모든 PENDING을 PUBLISHED로 전환': () => ok,
        'PENDING 잔량 0': () => finalStats.PENDING === 0,
        'FAILED 마킹된 행 없음': () => finalStats.FAILED === 0,
    });

    cleanupTestOutbox();
    closeDb();

    endAnnotation(data?.annotationId);
}
