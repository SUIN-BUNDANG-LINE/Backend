package com.sbl.sulmun2yong.notification.dto.event

/**
 * 사가 토픽의 `*.DLT` 를 Kafka 계층에서 받아 도메인 계층(dlt_messages 적재)으로 넘기는 내부 이벤트.
 *
 * SMS DLT([DltSmsNotificationConsumedEvent])와 달리 발송측 봉투가 없다 — 발행측 에러핸들러
 * (DeadLetterPublishingRecoverer)가 원본 레코드를 그대로 재발행하므로, 실패 정보는 kafka_dlt-* 헤더에서 꺼낸다.
 */
data class SagaDltConsumedEvent(
    val eventId: String,
    val originalTopic: String,
    val payload: String,
    val exceptionMessage: String?,
)
