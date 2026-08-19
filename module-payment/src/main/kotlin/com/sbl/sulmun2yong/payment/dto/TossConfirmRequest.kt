package com.sbl.sulmun2yong.payment.dto

data class TossConfirmRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Int,
)
