package com.sbl.sulmun2yong.payment.dto

// 토스 전액취소 요청 본문 겸 CANCEL 커맨드의 requestPayload - cancelAmount 생략 = 전액취소
// paymentKey는 담지 않는다 - 취소 시점에 릴레이가 장부(toss_orders)에서 해석한다.
data class TossCancelRequest(
    val cancelReason: String,
)
