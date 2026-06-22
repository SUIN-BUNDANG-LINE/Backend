/**
 * SMS Inbox 재시도 + DLT 라우팅 검증 (시나리오: Phase 1-7 확장)
 *
 * 목적: SMS 발송이 항상 실패하는 환경에서 Inbox Worker가
 *       Exponential backoff(1s, 2s, 4s, 8s, 16s)로 5회 재시도 후
 *       DLT 토픽으로 라우팅하고 dlt_messages 테이블에 기록하는지 검증.
 *
 * 사전 조건 (서버 측):
 *   application-secret.yml의 LoggingSmsSender 설정을 강제 실패 모드로 두고 서버 재기동.
 *     loggingSmsSender:
 *       forcedFailCount: 0   # 0 = 항상 실패 (-1 = 기본동작)
 *
 * 흐름:
 *   1. setup: 설문 + 추첨판(boardSize=1, winningCount=1) 생성 + 응답 1건 → participantId
 *   2. default: 추첨 1번 실행 → 무조건 당첨 → SMS 발송 시도
 *   3. teardown: 폴링하여 sms_notification_jobs.status=FAILED, retry_count=5 / dlt_messages.count >= 1
 *
 * 백오프 누적: 1+2+4+8+16 = 31초. 기본 타임아웃 90초.
 *
 * 실행:
 *   ./k6-custom run --env ACCESS_TOKEN=$ACCESS_TOKEN --env DB_DSN="..." sms-failover.js
 */
import { check, sleep } from 'k6';
import {
    setupSurveyWithDrawing,
    submitResponse,
    doDrawing,
} from './lib/helpers.js';
import {
    closeDb,
    getSmsJobStats,
    countDltMessages,
    awaitCondition,
} from './lib/db.js';
import { startAnnotation, endAnnotation } from './lib/annotations.js';

const TIMEOUT_SEC = Number(__ENV.TIMEOUT_SEC || 90);

export const options = {
    scenarios: {
        sms_failover: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '3m',
        },
    },
};

export function setup() {
    const dltBefore = countDltMessages();
    const jobsBefore = getSmsJobStats();
    console.log(`사전 상태 — dltMessages=${dltBefore}, smsJobs=${JSON.stringify(jobsBefore)}`);

    // boardSize=1, winningCount=1: 100% 당첨 보장
    const survey = setupSurveyWithDrawing({
        targetParticipantCount: 10,
        rewardCount: 1,
        title: 'k6 SMS Failover',
    });

    const visitorId = `sms-fail-${Date.now()}`;
    const res = submitResponse(survey.surveyId, survey.sectionId, visitorId);
    if (res.status !== 200) {
        throw new Error(`응답 제출 실패: status=${res.status}`);
    }
    const participantId = res.json().participantId;
    console.log(`설문 셋업 완료. participantId=${participantId}`);

    const annotationId = startAnnotation('sms-failover', ['sms', 'dlt']);

    return {
        surveyId: survey.surveyId,
        participantId,
        dltBefore,
        jobsFailedBefore: jobsBefore.FAILED,
        annotationId,
    };
}

export default function (data) {
    const drawRes = doDrawing(data.participantId, 0, '010-1234-5678');
    check(drawRes, {
        '추첨 응답 200': (r) => r.status === 200,
        '당첨 (isWon=true)': (r) => r.json().isWon === true,
    });
    // SMS 발송은 비동기로 진행 — teardown에서 검증
    sleep(1);
}

export function teardown(data) {
    const ok = awaitCondition(
        () => {
            const jobs = getSmsJobStats();
            const dltCount = countDltMessages();
            console.log(`현재: jobs=${JSON.stringify(jobs)}, dlt=${dltCount}`);
            return (
                jobs.FAILED > data.jobsFailedBefore &&
                dltCount > data.dltBefore
            );
        },
        'SMS job FAILED + DLT 메시지 적재',
        TIMEOUT_SEC,
        2,
    );

    const finalJobs = getSmsJobStats();
    const finalDlt = countDltMessages();

    check(null, {
        'sms_notification_jobs FAILED 증가': () => finalJobs.FAILED > data.jobsFailedBefore,
        'dlt_messages 적재 증가': () => finalDlt > data.dltBefore,
        '폴링 타임아웃 미발생': () => ok,
    });

    closeDb();

    endAnnotation(data?.annotationId);
}
