package com.sbl.sulmun2yong.payment.dto

// 착지장(confirm.html)이 토스 successUrl 쿼리에서 꺼내 body 로 옮겨 싣는 승인 확정 요청
data class PaymentSuccessRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Int,
)
