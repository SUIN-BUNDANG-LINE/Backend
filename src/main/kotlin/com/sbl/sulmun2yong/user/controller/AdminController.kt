package com.sbl.sulmun2yong.user.controller

import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import com.sbl.sulmun2yong.user.controller.doc.AdminApiDoc
import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import com.sbl.sulmun2yong.user.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.session.SessionRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/api/v1/admin")
class AdminController(
    private val sessionRegistry: SessionRegistry,
    private val adminService: AdminService,
) : AdminApiDoc {
    @GetMapping("/sessions/logged-in-users")
    @ResponseBody
    override fun getAllLoggedInUsers(): ResponseEntity<UserSessionsResponse> {
        val allPrincipals = sessionRegistry.allPrincipals
        return ResponseEntity.ok(adminService.getAllLoggedInUsers(allPrincipals))
    }

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
}
