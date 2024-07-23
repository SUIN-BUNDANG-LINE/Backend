package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionResponseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class QuestionResponseTest {
    private val detailA = ResponseDetail("a")
    private val etcDetailA = ResponseDetail("a", true)

    @Test
    fun `응답은 비어있으면 안된다`() {
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), emptyList())
        }
    }

    @Test
    fun `응답의 내용은 중복되면 안된다`() {
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), listOf(detailA, detailA))
        }

        // 내용이 같아도 기타 응답과 일반 응답은 중복이 아니다.
        assertDoesNotThrow { QuestionResponse(UUID.randomUUID(), listOf(detailA, etcDetailA)) }
    }

    @Test
    fun `기타 응답은 하나보다 많이 올 수 없다`() {
        assertThrows<InvalidQuestionResponseException> {
            QuestionResponse(UUID.randomUUID(), listOf(etcDetailA, etcDetailA))
        }
    }
}
