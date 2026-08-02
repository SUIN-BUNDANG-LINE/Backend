package com.sbl.sulmun2yong.payment.dto.event

import java.time.Instant

// payment-settled wire 계약 - confirm 성공 사실 (발행: settle tx 의 Outbox, 단독·모금 불문).
// 구독: 설문(단독 결제만 활성화 - 모금 걸린 설문은 스킵, 활성화는 ⑤ co-funding-confirmed 몫).
// 결제는 사실만 발행한다 - 단독/모금 판별은 구독자의 co_fundings 교차 읽기(허용) 몫.
data class PaymentSettledEvent(
    val eventId: String,
    val orderId: String,
    val surveyId: String,
    val settledAt: Instant,
)
