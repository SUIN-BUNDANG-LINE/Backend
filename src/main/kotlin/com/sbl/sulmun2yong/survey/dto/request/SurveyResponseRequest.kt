package com.sbl.sulmun2yong.survey.dto.request

import com.sbl.sulmun2yong.survey.domain.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionResponse
import java.util.UUID

data class SurveyResponseRequest(
    val sectionResponses: List<SectionResponseRequest>,
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
