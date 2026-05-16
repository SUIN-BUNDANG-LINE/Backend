package com.sbl.sulmun2yong.drawing.listener

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedSmsCostConsumedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmsCostEventListenerTest {
    private val listener = SmsCostEventListener()

    @Test
    fun `당첨자가 아니면 비용을 누적하지 않는다`() {
        listener.handle(
            DrawingCompletedSmsCostConsumedEvent(eventId = "e1", surveyId = "s1", isWinner = false),
        )

        assertEquals(0L, listener.totalCostKrw())
        assertEquals(0L, listener.sentCount())
        assertEquals(0L, listener.costBySurveyKrw("s1"))
    }

    @Test
    fun `당첨자 1건당 총 비용과 발송 수, 설문별 비용을 누적한다`() {
        listener.handle(DrawingCompletedSmsCostConsumedEvent(eventId = "e1", surveyId = "s1", isWinner = true))
        listener.handle(DrawingCompletedSmsCostConsumedEvent(eventId = "e2", surveyId = "s1", isWinner = true))
        listener.handle(DrawingCompletedSmsCostConsumedEvent(eventId = "e3", surveyId = "s2", isWinner = true))

        assertEquals(24L, listener.totalCostKrw()) // 8원 × 3
        assertEquals(3L, listener.sentCount())
        assertEquals(16L, listener.costBySurveyKrw("s1"))
        assertEquals(8L, listener.costBySurveyKrw("s2"))
    }

    @Test
    fun `reset 호출 시 누적값이 모두 0으로 초기화된다`() {
        listener.handle(DrawingCompletedSmsCostConsumedEvent(eventId = "e1", surveyId = "s1", isWinner = true))

        listener.reset()

        assertEquals(0L, listener.totalCostKrw())
        assertEquals(0L, listener.sentCount())
        assertEquals(0L, listener.costBySurveyKrw("s1"))
    }
}
