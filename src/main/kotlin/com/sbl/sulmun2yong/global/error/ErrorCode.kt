package com.sbl.sulmun2yong.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(val httpStatus: HttpStatus, val code: String, val message: String) {
    // Global (GL)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GL0001", "서버 오류가 발생했습니다."),
    INPUT_INVALID_VALUE(HttpStatus.BAD_REQUEST, "GL0002", "잘못된 입력입니다."),

    // Demo (DM)
    DEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "DM0001", "데모를 찾을 수 없습니다."),
}
