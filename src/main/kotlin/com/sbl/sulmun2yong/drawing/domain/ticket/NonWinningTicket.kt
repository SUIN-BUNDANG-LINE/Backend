package com.sbl.sulmun2yong.drawing.domain.ticket

data class NonWinningTicket(
    override var isSelected: Boolean,
) : Ticket {
    companion object {
        fun create() =
            NonWinningTicket(
                isSelected = false,
            )
    }
}
