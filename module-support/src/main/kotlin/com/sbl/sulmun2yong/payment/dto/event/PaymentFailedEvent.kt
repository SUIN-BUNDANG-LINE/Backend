package com.sbl.sulmun2yong.payment.dto.event

import java.time.Instant

// payment-failed wire 계약 - confirm 거절·결제창 이탈 사실 (발행: settleRejected·handleFail 의 Outbox).
// 구독: 설문(단독 결제만 NOT_STARTED 복귀 - 모금 걸린 설문은 스킵, 모금은 기한 만료 무산 경로).
data class PaymentFailedEvent(
    val eventId: String,
    val orderId: String,
    val surveyId: String,
    val failedAt: Instant,
)
