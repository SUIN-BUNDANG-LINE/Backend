package com.sbl.sulmun2yong.payment.dto

data class TossPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val totalAmount: Int,
)
