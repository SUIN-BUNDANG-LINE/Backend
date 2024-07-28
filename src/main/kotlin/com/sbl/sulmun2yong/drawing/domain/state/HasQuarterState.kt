package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class HasQuarterState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectTicket() {
        val selectedTicket = drawingMachine.getSelectedTicket()
        if (selectedTicket.isSelected) {
            drawingMachine.state = drawingMachine.noQuarterState
            throw AlreadySelectedTicketException()
        }

        drawingMachine.state = drawingMachine.openTicketState
    }

    override fun openTicket() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName() = throw InvalidDrawingException()
}
