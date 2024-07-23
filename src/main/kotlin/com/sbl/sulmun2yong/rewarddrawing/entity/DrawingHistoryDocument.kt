package com.sbl.sulmun2yong.rewarddrawing.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawing_histories")
class DrawingHistoryDocument(
    @Id
    val id: UUID,
    val userId: UUID,
    val drawingBoardId: UUID,
    val ticketIndex: Int,
) : BaseTimeDocument()
