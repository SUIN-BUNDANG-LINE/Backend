package com.sbl.sulmun2yong.drawing.dto.event

/**
 * `drawing-completed`(groupId: drawing-notification)를 Kafka 계층에서 받아 도메인 계층으로
 * 넘기는 내부 이벤트.
 *
 * DLT 재발행에 원본이 필요하므로 [rawPayload] 에 소비한 원문을 그대로 실어 보낸다.
 */
data class DrawingCompletedNotificationConsumedEvent(
    val eventId: String,
    val surveyId: String,
    val isWinner: Boolean,
    val rawPayload: String,
)
