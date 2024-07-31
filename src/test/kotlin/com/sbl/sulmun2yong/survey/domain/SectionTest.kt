package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createMockQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createMultipleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createSingleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createTextResponseQuestion
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.createSection
import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class SectionTest {
    private val a = "a"
    private val b = "b"
    private val c = "c"
    private val contentsABC = listOf(a, b, c)

    private val textResponseQuestionId = UUID.randomUUID()
    private val requiredTextResponseQuestion = createTextResponseQuestion(textResponseQuestionId)
    private val textResponseQuestion = createTextResponseQuestion(id = textResponseQuestionId, isRequired = false)

    private val singleChoiceQuestionId = UUID.randomUUID()
    private val requiredAllowOtherSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = contentsABC,
        )
    private val allowOtherSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = contentsABC,
            isRequired = false,
        )
    private val requiredSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = contentsABC,
            isAllowOther = false,
        )

    private val multipleChoiceQuestionId = UUID.randomUUID()
    private val allowOtherMultipleChoiceQuestion =
        createMultipleChoiceQuestion(
            id = multipleChoiceQuestionId,
            isRequired = false,
            contents = contentsABC,
        )

    @Test
    fun `번호순 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val routeDetails = RouteDetails.NumericalOrderRouting
        val questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion)

        // when
        val section =
            createSection(
                id = id,
                routeDetails = routeDetails,
                questions = questions,
            )

        // then
        with(section) {
            assertEquals(SectionId.Standard(id), this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    // @Test
    // fun `선택지 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
    //     // keyQuestionId에 해당하는 질문이 있어야한다.
    //     // 해당 질문의 유형이 SINGLE_CHOICE고, 필수 응답 질문이여야 한다.
    //     // sectionRouteConfigs에 해당 질문의 선택지, 기타 응답에 대한 라우팅이 설정되어 있어야 한다.
    //
    //     assertDoesNotThrow {
    //         createSection(
    //             routeDetails = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
    //             questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // keyQuestionId에 해당하는 질문이 없으면 예외를 반환한다.
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails = createMockSetByChoiceRouting(),
    //             questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // 유형이 SINGLE_CHOICE가 아니면 예외를 반환한다.
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails = createMockSetByChoiceRouting(keyQuestionId = multipleChoiceQuestionId),
    //             questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // 필수 응답 질문이 아니면 예외를 반환한다.
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
    //             questions = listOf(requiredTextResponseQuestion, allowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // 선택지가 옳바르지 않으면 예외를 반환한다(유효하지 않은 선택지 내용이 포함됨).
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails =
    //                 createMockSetByChoiceRouting(
    //                     keyQuestionId = singleChoiceQuestionId,
    //                     choiceSet = createChoiceSet(listOf(a, b, c, "invalid")),
    //                 ),
    //             questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // 선택지가 옳바르지 않으면 예외를 반환한다(선택지 일부가 누락됨).
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails =
    //                 createMockSetByChoiceRouting(
    //                     keyQuestionId = singleChoiceQuestionId,
    //                     choiceSet = createChoiceSet(listOf(a, b)),
    //                 ),
    //             questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    //
    //     // 선택지가 옳바르지 않으면 예외를 반환한다(isAllowOther가 false인데 null 선택지 포함).
    //     assertThrows<InvalidSectionException> {
    //         createSection(
    //             routeDetails = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
    //             questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //     }
    // }

    @Test
    fun `유저 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val routeDetails = RouteDetails.SetByUserRouting(SectionId.End)
        val questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion)

        // when
        val section = createSection(id = id, routeDetails = routeDetails, questions = questions, sectionIds = listOf(id))

        // then
        with(section) {
            assertEquals(SectionId.Standard(id), this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    // @Test
    // fun `섹션은 응답들을 확인하고, id가 다르거나, 필수 응답 질문에 답변을 하지 않으면 예외를 반환한다`() {
    //     // given
    //     val id = UUID.randomUUID()
    //     val section =
    //         createSection(
    //             id = id,
    //             routeDetails = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
    //             questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
    //         )
    //
    //     val sectionResponse1s =
    //         SectionResponse(
    //             SectionId.Standard(id),
    //             listOf(
    //                 createQuestionResponse(textResponseQuestionId, listOf("a")),
    //                 createQuestionResponse(singleChoiceQuestionId, listOf("a")),
    //             ),
    //         )
    //
    //     val sectionResponse2s =
    //         SectionResponse(
    //             SectionId.Standard(id),
    //             listOf(
    //                 createQuestionResponse(textResponseQuestionId, listOf("a")),
    //                 createQuestionResponse(multipleChoiceQuestionId, listOf("a")),
    //             ),
    //         )
    //
    //     val sectionResponse3s =
    //         SectionResponse(
    //             SectionId.Standard(UUID.randomUUID()),
    //             listOf(
    //                 createQuestionResponse(textResponseQuestionId, listOf("a")),
    //                 createQuestionResponse(multipleChoiceQuestionId, listOf("a")),
    //             ),
    //         )
    //
    //     // when, then
    //     assertDoesNotThrow { section.findNextSectionId(sectionResponse1s) }
    //     assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse2s) }
    //     assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse3s) }
    // }

    @Test
    fun `섹션은 응답들을 확인하고, 각 질문에 유효하지 않은 답변을 하면 예외를 반환한다`() {
        // given
        val questionId1 = UUID.randomUUID()
        val question1 = createMockQuestion(id = questionId1, isRequired = true, isResponseValid = true)
        val questionId2 = UUID.randomUUID()
        val question2 = createMockQuestion(id = questionId2, isRequired = true, isResponseValid = false)
        val questions = listOf(question1, question2)

        val id = UUID.randomUUID()
        val section = createSection(id = id, questions = questions)

        val sectionResponse =
            SectionResponse(
                SectionId.Standard(id),
                listOf(
                    QuestionResponse(questionId1, listOf(ResponseDetail("a"))),
                    QuestionResponse(questionId2, listOf(ResponseDetail("a"))),
                ),
            )

        // when, then
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse) }
    }

    @Test
    fun `번호순 라우팅 방식의 섹션은 현대 섹션의 다음 index 섹션으로 다음 섹션을 결정한다`() {
        // given
        val nextSectionId = UUID.randomUUID()
        val currentSectionId = UUID.randomUUID()
        val section =
            createSection(
                id = currentSectionId,
                questions = listOf(textResponseQuestion, allowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
                sectionIds = listOf(currentSectionId, nextSectionId),
            )
        val sectionResponse =
            SectionResponse(
                SectionId.Standard(currentSectionId),
                listOf(
                    QuestionResponse(textResponseQuestionId, listOf(ResponseDetail("a"))),
                    QuestionResponse(singleChoiceQuestionId, listOf(ResponseDetail("a", true))),
                    QuestionResponse(multipleChoiceQuestionId, listOf(ResponseDetail("b"))),
                ),
            )

        // when, then
        assertEquals(
            SectionId.Standard(nextSectionId),
            section.findNextSectionId(SectionResponse(SectionId.Standard(currentSectionId), listOf())),
        )
        assertEquals(SectionId.Standard(nextSectionId), section.findNextSectionId(sectionResponse))
    }

    // @Test
    // fun `선택지 기반 라우팅 방식의 섹션은 응답에 기반하여 다음 섹션을 결정한다`() {
    //     // give
    //     val questions = listOf(textResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion)
    //     val sectionId1 = UUID.randomUUID()
    //     val sectionId2 = UUID.randomUUID()
    //     val sectionId3 = UUID.randomUUID()
    //
    //     val id = UUID.randomUUID()
    //     val routeDetails =
    //         RouteDetails.SetByChoiceRouting(
    //             keyQuestionId = singleChoiceQuestionId,
    //             sectionRouteConfigs = createSectionRouteConfigs(mapOf(a to sectionId1, b to sectionId2, c to sectionId3, null to null)),
    //         )
    //     val section =
    //         createSection(
    //             id = id,
    //             routeDetails = routeDetails,
    //             questions = questions,
    //             sectionIds = listOf(sectionId1, sectionId2, sectionId3),
    //         )
    //
    //     val sectionResponse1s =
    //         SectionResponse(
    //             SectionId.Standard(id),
    //             listOf(
    //                 QuestionResponse(textResponseQuestionId, listOf(ResponseDetail(a))),
    //                 QuestionResponse(singleChoiceQuestionId, listOf(ResponseDetail(a))),
    //             ),
    //         )
    //
    //     val sectionResponse2s =
    //         SectionResponse(
    //             SectionId.Standard(id),
    //             listOf(
    //                 QuestionResponse(multipleChoiceQuestionId, listOf(ResponseDetail(b))),
    //                 QuestionResponse(singleChoiceQuestionId, listOf(ResponseDetail(a, true))),
    //             ),
    //         )
    //
    //     // when
    //     val nextSectionId1 = section.findNextSectionId(sectionResponse1s)
    //     val nextSectionId2 = section.findNextSectionId(sectionResponse2s)
    //
    //     // then
    //     assertEquals(SectionId.Standard(sectionId1), nextSectionId1)
    //     assertEquals(SectionId.End, nextSectionId2)
    // }

    @Test
    fun `유저 기반 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val nextSectionId = UUID.randomUUID()
        val currentSectionId = UUID.randomUUID()
        val section =
            createSection(
                id = currentSectionId,
                routeDetails = RouteDetails.SetByUserRouting(SectionId.Standard(nextSectionId)),
                questions = listOf(textResponseQuestion, allowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
                sectionIds = listOf(currentSectionId, nextSectionId),
            )
        val questionResponses =
            SectionResponse(
                SectionId.Standard(currentSectionId),
                listOf(
                    QuestionResponse(textResponseQuestionId, listOf(ResponseDetail("a"))),
                    QuestionResponse(singleChoiceQuestionId, listOf(ResponseDetail("a", true))),
                    QuestionResponse(multipleChoiceQuestionId, listOf(ResponseDetail("b"))),
                ),
            )

        // when, then
        assertEquals(
            SectionId.Standard(nextSectionId),
            section.findNextSectionId(SectionResponse(SectionId.Standard(currentSectionId), listOf())),
        )
        assertEquals(SectionId.Standard(nextSectionId), section.findNextSectionId(questionResponses))
    }

    private fun createChoiceSet(contents: List<String?>) = contents.map { Choice.from(it) }.toSet()
}
