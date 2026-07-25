package com.sbl.sulmun2yong.payment.service

// success 결과 — 컨트롤러가 어느 결과 페이지로 보낼지 결정하는 재료
enum class ConfirmOutcome {
    // 승인 확정 — 완료 화면
    DONE,

    // 미확정 — 확인 중 화면 (릴레이가 이어받음)
    PROCESSING,

    // 거절·검증 실패 — 실패 화면
    FAILED,
}
