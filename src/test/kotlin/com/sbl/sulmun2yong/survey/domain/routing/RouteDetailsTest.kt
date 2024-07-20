package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.SectionResponse
import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class RouteDetailsTest {
    @Test
    fun `NumericalOrder를 생성하면 SectionRouteType과 nextSectionId가 올바르게 설정된다`() {
        // given, when
        val numericalOrder = NumericalOrderRouting(null)

        // then
        assertEquals(SectionRouteType.NUMERICAL_ORDER, numericalOrder.type)
        assertEquals(null, numericalOrder.nextSectionId)
    }

    @Test
    fun `SetByUser를 생성하면 SectionRouteType과 nextSectionId가 올바르게 설정된다`() {
        // given, when
        val setByUser = SetByUserRouting(null)

        // then
        assertEquals(SectionRouteType.SET_BY_USER, setByUser.type)
        assertEquals(null, setByUser.nextSectionId)
    }

    @Test
    fun `SetByChoice를 생성하면 정보들이 올바르게 설정된다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("b", UUID.randomUUID()),
                ),
            )

        // when
        val setByChoice = SetByChoiceRouting(keyQuestionId, sectionRouteConfigs)

        // then
        with(setByChoice) {
            assertEquals(SectionRouteType.SET_BY_CHOICE, setByChoice.type)
            assertEquals(keyQuestionId, setByChoice.keyQuestionId)
            assertEquals(sectionRouteConfigs, this.sectionRouteConfigs)
        }
    }

    @Test
    fun `SetByChoice는 sectionRouteConfigs의 content의 집합을 가져올 수 있다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("b", UUID.randomUUID()),
                    SectionRouteConfig(null, UUID.randomUUID()),
                ),
            )

        val setByChoice = SetByChoiceRouting(keyQuestionId, sectionRouteConfigs)

        // when
        val contentSet = setByChoice.getContentSet()

        // then
        assertEquals(setOf("a", "b", null), contentSet)
    }

    @Test
    fun `SetByChoice는 SectionResponse를 받아서 다음 섹션 ID를 찾을 수 있다`() {
        // given
        val keyQuestionId = UUID.randomUUID()
        val sectionRouteConfigs =
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("b", UUID.randomUUID()),
                    SectionRouteConfig(null, UUID.randomUUID()),
                ),
            )
        // TODO: 테스트 코드 보완
        val setByChoice = SetByChoiceRouting(keyQuestionId, sectionRouteConfigs)

        val sectionId = UUID.randomUUID()
        val sectionResponse1 = SectionResponse(sectionId, listOf(QuestionResponse(keyQuestionId, listOf(ResponseDetail("a", false)))))
        val sectionResponse2 = SectionResponse(sectionId, listOf(QuestionResponse(keyQuestionId, listOf(ResponseDetail("b", false)))))
        val sectionResponse3 = SectionResponse(sectionId, listOf(QuestionResponse(keyQuestionId, listOf(ResponseDetail("c", true)))))
        val sectionResponse4 = SectionResponse(sectionId, listOf(QuestionResponse(keyQuestionId, listOf(ResponseDetail("c", false)))))

        // when
        val nextSectionId1 = setByChoice.findNextSectionId(sectionResponse1)
        val nextSectionId2 = setByChoice.findNextSectionId(sectionResponse2)
        val nextSectionId3 = setByChoice.findNextSectionId(sectionResponse3)

        // then
        assertEquals(sectionRouteConfigs[0].nextSectionId, nextSectionId1)
        assertEquals(sectionRouteConfigs[1].nextSectionId, nextSectionId2)
        assertEquals(sectionRouteConfigs[2].nextSectionId, nextSectionId3)
        assertThrows<InvalidSectionResponseException> { setByChoice.findNextSectionId(sectionResponse4) }
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

        val sectionRouteConfigs =
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", sectionId1),
                    SectionRouteConfig("b", sectionId2),
                ),
            )
        val setByChoice = SetByChoiceRouting(UUID.randomUUID(), sectionRouteConfigs)

        val sectionIdSet1 = setOf(sectionId1)
        val sectionIdSet3 = setOf(sectionId3)
        val sectionIdSetNull = setOf<UUID?>(null)
        val sectionIdSet12 = setOf(sectionId1, sectionId2)

        // when
        val result1 = numericalOrder1.getDestinationSectionIdSet()
        val result2 = numericalOrder2.getDestinationSectionIdSet()
        val result3 = setByUser1.getDestinationSectionIdSet()
        val result4 = setByUser2.getDestinationSectionIdSet()
        val result5 = setByChoice.getDestinationSectionIdSet()

        // then
        assertEquals(sectionIdSet1, result1)
        assertEquals(sectionIdSet3, result2)
        assertEquals(sectionIdSetNull, result3)
        assertEquals(sectionIdSet3, result4)
        assertEquals(sectionIdSet12, result5)
    }
}
