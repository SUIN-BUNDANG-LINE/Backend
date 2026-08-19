-- 서비스별 스키마 (DB per service) — 각 서비스가 자기 스키마의 단일 기록자이자 Flyway 주인이다.
-- 같은 MySQL 인스턴스에 스키마만 나눈 형태 — 인스턴스까지 분리하는 건 다음 단계.
CREATE DATABASE IF NOT EXISTS survey_db;
CREATE DATABASE IF NOT EXISTS cofunding_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS auth_db;

-- 컨슈머도 각자 스키마를 가진다.

-- 앱 계정에 전 스키마 권한 (로컬 개발 기준 — 운영이라면 스키마별 계정을 나눈다)
GRANT ALL PRIVILEGES ON survey_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON cofunding_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON auth_db.* TO 'user'@'%';
FLUSH PRIVILEGES;
