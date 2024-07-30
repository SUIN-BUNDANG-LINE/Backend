package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingHistoryAdapter(
    private val drawingHistoryRepository: DrawingHistoryRepository,
) {
    fun save(drawingHistory: DrawingHistory) {
        drawingHistoryRepository.save(DrawingHistoryDocument.of(drawingHistory))
    }

    fun findByParticipantIdOrPhoneNumber(
        id: UUID,
        phoneNumber: PhoneNumber,
    ): DrawingHistory? =
        drawingHistoryRepository
            .findByParticipantIdOrPhoneNumber(id, phoneNumber.toString())
            .map { it.toDomain() }
            .orElse(null)
}
