package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.request.SurveyResultRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyResultService(
    private val responseAdapter: ResponseAdapter,
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
) {
    fun getSurveyResult(
        surveyId: UUID,
        makerId: UUID,
        surveyResultRequest: SurveyResultRequest,
        participantId: UUID?,
    ): SurveyResultResponse {
        val survey = surveyAdapter.getByIdAndMakerId(surveyId, makerId)

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
}
