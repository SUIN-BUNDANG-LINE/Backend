package com.sbl.sulmun2yong.entity

import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingBoards")
data class DrawingBoardDocument(
    private val id: UUID,
)
