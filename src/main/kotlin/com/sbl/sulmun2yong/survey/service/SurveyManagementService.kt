package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingHistoryAdapter
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.dto.request.SurveyResultRequest
import com.sbl.sulmun2yong.survey.dto.response.ParticipantsInfoListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyRawResultResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyAccessException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyManagementService(
    private val responseAdapter: ResponseAdapter,
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val drawingHistoryAdapter: DrawingHistoryAdapter,
) {
    fun getSurveyResult(
        surveyId: UUID,
        makerId: UUID?,
        surveyResultRequest: SurveyResultRequest,
        participantId: UUID?,
        visitorId: String?,
    ): SurveyResultResponse {
        val survey = surveyAdapter.getSurvey(surveyId)
        // visitorId가 있으면 참가자인지 확인, 없으면 makerId를 확인
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        // DB에서 설문 결과 조회
        val surveyResult = responseAdapter.getSurveyResult(surveyId, participantId)

        // 요청에 따라 설문 결과 필터링
        val resultFilter = surveyResultRequest.toDomain()
        val filteredSurveyResult = surveyResult.getFilteredResult(resultFilter)

        val participantCount =
            if (resultFilter.questionFilters.isEmpty()) {
                // 필터를 걸지 않은 경우는 Participant Document에서 참가자 수 조회
                participantAdapter.findBySurveyId(surveyId).size
            } else {
                // 필터를 건 경우는 필터링된 결과 수로 참가자 수 조회
                surveyResult.getParticipantCount()
            }

        return SurveyResultResponse.of(filteredSurveyResult, survey, participantCount)
    }

    fun getSurveyParticipants(
        surveyId: UUID,
        makerId: UUID?,
        visitorId: String?,
    ): ParticipantsInfoListResponse {
        val survey = surveyAdapter.getSurvey(surveyId)
        // visitorId가 있으면 참가자인지 확인, 없으면 makerId를 확인
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        val participants = participantAdapter.findBySurveyId(surveyId)
        // 즉시 추첨이고, visitorId가 없는 경우에만 추첨 이력 조회
        val drawingHistories =
            if (survey.isImmediateDraw() &&
                visitorId == null
            ) {
                drawingHistoryAdapter.getBySurveyId(surveyId, false)
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
        val survey = surveyAdapter.getSurvey(surveyId)
        // visitorId가 있으면 참가자인지 확인, 없으면 makerId를 확인
        if (!isValidRequest(survey, makerId, visitorId)) throw InvalidSurveyAccessException()

        val surveyResult = responseAdapter.getSurveyResult(surveyId, null)
        val participants = participantAdapter.findBySurveyId(surveyId)

        return SurveyRawResultResponse.of(survey, surveyResult, participants)
    }

    private fun isValidRequest(
        survey: Survey,
        makerId: UUID?,
        visitorId: String?,
    ): Boolean =
        // visitorId가 있고, 결과 공개가 되어있는 경우 참가자인지 확인
        if (visitorId != null && survey.isResultOpen) {
            val participant = participantAdapter.findBySurveyIdAndVisitorId(survey.id, visitorId)
            participant != null
        } else {
            // 아닌 경우 설문 제작자인지 확인
            makerId != null && survey.makerId == makerId
        }
}
