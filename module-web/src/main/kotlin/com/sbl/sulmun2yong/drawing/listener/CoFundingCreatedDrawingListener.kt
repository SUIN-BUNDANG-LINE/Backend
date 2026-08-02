package com.sbl.sulmun2yong.drawing.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingCreatedEvent
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ② co-funding-created 구독 - 추첨 서비스의 리스너. drawing_boards 는 추첨만 쓴다(단일 기록자).
// 개설 tx 가 하던 경품 보드 사전 생성을 대체한다. 보드 크기·경품은 설문에서 읽는다(교차 읽기 허용).
// 재전달 안전: findBySurveyId 선조회로 멱등.
@Component
class CoFundingCreatedDrawingListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingCreatedDrawingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CREATED],
        groupId = "drawing-cofunding-created",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingCreatedEvent::class.java)
        val surveyId = UUID.fromString(event.surveyId)
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElse(null)

        if (survey == null) {
            log.warn("개설 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
        } else {
            drawingBoardRepository.findBySurveyId(surveyId).orElseGet {
                drawingBoardRepository.save(
                    DrawingBoard.create(
                        surveyId = surveyId,
                        boardSize = survey.rewardSetting.targetParticipantCount!!,
                        rewards = survey.rewardSetting.rewards,
                    ),
                )
            }
        }
        ack.acknowledge()
    }
}
