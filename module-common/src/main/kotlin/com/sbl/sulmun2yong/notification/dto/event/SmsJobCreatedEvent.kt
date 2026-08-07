package com.sbl.sulmun2yong.notification.dto.event

/**
 * SMS 알림 잡이 INSERT 된 직후, 비동기 발송을 트리거하기 위한 도메인 후속 이벤트.
 *
 * DrawingSmsNotificationEventListener 가 잡 저장 후 발행하고,
 * SmsJobEventListener 가 AFTER_COMMIT + @Async 로 받아 즉시 발송을 시도한다.
 */
data class SmsJobCreatedEvent(
    val jobId: Long,
)
