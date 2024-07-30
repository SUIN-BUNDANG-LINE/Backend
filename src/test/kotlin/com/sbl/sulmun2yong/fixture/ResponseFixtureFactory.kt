package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionResponse
import java.util.UUID

object ResponseFixtureFactory {
    private val QUESTION_RESPONSE_AETCA = createQuestionResponse(contents = listOf("a"), isOtherContent = "a")
    private val QUESTION_RESPONSE_B = createQuestionResponse(contents = listOf("b"))
    private val QUESTION_RESPONSE_ETCC = createQuestionResponse(isOtherContent = "c")
    private val DUMMY_QUESTION_RESPONSES = listOf(QUESTION_RESPONSE_AETCA, QUESTION_RESPONSE_B, QUESTION_RESPONSE_ETCC)

    fun createSectionResponse(
        id: UUID = UUID.randomUUID(),
        questionId: UUID = UUID.randomUUID(),
        contents: List<String> = listOf(),
        isOtherContent: String? = null,
    ) = SectionResponse(
        SectionId.Standard(id),
        (DUMMY_QUESTION_RESPONSES.shuffled() + listOf(createQuestionResponse(questionId, contents, isOtherContent))).shuffled(),
    )

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
