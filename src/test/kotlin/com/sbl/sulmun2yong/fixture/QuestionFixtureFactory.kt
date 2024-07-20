package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.question.Choices
import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.TextResponseQuestion
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.util.UUID

object QuestionFixtureFactory {
    const val TITLE = "질문 제목"
    const val DESCRIPTION = "질문 설명"
    val CHOICES = Choices(listOf("a", "b", "c"))

    fun createTextResponseQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
    ) = TextResponseQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
    )

    fun createSingleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isAllowOther: Boolean = true,
        choices: Choices = CHOICES,
    ) = SingleChoiceQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
        choices = choices,
        isAllowOther = isAllowOther,
    )

    fun createMultipleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isAllowOther: Boolean = true,
        choices: Choices = CHOICES,
    ) = MultipleChoiceQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
        choices = choices,
        isAllowOther = isAllowOther,
    )

    fun createMockQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isResponseValid: Boolean = true,
    ): TextResponseQuestion {
        val mockQuestion = mock<TextResponseQuestion>()
        `when`(mockQuestion.id).thenReturn(id)
        `when`(mockQuestion.isRequired).thenReturn(isRequired)
        `when`(mockQuestion.isValidResponse(any())).thenReturn(isResponseValid)
        `when`(mockQuestion.canBeKeyQuestion()).thenReturn(isRequired)
        return mockQuestion
    }
}
