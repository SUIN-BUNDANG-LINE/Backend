package com.sbl.sulmun2yong.drawing.domain.ticket

class NonWinningTicket(
    override var isSelected: Boolean,
) : Ticket {
    companion object {
        fun create() =
            NonWinningTicket(
                isSelected = false,
            )
    }

    override fun toString(): String = "꽝"
}
