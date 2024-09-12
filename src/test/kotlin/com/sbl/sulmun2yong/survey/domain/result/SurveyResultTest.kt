package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.EXCEPT_STUDENT_FILTER
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.K_J_FOOD_FILTER
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.MAN_FILTER
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.PARTICIPANT_RESULT_DETAILS_1
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.PARTICIPANT_RESULT_DETAILS_2
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.PARTICIPANT_RESULT_DETAILS_3
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.SURVEY_RESULT
import com.sbl.sulmun2yong.survey.exception.InvalidResultDetailsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurveyResultTest {
    @Test
    fun `설문 결과를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val questionId1 = UUID.randomUUID()
        val questionId2 = UUID.randomUUID()
        val participantId1 = UUID.randomUUID()
        val participantId2 = UUID.randomUUID()
        val contents1 = listOf("content1")
        val contents2 = listOf("content2")
        val contents3 = listOf("content3")

        val response1 =
            ResultDetails(
                questionId = questionId1,
                participantId = participantId1,
                contents = contents1,
            )
        val response2 =
            ResultDetails(
                questionId = questionId2,
                participantId = participantId1,
                contents = contents2,
            )
        val response3 =
            ResultDetails(
                questionId = questionId1,
                participantId = participantId2,
                contents = contents3,
            )

        // when
        val surveyResult = SurveyResult(listOf(response1, response2, response3))

        // then
        with(surveyResult.resultDetails[0]) {
            assertEquals(questionId1, questionId)
            assertEquals(participantId1, participantId)
            assertEquals(contents1, contents)
        }
        with(surveyResult.resultDetails[1]) {
            assertEquals(questionId2, questionId)
            assertEquals(participantId1, participantId)
            assertEquals(contents2, contents)
        }
        with(surveyResult.resultDetails[2]) {
            assertEquals(questionId1, questionId)
            assertEquals(participantId2, participantId)
            assertEquals(contents3, contents)
        }
    }

    @Test
    fun `설문 결과 상세의 contents는 비어있을 수 없다`() {
        // given
        val questionId = UUID.randomUUID()
        val participantId = UUID.randomUUID()
        val contents = emptyList<String>()

        // when, then
        assertThrows<InvalidResultDetailsException> {
            ResultDetails(questionId, participantId, contents)
        }
    }

    @Test
    fun `설문 결과 상세는 질문 필터를 받으면 필터링되는 응답인지 확인할 수 있다`() {
        // given
        val jobQuestionDetails1 = PARTICIPANT_RESULT_DETAILS_1[0]
        val genderQuestionDetails1 = PARTICIPANT_RESULT_DETAILS_1[1]
        val foodQuestionDetails1 = PARTICIPANT_RESULT_DETAILS_1[2]

        val jobQuestionDetails2 = PARTICIPANT_RESULT_DETAILS_2[0]
        val genderQuestionDetails2 = PARTICIPANT_RESULT_DETAILS_2[1]
        val foodQuestionDetails2 = PARTICIPANT_RESULT_DETAILS_2[2]

        val jobQuestionDetails3 = PARTICIPANT_RESULT_DETAILS_3[0]
        val genderQuestionDetails3 = PARTICIPANT_RESULT_DETAILS_3[1]
        val foodQuestionDetails3 = PARTICIPANT_RESULT_DETAILS_3[2]

        // when
        // isMatched는 질문 필터의 contents에 응답의 contents가 포함되어 있고,
        // 질문 필터의 questionId가 응답의 questionId가 같은지 판단
        val isStudent1 = jobQuestionDetails1.isMatched(EXCEPT_STUDENT_FILTER)
        val isMan1 = genderQuestionDetails1.isMatched(MAN_FILTER)
        val isKOrJFood1 = foodQuestionDetails1.isMatched(K_J_FOOD_FILTER)

        val isStudent2 = jobQuestionDetails2.isMatched(EXCEPT_STUDENT_FILTER)
        val isMan2 = genderQuestionDetails2.isMatched(MAN_FILTER)
        val isKOrJFood2 = foodQuestionDetails2.isMatched(K_J_FOOD_FILTER)

        val isStudent3 = jobQuestionDetails3.isMatched(EXCEPT_STUDENT_FILTER)
        val isMan3 = genderQuestionDetails3.isMatched(MAN_FILTER)
        val isKOrJFood3 = foodQuestionDetails3.isMatched(K_J_FOOD_FILTER)

        // 다른 질문인 경우
        val differentQuestion = foodQuestionDetails3.isMatched(MAN_FILTER)

        // then
        // 1번 참가자의 응답(학생, 남자, 한식 & 일식, 맛있어요!)
        assertEquals(true, isStudent1)
        assertEquals(true, isMan1)
        assertEquals(true, isKOrJFood1)

        // 2번 참가자의 응답(직장인, 여자, 한식 & 패스트푸드 & 중식, 매콥해요!)
        assertEquals(false, isStudent2)
        assertEquals(false, isMan2)
        assertEquals(true, isKOrJFood2)

        // 3번 참가자의 응답(학생, 여자, 패스트푸드 & 분식, 달아요!)
        assertEquals(true, isStudent3)
        assertEquals(false, isMan3)
        assertEquals(false, isKOrJFood3)

        // 다른 질문인 경우 false
        assertEquals(false, differentQuestion)
    }

    @Test
    fun `설문 결과는 설문 결과 필터를 받으면 필터링된 결과를 반환한다`() {
        // given
        val surveyResult = SURVEY_RESULT
        val exceptStudentAndKOrJFoodFilter = ResultFilter(listOf(EXCEPT_STUDENT_FILTER, K_J_FOOD_FILTER))
        val exceptStudentAndManAndKOrJFoodFilter = ResultFilter(listOf(EXCEPT_STUDENT_FILTER, MAN_FILTER, K_J_FOOD_FILTER))
        val emptyFilter = ResultFilter(listOf())

        // when
        val filteredResult1 = surveyResult.getFilteredResult(exceptStudentAndKOrJFoodFilter)
        val filteredResult2 = surveyResult.getFilteredResult(exceptStudentAndManAndKOrJFoodFilter)
        val filteredResult3 = surveyResult.getFilteredResult(emptyFilter)

        // then
        with(filteredResult1.resultDetails) {
            assertEquals(4, size)
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_2) }
        }
        with(filteredResult2.resultDetails) {
            assertEquals(0, size)
        }
        with(filteredResult3.resultDetails) {
            assertEquals(12, size)
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_2) }
        }
    }
}
