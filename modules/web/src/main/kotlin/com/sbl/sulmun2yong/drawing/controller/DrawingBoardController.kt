package com.sbl.sulmun2yong.drawing.controller

import com.sbl.sulmun2yong.drawing.dto.request.DrawingRequest
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingBoardService
import com.sbl.sulmun2yong.survey.controller.doc.DrawingBoardApiDoc
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/drawing-board")
class DrawingBoardController(
    private val drawingBoardService: DrawingBoardService,
) : DrawingBoardApiDoc {
    @GetMapping("/info/{surveyId}")
    override fun getDrawingBoard(
        @PathVariable surveyId: UUID,
    ): ResponseEntity<DrawingBoardResponse> {
        val drawingResultResponse = drawingBoardService.getDrawingBoard(surveyId)
        return ResponseEntity.ok(drawingResultResponse)
    }

    @PostMapping("/draw")
    override fun doDrawing(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> {
        val drawingResultResponse =
            drawingBoardService.doDrawing(
                participantId = request.participantId,
                selectedNumber = request.selectedNumber,
                phoneNumber = request.phoneNumber,
            )
        return ResponseEntity.ok(drawingResultResponse)
    }

    // 실험용 — 분산락 미적용. T8 race condition 비교 측정 전용.
    // 운영 사용 금지. 측정 종료 후 제거 가능.
    @PostMapping("/draw-no-lock")
    fun doDrawingWithoutLock(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> {
        val drawingResultResponse =
            drawingBoardService.doDrawingWithoutLock(
                participantId = request.participantId,
                selectedNumber = request.selectedNumber,
                phoneNumber = request.phoneNumber,
            )
        return ResponseEntity.ok(drawingResultResponse)
    }
}
