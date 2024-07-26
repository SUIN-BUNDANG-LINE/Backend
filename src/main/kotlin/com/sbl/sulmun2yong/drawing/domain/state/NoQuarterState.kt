package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class NoQuarterState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() {
        if (drawingMachine.getRemainingTicketCount() == 0) {
            drawingMachine.state = drawingMachine.outOfPaperState
            throw InvalidDrawingException()
        }
        drawingMachine.state = drawingMachine.hasQuarterState
    }

    override fun selectPaper() = throw InvalidDrawingException()

    override fun openPaper() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName(): String = throw InvalidDrawingException()
}
