package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.ChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.result.QuestionResult
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import com.sbl.sulmun2yong.survey.domain.section.Section
import java.util.UUID

data class SurveyResultResponse(
    val sectionResults: List<SectionResultResponse>,
    val participantCount: Int,
) {
    companion object {
        fun of(
            surveyResult: SurveyResult,
            survey: Survey,
            participantCount: Int,
        ) = SurveyResultResponse(survey.sections.map { SectionResultResponse.of(surveyResult, it) }, participantCount)
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
                    questionResults = section.questions.map { QuestionResultResponse.of(it, surveyResult.findQuestionResult(it.id)) },
                )
        }
    }

    data class QuestionResultResponse(
        val questionId: UUID,
        val title: String,
        val type: QuestionType,
        val participantCount: Int,
        val responses: List<Response>,
        val responseContents: List<String>,
    ) {
        companion object {
            fun of(
                question: Question,
                questionResult: QuestionResult?,
            ): QuestionResultResponse {
                if (questionResult == null) {
                    return QuestionResultResponse(
                        questionId = question.id,
                        title = question.title,
                        type = question.questionType,
                        participantCount = 0,
                        responses = listOf(),
                        responseContents = question.choices?.standardChoices?.map { it.content } ?: listOf(),
                    )
                }

                val allContents =
                    if (question is ChoiceQuestion) {
                        val tempContents = question.choices.standardChoices.map { it.content }
                        tempContents + questionResult.contents.filter { !tempContents.contains(it) }
                    } else {
                        questionResult.contents.toList()
                    }

                val responses =
                    if (question is ChoiceQuestion) {
                        val contentCountMap =
                            questionResult.resultDetails
                                .flatMap { it.contents }
                                .groupingBy { it }
                                .eachCount()
                        allContents.map { Response(it, contentCountMap[it] ?: 0) }
                    } else {
                        questionResult.resultDetails.map { Response(it.contents.first(), 1) }
                    }
                return QuestionResultResponse(
                    questionId = question.id,
                    title = question.title,
                    type = question.questionType,
                    participantCount = questionResult.resultDetails.size,
                    responses = responses,
                    responseContents = allContents,
                )
            }
        }

        data class Response(
            val content: String,
            val count: Int,
        )
    }
}
