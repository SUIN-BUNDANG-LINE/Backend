package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.fixture.survey.ResponseFixtureFactory.createSectionResponse
import com.sbl.sulmun2yong.fixture.survey.RoutingFixtureFactory.createContentIdMap
import com.sbl.sulmun2yong.fixture.survey.RoutingFixtureFactory.createSetByChoiceRouting
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RoutingStrategyTest {
    private val id1 = UUID.randomUUID()
    private val id2 = UUID.randomUUID()
    private val id3 = UUID.randomUUID()
    private val stringUUIDMap = mapOf("a" to id1, "b" to id2, null to id3)
    private val keyQuestionId = UUID.randomUUID()
    private val setByChoice = createSetByChoiceRouting(keyQuestionId, stringUUIDMap)

    @Test
    fun `NumericalOrder를 생성하면 정보가 올바르게 설정된다`() {
        // given, when
        val numericalOrder = RoutingStrategy.NumericalOrder

        // then
        assertEquals(RoutingType.NUMERICAL_ORDER, numericalOrder.type)
        assertEquals(emptyList(), numericalOrder.getNextSectionIds())
    }

    @Test
    fun `SetByUser를 생성하면 정보가 올바르게 설정된다`() {
        // given, when
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val setByUser = RoutingStrategy.SetByUser(sectionId)

        // then
        assertEquals(RoutingType.SET_BY_USER, setByUser.type)
        assertEquals(sectionId, setByUser.nextSectionId)
        assertEquals(listOf(sectionId), setByUser.getNextSectionIds())
    }

    @Test
    fun `SetByChoice를 생성하면 정보들이 올바르게 설정된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val contentIdMap = createContentIdMap(stringUUIDMap)

        // when
        val setByChoice = RoutingStrategy.SetByChoice(keyQuestionId, contentIdMap)

        // then
        with(setByChoice) {
            assertEquals(RoutingType.SET_BY_CHOICE, setByChoice.type)
            assertEquals(keyQuestionId, setByChoice.keyQuestionId)
            assertEquals(contentIdMap, this.routingMap)
            assertEquals(listOf("a", "b", null).map { Choice.from(it) }.toSet(), this.getChoiceSet())
            assertEquals(listOf(id1, id2, id3).map { SectionId.from(it) }, this.getNextSectionIds())
        }
    }

    @Test
    fun `SetByChoice는 SectionResponse를 받아서 다음 섹션 ID를 찾을 수 있다`() {
        // given
        val sectionResponse1 = createSectionResponse(questionId = keyQuestionId, contents = listOf("a"))
        val sectionResponse2 = createSectionResponse(questionId = keyQuestionId, contents = listOf("b"))
        val sectionResponse3 = createSectionResponse(questionId = keyQuestionId, isOtherContent = "c")

        // when
        val nextSectionId1 = setByChoice.findNextSectionId(sectionResponse1)
        val nextSectionId2 = setByChoice.findNextSectionId(sectionResponse2)
        val nextSectionId3 = setByChoice.findNextSectionId(sectionResponse3)

        // then
        assertEquals(stringUUIDMap["a"], nextSectionId1.value)
        assertEquals(stringUUIDMap["b"], nextSectionId2.value)
        assertEquals(stringUUIDMap[null], nextSectionId3.value)
    }

    @Test
    fun `SetByChoice가 다음 섹션 ID를 찾을 때 SectionResponse가 유효하지 않으면 예외를 반환한다`() {
        // SetByChoice가 다음 섹션 ID를 찾기 위해 받는 SectionResponse는 다음 조건을 만족해야한다.
        // 1. keyQuestionId에 해당하는 응답을 포함해야한다.
        // 2. keyQuestionId에 해당하는 응답은 1개여야한다.
        // 3. 응답에 해당하는 선택지가 있어야한다.
        // given
        val sectionResponse1 = createSectionResponse(contents = listOf("a"))
        val sectionResponse2A = createSectionResponse(questionId = keyQuestionId, contents = listOf("b"), isOtherContent = "c")
        val sectionResponse2B = createSectionResponse(questionId = keyQuestionId, contents = listOf("a", "b"))
        val sectionResponse3 = createSectionResponse(questionId = keyQuestionId, contents = listOf("c"))

        // when, then
        assertThrows<InvalidSectionResponseException> { setByChoice.findNextSectionId(sectionResponse1) }
        assertThrows<InvalidSectionResponseException> { setByChoice.findNextSectionId(sectionResponse2A) }
        assertThrows<InvalidSectionResponseException> { setByChoice.findNextSectionId(sectionResponse2B) }
        assertThrows<InvalidSectionResponseException> {
            createSetByChoiceRouting(
                keyQuestionId,
                stringUUIDMap,
            ).findNextSectionId(sectionResponse3)
        }
    }
}
