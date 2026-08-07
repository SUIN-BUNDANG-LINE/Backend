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

-- 설문조사
CREATE TABLE surveys (
    id BINARY(16) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail VARCHAR(500),
    published_at DATETIME(6),
    finished_at DATETIME(6),
    status VARCHAR(20) NOT NULL,
    finish_message TEXT NOT NULL,
    target_participant_count INT,
    reward_setting_type VARCHAR(20) NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    maker_id BINARY(16) NOT NULL,
    is_result_open BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    count INT NOT NULL,
    CONSTRAINT fk_rewards_survey FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE
);

CREATE TABLE sections (
    id BINARY(16) PRIMARY KEY,
    survey_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    route_type VARCHAR(20) NOT NULL,
    next_section_id BINARY(16),
    key_question_id BINARY(16),
    CONSTRAINT fk_sections_survey FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE
);

CREATE TABLE section_route_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    choice_content VARCHAR(255),
    next_section_id BINARY(16),
    CONSTRAINT fk_route_configs_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE
);

CREATE TABLE questions (
    id BINARY(16) PRIMARY KEY,
    section_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    is_required BOOLEAN NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_allow_other BOOLEAN NOT NULL,
    CONSTRAINT fk_questions_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE
);

CREATE TABLE choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    content VARCHAR(255) NOT NULL,
    CONSTRAINT fk_choices_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 응답/참가자
CREATE TABLE responses (
    id BINARY(16) PRIMARY KEY,
    participant_id BINARY(16) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    question_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE participants (
    id BINARY(16) PRIMARY KEY,
    visitor_id VARCHAR(255) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    user_id BINARY(16),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- 추첨
CREATE TABLE drawing_boards (
    id BINARY(16) PRIMARY KEY,
    survey_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_board_id BINARY(16) NOT NULL,
    ticket_index INT NOT NULL,
    dtype VARCHAR(20) NOT NULL,
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    reward_name VARCHAR(255),
    reward_category VARCHAR(255),
    CONSTRAINT fk_tickets_drawing_board FOREIGN KEY (drawing_board_id) REFERENCES drawing_boards(id) ON DELETE CASCADE
);

CREATE TABLE drawing_histories (
    id BINARY(16) PRIMARY KEY,
    participant_id BINARY(16) NOT NULL,
    phone_number VARCHAR(500) NOT NULL,
    survey_id BINARY(16) NOT NULL,
    selected_ticket_index INT NOT NULL,
    ticket_type VARCHAR(20) NOT NULL,
    reward_name VARCHAR(255),
    reward_category VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- AI
CREATE TABLE ai_generate_logs (
    id BINARY(16) PRIMARY KEY,
    survey_id BINARY(16) NOT NULL,
    maker_id BINARY(16),
    user_prompt TEXT NOT NULL,
    file_url VARCHAR(500),
    target VARCHAR(255) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    generated_survey TEXT NOT NULL,
    visitor_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE ai_edit_logs (
    id BINARY(16) PRIMARY KEY,
    survey_id BINARY(16) NOT NULL,
    maker_id BINARY(16) NOT NULL,
    user_prompt TEXT NOT NULL,
    original_survey TEXT NOT NULL,
    edited_survey TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- 인덱스
CREATE INDEX idx_surveys_maker_id ON surveys(maker_id);
CREATE INDEX idx_surveys_status ON surveys(status);
CREATE INDEX idx_responses_survey_id ON responses(survey_id);
CREATE INDEX idx_responses_participant_id ON responses(participant_id);
CREATE INDEX idx_participants_survey_id ON participants(survey_id);
CREATE INDEX idx_participants_visitor_id ON participants(survey_id, visitor_id);
CREATE INDEX idx_drawing_boards_survey_id ON drawing_boards(survey_id);
CREATE INDEX idx_drawing_histories_survey_id ON drawing_histories(survey_id);
CREATE INDEX idx_drawing_histories_participant_id ON drawing_histories(participant_id);
