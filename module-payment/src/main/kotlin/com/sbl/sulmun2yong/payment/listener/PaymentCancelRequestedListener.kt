package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.PaymentCancelRequestedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.repository.PaymentCommandRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// ⑧ payment-cancel-requested 구독 - 결제 서비스의 늦은 결제 환불 리스너.
// payment_commands 는 결제만 쓴다(단일 기록자) - 모금이 명령을 발행하고 결제가 CANCEL 을 적재한다.
// 이중 환불의 최종 방어는 UNIQUE(aggregate_id, command_type) - 사전 exists 검사가 흔한 중복을
// 조용히 흡수하고, 검사 틈새의 동시 삽입은 UNIQUE 위반 -> tx 롤백 -> 재전달의 exists 에서 수렴한다.
@Component
class PaymentCancelRequestedListener(
    private val objectMapper: ObjectMapper,
    private val paymentCommandRepository: PaymentCommandRepository,
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

        if (paymentCommandRepository.existsByAggregateIdAndCommandType(
                event.orderId,
                PaymentCommandType.CANCEL,
            )
        ) {
            log.debug("이미 적재된 CANCEL(멱등 스킵): orderId={}", event.orderId)
        } else {
            paymentCommandRepository.save(
                PaymentCommand.create(
                    commandType = PaymentCommandType.CANCEL,
                    aggregateId = event.orderId,
                    requestPayload = objectMapper.writeValueAsString(TossCancelRequest(CANCEL_REASON)),
                ),
            )
            log.warn("늦은 결제 환불 CANCEL 적재: orderId={}", event.orderId)
        }
        ack.acknowledge()
    }
}
