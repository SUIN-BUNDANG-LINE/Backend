package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DrawingBoardService(
    private val drawingBoardAdapter: DrawingBoardAdapter,
) {
    fun getDrawingBoard(surveyId: UUID) {
        drawingBoardAdapter.getDrawingBoard(surveyId)
    }

    fun doDrawing(
        participantId: UUID,
        selectedNumber: Int,
    ): DrawingResultResponse {
        drawingBoardAdapter.checkHasQuarter(participantId)
        TODO()
    }

    fun makeDrawingBoard(surveyId: UUID) {
        drawingBoardAdapter.makeDrawingBoard(surveyId)
    }
}
