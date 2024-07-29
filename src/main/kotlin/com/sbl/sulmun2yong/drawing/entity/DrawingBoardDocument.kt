package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingBoards")
data class DrawingBoardDocument(
    @Id
    val id: UUID,
    @Indexed
    val surveyId: UUID,
    val selectedTicketCount: Int,
    val tickets: Array<TicketSubDocument>,
) : BaseTimeDocument() {
    sealed class TicketSubDocument {
        abstract val isSelected: Boolean
        abstract val isWinning: Boolean

        data class WinningTicket(
            override val isSelected: Boolean,
            override val isWinning: Boolean = true,
            val rewardName: String,
        ) : TicketSubDocument()

        data class NonWinningTicket(
            override val isSelected: Boolean,
            override val isWinning: Boolean = false,
        ) : TicketSubDocument()
    }

    fun toDomain() =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            selectedTicketCount = this.selectedTicketCount,
            tickets =
                this.tickets
                    .map { ticket ->
                        if (ticket is TicketSubDocument.WinningTicket) {
                            WinningTicket(
                                rewardName = ticket.rewardName,
                                isSelected = ticket.isSelected,
                            )
                        } else {
                            NonWinningTicket(
                                isSelected = ticket.isSelected,
                            )
                        }
                    }.toTypedArray(),
        )

    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardDocument {
            val tickets =
                drawingBoard.tickets
                    .map { ticket ->
                        if (ticket is WinningTicket) {
                            TicketSubDocument.WinningTicket(
                                isSelected = ticket.isSelected,
                                rewardName = ticket.rewardName,
                            )
                        } else {
                            TicketSubDocument.NonWinningTicket(
                                isSelected = ticket.isSelected,
                            )
                        }
                    }.toTypedArray()

            return DrawingBoardDocument(
                id = drawingBoard.id,
                surveyId = drawingBoard.surveyId,
                selectedTicketCount = drawingBoard.selectedTicketCount,
                tickets = tickets,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DrawingBoardDocument

        if (id != other.id) return false
        if (surveyId != other.surveyId) return false
        if (selectedTicketCount != other.selectedTicketCount) return false
        if (!tickets.contentEquals(other.tickets)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + surveyId.hashCode()
        result = 31 * result + selectedTicketCount
        result = 31 * result + tickets.contentHashCode()
        return result
    }
}
