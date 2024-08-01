package com.sbl.sulmun2yong.drawing.dto.request

import java.util.UUID

data class DrawingRequest(
    val participantId: UUID,
    val selectedNumber: Int,
    val phoneNumber: String,
)
