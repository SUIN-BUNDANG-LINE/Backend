package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardDocument
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
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
            .orElseThrow { InvalidDrawingBoardException() }
            .toDomain()

    fun save(drawingBoard: DrawingBoard) {
        val previousDrawingBoardDocument = drawingBoardRepository.findById(drawingBoard.id)
        val drawingBoardDocument = DrawingBoardDocument.of(drawingBoard)
        // 기존 추첨 보드를 업데이트하는 경우, createdAt을 유지
        if (previousDrawingBoardDocument.isPresent) drawingBoardDocument.createdAt = previousDrawingBoardDocument.get().createdAt
        drawingBoardRepository.save(drawingBoardDocument)
    }
}
