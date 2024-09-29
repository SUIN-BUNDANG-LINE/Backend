package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveyInfoResponse
import java.util.UUID

interface SurveyCustomRepository {
    fun findSurveysWithResponseCount(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ): List<MyPageSurveyInfoResponse>

    fun softDelete(
        surveyId: UUID,
        makerId: UUID,
    ): Boolean
}
