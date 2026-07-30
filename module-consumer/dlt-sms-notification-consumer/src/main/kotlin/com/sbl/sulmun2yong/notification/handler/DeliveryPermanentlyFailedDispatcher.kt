package com.sbl.sulmun2yong.notification.handler

import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent

/**
 * SMS 발송이 영구 실패했음을 사가 하류로 알리는 **영구 실패 신호의 발행 계약**.
 *
 * DLT(죽은 편지 큐) 도달 = 재시도 소진에 따른 영구 실패로 간주한다(FR-007). 이 인터페이스는
 * "영구 실패 신호를 내보낸다"는 **무엇을**만 규정하고, **어떻게**(Kafka 발행)는
 * 구현체([KafkaDeliveryPermanentlyFailedDispatcher])에 위임한다.
 *
 * 비용 컨슈머는 이 신호를 소비해 해당 건의 비용을 취소(REVERSED)한다.
 * notification 모듈의 [com.sbl.sulmun2yong.notification.handler.DeliveryConfirmedDispatcher] 와 대칭인 어댑터다.
 * @see KafkaDeliveryPermanentlyFailedDispatcher `sms-delivery-permanently-failed` 토픽으로 발행하는 구현체
 * @see com.sbl.sulmun2yong.notification.dto.event.SmsDeliveryPermanentlyFailedEvent 발행되는 신호의 payload 스키마
 */
interface DeliveryPermanentlyFailedDispatcher {
    /**
     * DLT로 넘어간 실패 메시지를 받아, 영구 실패 신호를 하류로 발행한다.
     *
     * @param event DLT 봉투. 원본 drawing-completed JSON(`payload`)에서 상관 키와
     *              참여자/설문 식별자를 파싱해 신호를 조립한다.
     */
    fun dispatch(event: DltSmsNotificationEvent)
}
