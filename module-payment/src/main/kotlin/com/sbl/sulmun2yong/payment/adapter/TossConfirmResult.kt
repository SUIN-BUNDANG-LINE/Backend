package com.sbl.sulmun2yong.payment.adapter

// confirm 결과 삼분법 - "타임아웃을 실패로 오인하면 인중결제"를 타입으로 강제한다
sealed class TossConfirmResult {
    // 승인 완료(status=DONE) - 커맨드 SUCCEEDED + 결제 장부 DONE
    data class Approved(
        val paymentKey: String,
    ) : TossConfirmResult()

    // 명시적 거절(카드 한도 등 4xx) - 커맨드 & 장부 FAILED, 재시도 무의미
    data class Rejected(
        val code: String,
        val message: String,
    ) : TossConfirmResult()

    // 미확정(타임아웃 & 5xx & 이미 처리됨) - PENDING 유지, 릴레이가 재시도 & 조회로 수렴
    data object Unknown : TossConfirmResult()
}
