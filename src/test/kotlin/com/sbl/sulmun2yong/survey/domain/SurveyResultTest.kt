package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals

class SurveyResultTest {
    @Test
    fun `설문 결과를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val questionId1 = UUID.randomUUID()
        val questionId2 = UUID.randomUUID()
        val participantId1 = UUID.randomUUID()
        val participantId2 = UUID.randomUUID()
        val content1 = "content1"
        val content2 = "content2"
        val content3 = "content3"
        val date = Date()

        val response1 =
            SurveyResult.Response(
                questionId = questionId1,
                participantId = participantId1,
                content = content1,
                createdAt = date,
            )
        val response2 =
            SurveyResult.Response(
                questionId = questionId2,
                participantId = participantId1,
                content = content2,
                createdAt = date,
            )
        val response3 =
            SurveyResult.Response(
                questionId = questionId1,
                participantId = participantId2,
                content = content3,
                createdAt = date,
            )

        // when
        val surveyResult = SurveyResult(listOf(response1, response2, response3))

        // then
        with(surveyResult.responses[0]) {
            assertEquals(questionId1, questionId)
            assertEquals(participantId1, participantId)
            assertEquals(content1, content)
            assertEquals(date, createdAt)
        }
        with(surveyResult.responses[1]) {
            assertEquals(questionId2, questionId)
            assertEquals(participantId1, participantId)
            assertEquals(content2, content)
            assertEquals(date, createdAt)
        }
        with(surveyResult.responses[2]) {
            assertEquals(questionId1, questionId)
            assertEquals(participantId2, participantId)
            assertEquals(content3, content)
            assertEquals(date, createdAt)
        }
    }
}
