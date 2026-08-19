package com.sbl.sulmun2yong.cofunding.entity

enum class CoFundingStatus {
    // 더치페이 주문이 접수되었으며, 설문 검증 및 확정을 기다린다.
    PENDING,

    // 더치페이 주문이 거부되었다
    REJECTED,

    // 설문이 검증되었다
    FUNDING,

    // 전원 결제 -> 개설 확정
    CONFIRMED,

    // 무산 확정 -> 환불 진행
    FAILED,

    // 전원 환불 완료
    REFUNDED,
}
