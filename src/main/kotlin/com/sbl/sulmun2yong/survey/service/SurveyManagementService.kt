package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.survey.domain.result.QuestionResult
import com.sbl.sulmun2yong.survey.domain.result.ResultDetails
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import com.sbl.sulmun2yong.survey.dto.request.SurveyResultRequest
import com.sbl.sulmun2yong.survey.dto.response.ParticipantsInfoListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyRawResultResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import com.sbl.sulmun2yong.survey.entity.ResponseEntity
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyAccessException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyManagementService(
    private val surveyRepository: SurveyRepository,
    private val responseRepository: ResponseRepository,
    private val participantRepository: ParticipantRepository,
    private val drawingHistoryRepository: DrawingHistoryRepository,
) {
    fun getSurveyResult(
        surveyId: UUID,
        makerId: UUID?,
        surveyResultRequest: SurveyResultRequest,
        participantId: UUID?,
        visitorId: String?,
    ): SurveyResultResponse {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        val surveyResult = getSurveyResult(surveyId, participantId)

        val resultFilter = surveyResultRequest.toDomain()
        val filteredSurveyResult = surveyResult.getFilteredResult(resultFilter)

        val participantCount =
            if (resultFilter.questionFilters.isEmpty()) {
                participantRepository.findBySurveyId(surveyId).size
            } else {
                surveyResult.getParticipantCount()
            }

        return SurveyResultResponse.of(filteredSurveyResult, survey, participantCount)
    }

    fun getSurveyParticipants(
        surveyId: UUID,
        makerId: UUID?,
        visitorId: String?,
    ): ParticipantsInfoListResponse {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        val participants = participantRepository.findBySurveyId(surveyId)
        val drawingHistories =
            if (survey.isImmediateDraw() && visitorId == null) {
                val histories = drawingHistoryRepository.findBySurveyId(surveyId)
                DrawingHistoryGroup(surveyId, histories.size, histories)
            } else {
                null
            }
        return ParticipantsInfoListResponse.of(participants, drawingHistories, survey.rewardSetting.targetParticipantCount)
    }

    fun getSurveyRawResult(
        surveyId: UUID,
        makerId: UUID?,
        visitorId: String?,
    ): SurveyRawResultResponse {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        val surveyResult = getSurveyResult(surveyId, null)
        val participants = participantRepository.findBySurveyId(surveyId)

        return SurveyRawResultResponse.of(survey, surveyResult, participants)
    }

    private fun getSurveyResult(
        surveyId: UUID,
        participantId: UUID?,
    ): SurveyResult {
        val responses =
            if (participantId != null) {
                responseRepository.findBySurveyIdAndParticipantId(surveyId, participantId)
            } else {
                responseRepository.findBySurveyId(surveyId)
            }
        val groupingResponses = responses.groupBy { it.questionId }.values
        return SurveyResult(questionResults = groupingResponses.map { it.toQuestionResult() })
    }

    private fun List<ResponseEntity>.toQuestionResult() =
        QuestionResult(
            questionId = first().questionId,
            resultDetails =
                this.groupBy { it.participantId }.map {
                    ResultDetails(
                        participantId = it.key,
                        contents = it.value.map { entity -> entity.content },
                    )
                },
            contents = this.map { it.content }.toSortedSet(),
        )

    private fun isValidRequest(
        survey: Survey,
        makerId: UUID?,
        visitorId: String?,
    ): Boolean =
        if (visitorId != null && survey.isResultOpen) {
            val participant = participantRepository.findBySurveyIdAndVisitorId(survey.id, visitorId).orElse(null)
            participant != null
        } else {
            makerId != null && survey.makerId == makerId
        }
}
