package com.sbl.sulmun2yong.survey.dto.event

// drawing-completed Kafka 메시지 → survey 도메인 입력 이벤트 (groupId: drawing-auto-close)
data class DrawingCompletedAutoCloseConsumedEvent(
    val surveyId: String,
    val remainingTickets: Int,
)
