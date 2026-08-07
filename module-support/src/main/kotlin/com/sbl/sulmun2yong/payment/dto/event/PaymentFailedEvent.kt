package com.sbl.sulmun2yong.payment.dto.event

import java.time.Instant

// payment-failed wire 계약 - confirm 거절·결제창 이탈 사실 (발행: settleRejected·handleFail 의 Outbox).
// 구독: 설문(단독 결제만 NOT_STARTED 복귀 - 모금 건 스킵, 모금은 기한 만료 무산 경로).
// origin(주문 발급 출처)을 실어 보내 구독자가 교차 읽기 없이 단독/모금을 판별한다.
data class PaymentFailedEvent(
    val eventId: String,
    val orderId: String,
    val surveyId: String,
    // PaymentOrderOrigin name - 필드 없던 옛 이벤트 재소비 시 보수적으로 모금 취급(단독 복귀 억제)
    val origin: String = "CO_FUNDING",
    val failedAt: Instant,
)
