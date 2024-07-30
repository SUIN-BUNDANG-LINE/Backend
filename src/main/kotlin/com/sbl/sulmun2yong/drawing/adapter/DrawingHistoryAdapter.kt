package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingHistoryAdapter(
    private val drawingHistoryRepository: DrawingHistoryRepository,
) {
    fun save(drawingHistory: DrawingHistory) {
        drawingHistoryRepository.save(DrawingHistoryDocument.of(drawingHistory))
    }

    fun findByParticipantId(id: UUID): DrawingHistory? =
        drawingHistoryRepository
            .findByParticipantId(id)
            .map { it.toDomain() }
            .orElse(null)
}
