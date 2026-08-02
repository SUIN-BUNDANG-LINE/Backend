package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.CONTENTS
import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.TITLE
import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
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
    const val DESCRIPTION = "질문 설명"
    val CHOICES = Choices(CONTENTS.map { Choice.Standard(it) }, true)

    /**
     * 테스트용 주관식 질문 생성 메서드
     * @param id 기본값 = UUID.randomUUID()
     * @param isRequired 기본값 = true
     * */
    fun createTextResponseQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
    ): TextQuestion =
        StandardTextQuestion(
            id = id,
            title = TITLE + id,
            description = DESCRIPTION + id,
            isRequired = isRequired,
        )

    /**
     * 테스트용 단일 선택 질문 생성 메서드
     * @param id 기본값 = UUID.randomUUID()
     * @param isRequired 기본값 = true
     * @param isAllowOther 기본값 = true
     * @param contents 기본값 = listOf("1", "2", "3")
     * */
    fun createSingleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isAllowOther: Boolean = true,
        contents: List<String> = CONTENTS,
    ): SingleChoiceQuestion =
        StandardSingleChoiceQuestion(
            id = id,
            title = TITLE + id,
            description = DESCRIPTION + id,
            isRequired = isRequired,
            choices = createChoices(contents, isAllowOther),
        )

    /**
     * 테스트용 다중 선택 질문 생성 메서드
     * @param id 기본값 = UUID.randomUUID()
     * @param isRequired 기본값 = true
     * @param isAllowOther 기본값 = true
     * @param contents 기본값 = listOf("1", "2", "3")
     * */
    fun createMultipleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        isAllowOther: Boolean = true,
        contents: List<String> = CONTENTS,
    ): MultipleChoiceQuestion =
        StandardMultipleChoiceQuestion(
            id = id,
            title = TITLE + id,
            description = DESCRIPTION + id,
            isRequired = isRequired,
            choices = createChoices(contents, isAllowOther),
        )

    /**
     * 테스트용 다중 선택 질문 생성 메서드
     * @param id 기본값 = UUID.randomUUID()
     * @param isRequired 기본값 = true
     * @param choices 기본값 = Choices("1", "2", "3", Other)
     * */
    fun createMultipleChoiceQuestion(
        id: UUID = UUID.randomUUID(),
        isRequired: Boolean = true,
        choices: Choices = createChoices(CONTENTS, true),
    ): MultipleChoiceQuestion =
        StandardMultipleChoiceQuestion(
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
