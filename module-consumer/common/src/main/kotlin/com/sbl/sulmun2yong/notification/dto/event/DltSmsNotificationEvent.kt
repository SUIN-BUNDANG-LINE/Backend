package com.sbl.sulmun2yong.notification.dto.event

import java.time.Instant

/**
 * `drawing-notification.DLT` 토픽 payload 스키마 — 죽은 편지 큐로 넘어간 실패 메시지의 봉투.
 *
 * notification 컨슈머의 DltDispatcher 가 재시도(MAX_RETRY)를 소진한 SMS 잡을 이 형태로 발행하고,
 * dlt 컨슈머가 소비해 원본 payload 와 실패 사유를 함께 영구 저장한다.
 */
data class DltSmsNotificationEvent(
    // 원본 정보
    val eventId: String,
    val payload: String,
    val notificationType: String,
    // 실패 정보
    val retryCount: Int,
    val lastError: String?,
    val failedAt: Instant,
)
