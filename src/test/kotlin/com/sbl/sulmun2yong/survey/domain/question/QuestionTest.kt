package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.CHOICES
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.DESCRIPTION
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.TITLE
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createMultipleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createSingleChoiceQuestion
import com.sbl.sulmun2yong.fixture.survey.QuestionFixtureFactory.createTextResponseQuestion
import com.sbl.sulmun2yong.fixture.survey.ResponseFixtureFactory.createQuestionResponse
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class QuestionTest {
    private val questionId = UUID.randomUUID()

    private val a = "a"
    private val b = "b"
    private val c = "c"
    private val d = "d"
    private val contents = listOf(a, b, c, d)

    private val dummyResponses =
        listOf(
            createQuestionResponse(id = questionId, contents = listOf(a)),
            createQuestionResponse(id = questionId, contents = listOf(d)),
            createQuestionResponse(id = questionId, isOtherContent = a),
            createQuestionResponse(id = questionId, isOtherContent = d),
            createQuestionResponse(id = questionId, contents = listOf(a, b)),
            createQuestionResponse(id = questionId, contents = listOf(c, d)),
            createQuestionResponse(id = questionId, contents = listOf(d), isOtherContent = a),
            createQuestionResponse(id = questionId, contents = listOf(a, b), isOtherContent = a),
            createQuestionResponse(id = questionId, contents = listOf(a, b), isOtherContent = d),
        )

    private fun validateResponses(
        question: Question,
        expected: List<Boolean>,
    ) {
        dummyResponses.forEachIndexed { index, response -> assertEquals(expected[index], question.isValidResponse(response)) }
    }

    @Test
    fun `Question은 keyQuestion이 될 수 있는지 확인할 수 있다`() {
        // given
        val singleChoiceQuestion1: Question = createSingleChoiceQuestion()
        val singleChoiceQuestion2: Question = createSingleChoiceQuestion(isRequired = false)
        val textResponseQuestion: Question = createTextResponseQuestion()
        val multipleChoiceQuestion: Question = createMultipleChoiceQuestion()

        // when, then
        assertEquals(true, singleChoiceQuestion1.canBeKeyQuestion())
        assertEquals(false, singleChoiceQuestion2.canBeKeyQuestion())
        assertEquals(false, textResponseQuestion.canBeKeyQuestion())
        assertEquals(false, multipleChoiceQuestion.canBeKeyQuestion())
    }

    @Test
    fun `주관식 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 주관식 질문은 선택지가 없고, 질문 유형이 TEXT_RESPONSE다.
        // given
        val id = UUID.randomUUID()
        val isRequired = true

        // when
        val question = createTextResponseQuestion(id, isRequired)

        // then
        with(question) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(isRequired, this.isRequired)
            assertEquals(null, this.choices)
            assertEquals(QuestionType.TEXT_RESPONSE, this.questionType)
        }
    }

    @Test
    fun `주관식 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 주관식 질문은 하나의 응답만 받을 수 있고, 기타 응답을 받을 수 없다.
        // given
        val question = createTextResponseQuestion(id = questionId)

        // when, then
        val expected = listOf(true, true, false, false, false, false, false, false, false)
        validateResponses(question, expected)
    }

    @Test
    fun `단일 선택 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 단일 선택 질문은 선택지가 하나 이상 있고, 기타 응답을 허용할 수 있고, 질문 유형이 SINGLE_CHOICE다.
        // given
        val id = UUID.randomUUID()

        // when
        val question = createSingleChoiceQuestion(id = id)

        // then
        with(question) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(true, this.isRequired)
            assertEquals(CHOICES, this.choices)
            assertEquals(QuestionType.SINGLE_CHOICE, this.questionType)
        }
    }

    @Test
    fun `단일 선택 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 단일 선택 질문은 하나의 응답만 받을 수 있고, 응답은 선택지에 있어야 하고, 기타 응답은 허용된 경우만 받을 수 있다.
        // given
        val allowOtherQuestion = createSingleChoiceQuestion(id = questionId)
        val notAllowOtherQuestion = createSingleChoiceQuestion(id = questionId, isAllowOther = false)
        val allowOtherQuestionExpected = listOf(true, false, true, true, false, false, false, false, false)
        val notAllowOtherQuestionExpected = listOf(true, false, false, false, false, false, false, false, false)

        // when, then
        validateResponses(allowOtherQuestion, allowOtherQuestionExpected)
        validateResponses(notAllowOtherQuestion, notAllowOtherQuestionExpected)
    }

    // @Test
    // fun `단일 선택 질문은 선택지 집합을 받아서 선택지의 content와 같은지 확인할 수 있다`() {
    //     // given
    //     val allowOtherQuestion = createSingleChoiceQuestion(id = questionId)
    //     val notAllowOtherQuestion = createSingleChoiceQuestion(id = questionId, isAllowOther = false)
    //     val choiceA = Choice.from(a)
    //     val choiceB = Choice.from(b)
    //     val choiceC = Choice.from(c)
    //     val choiceD = Choice.from(d)
    //     val choiceOther = Choice.Other
    //
    //     // when
    //     val isAllowEqual1 = allowOtherQuestion.isEqualToChoices(setOf(choiceA, choiceB, choiceC, choiceOther))
    //     val isAllowEqual2 = allowOtherQuestion.isEqualToChoices(setOf(choiceA, choiceB, choiceD, choiceOther))
    //     val isNotAllowEqual1 = notAllowOtherQuestion.isEqualToChoices(setOf(choiceA, choiceB, choiceC))
    //     val isNotAllowEqual2 = notAllowOtherQuestion.isEqualToChoices(setOf(choiceA, choiceB, choiceC, choiceOther))
    //
    //     // then
    //     assertEquals(true, isAllowEqual1)
    //     assertEquals(false, isAllowEqual2)
    //     assertEquals(true, isNotAllowEqual1)
    //     assertEquals(false, isNotAllowEqual2)
    // }

    @Test
    fun `다중 선택 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 다중 선택 질문은 선택지가 하나 이상 있고, 기타 응답을 허용할 수 있고, 질문 유형이 MULTIPLE_CHOICE다.
        // given
        val id = UUID.randomUUID()

        // when
        val question = createMultipleChoiceQuestion(id = id)

        // then
        with(question) {
            assertEquals(id, this.id)
            assertEquals(TITLE + id, this.title)
            assertEquals(DESCRIPTION + id, this.description)
            assertEquals(true, this.isRequired)
            assertEquals(CHOICES, this.choices)
            assertEquals(QuestionType.MULTIPLE_CHOICE, this.questionType)
        }
    }

    @Test
    fun `다중 선택 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 다중 선택 질문은 하나 이상의 응답을 받을 수 있고, 응답은 선택지에 있어야 하고, 기타 응답은 허용된 경우만 받을 수 있다.
        // given
        val questionABC = createMultipleChoiceQuestion(id = questionId, isAllowOther = false)
        val questionABCEtc = createMultipleChoiceQuestion(id = questionId)
        val questionAEtc = createMultipleChoiceQuestion(id = questionId, contents = listOf(a))
        val questionABCExpected = listOf(true, false, false, false, true, false, false, false, false)
        val questionABCEtcExpected = listOf(true, false, true, true, true, false, false, true, true)
        val questionAEtcExpected = listOf(true, false, true, true, false, false, false, false, false)

        // when, then
        validateResponses(questionABC, questionABCExpected)
        validateResponses(questionABCEtc, questionABCEtcExpected)
        validateResponses(questionAEtc, questionAEtcExpected)
    }
}
