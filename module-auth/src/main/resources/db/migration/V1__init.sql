-- 인증 서비스 스키마 (auth_db) — 사용자의 주인. users·refresh_tokens

-- 사용자
CREATE TABLE users (
    id BINARY(16) PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    phone_number VARCHAR(500),
    role VARCHAR(30) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE refresh_tokens (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    token VARCHAR(500) NOT NULL,
    expiration_date DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
