// API 호출 헬퍼 함수
import http from 'k6/http';
import { check, sleep } from 'k6';
import { pickBaseUrl, authParams, jsonParams } from './config.js';

// UUID v4 생성
export function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

// ── 설문 생성/설정 (인증 필요) ──

// 설문 생성 → surveyId 반환
export function createSurvey() {
    const res = http.post(`${pickBaseUrl()}/api/v1/surveys/workbench/create`, null, authParams());
    check(res, { '설문 생성 성공': (r) => r.status === 200 });
    return res.json();
}

// 설문 저장 (ImmediateDraw 설정) → sectionId 반환
export function saveSurveyWithImmediateDraw(surveyId, opts = {}) {
    const sectionId = uuidv4();
    const targetParticipantCount = opts.targetParticipantCount || 100;
    const rewardCount = opts.rewardCount || 10;

    // FinishedAt 도메인 규칙 — 분/초/밀리초가 모두 0이어야 함 (정시 단위)
    const finishedAt = new Date();
    finishedAt.setDate(finishedAt.getDate() + 30);
    finishedAt.setMinutes(0, 0, 0);

    const payload = JSON.stringify({
        title: opts.title || 'k6 부하 테스트 설문',
        description: opts.description || 'k6 자동 생성',
        thumbnail: null,
        finishMessage: '감사합니다',
        isVisible: true,
        isResultOpen: false,
        rewardSetting: {
            type: 'IMMEDIATE_DRAW',
            rewards: [{ name: '테스트 리워드', category: '테스트', count: rewardCount }],
            targetParticipantCount: targetParticipantCount,
            finishedAt: finishedAt.toISOString(),
        },
        sections: [
            {
                sectionId: sectionId,
                title: '섹션 1',
                description: '',
                questions: [],
                routeDetails: {
                    type: 'NUMERICAL_ORDER',
                    nextSectionId: null,
                    keyQuestionId: null,
                    sectionRouteConfigs: null,
                },
            },
        ],
    });

    const res = http.put(`${pickBaseUrl()}/api/v1/surveys/workbench/${surveyId}`, payload, authParams());
    check(res, { '설문 저장 성공': (r) => r.status === 200 });
    return sectionId;
}

// 설문 시작 (ImmediateDraw면 DrawingBoard 자동 생성)
export function startSurvey(surveyId) {
    const res = http.patch(`${pickBaseUrl()}/api/v1/surveys/workbench/start/${surveyId}`, null, authParams());
    check(res, { '설문 시작 성공': (r) => r.status === 200 });
}

// 결제 우회(테스트 전용) — 경품 설문이 PENDING_PAYMENT 에 머무는 것을 IN_PROGRESS 로 강제 전환.
// TEST_AUTH_ENABLED=true 로 뜬 서버의 TestAuthController 활성화 엔드포인트를 호출한다.
export function activateSurvey(surveyId) {
    const res = http.post(`${pickBaseUrl()}/api/v1/test/surveys/${surveyId}/activate`, null, jsonParams());
    check(res, { '설문 활성화(결제 우회) 성공': (r) => r.status === 200 });
    return res;
}

// 설문 삭제
export function deleteSurvey(surveyId) {
    const res = http.del(`${pickBaseUrl()}/api/v1/surveys/workbench/delete/${surveyId}`, null, authParams());
    return res;
}

// ── 응답/추첨 (인증 불필요) ──

// 설문 응답 제출 → { participantId, isImmediateDraw }
export function submitResponse(surveyId, sectionId, visitorId) {
    const payload = JSON.stringify({
        sectionResponses: [
            {
                sectionId: sectionId,
                questionResponses: [],
            },
        ],
        visitorId: visitorId,
    });

    return http.post(`${pickBaseUrl()}/api/v1/surveys/response/${surveyId}`, payload, jsonParams());
}

// 추첨 실행
// LOCK_MODE=off              → /draw-no-lock          (실험용, 락 없음)
// LOCK_MODE=optimistic       → /draw-optimistic       (실험용, 낙관적 락)
// LOCK_MODE=optimistic-retry → /draw-optimistic-retry (실험용, 낙관적 락 + 재시도 5회)
// LOCK_MODE=on (기본)        → /draw                  (운영, Redisson 분산락)
const DRAW_PATHS = {
    'off': '/api/v1/drawing-board/draw-no-lock',
    'optimistic': '/api/v1/drawing-board/draw-optimistic',
    'optimistic-retry': '/api/v1/drawing-board/draw-optimistic-retry',
};

export function doDrawing(participantId, selectedNumber, phoneNumber) {
    const payload = JSON.stringify({
        participantId: participantId,
        selectedNumber: selectedNumber,
        phoneNumber: phoneNumber,
    });

    const path = DRAW_PATHS[__ENV.LOCK_MODE] || '/api/v1/drawing-board/draw';

    return http.post(`${pickBaseUrl()}${path}`, payload, jsonParams());
}

// 추첨판 정보 조회
export function getDrawingBoardInfo(surveyId) {
    return http.get(`${pickBaseUrl()}/api/v1/drawing-board/info/${surveyId}`, jsonParams());
}

// 설문 정보 조회
export function getSurveyInfo(surveyId) {
    return http.get(`${pickBaseUrl()}/api/v1/surveys/info/${surveyId}`, jsonParams());
}

// ── 설문 + 추첨 전체 셋업 (setup 함수에서 호출) ──

// 설문 생성 → 저장 → 시작 → { surveyId, sectionId } 반환
export function setupSurveyWithDrawing(opts = {}) {
    const createRes = createSurvey();
    const surveyId = createRes.surveyId || createRes.id;
    if (!surveyId) {
        console.error('설문 생성 실패: surveyId를 받지 못함');
        return null;
    }

    const sectionId = saveSurveyWithImmediateDraw(surveyId, opts);
    startSurvey(surveyId);
    // 경품 설문은 start 시 PENDING_PAYMENT 로 가므로, 테스트 우회로 IN_PROGRESS 로 전환한다.
    activateSurvey(surveyId);

    console.log(`설문 셋업 완료: surveyId=${surveyId}, sectionId=${sectionId}`);
    return { surveyId, sectionId };
}

// VU별 고유 visitorId 생성
export function makeVisitorId(vuId, iter) {
    return `k6-vu${vuId}-iter${iter}-${Date.now()}`;
}

// VU별 고유 전화번호 생성
export function makePhoneNumber(vuId, iter) {
    const num = ((vuId * 1000 + iter) % 10000).toString().padStart(4, '0');
    return `010-0000-${num}`;
}

// 폴링: 설문 상태가 기대값이 될 때까지 대기
export function waitForSurveyStatus(surveyId, expectedStatus, timeoutSec = 60) {
    const start = Date.now();
    while (Date.now() - start < timeoutSec * 1000) {
        const res = getSurveyInfo(surveyId);
        if (res.status === 200) {
            const body = res.json();
            if (body.status === expectedStatus) {
                return true;
            }
        }
        sleep(1);
    }
    console.error(`타임아웃: surveyId=${surveyId}가 ${expectedStatus} 상태가 되지 않음 (${timeoutSec}초)`);
    return false;
}
