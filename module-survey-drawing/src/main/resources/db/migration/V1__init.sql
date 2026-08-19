-- 설문조사
CREATE TABLE surveys
(
    id                       BINARY(16) PRIMARY KEY,
    title                    VARCHAR(255) NOT NULL,
    description              TEXT         NOT NULL,
    thumbnail                VARCHAR(500),
    published_at             DATETIME(6),
    finished_at              DATETIME(6),
    status                   VARCHAR(20)  NOT NULL,
    finish_message           TEXT         NOT NULL,
    target_participant_count INT,
    reward_setting_type      VARCHAR(20)  NOT NULL,
    is_visible               BOOLEAN      NOT NULL DEFAULT TRUE,
    maker_id                 BINARY(16)   NOT NULL,
    is_result_open           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL
);
CREATE INDEX idx_surveys_maker_id ON surveys (maker_id);
CREATE INDEX idx_surveys_status ON surveys (status);

CREATE TABLE rewards
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id   BINARY(16)   NOT NULL,
    order_index INT          NOT NULL,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(255) NOT NULL,
    count       INT          NOT NULL,
    CONSTRAINT fk_rewards_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE
);

CREATE TABLE sections
(
    id              BINARY(16) PRIMARY KEY,
    survey_id       BINARY(16)   NOT NULL,
    order_index     INT          NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    route_type      VARCHAR(20)  NOT NULL,
    next_section_id BINARY(16),
    key_question_id BINARY(16),
    CONSTRAINT fk_sections_survey FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE
);

CREATE TABLE section_route_configs
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id      BINARY(16) NOT NULL,
    order_index     INT        NOT NULL,
    choice_content  VARCHAR(255),
    next_section_id BINARY(16),
    CONSTRAINT fk_route_configs_section FOREIGN KEY (section_id) REFERENCES sections (id) ON DELETE CASCADE
);

CREATE TABLE questions
(
    id             BINARY(16) PRIMARY KEY,
    section_id     BINARY(16)   NOT NULL,
    order_index    INT          NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    is_required    BOOLEAN      NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    is_allow_other BOOLEAN      NOT NULL,
    CONSTRAINT fk_questions_section FOREIGN KEY (section_id) REFERENCES sections (id) ON DELETE CASCADE
);

CREATE TABLE choices
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BINARY(16)   NOT NULL,
    order_index INT          NOT NULL,
    content     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_choices_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);

-- 응답/참가자
CREATE TABLE responses
(
    id             BINARY(16) PRIMARY KEY,
    participant_id BINARY(16)  NOT NULL,
    survey_id      BINARY(16)  NOT NULL,
    question_id    BINARY(16)  NOT NULL,
    content        TEXT        NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL
);
CREATE INDEX idx_responses_survey_id ON responses (survey_id);
CREATE INDEX idx_responses_participant_id ON responses (participant_id);

CREATE TABLE participants
(
    id         BINARY(16) PRIMARY KEY,
    visitor_id VARCHAR(255) NOT NULL,
    survey_id  BINARY(16)   NOT NULL,
    user_id    BINARY(16),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL
);
CREATE INDEX idx_participants_survey_id ON participants (survey_id);
CREATE INDEX idx_participants_visitor_id ON participants (survey_id, visitor_id);

-- 추첨
-- 경품 보드 = 산 물건 - 결제 수명 주기(status)를 보드가 진다. 설문 상태에는 결제 개념이 없다.
CREATE TABLE drawing_boards
(
    id         BINARY(16) PRIMARY KEY,
    survey_id  BINARY(16)  NOT NULL,
    -- PENDING_PAYMENT(대금 대기 - 이 보드의 존재가 설문 수정 잠금) → ACTIVE(대금 확정)
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    -- 설문당 보드 1개를 DB 가 강제 - 동시 개시(HTTP)·판정(Kafka)의 이중 생성 경합 차단. 조회 인덱스 겸용.
    CONSTRAINT uk_drawing_boards_survey_id UNIQUE (survey_id)
);

CREATE TABLE tickets
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_board_id BINARY(16)  NOT NULL,
    ticket_index     INT         NOT NULL,
    dtype            VARCHAR(20) NOT NULL,
    is_selected      BOOLEAN     NOT NULL DEFAULT FALSE,
    reward_name      VARCHAR(255),
    reward_category  VARCHAR(255),
    CONSTRAINT fk_tickets_drawing_board FOREIGN KEY (drawing_board_id) REFERENCES drawing_boards (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uk_tickets_board_index ON tickets (drawing_board_id, ticket_index);
-- COUNT(*) 용 커버링 인덱스
CREATE INDEX tickets_board_selected ON tickets (drawing_board_id, is_selected);

CREATE TABLE drawing_histories
(
    id                    BINARY(16) PRIMARY KEY,
    participant_id        BINARY(16)   NOT NULL,
    phone_number          VARCHAR(500) NOT NULL,
    survey_id             BINARY(16)   NOT NULL,
    selected_ticket_index INT          NOT NULL,
    ticket_type           VARCHAR(20)  NOT NULL,
    reward_name           VARCHAR(255),
    reward_category       VARCHAR(255),
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL
);
CREATE INDEX idx_drawing_histories_survey_id ON drawing_histories (survey_id);
CREATE INDEX idx_drawing_histories_participant_id ON drawing_histories (participant_id);
-- 한 설문의 한 티켓은 한 명에게만 - 동시 추첨이 같은 칸에 각자 커밋하는 이중 당첨의 최후 방어선.
ALTER TABLE drawing_histories
    ADD CONSTRAINT uk_drawing_histories_survey_ticket UNIQUE (survey_id, selected_ticket_index);

-- AI
CREATE TABLE ai_generate_logs
(
    id                    BINARY(16) PRIMARY KEY,
    survey_id             BINARY(16)   NOT NULL,
    maker_id              BINARY(16),
    user_prompt           TEXT         NOT NULL,
    file_url              VARCHAR(500),
    target                VARCHAR(255) NOT NULL,
    group_name            VARCHAR(255) NOT NULL,
    generated_survey_json TEXT         NOT NULL,
    visitor_id            VARCHAR(255),
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL
);

CREATE TABLE ai_edit_logs
(
    id                   BINARY(16) PRIMARY KEY,
    survey_id            BINARY(16)  NOT NULL,
    maker_id             BINARY(16)  NOT NULL,
    user_prompt          TEXT        NOT NULL,
    original_survey_json TEXT        NOT NULL,
    edited_survey_json   TEXT        NOT NULL,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL
);

-- Outbox 패턴: DB 저장과 Kafka 발행의 원자성 보장
CREATE TABLE kafka_record_outbox
(
    id                 BINARY(16)   NOT NULL PRIMARY KEY,
    kafka_topic        VARCHAR(100) NOT NULL,
    kafka_record_key   VARCHAR(100) NOT NULL,
    kafka_record_value TEXT         NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count        INT          NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    published_at       DATETIME(6),

    INDEX idx_outbox_status_created (status, created_at)
);
