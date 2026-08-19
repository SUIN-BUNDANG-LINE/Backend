package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingExpiredEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.service.TossApiCallOutboxService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CoFundingExpiredPaymentListener(
    private val objectMapper: ObjectMapper,
    private val tossApiCallOutboxService: TossApiCallOutboxService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingExpiredPaymentListener::class.java)
        private const val CANCEL_REASON = "공동 모금 무산 전액 환불"
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_EXPIRED],
        groupId = "payment-cofunding-expired",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingExpiredEvent::class.java)

        val enqueued =
            event.paidOrderIds.count { orderId ->
                tossApiCallOutboxService.commitCancelIntent(
                    orderId = orderId,
                    requestPayload = objectMapper.writeValueAsString(TossCancelRequest(CANCEL_REASON)),
                )
            }

        log.info(
            "환불 팬아웃 - fundingId={}, 대상 {}건 중 {}건 적재",
            event.fundingId,
            event.paidOrderIds.size,
            enqueued,
        )
        ack.acknowledge()
    }
}
