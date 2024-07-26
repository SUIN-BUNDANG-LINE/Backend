package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import java.util.UUID

class DrawingService {
    fun checkHasQuarter(
        surveyId: UUID,
        participantId: UUID,
    ): Boolean {
        if (participantId.toString() == "00363c6a-db22-4df3-b75a-2dd347c8089f") {
            return true
        }
        return false
    }

    fun draw(
        drawingBoard: DrawingBoard,
        selectedNumber: Int,
    ) {
        val drawingMachine = DrawingMachine(drawingBoard, selectedNumber)
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        if (drawingMachine.openPaperAndCheckIsWon()) {
            drawingMachine.getRewardName()
        }
    }
}
