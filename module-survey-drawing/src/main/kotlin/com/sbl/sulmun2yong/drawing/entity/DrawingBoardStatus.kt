package com.sbl.sulmun2yong.drawing.entity

// 값을 치르는 대상(경품 보드)이 결제 대기 상태를 진다 - 설문은 결제를 모른다.
// PENDING_PAYMENT: 개시 요청으로 생성됐고 대금 확정을 기다린다 - 이 보드가 살아 있는 동안 설문 수정이 잠긴다.
// ACTIVE: 대금 확정 - 설문 활성화와 같은 tx 에서 전이된다.
enum class DrawingBoardStatus {
    PENDING_PAYMENT,
    ACTIVE,
}
