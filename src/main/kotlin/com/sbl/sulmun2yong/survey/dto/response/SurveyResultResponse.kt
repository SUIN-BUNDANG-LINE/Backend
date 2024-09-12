package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.result.ResultDetails
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import java.util.UUID

data class SurveyResultResponse(
    val results: List<Result>,
) {
    companion object {
        fun of(surveyResult: SurveyResult) =
            SurveyResultResponse(
                results =
                    surveyResult.resultDetails.groupBy { it.questionId }.map {
                        Result.from(it.value)
                    },
            )
    }

    data class Result(
        val questionId: UUID,
        val responses: List<Response>,
    ) {
        companion object {
            fun from(responses: List<ResultDetails>): Result =
                Result(
                    questionId = responses.first().questionId,
                    responses =
                        responses
                            .map { it.contents }
                            .flatten()
                            .groupingBy { it }
                            .eachCount()
                            .map { Response(it.key, it.value) },
                )
        }

        data class Response(
            val content: String,
            val count: Int,
        )
    }
}
