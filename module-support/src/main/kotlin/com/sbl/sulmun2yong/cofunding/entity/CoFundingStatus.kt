package com.sbl.sulmun2yong.cofunding.entity

enum class CoFundingStatus {
    // 모금 중
    FUNDING,

    // 전원 결제 -> 개설 확정
    CONFIRMED,

    // 무산 확정 -> 환불 진행
    FAILED,

    // 전원 환불 완료
    REFUNDED,
}
