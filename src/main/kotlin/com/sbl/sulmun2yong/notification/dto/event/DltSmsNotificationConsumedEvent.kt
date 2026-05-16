package com.sbl.sulmun2yong.notification.dto.event

// drawing-notification.DLT Kafka 메시지 → notification 도메인 입력 이벤트 (groupId: dlt-sms-notification)
data class DltSmsNotificationConsumedEvent(
    val event: DltSmsNotificationEvent,
)
