package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.global.data.PhoneNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DrawingHistoryTest {
    @Test
    fun `추첨 기록을 만든다`() {
        // given
        val participantId = UUID.randomUUID()
        val phoneNumber = PhoneNumber("010-1234-5678")
        val drawingBoardId = UUID.randomUUID()
        val selectedTicketIndex = 0
        val ticket =
            com.sbl.sulmun2yong.drawing.domain.ticket.Ticket.WinningTicket(
                "테스트 아메리카노",
                "테스트",
                false,
            )
        // when
        val drawingHistory =
            DrawingHistory.create(
                participantId,
                phoneNumber,
                drawingBoardId,
                selectedTicketIndex,
                ticket,
            )

        // then
        with(drawingHistory) {
            assertNotNull(id)
            assertEquals(participantId, this.participantId)
            assertEquals(phoneNumber, this.phoneNumber)
            assertEquals(drawingBoardId, this.drawingBoardId)
            assertEquals(selectedTicketIndex, this.selectedTicketIndex)
            assertEquals(ticket, this.ticket)
        }
    }
}
