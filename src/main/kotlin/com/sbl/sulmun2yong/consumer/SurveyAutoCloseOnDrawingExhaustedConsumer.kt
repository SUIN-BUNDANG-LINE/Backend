package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.consumer.repository.SurveyConsumerRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class SurveyAutoCloseOnDrawingExhaustedConsumer(
    private val surveyConsumerRepository: SurveyConsumerRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log =
            LoggerFactory.getLogger(SurveyAutoCloseOnDrawingExhaustedConsumer::class.java)
    }

    @KafkaListener(
        topics = ["drawing-completed"],
        groupId = "drawing-auto-close",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)

        if (event.remainingTickets > 0) {
            ack.acknowledge()
            return
        }

        val survey =
            surveyConsumerRepository
                .findByIdAndIsDeletedFalseWithLock(UUID.fromString(event.surveyId))
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, surveyId: {}", survey.id)
            ack.acknowledge()
            return
        }

        surveyConsumerRepository.save(survey.finish())
        log.info("티켓 소진으로 종료, surveyId: {}", survey.id)

        ack.acknowledge()
    }
}
