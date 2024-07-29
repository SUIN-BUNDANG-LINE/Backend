package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
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
    val tickets: Array<Ticket>,
) : BaseTimeDocument() {
    fun toDomain() =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            selectedTicketCount = this.selectedTicketCount,
            tickets = this.tickets,
        )

    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardDocument {
            val tickets = drawingBoard.tickets

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
