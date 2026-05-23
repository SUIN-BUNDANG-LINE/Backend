/**
 * 추첨 동시성 부하 테스트
 *
 * 원본: DrawingBoardServiceConcurrencyTest.kt
 *
 * 시나리오:
 *   1. same_ticket   — 10명이 같은 티켓 번호에 동시 추첨 → 1명만 성공
 *   2. same_user     — 1명이 서로 다른 번호로 10번 동시 추첨 → 1번만 성공
 *   3. diff_tickets  — 10명이 각각 다른 번호로 동시 추첨 → 전부 성공
 *
 * 실행:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *          --env ACCESS_TOKEN=<jwt> \
 *          drawing-concurrency.js
 *
 * 환경변수 (선택):
 *   SURVEY_ID   — 미리 생성된 설문 ID (생략 시 setup에서 자동 생성)
 *   SECTION_ID  — 미리 생성된 섹션 ID
 */
import { check, group, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    setupSurveyWithDrawing,
    submitResponse,
    doDrawing,
    makeVisitorId,
    makePhoneNumber,
} from './lib/helpers.js';

// ── 커스텀 메트릭 ──
const drawingSuccess = new Counter('drawing_success_total');
const drawingFail = new Counter('drawing_fail_total');
const drawingSuccessRate = new Rate('drawing_success_rate');

export const options = {
    scenarios: {
        // 시나리오 1: 같은 티켓 번호에 10명 동시 요청 → 1명만 성공
        same_ticket: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 10,
            maxDuration: '60s',
            exec: 'sameTicketTest',
            tags: { scenario: 'same_ticket' },
        },
        // 시나리오 2: 동일 사용자가 다른 번호로 10번 동시 요청 → 1번만 성공
        same_user: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 10,
            maxDuration: '60s',
            exec: 'sameUserTest',
            startTime: '65s',
            tags: { scenario: 'same_user' },
        },
        // 시나리오 3: 10명이 각각 다른 번호로 동시 추첨 → 전부 성공
        diff_tickets: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 10,
            maxDuration: '60s',
            exec: 'differentTicketsTest',
            startTime: '130s',
            tags: { scenario: 'diff_tickets' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
    },
};

// ── setup: 시나리오별 설문 생성 ──
export function setup() {
    const presetSurveyId = __ENV.SURVEY_ID;
    const presetSectionId = __ENV.SECTION_ID;

    if (presetSurveyId && presetSectionId) {
        console.log(`사전 설정된 설문 사용: surveyId=${presetSurveyId}`);
    }

    // 시나리오 1용 설문 (boardSize=100, reward=50 → 티켓 100장)
    const survey1 = presetSurveyId
        ? { surveyId: presetSurveyId, sectionId: presetSectionId }
        : setupSurveyWithDrawing({ targetParticipantCount: 100, rewardCount: 50, title: 'k6 같은 티켓 테스트' });

    // 시나리오 2용 설문
    const survey2 = setupSurveyWithDrawing({ targetParticipantCount: 100, rewardCount: 50, title: 'k6 동일 사용자 테스트' });

    // 시나리오 3용 설문
    const survey3 = setupSurveyWithDrawing({ targetParticipantCount: 100, rewardCount: 50, title: 'k6 다른 번호 테스트' });

    // 시나리오 1: 10명의 participantId 미리 생성
    const s1Participants = [];
    for (let i = 0; i < 10; i++) {
        const visitorId = `same-ticket-vu-${i}-${Date.now()}`;
        const res = submitResponse(survey1.surveyId, survey1.sectionId, visitorId);
        if (res.status === 200) {
            const body = res.json();
            s1Participants.push({
                participantId: body.participantId,
                phoneNumber: `010-1234-${String(i).padStart(4, '0')}`,
            });
        }
    }

    // 시나리오 2: 1명의 participantId 생성
    const s2Res = submitResponse(survey2.surveyId, survey2.sectionId, `same-user-${Date.now()}`);
    const s2Participant = s2Res.status === 200 ? s2Res.json() : null;

    // 시나리오 3: 10명의 participantId 미리 생성
    const s3Participants = [];
    for (let i = 0; i < 10; i++) {
        const visitorId = `diff-ticket-vu-${i}-${Date.now()}`;
        const res = submitResponse(survey3.surveyId, survey3.sectionId, visitorId);
        if (res.status === 200) {
            const body = res.json();
            s3Participants.push({
                participantId: body.participantId,
                phoneNumber: `010-5678-${String(i).padStart(4, '0')}`,
                selectedNumber: i, // 각각 다른 번호
            });
        }
    }

    return {
        scenario1: { surveyId: survey1.surveyId, participants: s1Participants, targetNumber: 5 },
        scenario2: { surveyId: survey2.surveyId, participant: s2Participant },
        scenario3: { surveyId: survey3.surveyId, participants: s3Participants },
    };
}

// ── 시나리오 1: 같은 티켓 번호에 10명 동시 요청 ──
// 기대: 1명만 성공, 나머지 AlreadySelectedTicketException (409 또는 400)
export function sameTicketTest(data) {
    const { participants, targetNumber } = data.scenario1;
    const vuIndex = __VU - 1;

    if (vuIndex >= participants.length) return;

    const p = participants[vuIndex];
    group('같은 티켓 동시 추첨', () => {
        const res = doDrawing(p.participantId, targetNumber, p.phoneNumber);

        if (res.status === 200) {
            drawingSuccess.add(1);
            drawingSuccessRate.add(true);
            check(res, { '추첨 성공 응답': (r) => r.json().isWon !== undefined });
        } else {
            drawingFail.add(1);
            drawingSuccessRate.add(false);
            check(res, {
                '이미 선택된 티켓 에러': (r) => r.status === 400 || r.status === 409,
            });
        }
    });
}

// ── 시나리오 2: 동일 사용자 동시 요청 ──
// 기대: 1번만 성공, 나머지 AlreadyParticipatedDrawingException
export function sameUserTest(data) {
    const { participant } = data.scenario2;
    if (!participant) return;

    const vuIndex = __VU - 1;

    group('동일 사용자 동시 추첨', () => {
        const selectedNumber = vuIndex; // 각 VU가 다른 번호 선택
        const res = doDrawing(participant.participantId, selectedNumber, '010-1234-5678');

        if (res.status === 200) {
            drawingSuccess.add(1);
            drawingSuccessRate.add(true);
        } else {
            drawingFail.add(1);
            drawingSuccessRate.add(false);
            check(res, {
                '이미 참여한 추첨 에러': (r) => r.status === 400 || r.status === 409,
            });
        }
    });
}

// ── 시나리오 3: 여러 사용자 다른 번호 동시 추첨 ──
// 기대: 전부 성공
export function differentTicketsTest(data) {
    const { participants } = data.scenario3;
    const vuIndex = __VU - 1;

    if (vuIndex >= participants.length) return;

    const p = participants[vuIndex];
    group('다른 번호 동시 추첨', () => {
        const res = doDrawing(p.participantId, p.selectedNumber, p.phoneNumber);

        check(res, {
            '추첨 성공': (r) => r.status === 200,
            '결과 포함': (r) => r.json().isWon !== undefined,
        });

        if (res.status === 200) {
            drawingSuccess.add(1);
            drawingSuccessRate.add(true);
        } else {
            drawingFail.add(1);
            drawingSuccessRate.add(false);
            console.error(`다른 번호 추첨 실패: VU=${__VU}, status=${res.status}, body=${res.body}`);
        }
    });
}
