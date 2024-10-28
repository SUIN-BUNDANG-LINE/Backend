package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createChoices
import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.CONTENTS
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.ChoiceEmptyException
import com.sbl.sulmun2yong.survey.exception.ChoiceSizeExceedException
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
        assertThrows<ChoiceEmptyException> { Choices(listOf(), true) }
        assertThrows<ChoiceEmptyException> { Choices(listOf(), false) }
    }

    @Test
    fun `선택지는 최대 20개 까지만 추가할 수 있다`() {
        val standardChoices = List(Choices.MAX_SIZE + 1) { Choice.Standard(it.toString()) }
        assertThrows<ChoiceSizeExceedException> { Choices(standardChoices, true) }
        assertThrows<ChoiceSizeExceedException> { Choices(standardChoices, false) }
    }

    @Test
    fun `선택지 목록에 중복된 내용이 있는지 확인할 수 있다`() {
        // given
        val uniqueContents = listOf("1", "2", "3")
        val duplicatedContents1 = listOf("1", "2", "2")
        val duplicatedContents2 = listOf("3", "3")

        // when
        val uniqueChoices = createChoices(uniqueContents, true)
        val duplicatedChoices1 = createChoices(duplicatedContents1, true)
        val duplicatedChoices2 = createChoices(duplicatedContents2, false)

        // then
        assertEquals(true, uniqueChoices.isUnique())
        assertEquals(false, duplicatedChoices1.isUnique())
        assertEquals(false, duplicatedChoices2.isUnique())
    }

    @Test
    fun `응답이 선택지에 포함되는지 확인한다`() {
        // given
        val allowOtherChoices = createChoices()
        val notAllowOtherChoices = createChoices(isAllowOther = false)

        // when, then
        assertEquals(true, allowOtherChoices.isContains(ResponseDetail(CONTENTS[0])))
        assertEquals(false, allowOtherChoices.isContains(ResponseDetail("4")))
        assertEquals(true, allowOtherChoices.isContains(ResponseDetail("4", true)))
        assertEquals(true, notAllowOtherChoices.isContains(ResponseDetail(CONTENTS[0])))
        assertEquals(false, notAllowOtherChoices.isContains(ResponseDetail("4")))
        assertEquals(false, notAllowOtherChoices.isContains(ResponseDetail("4", true)))
    }

    @Test
    fun `Choices의 선택지 집합을 얻을 수 있다`() {
        // given
        val standardChoiceList = CONTENTS.map { Choice.Standard(it) }
        val allowOtherChoices = createChoices(CONTENTS)
        val notAllowOtherChoices = createChoices(CONTENTS, false)

        // when, then
        assertEquals(standardChoiceList.toSet() + Choice.Other, allowOtherChoices.getChoiceSet())
        assertEquals(standardChoiceList.toSet(), notAllowOtherChoices.getChoiceSet())
    }
}
