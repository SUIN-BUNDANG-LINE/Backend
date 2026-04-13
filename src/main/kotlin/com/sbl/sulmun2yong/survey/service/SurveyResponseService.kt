package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedEvent
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import com.sbl.sulmun2yong.survey.exception.AlreadyParticipatedException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SurveyResponseService(
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val responseAdapter: ResponseAdapter,
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun responseToSurvey(
        surveyId: UUID,
        surveyResponseRequest: SurveyResponseRequest,
    ): SurveyParticipantResponse {
        validateIsAlreadyParticipated(surveyId, surveyResponseRequest.visitorId)
        val visitorId = surveyResponseRequest.visitorId
        val survey = surveyAdapter.getSurvey(surveyId)
        val surveyResponse = surveyResponseRequest.toDomain(surveyId)
        survey.validateResponse(surveyResponse)
        val participant = Participant.create(visitorId, surveyId, null)
        participantAdapter.insert(participant)
        responseAdapter.insertSurveyResponse(surveyResponse, participant.id)

        publishResponseSubmittedEvent(surveyId, participant.id, survey)

        return SurveyParticipantResponse(participant.id, survey.isImmediateDraw())
    }

    private fun publishResponseSubmittedEvent(
        surveyId: UUID,
        participantId: UUID,
        survey: Survey,
    ) {
        // 1. 현재 참여자 수 조회
        val participants = participantAdapter.findBySurveyId(surveyId)
        // 2. 이벤트 DTO 생성
        val surveyResponseSubmittedEvent =
            SurveyResponseSubmittedEvent(
                eventId = UUID.randomUUID().toString(),
                surveyId = surveyId.toString(),
                participantId = participantId.toString(),
                currentParticipantCount = participants.size,
                targetParticipantCount = survey.rewardSetting.targetParticipantCount,
                timestamp = Instant.now(),
            )
        // 3. OutboxEventFactory로 Outbox 엔티티 생성
        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "Survey",
                aggregateId = surveyResponseSubmittedEvent.surveyId,
                eventType = "SurveyResponseSubmitted",
                kafkaTopic = "survey-response-submitted",
                event = surveyResponseSubmittedEvent,
            )
        // 4. Repository 저장
        outboxEventRepository.save(outboxEvent)
        // 5. ApplicationEventPublisher로 OutboxPublishEvent 발행
        applicationEventPublisher.publishEvent(
            OutboxPublishEvent(
                outboxId = outboxEvent.id,
                topic = outboxEvent.kafkaTopic,
                key = outboxEvent.kafkaKey,
                payload = outboxEvent.kafkaPayload,
            ),
        )
    }

    private fun validateIsAlreadyParticipated(
        surveyId: UUID,
        visitorId: String,
    ) {
        val participant = participantAdapter.findBySurveyIdAndVisitorId(surveyId, visitorId)
        participant?.let {
            throw AlreadyParticipatedException()
        }
    }
}
