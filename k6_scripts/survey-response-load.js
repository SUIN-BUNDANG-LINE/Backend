/**
 * 설문 응답 제출 부하 테스트
 *
 * 원본: SurveyResponseKafkaIntegrationTest.kt
 *
 * 시나리오:
 *   1. 설문 생성 (targetParticipantCount=50, boardSize=200)
 *   2. 100명이 동시에 응답 제출
 *   3. Kafka Fan-out 검증:
 *      - auto-close Consumer → targetParticipantCount 도달 시 설문 CLOSED
 *      - Outbox 전량 PUBLISHED (HTTP로는 직접 검증 불가 → 설문 상태로 간접 검증)
 *
 * 실행:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *          --env ACCESS_TOKEN=<jwt> \
 *          survey-response-load.js
 *
 * 환경변수 (선택):
 *   TARGET_COUNT   — 자동 종료 목표 참여자 수 (기본: 50)
 *   SUBMISSION_COUNT — 총 제출 수 (기본: 100)
 *   VUS            — 동시 VU 수 (기본: 20)
 */
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { setupSurveyWithDrawing, submitResponse, waitForSurveyStatus } from './lib/helpers.js';

const TARGET_COUNT = parseInt(__ENV.TARGET_COUNT || '50');
const SUBMISSION_COUNT = parseInt(__ENV.SUBMISSION_COUNT || '100');
const VUS = parseInt(__ENV.VUS || '20');

// ── 커스텀 메트릭 ──
const submitSuccess = new Counter('submit_success_total');
const submitFail = new Counter('submit_fail_total');
const submitSuccessRate = new Rate('submit_success_rate');
const submitDuration = new Trend('submit_req_duration');

export const options = {
    scenarios: {
        // Phase 1: 동시 응답 제출
        concurrent_submit: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: SUBMISSION_COUNT,
            maxDuration: '120s',
            exec: 'concurrentSubmit',
            tags: { phase: 'submit' },
        },
        // Phase 2: auto-close 검증
        verify_auto_close: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '120s',
            exec: 'verifyAutoClose',
            startTime: '125s',
            tags: { phase: 'verify' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<3000'],
        'submit_success_rate': ['rate>0.5'], // 최소 50% 성공 (auto-close 이후 일부 실패 가능)
    },
};

// ── setup ──
export function setup() {
    const survey = setupSurveyWithDrawing({
        targetParticipantCount: TARGET_COUNT,
        rewardCount: 10,
        title: `k6 응답 제출 테스트 (target=${TARGET_COUNT})`,
    });

    if (!survey) {
        console.error('설문 셋업 실패');
        return null;
    }

    return { surveyId: survey.surveyId, sectionId: survey.sectionId };
}

// ── Phase 1: 동시 응답 제출 ──
export function concurrentSubmit(data) {
    if (!data) return;

    const visitorId = `k6-response-vu${__VU}-iter${__ITER}-${Date.now()}`;

    group('동시 응답 제출', () => {
        const res = submitResponse(data.surveyId, data.sectionId, visitorId);
        submitDuration.add(res.timings.duration);

        if (res.status === 200) {
            submitSuccess.add(1);
            submitSuccessRate.add(true);
            check(res, {
                '응답 성공': (r) => r.status === 200,
                'participantId 반환': (r) => r.json().participantId !== undefined,
                'isImmediateDraw 포함': (r) => r.json().isImmediateDraw !== undefined,
            });
        } else {
            submitFail.add(1);
            submitSuccessRate.add(false);
            // auto-close 이후 제출 시 SurveyClosedException 발생 가능
            check(res, {
                '설문 종료 에러 (예상됨)': (r) => r.status === 400 || r.status === 403,
            });
        }
    });
}

// ── Phase 2: auto-close 검증 ──
export function verifyAutoClose(data) {
    if (!data) return;

    group('auto-close 검증', () => {
        // auto-close Consumer가 targetParticipantCount 도달 시 설문을 CLOSED로 변경
        const closed = waitForSurveyStatus(data.surveyId, 'CLOSED', 60);
        check(null, {
            '설문 자동 종료 (CLOSED)': () => closed,
        });

        if (closed) {
            console.log(`auto-close 검증 통과: surveyId=${data.surveyId} → CLOSED`);
        } else {
            console.error(`auto-close 실패: surveyId=${data.surveyId}가 CLOSED되지 않음`);
        }
    });
}
