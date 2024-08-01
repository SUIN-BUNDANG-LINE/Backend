package com.sbl.sulmun2yong.drawing.domain.ticket

data class NonWinningTicket(
    override val isSelected: Boolean,
) : Ticket {
    companion object {
        fun create() =
            NonWinningTicket(
                isSelected = false,
            )
    }
}
