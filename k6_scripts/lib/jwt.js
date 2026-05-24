// JWT 자동 발급 (HS256) — 부하 테스트 셋업에서 실제 OAuth 로그인 없이 인증 통과용
// 로컬/스테이징 dev 환경 전용. 운영 secret 사용 금지.
//
// JwtTokenProvider.kt와 동일 구조
//   header  = {alg: 'HS256', typ: 'JWT'}
//   payload = {sub: <userId>, role: <UserRole>, nickname: <string>, iat, exp}
//   sig     = HMAC-SHA256(header.payload, Base64URL-decoded secret)
//
// secret 우선순위
//   1) opts.secret 인자
//   2) ENV JWT_SECRET_KEY
//   3) application-secret.yml의 jwt.secret-key (k6를 k6_scripts/에서 실행할 때 자동 로드)
//
// JwtAuthenticationFilter는 stateless 검증 — DB lookup 안 함. SurveyWorkbenchService도
// makerId FK 검증 안 함. → sub에 임의 UUID 박아도 createSurvey/save/start 통과.

import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

// init context (모듈 로드 시점)에 한 번만 yml 파일 읽기 시도
// k6 open()은 호출 파일(jwt.js) 위치 기준 상대경로 — k6_scripts/lib/ → repo root는 2단계 위
const SECRET_YML_PATH = '../../modules/common/src/main/resources/application-secret.yml';

function loadSecretFromYml() {
    try {
        const yml = open(SECRET_YML_PATH);
        // 여러 secret-key 중 jwt: 블록 뒤의 것만 정확히 추출
        // (앞쪽엔 OAuth provider들의 secret-key가 들어있음)
        const m = yml.match(/^jwt:[\s\S]*?secret-key:\s*([^\s#]+)/m);
        return m ? m[1] : null;
    } catch (e) {
        return null; // 파일 없거나 권한 없음 — ENV로 fallback
    }
}

const _ymlSecret = loadSecretFromYml();

function b64urlString(str) {
    return encoding.b64encode(str, 'rawurl');
}

function base64ToBase64Url(b64) {
    return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function makeAccessToken(opts = {}) {
    const secret = opts.secret || __ENV.JWT_SECRET_KEY || _ymlSecret;
    if (!secret) {
        throw new Error(
            'JWT secret을 찾지 못했습니다. JWT_SECRET_KEY ENV를 주거나, ' +
                'k6를 k6_scripts/ 디렉토리에서 실행하세요(application-secret.yml 자동 로드).',
        );
    }
    const secretBytes = encoding.b64decode(secret, 'rawurl');

    const userId =
        opts.userId || __ENV.JWT_USER_ID || '00000000-0000-4000-8000-000000000001';
    const role = opts.role || __ENV.JWT_ROLE || 'ROLE_AUTHENTICATED_USER';
    const nickname = opts.nickname || __ENV.JWT_NICKNAME || 'k6-loadtest';
    const ttlSec = parseInt(opts.ttlSec || __ENV.JWT_TTL_SEC || '600', 10);

    const nowSec = Math.floor(Date.now() / 1000);
    const expSec = nowSec + ttlSec;

    const header = { alg: 'HS256', typ: 'JWT' };
    const payload = {
        sub: userId,
        role: role,
        nickname: nickname,
        iat: nowSec,
        exp: expSec,
    };

    const headerB64 = b64urlString(JSON.stringify(header));
    const payloadB64 = b64urlString(JSON.stringify(payload));
    const signingInput = `${headerB64}.${payloadB64}`;

    // crypto.hmac는 표준 base64로만 출력 → base64url로 변환
    const sigB64 = crypto.hmac('sha256', secretBytes, signingInput, 'base64');
    const sigB64Url = base64ToBase64Url(sigB64);

    return `${signingInput}.${sigB64Url}`;
}

// secret이 정상 로드됐는지 init context에서 미리 확인
export function isSecretAvailable() {
    return !!(__ENV.JWT_SECRET_KEY || _ymlSecret);
}
