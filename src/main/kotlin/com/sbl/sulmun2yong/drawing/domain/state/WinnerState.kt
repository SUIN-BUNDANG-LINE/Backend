package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class WinnerState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectTicket() = throw InvalidDrawingException()

    override fun openTicket() = throw InvalidDrawingException()

    override fun getResult(): Boolean = true

    override fun getRewardName(): String {
        drawingMachine.state =
            if (drawingMachine.getRemainingTicketCount() == 0) {
                drawingMachine.outOfTicketState
            } else {
                drawingMachine.noQuarterState
            }

        val selectedTicket = drawingMachine.getSelectedTicket()
        if (selectedTicket is WinningTicket) {
            return selectedTicket.rewardName
        } else {
            throw InvalidDrawingException()
        }
    }
}
