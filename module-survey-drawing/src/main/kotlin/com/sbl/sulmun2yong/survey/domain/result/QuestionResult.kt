package com.sbl.sulmun2yong.survey.domain.result

import java.util.SortedSet
import java.util.UUID

data class QuestionResult(
    val questionId: UUID,
    val resultDetails: List<ResultDetails>,
    /** 해당 질문의 모든 응답 집합 */
    val contents: SortedSet<String>,
) {
    fun getMatchedParticipants(questionFilter: QuestionFilter): Set<UUID> =
        resultDetails.mapNotNull { if (it.isMatched(questionFilter)) it.participantId else null }.toSet()
}
