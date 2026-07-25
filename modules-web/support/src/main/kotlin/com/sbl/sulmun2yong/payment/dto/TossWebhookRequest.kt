package com.sbl.sulmun2yong.payment.dto

// 토스 웹훅 본문 - eventType + data(Payment 객체). 필요한 필드만 받는다.
data class TossWebhookRequest(
    val eventType: String,
    val createdAt: String? = null,
    val data: TossWebhookData,
)

data class TossWebhookData(
    val paymentKey: String,
    val orderId: String,
    val status: String,
)
