package com.sbl.sulmun2yong.cofunding.dto.response

data class CoFundingMyOrderResponse(
    val orderId: String,
    val amount: Int,
    val checkoutUrl: String,
)
