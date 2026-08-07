package com.sbl.sulmun2yong.cofunding.dto.event

/**
 * `co-funding-failed`(groupId: cofunding-failed-notification)를 Kafka 계층에서 받아
 * 도메인 계층(SMS 잡 적재)으로 넘기는 내부 이벤트.
 *
 * DLT 재발행·발송 시 원본이 필요하므로 [rawPayload] 에 소비한 원문을 그대로 실어 보낸다.
 */
data class CoFundingFailedNotificationConsumedEvent(
    val eventId: String,
    val fundingId: String,
    val rawPayload: String,
)
