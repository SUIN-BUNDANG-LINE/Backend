package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedEvent
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

// ⑥ co-funding-failed 구독 - 결제 서비스의 무산 환불 팬아웃 리스너(구 co-funding-consumer 대체).
// settledOrderIds 스냅샷 각각에 CANCEL 을 적재한다. 스냅샷은 스케줄러의 행 잠금 후 조회라 완전하고,
// 스냅샷 이후의 늦은 결제는 모금 ④ 리스너가 ⑧ payment-cancel-requested 로 잡는다 - 재조회 불필요.
// 결제자 0명 무산은 스케줄러가 직접 종착하므로 여기엔 오지 않는다(빈 배열이어도 no-op 로 무해).
// 이중 환불의 최종 방어는 UNIQUE(aggregate_id, command_type).
@Component
class CoFundingFailedPaymentListener(
    private val objectMapper: ObjectMapper,
    private val paymentCommandRepository: PaymentCommandRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingFailedPaymentListener::class.java)
        private const val CANCEL_REASON = "공동 모금 무산 전액 환불"
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_FAILED],
        groupId = "payment-cofunding-failed",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingFailedEvent::class.java)

        val enqueued =
            event.settledOrderIds.count { orderId ->
                if (paymentCommandRepository.existsByAggregateIdAndCommandType(
                        orderId,
                        PaymentCommandType.CANCEL,
                    )
                ) {
                    false
                } else {
                    paymentCommandRepository.save(
                        PaymentCommand.create(
                            commandType = PaymentCommandType.CANCEL,
                            aggregateId = orderId,
                            requestPayload = objectMapper.writeValueAsString(TossCancelRequest(CANCEL_REASON)),
                        ),
                    )
                    true
                }
            }

        log.info("환불 팬아웃 - fundingId={}, 대상 {}건 중 {}건 적재", event.fundingId, event.settledOrderIds.size, enqueued)
        ack.acknowledge()
    }
}
