package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import com.sbl.sulmun2yong.survey.entity.Participant
import com.sbl.sulmun2yong.survey.entity.ResponseEntity
import com.sbl.sulmun2yong.survey.exception.AlreadyParticipatedException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.publisher.SurveyResponseEventPublisher
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SurveyResponseService(
    private val surveyRepository: SurveyRepository,
    private val participantRepository: ParticipantRepository,
    private val responseRepository: ResponseRepository,
    private val surveyResponseEventPublisher: SurveyResponseEventPublisher,
) {
    @Transactional
    fun responseToSurvey(
        surveyId: UUID,
        surveyResponseRequest: SurveyResponseRequest,
    ): SurveyParticipantResponse {
        validateIsAlreadyParticipated(surveyId, surveyResponseRequest.visitorId)
        val visitorId = surveyResponseRequest.visitorId
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        val surveyResponse = surveyResponseRequest.toDomain(surveyId)
        survey.validateResponse(surveyResponse)
        val participant = Participant.create(visitorId, surveyId, null)
        participantRepository.save(participant)

        // 설문 응답을 ResponseEntity로 변환하여 저장
        val responseEntities =
            surveyResponse.flatMap { sectionResponse ->
                sectionResponse.flatMap { questionResponse ->
                    questionResponse.map {
                        ResponseEntity(
                            id = UUID.randomUUID(),
                            participantId = participant.id,
                            surveyId = surveyResponse.surveyId,
                            questionId = questionResponse.questionId,
                            content = it.content,
                        )
                    }
                }
            }
        responseRepository.saveAll(responseEntities)

        val currentParticipantCount = participantRepository.findBySurveyId(surveyId).size
        surveyResponseEventPublisher.publishSubmitted(
            surveyId = surveyId,
            participantId = participant.id,
            currentParticipantCount = currentParticipantCount,
            targetParticipantCount = survey.rewardSetting.targetParticipantCount,
        )

        return SurveyParticipantResponse(participant.id, survey.isImmediateDraw())
    }

    private fun validateIsAlreadyParticipated(
        surveyId: UUID,
        visitorId: String,
    ) {
        val participant = participantRepository.findBySurveyIdAndVisitorId(surveyId, visitorId).orElse(null)
        participant?.let {
            throw AlreadyParticipatedException()
        }
    }
}
