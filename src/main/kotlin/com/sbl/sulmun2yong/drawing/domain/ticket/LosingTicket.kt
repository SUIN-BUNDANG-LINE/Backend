package com.sbl.sulmun2yong.drawing.domain.ticket

class LosingTicket : Ticket {
    override val isWinningPosition = false
    override var isSelected: Boolean = false
}
