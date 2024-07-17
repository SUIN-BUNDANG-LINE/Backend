package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.ResponseCommand
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.TextResponseQuestion
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.util.UUID
import kotlin.test.assertEquals

class SectionTest {
    private val a = "a"
    private val b = "b"
    private val c = "c"
    private val choicesABC = listOf(a, b, c)

    private val tQuestionId = UUID.randomUUID()
    private val tQuestionTitle = "textResponseQuestionTitle"
    private val tQuestionDescription = "textResponseQuestionDescription"
    private val requiredTQuestion =
        TextResponseQuestion(tQuestionId, tQuestionTitle, tQuestionDescription, true)
    private val tQuestion =
        TextResponseQuestion(tQuestionId, tQuestionTitle, tQuestionDescription, false)

    private val sQuestionId = UUID.randomUUID()
    private val sQuestionTitle = "singleChoiceQuestionTitle"
    private val sQuestionDescription = "singleChoiceQuestionDescription"
    private val requiredAllowOtherSQuestion =
        SingleChoiceQuestion(
            sQuestionId,
            sQuestionTitle,
            sQuestionDescription,
            true,
            choicesABC,
            true,
        )

    private val allowOtherSQuestion =
        SingleChoiceQuestion(
            sQuestionId,
            sQuestionTitle,
            sQuestionDescription,
            false,
            choicesABC,
            true,
        )

    private val requiredSQuestion =
        SingleChoiceQuestion(
            sQuestionId,
            sQuestionTitle,
            sQuestionDescription,
            true,
            choicesABC,
            false,
        )

    private val mQuestionId = UUID.randomUUID()
    private val mQuestionTitle = "multipleChoiceQuestionTitle"
    private val mQuestionDescription = "multipleChoiceQuestionDescription"
    private val allowOtherMQuestion =
        MultipleChoiceQuestion(
            mQuestionId,
            mQuestionTitle,
            mQuestionDescription,
            false,
            choicesABC,
            true,
        )

    @Test
    fun `기본 섹션을 생성할 수 있다`() {
        // given
        val id = UUID.randomUUID()

        mockStatic(UUID::class.java).use { mockedUUID ->
            mockedUUID.`when`<UUID> { UUID.randomUUID() }.thenReturn(id)

            // when
            val section = Section.create()

            // then
            with(section) {
                assertEquals(id, this.id)
                assertEquals("", this.title)
                assertEquals("", this.description)
                assertEquals(RouteDetails.NumericalOrder(null), this.routeDetails)
                assertEquals(emptyList(), this.questions)
            }
        }
    }

    @Test
    fun `번호순 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val title = "title"
        val description = "description"
        val routeDetails = RouteDetails.NumericalOrder(null)
        val questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)

        // when
        val section =
            Section(
                id = id,
                title = title,
                description = description,
                routeDetails = routeDetails,
                questions = questions,
            )

        // then
        with(section) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    @Test
    fun `선택지 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // keyQuestionId에 해당하는 질문이 있어야한다.
        // 해당 질문의 유형이 SINGLE_CHOICE고, 필수 응답 질문이여야 한다.
        // sectionRouteConfigs에 해당 질문의 선택지, 기타 응답에 대한 라우팅이 설정되어 있어야 한다.

        // given
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()

        // when, then
        assertDoesNotThrow {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, null),
                                SectionRouteConfig(c, sectionId1),
                                SectionRouteConfig(null, sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // keyQuestionId에 해당하는 질문이 없으면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = UUID.randomUUID(),
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, sectionId2),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig(null, sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 유형이 SINGLE_CHOICE가 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = mQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, sectionId2),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig(null, sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 필수 응답 질문이 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, sectionId2),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig(null, null),
                            ),
                    ),
                questions = listOf(requiredTQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(유효하지 않은 선택지 내용이 포함됨).
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, sectionId2),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig("invalid", sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(선택지 일부가 누락됨).
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(isAllowOther가 false인데 null 선택지 포함).
        assertThrows<InvalidSectionException> {
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, sectionId1),
                                SectionRouteConfig(b, null),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig(null, sectionId2),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }
    }

    @Test
    fun `유저 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val title = "title"
        val description = "description"
        val routeDetails = RouteDetails.SetByUser(null)
        val questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)

        // when
        val section =
            Section(
                id = id,
                title = title,
                description = description,
                routeDetails = routeDetails,
                questions = questions,
            )

        // then
        with(section) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    @Test
    fun `섹션은 응답들을 확인하고, 필수 응답 질문에 답변을 하지 않으면 예외를 반환한다`() {
        // given
        val section =
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails =
                    RouteDetails.SetByChoice(
                        keyQuestionId = sQuestionId,
                        sectionRouteConfigs =
                            listOf(
                                SectionRouteConfig(a, null),
                                SectionRouteConfig(b, null),
                                SectionRouteConfig(c, null),
                                SectionRouteConfig(null, null),
                            ),
                    ),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )

        val sectionResponses1: List<SectionResponse> =
            listOf(
                SectionResponse(tQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
                SectionResponse(sQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
            )
        val sectionResponses2: List<SectionResponse> =
            listOf(
                SectionResponse(tQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
                SectionResponse(mQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
            )

        // when, then
        assertDoesNotThrow { section.findNextSectionId(sectionResponses1) }
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponses2) }
    }

    @Test
    fun `섹션은 응답들을 확인하고, 각 질문에 유효하지 않은 답변을 하면 예외를 반환한다`() {
        // given
        val questionId1 = UUID.randomUUID()
        val question1 = mock(Question::class.java)
        `when`(question1.id).thenReturn(questionId1)
        `when`(question1.isRequired).thenReturn(true)
        `when`(question1.isValidResponse(any())).thenReturn(true)

        val questionId2 = UUID.randomUUID()
        val question2 = mock(Question::class.java)
        `when`(question2.id).thenReturn(questionId2)
        `when`(question2.isRequired).thenReturn(true)
        `when`(question2.isValidResponse(any())).thenReturn(false)

        val questions = listOf(question1, question2)

        val section = Section(UUID.randomUUID(), "title", "description", RouteDetails.NumericalOrder(null), questions)

        val sectionResponses: List<SectionResponse> =
            listOf(
                SectionResponse(questionId1, ResponseCommand(listOf(ResponseDetail("a")))),
                SectionResponse(questionId2, ResponseCommand(listOf(ResponseDetail("a")))),
            )

        // when, then
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponses) }
    }

    @Test
    fun `번호순 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val sectionId = UUID.randomUUID()
        val section =
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails = RouteDetails.NumericalOrder(sectionId),
                questions = listOf(tQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        val sectionResponses: List<SectionResponse> =
            listOf(
                SectionResponse(tQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
                SectionResponse(sQuestionId, ResponseCommand(listOf(ResponseDetail("a", true)))),
                SectionResponse(mQuestionId, ResponseCommand(listOf(ResponseDetail("b")))),
            )

        // when, then
        assertEquals(sectionId, section.findNextSectionId(listOf()))
        assertEquals(sectionId, section.findNextSectionId(sectionResponses))
    }

    @Test
    fun `선택지 기반 라우팅 방식의 섹션은 응답에 기반하여 다음 섹션을 결정한다`() {
        // give
        val questions = listOf(tQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)

        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()

        val id = UUID.randomUUID()
        val title = "title"
        val description = "description"
        val routeDetails =
            RouteDetails.SetByChoice(
                keyQuestionId = sQuestionId,
                sectionRouteConfigs =
                    listOf(
                        SectionRouteConfig(a, sectionId1),
                        SectionRouteConfig(b, sectionId2),
                        SectionRouteConfig(c, sectionId3),
                        SectionRouteConfig(null, null),
                    ),
            )
        val section =
            Section(
                id = id,
                title = title,
                description = description,
                routeDetails = routeDetails,
                questions = questions,
            )

        val sectionResponses1: List<SectionResponse> =
            listOf(
                SectionResponse(tQuestionId, ResponseCommand(listOf(ResponseDetail(a)))),
                SectionResponse(sQuestionId, ResponseCommand(listOf(ResponseDetail(a)))),
            )

        val sectionResponses2: List<SectionResponse> =
            listOf(
                SectionResponse(mQuestionId, ResponseCommand(listOf(ResponseDetail(b)))),
                SectionResponse(sQuestionId, ResponseCommand(listOf(ResponseDetail(a, true)))),
            )

        // when
        val nextSectionId1 = section.findNextSectionId(sectionResponses1)
        val nextSectionId2 = section.findNextSectionId(sectionResponses2)

        // then
        assertEquals(sectionId1, nextSectionId1)
        assertEquals(null, nextSectionId2)
    }

    @Test
    fun `유저 기반 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val sectionId = UUID.randomUUID()
        val section =
            Section(
                id = UUID.randomUUID(),
                title = "title",
                description = "description",
                routeDetails = RouteDetails.SetByUser(sectionId),
                questions = listOf(tQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        val sectionResponses: List<SectionResponse> =
            listOf(
                SectionResponse(tQuestionId, ResponseCommand(listOf(ResponseDetail("a")))),
                SectionResponse(sQuestionId, ResponseCommand(listOf(ResponseDetail("a", true)))),
                SectionResponse(mQuestionId, ResponseCommand(listOf(ResponseDetail("b")))),
            )

        // when, then
        assertEquals(sectionId, section.findNextSectionId(listOf()))
        assertEquals(sectionId, section.findNextSectionId(sectionResponses))
    }
}
