package com.sbl.sulmun2yong.payment.entity

// 성공 종착은 아웃박스 도장과 같은 어휘(SUCCEEDED)로 통일한다.
// 토스 API 응답의 "DONE" 은 외부 계약이라 어댑터 판독에서만 원문 그대로 다룬다.
enum class TossOrderStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELED,
}
