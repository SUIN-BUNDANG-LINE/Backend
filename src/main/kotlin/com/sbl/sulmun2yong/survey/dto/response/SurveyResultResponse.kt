package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.result.ResultDetails
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import com.sbl.sulmun2yong.survey.domain.section.Section
import java.util.UUID

data class SurveyResultResponse(
    val sectionResults: List<SectionResultResponse>,
) {
    companion object {
        fun of(
            surveyResult: SurveyResult,
            survey: Survey,
        ) = SurveyResultResponse(survey.sections.map { SectionResultResponse.of(surveyResult, it) })
    }

    data class SectionResultResponse(
        val sectionId: UUID,
        val title: String,
        val questionResults: List<QuestionResultResponse>,
    ) {
        companion object {
            fun of(
                surveyResult: SurveyResult,
                section: Section,
            ): SectionResultResponse =
                SectionResultResponse(
                    sectionId = section.id.value,
                    title = section.title,
                    questionResults =
                        section.questions.map {
                            QuestionResultResponse.of(
                                question = it,
                                responses = surveyResult.findResultDetailsByQuestionId(it.id),
                            )
                        },
                )
        }
    }

    data class QuestionResultResponse(
        val questionId: UUID,
        val title: String,
        val type: QuestionType,
        val participantCount: Int,
        val responses: List<Response>,
    ) {
        companion object {
            fun of(
                question: Question,
                responses: List<ResultDetails>,
            ): QuestionResultResponse {
                val contentCountMap =
                    responses
                        .map { it.contents }
                        .flatten()
                        .groupingBy { it }
                        .eachCount()
                        .toMutableMap()
                val contents = question.choices?.standardChoices?.map { it.content } ?: emptyList()
                contents.forEach { contentCountMap.putIfAbsent(it, 0) }
                return QuestionResultResponse(
                    questionId = question.id,
                    title = question.title,
                    type = question.questionType,
                    participantCount = responses.size,
                    responses = contentCountMap.map { Response(it.key, it.value) },
                )
            }
        }

        data class Response(
            val content: String,
            val count: Int,
        )
    }
}
