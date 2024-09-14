package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveysResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyProgressInfoResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyAccessException
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
        if (survey.status == SurveyStatus.NOT_STARTED) throw InvalidSurveyAccessException()
        val selectedTicketCount =
            if (survey.rewardSetting.isImmediateDraw) drawingBoardAdapter.getBySurveyId(surveyId).selectedTicketCount else null
        return SurveyInfoResponse.of(survey, selectedTicketCount)
    }

    fun getSurveyProgressInfo(surveyId: UUID): SurveyProgressInfoResponse? {
        val survey = surveyAdapter.getSurvey(surveyId)
        if (survey.status != SurveyStatus.IN_PROGRESS) throw InvalidSurveyAccessException()
        return SurveyProgressInfoResponse.of(survey)
    }

    fun getMyPageSurveys(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ): MyPageSurveysResponse {
        val myPageSurveysInfoResponse = surveyAdapter.getMyPageSurveysInfo(makerId, status, sortType)
        return MyPageSurveysResponse(myPageSurveysInfoResponse)
    }
}
