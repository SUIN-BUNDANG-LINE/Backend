package com.sbl.sulmun2yong.survey.domain.result

import java.util.UUID

data class SurveyResult(
    val questionResults: List<QuestionResult>,
) {
    fun getFilteredResult(resultFilter: ResultFilter): SurveyResult {
        val questionFilters = resultFilter.questionFilters
        var filteredSurveyResult = copy()
        for (questionFilter in questionFilters) {
            val targetQuestionResult = findQuestionResult(questionFilter.questionId) ?: continue
            val participantSet = targetQuestionResult.getMatchedParticipants(questionFilter)
            filteredSurveyResult = filteredSurveyResult.filterByQuestionFilter(participantSet, questionFilter)
        }
        return filteredSurveyResult
    }

    fun findQuestionResult(questionId: UUID) = questionResults.find { it.questionId == questionId }

    fun getParticipantCount() =
        questionResults
            .map { it.resultDetails.map { resultDetail -> resultDetail.participantId } }
            .flatten()
            .toSet()
            .size

    private fun filterByQuestionFilter(
        participantSet: Set<UUID>,
        questionFilter: QuestionFilter,
    ): SurveyResult =
        SurveyResult(
            questionResults.map { questionResult ->
                val responseDetails =
                    if (questionFilter.isPositive) {
                        questionResult.resultDetails.filter {
                            participantSet.contains(it.participantId)
                        }
                    } else {
                        questionResult.resultDetails.filter { !participantSet.contains(it.participantId) }
                    }
                QuestionResult(
                    questionResult.questionId,
                    responseDetails,
                    questionResult.contents,
                )
            },
        )
}
