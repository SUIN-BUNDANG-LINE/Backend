package com.sbl.sulmun2yong.payment.dto

// 착지장(confirm.html)이 토스 failUrl 쿼리에서 꺼내 body 로 옮겨 싣는 실패 기록 요청
data class PaymentFailRequest(
    val orderId: String,
    val code: String? = null,
    val message: String? = null,
)
