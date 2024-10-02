package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.domain.response.QuestionResponse
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidQuestionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class QuestionResultDetailsTest {
    private val contentA = "A"
    private val contentB = "B"
    private val detailA = ResponseDetail(contentA)
    private val etcDetailA = ResponseDetail(contentA, true)
    private val etcDetailB = ResponseDetail(contentB, true)

    @Test
    fun `ResponseDetail을 생성하면 정보가 올바르게 설정된다`() {
        assertEquals(contentA, detailA.content)
        assertEquals(false, detailA.isOther)
        assertEquals(contentA, etcDetailA.content)
        assertEquals(true, etcDetailA.isOther)
    }

    @Test
    fun `QuestionResponse 생성하면 정보가 올바르게 설정된다`() {
        val id = UUID.randomUUID()
        val responses = listOf(detailA, etcDetailA)
        val questionResponse = QuestionResponse(id, responses)
        assertEquals(id, questionResponse.questionId)
        assertEquals(responses, questionResponse)
    }

    @Test
    fun `응답은 비어있으면 안된다`() {
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), emptyList())
        }
    }

    @Test
    fun `응답의 내용은 중복되면 안된다`() {
        // 내용이 같아도 기타 응답과 일반 응답은 중복이 아니다.
        assertDoesNotThrow { QuestionResponse(UUID.randomUUID(), listOf(detailA, etcDetailA)) }

        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), listOf(detailA, detailA))
        }

        // 기타 응답은 내용이 달라도 중복이다.
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), listOf(detailA, etcDetailA, etcDetailB))
        }
    }

    @Test
    fun `기타 응답은 하나보다 많이 올 수 없다`() {
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), listOf(etcDetailA, etcDetailA))
        }
    }
}
