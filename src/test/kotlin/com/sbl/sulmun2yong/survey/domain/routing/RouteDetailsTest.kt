package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.fixture.ResponseFixtureFactory.createSectionResponse
import com.sbl.sulmun2yong.fixture.RoutingFixtureFactory.createSectionRouteConfigs
import com.sbl.sulmun2yong.fixture.RoutingFixtureFactory.createSetByChoiceRouting
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
        val sectionId = UUID.randomUUID()
        val numericalOrder = NumericalOrderRouting(sectionId)

        // then
        assertEquals(SectionRouteType.NUMERICAL_ORDER, numericalOrder.type)
        assertEquals(sectionId, numericalOrder.nextSectionId)
        assertEquals(null, numericalOrder.keyQuestionId)
        assertEquals(null, numericalOrder.sectionRouteConfigs)
    }

    @Test
    fun `NumericalOrder는 SectionResponse를 받아서 다음 섹션 ID를 찾을 수 있다`() {
        // given
        val sectionId = UUID.randomUUID()
        val numericalOrder = NumericalOrderRouting(sectionId)
        val sectionResponse = createSectionResponse(isOtherContent = "a")

        // when
        val nextSectionId = numericalOrder.findNextSectionId(sectionResponse)

        // then
        assertEquals(sectionId, nextSectionId)
    }

    @Test
    fun `SetByUser를 생성하면 정보가 올바르게 설정된다`() {
        // given, when
        val sectionId = UUID.randomUUID()
        val setByUser = SetByUserRouting(sectionId)

        // then
        assertEquals(SectionRouteType.SET_BY_USER, setByUser.type)
        assertEquals(sectionId, setByUser.nextSectionId)
        assertEquals(null, setByUser.keyQuestionId)
        assertEquals(null, setByUser.sectionRouteConfigs)
    }

    @Test
    fun `SetByUser는 SectionResponse를 받아서 다음 섹션 ID를 찾을 수 있다`() {
        // given
        val sectionId = UUID.randomUUID()
        val setByUser = SetByUserRouting(sectionId)
        val sectionResponse = createSectionResponse(isOtherContent = "a")

        // when
        val nextSectionId = setByUser.findNextSectionId(sectionResponse)

        // then
        assertEquals(sectionId, nextSectionId)
    }

    @Test
    fun `SetByChoice를 생성하면 정보들이 올바르게 설정된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs = createSectionRouteConfigs(contentIdMap)

        // when
        val setByChoice = SetByChoiceRouting(keyQuestionId, sectionRouteConfigs)

        // then
        with(setByChoice) {
            assertEquals(SectionRouteType.SET_BY_CHOICE, setByChoice.type)
            assertEquals(null, setByChoice.nextSectionId)
            assertEquals(keyQuestionId, setByChoice.keyQuestionId)
            assertEquals(sectionRouteConfigs, this.sectionRouteConfigs)
        }
    }

    @Test
    fun `SetByChoice는 sectionRouteConfigs의 content의 집합을 가져올 수 있다`() {
        assertEquals(setOf("a", "b", null), setByChoice.getContentSet())
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
        assertEquals(contentIdMap["a"], nextSectionId1)
        assertEquals(contentIdMap["b"], nextSectionId2)
        assertEquals(contentIdMap[null], nextSectionId3)
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

    @Test
    fun `RouteDetails는 목적지가 될 수 있는 SectionId의 집합을 가져올 수 있다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()

        val numericalOrder1 = NumericalOrderRouting(sectionId1)
        val numericalOrder2 = NumericalOrderRouting(sectionId3)
        val setByUser1 = SetByUserRouting(null)
        val setByUser2 = SetByUserRouting(sectionId3)
        val setByChoice = createSetByChoiceRouting(contentIdMap = mapOf("a" to sectionId1, "b" to sectionId2))

        // when, then
        assertEquals(setOf(sectionId1), numericalOrder1.getDestinationSectionIdSet())
        assertEquals(setOf(sectionId3), numericalOrder2.getDestinationSectionIdSet())
        assertEquals(setOf<UUID?>(null), setByUser1.getDestinationSectionIdSet())
        assertEquals(setOf(sectionId3), setByUser2.getDestinationSectionIdSet())
        assertEquals(setOf(sectionId1, sectionId2), setByChoice.getDestinationSectionIdSet())
    }
}
