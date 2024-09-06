package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import java.util.UUID

data class SurveyResultResponse(
    val results: List<Result>,
) {
    companion object {
        fun of(surveyResult: SurveyResult) =
            SurveyResultResponse(
                results =
                    surveyResult.responses.groupBy { it.questionId }.map {
                        Result.from(it.value)
                    },
            )
    }

    data class Result(
        val questionId: UUID,
        val responses: List<Response>,
    ) {
        companion object {
            fun from(responses: List<SurveyResult.Response>): Result =
                Result(
                    questionId = responses.first().questionId,
                    responses = responses.groupBy { it.content }.map { Response(it.key, it.value.size) },
                )
        }

        data class Response(
            val content: String,
            val count: Int,
        )
    }
}
