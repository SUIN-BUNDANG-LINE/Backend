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
import { setupSurveyWithDrawing, submitResponse, doDrawing } from '../../lib/helpers.js';

const drawSuccess = new Counter('drawing_success_total');
const drawFail = new Counter('drawing_fail_total');

const LOCK_MODE = __ENV.LOCK_MODE || 'on'; // on | off | optimistic
const RATE = Number(__ENV.RATE || 10);
const DURATION_S = Number(__ENV.DURATION_S || 30);
const POOL = RATE * DURATION_S;

export const options = {
    scenarios: {
        draw_load: {
            executor: 'constant-arrival-rate',
            rate: RATE,
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

export function drawLoad(data) {
    const idx = exec.scenario.iterationInTest;
    const { participants } = data;
    if (idx >= participants.length) return;

    // 전화번호는 참가자마다 고유해야 한다 — 중복 시 DR0005("이미 참여한 추첨")로 막힌다.
    const phone = `010-${String(Math.floor(idx / 10000)).padStart(4, '0')}-${String(idx % 10000).padStart(4, '0')}`;
    const res = doDrawing(participants[idx], idx, phone);
    if (res.status === 200) {
        drawSuccess.add(1);
        check(res, { '추첨 성공(200)': (r) => r.json().isWon !== undefined });
    } else {
        drawFail.add(1);
        check(res, { '실패 상태코드 기록': () => true });
    }
}
