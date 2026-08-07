package com.sbl.sulmun2yong.payment.dto.event

import java.time.Instant

// payment-settled wire 계약 - confirm 성공 사실 (발행: settle tx 의 Outbox, 단독·모금 불문).
// 구독: 설문(단독 결제만 활성화 - 모금 건 스킵, 활성화는 ⑤ co-funding-confirmed 몫) · 모금(SETTLED 전이+장벽).
// origin(주문 발급 출처)을 실어 보내 구독자가 교차 읽기 없이 단독/모금을 판별한다.
data class PaymentSettledEvent(
    val eventId: String,
    val orderId: String,
    val surveyId: String,
    // PaymentOrderOrigin name - 필드 없던 옛 이벤트 재소비 시 보수적으로 모금 취급(단독 활성화 억제)
    val origin: String = "CO_FUNDING",
    val settledAt: Instant,
)
