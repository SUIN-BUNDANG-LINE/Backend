package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingHistories")
data class DrawingHistoryDocument(
    @Id
    val id: UUID,
    val participantId: UUID,
    val phoneNumber: String,
    val surveyId: UUID,
    val selectedTicketIndex: Int,
    val ticket: Ticket,
) : BaseTimeDocument()
