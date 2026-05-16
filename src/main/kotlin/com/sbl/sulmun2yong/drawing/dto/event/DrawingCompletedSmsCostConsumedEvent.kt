package com.sbl.sulmun2yong.drawing.dto.event

// drawing-completed Kafka 메시지 → drawing 도메인 입력 이벤트 (groupId: sms-cost-calculator)
// 동일 토픽을 별도 groupId로 fan-out 구독하여 SMS 발송 비용을 누적 집계한다.
data class DrawingCompletedSmsCostConsumedEvent(
    val eventId: String,
    val surveyId: String,
    val isWinner: Boolean,
)
