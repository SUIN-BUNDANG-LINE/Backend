package com.sbl.sulmun2yong.survey.dto.request

import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID

data class SurveyResponseRequest(
    @field:Valid
    val sectionResponses: List<SectionResponseRequest>,
) {
    data class SectionResponseRequest(
        val sectionId: UUID,
        @field:Valid
        val questionResponses: List<@Valid QuestionResponseRequest>,
    ) {
        data class QuestionResponseRequest(
            val questionId: UUID,
            @field:Valid
            val responses: List<@Valid ResponseDetailRequest>,
        ) {
            data class ResponseDetailRequest(
                @field:Size(max = 1000, message = "내용은 최대 1000자까지 입력 가능합니다.")
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

    fun toDomain(surveyId: UUID) =
        SurveyResponse(
            surveyId = surveyId,
            sectionResponses =
                sectionResponses.map {
                    it.toDomain()
                },
        )
}
