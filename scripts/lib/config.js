// k6 공통 설정
//
// 클러스터 라운드로빈 (기본): k6 run script.js
//   → BASE_URLS 기본값 http://localhost:18080,http://localhost:18081
// 단일 URL: k6 run --env BASE_URL=http://localhost:18080 script.js
// 명시 라운드로빈: k6 run --env BASE_URLS=url1,url2 script.js
//
// 인증 (게이트웨이 헤더 방식)
//   web 은 JWT 를 직접 검증하지 않고 게이트웨이가 주입한 X-User-Id/X-User-Role 을 신뢰한다.
//   직접 호출(부하 테스트)은 GatewayOnlyFilter 의 공유 비밀(X-Gateway-Auth)과 함께
//   사용자 헤더를 스스로 넣어 게이트웨이 통과를 재현한다.
//   오버라이드: GATEWAY_SECRET(기본 local-dev-secret), TEST_USER_ID, TEST_USER_ROLE

import exec from 'k6/execution';

const _rawBaseUrls = __ENV.BASE_URLS || __ENV.BASE_URL || 'http://localhost:18080,http://localhost:18081';
export const BASE_URLS = _rawBaseUrls
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

// 하위 호환 — 단일 URL 기대 코드용 (첫 번째)
export const BASE_URL = BASE_URLS[0];

// 인스턴스 선택 — VU 전역 id(idInTest) 기준으로 분산한다.
// per-vu-iterations 처럼 VU당 호출이 1회여도 VU마다 다른 인스턴스로 가므로
// web-1/web-2 에 동시 부하가 갈려 분산락의 cross-JVM 경합이 실제로 검증된다.
// (기존 모듈 카운터 방식은 k6 VU가 독립 isolate라 매 VU가 index 0 = web-1 로만 몰렸다.)
// setup/teardown 등 VU 밖 컨텍스트에서는 idInTest 가 없으므로 0(첫 인스턴스)로 폴백.
export function pickBaseUrl() {
    const vu = exec.vu && exec.vu.idInTest ? exec.vu.idInTest : 0;
    return BASE_URLS[vu % BASE_URLS.length];
}

// 버스트 내 분산 — 한 반복(VU)이 http.batch 로 묶어 보내는 요청들을 요청 인덱스로 번갈아 배정한다.
// pickBaseUrl(VU 기준)만 쓰면 버스트 전체가 같은 JVM 으로 가서, 같은 칸 경합이 JVM 경계를
// 넘는 일이 없다 — synchronized 의 cross-JVM 검증이 성립하려면 요청 단위로 갈라야 한다.
export function pickBaseUrlAt(i) {
    return BASE_URLS[i % BASE_URLS.length];
}

// 게이트웨이 공유 비밀 — GatewayOnlyFilter 가 전 요청(/management 제외)에서 검사한다
const GATEWAY_SECRET = __ENV.GATEWAY_SECRET || 'local-dev-secret';
const TEST_USER_ID = __ENV.TEST_USER_ID || '00000000-0000-4000-8000-000000000001';
const TEST_USER_ROLE = __ENV.TEST_USER_ROLE || 'ROLE_AUTHENTICATED_USER';

// 인증 필요 요청 옵션 (설문 생성/저장/시작) — 게이트웨이가 주입했을 사용자 헤더를 재현
export function authParams() {
    return {
        headers: {
            'Content-Type': 'application/json',
            'X-Gateway-Auth': GATEWAY_SECRET,
            'X-User-Id': TEST_USER_ID,
            'X-User-Role': TEST_USER_ROLE,
        },
    };
}

// 인증 불필요 요청 옵션 (응답 제출, 추첨) — 게이트웨이 통과 비밀만 필요
export function jsonParams() {
    return {
        headers: {
            'Content-Type': 'application/json',
            'X-Gateway-Auth': GATEWAY_SECRET,
        },
    };
}
