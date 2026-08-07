package com.sbl.sulmun2yong.notification.exception

// SmsSender 구현체가 발송 실패 시 던지는 도메인 예외.
// 시스템 오류(OOM/NPE/JSON 파싱 실패 등)와 구분하여 재시도/DLT 회로에 들이지 않기 위함.
class SmsSendException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
