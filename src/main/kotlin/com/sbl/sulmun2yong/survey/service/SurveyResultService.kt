package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.response.SurveyResultResponse
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyResultService(
    private val responseAdapter: ResponseAdapter,
    private val surveyAdapter: SurveyAdapter,
) {
    fun getSurveyResult(
        surveyId: UUID,
        userId: UUID,
    ): SurveyResultResponse {
        val isSurveyExists = surveyAdapter.existsByIdAndMakerId(surveyId, userId)
        // 본인이 만든 설문이 아닌 경우 예외 발생
        if (!isSurveyExists) throw SurveyNotFoundException()
        val response = responseAdapter.getResponses(surveyId)
        return SurveyResultResponse.of(response)
    }
}
