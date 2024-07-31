package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.CONTENTS
import com.sbl.sulmun2yong.survey.domain.question.TextQuestion
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.util.UUID

object QuestionFixtureFactory {
    const val TITLE = "질문 제목"
    const val DESCRIPTION = "질문 설명"
    val CHOICES = Choices(CONTENTS.map { Choice.Standard(it) }, true)

    fun createTextResponseQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
    ) = StandardTextQuestion(
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
    ) = StandardSingleChoiceQuestion(
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
    ) = StandardMultipleChoiceQuestion(
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
    ) = StandardMultipleChoiceQuestion(
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
    ): TextQuestion {
        val mockQuestion = mock<TextQuestion>()
        `when`(mockQuestion.id).thenReturn(id)
        `when`(mockQuestion.isRequired).thenReturn(isRequired)
        `when`(mockQuestion.isValidResponse(any())).thenReturn(isResponseValid)
        `when`(mockQuestion.canBeKeyQuestion()).thenReturn(isRequired)
        return mockQuestion
    }

    /**
     * 테스트용 선택지 목록 생성 메서드
     * @param contents 기본값 = listOf("1", "2", "3")
     * @param isAllowOther 기본값 = true
     * */
    fun createChoices(
        contents: List<String> = CONTENTS,
        isAllowOther: Boolean = true,
    ) = Choices(contents.map { Choice.Standard(it) }, isAllowOther)
}
