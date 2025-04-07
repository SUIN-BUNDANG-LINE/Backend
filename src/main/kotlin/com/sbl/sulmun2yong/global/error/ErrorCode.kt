package com.sbl.sulmun2yong.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    // Global (GL)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GL0001", "서버 오류가 발생했습니다."),
    INPUT_INVALID_VALUE(HttpStatus.BAD_REQUEST, "GL0002", "잘못된 입력입니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "GL0003", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "GL0004", "접근 권한이 없습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "GL0005", "파일은 최대 5mb까지만 업로드할 수 있습니다."),
    UNCLEAN_VISITOR(HttpStatus.FORBIDDEN, "GL0006", "유효하지 않은 visitorId입니다."),

    // Survey (SV)
    ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "SV0001", "이미 참여한 설문입니다."),
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "SV0002", "설문을 찾을 수 없습니다."),
    INVALID_QUESTION_RESPONSE(HttpStatus.BAD_REQUEST, "SV0004", "유효하지 않은 질문 응답입니다."),
    INVALID_SECTION(HttpStatus.BAD_REQUEST, "SV0005", "유효하지 않은 섹션입니다."),
    INVALID_SECTION_RESPONSE(HttpStatus.BAD_REQUEST, "SV0006", "유효하지 않은 섹션 응답입니다."),
    INVALID_SURVEY(HttpStatus.BAD_REQUEST, "SV0008", "유효하지 않은 설문입니다."),
    INVALID_SURVEY_RESPONSE(HttpStatus.BAD_REQUEST, "SV0009", "유효하지 않은 설문 응답입니다."),
    CHOICE_SIZE_EXCEED(HttpStatus.BAD_REQUEST, "SV0010", "선택지는 최대 20개까지만 가능합니다."),
    INVALID_REWARD(HttpStatus.BAD_REQUEST, "SV0011", "유효하지 않은 리워드 정보 입니다."),
    INVALID_PARTICIPANT(HttpStatus.BAD_REQUEST, "SV0012", "유효하지 않은 참여자입니다."),
    INVALID_SECTION_IDS(HttpStatus.BAD_REQUEST, "SV0013", "유효하지 않은 섹션 ID입니다."),
    SURVEY_CLOSED(HttpStatus.BAD_REQUEST, "SV0014", "응답을 받지 않는 설문입니다."),
    INVALID_UPDATE_SURVEY(HttpStatus.BAD_REQUEST, "SV0015", "설문 정보 갱신에 실패했습니다."),
    INVALID_SURVEY_ACCESS(HttpStatus.FORBIDDEN, "SV0016", "설문 접근 권한이 없습니다."),
    INVALID_SURVEY_START(HttpStatus.BAD_REQUEST, "SV0018", "설문 시작에 실패했습니다."),
    INVALID_REWARD_SETTING(HttpStatus.BAD_REQUEST, "SV0019", "유효하지 않은 리워드 지급 설정입니다."),
    INVALID_RESULT_DETAILS(HttpStatus.BAD_REQUEST, "SV0020", "유효하지 않은 설문 결과입니다."),
    INVALID_QUESTION_FILTER(HttpStatus.BAD_REQUEST, "SV0021", "유효하지 않은 질문 필터입니다."),
    INVALID_FINISHED_AT(HttpStatus.BAD_REQUEST, "SV0022", "유효하지 않은 마감일입니다."),
    INVALID_SURVEY_EDIT(HttpStatus.BAD_REQUEST, "SV0023", "설문 수정 상태 변경에 실패했습니다."),
    INVALID_PUBLISHED_AT(HttpStatus.BAD_REQUEST, "SV0024", "설문 마감일이 설문 공개일 보다 빠릅니다."),
    INVALID_RESULT_FILTER(HttpStatus.BAD_REQUEST, "SV0025", "필터는 최대 20개 까지만 적용 가능합니다."),
    INVALID_MODIFICATION_TARGET_ID(HttpStatus.BAD_REQUEST, "SV0026", "유효하지 않은 수정 대상 ID입니다."),
    EMPTY_CHOICE(HttpStatus.BAD_REQUEST, "SV0027", "선택지는 적어도 하나 이상 존재해야 합니다."),

    // Drawing (DR)
    INVALID_DRAWING_BOARD(HttpStatus.BAD_REQUEST, "DR0001", "유효하지 않은 추첨 보드입니다."),
    INVALID_DRAWING(HttpStatus.BAD_REQUEST, "DR0002", "유효하지 않은 추첨입니다."),
    OUT_OF_TICKET(HttpStatus.BAD_REQUEST, "DR0003", "모든 티켓이 추첨이 완료된 추첨 보드입니다."),
    ALREADY_SELECTED_TICKET(HttpStatus.BAD_REQUEST, "DR0004", "이미 선택된 티켓입니다."),
    ALREADY_PARTICIPATED_DRAWING(HttpStatus.BAD_REQUEST, "DR0005", "이미 참여한 추첨입니다."),
    FINISHED_DRAWING(HttpStatus.BAD_REQUEST, "DR0005", "이미 마감된 추첨입니다."),
    INVALID_DRAWING_HISTORY(HttpStatus.BAD_REQUEST, "DR0006", "유효하지 않은 추첨 기록입니다."),
    INVALID_DRAWING_BOARD_ACCESS(HttpStatus.BAD_REQUEST, "DR0007", "이미 종료되었거나, 접근할 수 없는 설문입니다."),

    // OAuth2 (OA)
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OA0001", "지원하지 않는 소셜 로그인입니다."),
    NAVER_ATTRIBUTE_CASTING_FAILED(HttpStatus.BAD_REQUEST, "OA0002", "네이버 Attribute가 null이거나 Map이 아닙니다"),

    // User (US)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US0001", "회원을 찾을 수 없습니다."),
    INVALID_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "US0002", "잘못된 회원정보 입니다."),

    // Data (DT)
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "DT0001", "유효하지 않은 전화번호입니다."),

    // File Validator (FV)
    INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "FV0001", "허용하지 않는 확장자입니다."),
    FILE_NAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "FV0003", "파일 이름이 너무 짧습니다."),
    FILE_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "FV0004", "파일 이름이 너무 깁니다."),
    NO_FILE_EXIST(HttpStatus.BAD_REQUEST, "FV0005", "파일이 존재하지 않습니다."),
    NO_EXTENSION_EXIST(HttpStatus.BAD_REQUEST, "FV0006", "파일 확장자가 존재하지 않습니다."),
    INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "FV0007", "유효하지 않은 파일 주소입니다."),

    // Python Server (PY)
    SURVEY_AI_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PY0001", "설문 AI 기능 호출에 실패했습니다."),
    TEXT_TOO_LONG(HttpStatus.BAD_REQUEST, "PY0002", "텍스트의 길이가 너무 깁니다"),
    FILE_EXTENSION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "PY0003", "지원하지 않는 파일 확장자입니다."),
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "PY0004", "파일을 찾을 수 없습니다."),
    CHAT_SESSION_ID_COOKIE_NOT_FOUND(HttpStatus.BAD_REQUEST, "PY0005", "챗봇 세션 ID 쿠키를 찾을 수 없습니다."),
    AI_EDIT_LOG_NOT_FOUND(HttpStatus.BAD_REQUEST, "PY0006", "AI 수정 기록을 찾을 수 없습니다."),
    AI_DEMO_COUNT_LIMIT(HttpStatus.BAD_REQUEST, "PY0007", "데모 AI 기능은 하루에 100회 까지만 사용 가능합니다."),
    AI_HEALTH_CHECK_FAIL(HttpStatus.BAD_REQUEST, "PY0008", "ai 서버 health check에 실패했습니다."),
}
