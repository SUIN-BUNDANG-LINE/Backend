package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createMockQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createMultipleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createSingleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createTextResponseQuestion
import com.sbl.sulmun2yong.fixture.survey.ResponseFixtureFactory.createQuestionResponse
import com.sbl.sulmun2yong.fixture.survey.RoutingFixtureFactory.createMockSetByChoiceRouting
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.survey.SectionFixtureFactory.createSection
import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.CONTENTS
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class SectionTest {
    private val textResponseQuestionId = UUID.randomUUID()
    private val requiredTextResponseQuestion = createTextResponseQuestion(textResponseQuestionId)
    private val textResponseQuestion = createTextResponseQuestion(id = textResponseQuestionId, isRequired = false)

    private val singleChoiceQuestionId = UUID.randomUUID()
    private val requiredAllowOtherSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = CONTENTS,
        )
    private val allowOtherSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = CONTENTS,
            isRequired = false,
        )
    private val requiredSingleChoiceQuestion =
        createSingleChoiceQuestion(
            id = singleChoiceQuestionId,
            contents = CONTENTS,
            isAllowOther = false,
        )

    private val multipleChoiceQuestionId = UUID.randomUUID()
    private val allowOtherMultipleChoiceQuestion =
        createMultipleChoiceQuestion(
            id = multipleChoiceQuestionId,
            isRequired = false,
            contents = CONTENTS,
        )

    @Test
    fun `섹션의 응답은 중복될 수 없다`() {
        // given
        val id = UUID.randomUUID()
        val questionResponse1 = QuestionResponse(id, listOf(ResponseDetail("a")))
        val questionResponse2 = QuestionResponse(id, listOf(ResponseDetail("b")))

        // when, then
        assertThrows<InvalidSurveyResponseException> {
            SectionResponse(SectionId.Standard(UUID.randomUUID()), listOf(questionResponse1, questionResponse2))
        }
    }

    @Test
    fun `섹션을 생성하면 올바르게 정보가 설정된다`() {
        // given
        val id = UUID.randomUUID()
        val routeDetails = RoutingStrategy.NumericalOrder
        val questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion)

        // when
        val section =
            createSection(
                id = id,
                routingStrategy = routeDetails,
                questions = questions,
            )

        // then
        with(section) {
            assertEquals(SectionId.Standard(id), this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(routeDetails, this.routingStrategy)
            assertEquals(questions, this.questions)
            assertEquals(SectionIds.from(listOf(SectionId.Standard(id))), this.sectionIds)
        }
    }

    @Test
    fun `선택지 기반 라우팅 방식의 섹션을 생성하면 올바르게 정보가 설정된다`() {
        // keyQuestionId에 해당하는 질문이 있어야한다.
        // 해당 질문의 유형이 SINGLE_CHOICE고, 필수 응답 질문이여야 한다.
        // sectionRouteConfigs에 해당 질문의 선택지, 기타 응답에 대한 라우팅이 설정되어 있어야 한다.

        assertDoesNotThrow {
            createSection(
                routingStrategy = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId, sectionIds = listOf()),
                questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // keyQuestionId에 해당하는 질문이 없으면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = createMockSetByChoiceRouting(),
                questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // SingleChoiceQuestion이 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = createMockSetByChoiceRouting(keyQuestionId = multipleChoiceQuestionId),
                questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // 필수 응답 질문이 아니면 예외를 반환한다.
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
                questions = listOf(requiredTextResponseQuestion, allowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // 선택지가 옳바르지 않으면 예외를 반환한다(유효하지 않은 선택지 내용이 포함됨).
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy =
                    createMockSetByChoiceRouting(
                        keyQuestionId = singleChoiceQuestionId,
                        choiceSet = createChoiceSet(CONTENTS + "invalid"),
                    ),
                questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // 선택지가 옳바르지 않으면 예외를 반환한다(선택지 일부가 누락됨).
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy =
                    createMockSetByChoiceRouting(
                        keyQuestionId = singleChoiceQuestionId,
                        choiceSet = createChoiceSet(listOf(CONTENTS[0], CONTENTS[1])),
                    ),
                questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }

        // 선택지가 옳바르지 않으면 예외를 반환한다(isAllowOther가 false인데 null 선택지 포함).
        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
                questions = listOf(requiredTextResponseQuestion, requiredSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )
        }
    }

    @Test
    fun `섹션 라우팅으로 나올 수 있는 섹션들이 모두 설문 내에 존재하는 섹션이여야한다`() {
        // given
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val stdId1 = SectionId.Standard(id1)
        val stdId2 = SectionId.Standard(id2)

        val setByUser = RoutingStrategy.SetByUser(stdId1)
        val setByChoice =
            createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId, sectionIds = listOf(stdId1, stdId2, SectionId.End))
        val numericalOrder = RoutingStrategy.NumericalOrder
        val questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion)

        // when, then
        assertDoesNotThrow {
            createSection(
                routingStrategy = setByUser,
                questions = questions,
                sectionIds = listOf(id1, id2),
            )
        }

        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = setByUser,
                questions = questions,
                sectionIds = listOf(id2),
            )
        }

        assertDoesNotThrow {
            createSection(
                routingStrategy = setByChoice,
                questions = questions,
                sectionIds = listOf(id1, id2, UUID.randomUUID()),
            )
        }

        assertThrows<InvalidSectionException> {
            createSection(
                routingStrategy = setByChoice,
                questions = questions,
                sectionIds = listOf(id2, UUID.randomUUID()),
            )
        }

        assertDoesNotThrow {
            createSection(
                routingStrategy = numericalOrder,
                questions = questions,
                sectionIds = listOf(id1),
            )
        }
    }

    @Test
    fun `섹션은 응답들을 확인하고, id가 다르거나, 필수 응답 질문에 답변을 하지 않으면 예외를 반환한다`() {
        // given
        val id = UUID.randomUUID()
        val section =
            createSection(
                id = id,
                routingStrategy = createMockSetByChoiceRouting(keyQuestionId = singleChoiceQuestionId),
                questions = listOf(requiredTextResponseQuestion, requiredAllowOtherSingleChoiceQuestion, allowOtherMultipleChoiceQuestion),
            )

        val sectionResponse1s =
            SectionResponse(
                SectionId.Standard(id),
                listOf(
                    createQuestionResponse(textResponseQuestionId, listOf("a")),
                    createQuestionResponse(singleChoiceQuestionId, listOf("a")),
                ),
            )

        val sectionResponse2s =
            SectionResponse(
                SectionId.Standard(id),
                listOf(
                    createQuestionResponse(textResponseQuestionId, listOf("a")),
                    createQuestionResponse(multipleChoiceQuestionId, listOf("a")),
                ),
            )

        val sectionResponse3s =
            SectionResponse(
                SectionId.Standard(UUID.randomUUID()),
                listOf(
                    createQuestionResponse(textResponseQuestionId, listOf("a")),
                    createQuestionResponse(multipleChoiceQuestionId, listOf("a")),
                ),
            )

        // when, then
        assertDoesNotThrow { section.findNextSectionId(sectionResponse1s) }
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse2s) }
        assertThrows<InvalidSectionResponseException> { section.findNextSectionId(sectionResponse3s) }
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

    @Test
    fun `유저 기반 라우팅 방식의 섹션은 nextQuestionId로 다음 섹션을 결정한다`() {
        // given
        val nextSectionId = UUID.randomUUID()
        val currentSectionId = UUID.randomUUID()
        val section =
            createSection(
                id = currentSectionId,
                routingStrategy = RoutingStrategy.SetByUser(SectionId.Standard(nextSectionId)),
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
