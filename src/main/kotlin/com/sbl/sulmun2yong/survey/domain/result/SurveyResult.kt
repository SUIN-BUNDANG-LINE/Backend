package com.sbl.sulmun2yong.survey.domain.result

import java.util.UUID

data class SurveyResult(
    val resultDetails: List<ResultDetails>,
) {
    fun getFilteredResult(resultFilter: ResultFilter): SurveyResult {
        val questionFilters = resultFilter.questionFilters
        if (questionFilters.isEmpty()) return this
        var filteredSurveyResult = copy()
        for (questionFilter in questionFilters) {
            filteredSurveyResult = filteredSurveyResult.filterByQuestionFilter(questionFilter)
            if (filteredSurveyResult.resultDetails.isEmpty()) return filteredSurveyResult
        }
        return filteredSurveyResult
    }

    private fun filterByQuestionFilter(questionFilter: QuestionFilter): SurveyResult {
        val participantSet = getMatchedParticipants(questionFilter)
        return if (questionFilter.isPositive) {
            // isPositive가 true이면 해당 참가자들을 포함
            SurveyResult(resultDetails.filter { participantSet.contains(it.participantId) })
        } else {
            // isPositive가 false이면 해당 참가자들을 제외
            SurveyResult(resultDetails.filter { !participantSet.contains(it.participantId) })
        }
    }

    private fun getMatchedParticipants(questionFilter: QuestionFilter): Set<UUID> =
        resultDetails
            .mapNotNull { response ->
                if (response.isMatched(questionFilter)) response.participantId else null
            }.toSet()
}
