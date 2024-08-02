package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.fixture.drawing.DrawingHistoryFixtureFactory
import com.sbl.sulmun2yong.fixture.drawing.DrawingHistoryFixtureFactory.drawingBoardId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DrawingHistoryTest {
    @Test
    fun `추첨 기록을 만든다`() {
        // given
        val drawingHistory = DrawingHistoryFixtureFactory.createdDrawingHistory()

        // when, then
        with(drawingHistory) {
            assertNotNull(id)
            assertEquals(participantId, this.participantId)
            assertEquals(phoneNumber, this.phoneNumber)
            assertEquals(drawingBoardId, this.surveyId)
            assertEquals(selectedTicketIndex, this.selectedTicketIndex)
            assertEquals(ticket, this.ticket)
        }
    }

    @Test
    fun `추첨 기록을 그룹을 만든다`() {
        // given
        val drawingHistory1 = DrawingHistoryFixtureFactory.createdDrawingHistory()
        val drawingHistory2 = DrawingHistoryFixtureFactory.createdDrawingHistory()
        val drawingHistory3 = DrawingHistoryFixtureFactory.createdDrawingHistory()

        // when
        val drawingHistoryGroup =
            DrawingHistoryGroup(
                surveyId = drawingBoardId,
                count = 3,
                histories = listOf(drawingHistory1, drawingHistory2, drawingHistory3),
            )

        // then
        with(drawingHistoryGroup) {
            assertEquals(drawingBoardId, this.surveyId)
            assertEquals(3, this.count)
            assertEquals(3, this.histories.size)
        }
    }
}
