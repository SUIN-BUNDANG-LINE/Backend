package com.sbl.sulmun2yong.global.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.newrelic.agent.deps.io.grpc.okhttp.internal.Platform.logger
import com.sbl.sulmun2yong.drawing.dto.event.WinningEvent
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${kafka.topics.survey-response}") private val surveyResponseTopic: String,
    @Value("\${kafka.topics.winning}") private val winnerTopic: String,
) {
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())

    fun publishSurveyResponse(event: SurveyResponseEvent) {
        val payload = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(surveyResponseTopic, event.participantId.toString(), payload)
        logger.info("$surveyResponseTopic: $event")
    }

    fun publishWinner(winningEvent: WinningEvent) {
        val payload = objectMapper.writeValueAsString(winningEvent)
        kafkaTemplate.send(winnerTopic, winningEvent.drawingHistoryId.toString(), payload)
        logger.info("$winnerTopic: $winningEvent")
    }
}
