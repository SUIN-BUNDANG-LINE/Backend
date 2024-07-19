package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidRouteDetailsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RouteDetailsTest {
    @Test
    fun `NumericalOrder를 생성하면 SectionRouteType과 nextSectionId가 올바르게 설정된다`() {
        // given, when
        val numericalOrder = RouteDetails.NumericalOrder(null)

        // then
        assertEquals(SectionRouteType.NUMERICAL_ORDER, numericalOrder.type)
        assertEquals(null, numericalOrder.nextSectionId)
    }

    @Test
    fun `SetByUser를 생성하면 SectionRouteType과 nextSectionId가 올바르게 설정된다`() {
        // given, when
        val setByUser = RouteDetails.SetByUser(null)

        // then
        assertEquals(SectionRouteType.SET_BY_USER, setByUser.type)
        assertEquals(null, setByUser.nextSectionId)
    }

    @Test
    fun `SetByChoice를 생성하면 정보들이 올바르게 설정된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            listOf(
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("b", UUID.randomUUID()),
            )

        // when
        val setByChoice = RouteDetails.SetByChoice(keyQuestionId, sectionRouteConfigs)

        // then
        with(setByChoice) {
            assertEquals(SectionRouteType.SET_BY_CHOICE, setByChoice.type)
            assertEquals(keyQuestionId, setByChoice.keyQuestionId)
            assertEquals(sectionRouteConfigs, this.sectionRouteConfigs)
        }
    }

    @Test
    fun `SetByChoice는 sectionRouteConfigs에는 중복되는 content가 있으면 안된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs1 =
            listOf(
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("b", UUID.randomUUID()),
                SectionRouteConfig(null, UUID.randomUUID()),
                SectionRouteConfig(null, UUID.randomUUID()),
            )
        val sectionRouteConfigs2 =
            listOf(
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("b", UUID.randomUUID()),
            )

        // when, then
        assertThrows<InvalidRouteDetailsException> { RouteDetails.SetByChoice(keyQuestionId, sectionRouteConfigs1) }
        assertThrows<InvalidRouteDetailsException> { RouteDetails.SetByChoice(keyQuestionId, sectionRouteConfigs2) }
    }

    @Test
    fun `SetByChoice는 keyQuestion의 isAllowOther와 선택지들로 sectionRouteConfigs가 유효한지 판단할 수 있다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            listOf(
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("b", UUID.randomUUID()),
                SectionRouteConfig(null, UUID.randomUUID()),
            )

        val setByChoice = RouteDetails.SetByChoice(keyQuestionId, sectionRouteConfigs)

        // when
        val valid1 = setByChoice.isValidSectionRouteConfig(true, listOf("a", "b"))
        val invalid1 = setByChoice.isValidSectionRouteConfig(false, listOf("a", "b"))
        val invalid2 = setByChoice.isValidSectionRouteConfig(true, listOf("a", "b", "c"))

        // then
        assertEquals(true, valid1)
        assertEquals(false, invalid1)
        assertEquals(false, invalid2)
    }

    @Test
    fun `SetByChoice는 ResponseDetail을 받아서 다음 섹션 ID를 찾을 수 있다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            listOf(
                SectionRouteConfig("a", UUID.randomUUID()),
                SectionRouteConfig("b", UUID.randomUUID()),
                SectionRouteConfig(null, UUID.randomUUID()),
            )

        val setByChoice = RouteDetails.SetByChoice(keyQuestionId, sectionRouteConfigs)

        // when
        val nextSectionId1 = setByChoice.findNextSectionId(ResponseDetail("a", false))
        val nextSectionId2 = setByChoice.findNextSectionId(ResponseDetail("b", false))
        val nextSectionId3 = setByChoice.findNextSectionId(ResponseDetail("c", true))

        // then
        assertEquals(sectionRouteConfigs[0].nextSectionId, nextSectionId1)
        assertEquals(sectionRouteConfigs[1].nextSectionId, nextSectionId2)
        assertEquals(sectionRouteConfigs[2].nextSectionId, nextSectionId3)
        assertThrows<InvalidRouteDetailsException> { setByChoice.findNextSectionId(ResponseDetail("c", false)) }
    }

    @Test
    fun `RouteDetails는 섹션 ID 집합을 받으면, RouteDetails의 nextSectionId가 유효한지 확인할 수 있다`() {
        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()

        val numericalOrder1 = RouteDetails.NumericalOrder(sectionId1)
        val numericalOrder2 = RouteDetails.NumericalOrder(sectionId3)
        val setByUser1 = RouteDetails.SetByUser(null)
        val setByUser2 = RouteDetails.SetByUser(sectionId3)

        val sectionRouteConfigs =
            listOf(
                SectionRouteConfig("a", sectionId1),
                SectionRouteConfig("b", sectionId2),
            )
        val setByChoice = RouteDetails.SetByChoice(UUID.randomUUID(), sectionRouteConfigs)

        val sectionIdSet = setOf(sectionId1, sectionId2)

        // when
        val result1 = numericalOrder1.isRouteDetailsSectionIdValid(sectionIdSet)
        val result2 = numericalOrder2.isRouteDetailsSectionIdValid(sectionIdSet)
        val result3 = setByUser1.isRouteDetailsSectionIdValid(sectionIdSet)
        val result4 = setByUser2.isRouteDetailsSectionIdValid(sectionIdSet)
        val result5 = setByChoice.isRouteDetailsSectionIdValid(sectionIdSet)
        val result6 = setByChoice.isRouteDetailsSectionIdValid(setOf(sectionId1, sectionId3))

        // then
        assertEquals(true, result1)
        assertEquals(false, result2)
        assertEquals(true, result3)
        assertEquals(false, result4)
        assertEquals(true, result5)
        assertEquals(false, result6)
    }
}
