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

    // Survey (SV)
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "SV0002", "설문을 찾을 수 없습니다."),
    INVALID_SECTION_ROUTE_CONFIGS(HttpStatus.BAD_REQUEST, "SV0003", "유효하지 않은 섹션 루트 설정입니다."),
    INVALID_QUESTION_RESPONSE(HttpStatus.BAD_REQUEST, "SV0004", "유효하지 않은 질문 응답입니다."),
    INVALID_SECTION(HttpStatus.BAD_REQUEST, "SV0005", "유효하지 않은 섹션입니다."),
    INVALID_SECTION_RESPONSE(HttpStatus.BAD_REQUEST, "SV0006", "유효하지 않은 섹션 응답입니다."),
    INVALID_SURVEY(HttpStatus.BAD_REQUEST, "SV0008", "유효하지 않은 설문입니다."),
    INVALID_SURVEY_RESPONSE(HttpStatus.BAD_REQUEST, "SV0009", "유효하지 않은 설문 응답입니다."),
    INVALID_CHOICE(HttpStatus.BAD_REQUEST, "SV0010", "유효하지 않은 선택지입니다."),
    INVALID_REWARD(HttpStatus.BAD_REQUEST, "SV0011", "유효하지 않은 리워드 정보 입니다."),

    // Drawing (DR)
    INVALID_DRAWING_BOARD(HttpStatus.BAD_REQUEST, "DR0001", "유효하지 않은 추첨 보드입니다."),
    INVALID_DRAWING(HttpStatus.BAD_REQUEST, "DR0002", "유효하지 않은 추첨입니다."),

    // OAuth2 (OA)
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OA0001", "지원하지 않는 소셜 로그인입니다."),
    NAVER_ATTRIBUTE_CASTING_FAILED(HttpStatus.BAD_REQUEST, "OA0002", "네이버 Attribute가 null이거나 Map이 아닙니다"),

    // User (US)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US0001", "회원을 찾을 수 없습니다."),
    INVALID_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "US0002", "잘못된 회원정보 입니다."),
}
