package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class NonWinnerState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectPaper() = throw InvalidDrawingException()

    override fun openPaper() = throw InvalidDrawingException()

    override fun getResult(): Boolean {
        drawingMachine.state =
            if (drawingMachine.getRemainingTicketCount() == 0) {
                drawingMachine.outOfPaperState
            } else {
                drawingMachine.noQuarterState
            }
        return false
    }

    override fun getRewardName() = throw InvalidDrawingException()
}
