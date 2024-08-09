package com.sbl.sulmun2yong.survey.dto.request

import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import java.util.UUID

data class SurveyResponseRequest(
    val sectionResponses: List<SectionResponseRequest>,
    val visitorId: String,
) {
    data class SectionResponseRequest(
        val sectionId: UUID,
        val questionResponses: List<QuestionResponseRequest>,
    ) {
        data class QuestionResponseRequest(
            val questionId: UUID,
            val responses: List<ResponseDetailRequest>,
        ) {
            data class ResponseDetailRequest(
                val content: String,
                val isOther: Boolean,
            ) {
                fun toDomain() =
                    ResponseDetail(
                        content = content,
                        isOther = isOther,
                    )
            }

            fun toDomain() =
                QuestionResponse(
                    questionId = questionId,
                    responses = responses.map { it.toDomain() },
                )
        }

        fun toDomain() =
            SectionResponse(
                sectionId = SectionId.Standard(sectionId),
                questionResponses = questionResponses.map { it.toDomain() },
            )
    }

    fun toDomain(surveyId: UUID) = SurveyResponse(surveyId = surveyId, sectionResponses = sectionResponses.map { it.toDomain() })
}
