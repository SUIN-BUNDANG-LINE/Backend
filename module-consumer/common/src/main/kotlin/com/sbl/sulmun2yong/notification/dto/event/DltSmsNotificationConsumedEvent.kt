package com.sbl.sulmun2yong.notification.dto.event

/**
 * `drawing-notification.DLT`(groupId: dlt-sms-notification) 메시지를 도메인 계층으로 넘기는 내부 이벤트.
 *
 * 죽은 편지 큐에 쌓인 원본 실패 메시지 [event] 를 감싸, DltMessageEventListener 가 영구 저장하도록 한다.
 */
data class DltSmsNotificationConsumedEvent(
    val event: DltSmsNotificationEvent,
)
