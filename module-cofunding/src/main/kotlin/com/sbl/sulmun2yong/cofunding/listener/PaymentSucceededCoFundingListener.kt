package com.sbl.sulmun2yong.cofunding.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingOutboxKafkaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.PaymentSucceededEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PaymentSucceededCoFundingListener(
    private val objectMapper: ObjectMapper,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val sagaPublisher: CoFundingOutboxKafkaPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentSucceededCoFundingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_SUCCEEDED],
        groupId = "cofunding-payment-succeeded",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentSucceededEvent::class.java)
        val participant = coFundingParticipantRepository.findByTossOrderId(event.orderId)

        if (participant == null) {
            log.warn("참여자 없는 주문의 결제 사실(데이터 이상 - 무시): orderId={}", event.orderId)
            ack.acknowledge()
            return
        }

        val funding =
            coFundingRepository.findByIdForUpdate(participant.fundingId)
                ?: throw CoFundingNotFoundException()
        val now = LocalDateTime.now()

        when (funding.status) {
            CoFundingStatus.FUNDING -> {
                if (participant.status == CoFundingParticipantStatus.REGISTERED) {
                    participant.markPaid(now)
                }
                if (coFundingRepository.tryConfirm(funding.id, now) == 1) {
                    sagaPublisher.publishConfirmed(funding)
                    log.info("공동 모금 장벽 통과 - fundingId={}", funding.id)
                }
            }

            CoFundingStatus.FAILED, CoFundingStatus.REFUNDED -> {
                sagaPublisher.publishCancelRequested(participant.tossOrderId)
                log.warn("무산 후 늦은 결제 - 환불 명령 발행: orderId={}", participant.tossOrderId)
            }

            CoFundingStatus.CONFIRMED -> {
                log.debug("개설 확정 모금의 결제 재전달(무시): orderId={}", participant.tossOrderId)
            }

            CoFundingStatus.PENDING, CoFundingStatus.REJECTED -> {
                log.warn(
                    "판정 전 모금에 결제 도착(무시): orderId={}, status={}",
                    participant.tossOrderId,
                    funding.status,
                )
            }
        }
        ack.acknowledge()
    }
}
