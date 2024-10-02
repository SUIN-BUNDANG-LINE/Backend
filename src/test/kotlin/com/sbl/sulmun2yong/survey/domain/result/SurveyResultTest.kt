package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.EXCEPT_STUDENT_FILTER
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.JOB_QUESTION_ID
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
                participantId = participantId1,
                contents = contents1,
            )
        val response2 =
            ResultDetails(
                participantId = participantId1,
                contents = contents2,
            )
        val response3 =
            ResultDetails(
                participantId = participantId2,
                contents = contents3,
            )

        val questionContents1 = sortedSetOf(contents1.first(), contents3.first())
        val questionResult1 =
            QuestionResult(
                questionId = questionId1,
                resultDetails = listOf(response1, response3),
                contents = questionContents1,
            )
        val questionContents2 = sortedSetOf(contents2.first())
        val questionResult2 =
            QuestionResult(
                questionId = questionId2,
                resultDetails = listOf(response2),
                contents = sortedSetOf(contents2.first()),
            )

        // when
        val surveyResult = SurveyResult(listOf(questionResult1, questionResult2))

        // then
        with(surveyResult.questionResults[0]) {
            assertEquals(questionId1, questionId)
            assertEquals(questionContents1, contents)
            assertEquals(contents1, resultDetails[0].contents)
            assertEquals(participantId1, resultDetails[0].participantId)
            assertEquals(contents3, resultDetails[1].contents)
            assertEquals(participantId2, resultDetails[1].participantId)
        }
        with(surveyResult.questionResults[1]) {
            assertEquals(questionId2, questionId)
            assertEquals(questionContents2, contents)
            assertEquals(contents2, resultDetails[0].contents)
            assertEquals(participantId1, resultDetails[0].participantId)
        }
    }

    @Test
    fun `설문 결과 상세의 contents는 비어있을 수 없다`() {
        // given
        val participantId = UUID.randomUUID()
        val contents = emptyList<String>()

        // when, then
        assertThrows<InvalidResultDetailsException> {
            ResultDetails(participantId, contents)
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
    }

    @Test
    fun `설문 결과는 설문 결과 필터를 받으면 필터링된 결과를 반환한다`() {
        // given
        val surveyResult = SURVEY_RESULT
        val exceptStudentAndKOrJFoodFilter = ResultFilter(listOf(EXCEPT_STUDENT_FILTER, K_J_FOOD_FILTER))
        val exceptStudentAndManAndKOrJFoodFilter = ResultFilter(listOf(EXCEPT_STUDENT_FILTER, MAN_FILTER, K_J_FOOD_FILTER))
        val invalidFilter = ResultFilter(listOf(QuestionFilter(UUID.randomUUID(), listOf("invalid_content"), true)))

        // when
        val filteredResult1 = surveyResult.getFilteredResult(exceptStudentAndKOrJFoodFilter)
        val filteredResult2 = surveyResult.getFilteredResult(exceptStudentAndManAndKOrJFoodFilter)
        val filteredResult3 = surveyResult.getFilteredResult(invalidFilter)

        // then
        with(filteredResult1.questionResults.map { it.resultDetails }.flatten()) {
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_2) }
        }
        with(filteredResult2.questionResults.map { it.resultDetails }.flatten()) {
            assertEquals(0, size)
        }
        // 이상한 필터는 무시한다.
        with(filteredResult3.questionResults.map { it.resultDetails }.flatten()) {
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_1) }
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_2) }
            assertTrue { this.containsAll(PARTICIPANT_RESULT_DETAILS_3) }
        }
    }

    @Test
    fun `설문 결과는 질문 ID를 받으면 해당 질문의 응답들만 반환한다`() {
        // given
        val surveyResult = SURVEY_RESULT

        // when
        val jobQuestionResponses = surveyResult.findQuestionResult(JOB_QUESTION_ID)

        // then
        assertEquals(3, jobQuestionResponses?.resultDetails?.size)
        with(jobQuestionResponses!!.resultDetails.map { it.contents }.flatten()) {
            assertEquals(true, containsAll(PARTICIPANT_RESULT_DETAILS_1[0].contents))
            assertEquals(true, containsAll(PARTICIPANT_RESULT_DETAILS_2[0].contents))
            assertEquals(true, containsAll(PARTICIPANT_RESULT_DETAILS_3[0].contents))
        }
    }

    @Test
    fun `설문 결과는 해당 설문의 참여자 수를 가져올 수 있다`() {
        // given
        val surveyResult = SURVEY_RESULT

        // when
        val participantCount = surveyResult.getParticipantCount()

        // then
        assertEquals(3, participantCount)
    }
}
