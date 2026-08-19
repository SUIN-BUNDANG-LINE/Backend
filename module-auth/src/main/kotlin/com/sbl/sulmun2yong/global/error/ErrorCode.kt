package com.sbl.sulmun2yong.global.error

import org.springframework.http.HttpStatus

// 이 서비스가 내는 오류만 정의한다 - 코드 카탈로그는 서비스별 소유(접두사로 전역 유일성 유지).
enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    // OAuth2 (OA)
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OA0001", "지원하지 않는 소셜 로그인입니다."),
    NAVER_ATTRIBUTE_CASTING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OA0002", "네이버 응답을 해석하지 못했습니다."),

    // User (US)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "US0001", "회원을 찾을 수 없습니다."),
    INVALID_USER_EXCEPTION(HttpStatus.BAD_REQUEST, "US0002", "잘못된 회원정보 입니다."),

    // Data (DT) - PhoneNumber 검증
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "DT0001", "유효하지 않은 전화번호입니다."),
}
