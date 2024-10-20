package com.sbl.sulmun2yong.user.controller.doc

import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Admin", description = "관리자 API")
interface AdminApiDoc {
    @Operation(summary = "추첨 기록 설문 id로 조회")
    @GetMapping("/drawing-history/{surveyId}")
    fun getDrawingHistory(
        @PathVariable surveyId: UUID,
        @RequestParam(defaultValue = "false") isWinnerOnly: Boolean,
    ): ResponseEntity<DrawingHistoryGroupResponse>

    @Operation(summary = "추첨 기록 리스트 조회")
    @GetMapping("/drawing-history/list")
    fun getDrawingHistoryList(
        @RequestParam(defaultValue = "false") isWinnerOnly: Boolean,
    ): ResponseEntity<DrawingHistoryGroupListResponse>

    @Operation(summary = "더미 데이터 생성")
    @GetMapping("/dummy-data")
    fun insertDummyData(
        @RequestParam surveyCount: Int,
    ): ResponseEntity<Unit>
}
