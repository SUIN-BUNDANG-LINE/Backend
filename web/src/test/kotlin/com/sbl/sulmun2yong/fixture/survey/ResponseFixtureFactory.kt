package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.section.SectionId
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

    /**
     * 테스트용 QuestionResponse 생성 메서드
     * @param id 기본값 = UUID.randomUUID()
     * @param contents 기본값 = listOf()
     * @param isOtherContent 기본값 = true
     * */
    fun createQuestionResponse(
        id: UUID = UUID.randomUUID(),
        contents: List<String> = listOf(),
        isOtherContent: String? = null,
    ) = QuestionResponse(id, createResponseDetails(contents, isOtherContent))

    private fun createResponseDetails(
        contents: List<String>,
        isOtherContent: String? = null,
    ) = contents.map {
        ResponseDetail(it, false)
    } + if (isOtherContent != null) listOf(ResponseDetail(isOtherContent, true)) else listOf()
}
