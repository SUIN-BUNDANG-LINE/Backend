/**
 * Outbox 원자성 부하 테스트 — 중복 응답 방어 검증
 *
 * 원본: OutboxAtomicityIntegrationTest.kt
 *
 * 시나리오:
 *   1. 설문 생성 (targetParticipantCount=100)
 *   2. 동일 visitorId로 첫 번째 응답 제출 → 성공
 *   3. 동일 visitorId로 두 번째 응답 제출 → AlreadyParticipatedException (실패)
 *   4. 부하 테스트: 같은 visitorId로 N번 동시 요청 → 1번만 성공
 *
 * 실행:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *          --env ACCESS_TOKEN=<jwt> \
 *          outbox-atomicity.js
 *
 * 환경변수 (선택):
 *   DUPLICATE_VUS — 중복 요청 동시 VU 수 (기본: 10)
 */
import { check, group, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { setupSurveyWithDrawing, submitResponse } from './lib/helpers.js';

const DUPLICATE_VUS = parseInt(__ENV.DUPLICATE_VUS || '10');

// ── 커스텀 메트릭 ──
const duplicateSuccess = new Counter('duplicate_submit_success');
const duplicateFail = new Counter('duplicate_submit_fail');
const duplicateSuccessRate = new Rate('duplicate_success_rate');

export const options = {
    scenarios: {
        // 시나리오 1: 기본 원자성 검증 (순차)
        basic_atomicity: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '30s',
            exec: 'basicAtomicityTest',
            tags: { scenario: 'basic' },
        },
        // 시나리오 2: 동일 visitorId 동시 제출 (부하)
        concurrent_duplicate: {
            executor: 'shared-iterations',
            vus: DUPLICATE_VUS,
            iterations: DUPLICATE_VUS,
            maxDuration: '30s',
            exec: 'concurrentDuplicateTest',
            startTime: '35s',
            tags: { scenario: 'concurrent_duplicate' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<3000'],
    },
};

// ── setup ──
export function setup() {
    // 시나리오 1용 설문
    const survey1 = setupSurveyWithDrawing({
        targetParticipantCount: 100,
        rewardCount: 1,
        title: 'k6 원자성 테스트 (기본)',
    });

    // 시나리오 2용 설문
    const survey2 = setupSurveyWithDrawing({
        targetParticipantCount: 100,
        rewardCount: 1,
        title: 'k6 원자성 테스트 (동시 중복)',
    });

    return { survey1, survey2 };
}

// ── 시나리오 1: 기본 원자성 검증 (순차) ──
export function basicAtomicityTest(data) {
    if (!data || !data.survey1) return;

    const { surveyId, sectionId } = data.survey1;
    const visitorId = `duplicate-visitor-${Date.now()}`;

    group('기본 원자성 검증', () => {
        // 첫 번째 응답 → 성공
        const firstRes = submitResponse(surveyId, sectionId, visitorId);
        check(firstRes, {
            '첫 번째 응답 성공 (200)': (r) => r.status === 200,
            'participantId 반환': (r) => r.json().participantId !== undefined,
        });

        if (firstRes.status !== 200) {
            console.error(`첫 번째 응답 실패: status=${firstRes.status}`);
            return;
        }

        sleep(0.5);

        // 두 번째 응답 (동일 visitorId) → 실패
        const secondRes = submitResponse(surveyId, sectionId, visitorId);
        check(secondRes, {
            '중복 응답 실패 (4xx)': (r) => r.status >= 400 && r.status < 500,
        });

        if (secondRes.status >= 400) {
            console.log('원자성 검증 통과: 중복 visitorId 응답 차단됨');
        } else {
            console.error(`원자성 위반! 중복 응답이 성공함: status=${secondRes.status}`);
        }
    });
}

// ── 시나리오 2: 동일 visitorId 동시 제출 ──
// 기대: VU 중 1개만 성공, 나머지 실패
export function concurrentDuplicateTest(data) {
    if (!data || !data.survey2) return;

    const { surveyId, sectionId } = data.survey2;
    // 모든 VU가 같은 visitorId 사용
    const visitorId = 'concurrent-duplicate-test';

    group('동시 중복 응답 테스트', () => {
        const res = submitResponse(surveyId, sectionId, visitorId);

        if (res.status === 200) {
            duplicateSuccess.add(1);
            duplicateSuccessRate.add(true);
            check(res, {
                '응답 성공': (r) => r.status === 200,
            });
        } else {
            duplicateFail.add(1);
            duplicateSuccessRate.add(false);
            check(res, {
                '중복 응답 차단 (4xx)': (r) => r.status >= 400 && r.status < 500,
            });
        }
    });
}

// ── teardown: 결과 요약 ──
export function teardown(data) {
    console.log('=== Outbox 원자성 테스트 결과 ===');
    console.log('시나리오 2 (동시 중복): 1개만 성공해야 정상');
    console.log('duplicate_submit_success 카운터를 확인하세요 (기대값: 1)');
}
