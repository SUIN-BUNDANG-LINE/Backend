package com.sbl.sulmun2yong.payment.dto.response

// confirm.html(fetch)에 돌려주는 확정 결과 - state 는 결과 화면 계약(succeeded|processing|failed)
data class ConfirmResultResponse(
    val state: String,
)
