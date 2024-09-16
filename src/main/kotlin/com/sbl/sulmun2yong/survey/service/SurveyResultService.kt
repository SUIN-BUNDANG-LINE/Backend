package com.sbl.sulmun2yong.survey.service

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
        return SurveyResultResponse.of(filteredSurveyResult, survey)
    }
}
