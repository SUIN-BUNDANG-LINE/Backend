package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.EXCEPT_STUDENT_FILTER
import com.sbl.sulmun2yong.fixture.survey.SurveyResultConstFactory.MAN_FILTER
import com.sbl.sulmun2yong.survey.exception.InvalidQuestionFilterException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class ResultFilterTest {
    @Test
    fun `질문 필터를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val questionId = UUID.randomUUID()
        val contents = listOf("content1", "content2")
        val isPositive = true

        // when
        val questionFilter = QuestionFilter(questionId, contents, isPositive)

        // then
        with(questionFilter) {
            assertEquals(questionId, this.questionId)
            assertEquals(contents, this.contents)
            assertEquals(isPositive, this.isPositive)
        }
    }

    @Test
    fun `질문 필터의 contents는 비어있을 수 없다`() {
        assertThrows<InvalidQuestionFilterException> { QuestionFilter(UUID.randomUUID(), emptyList(), true) }
    }

    @Test
    fun `설문 결과 필터를 생성하면 정보가 올바르게 설정된다`() {
        // given
        val questionFilters = listOf(EXCEPT_STUDENT_FILTER, MAN_FILTER)

        // when
        val resultFilter = ResultFilter(questionFilters)

        // then
        assertEquals(questionFilters, resultFilter.questionFilters)
    }
}
