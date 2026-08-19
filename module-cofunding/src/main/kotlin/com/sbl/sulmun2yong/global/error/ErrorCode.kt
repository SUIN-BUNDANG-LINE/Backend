package com.sbl.sulmun2yong.global.error

import org.springframework.http.HttpStatus

// 이 서비스가 내는 오류만 정의한다 - 코드 카탈로그는 서비스별 소유(접두사로 전역 유일성 유지).
enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    // Co-Funding (CF)
    INVALID_CO_FUNDING_CAPACITY(HttpStatus.BAD_REQUEST, "CF0001", "공동 모금 정원은 2인 이상이어야 합니다."),
    INVALID_CO_FUNDING_STATE(HttpStatus.CONFLICT, "CF0002", "허용되지 않는 공동 모금 상태입니다."),
    CO_FUNDING_NOT_FOUND(HttpStatus.NOT_FOUND, "CF0003", "공동 모금 정보를 찾을 수 없습니다."),
    INVALID_CO_FUNDING_REQUEST(HttpStatus.BAD_REQUEST, "CF0004", "공동 모금 개시 요청이 올바르지 않습니다."),
}
