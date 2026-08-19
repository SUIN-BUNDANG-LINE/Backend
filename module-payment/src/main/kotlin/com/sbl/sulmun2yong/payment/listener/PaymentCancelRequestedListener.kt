package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.PaymentCancelRequestedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.service.TossApiCallOutboxService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentCancelRequestedListener(
    private val objectMapper: ObjectMapper,
    private val tossApiCallOutboxService: TossApiCallOutboxService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentCancelRequestedListener::class.java)
        private const val CANCEL_REASON = "공동 모금 무산 전액 환불"
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_CANCEL_REQUESTED],
        groupId = "payment-cancel-requested",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentCancelRequestedEvent::class.java)

        val committed =
            tossApiCallOutboxService.commitCancelIntent(
                orderId = event.orderId,
                requestPayload = objectMapper.writeValueAsString(TossCancelRequest(CANCEL_REASON)),
            )

        if (committed) {
            log.info("늦은 결제로 인해 환불 CANCEL이 적재됩니다: orderId={}", event.orderId)
        }
        ack.acknowledge()
    }
}
