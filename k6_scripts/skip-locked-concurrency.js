/**
 * SKIP LOCKED 정합성 검증 (시나리오: Phase 1-10)
 *
 * 목적: 다수의 PENDING outbox 행이 동시에 존재할 때 OutboxRelayScheduler가
 *       FOR UPDATE SKIP LOCKED로 폴링하여 중복 발행 없이 정확히 1회만 PUBLISHED
 *       마킹하는지 검증. (단일 인스턴스 환경에서도 retry_count=0인 채로 PUBLISHED 1회만
 *       찍히는지 확인하면 락 충돌·중복 처리가 없었다는 의미)
 *
 * 흐름:
 *   1. setup: PENDING 200건을 동시에 INSERT (배치)
 *   2. default: 트래픽 없음
 *   3. teardown: 폴링하여 모두 PUBLISHED + retry_count=0 + 발행 횟수 정확히 200회
 *
 * 멀티 인스턴스 검증이 필요한 경우:
 *   서버를 8080/8081 포트로 2대 띄우고 동일 DB·Kafka를 공유. 둘 다 Relay가 돌아도
 *   SKIP LOCKED 덕분에 중복 발행이 없어야 함. 이 스크립트는 어느 환경에서나 동일하게 동작.
 *
 * 실행:
 *   ./k6-custom run --env DB_DSN="..." --env INJECT_COUNT=200 skip-locked-concurrency.js
 */
import { check } from 'k6';
import {
    db,
    closeDb,
    insertPendingOutbox,
    getOutboxStatsByTopic,
    cleanupTestOutbox,
    awaitCondition,
} from './lib/db.js';

const TOPIC = 'survey-response-submitted';
const INJECT_COUNT = Number(__ENV.INJECT_COUNT || 200);
const TIMEOUT_SEC = Number(__ENV.TIMEOUT_SEC || 120);

export const options = {
    scenarios: {
        skip_locked: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '5m',
        },
    },
};

export function setup() {
    cleanupTestOutbox();
    const before = getOutboxStatsByTopic(TOPIC);

    const keyPrefix = `skiplocked-${Date.now()}`;
    console.log(`PENDING ${INJECT_COUNT}건 일괄 주입 중...`);

    for (let i = 0; i < INJECT_COUNT; i++) {
        const key = `${keyPrefix}-${i}`;
        const payload = JSON.stringify({ test: 'skip-locked', idx: i, key });
        insertPendingOutbox('TestEvent', TOPIC, key, payload, 90);
    }

    const after = getOutboxStatsByTopic(TOPIC);
    console.log(`주입 전: ${JSON.stringify(before)}, 주입 후: ${JSON.stringify(after)}`);
    return { keyPrefix, beforePublished: before.PUBLISHED };
}

export default function () {
    // 트래픽 없음
}

export function teardown(data) {
    const ok = awaitCondition(
        () => {
            const stats = getOutboxStatsByTopic(TOPIC);
            // 주입 전 대비 PUBLISHED가 INJECT_COUNT만큼 정확히 증가
            return stats.PENDING === 0 && stats.PUBLISHED >= data.beforePublished + INJECT_COUNT;
        },
        `PENDING ${INJECT_COUNT}건 모두 PUBLISHED 처리`,
        TIMEOUT_SEC,
        3,
    );

    const final = getOutboxStatsByTopic(TOPIC);
    const newlyPublished = final.PUBLISHED - data.beforePublished;

    // 모든 TEST_K6 행이 retry_count=0인지 확인 (재시도 없이 한 번에 발행)
    const retryRows = db.query(
        "SELECT COUNT(*) AS cnt FROM outbox_events WHERE aggregate_type = 'TEST_K6' AND retry_count > 0",
    );
    const retried = Number(retryRows[0].cnt);

    console.log(
        `최종: ${JSON.stringify(final)}, 신규 PUBLISHED=${newlyPublished}, retry>0인 행=${retried}`,
    );

    check(null, {
        'PENDING 잔량 0': () => final.PENDING === 0,
        'FAILED 행 없음': () => final.FAILED === 0,
        '정확히 INJECT_COUNT만큼 신규 PUBLISHED': () => newlyPublished === INJECT_COUNT,
        '재시도 없이 1회 발행 (retry_count=0)': () => retried === 0,
        '폴링 타임아웃 미발생': () => ok,
    });

    cleanupTestOutbox();
    closeDb();
}
