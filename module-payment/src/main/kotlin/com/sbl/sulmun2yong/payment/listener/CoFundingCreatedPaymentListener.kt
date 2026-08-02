package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingCreatedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ② co-funding-created 구독 - 결제 서비스의 주문 발급 리스너. payment_orders 는 결제만 쓴다(단일 기록자).
// 개설 tx 가 하던 참여자별 주문 사전 발급을 대체한다 - orderId·amount 는 모금이 확정해 페이로드로 준 값.
// 재전달 안전: tossOrderId 선조회(+UNIQUE 제약이 최종 방어)로 멱등.
@Component
class CoFundingCreatedPaymentListener(
    private val objectMapper: ObjectMapper,
    private val paymentOrderRepository: PaymentOrderRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingCreatedPaymentListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CREATED],
        groupId = "payment-cofunding-created",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingCreatedEvent::class.java)
        val surveyId = UUID.fromString(event.surveyId)

        event.participants.forEach { participant ->
            if (paymentOrderRepository.findByTossOrderId(participant.orderId).isEmpty) {
                paymentOrderRepository.save(
                    PaymentOrder.create(
                        surveyId = surveyId,
                        makerId = UUID.fromString(participant.userId),
                        orderId = participant.orderId,
                        amount = participant.amount,
                    ),
                )
            } else {
                log.debug("이미 발급된 주문(멱등 스킵): orderId={}", participant.orderId)
            }
        }
        log.info("모금 주문 발급 - fundingId={}, 참여자 {}명", event.fundingId, event.participants.size)
        ack.acknowledge()
    }
}
