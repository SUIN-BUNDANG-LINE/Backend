package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class OutOfTicketState : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectTicket() = throw InvalidDrawingException()

    override fun openTicket() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName() = throw InvalidDrawingException()
}
