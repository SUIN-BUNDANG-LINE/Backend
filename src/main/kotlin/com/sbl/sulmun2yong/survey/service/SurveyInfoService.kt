package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import org.springframework.stereotype.Service

@Service
class SurveyInfoService(private val surveyAdapter: SurveyAdapter) {
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
}
