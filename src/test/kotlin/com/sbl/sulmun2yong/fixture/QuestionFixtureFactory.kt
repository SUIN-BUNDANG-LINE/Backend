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
    private val CONTENTS = listOf("a", "b", "c")
    val CHOICES = Choices.of(contents = CONTENTS, isAllowOther = true)

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
        contents: List<String> = CONTENTS,
    ) = SingleChoiceQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
        choices = createChoices(contents, isAllowOther),
    )

    fun createMultipleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isAllowOther: Boolean = true,
        contents: List<String> = CONTENTS,
    ) = MultipleChoiceQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
        choices = createChoices(contents, isAllowOther),
    )

    fun createMultipleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        choices: Choices = createChoices(CONTENTS, true),
    ) = MultipleChoiceQuestion(
        id = id,
        title = TITLE + id,
        description = DESCRIPTION + id,
        isRequired = isRequired,
        choices = choices,
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

    private fun createChoices(
        contents: List<String>,
        isAllowOther: Boolean,
    ) = Choices.of(contents = contents, isAllowOther = isAllowOther)
}
