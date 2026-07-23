/**
 * SMS 발송-비용 정합 사가 전용 부하/정합성 테스트.
 *
 * 실제 프로듀서(backend 추첨 API)로 당첨/비당첨을 대량 생성하고, 컨슈머 사가가
 * cost_record 를 불변식대로 수렴시키는지 xk6-sql 로 직접 검증한다. (사가엔 조회 REST 가 없다.)
 *
 * ── 검증 불변식 (배포 실패율과 무관하게 성립) ──
 *   INV1  당첨 수 == CONFIRMED + REVERSED        유실/중복 0 (FR-005 / SC-004: 정확히 1회)
 *   INV2  PENDING(final) == 0                    사가 수렴 완료
 *   INV3  cost_record 총행 == 당첨 수             비당첨은 행 없음 (FR-004)
 *   INV4  청구액 == SUM(amount WHERE CONFIRMED)   실제 발송 성공분만 과금
 *
 * ── 실패율 모드 (notification 컨슈머 SMS_MOCK_FAILURE_RATE) ──
 *   0.0 → 전건 CONFIRMED (수초 수렴)          EXPECT_MODE=confirmed
 *   1.0 → 전건 REVERSED  (재시도 소진 ~분)     EXPECT_MODE=reversed   (보상 경로)
 *   그 외/혼합 → EXPECT_MODE=any (불변식만)
 *
 * ── 실행 (xk6-sql 커스텀 바이너리 필요) ──
 *   ./k6-custom run \
 *     --env DB_DSN="user:password@tcp(localhost:13306)/test?parseTime=true" \
 *     --env NUM_SURVEYS=20 --env BOARD_SIZE=1000 --env WINNING_COUNT=200 --env PARTICIPANT_COUNT=1000 \
 *     --env EXPECT_MODE=confirmed --env SAGA_TIMEOUT_SEC=180 \
 *     saga-cost-integrity.js
 */
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';
import { setupSurveyWithDrawing, submitResponse, doDrawing } from '../../lib/helpers.js';
import { db } from '../../lib/db.js';

const NUM_SURVEYS = parseInt(__ENV.NUM_SURVEYS || '20');
const BOARD_SIZE = parseInt(__ENV.BOARD_SIZE || '1000');
const WINNING_COUNT = parseInt(__ENV.WINNING_COUNT || '200');
const PARTICIPANT_COUNT = parseInt(__ENV.PARTICIPANT_COUNT || '1000');
const EXPECT_MODE = (__ENV.EXPECT_MODE || 'any').toLowerCase(); // confirmed | reversed | any
const SAGA_TIMEOUT_SEC = parseInt(__ENV.SAGA_TIMEOUT_SEC || '180');
const UNIT_COST = parseFloat(__ENV.UNIT_COST || '8.00'); // SmsCostEventListener.SMS_UNIT_COST_KRW

const PER_BOARD = Math.floor(BOARD_SIZE / NUM_SURVEYS);
const PER_WINNING = Math.floor(WINNING_COUNT / NUM_SURVEYS);
const PER_PARTICIPANTS = Math.floor(PARTICIPANT_COUNT / NUM_SURVEYS);
if (PER_BOARD < PER_WINNING || PER_BOARD < 1 || PER_PARTICIPANTS < 1) {
    throw new Error(`설정 오류: NUM_SURVEYS=${NUM_SURVEYS} → board=${PER_BOARD}, win=${PER_WINNING}, part=${PER_PARTICIPANTS}`);
}
// fanout 은 추첨판을 전부 소진하므로 당첨 수 == 실제 발행된 winning 티켓 수.
const EXPECTED_WINNERS = PER_WINNING * NUM_SURVEYS;

const drawingWin = new Counter('drawing_win_total');
const drawingLose = new Counter('drawing_lose_total');
const drawingFail = new Counter('drawing_fail_total');
const sagaConfirmed = new Counter('saga_confirmed_final');
const sagaReversed = new Counter('saga_reversed_final');
const sagaViolations = new Counter('saga_invariant_violations');
const sagaConvergeMs = new Trend('saga_converge_ms');

export const options = {
    scenarios: {
        do_drawings: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: PARTICIPANT_COUNT,
            maxDuration: '180s',
            exec: 'drawingPhase',
        },
    },
    teardownTimeout: `${SAGA_TIMEOUT_SEC + 30}s`,
    thresholds: {
        http_req_duration: ['p(95)<5000'],
        // 핵심 게이트: 사가 불변식 위반이 단 1건도 없어야 한다.
        saga_invariant_violations: ['count==0'],
    },
};

// xk6-sql-driver-mysql 은 VARCHAR/ENUM/DECIMAL 을 byte 배열(number[])로 반환한다 → 문자열 복원.
function toStr(v) {
    if (v == null) return '';
    if (Array.isArray(v)) return v.map((c) => String.fromCharCode(c)).join('');
    return String(v);
}
const num = (v) => Number(toStr(v)); // byte배열이든 숫자든 안전하게 수치화

// cost_record 상태별 집계 (xk6-sql).
function costStats() {
    const rows = db.query('SELECT state, COUNT(*) AS cnt, COALESCE(SUM(amount),0) AS krw FROM cost_record GROUP BY state');
    const m = { PENDING: 0, CONFIRMED: 0, REVERSED: 0, krwConfirmed: 0, total: 0 };
    for (const r of rows) {
        const s = toStr(r.state);
        const cnt = num(r.cnt);
        m[s] = cnt;
        m.total += cnt;
        if (s === 'CONFIRMED') m.krwConfirmed = num(r.krw);
    }
    return m;
}

export function setup() {
    // 이 실행분만 정확히 셈하기 위해 시작 시 초기화.
    db.exec('TRUNCATE cost_record');

    const participants = [];
    for (let s = 0; s < NUM_SURVEYS; s++) {
        const survey = setupSurveyWithDrawing({
            targetParticipantCount: PER_BOARD,
            rewardCount: PER_WINNING,
            title: `k6 saga-cost #${s + 1}/${NUM_SURVEYS}`,
        });
        if (!survey) {
            console.error(`설문 #${s + 1} 셋업 실패 — ACCESS_TOKEN/secret 확인`);
            return null;
        }
        for (let i = 0; i < PER_PARTICIPANTS; i++) {
            const visitorId = `saga-${survey.surveyId}-${i}-${Date.now()}`;
            const res = submitResponse(survey.surveyId, survey.sectionId, visitorId);
            if (res.status === 200) {
                const globalIdx = s * PER_PARTICIPANTS + i;
                participants.push({
                    participantId: res.json().participantId,
                    selectedNumber: i % PER_BOARD,
                    phoneNumber: `010-9100-${String(globalIdx).padStart(4, '0')}`,
                });
            }
        }
    }
    console.log(`셋업 완료: ${NUM_SURVEYS}개 설문, ${participants.length}명, 기대 당첨=${EXPECTED_WINNERS}, mode=${EXPECT_MODE}`);
    return { participants };
}

export function drawingPhase(data) {
    if (!data || !data.participants) return;
    const p = data.participants[exec.scenario.iterationInTest % data.participants.length];
    group('추첨', () => {
        const res = doDrawing(p.participantId, p.selectedNumber, p.phoneNumber);
        if (res.status === 200) {
            res.json().isWon ? drawingWin.add(1) : drawingLose.add(1);
        } else {
            drawingFail.add(1);
        }
    });
}

// teardown: 사가 수렴을 폴링한 뒤 불변식 4종을 검증한다.
export function teardown(data) {
    if (!data) return;
    const start = Date.now();
    const deadline = start + SAGA_TIMEOUT_SEC * 1000;
    let st = costStats();
    // 수렴 조건: 당첨 전건이 종결(CONFIRMED|REVERSED)되고 PENDING 이 사라질 때까지.
    while (Date.now() < deadline) {
        st = costStats();
        if (st.PENDING === 0 && st.CONFIRMED + st.REVERSED >= EXPECTED_WINNERS) break;
        sleep(3);
    }
    const convergeMs = Date.now() - start;
    sagaConvergeMs.add(convergeMs);
    sagaConfirmed.add(st.CONFIRMED);
    sagaReversed.add(st.REVERSED);

    const settled = st.CONFIRMED + st.REVERSED;
    const billable = st.krwConfirmed;
    const expectedBillable = st.CONFIRMED * UNIT_COST;

    console.log(
        `사가 수렴(${convergeMs}ms): CONFIRMED=${st.CONFIRMED} REVERSED=${st.REVERSED} ` +
            `PENDING=${st.PENDING} 총행=${st.total} 청구액=${billable}원 (기대 당첨=${EXPECTED_WINNERS}, mode=${EXPECT_MODE})`,
    );

    const bump = (name, ok) => { if (!ok) { sagaViolations.add(1); console.error(`INV 위반: ${name}`); } };
    // INV1 유실/중복 0
    bump('INV1 당첨==CONFIRMED+REVERSED', settled === EXPECTED_WINNERS);
    // INV2 수렴 완료
    bump('INV2 PENDING==0', st.PENDING === 0);
    // INV3 비당첨 행 없음 (총행이 당첨 수와 정확히 일치 → 잉여행 없음)
    bump('INV3 총행==당첨수', st.total === EXPECTED_WINNERS);
    // INV4 청구액 = CONFIRMED×단가
    bump('INV4 청구액==CONFIRMED×단가', Math.abs(billable - expectedBillable) < 0.01);
    // EXPECT_MODE 별 추가 단언
    if (EXPECT_MODE === 'confirmed') bump('MODE confirmed: REVERSED==0', st.REVERSED === 0);
    if (EXPECT_MODE === 'reversed') bump('MODE reversed: CONFIRMED==0', st.CONFIRMED === 0);

    check(st, {
        '사가 전건 수렴(유실 0)': () => settled === EXPECTED_WINNERS,
        'PENDING 잔여 0': () => st.PENDING === 0,
        '잉여행 없음(비당첨 미과금)': () => st.total === EXPECTED_WINNERS,
        '청구액 = 확정건×단가': () => Math.abs(billable - expectedBillable) < 0.01,
    });
}
