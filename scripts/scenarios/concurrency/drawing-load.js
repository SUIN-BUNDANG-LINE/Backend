/**
 * 추첨 지속 부하 테스트 (constant-arrival-rate)
 *
 * burst(10 동시)가 아니라 초당 RATE건을 DURATION 동안 지속 주입해 실제 TPS 하에서
 * 분산락 ON/OFF 의 데드락·성공률·p95 를 측정한다.
 *
 * 실행 (기본: RATE=10, DURATION_S=30 → 300 draws):
 *   LOCK_MODE=off k6 run --env ACCESS_TOKEN=$T drawing-load.js
 *   LOCK_MODE=on  k6 run --env ACCESS_TOKEN=$T drawing-load.js
 *   부하 조정: --env RATE=20 --env DURATION_S=60
 *
 * 참가자 풀 = RATE * DURATION_S (draw 는 참가자당 1회) → setup 에서 미리 생성.
 * 티켓 소진 자동 종료를 피하려고 rewardCount = 풀 크기(모든 draw 가 당첨).
 */
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';
import { setupSurveyWithDrawing, submitResponse, doDrawing, doDrawingBatch } from '../../lib/helpers.js';

const drawSuccess = new Counter('drawing_success_total');
const drawFail = new Counter('drawing_fail_total');

const LOCK_MODE = __ENV.LOCK_MODE || 'on'; // on | off | optimistic
const RATE = Number(__ENV.RATE || 10);
const DURATION_S = Number(__ENV.DURATION_S || 30);
const POOL = RATE * DURATION_S;
// 버스트 폭 — 한 번에 **동시에** 내보낼 요청 수.
// 균등 간격(constant-arrival-rate)은 처리 시간(≈10ms)보다 간격(100ms)이 길면 요청이 겹치지 않아
// 경합이 아예 생기지 않는다. 기본을 버스트로 두어 실제로 줄이 서게 한다 — 평균 TPS 는 같다.
const BURST = __ENV.BURST ? Number(__ENV.BURST) : 10;
// 버스트 모드에서는 반복 1회가 BURST 건을 담당하므로 발사 횟수를 그만큼 줄인다
const ARRIVAL_RATE = BURST > 0 ? Math.max(1, Math.round(RATE / BURST)) : RATE;
const TOTAL_BURSTS = DURATION_S * ARRIVAL_RATE;
// 경합 폭 — 버스트들이 돌려 쓸 티켓 수.
//   기본(= 버스트 수) → 버스트마다 **자기 칸**을 갖고, 그 안의 BURST 건이 같은 칸을 두고 다툰다.
//                       매 버스트가 독립적인 경쟁이므로 표본이 버스트 수만큼 쌓인다.
//   TICKETS=0        → 버스트 안의 요청들이 **각자 다른 칸**. 논리적 충돌이 없으므로, 경합 단위를
//                       실제보다 크게 잡은 전략(보드 낙관락·설문 단위 락)만 손해를 보는 조건이다.
//   TICKETS=1        → 모든 버스트가 같은 칸. 첫 버스트에서 승부가 끝나 표본이 하나로 쪼그라든다.
const TICKETS = __ENV.TICKETS !== undefined && __ENV.TICKETS !== '' ? Number(__ENV.TICKETS) : TOTAL_BURSTS;

export const options = {
    scenarios: {
        draw_load: {
            executor: 'constant-arrival-rate',
            rate: ARRIVAL_RATE,
            timeUnit: '1s',
            duration: `${DURATION_S}s`,
            // VU 는 발사 속도와 무관한 일꾼 풀 (k6 필수 필드라 생략 불가).
            // 넉넉히 고정: 응답이 rate×20초까지 밀려도 10 TPS 유지, 그 이상은 드롭으로 기록.
            preAllocatedVUs: RATE * 20,
            exec: 'drawLoad',
            tags: { scenario: 'draw_load', lock_mode: LOCK_MODE },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<5000'],
    },
};

export function setup() {
    // 티켓(리워드) >= draw 수 → 소진 자동 종료 없음. 단 reward < target 이어야 응답 등록이 되므로
    // 보드는 넉넉히(2×), 리워드는 풀 크기로 둔다.
    const survey = setupSurveyWithDrawing({
        targetParticipantCount: POOL * 2,
        rewardCount: POOL,
        title: `k6 부하 ${RATE}tps ${LOCK_MODE}`,
    });

    const participants = [];
    for (let i = 0; i < POOL; i++) {
        const res = submitResponse(survey.surveyId, survey.sectionId, `load-${i}-${Date.now()}`);
        if (res.status === 200) participants.push(res.json().participantId);
    }
    console.log(`참가자 ${participants.length}/${POOL} 생성 (surveyId=${survey.surveyId})`);
    return { participants };
}

// 참가자마다 고유해야 한다 — 전화번호가 겹치면 DR0005("이미 참여한 추첨")로 막힌다.
function phoneOf(i) {
    return `010-${String(Math.floor(i / 10000)).padStart(4, '0')}-${String(i % 10000).padStart(4, '0')}`;
}
function record(res) {
    if (res.status === 200) {
        drawSuccess.add(1);
        check(res, { '추첨 성공(200)': (r) => r.json().isWon !== undefined });
    } else {
        drawFail.add(1);
        check(res, { '실패 상태코드 기록': () => true });
    }
}

export function drawLoad(data) {
    const idx = exec.scenario.iterationInTest;
    const { participants } = data;

    if (BURST > 0) {
        // 이 반복이 담당할 BURST 건을 한 번에 내보낸다.
        // TICKETS=0  → 버스트 안의 10건이 **각자 다른 칸**을 고른다. 논리적 충돌이 없으므로
        //              티켓 단위로 판정하는 방식은 전부 통과시켜야 한다. 보드 단위로 판정하는
        //              낙관락만 가짜 충돌을 일으키므로, 그 부적합을 드러내는 조건이다.
        // TICKETS=N  → 버스트 안의 10건이 **같은 칸**을 노리고, 버스트마다 칸을 바꾼다(idx % N).
        //              실제 경합 조건. N=1 이면 첫 버스트에서 승부가 끝나므로 N ≥ 버스트 수로 둔다.
        const base = idx * BURST;
        const burstTicket = TICKETS > 0 ? idx % TICKETS : -1;
        const requests = [];
        for (let j = 0; j < BURST; j++) {
            const i = base + j;
            if (i >= participants.length) break;
            requests.push({
                participantId: participants[i],
                selectedNumber: burstTicket >= 0 ? burstTicket : i,
                phoneNumber: phoneOf(i),
            });
        }
        if (requests.length === 0) return;
        doDrawingBatch(requests).forEach(record);
        return;
    }

    if (idx >= participants.length) return;

    const selectedNumber = TICKETS > 0 ? idx % TICKETS : idx;
    record(doDrawing(participants[idx], selectedNumber, phoneOf(idx)));
}
