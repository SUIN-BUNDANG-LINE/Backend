package com.sbl.sulmun2yong.survey.controller.doc

import com.sbl.sulmun2yong.drawing.dto.request.DrawingRequest
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "DrawingBoard", description = "추첨 관련 API")
@RequestMapping("/api/v1/drawing-board")
interface DrawingBoardApiDoc {
    @Operation(summary = "추첨 보드 정보 조회")
    @GetMapping("/info/{surveyId}")
    fun getDrawingBoard(
        @PathVariable surveyId: UUID,
    ): ResponseEntity<DrawingBoardResponse>

    @Operation(summary = "추점 시작")
    @PostMapping("/api/v1/drawing-board/draw")
    fun doDrawing(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse>
}
