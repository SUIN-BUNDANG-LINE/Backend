package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawing_boards")
class DrawingBoardDocument(
    @Id
    val id: UUID,
    val selectedTicketCount: Int,
    val tickets: Array<WinningTicket>,
    val isFinished: Boolean,
) : BaseTimeDocument()
