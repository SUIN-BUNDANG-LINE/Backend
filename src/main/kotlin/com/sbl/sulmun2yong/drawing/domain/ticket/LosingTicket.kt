package com.sbl.sulmun2yong.drawing.domain.ticket

class LosingTicket : Ticket {
    override var isSelected: Boolean = false
    override val isWinning: Boolean = false

    override fun toString(): String = "꽝"
}
