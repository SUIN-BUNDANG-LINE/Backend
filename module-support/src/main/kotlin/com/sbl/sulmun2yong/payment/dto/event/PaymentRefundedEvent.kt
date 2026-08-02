package com.sbl.sulmun2yong.payment.dto.event

import java.time.Instant

// ⑦ payment-refunded wire 계약 - 환불(CANCEL) 완료 사실 (발행: 릴레이 CANCEL 승인 tx 의 Outbox).
// 구독: 모금(참여자 REFUNDED 전이 + FAILED→REFUNDED 수렴 CAS - participants·co_fundings 는 모금만 쓴다).
data class PaymentRefundedEvent(
    val eventId: String,
    val orderId: String,
    val surveyId: String,
    val refundedAt: Instant,
)
