package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingConfirmedEvent
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardStatus
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class CoFundingConfirmedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingConfirmedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CONFIRMED],
        groupId = "survey-cofunding-confirmed",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingConfirmedEvent::class.java)
        val surveyId = UUID.fromString(event.surveyId)
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElse(null)
        val board = drawingBoardRepository.findBySurveyId(surveyId).orElse(null)

        when {
            survey == null -> {
                log.warn("개설 확정 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
            }

            survey.status == SurveyStatus.NOT_STARTED &&
                board?.status == DrawingBoardStatus.PENDING_PAYMENT -> {
                board.activate()
                surveyRepository.save(survey.start())
                log.info(
                    "공동 모금 개설 확정 - 보드 활성 + 설문 개시: fundingId={}, surveyId={}",
                    event.fundingId,
                    event.surveyId,
                )
            }

            else -> {
                log.debug(
                    "활성화 대상 아님(멱등 스킵): surveyId={}, status={}, board={}",
                    event.surveyId,
                    survey.status,
                    board?.status,
                )
            }
        }
        ack.acknowledge()
    }
}
