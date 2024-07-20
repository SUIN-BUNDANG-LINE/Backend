package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.QuestionFixtureFactory.createMockQuestion
import com.sbl.sulmun2yong.fixture.QuestionFixtureFactory.createMultipleChoiceQuestion
import com.sbl.sulmun2yong.fixture.QuestionFixtureFactory.createSingleChoiceQuestion
import com.sbl.sulmun2yong.fixture.QuestionFixtureFactory.createTextResponseQuestion
import com.sbl.sulmun2yong.fixture.ResponseFixtureFactory.createQuestionResponse
import com.sbl.sulmun2yong.fixture.RoutingFixtureFactory.createMockSetByChoiceRouting
import com.sbl.sulmun2yong.fixture.SectionFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.SectionFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.SectionFixtureFactory.createSection
import com.sbl.sulmun2yong.survey.domain.question.Choices
import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.routing.NumericalOrderRouting
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfig
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfigs
import com.sbl.sulmun2yong.survey.domain.routing.SetByChoiceRouting
import com.sbl.sulmun2yong.survey.domain.routing.SetByUserRouting
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mockStatic
import java.util.UUID
import kotlin.test.assertEquals

class SectionTest {
    private val a = "a"
    private val b = "b"
    private val c = "c"
    private val choicesABC = Choices(listOf(a, b, c))

    private val tQuestionId = UUID.randomUUID()
    private val requiredTQuestion = createTextResponseQuestion(tQuestionId)
    private val tQuestion = createTextResponseQuestion(id = tQuestionId, isRequired = false)

    private val sQuestionId = UUID.randomUUID()
    private val requiredAllowOtherSQuestion =
        createSingleChoiceQuestion(
            id = sQuestionId,
            choices = choicesABC,
        )
    private val allowOtherSQuestion =
        createSingleChoiceQuestion(
            id = sQuestionId,
            choices = choicesABC,
            isRequired = false,
        )
    private val requiredSQuestion =
        createSingleChoiceQuestion(
            id = sQuestionId,
            choices = choicesABC,
            isAllowOther = false,
        )

    private val mQuestionId = UUID.randomUUID()
    private val allowOtherMQuestion =
        createMultipleChoiceQuestion(
            id = mQuestionId,
            isRequired = false,
            choices = choicesABC,
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
                assertEquals(NumericalOrderRouting(null), this.routeDetails)
                assertEquals(emptyList(), this.questions)
            }
        }
    }

    @Test
    fun `번호순 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val routeDetails = NumericalOrderRouting(null)
        val questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)

        // when
        val section =
            createSection(
                id = id,
                routeDetails = routeDetails,
                questions = questions,
            )

        // then
        with(section) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    @Test
    fun `선택지 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // keyQuestionId에 해당하는 질문이 있어야한다.
        // 해당 질문의 유형이 SINGLE_CHOICE고, 필수 응답 질문이여야 한다.
        // sectionRouteConfigs에 해당 질문의 선택지, 기타 응답에 대한 라우팅이 설정되어 있어야 한다.

        assertDoesNotThrow {
            createSection(
                routeDetails = createMockSetByChoiceRouting(keyQuestionId = sQuestionId),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // keyQuestionId에 해당하는 질문이 없으면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails = createMockSetByChoiceRouting(),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 유형이 SINGLE_CHOICE가 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails = createMockSetByChoiceRouting(keyQuestionId = mQuestionId),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 필수 응답 질문이 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails = createMockSetByChoiceRouting(keyQuestionId = sQuestionId),
                questions = listOf(requiredTQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(유효하지 않은 선택지 내용이 포함됨).
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails =
                    createMockSetByChoiceRouting(
                        keyQuestionId = sQuestionId,
                        contentSet = setOf(a, b, c, "invalid"),
                    ),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(선택지 일부가 누락됨).
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails =
                    createMockSetByChoiceRouting(
                        keyQuestionId = sQuestionId,
                        contentSet = setOf(a, b),
                    ),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }

        // 선택지가 올바르지 않으면 예외를 반환한다(isAllowOther가 false인데 null 선택지 포함).
        assertThrows<InvalidSectionException> {
            createSection(
                routeDetails = createMockSetByChoiceRouting(keyQuestionId = sQuestionId),
                questions = listOf(requiredTQuestion, requiredSQuestion, allowOtherMQuestion),
            )
        }
    }

    @Test
    fun `유저 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val routeDetails = SetByUserRouting(null)
        val questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)

        // when
        val section = createSection(id = id, routeDetails = routeDetails, questions = questions)

        // then
        with(section) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(routeDetails, this.routeDetails)
            assertEquals(questions, this.questions)
        }
    }

    @Test
    fun `섹션은 응답들을 확인하고, 필수 응답 질문에 답변을 하지 않으면 예외를 반환한다`() {
        // given
        val id = UUID.randomUUID()
        val section =
            createSection(
                id = id,
                routeDetails = createMockSetByChoiceRouting(keyQuestionId = sQuestionId),
                questions = listOf(requiredTQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion),
            )

        val sectionResponse1s =
            SectionResponse(
                id,
                listOf(
                    createQuestionResponse(tQuestionId, listOf("a")),
                    createQuestionResponse(sQuestionId, listOf("a")),
                ),
            )

        val sectionResponse2s =
            SectionResponse(
                id,
                listOf(
                    createQuestionResponse(tQuestionId, listOf("a")),
                    createQuestionResponse(mQuestionId, listOf("a")),
                ),
            )

        // when, then
        assertDoesNotThrow { section.findNextSectionId(sectionResponse1s) }
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse2s) }
    }

    @Test
    fun `섹션은 응답들을 확인하고, 각 질문에 유효하지 않은 답변을 하면 예외를 반환한다`() {
        // given
        val questionId1 = UUID.randomUUID()
        val question1 = createMockQuestion(id = questionId1, isRequired = true, isResponseValid = true)
        val questionId2 = UUID.randomUUID()
        val question2 = createMockQuestion(id = questionId2, isRequired = true, isResponseValid = false)
        val questions = listOf(question1, question2)

        val id = UUID.randomUUID()
        val section = createSection(id = id, routeDetails = NumericalOrderRouting(null), questions = questions)

        val sectionResponse =
            SectionResponse(
                id,
                listOf(
                    QuestionResponse(questionId1, listOf(ResponseDetail("a"))),
                    QuestionResponse(questionId2, listOf(ResponseDetail("a"))),
                ),
            )

        // when, then
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse) }
    }

    @Test
    fun `번호순 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val nextSectionId = UUID.randomUUID()
        val currentSectionId = UUID.randomUUID()
        val section =
            Section(
                id = currentSectionId,
                title = "title",
                description = "description",
                routeDetails = NumericalOrderRouting(nextSectionId),
                questions = listOf(tQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        val sectionResponse =
            SectionResponse(
                currentSectionId,
                listOf(
                    QuestionResponse(tQuestionId, listOf(ResponseDetail("a"))),
                    QuestionResponse(sQuestionId, listOf(ResponseDetail("a", true))),
                    QuestionResponse(mQuestionId, listOf(ResponseDetail("b"))),
                ),
            )

        // when, then
        assertEquals(nextSectionId, section.findNextSectionId(SectionResponse(currentSectionId, listOf())))
        assertEquals(nextSectionId, section.findNextSectionId(sectionResponse))
    }

    @Test
    fun `선택지 기반 라우팅 방식의 섹션은 응답에 기반하여 다음 섹션을 결정한다`() {
        // give
        val questions = listOf(tQuestion, requiredAllowOtherSQuestion, allowOtherMQuestion)
        val sectionId1 = UUID.randomUUID()
        val sectionId2 = UUID.randomUUID()
        val sectionId3 = UUID.randomUUID()

        val id = UUID.randomUUID()
        val routeDetails =
            SetByChoiceRouting(
                keyQuestionId = sQuestionId,
                sectionRouteConfigs =
                    SectionRouteConfigs(
                        listOf(
                            SectionRouteConfig(a, sectionId1),
                            SectionRouteConfig(b, sectionId2),
                            SectionRouteConfig(c, sectionId3),
                            SectionRouteConfig(null, null),
                        ),
                    ),
            )
        val section =
            createSection(
                id = id,
                routeDetails = routeDetails,
                questions = questions,
            )

        val sectionResponse1s =
            SectionResponse(
                id,
                listOf(
                    QuestionResponse(tQuestionId, listOf(ResponseDetail(a))),
                    QuestionResponse(sQuestionId, listOf(ResponseDetail(a))),
                ),
            )

        val sectionResponse2s =
            SectionResponse(
                id,
                listOf(
                    QuestionResponse(mQuestionId, listOf(ResponseDetail(b))),
                    QuestionResponse(sQuestionId, listOf(ResponseDetail(a, true))),
                ),
            )

        // when
        val nextSectionId1 = section.findNextSectionId(sectionResponse1s)
        val nextSectionId2 = section.findNextSectionId(sectionResponse2s)

        // then
        assertEquals(sectionId1, nextSectionId1)
        assertEquals(null, nextSectionId2)
    }

    @Test
    fun `유저 기반 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val nextSectionId = UUID.randomUUID()
        val currentSectionId = UUID.randomUUID()
        val section =
            createSection(
                id = currentSectionId,
                routeDetails = SetByUserRouting(nextSectionId),
                questions = listOf(tQuestion, allowOtherSQuestion, allowOtherMQuestion),
            )
        val questionResponses =
            SectionResponse(
                currentSectionId,
                listOf(
                    QuestionResponse(tQuestionId, listOf(ResponseDetail("a"))),
                    QuestionResponse(sQuestionId, listOf(ResponseDetail("a", true))),
                    QuestionResponse(mQuestionId, listOf(ResponseDetail("b"))),
                ),
            )

        // when, then
        assertEquals(nextSectionId, section.findNextSectionId(SectionResponse(currentSectionId, listOf())))
        assertEquals(nextSectionId, section.findNextSectionId(questionResponses))
    }
}
