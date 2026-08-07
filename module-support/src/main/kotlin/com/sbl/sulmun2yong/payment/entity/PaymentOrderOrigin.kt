package com.sbl.sulmun2yong.payment.entity

// 주문의 발급 출처 - 발급 시점에 확정되는 불변 사실.
// payment-settled·payment-failed 이벤트에 실려 나가, 구독자(설문)가 단독/모금을
// co_fundings 교차 읽기 없이 판별하게 한다(Event-Carried State Transfer).
enum class PaymentOrderOrigin {
    // 단독 설문 개시(survey-payment-pending 리스너 발급)
    SOLO,

    // 공동 모금(② co-funding-created 리스너 발급)
    CO_FUNDING,
}
