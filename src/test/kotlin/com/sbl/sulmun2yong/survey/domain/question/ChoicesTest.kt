package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createChoices
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.exception.InvalidChoiceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ChoicesTest {
    @Test
    fun `선택지 목록을 생성하면 정보가 올바르게 설정된다`() {
        // given
        val contents = listOf("1", "2", "3")
        val standardChoiceList = contents.map { Choice.Standard(it) }

        // when
        val allowOtherChoices = createChoices(contents, true)
        val notAllowOtherChoices = createChoices(contents, false)

        // then
        with(allowOtherChoices) {
            assertEquals(standardChoiceList, this.standardChoices)
            assertEquals(true, this.isAllowOther)
        }
        with(notAllowOtherChoices) {
            assertEquals(standardChoiceList, this.standardChoices)
            assertEquals(false, this.isAllowOther)
        }
    }

    @Test
    fun `선택지 목록은 비어있을 수 없다`() {
        assertThrows<InvalidChoiceException> { Choices(listOf(), true) }
        assertThrows<InvalidChoiceException> { Choices(listOf(), false) }
    }

    @Test
    fun `선택지 목록의 내용은 중복될 수 없다`() {
        // given
        val duplicatedContents1 = listOf("1", "2", "2")
        val duplicatedContents2 = listOf("3", "3")

        // when, then
        assertThrows<InvalidChoiceException> { createChoices(duplicatedContents1, true) }
        assertThrows<InvalidChoiceException> { createChoices(duplicatedContents2, false) }
    }
}
