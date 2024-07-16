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

    // User (US)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US0001", "회원을 찾을 수 없습니다."),
}
