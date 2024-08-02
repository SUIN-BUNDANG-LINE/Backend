package com.sbl.sulmun2yong.user.controller.doc

import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Admin", description = "관리자 API")
@RequestMapping("/api/v1/admin")
interface AdminApiDoc {
    @Operation(summary = "로그인한 사용자 조회")
    @GetMapping("/sessions/logged-in-users")
    fun getAllLoggedInUsers(): ResponseEntity<UserSessionsResponse>

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
}
