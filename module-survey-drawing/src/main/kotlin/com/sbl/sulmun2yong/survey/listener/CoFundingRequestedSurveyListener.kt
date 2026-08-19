package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingRequestedEvent
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.publisher.SurveyOutboxKafkaPublisher
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

// co-funding-requested 구독 - 설문 서비스의 개설 판정 리스너. surveys 는 설문만 쓴다(단일 기록자).
// 판정 권한이 데이터 소유자(설문)에 있다 - 소유자·상태·경품 설정을 자기 트랜잭션에서 검증한다.
// 승인이면 결제 대기(PENDING_PAYMENT) 보드 생성 + 총액 확정을 한 트랜잭션으로 커밋하고 approved 를,
// 아니면 rejected 를 회신한다. 설문 행은 건드리지 않는다 -
// 결제 대기 상태는 산 물건(보드)이 지고, 보드의 존재가 곧 설문 수정 잠금이다.
// 보드 생성이 실패하면 승인 자체가 롤백된다 - 미회신 PENDING 은 기한 스케줄러가 REJECTED 로 종착시킨다.
// 재전달 안전: 보드 생성이 멱등(findBySurveyId)이라 approved 재발행 - 모금 쪽 PENDING 가드가 흡수한다.
@Component
class CoFundingRequestedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val surveyOutboxKafkaPublisher: SurveyOutboxKafkaPublisher,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingRequestedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_REQUESTED],
        groupId = "survey-cofunding-requested",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingRequestedEvent::class.java)
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(
                    UUID.fromString(event.surveyId),
                    UUID.fromString(event.ownerId),
                ).orElse(null)

        val rewardSetting = survey?.rewardSetting
        when {
            survey == null -> {
                surveyOutboxKafkaPublisher.publishRejectedReview(
                    event.fundingId,
                    event.surveyId,
                    "설문이 없거나 개설자 소유가 아님",
                )
                log.info("모금 개설 거절: fundingId={}, reason={}", event.fundingId, "설문이 없거나 개설자 소유가 아님")
            }

            rewardSetting !is ImmediateDrawSetting -> {
                surveyOutboxKafkaPublisher.publishRejectedReview(
                    event.fundingId,
                    event.surveyId,
                    "경품 설문이 아님",
                )
                log.info("모금 개설 거절: fundingId={}, reason={}", event.fundingId, "경품 설문이 아님")
            }

            survey.status == SurveyStatus.NOT_STARTED -> {
                val totalAmount = rewardUnitPrice * rewardSetting.rewards.sumOf { it.count }
                val board =
                    drawingBoardRepository.findBySurveyId(survey.id).orElseGet {
                        drawingBoardRepository.save(
                            DrawingBoard.create(
                                surveyId = survey.id,
                                boardSize = rewardSetting.targetParticipantCount,
                                rewards = rewardSetting.rewards,
                            ),
                        )
                    }
                surveyOutboxKafkaPublisher.publishApprovedReview(
                    event.fundingId,
                    event.surveyId,
                    board.id.toString(),
                    totalAmount,
                )
                log.info("모금 개설 승인: fundingId={}, totalAmount={}", event.fundingId, totalAmount)
            }

            else -> {
                surveyOutboxKafkaPublisher.publishRejectedReview(
                    event.fundingId,
                    event.surveyId,
                    "이미 시작·종료된 설문 (status=${survey.status})",
                )
            }
        }
        ack.acknowledge()
    }
}
