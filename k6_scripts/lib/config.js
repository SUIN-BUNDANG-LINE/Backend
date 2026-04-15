// k6 공통 설정
// 사용법: k6 run --env BASE_URL=http://localhost:8080 --env ACCESS_TOKEN=xxx script.js

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

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
