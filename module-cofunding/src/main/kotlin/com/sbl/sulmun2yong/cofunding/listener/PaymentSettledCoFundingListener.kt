package com.sbl.sulmun2yong.cofunding.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingSagaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.PaymentSettledEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// ④ payment-settled 구독 - 모금 서비스의 정산 리스너. participants·co_fundings 는 모금만 쓴다(단일 기록자).
// 결제 settle tx 가 하던 participant SETTLED 전이·장벽 판정·늦은 결제 판정을 대체한다.
//
// "무산 확정 vs 결제 확정" 경합: findByIdForUpdate(자기 테이블 행 잠금)가 기한 스케줄러의
// tryFail 과 직렬화한다 - 예전엔 결제가 남의 테이블을 잠갔지만, 이제 모금 로컬 tx 안의 잠금이다.
// 장벽 CAS(tryConfirm) 승자만 ⑤ co-funding-confirmed 를 발행한다(설문 활성화는 설문 리스너 몫).
// 재전달 안전: REGISTERED 가드·CAS 패배 no-op 이 곧 멱등이다.
@Component
class PaymentSettledCoFundingListener(
    private val objectMapper: ObjectMapper,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val sagaPublisher: CoFundingSagaPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentSettledCoFundingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_SETTLED],
        groupId = "cofunding-payment-settled",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentSettledEvent::class.java)
        val participant = coFundingParticipantRepository.findByTossOrderId(event.orderId)

        if (participant == null) {
            log.debug("단독 결제 건 - 모금 정산 스킵: orderId={}", event.orderId)
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
                    participant.settle(now)
                }
                // 1 = 마지막 결제를 확인해 FUNDING -> CONFIRMED 를 만든 유일한 승자 - 승자만 ⑤ 발행
                if (coFundingRepository.tryConfirm(funding.id, now) == 1) {
                    sagaPublisher.publishConfirmed(funding)
                    log.info("공동 모금 장벽 통과 - fundingId={}", funding.id)
                }
            }

            CoFundingStatus.FAILED, CoFundingStatus.REFUNDED -> {
                // 늦은 결제 - SETTLED 기록 없이 ⑧ 단건 환불 명령 발행 (CANCEL 적재는 결제 리스너 몫)
                sagaPublisher.publishCancelRequested(participant.tossOrderId)
                log.warn("무산 후 늦은 결제 - 환불 명령 발행: orderId={}", participant.tossOrderId)
            }

            CoFundingStatus.CONFIRMED -> {
                // 재전달/중복 - 이미 장벽 통과, no-op 이 곧 멱등
                log.debug("개설 확정 모금의 결제 재전달(무시): orderId={}", participant.tossOrderId)
            }
        }
        ack.acknowledge()
    }
}
