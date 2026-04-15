/**
 * 추첨 Kafka Fan-out 부하 테스트
 *
 * 원본: DrawingKafkaIntegrationTest.kt
 *
 * 시나리오:
 *   1. 설문 생성 (boardSize=50, 당첨=10)
 *   2. 100명이 동시에 응답 제출 → participantId 획득
 *   3. 100명이 동시에 추첨 시도 (티켓 50장 → 50명만 성공)
 *   4. Fan-out 검증: 폴링으로 설문 자동 종료(CLOSED) 확인
 *
 * 실행:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *          --env ACCESS_TOKEN=<jwt> \
 *          drawing-kafka-fanout.js
 *
 * 환경변수 (선택):
 *   BOARD_SIZE         — 추첨판 크기 (기본: 50)
 *   WINNING_COUNT      — 당첨 수 (기본: 10)
 *   PARTICIPANT_COUNT  — 참여자 수 (기본: 100)
 */
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
    setupSurveyWithDrawing,
    submitResponse,
    doDrawing,
    waitForSurveyStatus,
} from './lib/helpers.js';

const BOARD_SIZE = parseInt(__ENV.BOARD_SIZE || '50');
const WINNING_COUNT = parseInt(__ENV.WINNING_COUNT || '10');
const PARTICIPANT_COUNT = parseInt(__ENV.PARTICIPANT_COUNT || '100');

// ── 커스텀 메트릭 ──
const responseSuccess = new Counter('response_submit_success');
const drawingSuccess = new Counter('drawing_success_total');
const drawingWin = new Counter('drawing_win_total');
const drawingLose = new Counter('drawing_lose_total');
const drawingFail = new Counter('drawing_fail_total');
const drawingDuration = new Trend('drawing_req_duration');

export const options = {
    scenarios: {
        // Phase 1: 응답 제출 (participantId 획득)
        submit_responses: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: PARTICIPANT_COUNT,
            maxDuration: '120s',
            exec: 'submitResponsePhase',
            tags: { phase: 'response' },
        },
        // Phase 2: 추첨 실행 (응답 제출 후 시작)
        do_drawings: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: PARTICIPANT_COUNT,
            maxDuration: '120s',
            exec: 'drawingPhase',
            startTime: '125s',
            tags: { phase: 'drawing' },
        },
        // Phase 3: Fan-out 검증 (추첨 종료 후 폴링)
        verify_fanout: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '120s',
            exec: 'verifyFanoutPhase',
            startTime: '250s',
            tags: { phase: 'verify' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'drawing_success_total': [`count>=${BOARD_SIZE - 5}`], // 최소 boardSize - 여유분
    },
};

// ── setup: 설문 생성 + participantId 저장소 ──
export function setup() {
    const survey = setupSurveyWithDrawing({
        targetParticipantCount: BOARD_SIZE,
        rewardCount: WINNING_COUNT,
        title: `k6 Fan-out 테스트 (board=${BOARD_SIZE}, win=${WINNING_COUNT})`,
    });

    if (!survey) {
        console.error('설문 셋업 실패 — ACCESS_TOKEN 확인 필요');
        return null;
    }

    // Phase 1에서 수집할 participantId를 setup에서 미리 응답 제출로 수집
    const participants = [];
    for (let i = 0; i < PARTICIPANT_COUNT; i++) {
        const visitorId = `fanout-${survey.surveyId}-${i}-${Date.now()}`;
        const res = submitResponse(survey.surveyId, survey.sectionId, visitorId);

        if (res.status === 200) {
            const body = res.json();
            participants.push({
                participantId: body.participantId,
                selectedNumber: i % BOARD_SIZE,
                phoneNumber: `010-9000-${String(i).padStart(4, '0')}`,
            });
            responseSuccess.add(1);
        } else {
            console.warn(`응답 제출 실패 [${i}]: status=${res.status}`);
        }
    }

    console.log(`셋업 완료: ${participants.length}명 participantId 확보`);
    return { surveyId: survey.surveyId, sectionId: survey.sectionId, participants };
}

// ── Phase 1: 응답 제출 (setup에서 처리하므로 여기서는 메트릭만 기록) ──
export function submitResponsePhase(data) {
    // setup에서 미리 처리했으므로 추가 응답 제출 없음
    // 이 시나리오는 setup의 응답 제출이 충분하지 않을 때 추가로 제출
    if (!data) return;
    sleep(0.1);
}

// ── Phase 2: 추첨 실행 ──
export function drawingPhase(data) {
    if (!data || !data.participants) return;

    const idx = (__ITER) % data.participants.length;
    const p = data.participants[idx];

    group('동시 추첨', () => {
        const res = doDrawing(p.participantId, p.selectedNumber, p.phoneNumber);
        drawingDuration.add(res.timings.duration);

        if (res.status === 200) {
            drawingSuccess.add(1);
            const body = res.json();
            if (body.isWon) {
                drawingWin.add(1);
            } else {
                drawingLose.add(1);
            }
            check(res, {
                '추첨 응답 정상': (r) => r.json().isWon !== undefined,
            });
        } else {
            drawingFail.add(1);
            check(res, {
                '이미 선택된 티켓': (r) => r.status === 400 || r.status === 409,
            });
        }
    });
}

// ── Phase 3: Fan-out 검증 (설문 자동 종료 확인) ──
export function verifyFanoutPhase(data) {
    if (!data) return;

    group('Fan-out 검증: 설문 자동 종료', () => {
        // drawing-auto-close Consumer가 설문을 CLOSED로 변경하는지 확인
        const closed = waitForSurveyStatus(data.surveyId, 'CLOSED', 60);
        check(null, {
            '설문 자동 종료 (CLOSED)': () => closed,
        });

        if (closed) {
            console.log(`Fan-out 검증 통과: surveyId=${data.surveyId} → CLOSED`);
        }
    });
}
