package com.sbl.sulmun2yong.cofunding.entity

enum class CoFundingStatus {
    // 개설 접수 - 설문 판정(co-funding-approved/rejected) 대기
    PENDING_APPROVAL,

    // 설문 판정 거절 - 종착 (결제자가 없으므로 환불 없음)
    REJECTED,

    // 모금 중
    FUNDING,

    // 전원 결제 -> 개설 확정
    CONFIRMED,

    // 무산 확정 -> 환불 진행
    FAILED,

    // 전원 환불 완료
    REFUNDED,
}
