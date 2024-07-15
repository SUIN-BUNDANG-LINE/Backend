package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class QuestionTest {
    private val a = "a"
    private val b = "b"
    private val c = "c"
    private val d = "d"
    private val detailA = ResponseDetail(a)
    private val detailB = ResponseDetail(b)
    private val detailC = ResponseDetail(c)
    private val detailD = ResponseDetail(d)
    private val etcDetailA = ResponseDetail(a, true)
    private val etcDetailD = ResponseDetail(d, true)

    private val singleResponseA = ResponseCommand(listOf(detailA))
    private val singleResponseD = ResponseCommand(listOf(detailD))
    private val etcSingleResponseA = ResponseCommand(listOf(etcDetailA))
    private val etcSingleResponseD = ResponseCommand(listOf(etcDetailD))

    private val multipleResponseAB = ResponseCommand(listOf(detailA, detailB))
    private val multipleResponseCD = ResponseCommand(listOf(detailC, detailD))
    private val multipleResponseCEtcA = ResponseCommand(listOf(detailC, etcDetailA))
    private val multipleResponseABEtcA = ResponseCommand(listOf(detailA, detailB, etcDetailA))
    private val multipleResponseABEtcD = ResponseCommand(listOf(detailA, detailB, etcDetailD))

    private val choicesABC = listOf(a, b, c)
    private val choicesA = listOf(a)

    @Test
    fun `주관식 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 선택지가 없고, 기타 응답을 허용할 수 없고, 질문 유형이 TEXT_RESPONSE다

        // given
        val id = UUID.randomUUID()
        val title = "주관식 질문 제목"
        val description = "주관식 질문 설명"
        val isRequired = true

        // when
        val question: Question =
            TextResponseQuestion(
                id = id,
                title = title,
                description = description,
                isRequired = isRequired,
            )

        // then
        with(question) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(isRequired, this.isRequired)
            assertEquals(null, this.choices)
            assertEquals(false, this.isAllowOther)
            assertEquals(QuestionType.TEXT_RESPONSE, this.questionType)
        }
    }

    @Test
    fun `주관식 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 하나의 응답만 받을 수 있고, 기타 응답을 받을 수 없다

        // given
        val question: Question =
            TextResponseQuestion(
                id = UUID.randomUUID(),
                title = "주관식 질문 제목",
                description = "주관식 질문 설명",
                isRequired = true,
            )

        // when, then
        with(question) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(true, isValidResponse(singleResponseD))
            assertEquals(false, isValidResponse(etcSingleResponseA))
            assertEquals(false, isValidResponse(etcSingleResponseD))
            assertEquals(false, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(false, isValidResponse(multipleResponseCEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcD))
        }
    }

    @Test
    fun `단일 선택 질문과 다중 선택 질문의 선택지는 비어있으면 안된다`() {
        // given, when, then
        assertThrows<InvalidQuestionException> {
            SingleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "단일 선택 질문 제목",
                description = "단일 선택 질문 설명",
                isRequired = true,
                choices = listOf(),
                isAllowOther = false,
            )
        }

        assertThrows<InvalidQuestionException> {
            MultipleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "다중 선택 질문 제목",
                description = "다중 선택 질문 설명",
                isRequired = false,
                choices = listOf(),
                isAllowOther = true,
            )
        }
    }

    @Test
    fun `단일 선택 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 선택지가 하나 이상 있고, 기타 응답을 허용할 수 있고, 질문 유형이 SINGLE_CHOICE다

        // given
        val id = UUID.randomUUID()
        val title = "단일 선택 질문 제목"
        val description = "단일 선택 질문 설명"
        val isRequired = true
        val isAllowOther = false

        // when
        val questionABC: Question =
            SingleChoiceQuestion(
                id = id,
                title = title,
                description = description,
                isRequired = isRequired,
                choices = choicesABC,
                isAllowOther = isAllowOther,
            )

        // then
        with(questionABC) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(isRequired, this.isRequired)
            assertEquals(choicesABC, this.choices)
            assertEquals(isAllowOther, this.isAllowOther)
            assertEquals(QuestionType.SINGLE_CHOICE, this.questionType)
        }
    }

    @Test
    fun `단일 선택 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 하나의 응답만 받을 수 있고, 응답은 선택지에 있어야 하고, 기타 응답은 허용된 경우만 받을 수 있다

        // given
        val questionABC: Question =
            SingleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "단일 선택 질문 제목 1",
                description = "단일 선택 질문 설명 1",
                isRequired = true,
                choices = choicesABC,
                isAllowOther = false,
            )

        val questionABCEtc: Question =
            SingleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "단일 선택 질문 제목 2",
                description = "단일 선택 질문 설명 2",
                isRequired = false,
                choices = choicesABC,
                isAllowOther = true,
            )

        // when, then
        with(questionABC) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(false, isValidResponse(singleResponseD))
            assertEquals(false, isValidResponse(etcSingleResponseA))
            assertEquals(false, isValidResponse(etcSingleResponseD))
            assertEquals(false, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(false, isValidResponse(multipleResponseCEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcD))
        }
        with(questionABCEtc) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(false, isValidResponse(singleResponseD))
            assertEquals(true, isValidResponse(etcSingleResponseA))
            assertEquals(true, isValidResponse(etcSingleResponseD))
            assertEquals(false, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(false, isValidResponse(multipleResponseCEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcD))
        }
    }

    @Test
    fun `다중 선택 질문을 생성하면 올바르게 정보가 설정된다`() {
        // 선택지가 하나 이상 있고, 기타 응답을 허용할 수 있고, 질문 유형이 MULTIPLE_CHOICE다

        // given
        val id = UUID.randomUUID()
        val title = "다중 선택 질문 제목"
        val description = "다중 선택 질문 설명"
        val isRequired = false
        val isAllowOther = false

        // when
        val questionABC: Question =
            MultipleChoiceQuestion(
                id = id,
                title = title,
                description = description,
                isRequired = isRequired,
                choices = choicesABC,
                isAllowOther = isAllowOther,
            )

        // then
        with(questionABC) {
            assertEquals(id, this.id)
            assertEquals(title, this.title)
            assertEquals(description, this.description)
            assertEquals(isRequired, this.isRequired)
            assertEquals(choicesABC, this.choices)
            assertEquals(isAllowOther, this.isAllowOther)
            assertEquals(QuestionType.MULTIPLE_CHOICE, this.questionType)
        }
    }

    @Test
    fun `다중 선택 질문은 응답을 받으면 유효성을 검증할 수 있다`() {
        // 하나 이상의 응답을 받을 수 있고, 응답은 선택지에 있어야 하고, 기타 응답은 허용된 경우만 받을 수 있다

        // given
        val questionABC: Question =
            MultipleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "제목 1",
                description = "선택지가 A,B,C",
                isRequired = false,
                choices = choicesABC,
                isAllowOther = false,
            )

        val questionABCEtc: Question =
            MultipleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "제목 2",
                description = "선택지가 A,B,C고 기타 응답 허용",
                isRequired = true,
                choices = choicesABC,
                isAllowOther = true,
            )

        val questionAEtc: Question =
            MultipleChoiceQuestion(
                id = UUID.randomUUID(),
                title = "제목 3",
                description = "선택지가 A고 기타 응답 허용",
                isRequired = false,
                choices = choicesA,
                isAllowOther = true,
            )

        // when, then
        with(questionABC) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(false, isValidResponse(singleResponseD))
            assertEquals(false, isValidResponse(etcSingleResponseA))
            assertEquals(false, isValidResponse(etcSingleResponseD))
            assertEquals(true, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(false, isValidResponse(multipleResponseCEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcD))
        }
        with(questionABCEtc) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(false, isValidResponse(singleResponseD))
            assertEquals(true, isValidResponse(etcSingleResponseA))
            assertEquals(true, isValidResponse(etcSingleResponseD))
            assertEquals(true, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(true, isValidResponse(multipleResponseCEtcA))
            assertEquals(true, isValidResponse(multipleResponseABEtcA))
            assertEquals(true, isValidResponse(multipleResponseABEtcD))
        }
        with(questionAEtc) {
            assertEquals(true, isValidResponse(singleResponseA))
            assertEquals(false, isValidResponse(singleResponseD))
            assertEquals(true, isValidResponse(etcSingleResponseA))
            assertEquals(true, isValidResponse(etcSingleResponseD))
            assertEquals(false, isValidResponse(multipleResponseAB))
            assertEquals(false, isValidResponse(multipleResponseCD))
            assertEquals(false, isValidResponse(multipleResponseCEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcA))
            assertEquals(false, isValidResponse(multipleResponseABEtcD))
        }
    }
}
