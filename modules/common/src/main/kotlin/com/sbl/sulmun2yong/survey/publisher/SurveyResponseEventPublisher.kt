package com.sbl.sulmun2yong.survey.publisher

import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

// survey 도메인의 Kafka 발행 단일 진입점.
// `SurveyResponseService` 등 비즈니스 로직은 이 컴포넌트에만 의존한다.
@Component
class SurveyResponseEventPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun publishSubmitted(
        surveyId: UUID,
        participantId: UUID,
        currentParticipantCount: Int,
        targetParticipantCount: Int?,
    ) {
        val event =
            SurveyResponseSubmittedEvent(
                eventId = UUID.randomUUID().toString(),
                surveyId = surveyId.toString(),
                participantId = participantId.toString(),
                currentParticipantCount = currentParticipantCount,
                targetParticipantCount = targetParticipantCount,
                timestamp = Instant.now(),
            )

        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "Survey",
                aggregateId = event.surveyId,
                eventType = "SurveyResponseSubmitted",
                kafkaTopic = KafkaTopics.SURVEY_RESPONSE_SUBMITTED,
                event = event,
            )

        outboxEventRepository.save(outboxEvent)

        applicationEventPublisher.publishEvent(
            OutboxPublishEvent(
                outboxId = outboxEvent.id,
                topic = outboxEvent.kafkaTopic,
                key = outboxEvent.kafkaKey,
                payload = outboxEvent.kafkaPayload,
            ),
        )
    }
}
