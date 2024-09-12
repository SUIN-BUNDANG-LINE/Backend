package com.sbl.sulmun2yong.survey.dto.request

import com.sbl.sulmun2yong.survey.domain.result.QuestionFilter
import com.sbl.sulmun2yong.survey.domain.result.ResultFilter
import java.util.UUID

data class SurveyResultRequest(
    val questionFilters: List<QuestionFilterRequest>,
) {
    data class QuestionFilterRequest(
        val questionId: UUID,
        val contents: List<String>,
        val isPositive: Boolean,
    ) {
        fun toDomain() =
            QuestionFilter(
                questionId = questionId,
                contents = contents,
                isPositive = isPositive,
            )
    }

    fun toDomain() =
        ResultFilter(
            questionFilters = questionFilters.map { it.toDomain() },
        )
}
