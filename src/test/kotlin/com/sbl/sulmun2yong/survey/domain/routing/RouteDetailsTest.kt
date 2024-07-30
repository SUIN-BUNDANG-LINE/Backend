package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.fixture.ResponseFixtureFactory.createSectionResponse
import com.sbl.sulmun2yong.fixture.RoutingFixtureFactory.createSectionRouteConfigs
import com.sbl.sulmun2yong.fixture.RoutingFixtureFactory.createSetByChoiceRouting
import com.sbl.sulmun2yong.fixture.SectionFixtureFactory.createSectionIds
import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RouteDetailsTest {
    private val contentIdMap = mapOf("a" to UUID.randomUUID(), "b" to UUID.randomUUID(), null to UUID.randomUUID())
    private val keyQuestionId = UUID.randomUUID()
    private val setByChoice = createSetByChoiceRouting(keyQuestionId, contentIdMap)

    @Test
    fun `NumericalOrder를 생성하면 정보가 올바르게 설정된다`() {
        // given, when
        val numericalOrder = RouteDetails.NumericalOrderRouting

        // then
        assertEquals(SectionRouteType.NUMERICAL_ORDER, numericalOrder.type)
    }

    @Test
    fun `NumericalOrder는 어떤 sectionIds를 받아도 유효하다고 판단한다`() {
        // given
        val numericalOrder = RouteDetails.NumericalOrderRouting

        // when
        val isValid1 = numericalOrder.isSectionIdsValid(createSectionIds(listOf(UUID.randomUUID(), UUID.randomUUID())))
        val isValid2 = numericalOrder.isSectionIdsValid(createSectionIds(listOf(UUID.randomUUID())))

        // then
        assertEquals(true, isValid1)
        assertEquals(true, isValid2)
    }

    @Test
    fun `SetByUser를 생성하면 정보가 올바르게 설정된다`() {
        // given, when
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val setByUser = RouteDetails.SetByUserRouting(sectionId)

        // then
        assertEquals(SectionRouteType.SET_BY_USER, setByUser.type)
        assertEquals(sectionId, setByUser.nextSectionId)
    }

    @Test
    fun `SetByUser는 자신의 nextSectionId가 포함된 sectionIds를 받으면 유효하다고 판단한다`() {
        // given
        val sectionId = SectionId.Standard(UUID.randomUUID())
        val setByUser = RouteDetails.SetByUserRouting(sectionId)

        // when
        val isValid1 = setByUser.isSectionIdsValid(createSectionIds(listOf(UUID.randomUUID(), UUID.randomUUID())))
        val isValid2 = setByUser.isSectionIdsValid(createSectionIds(listOf(UUID.randomUUID(), sectionId.value)))
        val isValid3 = setByUser.isSectionIdsValid(createSectionIds(listOf(sectionId.value)))

        // then
        assertEquals(false, isValid1)
        assertEquals(true, isValid2)
        assertEquals(true, isValid3)
    }

    @Test
    fun `SetByChoice를 생성하면 정보들이 올바르게 설정된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs = createSectionRouteConfigs(contentIdMap)

        // when
        val setByChoice = RouteDetails.SetByChoiceRouting(keyQuestionId, sectionRouteConfigs)

        // then
        with(setByChoice) {
            assertEquals(SectionRouteType.SET_BY_CHOICE, setByChoice.type)
            assertEquals(keyQuestionId, setByChoice.keyQuestionId)
            assertEquals(sectionRouteConfigs, this.sectionRouteConfigs)
        }
    }

    @Test
    fun `SetByChoice는 라우팅의 대상이 되는 선택지들의 집합을 가져올 수 있다`() {
        assertEquals(setOf(Choice.from("a"), Choice.from("b"), Choice.Other), setByChoice.getChoiceSet())
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
        assertEquals(contentIdMap["a"], nextSectionId1.value)
        assertEquals(contentIdMap["b"], nextSectionId2.value)
        assertEquals(contentIdMap[null], nextSectionId3.value)
    }

    @Test
    fun `SetByChoice가 다음 섹션 ID를 찾을 때 SectionResponse가 유효하지 않으면 예외를 반환한다`() {
        // SetByChoice가 다음 섹션 ID를 찾기 위해 받는 SectionResponse는 다음 조건을 만족해야한다.
        // 1. keyQuestionId에 해당하는 응답을 포함해야한다.
        // 2. keyQuestionId에 해당하는 응답은 1개여야한다.
        // 3. 응답에 해당하는 sectionRouteConfigs가 있어야한다.
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
                contentIdMap,
            ).findNextSectionId(sectionResponse3)
        }
    }
}
