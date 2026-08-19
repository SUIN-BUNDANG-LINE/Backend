package com.sbl.sulmun2yong.cofunding.entity

enum class CoFundingParticipantStatus {
    // 자리 선점(결제 전)
    REGISTERED,

    // 결제 확정
    PAID,

    // 무산 환불 완료
    REFUNDED,
}
