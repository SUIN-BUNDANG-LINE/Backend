package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyProgressInfoResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyInfoService(
    private val surveyAdapter: SurveyAdapter,
    private val drawingBoardAdapter: DrawingBoardAdapter,
) {
    fun getSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isAsc: Boolean,
    ): SurveyListResponse {
        val surveys =
            surveyAdapter.findSurveysWithPagination(
                size = size,
                page = page,
                sortType = sortType,
                isAsc = isAsc,
            )
        return SurveyListResponse.of(surveys.totalPages, surveys.content)
    }

    fun getSurveyInfo(surveyId: UUID): SurveyInfoResponse {
        val survey = surveyAdapter.getSurvey(surveyId)
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        return SurveyInfoResponse.of(survey, drawingBoard.selectedTicketCount)
    }

    fun getSurveyProgressInfo(surveyId: UUID): SurveyProgressInfoResponse? {
        val survey = surveyAdapter.getSurvey(surveyId)
        return SurveyProgressInfoResponse.of(survey)
    }
}
