package com.sbl.sulmun2yong.fixture.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.global.data.PhoneNumber
import java.util.UUID

object DrawingHistoryFixtureFactory {
    val participantId = UUID.randomUUID()
    val phoneNumber = PhoneNumber("010-1234-5678")
    val drawingBoardId = UUID.randomUUID()
    val selectedTicketIndex = 0
    val ticket =
        com.sbl.sulmun2yong.drawing.domain.ticket.Ticket.Winning(
            "테스트 아메리카노",
            "테스트",
            true,
        )

    fun createdDrawingHistory(): DrawingHistory =
        DrawingHistory.create(
            participantId,
            phoneNumber,
            drawingBoardId,
            selectedTicketIndex,
            ticket,
        )
}
