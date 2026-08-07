package com.sbl.sulmun2yong.notification.dto.event

import java.time.Instant

/**
 * `sms-delivery-permanently-failed` 토픽 payload 스키마 — SMS 발송이 영구 실패했다는 사가 하류 신호.
 *
 * dlt 컨슈머가 죽은 편지 큐 도달(재시도 소진) 시 발행한다.
 * 하류 소비자와의 상관관계는 [originalDrawingEventId](원본 drawing-completed 이벤트 ID)로 맞춘다.
 */
data class SmsDeliveryPermanentlyFailedEvent(
    val eventId: String,
    val originalDrawingEventId: String,
    val participantId: String,
    val surveyId: String,
    val errorCode: String,
    val failedAt: Instant,
)
