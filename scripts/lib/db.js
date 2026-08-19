// MySQL 직접 접근 헬퍼 (xk6-sql)
//
// 사전 준비: 커스텀 k6 바이너리가 xk6-sql + xk6-sql-driver-mysql을 포함해야 함
//   xk6 build \
//     --with github.com/grafana/xk6-sql@latest \
//     --with github.com/grafana/xk6-sql-driver-mysql@latest
//
// 환경변수:
//   DB_DSN — MySQL DSN (기본: user:password@tcp(localhost:13306)/?parseTime=true)
//   서비스별 스키마(DB per service)라 기본 DB 없이 붙고 테이블을 스키마로 한정한다.
import sql from 'k6/x/sql';
import driver from 'k6/x/sql/driver/mysql';
import { sleep } from 'k6';

const DSN = __ENV.DB_DSN || 'user:password@tcp(localhost:13306)/?parseTime=true';
export const db = sql.open(driver, DSN);

export function closeDb() {
    db.close();
}

// xk6-sql-driver-mysql은 VARCHAR/ENUM 컬럼을 byte 배열(number[])로 반환하기 때문에
// JS 객체 키로 사용하면 "80,69,78,68,73,78,71"(ASCII 코드)로 직렬화되는 문제가 있다.
// 모든 문자열 컬럼은 이 헬퍼로 정상화한다.
function toStr(v) {
    if (v === null || v === undefined) return v;
    if (typeof v === 'string') return v;
    if (typeof v === 'object') {
        try {
            return Array.from(v)
                .map((c) => String.fromCharCode(c))
                .join('');
        } catch (e) {
            return String(v);
        }
    }
    return String(v);
}

// ── Outbox 검증 ──
export function countOutboxByStatus(status) {
    const rows = db.query('SELECT COUNT(*) AS cnt FROM survey_db.kafka_record_outbox WHERE status = ?', status);
    return Number(rows[0].cnt);
}

export function getOutboxStatsByTopic(kafkaTopic) {
    const rows = db.query(
        'SELECT status, COUNT(*) AS cnt FROM survey_db.kafka_record_outbox WHERE kafka_topic = ? GROUP BY status',
        kafkaTopic,
    );
    const result = { PENDING: 0, PUBLISHED: 0, FAILED: 0 };
    rows.forEach((r) => {
        result[toStr(r.status)] = Number(r.cnt);
    });
    return result;
}

export function getMaxOutboxRetryCountForKey(kafkaRecordKey) {
    const rows = db.query(
        'SELECT MAX(retry_count) AS max_retry, status FROM survey_db.kafka_record_outbox WHERE kafka_record_key = ? GROUP BY status',
        kafkaRecordKey,
    );
    if (rows.length === 0) return { maxRetry: 0, status: null };
    return { maxRetry: Number(rows[0].max_retry), status: toStr(rows[0].status) };
}

export function getOutboxRecentByKey(kafkaRecordKey, limit = 10) {
    return db.query(
        'SELECT HEX(id) AS hex_id, status, retry_count, created_at FROM survey_db.kafka_record_outbox WHERE kafka_record_key = ? ORDER BY created_at DESC LIMIT ?',
        kafkaRecordKey,
        limit,
    );
}

// 직접 PENDING 행을 주입 (Relay 검증용)
// agedSeconds: created_at을 현재 시점에서 N초 과거로 설정 (Relay의 60초 age 필터를 통과시키기 위해 90초 권장)
export function insertPendingOutbox(kafkaTopic, kafkaRecordKey, kafkaRecordValue, agedSeconds = 90) {
    db.exec(
        `INSERT INTO survey_db.kafka_record_outbox
         (id, kafka_topic, kafka_record_key, kafka_record_value, status, created_at, retry_count)
         VALUES
         (UNHEX(REPLACE(UUID(), '-', '')), ?, ?, ?, 'PENDING', DATE_SUB(NOW(6), INTERVAL ? SECOND), 0)`,
        kafkaTopic,
        kafkaRecordKey,
        kafkaRecordValue,
        agedSeconds,
    );
}

// 테스트 잔여 데이터 정리 - 주입 행은 kafka_record_key 가 'k6-' 로 시작한다
export function cleanupTestOutbox() {
    db.exec("DELETE FROM survey_db.kafka_record_outbox WHERE kafka_record_key LIKE 'k6-%'");
}

// ── 폴링 헬퍼 (Awaitility 패턴) ──
// predicate가 true를 반환할 때까지 폴링. 타임아웃 시 false 반환.
export function awaitCondition(predicate, label, timeoutSec = 60, intervalSec = 1) {
    const start = Date.now();
    let lastError = null;
    while (Date.now() - start < timeoutSec * 1000) {
        try {
            if (predicate()) return true;
        } catch (e) {
            lastError = e;
        }
        sleep(intervalSec);
    }
    console.error(`[awaitCondition] timeout: ${label} (${timeoutSec}s) lastError=${lastError}`);
    return false;
}
