package com.sbl.sulmun2yong.drawing.controller

import com.sbl.sulmun2yong.drawing.dto.request.DrawingRequest
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingBoardService
import com.sbl.sulmun2yong.survey.controller.doc.DrawingBoardApiDoc
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/drawing-board")
class DrawingBoardController(
    private val drawingBoardService: DrawingBoardService,
) : DrawingBoardApiDoc {
    @GetMapping("/info")
    override fun getDrawingBoard(
        @RequestParam surveyId: UUID,
    ): ResponseEntity<DrawingBoardResponse> {
        val drawingResultResponse = drawingBoardService.getDrawingBoard(surveyId)
        return ResponseEntity.ok(drawingResultResponse)
    }

    @PostMapping("/do-drawing")
    override fun doDrawing(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> {
        val drawingResultResponse =
            drawingBoardService.doDrawing(
                participantId = request.participantId,
                selectedNumber = request.selectedNumber,
            )
        return ResponseEntity.ok(drawingResultResponse)
    }
}
