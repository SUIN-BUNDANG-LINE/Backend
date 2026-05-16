package com.sbl.sulmun2yong.survey.listener

import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedStatsConsumedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SurveyResponseStatsEventListenerTest {
    private val listener = SurveyResponseStatsEventListener()

    @Test
    fun `이벤트마다 설문별 응답 수를 1씩 누적한다`() {
        listener.handle(SurveyResponseSubmittedStatsConsumedEvent(surveyId = "s1", currentParticipantCount = 1))
        listener.handle(SurveyResponseSubmittedStatsConsumedEvent(surveyId = "s1", currentParticipantCount = 2))
        listener.handle(SurveyResponseSubmittedStatsConsumedEvent(surveyId = "s2", currentParticipantCount = 1))

        assertEquals(2L, listener.snapshot("s1"))
        assertEquals(1L, listener.snapshot("s2"))
    }

    @Test
    fun `이벤트를 받지 않은 설문의 snapshot은 0이다`() {
        assertEquals(0L, listener.snapshot("unknown"))
    }
}
