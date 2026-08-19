package com.sbl.sulmun2yong.payment.entity

// 미확정 하나 + 종착 둘. 미발송/응답 유실을 구분하지 않는다 - 처방(재시도)이 같아서다.
// 재시도 안전은 토스 Idempotency-Key 와 SUCCEEDED 멱등 가드가 보장한다.
enum class TossApiCallStatus {
    PENDING,

    // 심부름 성공 - 토스가 받아들임 (confirm 승인 / cancel 완료). 동사 중립이라 CANCEL 에도 자연스럽다.
    SUCCEEDED,

    // 새 시도로 대체된 ApiCall Status
    SUPERSEDED,

    FAILED,
}
