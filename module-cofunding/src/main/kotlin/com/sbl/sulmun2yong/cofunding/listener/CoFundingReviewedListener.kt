package com.sbl.sulmun2yong.cofunding.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingReviewedEvent
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingOutboxKafkaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

// 설문 판정 결과 구독 - 접수(PENDING)를 모금(FUNDING) 또는 REJECTED 로 확정한다.
// 승인이면 실어 온 총액으로 분담금을 확정하고 같은 tx 에서 ② co-funding-created 를 발행한다
// (주문 발급·보드 생성은 ② 구독자 몫 - 전이·금액·발행이 원자라 유실 창이 없다).
// 거절이면 결제자가 없는 상태의 종착이라 보상 없이 상태 전이 하나로 끝난다.
// 재전달 안전: findByIdForUpdate 행 잠금 + PENDING 가드가 멱등을 보장한다.
@Component
class CoFundingReviewedListener(
    private val objectMapper: ObjectMapper,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val coFundingOutboxKafkaPublisher: CoFundingOutboxKafkaPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingReviewedListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_REVIEWED],
        groupId = "cofunding-cofunding-reviewed",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingReviewedEvent::class.java)
        val funding = coFundingRepository.findByIdForUpdate(UUID.fromString(event.fundingId))

        when {
            funding == null -> {
                log.warn("판정 이벤트의 모금 없음(무시): fundingId={}", event.fundingId)
            }

            funding.status != CoFundingStatus.PENDING -> {
                log.debug(
                    "이미 판정된 모금(멱등 스킵): fundingId={}, status={}",
                    event.fundingId,
                    funding.status,
                )
            }

            event.verdict == CoFundingReviewedEvent.Verdict.APPROVED -> {
                // 승인인데 총액이 없으면 계약 위반 - 시스템 오류로 전파해 DLT 회로에 태운다
                val totalAmount =
                    event.totalAmount
                        ?: throw IllegalStateException("승인 판정에 totalAmount 누락: fundingId=${event.fundingId}")
                // 판정 tx 가 세운 결제 대기 보드의 ID - 해석 없이 ③으로 반사만 한다(주문이 가리킬 물건)
                val boardId =
                    event.boardId
                        ?: throw IllegalStateException("승인 판정에 boardId 누락: fundingId=${event.fundingId}")

                funding.approve(totalAmount)
                val participants = coFundingParticipantRepository.findAllByFundingId(funding.id)
                coFundingOutboxKafkaPublisher.publishCreated(funding, participants, boardId)
                log.info("모금 개설 확정: fundingId={}, totalAmount={}", event.fundingId, totalAmount)
            }

            else -> {
                funding.markRejected()
                log.info("모금 개설 거절 종착: fundingId={}, reason={}", event.fundingId, event.reason)
            }
        }
        ack.acknowledge()
    }

}
