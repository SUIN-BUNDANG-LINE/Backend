/**
 * .env 자동 로드 헬퍼
 *
 * k6는 dotenv 내장 지원이 없어, `k6 run xxx.js` 단독 실행 시 셸에 export되지 않은
 * 변수는 못 읽는다. 이 모듈은 init context에서 .env 파일을 1회 파싱해 Map에 보관하고,
 * getEnv()로 `__ENV`와 동일한 우선순위 체계로 노출한다.
 *
 * 우선순위:
 *   1) 셸 __ENV[key]  (셸에서 export 또는 --env로 전달된 값)
 *   2) .env 파일의 key=value
 *   3) defaultValue
 *
 * open() 경로는 호출 파일(env.js) 기준 상대경로 — scripts/lib/ → 프로젝트 루트는 ../../
 *
 *
 * 파싱 규칙 (POSIX dotenv 호환):
 *   - 빈 줄, #으로 시작하는 주석 무시
 *   - KEY=VALUE 형식만 처리 (export 접두사 미지원)
 *   - 양 끝 따옴표(', ")는 제거
 *   - 값 내부 변수 보간(${...}) 미지원
 */

const DOTENV_PATH = '../../.env';

function parseDotenv(content) {
    const map = new Map();
    for (const rawLine of content.split('\n')) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#')) continue;

        const eq = line.indexOf('=');
        if (eq < 0) continue;

        const key = line.slice(0, eq).trim();
        let value = line.slice(eq + 1).trim();

        // 양 끝 따옴표 제거
        if (
            value.length >= 2 &&
            ((value.startsWith('"') && value.endsWith('"')) ||
                (value.startsWith("'") && value.endsWith("'")))
        ) {
            value = value.slice(1, -1);
        }

        if (key) map.set(key, value);
    }
    return map;
}

let _dotenv = new Map();
try {
    _dotenv = parseDotenv(open(DOTENV_PATH));
} catch (_) {
    // .env가 없거나 읽기 실패 — 셸 ENV만 사용 (graceful)
}

/**
 * 환경변수 조회
 *
 * @param {string} key
 * @param {string} [defaultValue]
 * @returns {string|undefined}  셸 __ENV > .env > defaultValue
 */
export function getEnv(key, defaultValue) {
    const shellValue = __ENV[key];
    if (shellValue !== undefined && shellValue !== '') {
        return shellValue;
    }
    if (_dotenv.has(key)) {
        return _dotenv.get(key);
    }
    return defaultValue;
}

/**
 * .env 로드 여부 확인 (디버깅용)
 *
 * @returns {boolean}
 */
export function isDotenvLoaded() {
    return _dotenv.size > 0;
}
