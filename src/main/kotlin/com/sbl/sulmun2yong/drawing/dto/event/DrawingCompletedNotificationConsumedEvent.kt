package com.sbl.sulmun2yong.drawing.dto.event

// drawing-completed Kafka 메시지 → drawing 도메인 입력 이벤트 (groupId: drawing-notification)
data class DrawingCompletedNotificationConsumedEvent(
    val eventId: String,
    val surveyId: String,
    val isWinner: Boolean,
    val rawPayload: String,
)
