package com.sbl.sulmun2yong.notification.dto.event

import java.time.Instant

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
