// k6 공통 설정
//
// 클러스터 라운드로빈 (기본): k6 run script.js
//   → BASE_URLS 기본값 http://localhost:18080,http://localhost:18081
// 단일 URL: k6 run --env BASE_URL=http://localhost:18080 script.js
// 명시 라운드로빈: k6 run --env BASE_URLS=url1,url2 script.js
//
// ACCESS_TOKEN
//   1) ENV ACCESS_TOKEN 주면 그대로 사용
//   2) 없으면 jwt.js의 makeAccessToken()으로 자동 발급
//      (application-secret.yml에서 secret 자동 로드 — k6_scripts/에서 실행 시)

import { makeAccessToken, isSecretAvailable } from './jwt.js';

const _rawBaseUrls = __ENV.BASE_URLS || __ENV.BASE_URL || 'http://localhost:18080,http://localhost:18081';
export const BASE_URLS = _rawBaseUrls
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

// 하위 호환 — 단일 URL 기대 코드용 (첫 번째)
export const BASE_URL = BASE_URLS[0];

// 호출당 라운드로빈 — VU별로 카운터 독립이지만, VU 여러 개면 전체적으로 균등 분산
let _rrCounter = 0;
export function pickBaseUrl() {
    const url = BASE_URLS[_rrCounter % BASE_URLS.length];
    _rrCounter += 1;
    return url;
}

// init context에서 토큰 1회 발급 후 모든 VU가 공유
function resolveAccessToken() {
    const envToken = __ENV.ACCESS_TOKEN;
    if (envToken && envToken.length > 0) {
        console.log('[config] ACCESS_TOKEN ENV 사용');
        return envToken;
    }
    if (!isSecretAvailable()) {
        console.warn(
            '[config] JWT secret을 찾지 못함 — 빈 토큰으로 진행. ' +
                '인증 필요 API(workbench/*) 호출 시 401 발생. ' +
                'JWT_SECRET_KEY ENV를 주거나 k6_scripts/ 디렉토리에서 실행하세요.',
        );
        return '';
    }
    const token = makeAccessToken();
    console.log('[config] JWT 자동 발급 완료');
    return token;
}

export const ACCESS_TOKEN = resolveAccessToken();

// 인증 필요 요청 옵션 (설문 생성/저장/시작)
export function authParams() {
    return {
        headers: { 'Content-Type': 'application/json' },
        cookies: { 'access-token': ACCESS_TOKEN },
    };
}

// 인증 불필요 요청 옵션 (응답 제출, 추첨)
export function jsonParams() {
    return {
        headers: { 'Content-Type': 'application/json' },
    };
}
