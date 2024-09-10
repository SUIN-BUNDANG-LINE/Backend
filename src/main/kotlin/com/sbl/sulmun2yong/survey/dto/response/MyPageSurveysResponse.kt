package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import java.util.UUID

data class MyPageSurveysResponse(
    val surveys: List<MyPageSurveyInfoResponse>,
) {
    data class MyPageSurveyInfoResponse(
        val surveyId: UUID,
        val title: String,
        val status: SurveyStatus,
    ) {
        companion object {
            fun from(survey: Survey) = MyPageSurveyInfoResponse(survey.id, survey.title, survey.status)
        }
    }

    companion object {
        fun from(surveys: List<Survey>) = MyPageSurveysResponse(surveys.map { MyPageSurveyInfoResponse.from(it) })
    }
}
