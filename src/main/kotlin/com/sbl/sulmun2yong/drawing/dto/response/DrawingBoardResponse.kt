package com.sbl.sulmun2yong.drawing.dto.response

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import java.util.UUID

class DrawingBoardResponse(
    val id: UUID,
    val surveyId: UUID,
    val tickets: Array<Boolean>,
) {
    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardResponse =
            DrawingBoardResponse(
                id = drawingBoard.id,
                surveyId = drawingBoard.surveyId,
                tickets = drawingBoard.tickets.map { it.isSelected }.toTypedArray(),
            )
    }
}
