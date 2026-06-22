/**
 * Grafana annotation API 헬퍼
 *
 * k6 부하 시나리오의 시작/종료 시각을 Grafana 대시보드에 마킹하기 위한 모듈.
 * setup()에서 startAnnotation, teardown()에서 endAnnotation을 호출하면
 * 모든 시계열 패널에 시간 축 음영이 자동으로 표시된다.
 *
 * ENV:
 *   GRAFANA_URL   — Grafana 베이스 URL (예: http://localhost:13000)
 *   GRAFANA_TOKEN — Service account Bearer 토큰 (Editor 권한 필요)
 *
 * 둘 중 하나라도 미설정 시 graceful skip (예외 X, null 반환).
 *
 * 동작:
 *   - POST  /api/annotations         → point annotation 생성, id 반환
 *   - PATCH /api/annotations/{id}    → timeEnd 추가 → range annotation 승격
 *
 * Annotation은 Prometheus 메트릭과 분리된 Grafana 자체 메타데이터다.
 * 시각 축에 음영을 박을 뿐이며, "이 시간대 요청 = 이 시나리오"라는
 * 인과 매핑을 제공하지 않는다. 시나리오 격리 측정을 가정한 시각 라벨링 도구.
 */
import http from 'k6/http';
import { getEnv } from './env.js';

// Grafana가 응답 못 해도 시나리오는 계속 돌아야 함 → 짧은 타임아웃
const REQUEST_TIMEOUT = '5s';

function authHeaders(token) {
    return {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
    };
}

/**
 * Grafana annotation 시작 마킹 (point annotation 생성)
 *
 * @param {string}   scenarioName  시나리오 식별자 (예: 'skip-locked-concurrency')
 * @param {string[]} extraTags     추가 태그 배열 (예: ['outbox', 'recovery'])
 * @returns {number|null}          annotation id (성공) 또는 null (skip/실패)
 */
export function startAnnotation(scenarioName, extraTags = []) {
    const url = getEnv('GRAFANA_URL');
    const token = getEnv('GRAFANA_TOKEN');

    if (!url || !token) {
        console.log('[annotation] GRAFANA_URL/GRAFANA_TOKEN 미설정 — skip');
        return null;
    }

    const payload = JSON.stringify({
        time: Date.now(),
        tags: ['k6', scenarioName, ...extraTags],
        text: `k6 시나리오 시작: ${scenarioName}`,
    });

    const res = http.post(`${url}/api/annotations`, payload, {
        headers: authHeaders(token),
        timeout: REQUEST_TIMEOUT,
    });

    if (res.status !== 200) {
        console.warn(
            `[annotation] startAnnotation 실패: status=${res.status} body=${res.body}`,
        );
        return null;
    }

    const id = res.json().id;
    console.log(`[annotation] 시작 마킹: scenario=${scenarioName} id=${id}`);
    return id;
}

/**
 * Grafana annotation 종료 마킹 (timeEnd 추가 → range annotation 승격)
 *
 * @param {number|null} annotationId  startAnnotation의 반환값
 */
export function endAnnotation(annotationId) {
    if (annotationId == null) {
        return;
    }

    const url = getEnv('GRAFANA_URL');
    const token = getEnv('GRAFANA_TOKEN');

    if (!url || !token) {
        return;
    }

    const payload = JSON.stringify({
        timeEnd: Date.now(),
    });

    const res = http.patch(`${url}/api/annotations/${annotationId}`, payload, {
        headers: authHeaders(token),
        timeout: REQUEST_TIMEOUT,
    });

    if (res.status !== 200) {
        console.warn(
            `[annotation] endAnnotation 실패: id=${annotationId} status=${res.status} body=${res.body}`,
        );
        return;
    }

    console.log(`[annotation] 종료 마킹: id=${annotationId}`);
}
