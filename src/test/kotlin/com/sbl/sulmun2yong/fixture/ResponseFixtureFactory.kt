package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import java.util.UUID

object ResponseFixtureFactory {
    fun createQuestionResponse(
        id: UUID = UUID.randomUUID(),
        contents: List<String> = listOf(),
        isOtherContent: String? = null,
    ) = QuestionResponse(id, createResponseDetails(contents, isOtherContent))

    private fun createResponseDetails(
        contents: List<String>,
        isOtherContent: String? = null,
    ) = contents.map {
        ResponseDetail(it)
    } + if (isOtherContent != null) listOf(ResponseDetail(isOtherContent, true)) else listOf()
}
