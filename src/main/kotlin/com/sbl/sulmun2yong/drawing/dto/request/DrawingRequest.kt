package com.sbl.sulmun2yong.drawing.dto.request

import com.sbl.sulmun2yong.global.data.PhoneNumber
import java.util.UUID

data class DrawingRequest(
    val participantId: UUID,
    val selectedNumber: Int,
    val phoneNumber: PhoneNumber,
) {
    companion object {
        fun create(
            participantId: UUID,
            selectedNumber: Int,
            phoneNumber: String,
        ): DrawingRequest =
            DrawingRequest(
                participantId = participantId,
                selectedNumber = selectedNumber,
                phoneNumber = PhoneNumber.create(phoneNumber),
            )
    }
}
