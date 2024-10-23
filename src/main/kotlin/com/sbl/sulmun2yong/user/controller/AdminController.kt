package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import com.sbl.sulmun2yong.user.controller.doc.AdminApiDoc
import com.sbl.sulmun2yong.user.service.AdminService
import com.sbl.sulmun2yong.user.service.DummyDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService,
    private val dummyDataService: DummyDataService,
) : AdminApiDoc {
    @GetMapping("/drawing-history/{surveyId}")
    override fun getDrawingHistory(
        @PathVariable surveyId: UUID,
        @RequestParam(defaultValue = "false") isWinnerOnly: Boolean,
    ): ResponseEntity<DrawingHistoryGroupResponse> = ResponseEntity.ok(adminService.getDrawingHistory(surveyId, isWinnerOnly))

    @GetMapping("/drawing-history/list")
    @ResponseBody
    override fun getDrawingHistoryList(
        @RequestParam(defaultValue = "false") isWinnerOnly: Boolean,
    ): ResponseEntity<DrawingHistoryGroupListResponse> = ResponseEntity.ok(adminService.getDrawingHistoryList(isWinnerOnly))

    @GetMapping("/dummy-data")
    override fun insertDummyData(
        @RequestParam surveyCount: Int,
    ) = ResponseEntity.ok(dummyDataService.insertDummyData(surveyCount))
}
