package com.sbl.sulmun2yong.notification.entity

enum class SmsJobStatus {
    /** 발송 대기 중 (초기 상태 또는 재시도 대기) */
    PENDING,

    /** Worker가 발송 시도 중 (다른 Worker의 중복 실행 방지) */
    EXECUTING,

    /** 발송 성공 (종료 상태) */
    COMPLETED,

    /** 최대 재시도 초과 — DLT 토픽으로 라우팅, 수동 개입 필요 */
    FAILED,
}
