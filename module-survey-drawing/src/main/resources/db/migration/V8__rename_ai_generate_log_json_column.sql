-- AIGenerateLog 엔티티가 generated_survey_json 컬럼을 기대하는데
-- V1 은 generated_survey 로 만들어 스키마 검증이 실패한다. 엔티티 필드명에 맞춰 정정한다.
ALTER TABLE ai_generate_logs
    CHANGE COLUMN generated_survey generated_survey_json TEXT NOT NULL;
