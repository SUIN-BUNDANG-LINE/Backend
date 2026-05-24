/**
 * 추첨 Kafka Fan-out 부하 테스트
 *
 * 원본: DrawingKafkaIntegrationTest.kt
 *
 * 시나리오:
 *   1. N개 설문 생성 — 파티션 키 스큐 회피 (kafkaKey=surveyId, default partitioner=murmur2%6)
 *   2. PARTICIPANT_COUNT명이 N개 설문에 분산 응답 제출 → participantId 획득
 *   3. PARTICIPANT_COUNT명이 동시에 추첨 시도 (티켓 BOARD_SIZE장 → BOARD_SIZE명만 성공)
 *   4. Fan-out 검증: N개 설문 모두 자동 종료(CLOSED) 폴링
 *
 * 실행:
 *   k6 run drawing-kafka-fanout.js
 *
 * 환경변수 (선택):
 *   NUM_SURVEYS        — 생성할 설문 수 (기본: 20). 6 파티션 토픽에 ~85% 분산 확률.
 *   BOARD_SIZE         — 추첨판 총 크기 (기본: 100). per-survey = BOARD_SIZE/NUM_SURVEYS = 5
 *   WINNING_COUNT      — 당첨 총 수 (기본: 20). per-survey = WINNING_COUNT/NUM_SURVEYS = 1
 *   PARTICIPANT_COUNT  — 참여자 총 수 (기본: 200). per-survey = PARTICIPANT_COUNT/NUM_SURVEYS = 10
 */
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import {
    setupSurveyWithDrawing,
    submitResponse,
    doDrawing,
    waitForSurveyStatus,
} from './lib/helpers.js';

const NUM_SURVEYS = parseInt(__ENV.NUM_SURVEYS || '20');
const BOARD_SIZE = parseInt(__ENV.BOARD_SIZE || '100');
const WINNING_COUNT = parseInt(__ENV.WINNING_COUNT || '20');
const PARTICIPANT_COUNT = parseInt(__ENV.PARTICIPANT_COUNT || '200');

// per-survey 분배 — 총량을 NUM_SURVEYS로 균등 분할
const PER_BOARD = Math.floor(BOARD_SIZE / NUM_SURVEYS);
const PER_WINNING = Math.floor(WINNING_COUNT / NUM_SURVEYS);
const PER_PARTICIPANTS = Math.floor(PARTICIPANT_COUNT / NUM_SURVEYS);

if (PER_BOARD < PER_WINNING || PER_BOARD < 1 || PER_PARTICIPANTS < 1) {
    throw new Error(
        `설정 오류: NUM_SURVEYS=${NUM_SURVEYS} → per-survey board=${PER_BOARD}, ` +
            `winning=${PER_WINNING}, participants=${PER_PARTICIPANTS}. NUM_SURVEYS를 줄이세요.`,
    );
}

// ── 커스텀 메트릭 ──
const responseSuccess = new Counter('response_submit_success');
const drawingSuccess = new Counter('drawing_success_total');
const drawingWin = new Counter('drawing_win_total');
const drawingLose = new Counter('drawing_lose_total');
const drawingFail = new Counter('drawing_fail_total');
const drawingDuration = new Trend('drawing_req_duration');

export const options = {
    scenarios: {
        // Phase 1: 응답 제출 — setup에서 일괄 처리되므로 이 시나리오는 자리만 잡음
        submit_responses: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: PARTICIPANT_COUNT,
            maxDuration: '120s',
            exec: 'submitResponsePhase',
            tags: { phase: 'response' },
        },
        // Phase 2: 추첨 실행
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
            // NUM_SURVEYS개 설문 순차 폴링 — 최악 60s × NUM_SURVEYS 가정
            maxDuration: '600s',
            exec: 'verifyFanoutPhase',
            startTime: '250s',
            tags: { phase: 'verify' },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<5000'],
        // 총 티켓 = PER_BOARD × NUM_SURVEYS. 여유분 5 차감.
        drawing_success_total: [`count>=${PER_BOARD * NUM_SURVEYS - 5}`],
    },
};

// ── setup: N개 설문 생성 + participants 풀 구축 ──
export function setup() {
    const surveys = [];
    const allParticipants = [];

    for (let s = 0; s < NUM_SURVEYS; s++) {
        const survey = setupSurveyWithDrawing({
            // target-reached auto-close 경로 회피 — 충분히 크게 둠.
            // 검증 대상: drawing-completed → auto-close (추첨판 소진 시)
            targetParticipantCount: PER_PARTICIPANTS * 10,
            rewardCount: PER_WINNING,
            title: `k6 Fan-out #${s + 1}/${NUM_SURVEYS} (board=${PER_BOARD}, win=${PER_WINNING})`,
        });

        if (!survey) {
            console.error(`설문 #${s + 1} 셋업 실패 — ACCESS_TOKEN/secret 확인 필요`);
            return null;
        }
        surveys.push(survey);

        // 각 설문에 PER_PARTICIPANTS명 응답 제출
        for (let i = 0; i < PER_PARTICIPANTS; i++) {
            const visitorId = `fanout-${survey.surveyId}-${i}-${Date.now()}`;
            const res = submitResponse(survey.surveyId, survey.sectionId, visitorId);

            if (res.status === 200) {
                const body = res.json();
                const globalIdx = s * PER_PARTICIPANTS + i;
                allParticipants.push({
                    surveyId: survey.surveyId,
                    participantId: body.participantId,
                    selectedNumber: i % PER_BOARD,
                    phoneNumber: `010-9000-${String(globalIdx).padStart(4, '0')}`,
                });
                responseSuccess.add(1);
            } else {
                console.warn(`응답 제출 실패 [survey=${s + 1}, idx=${i}]: status=${res.status}`);
            }
        }
    }

    console.log(
        `셋업 완료: ${surveys.length}개 설문, ${allParticipants.length}명 participantId 확보 ` +
            `(per-survey: board=${PER_BOARD}, win=${PER_WINNING}, participants=${PER_PARTICIPANTS})`,
    );
    return { surveys, participants: allParticipants };
}

// ── Phase 1: setup에서 처리하므로 추가 작업 없음 ──
export function submitResponsePhase(data) {
    if (!data) return;
    sleep(0.1);
}

// ── Phase 2: 추첨 실행 ──
export function drawingPhase(data) {
    if (!data || !data.participants) return;

    const idx = __ITER % data.participants.length;
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

// ── Phase 3: Fan-out 검증 (N개 설문 모두 자동 종료 확인) ──
export function verifyFanoutPhase(data) {
    if (!data || !data.surveys) return;

    group('Fan-out 검증: N개 설문 자동 종료', () => {
        let closedCount = 0;
        for (const survey of data.surveys) {
            // 설문당 60초까지 폴링 — drawing-auto-close consumer가 CLOSED 처리하는지 확인
            const closed = waitForSurveyStatus(survey.surveyId, 'CLOSED', 60);
            check(null, {
                [`설문 ${survey.surveyId.slice(0, 8)} 자동 종료`]: () => closed,
            });
            if (closed) closedCount += 1;
        }
        console.log(`Fan-out 검증 결과: ${closedCount}/${data.surveys.length}개 설문 CLOSED`);
    });
}
