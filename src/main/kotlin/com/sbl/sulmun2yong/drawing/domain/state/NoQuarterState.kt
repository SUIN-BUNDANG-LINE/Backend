package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException
import com.sbl.sulmun2yong.drawing.exception.OutOfTicketException

class NoQuarterState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() {
        if (drawingMachine.getRemainingTicketCount() == 0) {
            drawingMachine.state = drawingMachine.outOfTicketState
            throw OutOfTicketException()
        }
        drawingMachine.state = drawingMachine.hasQuarterState
    }

    override fun selectTicket() = throw InvalidDrawingException()

    override fun openTicket() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName(): String = throw InvalidDrawingException()
}
