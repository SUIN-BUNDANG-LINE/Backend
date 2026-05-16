package com.sbl.sulmun2yong.notification.dto.event

// SMS 알림 잡이 INSERT 된 직후 Worker(@Async listener)를 트리거하기 위한 도메인 후속 이벤트
data class SmsJobCreatedEvent(
    val jobId: Long,
)
