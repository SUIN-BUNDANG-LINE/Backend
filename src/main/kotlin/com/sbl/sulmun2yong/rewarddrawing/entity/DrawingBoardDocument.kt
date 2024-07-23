package com.sbl.sulmun2yong.rewarddrawing.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.rewarddrawing.domain.Ticket
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawing_boards")
class DrawingBoardDocument(
    @Id
    val id: UUID,
    val selectedTicketCount: Int,
    val tickets: Array<Ticket>,
    val isFinished: Boolean,
) : BaseTimeDocument()
