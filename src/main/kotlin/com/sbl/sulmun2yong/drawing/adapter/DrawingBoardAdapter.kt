package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardDocument
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingBoardAdapter(
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    fun getBySurveyId(surveyId: UUID) =
        drawingBoardRepository
            .findBySurveyId(surveyId)
            .orElseThrow { TODO() }
            .toDomain()

    fun save(drawingBoard: DrawingBoard) {
        drawingBoardRepository.save(DrawingBoardDocument.of(drawingBoard))
    }
}
