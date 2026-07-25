package com.sbl.sulmun2yong.payment.dto.response

data class CheckoutInfoResponse(
    val orderId: String,
    val amount: Int,
    val status: String,
    val clientKey: String,
)
