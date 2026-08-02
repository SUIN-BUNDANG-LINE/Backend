package com.sbl.sulmun2yong.payment.dto

import java.util.UUID

// 내부 주문 발급 계약 - 설문(단독 결제 개시)이 결제의 /internal/payments/orders 를 동기 호출한다.
// payment_orders 쓰기는 결제만(단일 기록자) - 설문은 checkoutUrl 동기 반환 계약 때문에 이벤트 대신
// 내부 API 를 쓴다. /internal/** 은 게이트웨이에 라우트가 없어 외부에서 접근 불가.
data class IssueOrderRequest(
    val surveyId: UUID,
    val makerId: UUID,
    val amount: Int,
)

data class IssueOrderResponse(
    val orderId: String,
)
