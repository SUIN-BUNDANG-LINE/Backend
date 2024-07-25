package com.sbl.sulmun2yong.drawing.domain

import java.util.UUID

class DrawingHistory(
    val userId: UUID,
    val drawingBoardId: UUID,
    val ticketIndex: Int,
    val ip: String,
    val browserFingerprint: String,
)
