-- AIEditLog 엔티티가 original_survey_json / edited_survey_json 컬럼을 기대하는데
-- V1 은 original_survey / edited_survey 로 만들어 스키마 검증(ddl-auto=validate)이 실패한다.
-- 엔티티 필드명(*Json)에 맞춰 컬럼명을 정정한다.
ALTER TABLE ai_edit_logs
    CHANGE COLUMN original_survey original_survey_json TEXT NOT NULL,
    CHANGE COLUMN edited_survey edited_survey_json TEXT NOT NULL;
