/**
 * 추첨 Kafka Fan-out 부하 테스트
 *
 * 원본: DrawingKafkaIntegrationTest.kt
 *
 * 도메인 제약:
 *   서버의 추첨판 크기(boardSize) = rewardSetting.targetParticipantCount (1:1 결합).
 *   IMMEDIATE_DRAW 설문은 추첨판이 모두 소진되어야 종료되며, target 도달은 종료 트리거가 아니다.
 *
 * 시나리오:
 *   1. setup: N개 설문 생성 + PARTICIPANT_COUNT명 응답 제출 → participantId 풀 구축
 *   2. drawingPhase: PARTICIPANT_COUNT명이 동시에 추첨 시도 (티켓 PER_BOARD장 → 모두 성공)
 *      — 추첨판 소진 → drawing-completed → drawing-auto-close consumer가 설문 종료
 *   3. teardown: drawingPhase 종료 즉시 Fan-out 검증
 *      — N개 설문을 라운드로빈 폴링 (전체 대기시간 ≈ 가장 느린 단일 설문 close 지연)
 *
 * 실행:
 *   k6 run drawing-kafka-fanout.js
 *
 * 환경변수 (선택):
 *   NUM_SURVEYS        — 생성할 설문 수 (기본: 20). 6 파티션 토픽에 ~85% 분산 확률.
 *   BOARD_SIZE         — 추첨판 총 크기 (기본: 100). per-survey = BOARD_SIZE/NUM_SURVEYS = 5
 *   WINNING_COUNT      — 당첨 총 수 (기본: 20). per-survey = WINNING_COUNT/NUM_SURVEYS = 1
 *   PARTICIPANT_COUNT  — 참여자 총 수 (기본: 100, = BOARD_SIZE). per-survey = 5
 *   FANOUT_TIMEOUT_SEC — Fan-out 검증 폴링 타임아웃 (기본: 120)
 */
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import {
    setupSurveyWithDrawing,
    submitResponse,
    doDrawing,
    getSurveyInfo,
} from '../../lib/helpers.js';
import { startAnnotation, endAnnotation } from '../../lib/annotations.js';

const NUM_SURVEYS = parseInt(__ENV.NUM_SURVEYS || '20');
const BOARD_SIZE = parseInt(__ENV.BOARD_SIZE || '100');
const WINNING_COUNT = parseInt(__ENV.WINNING_COUNT || '20');
const PARTICIPANT_COUNT = parseInt(__ENV.PARTICIPANT_COUNT || '100');
const FANOUT_TIMEOUT_SEC = parseInt(__ENV.FANOUT_TIMEOUT_SEC || '120');

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
const fanoutClosed = new Counter('fanout_closed_total');
const fanoutCloseLatency = new Trend('fanout_close_latency_ms');

export const options = {
    scenarios: {
        // 추첨 실행 — setup 완료 직후 시작 (응답 제출은 setup에서 동기 처리)
        do_drawings: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: PARTICIPANT_COUNT,
            maxDuration: '120s',
            exec: 'drawingPhase',
            tags: { phase: 'drawing' },
        },
    },
    // Fan-out 검증은 teardown에서 폴링 — scenarios 종료 즉시 실행
    teardownTimeout: `${FANOUT_TIMEOUT_SEC + 30}s`,
    thresholds: {
        http_req_duration: ['p(95)<5000'],
        // 총 티켓 = PER_BOARD × NUM_SURVEYS. 여유분 5 차감.
        drawing_success_total: [`count>=${PER_BOARD * NUM_SURVEYS - 5}`],
        // Fan-out: N개 설문 전부 자동 종료 기대 (여유분 1)
        fanout_closed_total: [`count>=${NUM_SURVEYS - 1}`],
    },
};

// ── setup: N개 설문 생성 + participants 풀 구축 ──
export function setup() {
    const surveys = [];
    const allParticipants = [];

    for (let s = 0; s < NUM_SURVEYS; s++) {
        const survey = setupSurveyWithDrawing({
            // 도메인 규칙: boardSize = targetParticipantCount (SurveyWorkbenchService 참고).
            // PER_BOARD 로 맞춰 추첨판이 PER_PARTICIPANTS 명 응답/추첨으로 정확히 소진되게 한다.
            // 부수 효과: target-reached 경로도 동시 발화 — 멱등 close 검증.
            targetParticipantCount: PER_BOARD,
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
    const annotationId = startAnnotation('drawing-kafka-fanout', ['kafka', 'fanout']);

    // teardown 폴링용 기준 시각 — 추첨 마감 → close까지 fan-out latency 측정
    return { surveys, participants: allParticipants, annotationId };
}

// ── 추첨 실행 ──
export function drawingPhase(data) {
    if (!data || !data.participants) return;

    // shared-iterations에서 __ITER는 VU 로컬 카운터라 0..(iter/vus)에 갇혀 일부 참가자만 반복 호출됨.
    // 시나리오 전역 유니크 카운터로 100명 전체에 1:1 매핑.
    const idx = exec.scenario.iterationInTest % data.participants.length;
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

// ── teardown: drawingPhase 종료 즉시 Fan-out 폴링 ──
// 모든 설문을 라운드로빈으로 동시 폴링 → 전체 대기시간 = max(close latency), not sum.
export function teardown(data) {
    if (!data || !data.surveys) return;

    const pending = new Map(data.surveys.map((s) => [s.surveyId, s]));
    // 각 surveyId의 마지막 폴링 응답 — timeout 시 "실제 무슨 status가 왔는지" 진단용
    const lastSeen = new Map();
    const pollStart = Date.now();
    const deadline = pollStart + FANOUT_TIMEOUT_SEC * 1000;

    console.log(
        `Fan-out 폴링 시작: ${pending.size}개 설문 CLOSED 감지 (timeout=${FANOUT_TIMEOUT_SEC}s)`,
    );

    while (pending.size > 0 && Date.now() < deadline) {
        for (const surveyId of [...pending.keys()]) {
            const res = getSurveyInfo(surveyId);
            const status = res.status === 200 ? res.json().status : `HTTP_${res.status}`;
            lastSeen.set(surveyId, status);
            if (status === 'CLOSED') {
                fanoutCloseLatency.add(Date.now() - pollStart);
                fanoutClosed.add(1);
                pending.delete(surveyId);
            }
        }
        if (pending.size > 0) sleep(1);
    }

    const closedCount = data.surveys.length - pending.size;
    for (const survey of data.surveys) {
        check(null, {
            [`설문 ${survey.surveyId.slice(0, 8)} 자동 종료`]: () => !pending.has(survey.surveyId),
        });
    }
    if (pending.size > 0) {
        // 마지막으로 endpoint가 돌려준 status를 함께 출력 — 진짜 CLOSED인데 못 잡았는지 vs 정말 close 안 됐는지 구분
        const pendingDetail = [...pending.keys()]
            .map((sid) => `${sid.slice(0, 8)}=${lastSeen.get(sid) ?? 'NO_RESPONSE'}`)
            .join(', ');
        console.warn(
            `Fan-out 미완료: ${pending.size}개 설문이 ${FANOUT_TIMEOUT_SEC}s 내 CLOSED 안 됨 → ${pendingDetail}`,
        );
    }
    console.log(
        `Fan-out 검증 결과: ${closedCount}/${data.surveys.length}개 설문 CLOSED ` +
            `(소요 ${Math.round((Date.now() - pollStart) / 1000)}s)`,
    );

    endAnnotation(data?.annotationId);
}
